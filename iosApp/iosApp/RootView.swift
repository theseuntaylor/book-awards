import SwiftUI

enum AppTab: Hashable {
    case home, library
}

struct RootView: View {
    @StateObject private var model = AppModel()
    @State private var tab = AppTab.home

    var body: some View {
        TabView(selection: $tab) {
            HomeView(openLibrary: { tab = .library })
                .tabItem { Label("Home", systemImage: "house") }
                .tag(AppTab.home)
            LibraryView()
                .tabItem { Label("Library", systemImage: "books.vertical") }
                .tag(AppTab.library)
        }
        .environmentObject(model)
        .tint(.brand)
        .font(.bodyFont(.body))
        .overlay(alignment: .bottom) {
            ToastView(message: model.toast)
                .padding(.bottom, 64)
        }
        .task { await model.start() }
    }
}
