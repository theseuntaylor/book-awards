import SwiftUI
import ComposeApp

struct AboutSection: View {
    let info: BookInfo?
    let lookedUp: Bool

    var body: some View {
        BookSection(title: "About") {
            if !lookedUp {
                HStack(spacing: 8) {
                    ProgressView()
                    Text("Looking this book up on Open Library…")
                        .foregroundStyle(.secondary)
                }
            } else if let info {
                if let description = info.description_ {
                    Text(description)
                }
                if let url = URL(string: info.openLibraryUrl) {
                    Link("View on Open Library", destination: url)
                }
            } else {
                Text("Open Library doesn't have details for this book.")
                    .foregroundStyle(.secondary)
            }
        }
    }
}
