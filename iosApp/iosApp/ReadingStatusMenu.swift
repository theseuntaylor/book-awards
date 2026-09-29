import SwiftUI
import ComposeApp

/// Reading status as a menu button, like Apple Books' "Want to Read".
struct ReadingStatusMenu: View {
    let status: ReadingStatus?
    let onChange: (ReadingStatus?) -> Void

    var body: some View {
        Menu {
            ForEach(AppModel.readingStatuses, id: \.self) { option in
                Button {
                    onChange(option)
                } label: {
                    if option == status {
                        Label(option.label, systemImage: "checkmark")
                    } else {
                        Text(option.label)
                    }
                }
            }
            if status != nil {
                Button("Remove from My Books", role: .destructive) { onChange(nil) }
            }
        } label: {
            Label(status?.label ?? "Add to My Books", systemImage: status == nil ? "plus" : "checkmark")
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
        .accessibilityIdentifier("reading-status")
    }
}
