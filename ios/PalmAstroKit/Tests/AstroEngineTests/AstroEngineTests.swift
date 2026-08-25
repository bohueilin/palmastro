import Foundation
import Testing
import CoreContracts
@testable import AstroEngine

@Suite struct AstronomyTests {

    /// Meeus "Astronomical Algorithms" ch. 7 checks:
    /// 1987 April 10, 0h UT -> JD 2446895.5; J2000.0 epoch -> JD 2451545.0.
    @Test func julianDay() {
        #expect(abs(Astronomy.julianDay(year: 1987, month: 4, day: 10, hourUT: 0) - 2446895.5) < 1e-9)
        #expect(abs(Astronomy.julianDay(year: 2000, month: 1, day: 1, hourUT: 12) - 2451545.0) < 1e-9)
    }

    /// Meeus example 12.a: 1987 April 10, 0h UT ->
    /// mean sidereal time 13h10m46.3668s = 197.693195 deg.
    @Test func greenwichMeanSiderealTime() {
        let gmst = Astronomy.gmstDegrees(julianDay: 2446895.5)
        #expect(abs(gmst - 197.693195) < 0.0001)
    }

    /// Meeus example 47.a: 1992 April 12, 0h TD -> lunar longitude
    /// 133.162655 deg (full series). The low-precision series must stay
    /// sign-accurate (well within +/-0.5 deg).
    @Test func moonLongitudeAgainstMeeusExample() {
        let jd = Astronomy.julianDay(year: 1992, month: 4, day: 12, hourUT: 0)
        let longitude = Astronomy.moonEclipticLongitude(julianDay: jd)
        #expect(abs(longitude - 133.162655) < 0.5)
        #expect(Zodiac.sign(forEclipticLongitude: longitude).name == "LEO")
    }

    /// Equator sanity anchors: when 0/90/180/270 deg of RA culminate, the
    /// rising ecliptic point is 90/180/270/0 deg respectively.
    @Test func ascendantQuadrantAnchorsAtEquator() {
        #expect(abs(Astronomy.ascendantLongitude(lstDegrees: 0, latitudeDegrees: 0) - 90) < 0.01)
        #expect(abs(Astronomy.ascendantLongitude(lstDegrees: 90, latitudeDegrees: 0) - 180) < 0.01)
        #expect(abs(Astronomy.ascendantLongitude(lstDegrees: 180, latitudeDegrees: 0) - 270) < 0.01)
        #expect(abs(Astronomy.ascendantLongitude(lstDegrees: 270, latitudeDegrees: 0) - 0) < 0.01
            || abs(Astronomy.ascendantLongitude(lstDegrees: 270, latitudeDegrees: 0) - 360) < 0.01)
    }

    @Test func ascendantStaysInRangeAcrossLatitudes() {
        for lst in stride(from: 0.0, to: 360.0, by: 30.0) {
            for lat in [-60.0, -45.0, 0.0, 23.5, 45.0, 60.0] {
                let asc = Astronomy.ascendantLongitude(lstDegrees: lst, latitudeDegrees: lat)
                #expect(asc >= 0 && asc < 360)
            }
        }
    }

    /// Polar latitudes must reach the formula unclamped up to ±89.9, matching
    /// AstroMath.ascendantDegrees — a tighter clamp silently computes the
    /// ascendant as if the user were born far to the south.
    @Test func ascendantClampMatchesAndroidBound() {
        // Tromso (69.6496 N): a ±66.5 clamp would collapse these two onto the
        // same value, and the resolved sign would differ from Android's.
        let tromso = Astronomy.ascendantLongitude(lstDegrees: 200, latitudeDegrees: 69.6496)
        let clampedEquivalent = Astronomy.ascendantLongitude(lstDegrees: 200, latitudeDegrees: 66.5)
        #expect(abs(tromso - clampedEquivalent) > 0.5)
        // Beyond the bound both platforms saturate at the same place.
        let beyond = Astronomy.ascendantLongitude(lstDegrees: 200, latitudeDegrees: 95)
        let atBound = Astronomy.ascendantLongitude(lstDegrees: 200, latitudeDegrees: 89.9)
        #expect(abs(beyond - atBound) < 1e-9)
    }
}

@Suite struct ZodiacTests {

    @Test func sunSignBoundaries() {
        #expect(Zodiac.sunSign(for: CivilDate(year: 1990, month: 3, day: 21)).name == "ARIES")
        #expect(Zodiac.sunSign(for: CivilDate(year: 1990, month: 4, day: 19)).name == "ARIES")
        #expect(Zodiac.sunSign(for: CivilDate(year: 1990, month: 4, day: 20)).name == "TAURUS")
        #expect(Zodiac.sunSign(for: CivilDate(year: 1990, month: 12, day: 22)).name == "CAPRICORN")
        #expect(Zodiac.sunSign(for: CivilDate(year: 1990, month: 1, day: 19)).name == "CAPRICORN")
        #expect(Zodiac.sunSign(for: CivilDate(year: 1990, month: 1, day: 20)).name == "AQUARIUS")
        #expect(Zodiac.sunSign(for: CivilDate(year: 1990, month: 2, day: 19)).name == "PISCES")
    }

