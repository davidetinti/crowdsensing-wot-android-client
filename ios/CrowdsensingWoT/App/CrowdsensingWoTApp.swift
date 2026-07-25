import SwiftUI

@main
struct CrowdsensingWoTApp: App {
    @StateObject private var viewModel = AppViewModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(viewModel)
                .task {
                    await viewModel.onAppStart()
                }
        }
    }
}
