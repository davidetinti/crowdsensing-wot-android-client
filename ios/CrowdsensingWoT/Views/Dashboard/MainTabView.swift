import SwiftUI

/// Contenitore a tab della dashboard (equivalente di `DashboardFragment`
/// con la bottom navigation).
struct MainTabView: View {
    @EnvironmentObject private var viewModel: AppViewModel

    var body: some View {
        TabView {
            CampaignsListView()
                .tabItem {
                    Label("Campagne", systemImage: "list.bullet.rectangle")
                }
            JoinedCampaignsView()
                .tabItem {
                    Label("Le mie campagne", systemImage: "checkmark.seal")
                }
            ProfileView()
                .tabItem {
                    Label("Profilo", systemImage: "person.crop.circle")
                }
        }
        .task {
            await viewModel.fetchDashboardData()
        }
    }
}
