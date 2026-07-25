import Foundation
import Combine
import CoreLocation

/// Wrapper di CLLocationManager, controparte del FusedLocationProviderClient
/// usato su Android. Mantiene aggiornamenti continui (necessari perché il
/// server possa leggere la posizione anche con l'app in background, grazie
/// al background mode "location") ed espone una lettura puntuale async.
final class LocationProvider: NSObject, ObservableObject, CLLocationManagerDelegate {
    static let shared = LocationProvider()

    private let manager = CLLocationManager()
    @Published private(set) var lastLocation: CLLocation?
    @Published private(set) var authorizationStatus: CLAuthorizationStatus = .notDetermined

    private var pendingRequests: [UUID: CheckedContinuation<CLLocation, Error>] = [:]
    private let lock = NSLock()

    enum LocationError: LocalizedError {
        case notAuthorized
        case unavailable

        var errorDescription: String? {
            switch self {
            case .notAuthorized: return "Permesso di localizzazione non concesso"
            case .unavailable: return "Posizione non disponibile"
            }
        }
    }

    override private init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.pausesLocationUpdatesAutomatically = false
        authorizationStatus = manager.authorizationStatus
    }

    /// Richiede i permessi e avvia gli aggiornamenti continui.
    func start() {
        switch manager.authorizationStatus {
        case .notDetermined:
            manager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse:
            // Per la raccolta in background serve "Sempre".
            manager.requestAlwaysAuthorization()
            startUpdates()
        case .authorizedAlways:
            startUpdates()
        default:
            break
        }
    }

    func stop() {
        manager.stopUpdatingLocation()
    }

    private func startUpdates() {
        if manager.authorizationStatus == .authorizedAlways {
            manager.allowsBackgroundLocationUpdates = true
            manager.showsBackgroundLocationIndicator = true
        }
        manager.startUpdatingLocation()
    }

    var isAuthorized: Bool {
        manager.authorizationStatus == .authorizedAlways
            || manager.authorizationStatus == .authorizedWhenInUse
    }

    /// Restituisce la posizione corrente: se l'ultima nota è recente (< 10 s)
    /// la riusa, altrimenti attende il prossimo aggiornamento continuo
    /// (equivalente di `getCurrentLocation` con maxUpdateAge=0 su Android,
    /// con una piccola tolleranza per non bloccare le letture pull).
    /// Un timeout evita di lasciare in sospeso per sempre la risposta a una
    /// lettura pull se CoreLocation non produce aggiornamenti.
    func currentLocation(maxAge: TimeInterval = 10, timeout: TimeInterval = 8) async throws -> CLLocation {
        guard isAuthorized else { throw LocationError.notAuthorized }
        if let last = lastLocation, -last.timestamp.timeIntervalSinceNow < maxAge {
            return last
        }
        let requestId = UUID()
        return try await withCheckedThrowingContinuation { continuation in
            lock.lock()
            pendingRequests[requestId] = continuation
            lock.unlock()
            DispatchQueue.global().asyncAfter(deadline: .now() + timeout) { [weak self] in
                guard let self else { return }
                self.lock.lock()
                let expired = self.pendingRequests.removeValue(forKey: requestId)
                self.lock.unlock()
                expired?.resume(throwing: LocationError.unavailable)
            }
        }
    }

    /// Payload JSON della posizione inviato al server per le campagne
    /// di tipo "location".
    static func payload(for location: CLLocation) -> [String: Any] {
        [
            "latitude": location.coordinate.latitude,
            "longitude": location.coordinate.longitude,
            "accuracy": location.horizontalAccuracy,
            "altitude": location.altitude,
            "speed": location.speed,
            "time": Int(location.timestamp.timeIntervalSince1970 * 1000),
            "provider": "ios-corelocation",
        ]
    }

    // MARK: CLLocationManagerDelegate

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
        switch manager.authorizationStatus {
        case .authorizedWhenInUse:
            manager.requestAlwaysAuthorization()
            startUpdates()
        case .authorizedAlways:
            startUpdates()
        default:
            break
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        lastLocation = location
        lock.lock()
        let pending = pendingRequests.values
        pendingRequests = [:]
        lock.unlock()
        pending.forEach { $0.resume(returning: location) }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        lock.lock()
        let pending = pendingRequests.values
        pendingRequests = [:]
        lock.unlock()
        pending.forEach { $0.resume(throwing: error) }
    }
}
