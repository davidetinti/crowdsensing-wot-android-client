import Foundation

/// Client HTTP per la Thing Description Directory (TDD).
/// Replica le operazioni di `Servient.java`:
/// - fetch:      GET    {directory}/things/{id}
/// - register:   PUT    {directory}/things/{id}  (body: TD JSON)
/// - unregister: DELETE {directory}/things/{id}
struct ThingDirectoryClient {
    let directoryURL: URL
    var session: URLSession = .shared

    func fetchThingDescription(id: String) async throws -> ThingDescription {
        let url = thingURL(id: id)
        var request = URLRequest(url: url)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        let (data, response) = try await session.data(for: request)
        try checkResponse(response)
        return try ThingDescription(data: data)
    }

    func register(thingDescription: [String: Any], id: String) async throws {
        var request = URLRequest(url: thingURL(id: id))
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: thingDescription)
        let (_, response) = try await session.data(for: request)
        try checkResponse(response)
    }

    func unregister(id: String) async throws {
        var request = URLRequest(url: thingURL(id: id))
        request.httpMethod = "DELETE"
        let (_, response) = try await session.data(for: request)
        try checkResponse(response)
    }

    private func thingURL(id: String) -> URL {
        directoryURL.appendingPathComponent("things").appendingPathComponent(id)
    }

    private func checkResponse(_ response: URLResponse) throws {
        guard let http = response as? HTTPURLResponse else { throw WoTError.invalidResponse }
        guard (200..<300).contains(http.statusCode) else { throw WoTError.httpError(http.statusCode) }
    }
}
