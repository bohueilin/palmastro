package com.palmastro.palm

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PalmFeatureExtractorTest {
    private val extractor = PalmFeatureExtractorImpl()

    private fun makeBestFrames(qualityScore: Int = 80, coverage: Float = 0.9f): Map<Angle, BestFrameResult> =
        Angle.entries.associateWith { angle ->
            BestFrameResult(angle, 0, QualityScores(0.8f, 0.8f, 0.8f, coverage, 0.8f, qualityScore), null)
        }

    @Test
    fun `extract returns non-identifying categorical features`() {
        val result = extractor.extract(makeBestFrames(), Hand.RIGHT)
        assertTrue(result.features["headline_present"] is Boolean)
        assertTrue(result.features["headline_shape"] is String)
        assertTrue(result.features["headline_clarity"] is String)
    }

    @Test
    fun `confidence is high when quality ge 70 and coverage ge 0_8`() {
        val result = extractor.extract(makeBestFrames(qualityScore = 80, coverage = 0.9f), Hand.RIGHT)
        assertEquals("high", result.confidence)
    }

    @Test
    fun `confidence is med when quality between 40-70`() {
        val result = extractor.extract(makeBestFrames(qualityScore = 55, coverage = 0.9f), Hand.RIGHT)
        assertEquals("med", result.confidence)
    }

    @Test
    fun `confidence is low when quality below 40`() {
        val result = extractor.extract(makeBestFrames(qualityScore = 30, coverage = 0.3f), Hand.RIGHT)
        assertEquals("low", result.confidence)
    }

    @Test
    fun `feature coverage reflects how many features were extractable`() {
        val result = extractor.extract(makeBestFrames(), Hand.RIGHT)
        assertTrue(result.featureCoverage in 0f..1f)
    }

    @Test
    fun `extractor version is semver`() {
        val result = extractor.extract(makeBestFrames(), Hand.RIGHT)
        assertTrue(result.extractorVersion.matches(Regex("""\d+\.\d+\.\d+""")))
    }
}
