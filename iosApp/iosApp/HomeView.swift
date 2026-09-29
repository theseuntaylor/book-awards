import SwiftUI
import ComposeApp

/// Home: the most recent award years as a list with year headers. Older years live in the Library tab.
struct HomeView: View {
    @EnvironmentObject private var model: AppModel
    let openLibrary: () -> Void

    var body: some View {
        NavigationStack {
            Group {
                if model.nominations == nil {
                    ProgressView()
                } else {
                    List {
                        ForEach(model.homeYears, id: \.year) { group in
                            Section {
                                ForEach(group.nominations, id: \.self) { nomination in
                                    NavigationLink(value: nomination) {
                                        NominationRow(nomination: nomination, readingStatus: model.readingStatus(of: nomination))
                                    }
                                }
                            } header: {
                                Text(String(group.year))
                                    .font(.headingFont(.title3))
                                    .foregroundStyle(Color.brand)
                            }
                        }
                        Section {
                            Button("Earlier years are in the Library", action: openLibrary)
                        }
                    }
                    .listStyle(.plain)
                    .refreshable { await model.refresh() }
                }
            }
            .navigationTitle("Book Awards")
            .toolbar {
                ToolbarItemGroup(placement: .primaryAction) {
                    AwardFilterMenu()
                    ReminderButton()
                }
            }
            .navigationDestination(for: Nomination.self) { BookView(nomination: $0) }
        }
    }
}
