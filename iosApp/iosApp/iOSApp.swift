import SwiftUI

@main
struct iOSApp: App {
    init() {
        // UI tests launch with -resetState so every run starts from the same reading list and settings.
        if CommandLine.arguments.contains("-resetState"), let bundleId = Bundle.main.bundleIdentifier {
            UserDefaults.standard.removePersistentDomain(forName: bundleId)
        }
        BrandFont.register()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
