import CoreContracts

/// Tropical zodiac tables shared by sun (calendar boundaries, mirroring the
/// Android ZodiacCalculator) and moon/ascendant (ecliptic longitude / 30 deg).
public struct ZodiacSign: Equatable, Sendable {
    public let name: String       // e.g. "ARIES"
    public let element: String    // FIRE | EARTH | AIR | WATER
    public let modality: String   // CARDINAL | FIXED | MUTABLE

    public init(name: String, element: String, modality: String) {
        self.name = name
        self.element = element
        self.modality = modality
    }
}

public enum Zodiac {

    /// Order matches ecliptic longitude: index = floor(longitude / 30).
    public static let signs: [ZodiacSign] = [
        ZodiacSign(name: "ARIES", element: "FIRE", modality: "CARDINAL"),
        ZodiacSign(name: "TAURUS", element: "EARTH", modality: "FIXED"),
        ZodiacSign(name: "GEMINI", element: "AIR", modality: "MUTABLE"),
        ZodiacSign(name: "CANCER", element: "WATER", modality: "CARDINAL"),
        ZodiacSign(name: "LEO", element: "FIRE", modality: "FIXED"),
        ZodiacSign(name: "VIRGO", element: "EARTH", modality: "MUTABLE"),
        ZodiacSign(name: "LIBRA", element: "AIR", modality: "CARDINAL"),
        ZodiacSign(name: "SCORPIO", element: "WATER", modality: "FIXED"),
        ZodiacSign(name: "SAGITTARIUS", element: "FIRE", modality: "MUTABLE"),
        ZodiacSign(name: "CAPRICORN", element: "EARTH", modality: "CARDINAL"),
        ZodiacSign(name: "AQUARIUS", element: "AIR", modality: "FIXED"),
        ZodiacSign(name: "PISCES", element: "WATER", modality: "MUTABLE"),
    ]

    /// (startMonth, startDay, endMonth, endDay) inclusive calendar boundaries.
    /// Same table as the Android ZodiacCalculator.
    private static let sunBoundaries: [(Int, Int, Int, Int, ZodiacSign)] = [
        (3, 21, 4, 19, signs[0]),   // Aries
        (4, 20, 5, 20, signs[1]),   // Taurus
        (5, 21, 6, 20, signs[2]),   // Gemini
        (6, 21, 7, 22, signs[3]),   // Cancer
        (7, 23, 8, 22, signs[4]),   // Leo
        (8, 23, 9, 22, signs[5]),   // Virgo
        (9, 23, 10, 22, signs[6]),  // Libra
        (10, 23, 11, 21, signs[7]), // Scorpio
        (11, 22, 12, 21, signs[8]), // Sagittarius
        (12, 22, 1, 19, signs[9]),  // Capricorn (wraps year end)
        (1, 20, 2, 18, signs[10]),  // Aquarius
        (2, 19, 3, 20, signs[11]),  // Pisces
    ]

    public static func sunSign(for date: CivilDate) -> ZodiacSign {
        let md = date.month * 100 + date.day
        for (sm, sd, em, ed, sign) in sunBoundaries {
            let start = sm * 100 + sd
            let end = em * 100 + ed
            if start <= end {
                if md >= start && md <= end { return sign }
            } else {
                if md >= start || md <= end { return sign }
            }
        }
        return signs[9] // Capricorn — unreachable, table covers the year
    }

    public static func sign(forEclipticLongitude longitude: Double) -> ZodiacSign {
        let normalized = Astronomy.normalizeDegrees(longitude)
        let index = min(Int(normalized / 30.0), 11)
        return signs[index]
    }
}
