import SwiftUI

/// Dettaglio di una campagna con avvio del flusso di adesione
/// (equivalente di `CampaignDetailsDialog`).
struct CampaignDetailView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

    let campaign: Campaign
    @State private var showJoinFlow = false

    private var isCompleted: Bool {
        viewModel.user?.completedCampaigns.contains(campaign.id) == true
    }

    private var isJoined: Bool {
        viewModel.joinedCampaigns.contains { $0.id == campaign.id }
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 8) {
                        Label(campaign.typeLabel, systemImage: campaign.typeSystemImage)
                            .font(.subheadline.bold())
                            .foregroundStyle(.teal)
                        Text(campaign.description)
                            .font(.body)
                    }
                    .padding(.vertical, 4)
                }

                Section("Dettagli") {
                    DetailRow(label: "Organizzazione", value: campaign.organization)
                    DetailRow(label: "Punti", value: "\(campaign.points)")
                    DetailRow(label: "Invii richiesti", value: "\(campaign.submissionRequired)")
                    DetailRow(
                        label: "Intervallo ideale",
                        value: timeString(fromSeconds: campaign.idealSubmissionInterval)
                    )
                }

                if let area = campaign.area {
                    Section("Area della campagna") {
                        CampaignAreaMapView(area: area)
                            .frame(height: 220)
                            .listRowInsets(EdgeInsets())
                    }
                }

                Section {
                    if isCompleted {
                        Label("Campagna già completata", systemImage: "checkmark.seal.fill")
                            .foregroundStyle(.green)
                    } else if isJoined {
                        Label("Hai già aderito a questa campagna", systemImage: "checkmark.circle")
                            .foregroundStyle(.teal)
                    } else if campaign.availableModes.isEmpty {
                        Label("Nessuna modalità di invio disponibile", systemImage: "xmark.circle")
                            .foregroundStyle(.secondary)
                    } else {
                        Button {
                            showJoinFlow = true
                        } label: {
                            Text("Partecipa alla campagna")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .listRowBackground(Color.clear)
                    }
                }
            }
            .navigationTitle(campaign.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Chiudi") { dismiss() }
                }
            }
            .sheet(isPresented: $showJoinFlow) {
                JoinCampaignFlowView(campaign: campaign) {
                    dismiss()
                }
            }
        }
    }
}

struct DetailRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
            Spacer()
            Text(value)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.trailing)
        }
    }
}
