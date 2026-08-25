import SwiftUI
import UIKit

/// PalmAstro brand palette (PRD §37): royal purple as mystical intelligence,
/// calm teal as clarity, night-sky darks for the premium dark experience.
/// No aggressive red anywhere in the brand layer — true errors use the
/// platform's semantic colors.
enum BrandPalette {

    /// Royal purple #6B46C1.
    static let royalPurple = Color(red: 0x6B / 255.0, green: 0x46 / 255.0, blue: 0xC1 / 255.0)

    /// Calm teal #4FD1C5.
    static let calmTeal = Color(red: 0x4F / 255.0, green: 0xD1 / 255.0, blue: 0xC5 / 255.0)

    /// Night-sky card base.
    static let nightSky = Color(red: 0x14 / 255.0, green: 0x0F / 255.0, blue: 0x2E / 255.0)

    /// Slightly lifted night-sky tone for subtle vertical depth.
    static let nightSkyHigh = Color(red: 0x23 / 255.0, green: 0x1A / 255.0, blue: 0x4A / 255.0)

    /// Starlight white with a lavender cast, for constellation nodes.
    static let starlight = Color(red: 0xED / 255.0, green: 0xE9 / 255.0, blue: 0xFE / 255.0)

    // MARK: - Grade + delta semantics

    // Mirrors PalmAstroExtendedColors in the Android theme (Theme.kt), token for
    // token. Deliberately desaturated versus the iOS system palette: a
    // "Watchout" month is a coaching signal, not an alarm (PRD §12.3, §37).
    // Each pair is declared light/dark because a static Color literal would not
    // follow the appearance, and every grade pairs with an `on*` tone that keeps
    // >= 4.5:1 on a SOLID chip.

    /// Grade "Growing" — also the positive delta tone. #2E7D32 / #81C784.
    static let gradeGrowing = adaptive(light: 0x2E7D32, dark: 0x81C784)
    /// Text tone paired with ``gradeGrowing`` on a solid chip.
    static let onGradeGrowing = adaptive(light: 0xFFFFFF, dark: 0x0A2E10)

    /// Grade "Stable". #00695C / #80CBC4.
    static let gradeStable = adaptive(light: 0x00695C, dark: 0x80CBC4)
    /// Text tone paired with ``gradeStable`` on a solid chip.
    static let onGradeStable = adaptive(light: 0xFFFFFF, dark: 0x00201C)

    /// Grade "Building" — the brand's indigo. #5E35B1 / #B39DDB.
    static let gradeBuilding = adaptive(light: 0x5E35B1, dark: 0xB39DDB)
    /// Text tone paired with ``gradeBuilding`` on a solid chip.
    static let onGradeBuilding = adaptive(light: 0xFFFFFF, dark: 0x21005E)

    /// Grade "Watchout" — also the negative delta tone. #B35A00 / #FFB77C.
    static let gradeWatchOut = adaptive(light: 0xB35A00, dark: 0xFFB77C)
    /// Text tone paired with ``gradeWatchOut`` on a solid chip.
    static let onGradeWatchOut = adaptive(light: 0xFFFFFF, dark: 0x3A2000)

    /// Month-over-month rise.
    static let deltaPositive = gradeGrowing
    /// Month-over-month fall.
    static let deltaNegative = gradeWatchOut
    /// Unchanged month.
    static let deltaNeutral = adaptive(light: 0x49454E, dark: 0xCAC4D0)

    /// Resolves per appearance so the tokens track light/dark like the Android
    /// scheme does; `Color(red:green:blue:)` alone is fixed at declaration.
    private static func adaptive(light: Int, dark: Int) -> Color {
        Color(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? uiColor(dark) : uiColor(light)
        })
    }

    private static func uiColor(_ rgb: Int) -> UIColor {
        UIColor(
            red: CGFloat((rgb >> 16) & 0xFF) / 255.0,
            green: CGFloat((rgb >> 8) & 0xFF) / 255.0,
            blue: CGFloat(rgb & 0xFF) / 255.0,
            alpha: 1
        )
    }
}
