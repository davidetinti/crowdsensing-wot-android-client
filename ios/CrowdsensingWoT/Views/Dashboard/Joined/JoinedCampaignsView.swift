import SwiftUI

/// Elenco delle campagne a cui l'utente partecipa
/// (equivalente di `JoinedCampaignsFragment`).
struct JoinedCampaignsView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @State private var selected: AppliedCampaign?

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.joinedCampaigns.isEmpty {
                    ContentUnavailableCompatView(
                        title: "Nessuna campagna attiva",
                        message: "Aderisci a una campagna dalla scheda \"Campagne\" per iniziare a raccogliere punti.",
                        systemImage: "checkmark.seal"
                    )
                } else {
                    List(viewModel.joinedCampaigns) { applied in
                        Button {
                            selected = applied
                        } label: {
                            VStack(alignment: .leading, spacing: 6) {
                                CampaignRow(campaign: applied.campaign)
                                HStack(spacing: 12) {
                                    Label(applied.mode.label, systemImage: modeIcon(applied.mode))
                                    if applied.mode != .manual {
                                        Label(
                                            timeString(fromSeconds: applied.effectiveInterval),
                                            systemImage: "clock"
                                        )
                                    }
                                }
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .navigationTitle("Le mie campagne")
            .refreshable {
                await viewModel.fetchDashboardData()
            }
            .sheet(item: $selected) { applied in
                JoinedCampaignDetailView(applied: applied)
            }
        }
    }

    private func modeIcon(_ mode: SubmissionMode) -> String {
        switch mode {
        case .automatic: return "arrow.triangle.2.circlepath"
        case .autoWithPref: return "timer"
        case .manual: return "hand.tap"
        }
    }
}
