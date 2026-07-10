package com.palmastro.astro

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reference vectors for AstroMath.
 *
 * External anchors (Meeus, "Astronomical Algorithms"):
 * - Julian Day: 2000-01-01 12:00 UT = 2451545.0 exactly (J2000.0).
 * - Julian Day + GMST: example 12.b (1987-04-10 19:21:00 UT,
 *   JD 2446896.30625, mean sidereal time 8h34m57.09s = 128.73787 deg).
 * - Moon: example 47.a (1992-04-12 0h TD) - the full theory gives an
 *   apparent longitude of 133.1627 deg; the low-precision truncation used
 *   here gives 133.2489 deg, within its documented ~0.3 deg accuracy.
 *
 * All other values are self-consistent vectors computed once with these
 * exact formulas and frozen here to catch regressions.
 * Tolerances: 1e-6 day for JD, 5e-4 deg for GMST, 0.01 deg for frozen
 * moon/ascendant vectors, 0.3 deg vs external ephemeris values.
 */
class AstroMathTest {

    @Nested
    inner class JulianDay {
        @Test
        fun `J2000 epoch - 2000-01-01 noon UT at lon 0`() {
            val jd = AstroMath.julianDayUt(LocalDate.of(2000, 1, 1), LocalTime.of(12, 0), 0.0)
            assertEquals(2451545.0, jd, 1e-6)
        }

        @Test
        fun `Meeus 12b - 1987-04-10 19-21 UT at lon 0`() {
            val jd = AstroMath.julianDayUt(LocalDate.of(1987, 4, 10), LocalTime.of(19, 21), 0.0)
            assertEquals(2446896.30625, jd, 1e-6)
        }

        @Test
        fun `east longitude shifts UT earlier by lon over 15 hours`() {
            val jdGreenwich = AstroMath.julianDayUt(LocalDate.of(1990, 7, 15), LocalTime.of(10, 0), 0.0)
            val jdTaipei = AstroMath.julianDayUt(LocalDate.of(1990, 7, 15), LocalTime.of(10, 0), 121.5654)
            // 121.5654 / 15 hours = 8.1044 h = 0.337682 day earlier in UT
            assertEquals(121.5654 / 15.0 / 24.0, jdGreenwich - jdTaipei, 1e-9)
        }

        @Test
        fun `west longitude shifts UT later`() {
            val jd = AstroMath.julianDayUt(LocalDate.of(2000, 1, 1), LocalTime.of(0, 0), -74.0060)
            assertEquals(2451544.70557, jd, 1e-4)
        }
    }

    @Nested
    inner class Gmst {
        @Test
        fun `Meeus 12b mean sidereal time`() {
            // Expected 8h34m57.0896s (mean) = 128.73787 deg
            val gmst = AstroMath.gmstDegrees(2446896.30625)
            assertEquals(128.73787, gmst, 5e-4)
        }

        @Test
        fun `gmst is normalized to 0-360`() {
            for (jd in listOf(2400000.5, 2451545.0, 2466154.0)) {
                val g = AstroMath.gmstDegrees(jd)
                assertTrue(g >= 0.0 && g < 360.0, "GMST $g out of range for JD $jd")
            }
        }
    }

    @Nested
    inner class MoonLongitude {
        @Test
        fun `Meeus 47a - within documented tolerance of full theory`() {
            // Full theory apparent longitude: 133.162655 deg.
            val lon = AstroMath.moonEclipticLongitudeDeg(2448724.5)
            assertTrue(abs(lon - 133.162655) < 0.3, "moon lon $lon deviates more than 0.3 deg")
            // Frozen self-consistent value for regression detection.
            assertEquals(133.2489, lon, 0.01)
        }

        @Test
        fun `J2000 epoch moon in Scorpio`() {
            val lon = AstroMath.moonEclipticLongitudeDeg(2451545.0)
            assertEquals(223.2814, lon, 0.01)
            assertEquals("SCORPIO", ZodiacCalculator.signAtLongitude(lon).first)
        }

        @Test
        fun `1995-11-02 2345 UT moon in Pisces`() {
            val lon = AstroMath.moonEclipticLongitudeDeg(2450024.48958)
            assertEquals(349.3224, lon, 0.01)
            assertEquals("PISCES", ZodiacCalculator.signAtLongitude(lon).first)
        }

        @Test
        fun `2001-09-09 0146 UT moon in Taurus`() {
            val lon = AstroMath.moonEclipticLongitudeDeg(2452161.57361)
            assertEquals(55.7688, lon, 0.01)
            assertEquals("TAURUS", ZodiacCalculator.signAtLongitude(lon).first)
        }
    }

    @Nested
    inner class Ascendant {
        @Test
        fun `Greenwich 1987-04-10 1921 UT ascendant in Libra`() {
            val asc = AstroMath.ascendantDegrees(2446896.30625, 51.4778, 0.0)
            assertEquals(207.2441, asc, 0.01)
            assertEquals("LIBRA", ZodiacCalculator.signAtLongitude(asc).first)
        }

        @Test
        fun `equator lon 0 1985-06-01 0600 ascendant in Gemini`() {
            val asc = AstroMath.ascendantDegrees(2446217.75, 0.0, 0.0)
            assertEquals(71.2257, asc, 0.01)
            assertEquals("GEMINI", ZodiacCalculator.signAtLongitude(asc).first)
        }

        @Test
        fun `ascendant is normalized to 0-360 across a full day`() {
            for (hour in 0 until 24) {
                val jd = 2451545.0 + hour / 24.0
                val asc = AstroMath.ascendantDegrees(jd, 40.0, -74.0)
                assertTrue(asc >= 0.0 && asc < 360.0, "asc $asc out of range at hour $hour")
            }
        }

        @Test
        fun `ascendant advances through all 12 signs over a sidereal day`() {
            val signs = (0 until 96).map { step ->
                val jd = 2451545.0 + step / 96.0
                ZodiacCalculator.signAtLongitude(AstroMath.ascendantDegrees(jd, 25.0, 121.5)).first
            }.toSet()
            assertEquals(12, signs.size, "expected all 12 signs, got $signs")
        }

        @Test
        fun `extreme latitude does not produce NaN`() {
            val asc = AstroMath.ascendantDegrees(2451545.0, 90.0, 0.0)
            assertTrue(asc.isFinite())
        }
    }

    @Test
    fun `normalizeDegrees handles negatives and wraps`() {
        assertEquals(350.0, AstroMath.normalizeDegrees(-10.0), 1e-9)
        assertEquals(0.0, AstroMath.normalizeDegrees(720.0), 1e-9)
        assertEquals(15.5, AstroMath.normalizeDegrees(375.5), 1e-9)
    }
}
