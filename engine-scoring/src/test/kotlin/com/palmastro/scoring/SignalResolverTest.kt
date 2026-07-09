package com.palmastro.scoring

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignalResolverTest {
    private val ruleset = Ruleset.default()

    private fun makePalmFeatures(
        headlinePresent: Boolean = false, headlineClarity: String = "faint", headlineLength: String = "medium",
        heartlinePresent: Boolean = false, heartlineClarity: String = "faint",
        lifelinePresent: Boolean = false, lifelineClarity: String = "faint",
        fatelinePresent: Boolean = false, fatelineShape: String = "faint",
    ) = PalmFeatureResult(
        features = PalmFeatures(
            headlinePresent = headlinePresent, heartlinePresent = heartlinePresent,
            lifelinePresent = lifelinePresent, fatelinePresent = fatelinePresent,
            headlineShape = "curved", heartlineShape = "curved", lifelineShape = "curved", fatelineShape = fatelineShape,
            headlineClarity = headlineClarity, heartlineClarity = heartlineClarity,
            lifelineClarity = lifelineClarity, fatelineClarity = "faint",
            headlineLength = headlineLength, fatelineLength = "short",
            venusMountDensity = "low", jupiterMountDensity = "low", saturnMountDensity = "low", minorLineDensity = "low",
        ),
        featureCoverage = 0.9f, confidence = "high", extractorVersion = "1.0.0"
    )

    @Test fun `headline long and clear matches PALM_HEADLINE_LONG_CLEAR`() {
        val f = makePalmFeatures(headlinePresent = true, headlineClarity = "clear", headlineLength = "long")
        val signals = SignalResolver.resolvePalmSignals(f, ruleset)
        assertEquals(1, signals.size); assertEquals("PALM_HEADLINE_LONG_CLEAR", signals[0].signalId)
    }
    @Test fun `headline present but not clear produces no match`() {
        val signals = SignalResolver.resolvePalmSignals(makePalmFeatures(headlinePresent = true, headlineClarity = "faint", headlineLength = "long"), ruleset)
        assertTrue(signals.isEmpty())
    }
    @Test fun `headline present and clear but not long produces no match`() {
        val signals = SignalResolver.resolvePalmSignals(makePalmFeatures(headlinePresent = true, headlineClarity = "clear", headlineLength = "short"), ruleset)
        assertTrue(signals.isEmpty())
    }
    @Test fun `heartline present and clear matches PALM_HEARTLINE_STRONG`() {
        val signals = SignalResolver.resolvePalmSignals(makePalmFeatures(heartlinePresent = true, heartlineClarity = "clear"), ruleset)
        assertEquals(1, signals.size); assertEquals("PALM_HEARTLINE_STRONG", signals[0].signalId)
    }
    @Test fun `heartline present and moderate matches PALM_HEARTLINE_STRONG`() {
        val signals = SignalResolver.resolvePalmSignals(makePalmFeatures(heartlinePresent = true, heartlineClarity = "moderate"), ruleset)
        assertEquals(1, signals.size); assertEquals("PALM_HEARTLINE_STRONG", signals[0].signalId)
    }
    @Test fun `heartline present but faint produces no match`() {
        val signals = SignalResolver.resolvePalmSignals(makePalmFeatures(heartlinePresent = true, heartlineClarity = "faint"), ruleset)
        assertTrue(signals.isEmpty())
    }
    @Test fun `lifeline present and clear matches PALM_LIFELINE_CLEAR`() {
        val signals = SignalResolver.resolvePalmSignals(makePalmFeatures(lifelinePresent = true, lifelineClarity = "clear"), ruleset)
        assertEquals(1, signals.size); assertEquals("PALM_LIFELINE_CLEAR", signals[0].signalId)
    }
    @Test fun `fateline present and straight matches PALM_FATELINE_STRONG`() {
        val signals = SignalResolver.resolvePalmSignals(makePalmFeatures(fatelinePresent = true, fatelineShape = "straight"), ruleset)
        assertEquals(1, signals.size); assertEquals("PALM_FATELINE_STRONG", signals[0].signalId)
    }
    @Test fun `all features matching produces 4 palm signals`() {
        val f = makePalmFeatures(headlinePresent = true, headlineClarity = "clear", headlineLength = "long",
            heartlinePresent = true, heartlineClarity = "clear", lifelinePresent = true, lifelineClarity = "clear",
            fatelinePresent = true, fatelineShape = "straight")
        assertEquals(4, SignalResolver.resolvePalmSignals(f, ruleset).size)
    }
    @Test fun `no features matching produces empty list`() {
        assertTrue(SignalResolver.resolvePalmSignals(makePalmFeatures(), ruleset).isEmpty())
    }
    @Test fun `astro signals with matching IDs resolve correctly`() {
        val astro = AstroResult(CalcLevel.L2, listOf(
            AstroSignal("ASTRO_SUN_FIRE", "+", 2, "low", "SAFE_GENERAL"),
            AstroSignal("ASTRO_SATURN_STRONG", "+", 3, "med", "SAFE_CAREER")), "1.0.0")
        val resolved = SignalResolver.resolveAstroSignals(astro, ruleset)
        assertEquals(2, resolved.size)
    }
    @Test fun `astro signals with non-matching IDs return empty`() {
        val astro = AstroResult(CalcLevel.L2, listOf(AstroSignal("NONEXISTENT", "+", 2, "low", "SAFE_GENERAL")), "1.0.0")
        assertTrue(SignalResolver.resolveAstroSignals(astro, ruleset).isEmpty())
    }
}
