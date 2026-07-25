import Foundation

/// Consumer HTTP minimale di una Thing esposta da node-wot (binding HTTP):
/// - lettura property: GET sull'href del form `readproperty`
/// - invocazione action: POST JSON sull'href del form `invokeaction`
/// Equivalente, per l'uso che ne fa l'app, della `ConsumedThing` del
/// wot-servient Java.
final class ConsumedThing {
    let thingDescription: ThingDescription
    private let session: URLSession

    init(thingDescription: ThingDescription, session: URLSession = .shared) {
        self.thingDescription = thingDescription
        self.session = session
    }

    func readProperty(_ name: String) async throws -> Any {
        guard let property = thingDescription.properties[name] else {
            throw WoTError.interactionNotFound(name)
        }
        guard let form = ThingDescription.form(of: property, forOperation: "readproperty"),
              let href = form["href"] as? String,
              let url = URL(string: href) else {
            throw WoTError.formNotFound(name)
        }
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        let (data, response) = try await session.data(for: request)
        try Self.checkResponse(response)
        return try JSONSerialization.jsonObject(with: data, options: [.fragmentsAllowed])
    }

    /// Invoca un'azione con input JSON e restituisce l'output decodificato
    /// (o `nil` se la risposta è vuota, come per `ping`).
    @discardableResult
    func invokeAction(_ name: String, input: [String: Any]) async throws -> Any? {
        guard let action = thingDescription.actions[name] else {
            throw WoTError.interactionNotFound(name)
        }
        guard let form = ThingDescription.form(of: action, forOperation: "invokeaction"),
              let href = form["href"] as? String,
              let url = URL(string: href) else {
            throw WoTError.formNotFound(name)
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.httpBody = try JSONSerialization.data(withJSONObject: input)
        let (data, response) = try await session.data(for: request)
        try Self.checkResponse(response)
        guard !data.isEmpty else { return nil }
        return try? JSONSerialization.jsonObject(with: data, options: [.fragmentsAllowed])
    }

    private static func checkResponse(_ response: URLResponse) throws {
        guard let http = response as? HTTPURLResponse else {
            throw WoTError.invalidResponse
        }
        guard (200..<300).contains(http.statusCode) else {
            throw WoTError.httpError(http.statusCode)
        }
    }
}
