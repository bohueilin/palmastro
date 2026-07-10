package com.palmastro.astro

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AstroEngineTest {
    private val engine = AstroEngineImpl()

    @Test
    fun `compute with birthday only gives L1`() {
        val result = engine.compute(LocalDate.of(1990, 3, 21), null, null, null)
        assertEquals(CalcLevel.L1, result.calcLevel)
    }

    @Test
    fun `compute with birthday + time + place gives L2`() {
        val result = engine.compute(
            LocalDate.of(1990, 3, 21),
            LocalTime.of(14, 30),
            25.0330,
            121.5654
        )
        assertEquals(CalcLevel.L2, result.calcLevel)
    }

    @Test
    fun `partial birth data still gives L1`() {
        val timeOnly = engine.compute(LocalDate.of(1990, 3, 21), LocalTime.of(14, 30), null, null)
        assertEquals(CalcLevel.L1, timeOnly.calcLevel)
        val placeOnly = engine.compute(LocalDate.of(1990, 3, 21), null, 25.0330, 121.5654)
        assertEquals(CalcLevel.L1, placeOnly.calcLevel)
    }

    @Test
    fun `L1 excludes ascendant moon and house signals`() {
        val result = engine.compute(LocalDate.of(1990, 7, 15), null, null, null)
        val signalIds = result.signals.map { it.signalId }
        assertTrue(signalIds.none {
            it.contains("HOUSE") || it.contains("ASCENDANT") || it.startsWith("ASTRO_ASC_") || it.startsWith("ASTRO_MOON_")
        })
    }

    @Test
    fun `L1 emits exactly sun sign element and modality`() {
        val result = engine.compute(LocalDate.of(1990, 7, 15), null, null, null)
        // July 15 -> Cancer (water, cardinal)
        assertEquals(
            listOf("ASTRO_SUN_CANCER", "ASTRO_SUN_WATER", "ASTRO_SUN_CARDINAL"),
            result.signals.map { it.signalId }
        )
    }

    @Test
    fun `L2 adds moon element and ascendant element signals`() {
        val result = engine.compute(
            LocalDate.of(1990, 7, 15), LocalTime.of(10, 0), 25.0330, 121.5654
        )
        val signalIds = result.signals.map { it.signalId }
        assertEquals(1, signalIds.count { it.startsWith("ASTRO_MOON_") })
        assertEquals(1, signalIds.count { it.startsWith("ASTRO_ASC_") })
    }

    @Test
    fun `fabricated planetary and house signals are never emitted`() {
        val results = listOf(
            // Years divisible by 3, 5, 7 used to trigger SATURN/JUPITER/MARS.
            engine.compute(LocalDate.of(1995, 6, 1), null, null, null),
            engine.compute(LocalDate.of(2001, 6, 1), LocalTime.of(9, 0), 51.5, -0.12),
            engine.compute(LocalDate.of(2100, 6, 1), LocalTime.of(23, 59), -33.87, 151.21)
        )
        results.flatMap { it.signals }.map { it.signalId }.forEach { id ->
            assertTrue(
                !id.contains("SATURN") && !id.contains("JUPITER") && !id.contains("MARS") &&
                    !id.contains("HOUSE") && !id.contains("ASCENDANT"),
                "fabricated signal leaked: $id"
            )
        }
    }

    // --- Reference vectors (see AstroMathTest for anchors + tolerance notes) ---
    // Birth time is local mean time; UTC offset = lon / 15 hours.

    @Test
    fun `L2 Taipei 1990-07-15 1000 - moon Aries fire, ascendant Virgo earth`() {
        // jdUt = 2448087.57899, moon lon = 17.31 deg (Aries -> FIRE),
        // asc = 173.35 deg (Virgo -> EARTH); both >6 deg from boundaries.
        val result = engine.compute(
            LocalDate.of(1990, 7, 15), LocalTime.of(10, 0), 25.0330, 121.5654
        )
        val signalIds = result.signals.map { it.signalId }
        assertTrue("ASTRO_MOON_FIRE" in signalIds, "expected ASTRO_MOON_FIRE in $signalIds")
        assertTrue("ASTRO_ASC_EARTH" in signalIds, "expected ASTRO_ASC_EARTH in $signalIds")
    }

    @Test
    fun `L2 New York 2000-01-01 0000 - moon Scorpio water, ascendant Libra air`() {
        // jdUt = 2451544.70557, moon lon = 219.74 deg (Scorpio -> WATER),
        // asc = 188.07 deg (Libra -> AIR); both >8 deg from boundaries.
        val result = engine.compute(
            LocalDate.of(2000, 1, 1), LocalTime.of(0, 0), 40.7128, -74.0060
        )
        val signalIds = result.signals.map { it.signalId }
        assertTrue("ASTRO_MOON_WATER" in signalIds, "expected ASTRO_MOON_WATER in $signalIds")
        assertTrue("ASTRO_ASC_AIR" in signalIds, "expected ASTRO_ASC_AIR in $signalIds")
    }

    @Test
    fun `sun sign for March 21 is Aries`() {
        val result = engine.compute(LocalDate.of(1990, 3, 21), null, null, null)
        assertTrue(result.signals.any { it.signalId == "ASTRO_SUN_ARIES" })
    }

    @Test
    fun `sun sign for July 15 is Cancer`() {
        val result = engine.compute(LocalDate.of(1990, 7, 15), null, null, null)
        assertTrue(result.signals.any { it.signalId == "ASTRO_SUN_CANCER" })
    }

    @Test
    fun `sun sign signal comes before element and modality`() {
        // Downstream consumers pick the first ASTRO_SUN_-prefixed signal as the sign.
        val result = engine.compute(LocalDate.of(2000, 2, 29), null, null, null)
        val first = result.signals.first { it.signalId.startsWith("ASTRO_SUN_") }
        assertEquals("ASTRO_SUN_PISCES", first.signalId)
    }

    @Test
    fun `signal shape preserved - direction magnitude confidence`() {
        val result = engine.compute(LocalDate.of(1990, 3, 21), LocalTime.of(14, 0), 25.0, 121.0)
        result.signals.forEach { signal ->
            assertEquals("+", signal.direction)
            assertTrue(signal.magnitude in 1..5, "magnitude out of range: ${signal.magnitude}")
            assertTrue(signal.confidence in setOf("high", "med", "low"))
        }
    }

    @Test
    fun `deterministic - same inputs give same outputs`() {
        val r1 = engine.compute(LocalDate.of(1990, 1, 1), LocalTime.of(3, 30), 48.85, 2.35)
        val r2 = engine.compute(LocalDate.of(1990, 1, 1), LocalTime.of(3, 30), 48.85, 2.35)
        assertEquals(r1, r2)
    }

    @Test
    fun `engine version is semver`() {
        val result = engine.compute(LocalDate.of(2000, 6, 1), null, null, null)
        assertTrue(result.engineVersion.matches(Regex("""\d+\.\d+\.\d+""")))
    }

    @Test
    fun `all signals have valid safety tags`() {
        val result = engine.compute(LocalDate.of(1990, 3, 21), LocalTime.of(14, 0), 25.0, 121.0)
        val validTags = setOf("SAFE_GENERAL", "SAFE_CAREER", "SAFE_WEALTH_SOFT_ONLY", "SAFE_HEALTH_SOFT_ONLY", "SAFE_FAMILY")
        result.signals.forEach { signal ->
            assertTrue(signal.safetyTag in validTags, "Invalid safety tag: ${signal.safetyTag}")
        }
    }

    @Test
    fun `water signals carry health-soft safety tag`() {
        // 2000-01-01 00:00 NYC: sun Capricorn (earth), moon Scorpio (water).
        val result = engine.compute(LocalDate.of(2000, 1, 1), LocalTime.of(0, 0), 40.7128, -74.0060)
        val moonWater = result.signals.first { it.signalId == "ASTRO_MOON_WATER" }
        assertEquals("SAFE_HEALTH_SOFT_ONLY", moonWater.safetyTag)
    }
}
