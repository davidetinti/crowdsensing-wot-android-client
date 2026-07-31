import SwiftUI

/// Dettaglio di una campagna attiva: invio manuale dei dati e abbandono
/// (equivalente di `CampaignDetails` + `CampaignSettings` +
/// `CampaignManualSendDialog`).
struct JoinedCampaignDetailView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

    let applied: AppliedCampaign

    @State private var remainingSubmissions: Int?
    @State private var showLeaveConfirmation = false
    @State private var showSendSuccess = false
    @State private var showSchemaForm = false

    var body: some View {
        NavigationStack {
            List {
                Section("Partecipazione") {
                    DetailRow(label: "Modalità", value: applied.mode.label)
                    if applied.mode != .manual {
                        DetailRow(
                            label: "Intervallo",
                            value: timeString(fromSeconds: applied.effectiveInterval)
                        )
                    }
                    DetailRow(label: "Invii richiesti", value: "\(applied.campaign.submissionRequired)")
                    if let remainingSubmissions {
                        DetailRow(label: "Invii rimanenti", value: "\(remainingSubmissions)")
                    }
                    DetailRow(label: "Punti al completamento", value: "\(applied.campaign.points)")
                }

                if let area = applied.campaign.area {
                    Section("Area della campagna") {
                        CampaignAreaMapView(area: area)
                            .frame(height: 200)
                            .listRowInsets(EdgeInsets())
                    }
                }

                if applied.mode == .manual {
                    Section {
                        if applied.campaign.dataSchema != nil {
                            Button {
                                showSchemaForm = true
                            } label: {
                                Label("Compila e invia i dati", systemImage: "square.and.pencil")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .listRowBackground(Color.clear)
                        } else {
                            Button {
                                sendManualData()
                            } label: {
                                Label("Invia i dati adesso", systemImage: "paperplane.fill")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .listRowBackground(Color.clear)
                        }
                    } footer: {
                        if applied.campaign.dataSchema != nil {
                            Text("Compila il modulo con i dati richiesti dalla campagna.")
                        } else {
                            Text("Invia il valore corrente del sensore (\(applied.campaign.typeLabel.lowercased())) come contributo alla campagna.")
                        }
                    }
                }

                Section {
                    Button(role: .destructive) {
                        showLeaveConfirmation = true
                    } label: {
                        Label("Abbandona la campagna", systemImage: "rectangle.portrait.and.arrow.right")
                            .frame(maxWidth: .infinity)
                    }
                    .listRowBackground(Color.clear)
                }
            }
            .navigationTitle(applied.campaign.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Chiudi") { dismiss() }
                }
            }
            .confirmationDialog(
                "Vuoi abbandonare questa campagna?",
                isPresented: $showLeaveConfirmation,
                titleVisibility: .visible
            ) {
                Button("Abbandona", role: .destructive) {
                    Task {
                        await viewModel.leaveCampaign(applied)
                        dismiss()
                    }
                }
                Button("Annulla", role: .cancel) {}
            }
            .sheet(isPresented: $showSchemaForm) {
                if let schema = applied.campaign.dataSchema {
                    SchemaFormView(
                        campaignTitle: applied.campaign.title,
                        schema: schema
                    ) { values in
                        sendSchemaData(values)
                    }
                }
            }
            .alert("Dati inviati!", isPresented: $showSendSuccess) {
                Button("OK", role: .cancel) {
                    if remainingSubmissions == 0 { dismiss() }
                }
            } message: {
                if let remainingSubmissions, remainingSubmissions > 0 {
                    Text("Mancano \(remainingSubmissions) invii per completare la campagna.")
                } else {
                    Text("Hai completato la campagna!")
                }
            }
        }
    }

    private func sendManualData() {
        Task { @MainActor in
            if let remaining = await viewModel.sendManualData(for: applied) {
                remainingSubmissions = remaining
                showSendSuccess = true
            }
        }
    }

    private func sendSchemaData(_ values: [String: Any]) {
        Task { @MainActor in
            if let remaining = await viewModel.sendManualData(for: applied, values: values) {
                remainingSubmissions = remaining
                showSendSuccess = true
            }
        }
    }
}
