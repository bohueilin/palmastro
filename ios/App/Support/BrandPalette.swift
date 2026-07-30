import SwiftUI

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
}
