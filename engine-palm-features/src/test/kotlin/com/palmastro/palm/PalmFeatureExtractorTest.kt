package com.palmastro.palm

import com.palmastro.contracts.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PalmFeatureExtractorTest {
    private val extractor = PalmFeatureExtractorImpl()

    // Canonical normalized MediaPipe-style landmark layout (right hand, palm up).
    private fun canonicalLandmarks(): List<LandmarkPoint> = listOf(
        LandmarkPoint(0.50f, 0.95f, 0f), // 0 wrist
        LandmarkPoint(0.38f, 0.85f, 0f), // 1 thumb CMC
        LandmarkPoint(0.30f, 0.75f, 0f), // 2 thumb MCP
        LandmarkPoint(0.24f, 0.66f, 0f), // 3 thumb IP
        LandmarkPoint(0.20f, 0.58f, 0f), // 4 thumb tip
        LandmarkPoint(0.34f, 0.48f, 0f), // 5 index MCP
        LandmarkPoint(0.32f, 0.36f, 0f), // 6
        LandmarkPoint(0.31f, 0.27f, 0f), // 7
        LandmarkPoint(0.30f, 0.19f, 0f), // 8
        LandmarkPoint(0.46f, 0.46f, 0f), // 9 middle MCP
        LandmarkPoint(0.46f, 0.32f, 0f), // 10
        LandmarkPoint(0.46f, 0.22f, 0f), // 11
        LandmarkPoint(0.46f, 0.13f, 0f), // 12
        LandmarkPoint(0.58f, 0.47f, 0f), // 13 ring MCP
        LandmarkPoint(0.59f, 0.34f, 0f), // 14
        LandmarkPoint(0.60f, 0.25f, 0f), // 15
        LandmarkPoint(0.61f, 0.17f, 0f), // 16
        LandmarkPoint(0.70f, 0.52f, 0f), // 17 pinky MCP
        LandmarkPoint(0.72f, 0.41f, 0f), // 18
        LandmarkPoint(0.73f, 0.33f, 0f), // 19
        LandmarkPoint(0.74f, 0.26f, 0f), // 20
    )

    private fun region(name: String, contrast: Float, continuity: Float, meanIntensity: Float = 0.5f) =
        LineRegionMetrics(name, contrast, continuity, meanIntensity)

    private fun palmMetrics(
        headline: LineRegionMetrics = region("headline", 0.70f, 0.90f),
        heartline: LineRegionMetrics = region("heartline", 0.70f, 0.85f),
        lifeline: LineRegionMetrics = region("lifeline", 0.65f, 0.80f),
        fateline: LineRegionMetrics = region("fateline", 0.60f, 0.80f),
        landmarks: List<LandmarkPoint> = canonicalLandmarks()
    ) = PalmMetrics(landmarks, listOf(headline, heartline, lifeline, fateline))

    private fun frames(
        metrics: PalmMetrics? = palmMetrics(),
        quality: Int = 80,
        coverage: Float = 0.9f,
        angles: List<Angle> = listOf(Angle.FRONT, Angle.NEAR, Angle.FAR)
    ): Map<Angle, BestFrameResult> = angles.associateWith { angle ->
        BestFrameResult(angle, 0, QualityScores(0.8f, 0.8f, 0.8f, coverage, 0.8f, quality), null, metrics)
    }

    @Test
    fun `extract returns non-identifying categorical features`() {
        val result = extractor.extract(frames(), Hand.RIGHT)
        assertTrue(result.features.headlinePresent)
        assertTrue(result.features.headlineShape.isNotBlank())
        assertTrue(result.features.headlineClarity.isNotBlank())
    }

    @Test
    fun `strong palm - clear lines and long headline`() {
        val f = extractor.extract(frames(), Hand.RIGHT).features
        assertEquals("clear", f.headlineClarity)
        assertEquals("long", f.headlineLength)
        assertEquals("clear", f.heartlineClarity)
        assertEquals("clear", f.lifelineClarity)
        assertTrue(f.fatelinePresent)
        assertEquals("clear", f.fatelineClarity)
    }

    @Test
    fun `canonical hand geometry - curved head and heart lines, straight fateline`() {
        val f = extractor.extract(frames(), Hand.RIGHT).features
        assertEquals("curved", f.headlineShape)
        assertEquals("curved", f.heartlineShape)
        assertEquals("straight", f.fatelineShape)
    }

    @Nested
    inner class ClarityBuckets {
        @Test
        fun `mid continuity with strong contrast reads broken`() {
            val m = palmMetrics(headline = region("headline", 0.55f, 0.50f))
            val f = extractor.extract(frames(m), Hand.RIGHT).features
            assertTrue(f.headlinePresent)
            assertEquals("broken", f.headlineClarity)
        }

        @Test
        fun `mid continuity with weak contrast reads faint`() {
            val m = palmMetrics(fateline = region("fateline", 0.30f, 0.50f))
            val f = extractor.extract(frames(m), Hand.RIGHT).features
            assertEquals("faint", f.fatelineClarity)
        }

        @Test
        fun `heartline with high continuity but shallow contrast reads thin`() {
            val m = palmMetrics(heartline = region("heartline", 0.30f, 0.80f))
            val f = extractor.extract(frames(m), Hand.RIGHT).features
            assertEquals("thin", f.heartlineClarity)
        }

        @Test
        fun `non-heartline with high continuity but shallow contrast reads faint`() {
            val m = palmMetrics(lifeline = region("lifeline", 0.30f, 0.70f))
            val f = extractor.extract(frames(m), Hand.RIGHT).features
            assertEquals("faint", f.lifelineClarity)
        }

        @Test
        fun `moderate contrast with good continuity reads medium`() {
            val m = palmMetrics(headline = region("headline", 0.45f, 0.70f))
            val f = extractor.extract(frames(m), Hand.RIGHT).features
            assertEquals("medium", f.headlineClarity)
        }

        @Test
        fun `low continuity means line absent and unclear`() {
            val m = palmMetrics(fateline = region("fateline", 0.70f, 0.20f))
            val f = extractor.extract(frames(m), Hand.RIGHT).features
            assertFalse(f.fatelinePresent)
            assertEquals("unclear", f.fatelineClarity)
            assertEquals("unclear", f.fatelineShape)
        }
    }

    @Test
    fun `dense minor lines from high residual contrast`() {
        val m = palmMetrics(
            headline = region("headline", 0.80f, 0.50f),
            heartline = region("heartline", 0.80f, 0.50f),
            lifeline = region("lifeline", 0.80f, 0.50f),
            fateline = region("fateline", 0.80f, 0.50f)
        )
        val f = extractor.extract(frames(m), Hand.RIGHT).features
        assertEquals("high", f.minorLineDensity)
    }

    @Test
    fun `clean continuous lines give low minor line density`() {
        val m = palmMetrics(
            headline = region("headline", 0.50f, 0.90f),
            heartline = region("heartline", 0.50f, 0.90f),
            lifeline = region("lifeline", 0.50f, 0.90f),
            fateline = region("fateline", 0.50f, 0.90f)
        )
        val f = extractor.extract(frames(m), Hand.RIGHT).features
        assertEquals("low", f.minorLineDensity)
    }

    @Test
    fun `mount density reflects darkness and contrast of adjacent region`() {
        val dark = palmMetrics(lifeline = region("lifeline", 0.65f, 0.80f, meanIntensity = 0.25f))
        assertEquals("high", extractor.extract(frames(dark), Hand.RIGHT).features.venusMountDensity)
        val bright = palmMetrics(lifeline = region("lifeline", 0.40f, 0.80f, meanIntensity = 0.95f))
        assertEquals("low", extractor.extract(frames(bright), Hand.RIGHT).features.venusMountDensity)
    }

    @Test
    fun `deterministic - same input gives same output`() {
        val input = frames()
        assertEquals(extractor.extract(input, Hand.RIGHT), extractor.extract(input, Hand.RIGHT))
    }

    @Test
    fun `input sensitive - different palmMetrics give different features`() {
        val strong = extractor.extract(frames(palmMetrics()), Hand.RIGHT)
        val weak = extractor.extract(
            frames(
                palmMetrics(
                    headline = region("headline", 0.55f, 0.50f),
                    heartline = region("heartline", 0.30f, 0.80f),
                    lifeline = region("lifeline", 0.30f, 0.70f),
                    fateline = region("fateline", 0.70f, 0.20f)
                )
            ),
            Hand.RIGHT
        )
        assertNotEquals(strong.features, weak.features)
    }

    @Nested
    inner class Aggregation {
        @Test
        fun `median across angles is robust to one outlier frame`() {
            val good = palmMetrics()
            val outlier = palmMetrics(headline = region("headline", 0.05f, 0.05f))
            val frames = mapOf(
                Angle.FRONT to BestFrameResult(Angle.FRONT, 0, QualityScores(0.8f, 0.8f, 0.8f, 0.9f, 0.8f, 80), null, good),
                Angle.NEAR to BestFrameResult(Angle.NEAR, 0, QualityScores(0.8f, 0.8f, 0.8f, 0.9f, 0.8f, 80), null, outlier),
                Angle.FAR to BestFrameResult(Angle.FAR, 0, QualityScores(0.8f, 0.8f, 0.8f, 0.9f, 0.8f, 80), null, good),
            )
            val f = extractor.extract(frames, Hand.RIGHT).features
            assertTrue(f.headlinePresent)
            assertEquals("clear", f.headlineClarity)
        }

        @Test
        fun `high contrast spread across angles marks line broken`() {
            // Median contrast 0.35 (below the broken-contrast bar) but the
            // across-angle spread 0.40 exceeds the spread proxy.
            val variants = listOf(0.20f, 0.35f, 0.60f).map { c ->
                palmMetrics(headline = region("headline", c, 0.50f))
            }
            val frames = listOf(Angle.FRONT, Angle.NEAR, Angle.FAR).zip(variants).associate { (angle, m) ->
                angle to BestFrameResult(angle, 0, QualityScores(0.8f, 0.8f, 0.8f, 0.9f, 0.8f, 80), null, m)
            }
            val f = extractor.extract(frames, Hand.RIGHT).features
            assertEquals("broken", f.headlineClarity)
        }
    }

    @Nested
    inner class Fallback {
        @Test
        fun `all palmMetrics null falls back to conservative low confidence`() {
            val result = extractor.extract(frames(metrics = null), Hand.RIGHT)
            assertEquals("low", result.confidence)
            val f = result.features
            // Conservative: no category that resolves a negative signal.
            assertTrue(f.headlineClarity !in setOf("broken", "faint", "thin"))
            assertTrue(f.heartlineClarity !in setOf("broken", "faint", "thin"))
            assertTrue(f.lifelineClarity !in setOf("broken", "faint", "thin"))
            assertNotEquals("high", f.minorLineDensity)
            assertFalse(f.fatelinePresent)
        }

        @Test
        fun `malformed landmark list is treated as missing metrics`() {
            val bad = PalmMetrics(canonicalLandmarks().take(5), palmMetrics().lineRegions)
            val result = extractor.extract(frames(metrics = bad), Hand.RIGHT)
            assertEquals("low", result.confidence)
        }

        @Test
        fun `empty frame map falls back`() {
            val result = extractor.extract(emptyMap(), Hand.RIGHT)
            assertEquals("low", result.confidence)
            assertTrue(result.featureCoverage in 0f..1f)
        }
    }

    @Nested
    inner class Confidence {
        @Test
        fun `confidence is high when quality ge 70 and rich metrics`() {
            val result = extractor.extract(frames(quality = 80), Hand.RIGHT)
            assertEquals("high", result.confidence)
        }

        @Test
        fun `confidence is med when quality between 40-70`() {
            val result = extractor.extract(frames(quality = 55), Hand.RIGHT)
            assertEquals("med", result.confidence)
        }

        @Test
        fun `confidence is low when quality below 40`() {
            val result = extractor.extract(frames(quality = 30), Hand.RIGHT)
            assertEquals("low", result.confidence)
        }
    }

    @Test
    fun `feature coverage reflects how many features were extractable`() {
        val rich = extractor.extract(frames(), Hand.RIGHT)
        assertTrue(rich.featureCoverage in 0f..1f)
        val fallback = extractor.extract(frames(metrics = null), Hand.RIGHT)
        assertTrue(rich.featureCoverage > fallback.featureCoverage)
    }

    @Test
    fun `missing region lowers coverage`() {
        val noFateline = PalmMetrics(
            canonicalLandmarks(),
            palmMetrics().lineRegions.filter { it.region != "fateline" }
        )
        val partial = extractor.extract(frames(noFateline), Hand.RIGHT)
        val full = extractor.extract(frames(), Hand.RIGHT)
        assertTrue(partial.featureCoverage < full.featureCoverage)
        assertFalse(partial.features.fatelinePresent)
    }

    @Test
    fun `extractor version is 2_0_0 semver`() {
        val result = extractor.extract(frames(), Hand.RIGHT)
        assertEquals("2.0.0", result.extractorVersion)
        assertTrue(result.extractorVersion.matches(Regex("""\d+\.\d+\.\d+""")))
    }
}
