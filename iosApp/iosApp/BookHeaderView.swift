import SwiftUI
import ComposeApp

struct BookHeaderView: View {
    let nomination: Nomination
    let info: BookInfo?

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            // The stored cover shows at once; the lookup only fills in books the pipeline found no cover for.
            CoverImage(nomination: nomination, url: nomination.coverLargeUrl ?? info?.coverUrl, width: 112, height: 168)
            VStack(alignment: .leading, spacing: 4) {
                Text(nomination.title)
                    .font(.headingFont(.title2))
                Text(nomination.author)
                    .font(.headingFont(.headline, medium: true))
                    .foregroundStyle(.secondary)
                if let year = info?.firstPublished {
                    Text("First published \(String(year.intValue))")
                        .font(.bodyFont(.subheadline))
                        .foregroundStyle(.secondary)
                }
            }
        }
    }
}
