import SwiftUI
import ComposeApp

struct ReadingStatusBadge: View {
    let status: ReadingStatus

    var body: some View {
        LabelChip(text: status.label, container: .secondaryContainer, content: .onSecondaryContainer)
    }
}
