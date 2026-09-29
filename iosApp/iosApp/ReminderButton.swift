import SwiftUI

struct ReminderButton: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        Button {
            Task { await model.toggleReminders() }
        } label: {
            Image(systemName: model.remindersOn ? "bell.fill" : "bell")
        }
        .accessibilityLabel(model.remindersOn ? "Turn off announcement reminders" : "Remind me on announcement days")
    }
}
