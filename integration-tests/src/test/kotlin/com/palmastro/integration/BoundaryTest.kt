package com.palmastro.integration

import com.palmastro.contracts.*
import com.palmastro.scan.QualityGateImpl
import com.palmastro.astro.AstroEngineImpl
import com.palmastro.scoring.ScoringEngineImpl
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoundaryTest {
    private val qualityGate = QualityGateImpl()
    private val astroEngine = AstroEngineImpl()
    private val scoringEngine = ScoringEngineImpl()

    private fun makePalmFeatures(quality: String = "clear"): PalmFeatures = PalmFeatures(
        headlinePresent = quality != "faint", heartlinePresent = quality != "faint",
        lifelinePresent = quality != "faint", fatelinePresent = quality == "clear",
        headlineShape = if (quality == "clear") "curved" else "faint",
        heartlineShape = if (quality == "clear") "curved" else "faint",
        lifelineShape = if (quality == "clear") "curved" else "faint",
        fatelineShape = if (quality == "clear") "straight" else "faint",
        headlineClarity = quality, heartlineClarity = quality, lifelineClarity = quality,
        fatelineClarity = if (quality == "clear") "moderate" else "faint",
        headlineLength = if (quality == "clear") "long" else "medium",
        fatelineLength = if (quality == "clear") "medium" else "short",
        venusMountDensity = if (quality == "clear") "med" else "low",
        jupiterMountDensity = "med", saturnMountDensity = "low",
        minorLineDensity = if (quality == "clear") "med" else "low",
    )

    private fun makePalmResult(pf: PalmFeatures, confidence: String = "high", coverage: Float = 0.9f) =
        PalmFeatureResult(features = pf, featureCoverage = coverage, confidence = confidence, extractorVersion = "1.0.0")

    @Test fun `quality gate - all zeros composite 0 and fails`() {
        val s = qualityGate.scoreFrame(0f, 0f, 0f, 0f, 0f); assertEquals(0, s.composite); assertFalse(qualityGate.evaluateAngle(Angle.FRONT, s).passed)
    }
    @Test fun `quality gate - all ones composite 100 and passes`() {
        val s = qualityGate.scoreFrame(1f, 1f, 1f, 1f, 1f); assertEquals(100, s.composite); assertTrue(qualityGate.evaluateAngle(Angle.FRONT, s).passed)
    }
    @Test fun `quality gate - negative values produce valid clamped output`() {
        assertTrue(qualityGate.scoreFrame(-0.5f, -0.3f, -0.2f, -0.1f, -0.4f).composite in 0..100)
    }
    @Test fun `quality gate - values greater than 1 produce clamped output`() {
        assertTrue(qualityGate.scoreFrame(2.0f, 1.5f, 3.0f, 2.5f, 1.8f).composite in 0..100)
    }
    @Test fun `astro - Feb 29 leap year returns Pisces`() {
        assertEquals("ASTRO_SUN_PISCES", astroEngine.compute(LocalDate.of(2000, 2, 29), null, null, null).signals.first { it.signalId.startsWith("ASTRO_SUN_") }.signalId)
    }
    @Test fun `astro - Jan 1 returns Capricorn`() {
        assertEquals("ASTRO_SUN_CAPRICORN", astroEngine.compute(LocalDate.of(1995, 1, 1), null, null, null).signals.first { it.signalId.startsWith("ASTRO_SUN_") }.signalId)
    }
    @Test fun `astro - Dec 31 returns Capricorn`() {
        assertEquals("ASTRO_SUN_CAPRICORN", astroEngine.compute(LocalDate.of(1988, 12, 31), null, null, null).signals.first { it.signalId.startsWith("ASTRO_SUN_") }.signalId)
    }
    @Test fun `astro - 1900-01-01 does not crash`() {
        assertTrue(astroEngine.compute(LocalDate.of(1900, 1, 1), null, null, null).signals.isNotEmpty())
    }
    @Test fun `astro - 2025-12-31 does not crash`() {
        assertTrue(astroEngine.compute(LocalDate.of(2025, 12, 31), null, null, null).signals.isNotEmpty())
    }
    @Test fun `scoring - empty astro signals produce scores in 0-100`() {
        val r = scoringEngine.score(ScoringInput(makePalmResult(makePalmFeatures("clear")), AstroResult(CalcLevel.L1, emptyList(), "1.0.0"), UserContext(Hand.RIGHT, false), "1.0.0"))
        r.domainScores.values.forEach { assertTrue(it in 0..100) }
    }
    @Test fun `scoring - all faint features produce low confidence and near-baseline scores`() {
        val r = scoringEngine.score(ScoringInput(makePalmResult(makePalmFeatures("faint"), "low", 0.3f), AstroResult(CalcLevel.L1, emptyList(), "1.0.0"), UserContext(Hand.RIGHT, false), "1.0.0"))
        assertTrue(r.confidenceReasons.contains("scan_quality_low"))
        r.domainScores.values.forEach { assertTrue(it in 30..70, "Score $it should be near baseline") }
    }
    @Test fun `scoring - determinism across 100 identical calls`() {
        val input = ScoringInput(makePalmResult(makePalmFeatures("clear")), astroEngine.compute(LocalDate.of(1990, 6, 15), null, null, null), UserContext(Hand.RIGHT, false), "1.0.0")
        val first = scoringEngine.score(input)
        repeat(99) { assertEquals(first.domainScores, scoringEngine.score(input).domainScores) }
    }
}
