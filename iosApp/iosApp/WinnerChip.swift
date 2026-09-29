import SwiftUI
import ComposeApp

/// Primary-tinted so a win stands apart from reading-status chips, which use the secondary colours.
struct WinnerChip: View {
    var body: some View {
        LabelChip(text: NominationStatus.winner.label, container: .brandContainer, content: .onBrandContainer)
    }
}
