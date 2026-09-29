import SwiftUI
import ComposeApp

struct CoverCard: View {
    let nomination: Nomination
    let readingStatus: ReadingStatus?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            CoverImage(nomination: nomination, url: nomination.coverThumbnailUrl, width: 104, height: 156)
                .overlay(alignment: .bottomLeading) {
                    if let readingStatus {
                        ReadingStatusBadge(status: readingStatus)
                            .padding(4)
                    }
                }
            Text(nomination.title)
                .font(.bodyFont(.caption))
                .lineLimit(2)
            if nomination.status == .winner {
                WinnerChip()
            } else {
                Text(nomination.status.label)
                    .font(.bodyFont(.caption2))
                    .foregroundStyle(.secondary)
            }
            Text(nomination.award.shortName)
                .font(.bodyFont(.caption2))
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .frame(width: 104, alignment: .leading)
    }
}
