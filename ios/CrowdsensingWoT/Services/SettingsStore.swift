import Foundation

/// Persistenza leggera basata su UserDefaults (equivalente delle
/// SharedPreferences Android): impostazioni dei protocolli, credenziali
/// ricordate, id dispositivo e campagne a cui l'utente è iscritto.
final class SettingsStore {
    static let shared = SettingsStore()
    private let defaults = UserDefaults.standard

    private enum Key {
        static let deviceId = "deviceId"
        static let directoryUrl = "directoryUrl"
        static let mqttHost = "mqttServerBroker"
        static let mqttPort = "mqttServerPort"
        static let mqttClientId = "mqttServerClientId"
        static let mqttUsername = "mqttServerUsername"
        static let mqttPassword = "mqttServerPassword"
        static let savedEmail = "savedEmail"
        static let savedPassword = "savedPassword"
        static let rememberMe = "rememberMe"
        static let joinedCampaignsPrefix = "joined_"
    }

    // Valori di default: gli stessi hardcoded nell'app Android
    // (ServientService.java e default_protocols_values.xml).
    enum Defaults {
        static let directoryUrl = "http://vz5nc4zqbrgzpn13.myfritz.net:8081"
        static let mqttHost = "vz5nc4zqbrgzpn13.myfritz.net"
        static let mqttPort: UInt16 = 1883
    }

    // MARK: Identità dispositivo

    /// Id della Thing esposta dal telefono; generato una volta e riusato,
    /// con lo stesso prefisso richiesto dal proxy ("smartphone_").
    var deviceId: String {
        if let existing = defaults.string(forKey: Key.deviceId) { return existing }
        let newId = "smartphone_" + UUID().uuidString
        defaults.set(newId, forKey: Key.deviceId)
        return newId
    }

    // MARK: Impostazioni server

    var directoryUrl: String {
        get { defaults.string(forKey: Key.directoryUrl) ?? Defaults.directoryUrl }
        set { defaults.set(newValue, forKey: Key.directoryUrl) }
    }

    var mqttHost: String {
        get { defaults.string(forKey: Key.mqttHost) ?? Defaults.mqttHost }
        set { defaults.set(newValue, forKey: Key.mqttHost) }
    }

    var mqttPort: UInt16 {
        get {
            let stored = defaults.integer(forKey: Key.mqttPort)
            return stored > 0 ? UInt16(stored) : Defaults.mqttPort
        }
        set { defaults.set(Int(newValue), forKey: Key.mqttPort) }
    }

    var mqttClientId: String {
        get { defaults.string(forKey: Key.mqttClientId) ?? "" }
        set { defaults.set(newValue, forKey: Key.mqttClientId) }
    }

    var mqttUsername: String {
        get { defaults.string(forKey: Key.mqttUsername) ?? "" }
        set { defaults.set(newValue, forKey: Key.mqttUsername) }
    }

    var mqttPassword: String {
        get { defaults.string(forKey: Key.mqttPassword) ?? "" }
        set { defaults.set(newValue, forKey: Key.mqttPassword) }
    }

    // MARK: Credenziali ricordate ("remember me")

    var rememberMe: Bool {
        get { defaults.bool(forKey: Key.rememberMe) }
        set { defaults.set(newValue, forKey: Key.rememberMe) }
    }

    var savedEmail: String? {
        get { defaults.string(forKey: Key.savedEmail) }
        set { defaults.set(newValue, forKey: Key.savedEmail) }
    }

    var savedPassword: String? {
        get { defaults.string(forKey: Key.savedPassword) }
        set { defaults.set(newValue, forKey: Key.savedPassword) }
    }

    func clearSavedCredentials() {
        defaults.removeObject(forKey: Key.savedEmail)
        defaults.removeObject(forKey: Key.savedPassword)
        defaults.set(false, forKey: Key.rememberMe)
    }

    // MARK: Campagne dell'utente

    /// Le adesioni sono salvate per-utente (chiave = bearer), come le
    /// SharedPreferences per-bearer dell'app Android:
    /// [campaignId: ["mode": Int, "interval": Int?]]
    func joinedCampaigns(bearer: String) -> [String: [String: Int]] {
        defaults.dictionary(forKey: Key.joinedCampaignsPrefix + bearer) as? [String: [String: Int]] ?? [:]
    }

    func saveJoinedCampaign(bearer: String, campaignId: String, mode: SubmissionMode, interval: Int?) {
        var joined = joinedCampaigns(bearer: bearer)
        var entry: [String: Int] = ["mode": mode.rawValue]
        if let interval { entry["interval"] = interval }
        joined[campaignId] = entry
        defaults.set(joined, forKey: Key.joinedCampaignsPrefix + bearer)
    }

    func removeJoinedCampaign(bearer: String, campaignId: String) {
        var joined = joinedCampaigns(bearer: bearer)
        joined.removeValue(forKey: campaignId)
        defaults.set(joined, forKey: Key.joinedCampaignsPrefix + bearer)
    }

    /// Rimuove le adesioni relative a campagne ormai completate
    /// (equivalente di `updateCompletedCampaigns`).
    func removeCompletedCampaigns(bearer: String, completedIds: [String]) {
        var joined = joinedCampaigns(bearer: bearer)
        for id in completedIds { joined.removeValue(forKey: id) }
        defaults.set(joined, forKey: Key.joinedCampaignsPrefix + bearer)
    }
}
