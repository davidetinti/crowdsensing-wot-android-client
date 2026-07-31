import Foundation
import CoreMotion
import UIKit

/// Un sensore del dispositivo in grado di produrre il payload per uno o più
/// tipi di campagna. Per rendere condivisibile un nuovo tipo di dato con
/// lettura automatica basta implementare questo protocollo e registrare
/// l'istanza nel `SensorRegistry`.
protocol DeviceSensor {
    /// Valori del campo `type` delle campagne gestite da questo sensore.
    var campaignTypes: [String] { get }
    /// True se il sensore esiste su questo dispositivo.
    var isAvailable: Bool { get }
    /// Legge il valore corrente; nil se la lettura non è possibile.
    func read() async -> [String: Any]?
}

/// Registro dei sensori disponibili sul dispositivo, consultato per le
/// letture pull, per il push automatico e per stabilire quali modalità di
/// partecipazione sono proponibili per una campagna.
final class SensorRegistry {
    static let shared = SensorRegistry()

    private let sensors: [DeviceSensor]

    private init() {
        sensors = [
            LocationSensor(),
            BatterySensor(),
            BarometerSensor(),
            AccelerometerSensor(),
        ]
    }

    func sensor(forCampaignType type: String) -> DeviceSensor? {
        sensors.first { $0.campaignTypes.contains(type) && $0.isAvailable }
    }

    func read(campaignType: String) async -> [String: Any]? {
        await sensor(forCampaignType: campaignType)?.read()
    }
}

/// Modalità effettivamente proponibili sul device: le modalità automatiche
/// richiedono un sensore per il tipo della campagna, quella manuale un
/// sensore oppure un `data_schema` da compilare a mano.
extension Campaign {
    var supportedModes: [SubmissionMode] {
        let hasSensor = SensorRegistry.shared.sensor(forCampaignType: type) != nil
        return availableModes.filter { mode in
            switch mode {
            case .automatic, .autoWithPref: return hasSensor
            case .manual: return hasSensor || dataSchema != nil
            }
        }
    }
}

// MARK: - Sensori

/// Posizione (CoreLocation) — tipi "location" e "gps".
struct LocationSensor: DeviceSensor {
    let campaignTypes = ["location", "gps"]
    var isAvailable: Bool { true }

    func read() async -> [String: Any]? {
        guard let location = try? await LocationProvider.shared.currentLocation() else {
            return nil
        }
        return LocationProvider.payload(for: location)
    }
}

/// Livello di batteria (UIDevice) — tipo "battery".
struct BatterySensor: DeviceSensor {
    let campaignTypes = ["battery"]
    var isAvailable: Bool { true }

    func read() async -> [String: Any]? {
        await MainActor.run { () -> [String: Any]? in
            let device = UIDevice.current
            device.isBatteryMonitoringEnabled = true
            let level = device.batteryLevel
            guard level >= 0 else { return nil }
            let state: String
            switch device.batteryState {
            case .charging: state = "charging"
            case .full: state = "full"
            case .unplugged: state = "unplugged"
            default: state = "unknown"
            }
            return [
                "level": Int((level * 100).rounded()),
                "state": state,
            ]
        }
    }
}

/// Pressione atmosferica dal barometro (CMAltimeter) — tipo "pressure".
final class BarometerSensor: DeviceSensor {
    let campaignTypes = ["pressure"]
    var isAvailable: Bool { CMAltimeter.isRelativeAltitudeAvailable() }

    private let altimeter = CMAltimeter()

    func read() async -> [String: Any]? {
        guard isAvailable else { return nil }
        return await withCheckedContinuation { continuation in
            var resumed = false
            let finish: ([String: Any]?) -> Void = { [altimeter] value in
                guard !resumed else { return }
                resumed = true
                altimeter.stopRelativeAltitudeUpdates()
                continuation.resume(returning: value)
            }
            altimeter.startRelativeAltitudeUpdates(to: .main) { data, _ in
                guard let data else {
                    finish(nil)
                    return
                }
                // kPa, come riportato da CMAltitudeData.pressure
                finish(["pressure": data.pressure.doubleValue])
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                finish(nil)
            }
        }
    }
}

/// Accelerazione (CoreMotion) — tipo "motion".
final class AccelerometerSensor: DeviceSensor {
    let campaignTypes = ["motion"]

    private let manager = CMMotionManager()

    var isAvailable: Bool { manager.isAccelerometerAvailable }

    func read() async -> [String: Any]? {
        guard isAvailable else { return nil }
        manager.accelerometerUpdateInterval = 0.1
        manager.startAccelerometerUpdates()
        defer { manager.stopAccelerometerUpdates() }
        // Attende brevemente il primo campione.
        for _ in 0..<20 {
            if let data = manager.accelerometerData {
                return [
                    "x": data.acceleration.x,
                    "y": data.acceleration.y,
                    "z": data.acceleration.z,
                ]
            }
            try? await Task.sleep(nanoseconds: 100_000_000)
        }
        return nil
    }
}
