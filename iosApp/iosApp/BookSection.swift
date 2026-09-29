import SwiftUI

struct BookSection<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.headingFont(.title3, medium: true))
                .foregroundStyle(Color.brand)
            content
        }
    }
}
