import SwiftUI
import MapKit

/// Mappa con il cerchio dell'area di una campagna (equivalente della mappa
/// Google Maps con overlay circolare dell'app Android). Usa MKMapView via
/// UIViewRepresentable perché MapCircle in SwiftUI richiede iOS 17.
struct CampaignAreaMapView: UIViewRepresentable {
    let area: CampaignCircleArea

    func makeUIView(context: Context) -> MKMapView {
        let mapView = MKMapView()
        mapView.delegate = context.coordinator
        mapView.isRotateEnabled = false
        mapView.isPitchEnabled = false
        mapView.showsUserLocation = true

        let circle = MKCircle(center: area.center, radius: area.radius)
        mapView.addOverlay(circle)

        // Inquadra il cerchio con un po' di margine.
        let region = MKCoordinateRegion(
            center: area.center,
            latitudinalMeters: area.radius * 2.6,
            longitudinalMeters: area.radius * 2.6
        )
        mapView.setRegion(region, animated: false)
        return mapView
    }

    func updateUIView(_ uiView: MKMapView, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            guard let circle = overlay as? MKCircle else {
                return MKOverlayRenderer(overlay: overlay)
            }
            let renderer = MKCircleRenderer(circle: circle)
            renderer.fillColor = UIColor.systemTeal.withAlphaComponent(0.18)
            renderer.strokeColor = UIColor.systemTeal
            renderer.lineWidth = 2
            return renderer
        }
    }
}
