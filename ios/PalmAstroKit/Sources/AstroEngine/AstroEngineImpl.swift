import Foundation
import CoreContracts

/// Astro engine v2 (EXECUTION_SPEC "L2 astrology: real math only").
///
/// - L1 (birthday only): tropical sun sign -> `ASTRO_SUN_<SIGN>`,
///   `ASTRO_SUN_<ELEMENT>` and `ASTRO_SUN_<MODALITY>` signals, in that order.
///   No ascendant, no houses (PRD §17).
/// - L2 (birthday + time + place): adds `ASTRO_MOON_<ELEMENT>` (Meeus
///   low-precision lunar longitude) and `ASTRO_ASC_<ELEMENT>` (standard
///   ascendant formula, obliquity 23.4367 deg).
///
/// The fabricated planetary-strength signals of the prototype engine
/// (SATURN_STRONG et al.) are intentionally absent.
///
/// Timezone note: the contract carries no timezone, so birth time is
/// interpreted as local mean solar time and converted to UT with
/// `UT = local - longitude/15h`. Documented Assumption (editable); worst-case
/// drift vs the civil timezone is under ~1h which only matters within a few
/// degrees of a sign cusp.
public final class AstroEngineImpl: AstroEngineProtocol {

    private let version: String

    public init(version: String = "2.0.0") {
        self.version = version
    }

    public func compute(
        birthday: CivilDate,
        birthTime: CivilTime?,
        birthPlaceLat: Double?,
        birthPlaceLon: Double?
    ) -> AstroResult {
        let isL2 = birthTime != nil && birthPlaceLat != nil && birthPlaceLon != nil
        let calcLevel: CalcLevel = isL2 ? .L2 : .L1

        var signals: [AstroSignal] = []

        // L1: sun sign, element, modality (tropical, calendar-boundary table).
        // Order is contractual — downstream consumers take the first
        // ASTRO_SUN_-prefixed signal as the sign.
        let sun = Zodiac.sunSign(for: birthday)
        signals.append(AstroSignal(
            signalId: "ASTRO_SUN_\(sun.name)",
            direction: "+", magnitude: 3, confidence: "high", safetyTag: "SAFE_GENERAL"
        ))
        signals.append(AstroSignal(
            signalId: "ASTRO_SUN_\(sun.element)",
            direction: "+", magnitude: 2, confidence: "high",
            safetyTag: Self.elementSafetyTag(body: "SUN", element: sun.element)
        ))
        signals.append(AstroSignal(
            signalId: "ASTRO_SUN_\(sun.modality)",
            direction: "+", magnitude: 2, confidence: "high", safetyTag: "SAFE_GENERAL"
        ))

        if isL2, let time = birthTime, let lat = birthPlaceLat, let lon = birthPlaceLon {
            let localHour = Double(time.hour) + Double(time.minute) / 60.0
            let utHour = localHour - lon / 15.0
            let jd = Astronomy.julianDay(
                year: birthday.year, month: birthday.month, day: birthday.day, hourUT: utHour
            )

            let moonLongitude = Astronomy.moonEclipticLongitude(julianDay: jd)
            let moon = Zodiac.sign(forEclipticLongitude: moonLongitude)
            signals.append(AstroSignal(
                signalId: "ASTRO_MOON_\(moon.element)",
                direction: "+", magnitude: 2, confidence: "high",
                safetyTag: Self.elementSafetyTag(body: "MOON", element: moon.element)
            ))

            let lst = Astronomy.normalizeDegrees(Astronomy.gmstDegrees(julianDay: jd) + lon)
            let ascLongitude = Astronomy.ascendantLongitude(lstDegrees: lst, latitudeDegrees: lat)
            let asc = Zodiac.sign(forEclipticLongitude: ascLongitude)
            signals.append(AstroSignal(
                signalId: "ASTRO_ASC_\(asc.element)",
                direction: "+", magnitude: 2, confidence: "high",
                safetyTag: Self.elementSafetyTag(body: "ASC", element: asc.element)
            ))
        }

        return AstroResult(calcLevel: calcLevel, signals: signals, engineVersion: version)
    }

    /// Safety tags mirror the ruleset v2 definitions (PRD Appendix A2): water
    /// elements touch the health domain (soft-only); a fire ascendant maps to
    /// career; everything else is general. One shared rule for every body, in
    /// the same precedence order as `AstroEngineImpl.elementSafetyTag` on
    /// Android — the element table is uppercase here, lowercase there.
    private static func elementSafetyTag(body: String, element: String) -> String {
        if element == "WATER" { return "SAFE_HEALTH_SOFT_ONLY" }
        if body == "ASC" && element == "FIRE" { return "SAFE_CAREER" }
        return "SAFE_GENERAL"
    }
}
