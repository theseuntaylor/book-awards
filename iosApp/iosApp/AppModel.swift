import SwiftUI
import ComposeApp

/// SwiftUI state over the shared Kotlin data layer (`BookAwardsModel`).
@MainActor
final class AppModel: ObservableObject {
    static let allAwards: [Award] = [.booker, .pulitzer, .nationalBookAward]
    static let readingStatuses: [ReadingStatus] = [.wantToRead, .reading, .read]

    @Published private(set) var nominations: [Nomination]?
    @Published private(set) var readingStatuses: [String: ReadingStatus] = [:]
    @Published var selectedAwards = Set(AppModel.allAwards)
    @Published private(set) var remindersOn: Bool
    @Published private(set) var toast: String?

    private let shared = BookAwardsModel()
    private var subscriptions: [Subscription] = []

    init() {
        remindersOn = shared.remindersEnabled
        subscriptions.append(shared.observeNominations { [weak self] in self?.nominations = $0 })
        subscriptions.append(shared.observeReadingStatuses { [weak self] in self?.readingStatuses = $0 })
    }

    deinit {
        subscriptions.forEach { $0.cancel() }
    }

    var homeYears: [YearGroup] {
        guard let nominations else { return [] }
        return shared.homeYears(all: nominations, awards: selectedAwards)
    }

    var libraryShelves: [YearGroup] {
        guard let nominations else { return [] }
        return shared.libraryShelves(all: nominations, awards: selectedAwards)
    }

    func nominations(for book: Nomination) -> [Nomination] {
        shared.nominationsForBook(all: nominations ?? [], nomination: book)
    }

    func otherBooks(by book: Nomination) -> [Nomination] {
        shared.otherBooksByAuthor(all: nominations ?? [], nomination: book)
    }

    func readingStatus(of book: Nomination) -> ReadingStatus? {
        readingStatuses[book.bookKey]
    }

    func setReadingStatus(_ status: ReadingStatus?, for book: Nomination) {
        shared.setReadingStatus(nomination: book, status: status)
    }

    func start() async {
        if let message = try? await shared.start() { show(message) }
    }

    func refresh() async {
        if let message = try? await shared.refresh() { show(message) }
    }

    func toggleReminders() async {
        guard let message = try? await shared.toggleReminders() else { return }
        remindersOn = shared.remindersEnabled
        show(message)
    }

    func lookUp(_ book: Nomination) async -> BookInfo? {
        (try? await shared.lookUpBook(nomination: book)) ?? nil
    }

    private func show(_ message: String) {
        withAnimation { toast = message }
        Task {
            try? await Task.sleep(for: .seconds(3))
            if toast == message { withAnimation { toast = nil } }
        }
    }
}
