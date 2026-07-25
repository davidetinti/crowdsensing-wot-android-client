import SwiftUI

/// Impostazioni dei protocolli: URL della Thing Directory e parametri del
/// broker MQTT (equivalente di `SettingsPreferenceFragment`).
/// Le modifiche hanno effetto al successivo login.
struct SettingsView: View {
    private let settings = SettingsStore.shared

    @State private var directoryUrl = SettingsStore.shared.directoryUrl
    @State private var mqttHost = SettingsStore.shared.mqttHost
    @State private var mqttPort = String(SettingsStore.shared.mqttPort)
    @State private var mqttClientId = SettingsStore.shared.mqttClientId
    @State private var mqttUsername = SettingsStore.shared.mqttUsername
    @State private var mqttPassword = SettingsStore.shared.mqttPassword
    @State private var saved = false

    var body: some View {
        Form {
            Section("Thing Description Directory") {
                TextField("URL della directory", text: $directoryUrl)
                    .keyboardType(.URL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }

            Section("Broker MQTT") {
                TextField("Host", text: $mqttHost)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                TextField("Porta", text: $mqttPort)
                    .keyboardType(.numberPad)
                TextField("Client ID (opzionale)", text: $mqttClientId)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                TextField("Username (opzionale)", text: $mqttUsername)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                SecureField("Password (opzionale)", text: $mqttPassword)
            }

            Section {
                Button("Salva") {
                    save()
                }
                .frame(maxWidth: .infinity)
                .buttonStyle(.borderedProminent)
                .listRowBackground(Color.clear)
            } footer: {
                Text("Le modifiche vengono applicate al prossimo accesso.")
            }

            Section("Dispositivo") {
                DetailRow(label: "ID dispositivo", value: settings.deviceId)
                    .font(.caption)
            }
        }
        .navigationTitle("Impostazioni")
        .alert("Impostazioni salvate", isPresented: $saved) {
            Button("OK", role: .cancel) {}
        }
    }

    private func save() {
        settings.directoryUrl = directoryUrl.trimmingCharacters(in: .whitespaces)
        settings.mqttHost = mqttHost.trimmingCharacters(in: .whitespaces)
        if let port = UInt16(mqttPort.trimmingCharacters(in: .whitespaces)), port > 0 {
            settings.mqttPort = port
        }
        settings.mqttClientId = mqttClientId.trimmingCharacters(in: .whitespaces)
        settings.mqttUsername = mqttUsername.trimmingCharacters(in: .whitespaces)
        settings.mqttPassword = mqttPassword
        saved = true
    }
}
