import SwiftUI
import ComposeApp

struct NominationRow: View {
    let nomination: Nomination
    let readingStatus: ReadingStatus?

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 2) {
                Text(nomination.award.displayName)
                    .font(.bodyFont(.caption, medium: true))
                    .foregroundStyle(.secondary)
                Text(nomination.title)
                HStack(spacing: 6) {
                    Text(nomination.author)
                        .font(.bodyFont(.subheadline))
                        .foregroundStyle(.secondary)
                    if let readingStatus {
                        ReadingStatusBadge(status: readingStatus)
                    }
                }
            }
            Spacer()
            if nomination.status == .winner {
                WinnerChip()
            } else {
                Text(nomination.status.label)
                    .font(.bodyFont(.caption, medium: true))
                    .foregroundStyle(.secondary)
            }
        }
    }
}
