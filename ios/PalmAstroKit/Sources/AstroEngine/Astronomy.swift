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
    /// Truncated to the ten largest longitude terms of the ELP-derived series
    /// (max error well under 0.5 deg — sign-accurate except within minutes of
    /// a cusp, which the product treats as acceptable per the PRD).
    public static func moonEclipticLongitude(julianDay jd: Double) -> Double {
        let t = (jd - 2451545.0) / 36525.0

        // Mean elements (degrees), Meeus 47.1–47.5.
        let lp = 218.3164477 + 481267.88123421 * t
            - 0.0015786 * t * t + t * t * t / 538841.0 - t * t * t * t / 65194000.0
        let d = 297.8501921 + 445267.1114034 * t
            - 0.0018819 * t * t + t * t * t / 545868.0 - t * t * t * t / 113065000.0
        let m = 357.5291092 + 35999.0502909 * t
            - 0.0001536 * t * t + t * t * t / 24490000.0
        let mp = 134.9633964 + 477198.8675055 * t
            + 0.0087414 * t * t + t * t * t / 69699.0 - t * t * t * t / 14712000.0
        let f = 93.2720950 + 483202.0175233 * t
            - 0.0036539 * t * t - t * t * t / 3526000.0 + t * t * t * t / 863310000.0

        // Eccentricity damping for terms containing the solar anomaly M.
        let e = 1.0 - 0.002516 * t - 0.0000074 * t * t

        let dR = radians(d), mR = radians(m), mpR = radians(mp), fR = radians(f)

        var sumL = 0.0
        sumL += 6.288774 * sin(mpR)
        sumL += 1.274027 * sin(2 * dR - mpR)
        sumL += 0.658314 * sin(2 * dR)
        sumL += 0.213618 * sin(2 * mpR)
        sumL += -0.185116 * e * sin(mR)
        sumL += -0.114332 * sin(2 * fR)
        sumL += 0.058793 * sin(2 * dR - 2 * mpR)
        sumL += 0.057066 * e * sin(2 * dR - mR - mpR)
        sumL += 0.053322 * sin(2 * dR + mpR)
        sumL += 0.045758 * e * sin(2 * dR - mR)

        return normalizeDegrees(lp + sumL)
    }

    // MARK: - Ascendant (standard formula, Meeus ch. 14 form)

    /// Ecliptic longitude of the ascendant in degrees [0, 360).
    /// - Parameters:
    ///   - lstDegrees: local sidereal time in degrees (= RAMC).
    ///   - latitudeDegrees: geographic latitude, clamped to ±66.5 to keep the
    ///     formula stable inside the polar circles.
    public static func ascendantLongitude(
        lstDegrees: Double,
        latitudeDegrees: Double,
        obliquityDegrees: Double = Astronomy.obliquityDegrees
    ) -> Double {
        let ramc = radians(normalizeDegrees(lstDegrees))
        let lat = radians(min(max(latitudeDegrees, -66.5), 66.5))
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

    private static func radians(_ deg: Double) -> Double { deg * .pi / 180.0 }
    private static func degrees(_ rad: Double) -> Double { rad * 180.0 / .pi }
}
