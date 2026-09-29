import SwiftUI
import ComposeApp

struct BookView: View {
    @EnvironmentObject private var model: AppModel
    let nomination: Nomination
    @State private var info: BookInfo?
    @State private var lookedUp = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                BookHeaderView(nomination: nomination, info: info)

                ReadingStatusMenu(
                    status: model.readingStatus(of: nomination),
                    onChange: { model.setReadingStatus($0, for: nomination) }
                )

                BookSection(title: "Nominations") {
                    ForEach(model.nominations(for: nomination), id: \.self) { entry in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(entry.award.displayName)
                                .font(.bodyFont(.caption, medium: true))
                                .foregroundStyle(.secondary)
                            Text("\(String(entry.year)) · \(entry.status.label)")
                        }
                    }
                }

                AboutSection(info: info, lookedUp: lookedUp)

                let others = model.otherBooks(by: nomination)
                if !others.isEmpty {
                    BookSection(title: "Also by \(nomination.author)") {
                        ForEach(others, id: \.self) { other in
                            NavigationLink(value: other) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(other.title)
                                    Text("\(other.award.displayName) \(other.status.label.lowercased()), \(String(other.year))")
                                        .font(.bodyFont(.caption))
                                        .foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
        }
        .navigationBarTitleDisplayMode(.inline)
        .task(id: nomination) {
            lookedUp = false
            info = await model.lookUp(nomination)
            lookedUp = true
        }
    }
}
