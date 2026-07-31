import Foundation

/// Schema dei dati di una campagna (campo `data_schema` del documento),
/// sottoinsieme di JSON Schema allineato al DataSchema delle WoT Thing
/// Description. Descrive i campi che il device deve inviare e permette di
/// definire nuovi tipi di dato condivisibili senza modificare le app.
struct DataSchema: Equatable {
    let fields: [Field]

    struct Field: Identifiable, Equatable {
        enum FieldType: String {
            case number, integer, string, boolean
        }

        let name: String
        let type: FieldType
        let title: String?
        let details: String?
        let unit: String?
        let minimum: Double?
        let maximum: Double?
        let allowedValues: [String]?
        let isRequired: Bool

        var id: String { name }

        var label: String { title ?? name }
    }

    init?(dictionary: [String: Any]) {
        guard (dictionary["type"] as? String ?? "object") == "object",
              let properties = dictionary["properties"] as? [String: [String: Any]],
              !properties.isEmpty
        else { return nil }
        let required = Set(dictionary["required"] as? [String] ?? [])
        var parsed: [Field] = []
        for (name, property) in properties {
            let type = Field.FieldType(rawValue: property["type"] as? String ?? "string") ?? .string
            let allowed = (property["enum"] as? [Any])?.map { String(describing: $0) }
            parsed.append(Field(
                name: name,
                type: type,
                title: property["title"] as? String,
                details: property["description"] as? String,
                unit: property["unit"] as? String,
                minimum: (property["minimum"] as? NSNumber)?.doubleValue,
                maximum: (property["maximum"] as? NSNumber)?.doubleValue,
                allowedValues: allowed,
                isRequired: required.contains(name)
            ))
        }
        // JSONSerialization non preserva l'ordine: prima i campi obbligatori,
        // poi gli altri, in ordine alfabetico per stabilità.
        fields = parsed.sorted {
            if $0.isRequired != $1.isRequired { return $0.isRequired }
            return $0.name < $1.name
        }
    }

    /// Valida localmente i valori raccolti dal form prima dell'invio
    /// (la stessa validazione viene rifatta lato proxy).
    /// Restituisce nil se tutto è valido, altrimenti il messaggio di errore.
    func validate(_ values: [String: Any]) -> String? {
        for field in fields {
            let value = values[field.name]
            if value == nil {
                if field.isRequired { return "Il campo \"\(field.label)\" è obbligatorio" }
                continue
            }
            switch field.type {
            case .number, .integer:
                guard !(value is Bool), let number = (value as? NSNumber)?.doubleValue else {
                    return "Il campo \"\(field.label)\" deve essere un numero"
                }
                if field.type == .integer, number != number.rounded() {
                    return "Il campo \"\(field.label)\" deve essere un numero intero"
                }
                if let minimum = field.minimum, number < minimum {
                    return "Il campo \"\(field.label)\" deve essere ≥ \(formatted(minimum))"
                }
                if let maximum = field.maximum, number > maximum {
                    return "Il campo \"\(field.label)\" deve essere ≤ \(formatted(maximum))"
                }
            case .string:
                guard value is String else {
                    return "Il campo \"\(field.label)\" deve essere un testo"
                }
            case .boolean:
                guard value is Bool else {
                    return "Il campo \"\(field.label)\" deve essere vero o falso"
                }
            }
            if let allowed = field.allowedValues, !matchesAllowed(value!, allowed: allowed) {
                return "Il campo \"\(field.label)\" deve essere uno tra: \(allowed.joined(separator: ", "))"
            }
        }
        return nil
    }

    /// Confronto con i valori ammessi tollerante ai numeri: "1" (dallo
    /// schema) deve corrispondere sia a 1 sia a 1.0 nel payload.
    private func matchesAllowed(_ value: Any, allowed: [String]) -> Bool {
        allowed.contains { allowedString in
            if !(value is Bool), let number = value as? NSNumber,
               let allowedNumber = Double(allowedString) {
                return number.doubleValue == allowedNumber
            }
            return allowedString == String(describing: value)
        }
    }

    private func formatted(_ value: Double) -> String {
        if value == value.rounded(), abs(value) < 1e15 {
            return String(Int(value))
        }
        return String(value)
    }
}
