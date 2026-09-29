import SwiftUI
import ComposeApp

/// Which awards to show, as a toolbar menu of toggles; the icon fills when some are hidden.
struct AwardFilterMenu: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        Menu {
            ForEach(AppModel.allAwards, id: \.self) { award in
                Toggle(award.displayName, isOn: shown(award))
            }
        } label: {
            let filtered = model.selectedAwards.count < AppModel.allAwards.count
            Image(systemName: filtered ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
        }
        .accessibilityLabel("Filter awards")
    }

    private func shown(_ award: Award) -> Binding<Bool> {
        Binding(
            get: { model.selectedAwards.contains(award) },
            set: { isOn in
                if isOn { model.selectedAwards.insert(award) } else { model.selectedAwards.remove(award) }
            }
        )
    }
}
