import SwiftUI

/// Flusso di adesione a una campagna in quattro passi:
/// modalità → intervallo (solo push automatico) → riepilogo → conferma.
/// Equivalente della catena di dialog Android
/// JoinCampaignDialog → IntervalDialog → ReviewDialog → SuccessDialog.
struct JoinCampaignFlowView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

    let campaign: Campaign
    /// Chiamata al termine dell'adesione per chiudere anche il dettaglio.
    var onJoined: () -> Void = {}

    private enum Step {
        case mode, interval, review, success
    }

    @State private var step: Step = .mode
    @State private var selectedMode: SubmissionMode?
    @State private var selectedInterval: Int?

    /// Intervalli proposti per la modalità push automatica: l'intervallo
    /// ideale della campagna e alcuni multipli.
    private var intervalOptions: [Int] {
        let ideal = max(campaign.idealSubmissionInterval, 10)
        return [ideal, ideal * 2, ideal * 3, ideal * 5]
    }

    var body: some View {
        NavigationStack {
            Group {
                switch step {
                case .mode: modeStep
                case .interval: intervalStep
                case .review: reviewStep
                case .success: successStep
                }
            }
            .navigationTitle("Partecipa")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if step != .success {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button("Annulla") { dismiss() }
                    }
                }
            }
        }
        .interactiveDismissDisabled(step == .success)
    }

    // MARK: Passi

    private var modeStep: some View {
        List {
            Section {
                Text("Scegli come inviare i dati per \"\(campaign.title)\".")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Section {
                ForEach(campaign.availableModes) { mode in
                    Button {
                        selectedMode = mode
                        selectedInterval = nil
                        step = (mode == .autoWithPref) ? .interval : .review
                    } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(mode.label)
                                .font(.headline)
                                .foregroundStyle(.primary)
                            Text(mode.explanation)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
        }
    }

    private var intervalStep: some View {
        List {
            Section {
                Text("Ogni quanto vuoi inviare i dati?")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Section {
                ForEach(intervalOptions, id: \.self) { interval in
                    Button {
                        selectedInterval = interval
                        step = .review
                    } label: {
                        HStack {
                            Text(timeString(fromSeconds: interval))
                                .foregroundStyle(.primary)
                            if interval == campaign.idealSubmissionInterval {
                                Spacer()
                                Text("Consigliato")
                                    .font(.caption.bold())
                                    .foregroundStyle(.teal)
                            }
                        }
                    }
                }
            }
        }
    }

    private var reviewStep: some View {
        List {
            Section("Riepilogo") {
                DetailRow(label: "Campagna", value: campaign.title)
                DetailRow(label: "Modalità", value: selectedMode?.label ?? "-")
                if selectedMode == .autoWithPref {
                    DetailRow(
                        label: "Intervallo",
                        value: timeString(fromSeconds: selectedInterval ?? campaign.idealSubmissionInterval)
                    )
                }
                if selectedMode == .automatic {
                    DetailRow(
                        label: "Intervallo del server",
                        value: timeString(fromSeconds: campaign.idealSubmissionInterval)
                    )
                }
                DetailRow(label: "Invii per completare", value: "\(campaign.submissionRequired)")
                DetailRow(label: "Punti", value: "\(campaign.points)")
            }
            if selectedMode != .manual {
                Section {
                    Label(
                        "L'app userà la tua posizione per inviare i dati. Assicurati di aver concesso il permesso di localizzazione.",
                        systemImage: "location.circle"
                    )
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                }
            }
            Section {
                Button {
                    confirm()
                } label: {
                    Text("Conferma adesione")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .listRowBackground(Color.clear)
            }
        }
    }

    private var successStep: some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 64))
                .foregroundStyle(.green)
            Text("Adesione completata!")
                .font(.title3.bold())
            Text("Ora partecipi alla campagna \"\(campaign.title)\". La trovi in \"Le mie campagne\".")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Fatto") {
                dismiss()
                onJoined()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }

    private func confirm() {
        guard let mode = selectedMode else { return }
        Task { @MainActor in
            let joined = await viewModel.applyToCampaign(
                campaign,
                mode: mode,
                interval: selectedInterval
            )
            if joined {
                step = .success
            } else {
                dismiss()
            }
        }
    }
}
