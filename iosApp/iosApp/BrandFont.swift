import CoreText
import SwiftUI
import UIKit

/// The design seed's fonts, shared with Android: heading_*.ttf and body_*.ttf from composeApp's compose resources,
/// which the Kotlin framework build already copies into the app (with the fonts' licences, and awards.json).
/// Names are read from the files, so a new seed needs no code change.
enum BrandFont {
    private static let fontDirectory = "compose-resources/composeResources/com.theseuntaylor.bookawards.resources/font"
    private static var postScriptNames: [String: String] = [:]

    /// Registers the bundled fonts and styles the navigation bar titles. Call once at launch.
    static func register() {
        let urls = Bundle.main.urls(forResourcesWithExtension: "ttf", subdirectory: fontDirectory) ?? []
        assert(!urls.isEmpty, "No fonts at \(fontDirectory); has the Compose resources layout changed?")
        for url in urls {
            guard let provider = CGDataProvider(url: url as CFURL),
                  let font = CGFont(provider),
                  let name = font.postScriptName as String? else { continue }
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
            postScriptNames[url.deletingPathExtension().lastPathComponent] = name
        }
        styleNavigationBars()
    }

    static func uiFont(_ role: String, medium: Bool, style: UIFont.TextStyle) -> UIFont? {
        guard let name = postScriptNames["\(role)_\(medium ? "medium" : "regular")"],
              let font = UIFont(name: name, size: UIFont.preferredFont(forTextStyle: style).pointSize) else { return nil }
        return UIFontMetrics(forTextStyle: style).scaledFont(for: font)
    }

    static func name(_ role: String, medium: Bool) -> String? {
        postScriptNames["\(role)_\(medium ? "medium" : "regular")"]
    }

    private static func styleNavigationBars() {
        let appearance = UINavigationBar.appearance()
        if let large = uiFont("heading", medium: false, style: .largeTitle) {
            appearance.largeTitleTextAttributes = [.font: large]
        }
        if let inline = uiFont("heading", medium: true, style: .headline) {
            appearance.titleTextAttributes = [.font: inline]
        }
    }
}

extension Font {
    /// Android's display, headline and title roles.
    static func headingFont(_ style: Font.TextStyle, medium: Bool = false) -> Font {
        brand("heading", style, medium)
    }

    /// Android's body and label roles.
    static func bodyFont(_ style: Font.TextStyle, medium: Bool = false) -> Font {
        brand("body", style, medium)
    }

    private static func brand(_ role: String, _ style: Font.TextStyle, _ medium: Bool) -> Font {
        guard let name = BrandFont.name(role, medium: medium) else {
            return .system(style, weight: medium ? .medium : .regular)
        }
        // relativeTo keeps Dynamic Type scaling.
        return .custom(name, size: UIFont.preferredFont(forTextStyle: style.uiTextStyle).pointSize, relativeTo: style)
    }
}

private extension Font.TextStyle {
    var uiTextStyle: UIFont.TextStyle {
        switch self {
        case .largeTitle: .largeTitle
        case .title: .title1
        case .title2: .title2
        case .title3: .title3
        case .headline: .headline
        case .subheadline: .subheadline
        case .callout: .callout
        case .footnote: .footnote
        case .caption: .caption1
        case .caption2: .caption2
        default: .body
        }
    }
}
