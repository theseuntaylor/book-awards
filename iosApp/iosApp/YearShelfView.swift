import SwiftUI
import ComposeApp

/// One year of nominations as a sideways-scrolling row of covers.
struct YearShelfView: View {
    @EnvironmentObject private var model: AppModel
    let group: YearGroup

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline) {
                Text(String(group.year))
                    .font(.headingFont(.title2))
                    .foregroundStyle(Color.brand)
                Spacer()
                Text(group.nominations.count == 1 ? "1 book" : "\(group.nominations.count) books")
                    .font(.bodyFont(.subheadline))
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal)

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: 12) {
                    ForEach(group.nominations, id: \.self) { nomination in
                        NavigationLink(value: nomination) {
                            CoverCard(nomination: nomination, readingStatus: model.readingStatus(of: nomination))
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal)
            }
        }
    }
}
