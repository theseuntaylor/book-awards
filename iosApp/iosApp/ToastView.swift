import SwiftUI

/// A brief message over the tab bar; iOS's stand-in for Android's snackbar.
struct ToastView: View {
    let message: String?

    var body: some View {
        if let message {
            Text(message)
                .font(.bodyFont(.subheadline))
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(.regularMaterial, in: Capsule())
                .shadow(radius: 4, y: 2)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .accessibilityIdentifier("toast")
        }
    }
}
