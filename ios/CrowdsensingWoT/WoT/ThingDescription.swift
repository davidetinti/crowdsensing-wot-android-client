import Foundation

/// Rappresentazione minimale di una W3C WoT Thing Description consumata,
/// sufficiente per interagire con la dashboard esposta da node-wot.
/// Viene mantenuta come dizionario JSON per tollerare qualsiasi variazione
/// dello schema (stesso approccio "a HashMap" usato dall'app Android).
struct ThingDescription {
    let raw: [String: Any]

    var id: String? { raw["id"] as? String }
    var title: String? { raw["title"] as? String }

    var properties: [String: [String: Any]] {
        raw["properties"] as? [String: [String: Any]] ?? [:]
    }

    var actions: [String: [String: Any]] {
        raw["actions"] as? [String: [String: Any]] ?? [:]
    }

    init(raw: [String: Any]) {
        self.raw = raw
    }

    init(data: Data) throws {
        guard let dict = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw WoTError.invalidThingDescription
        }
        self.init(raw: dict)
    }

    /// Restituisce i form di un'interazione (property o action).
    static func forms(of interaction: [String: Any]) -> [[String: Any]] {
        interaction["forms"] as? [[String: Any]] ?? []
    }

    /// Cerca il primo form la cui `op` (stringa o array) contiene l'operazione
    /// richiesta; se il form non dichiara `op`, viene accettato come fallback.
    static func form(of interaction: [String: Any], forOperation op: String) -> [String: Any]? {
        let forms = forms(of: interaction)
        for form in forms {
            if let single = form["op"] as? String, single == op { return form }
            if let list = form["op"] as? [String], list.contains(op) { return form }
        }
        // Fallback: form senza op esplicita (node-wot assegna le op di default)
        return forms.first { $0["op"] == nil }
            ?? forms.first
    }
}

enum WoTError: LocalizedError {
    case invalidThingDescription
    case interactionNotFound(String)
    case formNotFound(String)
    case invalidResponse
    case httpError(Int)
    case notConnected
    case timeout

    var errorDescription: String? {
        switch self {
        case .invalidThingDescription: return "Thing Description non valida"
        case .interactionNotFound(let name): return "Interazione '\(name)' non trovata nella Thing Description"
        case .formNotFound(let name): return "Nessun form utilizzabile per '\(name)'"
        case .invalidResponse: return "Risposta del server non valida"
        case .httpError(let code): return "Errore HTTP \(code)"
        case .notConnected: return "Servient non disponibile"
        case .timeout: return "Timeout della richiesta"
        }
    }
}
