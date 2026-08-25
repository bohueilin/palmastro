import Foundation

/// Deterministic low-precision astronomy per EXECUTION_SPEC:
/// - Meeus low-precision lunar ecliptic longitude (sign-accurate).
/// - Standard ascendant formula: GMST -> LST -> atan2, obliquity 23.4367 deg.
/// References: Jean Meeus, "Astronomical Algorithms" (2nd ed.), ch. 7, 12, 47.
public enum Astronomy {

    /// Mean obliquity of the ecliptic used for the ascendant (EXECUTION_SPEC).
    public static let obliquityDegrees = 23.4367

    // MARK: - Julian Day (Meeus eq. 7.1, Gregorian calendar)

    /// `hourUT` may be fractional and may fall outside 0..24; the linear form
    /// of eq. 7.1 handles day rollover naturally.
    public static func julianDay(year: Int, month: Int, day: Int, hourUT: Double) -> Double {
        var y = Double(year)
        var m = Double(month)
        if m <= 2 {
            y -= 1
            m += 12
        }
        let a = (y / 100).rounded(.down)
        let b = 2 - a + (a / 4).rounded(.down)
        let jd0 = (365.25 * (y + 4716)).rounded(.down)
            + (30.6001 * (m + 1)).rounded(.down)
            + Double(day) + b - 1524.5
        return jd0 + hourUT / 24.0
    }

    // MARK: - Greenwich Mean Sidereal Time (Meeus eq. 12.4)

    /// Returns GMST in degrees, normalized to [0, 360).
    public static func gmstDegrees(julianDay jd: Double) -> Double {
        let t = (jd - 2451545.0) / 36525.0
        let theta = 280.46061837
            + 360.98564736629 * (jd - 2451545.0)
            + 0.000387933 * t * t
            - t * t * t / 38710000.0
        return normalizeDegrees(theta)
    }

    // MARK: - Moon (Meeus ch. 47, truncated series — "low precision")

    /// Geocentric ecliptic longitude of the Moon in degrees [0, 360).
    ///
    /// Mean longitude plus the six largest periodic terms of the ELP truncated
    /// series (~0.3 deg accuracy — sign-accurate except within ~0.3 deg of a
    /// sign boundary, which the product treats as acceptable per the PRD).
    ///
    /// This mirrors `AstroMath.moonEclipticLongitudeDeg` line for line, down to
    /// the linear mean elements and the absence of the eccentricity factor: the
    /// Android engine is the reference implementation, and a richer truncation
    /// here would resolve a different moon sign for roughly 1 in 500 users.
    public static func moonEclipticLongitude(julianDay jd: Double) -> Double {
        let t = (jd - 2451545.0) / 36525.0

        // Mean elements (degrees), normalized before the radian conversion —
        // converting the raw (~1e5 deg) values would lose low-order digits.
        let meanLongitude = normalizeDegrees(218.3164477 + 481267.88123421 * t)
        let meanElongation = normalizeDegrees(297.8501921 + 445267.1114034 * t)
        let sunMeanAnomaly = normalizeDegrees(357.5291092 + 35999.0502909 * t)
        let moonMeanAnomaly = normalizeDegrees(134.9633964 + 477198.8675055 * t)
        let argumentOfLatitude = normalizeDegrees(93.2720950 + 483202.0175233 * t)

        let d = radians(meanElongation)
        let m = radians(sunMeanAnomaly)
        let mp = radians(moonMeanAnomaly)
        let f = radians(argumentOfLatitude)

        let longitude = meanLongitude +
            6.288774 * sin(mp) +
            1.274027 * sin(2 * d - mp) +
            0.658314 * sin(2 * d) +
            0.213618 * sin(2 * mp) -
            0.185116 * sin(m) -
            0.114332 * sin(2 * f)
        return normalizeDegrees(longitude)
    }

    // MARK: - Ascendant (standard formula, Meeus ch. 14 form)

    /// Ecliptic longitude of the ascendant in degrees [0, 360).
    /// - Parameters:
    ///   - lstDegrees: local sidereal time in degrees (= RAMC).
    ///   - latitudeDegrees: geographic latitude, clamped to ±89.9 to keep tan
    ///     finite (same bound as `AstroMath.ascendantDegrees`).
    public static func ascendantLongitude(
        lstDegrees: Double,
        latitudeDegrees: Double,
        obliquityDegrees: Double = Astronomy.obliquityDegrees
    ) -> Double {
        let ramc = radians(normalizeDegrees(lstDegrees))
        let lat = radians(min(max(latitudeDegrees, -89.9), 89.9))
        let eps = radians(obliquityDegrees)

        // tan(Asc) = -cos(RAMC) / (sin(RAMC)*cos(eps) + tan(lat)*sin(eps))
        // expressed through atan2 with the quadrant convention that puts the
        // ascendant on the eastern horizon.
        let asc = atan2(cos(ramc), -(sin(ramc) * cos(eps) + tan(lat) * sin(eps)))
        return normalizeDegrees(degrees(asc))
    }

    // MARK: - Helpers

    public static func normalizeDegrees(_ value: Double) -> Double {
        var v = value.truncatingRemainder(dividingBy: 360.0)
        if v < 0 { v += 360.0 }
        // Tiny negative inputs can round to exactly 360.0 after the addition.
        if v >= 360.0 { v -= 360.0 }
        return v
    }

    /// Operator order matches `Math.toRadians` (divide first) so the Android
    /// and iOS series round identically.
    private static func radians(_ deg: Double) -> Double { deg / 180.0 * .pi }
    private static func degrees(_ rad: Double) -> Double { rad * 180.0 / .pi }
}
