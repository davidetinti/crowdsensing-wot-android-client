import Foundation
import SwiftUI

/// Stato centrale dell'app, fusione di `MainViewModel` e `DashboardViewModel`
/// dell'app Android: autenticazione, dati della dashboard, adesioni e
/// invii automatici push.
@MainActor
final class AppViewModel: ObservableObject {

    // MARK: Stato pubblicato

    @Published var bearer: String?
    @Published var user: User?
    @Published var allCampaigns: [Campaign] = []
    @Published var joinedCampaigns: [AppliedCampaign] = []
    @Published var loadingCounter = 0
    @Published var errorMessage: String?
    @Published var completedCampaignPoints: Int?

    var isLoading: Bool { loadingCounter > 0 }
    var isAuthenticated: Bool { bearer != nil }

    private let servient = ServientService.shared
    private let settings = SettingsStore.shared
    private var servientStarted = false
    private var autoPushTasks: [String: Task<Void, Never>] = [:]

    // MARK: Ciclo di vita

    /// Avvio dell'app: parte la localizzazione e, se le credenziali sono
    /// state ricordate, tenta il login automatico.
    func onAppStart() async {
        LocationProvider.shared.start()
        await ensureServientStarted()
        if settings.rememberMe,
           let email = settings.savedEmail,
           let password = settings.savedPassword {
            await authenticate(email: email, password: password, isLogin: true, rememberMe: true)
        }
    }

    private func ensureServientStarted() async {
        guard !servientStarted else { return }
        addLoading()
        defer { removeLoading() }
        do {
            try await servient.start()
            servientStarted = true
        } catch {
            show(error)
        }
    }

    // MARK: Autenticazione

    func authenticate(email: String, password: String, isLogin: Bool, rememberMe: Bool) async {
        await ensureServientStarted()
        guard servientStarted else { return }
        addLoading()
        defer { removeLoading() }
        do {
            let token: String = isLogin
                ? try await servient.login(email: email, password: password)
                : try await servient.register(email: email, password: password)
            bearer = token
            if rememberMe {
                settings.rememberMe = true
                settings.savedEmail = email
                settings.savedPassword = password
            } else {
                settings.clearSavedCredentials()
            }
            // Espone il telefono come Thing ripristinando le campagne pull
            // già attive per questo utente.
            let restored = restoredCampaigns(bearer: token, campaigns: [])
            try await servient.exposeDevice(restoringPullCampaigns: restored)
            await fetchDashboardData()
        } catch {
            bearer = nil
            show(error)
        }
    }

    func logout(requestedByUser: Bool) async {
        addLoading()
        defer { removeLoading() }
        stopAllAutoPush()
        await servient.logout()
        if requestedByUser {
            settings.clearSavedCredentials()
        }
        bearer = nil
        user = nil
        allCampaigns = []
        joinedCampaigns = []
    }

    // MARK: Dati dashboard

    /// Scarica dati utente e campagne e ricostruisce l'elenco delle adesioni
    /// (equivalente di `fetchDashboardData` + `fetchUserCampaigns`).
    func fetchDashboardData() async {
        guard let bearer else { return }
        addLoading()
        defer { removeLoading() }
        do {
            let fetchedUser = try await servient.fetchUserData(bearer: bearer)
            user = fetchedUser
            settings.removeCompletedCampaigns(bearer: bearer, completedIds: fetchedUser.completedCampaigns)
            allCampaigns = try await servient.fetchCampaigns()
            rebuildJoinedCampaigns()
        } catch {
            show(error)
            await logout(requestedByUser: false)
        }
    }

    private func rebuildJoinedCampaigns() {
        guard let bearer else { return }
        joinedCampaigns = restoredCampaigns(bearer: bearer, campaigns: allCampaigns)
        syncAutoPushTimers()
    }

    /// Ricostruisce le AppliedCampaign dalle adesioni salvate localmente.
    /// Se `campaigns` è vuoto (login appena avvenuto, elenco non ancora
    /// scaricato) restituisce solo id e modalità per il ripristino pull.
    private func restoredCampaigns(bearer: String, campaigns: [Campaign]) -> [AppliedCampaign] {
        let saved = settings.joinedCampaigns(bearer: bearer)
        if campaigns.isEmpty {
            // Al login le campagne non sono ancora note: per ripristinare le
            // property pull basta un Campaign minimale con id e il tipo
            // persistito al momento dell'adesione.
            return saved.compactMap { id, entry in
                guard let modeRaw = entry["mode"] as? Int,
                      let mode = SubmissionMode(rawValue: modeRaw) else { return nil }
                let type = entry["type"] as? String ?? "location"
                guard let campaign = Campaign(dictionary: ["_id": id, "type": type]) else { return nil }
                return AppliedCampaign(campaign: campaign, mode: mode, interval: entry["interval"] as? Int)
            }
        }
        return campaigns.compactMap { campaign in
            guard let entry = saved[campaign.id],
                  let modeRaw = entry["mode"] as? Int,
                  let mode = SubmissionMode(rawValue: modeRaw) else { return nil }
            return AppliedCampaign(campaign: campaign, mode: mode, interval: entry["interval"] as? Int)
        }
    }

