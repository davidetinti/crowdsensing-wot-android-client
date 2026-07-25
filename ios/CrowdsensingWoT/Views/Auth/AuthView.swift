import SwiftUI

/// Schermata di accesso/registrazione (equivalente di `AuthFragment`).
struct AuthView: View {
    @EnvironmentObject private var viewModel: AppViewModel

    @State private var email = SettingsStore.shared.savedEmail ?? ""
    @State private var password = SettingsStore.shared.savedPassword ?? ""
    @State private var rememberMe = SettingsStore.shared.rememberMe
    @State private var isLogin = true
    @State private var emailError = false
    @State private var passwordError = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    VStack(spacing: 8) {
                        Image(systemName: "sensor.tag.radiowaves.forward.fill")
                            .font(.system(size: 48))
                            .foregroundStyle(.teal)
                        Text("Crowdsensing WoT")
                            .font(.title2.bold())
                        Text(isLogin ? "Accedi al tuo account" : "Crea un nuovo account")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
                }

                Section {
                    TextField("Email", text: $email)
                        .keyboardType(.emailAddress)
                        .textContentType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    if emailError {
                        Text("Inserisci un indirizzo email valido")
                            .font(.caption)
                            .foregroundStyle(.red)
                    }
                    SecureField("Password", text: $password)
                        .textContentType(isLogin ? .password : .newPassword)
                    if passwordError {
                        Text("La password deve avere almeno 6 caratteri")
                            .font(.caption)
                            .foregroundStyle(.red)
                    }
                    Toggle("Ricordami", isOn: $rememberMe)
                }

                Section {
                    Button(isLogin ? "Accedi" : "Registrati") {
                        submit()
                    }
                    .frame(maxWidth: .infinity)
                    .buttonStyle(.borderedProminent)
                    .listRowBackground(Color.clear)

                    Button(isLogin ? "Non hai un account? Registrati" : "Hai già un account? Accedi") {
                        isLogin.toggle()
                        emailError = false
                        passwordError = false
                    }
                    .font(.footnote)
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
                }
            }
            .navigationTitle(isLogin ? "Accesso" : "Registrazione")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    /// Validazione equivalente a `AuthViewModel.validateData` su Android.
    private func submit() {
        let emailValid = isValidEmail(email)
        let passwordValid = password.trimmingCharacters(in: .whitespaces).count > 5
        emailError = !emailValid
        passwordError = !passwordValid
        guard emailValid, passwordValid else { return }
        let credentials = (email: email, password: password, isLogin: isLogin, remember: rememberMe)
        Task {
            await viewModel.authenticate(
                email: credentials.email,
                password: credentials.password,
                isLogin: credentials.isLogin,
                rememberMe: credentials.remember
            )
        }
    }

    private func isValidEmail(_ value: String) -> Bool {
        let pattern = #"^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"#
        return value.range(of: pattern, options: .regularExpression) != nil
    }
}
