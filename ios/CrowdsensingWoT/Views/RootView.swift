import SwiftUI

/// Radice dell'app: mostra l'autenticazione o la dashboard, con overlay di
/// caricamento e alert di errore globali (equivalente di `MainActivity`).
struct RootView: View {
    @EnvironmentObject private var viewModel: AppViewModel

    var body: some View {
        ZStack {
            if viewModel.isAuthenticated {
                MainTabView()
            } else {
                AuthView()
            }
            if viewModel.isLoading {
                LoadingOverlay()
            }
        }
        .alert(
            "Errore",
            isPresented: Binding(
                get: { viewModel.errorMessage != nil },
                set: { if !$0 { viewModel.errorMessage = nil } }
            )
        ) {
            Button("OK", role: .cancel) { viewModel.errorMessage = nil }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
        .alert(
            "Campagna completata! 🎉",
            isPresented: Binding(
                get: { viewModel.completedCampaignPoints != nil },
                set: { if !$0 { viewModel.completedCampaignPoints = nil } }
            )
        ) {
            Button("Fantastico!", role: .cancel) { viewModel.completedCampaignPoints = nil }
        } message: {
            Text("Hai guadagnato \(viewModel.completedCampaignPoints ?? 0) punti.")
        }
    }
}

struct LoadingOverlay: View {
    var body: some View {
        ZStack {
            Color.black.opacity(0.25).ignoresSafeArea()
            ProgressView()
                .controlSize(.large)
                .padding(24)
                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
        }
        .transition(.opacity)
    }
}
