import SwiftUI
import ComposeApp

/// A book cover, with the title standing in while it loads or when there's none.
struct CoverImage: View {
    let nomination: Nomination
    let url: String?
    let width: CGFloat
    let height: CGFloat

    var body: some View {
        ZStack {
            Color(.secondarySystemFill)
            Text(nomination.title)
                .font(.bodyFont(.caption2))
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(6)
            if let url, let imageURL = URL(string: url) {
                AsyncImage(url: imageURL) { phase in
                    if let image = phase.image {
                        // Only described once it's on screen, so UI tests can wait for real covers.
                        image
                            .resizable()
                            .scaledToFill()
                            .accessibilityLabel("Cover of \(nomination.title)")
                    }
                }
            }
        }
        .frame(width: width, height: height)
        .clipShape(RoundedRectangle(cornerRadius: 6))
    }
}
