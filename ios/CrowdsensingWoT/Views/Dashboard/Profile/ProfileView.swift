import SwiftUI

/// Profilo utente: punti, campagne completate, impostazioni e logout
/// (equivalente di `ProfileFragment` + `ProfilePreferenceFragment` +
/// `HistoryPreferenceFragment`).
struct ProfileView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @State private var showLogoutConfirmation = false

    private var completedCampaigns: [Campaign] {
        guard let completed = viewModel.user?.completedCampaigns else { return [] }
        return viewModel.allCampaigns.filter { completed.contains($0.id) }
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack(spacing: 16) {
                        Image(systemName: "person.crop.circle.fill")
                            .font(.system(size: 56))
                            .foregroundStyle(.teal)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(viewModel.user?.email ?? "—")
                                .font(.headline)
                            Label("\(viewModel.user?.points ?? 0) punti", systemImage: "star.fill")
                                .font(.subheadline)
                                .foregroundStyle(.orange)
                        }
                    }
                    .padding(.vertical, 4)
                }

                Section("Storico") {
                    if completedCampaigns.isEmpty {
                        Text("Nessuna campagna completata finora.")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(completedCampaigns) { campaign in
                            CampaignRow(campaign: campaign, badge: .completed)
                        }
                    }
                }

                Section {
                    NavigationLink {
                        SettingsView()
                    } label: {
                        Label("Impostazioni", systemImage: "gearshape")
                    }
                }

                Section {
                    Button(role: .destructive) {
                        showLogoutConfirmation = true
                    } label: {
                        Label("Esci", systemImage: "rectangle.portrait.and.arrow.right")
                            .frame(maxWidth: .infinity)
                    }
                    .listRowBackground(Color.clear)
                }
            }
            .navigationTitle("Profilo")
            .refreshable {
                await viewModel.fetchDashboardData()
            }
            .confirmationDialog(
                "Vuoi uscire dall'account?",
                isPresented: $showLogoutConfirmation,
                titleVisibility: .visible
            ) {
                Button("Esci", role: .destructive) {
                    Task { await viewModel.logout(requestedByUser: true) }
                }
                Button("Annulla", role: .cancel) {}
            }
        }
    }
}
