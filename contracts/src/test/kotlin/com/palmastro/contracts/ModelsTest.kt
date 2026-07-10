package com.palmastro.contracts

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ModelsTest {
    @Test
    fun `Hand enum has LEFT and RIGHT`() {
        assertEquals(2, Hand.entries.size)
        assertNotNull(Hand.valueOf("LEFT"))
        assertNotNull(Hand.valueOf("RIGHT"))
    }

    @Test
    fun `Angle enum has 7 angles in correct order`() {
        val expected = listOf("FRONT", "LEFT_TILT", "RIGHT_TILT", "NEAR", "FAR", "UP_TILT", "DOWN_TILT")
        assertEquals(expected, Angle.entries.map { it.name })
    }

    @Test
    fun `CalcLevel L1 and L2`() {
        assertEquals(2, CalcLevel.entries.size)
    }

    @Test
    fun `Tone has three modes`() {
        assertEquals(3, Tone.entries.size)
        assertNotNull(Tone.valueOf("SCIENTIFIC"))
        assertNotNull(Tone.valueOf("HEALING"))
        assertNotNull(Tone.valueOf("ROAST_SAFE"))
    }

    @Test
    fun `QualityScores composite is 0-100 int`() {
        val qs = QualityScores(blur = 0.8f, glare = 0.9f, exposure = 0.7f, coverage = 0.85f, stability = 0.9f, composite = 82)
        assertEquals(82, qs.composite)
    }

    @Test
    fun `ScanSessionSummary contains 7 angle results`() {
        val angles = Angle.entries.associateWith { angle ->
            BestFrameResult(angle = angle, frameIndex = 0, qualityScores = QualityScores(0.8f, 0.9f, 0.7f, 0.85f, 0.9f, 80), fileRef = null)
        }
        val summary = ScanSessionSummary(
            sessionId = "test-123", hand = Hand.LEFT, angleResults = angles,
            overallQualityScore = 80, featureCoverage = 0.9f, totalDurationMs = 30000, totalAttempts = 7
        )
        assertEquals(7, summary.angleResults.size)
        assertEquals(Hand.LEFT, summary.hand)
    }

    @Test
    fun `PalmFeatureResult carries confidence level`() {
        val features = PalmFeatures(
            headlinePresent = true, heartlinePresent = true, lifelinePresent = true, fatelinePresent = false,
            headlineShape = "curved", heartlineShape = "straight", lifelineShape = "curved", fatelineShape = "none",
            headlineClarity = "clear", heartlineClarity = "medium", lifelineClarity = "clear", fatelineClarity = "faint",
            headlineLength = "long", fatelineLength = "short",
            venusMountDensity = "medium", jupiterMountDensity = "low", saturnMountDensity = "medium",
            minorLineDensity = "low"
        )
        val pfr = PalmFeatureResult(
            features = features,
            featureCoverage = 0.85f, confidence = "high", extractorVersion = "1.0.0"
        )
        assertEquals("high", pfr.confidence)
        assertEquals(0.85f, pfr.featureCoverage)
        assertEquals("curved", pfr.features.headlineShape)
    }

    @Test
    fun `AstroResult contains signals and calc level`() {
        val signal = AstroSignal(signalId = "ASTRO_SUN_EARTH_SIGN", direction = "+", magnitude = 3, confidence = "high", safetyTag = "SAFE_CAREER")
        val result = AstroResult(calcLevel = CalcLevel.L2, signals = listOf(signal), engineVersion = "1.0.0")
        assertEquals(CalcLevel.L2, result.calcLevel)
        assertEquals(1, result.signals.size)
    }

    @Test
    fun `ScoringResult contains domain scores and grade`() {
        val result = ScoringResult(
            domainScores = mapOf("career" to 78, "wealth" to 60, "family" to 50, "health" to 52),
            subdimScores = mapOf("career.focus" to 75),
            grade = "Stable", confidence = "high", confidenceReasons = emptyList(),
            explainability = emptyList(), matchedBuckets = listOf("BUCKET_CAREER_EXECUTOR"),
            rulesetVersion = "1.0.0"
        )
        assertEquals(78, result.domainScores["career"])
        assertEquals("Stable", result.grade)
    }

    @Test
    fun `DeltaResult tracks comparability`() {
        val delta = DeltaResult(
            domainDeltas = mapOf("career" to DeltaValue(4, "up")),
            subdimDeltas = emptyMap(), gradeShift = GradeShift("Building", "Stable"),
            comparabilityScore = 82, comparabilityBucket = ComparabilityBucket.HIGH,
            prevMonthKey = "2026-02", currentMonthKey = "2026-03"
        )
        assertEquals(82, delta.comparabilityScore)
        assertEquals(ComparabilityBucket.HIGH, delta.comparabilityBucket)
    }
}
