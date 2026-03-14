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
    fun `L1 excludes house and ascendant signals`() {
        val result = engine.compute(LocalDate.of(1990, 7, 15), null, null, null)
        val signalIds = result.signals.map { it.signalId }
        assertTrue(signalIds.none { it.contains("HOUSE") || it.contains("ASCENDANT") })
    }

    @Test
    fun `L2 includes house and ascendant signals`() {
        val result = engine.compute(
            LocalDate.of(1990, 7, 15), LocalTime.of(10, 0), 25.0330, 121.5654
        )
        val signalIds = result.signals.map { it.signalId }
        assertTrue(signalIds.any { it.contains("HOUSE") || it.contains("ASCENDANT") })
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
    fun `deterministic - same inputs give same outputs`() {
        val r1 = engine.compute(LocalDate.of(1990, 1, 1), null, null, null)
        val r2 = engine.compute(LocalDate.of(1990, 1, 1), null, null, null)
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
}
