import XCTest

/// E2E: Home shows only recent years and links to the Library; Library shelves show covers, scroll sideways
/// and open books; a reading status set on a book shows back on its shelf; the tabs switch.
/// Screenshots are attached to the test result (e2e/ios.sh exports them).
final class BookAwardsUITests: XCTestCase {
    private let app = XCUIApplication()
    private let firstHomeYear = Calendar.current.component(.year, from: Date()) - 2

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = ["-resetState"]
        app.launch()
    }

    func testHomeLibraryAndBook() {
        // 1. Home: recent years only, ending with a link to the Library.
        XCTAssertTrue(app.navigationBars["Book Awards"].waitForExistence(timeout: 20))
        XCTAssertTrue(app.staticTexts[String(firstHomeYear + 2)].waitForExistence(timeout: 10), "no current-year section on Home")
        screenshot("1-home")

        let libraryLink = app.buttons["Earlier years are in the Library"]
        for _ in 0..<60 where !libraryLink.isHittable { app.swipeUp(velocity: .fast) }
        XCTAssertTrue(libraryLink.isHittable, "Home never reached the Library link")
        XCTAssertFalse(app.staticTexts[String(firstHomeYear - 1)].exists, "Home shows \(firstHomeYear - 1)")
        screenshot("2-home-end")

        // 2. The link opens the Library, starting with the year before Home's window, with covers loading.
        libraryLink.tap()
        XCTAssertTrue(app.navigationBars["Library"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts[String(firstHomeYear - 1)].waitForExistence(timeout: 5))
        let loadedCovers = app.images.matching(NSPredicate(format: "label BEGINSWITH 'Cover of'"))
        XCTAssertTrue(loadedCovers.firstMatch.waitForExistence(timeout: 30), "no covers loaded")
        screenshot("3-library")

        // 3. A shelf scrolls sideways: its first cover moves out of view.
        let firstCover = loadedCovers.firstMatch
        let firstCoverLabel = firstCover.label
        firstCover.swipeLeft()
        XCTAssertFalse(app.images[firstCoverLabel].isHittable, "the shelf didn't scroll sideways")
        screenshot("4-shelf-scrolled")

        // 4. A cover opens its book.
        guard let visibleCover = loadedCovers.allElementsBoundByIndex.first(where: { $0.isHittable }) else {
            return XCTFail("no visible cover to tap")
        }
        let title = String(visibleCover.label.dropFirst("Cover of ".count))
        visibleCover.tap()
        XCTAssertTrue(app.staticTexts["Nominations"].waitForExistence(timeout: 5), "tapping a cover didn't open the book")
        screenshot("5-book")

        // 5. Mark it "Want to read"; the menu button then shows that status.
        app.buttons["reading-status"].tap()
        app.buttons["Want to read"].tap()
        XCTAssertTrue(app.buttons["reading-status"].label.contains("Want to read"))
        screenshot("6-want-to-read")

        // 6. Back to the Library, where the book's card now carries the status.
        app.navigationBars.buttons.firstMatch.tap()
        XCTAssertTrue(app.navigationBars["Library"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Want to read"].waitForExistence(timeout: 5), "no Want to read badge for \(title)")
        screenshot("7-library-with-status")

        // 7. The Home tab goes back to Home.
        app.tabBars.buttons["Home"].tap()
        XCTAssertTrue(app.navigationBars["Book Awards"].waitForExistence(timeout: 5))
    }

    private func screenshot(_ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
