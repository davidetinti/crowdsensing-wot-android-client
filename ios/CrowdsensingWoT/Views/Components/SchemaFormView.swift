import SwiftUI

/// Form generato dinamicamente dal `data_schema` di una campagna:
/// permette all'utente di compilare e inviare un tipo di dato
/// personalizzato definito lato server, senza modifiche all'app.
struct SchemaFormView: View {
    @Environment(\.dismiss) private var dismiss

    let campaignTitle: String
    let schema: DataSchema
    let onSubmit: ([String: Any]) -> Void

    @State private var numberInputs: [String: String] = [:]
    @State private var textInputs: [String: String] = [:]
    @State private var boolInputs: [String: Bool] = [:]
    @State private var enumInputs: [String: String] = [:]
    @State private var validationMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("Compila i dati richiesti dalla campagna \"\(campaignTitle)\".")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                ForEach(schema.fields) { field in
                    Section {
                        fieldInput(for: field)
                        if let details = field.details {
                            Text(details)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    } header: {
                        Text(headerText(for: field))
                    }
                }

                if let validationMessage {
                    Section {
                        Text(validationMessage)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }

                Section {
                    Button {
                        submit()
                    } label: {
                        Label("Invia i dati", systemImage: "paperplane.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .listRowBackground(Color.clear)
                }
            }
            .navigationTitle("Invia dati")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annulla") { dismiss() }
                }
            }
        }
    }

    private func headerText(for field: DataSchema.Field) -> String {
        var text = field.label
        if let unit = field.unit { text += " (\(unit))" }
        if field.isRequired { text += " *" }
        return text
    }

    @ViewBuilder
    private func fieldInput(for field: DataSchema.Field) -> some View {
        if let allowed = field.allowedValues {
            Picker(field.label, selection: binding(for: field, in: $enumInputs, default: "")) {
                Text("—").tag("")
                ForEach(allowed, id: \.self) { value in
                    Text(value).tag(value)
                }
            }
        } else {
            switch field.type {
            case .number, .integer:
                TextField(
                    rangePlaceholder(for: field),
                    text: binding(for: field, in: $numberInputs, default: "")
                )
                .keyboardType(field.type == .integer ? .numberPad : .decimalPad)
            case .string:
                TextField("Testo", text: binding(for: field, in: $textInputs, default: ""))
            case .boolean:
                Toggle(field.label, isOn: binding(for: field, in: $boolInputs, default: false))
            }
        }
    }

    private func rangePlaceholder(for field: DataSchema.Field) -> String {
        switch (field.minimum, field.maximum) {
        case let (minimum?, maximum?): return "Valore tra \(bound(minimum)) e \(bound(maximum))"
        case let (minimum?, nil): return "Valore ≥ \(bound(minimum))"
        case let (nil, maximum?): return "Valore ≤ \(bound(maximum))"
        default: return "Valore"
        }
    }

    private func bound(_ value: Double) -> String {
        if value == value.rounded(), abs(value) < 1e15 {
            return String(Int(value))
        }
        return String(value)
    }

    private func binding<V>(
        for field: DataSchema.Field,
        in storage: Binding<[String: V]>,
        default defaultValue: V
    ) -> Binding<V> {
        Binding(
            get: { storage.wrappedValue[field.name] ?? defaultValue },
            set: { storage.wrappedValue[field.name] = $0 }
        )
    }

    private func submit() {
        var values: [String: Any] = [:]
        for field in schema.fields {
            if let allowed = field.allowedValues {
                if let selected = enumInputs[field.name], !selected.isEmpty {
                    // Riconverte il valore selezionato al tipo dichiarato.
                    switch field.type {
                    case .number: values[field.name] = Double(selected) ?? selected
                    case .integer: values[field.name] = Int(selected) ?? selected
                    default: values[field.name] = selected
                    }
                    _ = allowed
                }
                continue
            }
            switch field.type {
            case .number:
                if let raw = numberInputs[field.name]?.replacingOccurrences(of: ",", with: "."),
                   !raw.isEmpty {
                    if let number = Double(raw) {
                        values[field.name] = number
                    } else {
                        validationMessage = "Il campo \"\(field.label)\" deve essere un numero"
                        return
                    }
                }
            case .integer:
                if let raw = numberInputs[field.name], !raw.isEmpty {
                    if let number = Int(raw) {
                        values[field.name] = number
                    } else {
                        validationMessage = "Il campo \"\(field.label)\" deve essere un numero intero"
                        return
                    }
                }
            case .string:
                if let text = textInputs[field.name], !text.isEmpty {
                    values[field.name] = text
                }
            case .boolean:
                values[field.name] = boolInputs[field.name] ?? false
            }
        }
        if let error = schema.validate(values) {
            validationMessage = error
            return
        }
        dismiss()
        onSubmit(values)
    }
}
