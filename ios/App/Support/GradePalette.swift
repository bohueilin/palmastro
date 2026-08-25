import SwiftUI

/// Grade presentation shared by Results / Detail / Explainability — the iOS
/// mirror of app/src/main/kotlin/com/palmastro/app/ui/results/GradePalette.kt.
/// The engine emits raw tokens ("Growing" / "Stable" / "Building" / "Watchout")
/// from the ruleset; nothing in the chrome may show them untranslated, and
/// color is never the only indicator — the grade name always renders beside it.

/// Localized grade name. Mirrors `gradeNameLocalized` on Android.
func gradeDisplayName(_ grade: String) -> String {
    switch grade {
    case "Growing": return NSLocalizedString("grade_growing", comment: "Grade name")
    case "Stable": return NSLocalizedString("grade_stable", comment: "Grade name")
    case "Building": return NSLocalizedString("grade_building", comment: "Grade name")
    case "Watchout": return NSLocalizedString("grade_watchout", comment: "Grade name")
    default: return grade
    }
}

/// Brand color for a grade token. Mirrors `gradeColor` on Android.
func gradeColor(_ grade: String) -> Color {
    switch grade {
    case "Growing": return BrandPalette.gradeGrowing
    case "Stable": return BrandPalette.gradeStable
    case "Building": return BrandPalette.gradeBuilding
    case "Watchout": return BrandPalette.gradeWatchOut
    default: return BrandPalette.royalPurple
    }
}

/// Text tone paired with ``gradeColor(_:)`` for SOLID grade chips (each pair
/// keeps >= 4.5:1 by construction). A low-alpha tinted chip cannot carry the
/// raw grade color as small text; use a solid chip with this pair instead.
func onGradeColor(_ grade: String) -> Color {
    switch grade {
    case "Growing": return BrandPalette.onGradeGrowing
    case "Stable": return BrandPalette.onGradeStable
    case "Building": return BrandPalette.onGradeBuilding
    case "Watchout": return BrandPalette.onGradeWatchOut
    default: return .white
    }
}
