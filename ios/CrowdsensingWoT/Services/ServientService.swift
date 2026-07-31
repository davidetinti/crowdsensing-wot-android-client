import Foundation
import Network

/// Orchestratore del lato Web of Things dell'app, controparte diretta di
/// `ServientService.java`:
/// - consuma la Thing "dashboard" (crwsns:dashboard) via HTTP
/// - espone il telefono come Thing via MQTT e la registra nella TDD
/// - invia un ping alla dashboard ogni 10 secondi
/// - reagisce a perdita/recupero della connettività
actor ServientService {
    static let shared = ServientService()

    private let settings = SettingsStore.shared
    private let mqttServer = MQTTExposureServer()

    private var consumedDashboard: ConsumedThing?
    private var exposedDevice: ExposedDeviceThing?
    private var pingTask: Task<Void, Never>?
    private var pathMonitor: NWPathMonitor?
    private var isNetworkAvailable = true

    private static let pingIntervalSeconds: UInt64 = 10

    enum ServientError: LocalizedError {
        case dashboardUnavailable
        case authenticationFailed(String)
        case requestFailed(String)

        var errorDescription: String? {
            switch self {
            case .dashboardUnavailable:
                return "Impossibile raggiungere il server. Riprova più tardi."
            case .authenticationFailed(let message),
                 .requestFailed(let message):
                return message
            }
        }
    }

    // MARK: Avvio / rete

    /// Inizializza il servient: consuma la dashboard e connette il broker MQTT.
    func start() async throws {
        try await consumeDashboard()
        try await connectMQTT()
        startNetworkMonitoring()
    }

    private func consumeDashboard() async throws {
        guard let directoryURL = URL(string: settings.directoryUrl) else {
            throw ServientError.dashboardUnavailable
        }
        let directory = ThingDirectoryClient(directoryURL: directoryURL)
        do {
            let td = try await directory.fetchThingDescription(id: "crwsns:dashboard")
            consumedDashboard = ConsumedThing(thingDescription: td)
        } catch {
            throw ServientError.dashboardUnavailable
        }
    }

    private func connectMQTT() async throws {
        try await mqttServer.connect(
            host: settings.mqttHost,
            port: settings.mqttPort,
            clientId: settings.mqttClientId,
            username: settings.mqttUsername,
            password: settings.mqttPassword
        )
    }

    private func startNetworkMonitoring() {
        guard pathMonitor == nil else { return }
        let monitor = NWPathMonitor()
        monitor.pathUpdateHandler = { [weak self] path in
            Task { await self?.handleNetworkChange(available: path.status == .satisfied) }
        }
        monitor.start(queue: DispatchQueue(label: "servient-network-monitor"))
        pathMonitor = monitor
    }

    private func handleNetworkChange(available: Bool) async {
        guard available != isNetworkAvailable else { return }
        isNetworkAvailable = available
        if available {
            // Alla riconnessione: ri-consuma la dashboard e ri-registra la
            // Thing (equivalente del connectivityObserver Android).
            try? await consumeDashboard()
            try? await connectMQTT()
            if let device = exposedDevice {
                try? mqttServer.expose(device)
                try? await registerDevice(device)
                startPing(deviceId: device.id)
            }
        } else {
            stopPing()
        }
    }

    // MARK: Autenticazione

    func login(email: String, password: String) async throws -> String {
        try await access(email: email, password: password, isLogin: true)
    }

    func register(email: String, password: String) async throws -> String {
        try await access(email: email, password: password, isLogin: false)
    }

    private func access(email: String, password: String, isLogin: Bool) async throws -> String {
        guard let dashboard = consumedDashboard else { throw ServientError.dashboardUnavailable }
        let action = isLogin ? "loginUser" : "registerNewUser"
        let result = try await dashboard.invokeAction(action, input: [
            "email": email,
            "password": password,
        ])
        guard let response = result as? [String: Any] else {
            throw ServientError.authenticationFailed("Risposta del server non valida")
        }
        guard response["authenticated"] as? Bool == true,
              let bearer = response["bearer"] as? String else {
            let message = response["error"] as? String ?? "Autenticazione fallita"
            throw ServientError.authenticationFailed(message)
        }
        return bearer
    }

    // MARK: Esposizione del dispositivo

    /// Espone la Thing del telefono e la registra nella TDD, ripristinando le
    /// property delle campagne pull a cui l'utente è già iscritto.
    /// Da chiamare dopo il login.
    func exposeDevice(restoringPullCampaigns campaigns: [AppliedCampaign] = []) async throws {
        let device = ExposedDeviceThing(id: settings.deviceId)
        for applied in campaigns where applied.mode == .automatic {
            addPullProperty(to: device, for: applied.campaign)
        }
        try mqttServer.expose(device)
        try await registerDevice(device)
        exposedDevice = device
        startPing(deviceId: device.id)
    }

    private func registerDevice(_ device: ExposedDeviceThing) async throws {
        guard let directoryURL = URL(string: settings.directoryUrl) else {
            throw ServientError.dashboardUnavailable
        }
        let directory = ThingDirectoryClient(directoryURL: directoryURL)
        let td = device.buildThingDescription(
            brokerHost: mqttServer.brokerHost,
            brokerPort: mqttServer.brokerPort
        )
        try await directory.register(thingDescription: td, id: device.id)
    }

    /// Aggiunge la property con il supplier del sensore richiesto dal tipo
    /// di campagna (equivalente di `exposeForCampaign`). Il sensore è
    /// risolto dal SensorRegistry, così nuovi tipi di dato si aggiungono
    /// registrando un nuovo DeviceSensor.
    private func addPullProperty(to device: ExposedDeviceThing, for campaign: Campaign) {
        let campaignType = campaign.type
        let supplier: PropertyValueSupplier = {
            await SensorRegistry.shared.read(campaignType: campaignType)
        }
        device.addProperty(name: campaign.id, type: "object", supplier: supplier)
    }

    // MARK: Ping

    private func startPing(deviceId: String) {
        stopPing()
        pingTask = Task { [weak self] in
            while !Task.isCancelled {
                await self?.sendPing(deviceId: deviceId)
                try? await Task.sleep(nanoseconds: Self.pingIntervalSeconds * 1_000_000_000)
            }
        }
    }

    private func sendPing(deviceId: String) async {
        guard let dashboard = consumedDashboard else { return }
        _ = try? await dashboard.invokeAction("ping", input: ["deviceId": deviceId])
    }

    private func stopPing() {
        pingTask?.cancel()
        pingTask = nil
    }

    // MARK: Dati utente e campagne

    func fetchUserData(bearer: String) async throws -> User {
        guard let dashboard = consumedDashboard else { throw ServientError.dashboardUnavailable }
        let result = try await dashboard.invokeAction("retrieveUserData", input: ["bearer": bearer])
        guard let dict = result as? [String: Any], let user = User(dictionary: dict) else {
            throw ServientError.requestFailed("Errore nel recupero dei dati utente")
        }
        return user
    }

    func fetchCampaigns() async throws -> [Campaign] {
        guard let dashboard = consumedDashboard else { throw ServientError.dashboardUnavailable }
        let result = try await dashboard.readProperty("campaigns")
        guard let list = result as? [[String: Any]] else {
            throw ServientError.requestFailed("Errore nel recupero delle campagne")
        }
        return list.compactMap { Campaign(dictionary: $0) }
    }

    // MARK: Partecipazione alle campagne

    func applyToCampaign(bearer: String, campaign: Campaign, mode: SubmissionMode) async throws {
        guard let dashboard = consumedDashboard, let device = exposedDevice else {
            throw ServientError.dashboardUnavailable
        }
        // In modalità pull la campagna diventa una property leggibile dal
        // server: va aggiunta e la TD va ri-registrata prima dell'apply.
        if mode == .automatic {
            addPullProperty(to: device, for: campaign)
            try mqttServer.exposeProperty(named: campaign.id)
            try await registerDevice(device)
        }
        let action = mode == .automatic ? "applyToCampaignPull" : "applyToCampaignPush"
        let result = try await dashboard.invokeAction(action, input: [
            "bearer": bearer,
            "campaignId": campaign.id,
            "deviceId": device.id,
        ])
        guard let response = result as? [String: Any], response["ok"] as? Bool == true else {
            let message = (result as? [String: Any])?["error"] as? String ?? "Errore nell'adesione alla campagna"
            throw ServientError.requestFailed(message)
        }
    }

    /// Invia una submission push (manuale o automatica lato app).
    /// Ritorna il numero di invii rimanenti per completare la campagna.
    func sendPushData(bearer: String, campaignId: String, data: [String: Any]) async throws -> Int {
        guard let dashboard = consumedDashboard, let device = exposedDevice else {
            throw ServientError.dashboardUnavailable
        }
        let result = try await dashboard.invokeAction("sendPushCampaignData", input: [
            "bearer": bearer,
            "campaignId": campaignId,
            "deviceId": device.id,
            "data": data,
        ])
        guard let response = result as? [String: Any], response["ok"] as? Bool == true,
              let remaining = (response["remaining"] as? NSNumber)?.intValue else {
            let message = (result as? [String: Any])?["error"] as? String ?? "Errore nell'invio dei dati"
            throw ServientError.requestFailed(message)
        }
        return remaining
    }

    func leaveCampaign(bearer: String, campaign: Campaign) async throws {
        guard let dashboard = consumedDashboard, let device = exposedDevice else {
            throw ServientError.dashboardUnavailable
        }
        device.removeProperty(name: campaign.id)
        mqttServer.unexposeProperty(named: campaign.id)
        try await registerDevice(device)
        let result = try await dashboard.invokeAction("leaveCampaign", input: [
            "bearer": bearer,
            "campaignId": campaign.id,
            "deviceId": device.id,
        ])
        guard let response = result as? [String: Any], response["ok"] as? Bool == true else {
            let message = (result as? [String: Any])?["error"] as? String ?? "Errore nell'abbandono della campagna"
            throw ServientError.requestFailed(message)
        }
    }

    // MARK: Logout

    func logout() async {
        stopPing()
        if let device = exposedDevice, let directoryURL = URL(string: settings.directoryUrl) {
            let directory = ThingDirectoryClient(directoryURL: directoryURL)
            try? await directory.unregister(id: device.id)
        }
        mqttServer.unexpose()
        exposedDevice = nil
    }
}
