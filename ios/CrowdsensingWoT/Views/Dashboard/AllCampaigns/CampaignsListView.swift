import SwiftUI

/// Elenco di tutte le campagne disponibili (equivalente di
/// `CampaignsFragment` / `fragment_all_campaigns`).
struct CampaignsListView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @State private var selectedCampaign: Campaign?

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.allCampaigns.isEmpty {
                    ContentUnavailableCompatView(
                        title: "Nessuna campagna",
                        message: "Al momento non ci sono campagne disponibili.",
                        systemImage: "tray"
                    )
                } else {
                    List(viewModel.allCampaigns) { campaign in
                        Button {
                            selectedCampaign = campaign
                        } label: {
                            CampaignRow(campaign: campaign, badge: badge(for: campaign))
                        }
                        .buttonStyle(.plain)
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .navigationTitle("Campagne")
            .refreshable {
                await viewModel.fetchDashboardData()
            }
            .sheet(item: $selectedCampaign) { campaign in
                CampaignDetailView(campaign: campaign)
            }
        }
    }

    private func badge(for campaign: Campaign) -> CampaignRow.Badge? {
        if viewModel.user?.completedCampaigns.contains(campaign.id) == true {
            return .completed
        }
        if viewModel.joinedCampaigns.contains(where: { $0.id == campaign.id }) {
            return .joined
        }
        return nil
    }
}

/// Riga di una campagna, riusata nelle due liste.
struct CampaignRow: View {
    enum Badge {
        case joined, completed

        var label: String {
            switch self {
            case .joined: return "Attiva"
            case .completed: return "Completata"
            }
        }

        var color: Color {
            switch self {
            case .joined: return .teal
            case .completed: return .green
            }
        }
    }

    let campaign: Campaign
    var badge: Badge? = nil

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: campaign.typeSystemImage)
                .font(.title3)
                .foregroundStyle(.teal)
                .frame(width: 36, height: 36)
                .background(Color.teal.opacity(0.12), in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(campaign.title)
                    .font(.headline)
                Text(campaign.organization)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text("\(campaign.points) pt")
                    .font(.subheadline.bold())
                    .foregroundStyle(.orange)
                if let badge {
                    Text(badge.label)
                        .font(.caption2.bold())
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(badge.color.opacity(0.15), in: Capsule())
                        .foregroundStyle(badge.color)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

/// Piccolo sostituto di ContentUnavailableView (disponibile solo da iOS 17).
struct ContentUnavailableCompatView: View {
    let title: String
    let message: String
    let systemImage: String

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: systemImage)
                .font(.system(size: 42))
                .foregroundStyle(.secondary)
            Text(title).font(.headline)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
    }
}
