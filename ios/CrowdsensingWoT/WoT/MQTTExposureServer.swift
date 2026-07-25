import Foundation
import CocoaMQTT

/// Espone una `ExposedDeviceThing` via MQTT replicando il protocollo del
/// `MqttProtocolServer` Java:
/// - per ogni property si sottoscrive a `{id}/properties/{name}/readAsk`
/// - alla ricezione di un messaggio (vuoto) legge il valore dal supplier e
///   pubblica il JSON su `{id}/properties/{name}/readAnswer`
/// - alla rimozione della Thing pubblica un retained vuoto su `{id}`
final class MQTTExposureServer: NSObject {
    private var mqtt: CocoaMQTT?
    private var exposedThing: ExposedDeviceThing?
    private var connectContinuations: [CheckedContinuation<Void, Error>] = []
    private let queue = DispatchQueue(label: "mqtt-exposure-server")

    private(set) var brokerHost: String = ""
    private(set) var brokerPort: UInt16 = 1883

    var isConnected: Bool { mqtt?.connState == .connected }

    /// Connette il client al broker. Ritorna quando la CONNACK è ricevuta.
    func connect(host: String, port: UInt16, clientId: String, username: String?, password: String?) async throws {
        if isConnected, host == brokerHost, port == brokerPort { return }
        disconnect()
        brokerHost = host
        brokerPort = port
        let resolvedClientId = clientId.isEmpty
            ? "ios-\(UUID().uuidString.prefix(8))"
            : clientId
        let client = CocoaMQTT(clientID: String(resolvedClientId), host: host, port: port)
        if let username, !username.isEmpty { client.username = username }
        if let password, !password.isEmpty { client.password = password }
        client.keepAlive = 60
        client.autoReconnect = true
        client.autoReconnectTimeInterval = 5
        client.delegate = self
        mqtt = client
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            queue.sync { connectContinuations.append(continuation) }
            if !client.connect() {
                queue.sync {
                    connectContinuations.removeAll()
                }
                continuation.resume(throwing: WoTError.notConnected)
            }
        }
    }

    func disconnect() {
        mqtt?.autoReconnect = false
        mqtt?.disconnect()
        mqtt = nil
        exposedThing = nil
    }

    /// Espone (o ri-espone) la Thing: sottoscrive i topic `readAsk` di tutte
    /// le property correnti.
    func expose(_ thing: ExposedDeviceThing) throws {
        guard let mqtt, mqtt.connState == .connected else { throw WoTError.notConnected }
        exposedThing = thing
        for name in thing.properties.keys {
            mqtt.subscribe(askTopic(thingId: thing.id, property: name), qos: .qos2)
        }
    }

    /// Sottoscrive la singola property aggiunta dopo una expose.
    func exposeProperty(named name: String) throws {
        guard let mqtt, mqtt.connState == .connected, let thing = exposedThing else {
            throw WoTError.notConnected
        }
        mqtt.subscribe(askTopic(thingId: thing.id, property: name), qos: .qos2)
    }

    func unexposeProperty(named name: String) {
        guard let mqtt, let thing = exposedThing else { return }
        mqtt.unsubscribe(askTopic(thingId: thing.id, property: name))
    }

    /// Rimuove la Thing dal broker: annulla le sottoscrizioni e pubblica un
    /// retained vuoto sul topic con l'id della Thing (stessa semantica di
    /// `unexposeTD` nel server Java).
    func unexpose() {
        guard let mqtt, let thing = exposedThing else { return }
        for name in thing.properties.keys {
            mqtt.unsubscribe(askTopic(thingId: thing.id, property: name))
        }
        let clearMessage = CocoaMQTTMessage(topic: thing.id, payload: [], qos: .qos0, retained: true)
        mqtt.publish(clearMessage)
        exposedThing = nil
    }

    private func askTopic(thingId: String, property: String) -> String {
        "\(thingId)/properties/\(property)/readAsk"
    }

    private func handleReadAsk(topic: String) {
        guard let thing = exposedThing else { return }
        let prefix = "\(thing.id)/properties/"
        let suffix = "/readAsk"
        guard topic.hasPrefix(prefix), topic.hasSuffix(suffix) else { return }
        let name = String(topic.dropFirst(prefix.count).dropLast(suffix.count))
        guard let property = thing.properties[name] else { return }
        let answerTopic = "\(thing.id)/properties/\(name)/readAnswer"
        Task { [weak self] in
            let value = await property.supplier?()
            let payload = Self.encodeJSON(value)
            self?.publish(topic: answerTopic, payload: payload)
        }
    }

    private func publish(topic: String, payload: Data) {
        guard let mqtt else { return }
        let message = CocoaMQTTMessage(topic: topic, payload: [UInt8](payload), qos: .qos0, retained: false)
        mqtt.publish(message)
    }

    /// Serializza un valore in JSON; `nil` diventa `null`, come la
    /// serializzazione Jackson del server Java.
    private static func encodeJSON(_ value: Any?) -> Data {
        guard let value else { return Data("null".utf8) }
        if let data = try? JSONSerialization.data(withJSONObject: value, options: [.fragmentsAllowed]) {
            return data
        }
        return Data("null".utf8)
    }
}

extension MQTTExposureServer: CocoaMQTTDelegate {
    func mqtt(_ mqtt: CocoaMQTT, didConnectAck ack: CocoaMQTTConnAck) {
        let continuations = queue.sync { () -> [CheckedContinuation<Void, Error>] in
            let pending = connectContinuations
            connectContinuations = []
            return pending
        }
        if ack == .accept {
            // Dopo una riconnessione automatica ripristina le sottoscrizioni.
            if let thing = exposedThing {
                for name in thing.properties.keys {
                    mqtt.subscribe(askTopic(thingId: thing.id, property: name), qos: .qos2)
                }
            }
            continuations.forEach { $0.resume() }
        } else {
            continuations.forEach { $0.resume(throwing: WoTError.notConnected) }
        }
    }

    func mqtt(_ mqtt: CocoaMQTT, didReceiveMessage message: CocoaMQTTMessage, id: UInt16) {
        handleReadAsk(topic: message.topic)
    }

    func mqttDidDisconnect(_ mqtt: CocoaMQTT, withError err: Error?) {
        let continuations = queue.sync { () -> [CheckedContinuation<Void, Error>] in
            let pending = connectContinuations
            connectContinuations = []
            return pending
        }
        continuations.forEach { $0.resume(throwing: err ?? WoTError.notConnected) }
    }

    func mqtt(_ mqtt: CocoaMQTT, didPublishMessage message: CocoaMQTTMessage, id: UInt16) {}
    func mqtt(_ mqtt: CocoaMQTT, didPublishAck id: UInt16) {}
    func mqtt(_ mqtt: CocoaMQTT, didSubscribeTopics success: NSDictionary, failed: [String]) {}
    func mqtt(_ mqtt: CocoaMQTT, didUnsubscribeTopics topics: [String]) {}
    func mqttDidPing(_ mqtt: CocoaMQTT) {}
    func mqttDidReceivePong(_ mqtt: CocoaMQTT) {}
}