    // MARK: Adesione alle campagne

    func applyToCampaign(_ campaign: Campaign, mode: SubmissionMode, interval: Int?) async -> Bool {
        guard let bearer, let user else { return false }
        guard !user.completedCampaigns.contains(campaign.id) else {
            errorMessage = "Hai già completato questa campagna!"
            return false
        }
        guard settings.joinedCampaigns(bearer: bearer)[campaign.id] == nil else {
            errorMessage = "Hai già aderito a questa campagna"
            return false
        }
        addLoading()
        defer { removeLoading() }
        do {
            try await servient.applyToCampaign(bearer: bearer, campaign: campaign, mode: mode)
            let effectiveInterval = mode == .automatic ? campaign.idealSubmissionInterval : interval
            settings.saveJoinedCampaign(
                bearer: bearer,
                campaignId: campaign.id,
                mode: mode,
                interval: effectiveInterval,
                type: campaign.type
            )
            rebuildJoinedCampaigns()
            return true
        } catch {
            show(error)
            return false
        }
    }

    func leaveCampaign(_ applied: AppliedCampaign) async {
        guard let bearer else { return }
        addLoading()
        defer { removeLoading() }
        do {
            try await servient.leaveCampaign(bearer: bearer, campaign: applied.campaign)
            settings.removeJoinedCampaign(bearer: bearer, campaignId: applied.id)
            rebuildJoinedCampaigns()
        } catch {
            show(error)
        }
    }

    // MARK: Invio dati push

    /// Invio manuale del valore letto dal sensore associato al tipo della
    /// campagna. Ritorna gli invii rimanenti, o nil in caso di errore.
    func sendManualData(for applied: AppliedCampaign) async -> Int? {
        addLoading()
        defer { removeLoading() }
        guard let payload = await SensorRegistry.shared.read(campaignType: applied.campaign.type) else {
            errorMessage = "Nessun sensore disponibile per questa campagna"
            return nil
        }
        return await submitPushData(campaign: applied.campaign, payload: payload)
    }

    /// Invio manuale di valori compilati dall'utente tramite il form
    /// generato dal `data_schema` della campagna.
    func sendManualData(for applied: AppliedCampaign, values: [String: Any]) async -> Int? {
        if let schema = applied.campaign.dataSchema,
           let validationError = schema.validate(values) {
            errorMessage = validationError
            return nil
        }
        addLoading()
        defer { removeLoading() }
        return await submitPushData(campaign: applied.campaign, payload: values)
    }

    /// Invio effettivo con gestione del completamento della campagna.
    private func submitPushData(campaign: Campaign, payload: [String: Any]) async -> Int? {
        guard let bearer else { return nil }
        do {
            let remaining = try await servient.sendPushData(
                bearer: bearer,
                campaignId: campaign.id,
                data: payload
            )
            if remaining == 0 {
                settings.removeJoinedCampaign(bearer: bearer, campaignId: campaign.id)
                completedCampaignPoints = campaign.points
                await fetchDashboardData()
            }
            return remaining
        } catch {
            show(error)
            return nil
        }
    }

    // MARK: Push automatico (AUTO_WITH_PREF)

    /// Mantiene un timer per ogni campagna in modalità push automatica.
    private func syncAutoPushTimers() {
        let autoCampaigns = joinedCampaigns.filter { $0.mode == .autoWithPref }
        let activeIds = Set(autoCampaigns.map(\.id))
        // Ferma i timer di campagne non più attive.
        for (id, task) in autoPushTasks where !activeIds.contains(id) {
            task.cancel()
            autoPushTasks.removeValue(forKey: id)
        }
        // Avvia i timer mancanti.
        for applied in autoCampaigns where autoPushTasks[applied.id] == nil {
            autoPushTasks[applied.id] = makeAutoPushTask(for: applied)
        }
    }

    private func makeAutoPushTask(for applied: AppliedCampaign) -> Task<Void, Never> {
        let interval = max(applied.effectiveInterval, 1)
        return Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: UInt64(interval) * 1_000_000_000)
                guard !Task.isCancelled, let self else { return }
                // Il payload è prodotto dal sensore associato al tipo della
                // campagna (posizione, batteria, barometro, ...).
                guard let payload = await SensorRegistry.shared.read(
                    campaignType: applied.campaign.type
                ) else { continue }
                let remaining = await self.submitPushData(campaign: applied.campaign, payload: payload)
                if remaining == 0 { return }
            }
        }
    }

    private func stopAllAutoPush() {
        autoPushTasks.values.forEach { $0.cancel() }
        autoPushTasks.removeAll()
    }

    // MARK: Loading / errori

    func addLoading() { loadingCounter += 1 }
    func removeLoading() { loadingCounter = max(0, loadingCounter - 1) }

    private func show(_ error: Error) {
        errorMessage = error.localizedDescription
    }
}
