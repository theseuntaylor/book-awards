import SwiftUI

/// A small label, e.g. a reading status or a win. The row or card it sits on handles taps.
struct LabelChip: View {
    let text: String
    let container: Color
    let content: Color

    var body: some View {
        Text(text)
            .font(.bodyFont(.caption2, medium: true))
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .foregroundStyle(content)
            .background(container, in: RoundedRectangle(cornerRadius: 6))
    }
}