    @Test func elementAndModalityTables() {
        let leo = Zodiac.sunSign(for: CivilDate(year: 1990, month: 8, day: 1))
        #expect(leo.name == "LEO")
        #expect(leo.element == "FIRE")
        #expect(leo.modality == "FIXED")

        let virgo = Zodiac.sign(forEclipticLongitude: 155)
        #expect(virgo.name == "VIRGO")
        #expect(virgo.element == "EARTH")
        #expect(virgo.modality == "MUTABLE")
    }
}

@Suite struct AstroEngineImplTests {

    private let engine = AstroEngineImpl()

    @Test func l1EmitsSunSignElementAndModality() {
        let result = engine.compute(
            birthday: CivilDate(year: 1995, month: 8, day: 2),
            birthTime: nil, birthPlaceLat: nil, birthPlaceLon: nil
        )
        #expect(result.calcLevel == .L1)
        // Order is contractual: the sign signal comes first (AstroEngineTest.kt).
        #expect(result.signals.map(\.signalId) == ["ASTRO_SUN_LEO", "ASTRO_SUN_FIRE", "ASTRO_SUN_FIXED"])
        #expect(result.signals.map(\.magnitude) == [3, 2, 2])
        #expect(result.engineVersion == "2.0.0")
        // PRD §17: L1 must not include ascendant or house signals.
        #expect(!result.signals.contains { $0.signalId.contains("ASC") })
        #expect(!result.signals.contains { $0.signalId.contains("HOUSE") })
        // No fabricated planetary-strength signals (EXECUTION_SPEC).
        #expect(!result.signals.contains { $0.signalId.contains("SATURN") || $0.signalId.contains("JUPITER") || $0.signalId.contains("MARS") })
    }

    @Test func l2AddsMoonAndAscendantElements() {
        let result = engine.compute(
            birthday: CivilDate(year: 1992, month: 4, day: 12),
            birthTime: CivilTime(hour: 8, minute: 30),
            birthPlaceLat: 25.03,   // Taipei
            birthPlaceLon: 121.56
        )
        #expect(result.calcLevel == .L2)
        #expect(result.signals.count == 5)
        #expect(result.signals[0].signalId == "ASTRO_SUN_ARIES")
        #expect(result.signals[1].signalId == "ASTRO_SUN_FIRE")
        #expect(result.signals[2].signalId == "ASTRO_SUN_CARDINAL")
        #expect(result.signals[3].signalId.hasPrefix("ASTRO_MOON_"))
        #expect(result.signals[4].signalId.hasPrefix("ASTRO_ASC_"))
        for signal in result.signals {
            #expect(signal.confidence == "high")
        }
    }

    /// 1992-04-12 08:30 local mean solar time in Taipei is ~00:24 UT — within
    /// hours of the Meeus 47.a epoch, so the Moon must still be in Leo (fire).
    @Test func l2MoonSignMatchesMeeusEpoch() {
        let result = engine.compute(
            birthday: CivilDate(year: 1992, month: 4, day: 12),
            birthTime: CivilTime(hour: 8, minute: 30),
            birthPlaceLat: 25.03,
            birthPlaceLon: 121.56
        )
        #expect(result.signals[3].signalId == "ASTRO_MOON_FIRE")
    }

    @Test func partialBirthDataStaysL1() {
        let timeOnly = engine.compute(
            birthday: CivilDate(year: 1990, month: 6, day: 15),
            birthTime: CivilTime(hour: 12, minute: 0),
            birthPlaceLat: nil, birthPlaceLon: nil
        )
        #expect(timeOnly.calcLevel == .L1)
        #expect(timeOnly.signals.count == 3)
    }

    /// Mirrors AstroEngineTest.kt: one shared rule tags every water element
    /// health-soft, whichever body carries it.
    @Test func waterElementsCarryHealthSoftSafetyTag() {
        // 2000-01-01 00:00 NYC: sun Capricorn (earth), moon Scorpio (water).
        let result = engine.compute(
            birthday: CivilDate(year: 2000, month: 1, day: 1),
            birthTime: CivilTime(hour: 0, minute: 0),
            birthPlaceLat: 40.7128, birthPlaceLon: -74.0060
        )
        let moonWater = result.signals.first { $0.signalId == "ASTRO_MOON_WATER" }
        #expect(moonWater?.safetyTag == "SAFE_HEALTH_SOFT_ONLY")
        #expect(result.signals.contains { $0.signalId == "ASTRO_ASC_AIR" })

        // A Pisces sun: the element signal itself must carry the soft tag.
        let pisces = engine.compute(
            birthday: CivilDate(year: 1990, month: 3, day: 1),
            birthTime: nil, birthPlaceLat: nil, birthPlaceLon: nil
        )
        let sunWater = pisces.signals.first { $0.signalId == "ASTRO_SUN_WATER" }
        #expect(sunWater?.safetyTag == "SAFE_HEALTH_SOFT_ONLY")
    }

    @Test func deterministicOutput() {
        let a = engine.compute(
            birthday: CivilDate(year: 1988, month: 11, day: 3),
            birthTime: CivilTime(hour: 23, minute: 45),
            birthPlaceLat: 40.71, birthPlaceLon: -74.0
        )
        let b = engine.compute(
            birthday: CivilDate(year: 1988, month: 11, day: 3),
            birthTime: CivilTime(hour: 23, minute: 45),
            birthPlaceLat: 40.71, birthPlaceLon: -74.0
        )
        #expect(a == b)
    }
}
