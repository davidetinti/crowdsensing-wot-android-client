import Foundation

/// Fornisce, on demand, il valore JSON-serializzabile di una property esposta.
typealias PropertyValueSupplier = () async -> Any?

/// Modello della Thing esposta dal telefono ("smartphone_<uuid>"):
/// una property `ping` sempre presente più una property per ogni campagna
/// a cui l'utente partecipa in modalità pull (equivalente della `ExposedThing`
/// costruita in `ServientService.java`).
final class ExposedDeviceThing {
    let id: String
    private(set) var properties: [String: DeviceProperty] = [:]

    struct DeviceProperty {
        let name: String
        let type: String?
        let supplier: PropertyValueSupplier?
    }

    init(id: String) {
        self.id = id
        // Property "ping": il proxy la usa solo per verificare che il device risponda.
        addProperty(name: "ping", type: nil, supplier: nil)
    }

    func addProperty(name: String, type: String?, supplier: PropertyValueSupplier?) {
        properties[name] = DeviceProperty(name: name, type: type, supplier: supplier)
    }

    func removeProperty(name: String) {
        properties.removeValue(forKey: name)
    }

    /// Costruisce la Thing Description del dispositivo con i form MQTT
    /// nello stesso formato prodotto da `MqttProtocolServer.java`:
    /// - href:            mqtt://host:port/{id}/properties/{name}/readAsk
    /// - mqv:answerTopic: {id}/properties/{name}/readAnswer
    func buildThingDescription(brokerHost: String, brokerPort: UInt16) -> [String: Any] {
        var tdProperties: [String: Any] = [:]
        for (name, property) in properties {
            let rootTopic = "\(id)/properties/\(name)"
            var tdProperty: [String: Any] = [
                "readOnly": true,
                "writeOnly": false,
                "observable": false,
                "forms": [
                    [
                        "href": "mqtt://\(brokerHost):\(brokerPort)/\(rootTopic)/readAsk",
                        "contentType": "application/json",
                        "op": ["readproperty"],
                        "mqv:answerTopic": "\(rootTopic)/readAnswer",
                    ] as [String: Any],
                ],
            ]
            if let type = property.type {
                tdProperty["type"] = type
            }
            tdProperties[name] = tdProperty
        }
        return [
            "@context": "https://www.w3.org/2019/wot/td/v1",
            "id": id,
            "title": id,
            "security": ["nosec_sc"],
            "securityDefinitions": ["nosec_sc": ["scheme": "nosec"]],
            "properties": tdProperties,
        ]
    }
}
