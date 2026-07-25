import Foundation
import CoreLocation

/// Modalità di partecipazione a una campagna.
/// I rawValue corrispondono agli ordinal dell'enum Android `SubmissionMode`,
/// così i dati salvati restano interpretabili allo stesso modo.
enum SubmissionMode: Int, Codable, CaseIterable, Identifiable {
    case automatic = 0      // il server legge i dati dal telefono (pull)
    case autoWithPref = 1   // il telefono invia i dati a intervallo scelto (push automatico)
    case manual = 2         // l'utente invia i dati manualmente (push manuale)

    var id: Int { rawValue }

    var label: String {
        switch self {
        case .automatic: return "Automatica"
        case .autoWithPref: return "Automatica con intervallo"
        case .manual: return "Manuale"
        }
    }

    var explanation: String {
        switch self {
        case .automatic:
            return "Il server raccoglie i dati dal tuo dispositivo all'intervallo ideale della campagna."
        case .autoWithPref:
            return "L'app invia i dati automaticamente all'intervallo che scegli tu."
        case .manual:
            return "Invii tu i dati, quando vuoi, con un tocco."
        }
    }
}

/// Area geografica di una campagna (attualmente solo cerchi, come su Android).
struct CampaignCircleArea: Equatable {
    let radius: Double
    let centerLat: Double
    let centerLng: Double

    var center: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: centerLat, longitude: centerLng)
    }

    func contains(_ location: CLLocation) -> Bool {
        let center = CLLocation(latitude: centerLat, longitude: centerLng)
        return location.distance(from: center) <= radius
    }

    init?(dictionary: [String: Any]) {
        guard (dictionary["type"] as? String) == "circle",
              let radius = (dictionary["radius"] as? NSNumber)?.doubleValue,
              let lat = (dictionary["center_lat"] as? NSNumber)?.doubleValue,
              let lng = (dictionary["center_lng"] as? NSNumber)?.doubleValue
        else { return nil }
        self.radius = radius
        self.centerLat = lat
        self.centerLng = lng
    }
}

/// Campagna di crowdsensing, decodificata dal documento restituito dalla
/// property `campaigns` della dashboard (stessi campi letti da `Campaign.java`).
struct Campaign: Identifiable, Equatable {
    let id: String
    let title: String
    let description: String
    let organization: String
    let idealSubmissionInterval: Int
    let points: Int
    let pullEnabled: Bool
    let pushAutoEnabled: Bool
    let pushInputEnabled: Bool
    let submissionRequired: Int
    let type: String
    let area: CampaignCircleArea?

    init?(dictionary: [String: Any]) {
        guard let id = dictionary["_id"] as? String else { return nil }
        self.id = id
        title = dictionary["title"] as? String ?? ""
        description = dictionary["description"] as? String ?? ""
        organization = dictionary["organization"] as? String ?? ""
        idealSubmissionInterval = (dictionary["ideal_submission_interval"] as? NSNumber)?.intValue ?? 0
        points = (dictionary["points"] as? NSNumber)?.intValue ?? 0
        pullEnabled = dictionary["pull_enabled"] as? Bool ?? false
        pushAutoEnabled = dictionary["push_auto_enabled"] as? Bool ?? false
        pushInputEnabled = dictionary["push_input_enabled"] as? Bool ?? false
        submissionRequired = (dictionary["submission_required"] as? NSNumber)?.intValue ?? 0
        type = dictionary["type"] as? String ?? ""
        if let areaDict = dictionary["area"] as? [String: Any] {
            area = CampaignCircleArea(dictionary: areaDict)
        } else {
            area = nil
        }
    }

    /// Modalità disponibili in base ai flag della campagna.
    var availableModes: [SubmissionMode] {
        var modes: [SubmissionMode] = []
        if pullEnabled { modes.append(.automatic) }
        if pushAutoEnabled { modes.append(.autoWithPref) }
        if pushInputEnabled { modes.append(.manual) }
        return modes
    }

    var typeLabel: String {
        switch type {
        case "location", "gps": return "Posizione"
        case "temperature": return "Temperatura"
        case "camera": return "Fotocamera"
        default: return type.capitalized
        }
    }

    var typeSystemImage: String {
        switch type {
        case "location", "gps": return "location.fill"
        case "temperature": return "thermometer.medium"
        case "camera": return "camera.fill"
        default: return "dot.radiowaves.left.and.right"
        }
    }
}

/// Campagna a cui l'utente è iscritto, con modalità e intervallo scelti
/// (equivalente di `AppliedCampaign.java`).
struct AppliedCampaign: Identifiable, Equatable {
    let campaign: Campaign
    let mode: SubmissionMode
    let interval: Int?

    var id: String { campaign.id }

    /// Intervallo effettivo in secondi.
    var effectiveInterval: Int {
        interval ?? campaign.idealSubmissionInterval
    }
}

/// Converte un numero di secondi in una stringa leggibile
/// (equivalente di `DashboardViewModel.calculateTimeString`).
func timeString(fromSeconds value: Int) -> String {
    guard value > 0 else { return "0 secondi" }
    let hours = value / 3600
    let minutes = (value % 3600) / 60
    let seconds = value % 60
    var parts: [String] = []
    if hours > 0 { parts.append("\(hours) " + (hours == 1 ? "ora" : "ore")) }
    if minutes > 0 { parts.append("\(minutes) " + (minutes == 1 ? "minuto" : "minuti")) }
    if seconds > 0 { parts.append("\(seconds) " + (seconds == 1 ? "secondo" : "secondi")) }
    if parts.count > 1 {
        let last = parts.removeLast()
        return parts.joined(separator: ", ") + " e " + last
    }
    return parts[0]
}
