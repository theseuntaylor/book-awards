import SwiftUI
import ComposeApp

/// Library: every year before Home's, one shelf of covers per year, newest first.
struct LibraryView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        NavigationStack {
            Group {
                if model.nominations == nil {
                    ProgressView()
                } else if model.libraryShelves.isEmpty {
                    Text("No earlier years for the selected awards")
                        .foregroundStyle(.secondary)
                } else {
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 24) {
                            ForEach(model.libraryShelves, id: \.year) { group in
                                YearShelfView(group: group)
                            }
                        }
                        .padding(.vertical)
                    }
                }
            }
            .navigationTitle("Library")
            .toolbar {
                ToolbarItem(placement: .primaryAction) { AwardFilterMenu() }
            }
            .navigationDestination(for: Nomination.self) { BookView(nomination: $0) }
        }
    }
}
