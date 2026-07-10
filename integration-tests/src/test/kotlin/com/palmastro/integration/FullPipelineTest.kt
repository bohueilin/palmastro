package com.palmastro.integration

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.SafetyCheckResult
import com.palmastro.scan.QualityGateImpl
import com.palmastro.palm.PalmFeatureExtractorImpl
import com.palmastro.astro.AstroEngineImpl
import com.palmastro.scoring.ScoringEngineImpl
import com.palmastro.scoring.DeltaEngineImpl
import com.palmastro.content.ContentComposerImpl
import com.palmastro.content.ToneRenderer
import com.palmastro.content.SafetyFilterImpl
import com.palmastro.analytics.AnalyticsEmitterImpl
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FullPipelineTest {

    @Test
    fun `full pipeline - scan to rendered report`() {
        // 1. Scan quality gate
        val qualityGate = QualityGateImpl()
        val scores = qualityGate.scoreFrame(0.85f, 0.9f, 0.8f, 0.88f, 0.92f)
        assertTrue(scores.composite >= 60, "Quality should pass threshold")

        val angleResult = qualityGate.evaluateAngle(Angle.FRONT, scores)
        assertTrue(angleResult.passed)

        // 2. Build scan session summary (simulating 7 angles all passing)
        val bestFrames = Angle.entries.associateWith { BestFrameResult(it, 0, scores, null) }
        val scanSummary = ScanSessionSummary("session-1", Hand.RIGHT, bestFrames, scores.composite, 0.9f, 25000, 7)

        // 3. Extract palm features
        val featureExtractor = PalmFeatureExtractorImpl()
        val palmResult = featureExtractor.extract(scanSummary.angleResults, scanSummary.hand)
        assertEquals("high", palmResult.confidence)

        // 4. Compute astrology (L2)
        val astroEngine = AstroEngineImpl()
        val astroResult = astroEngine.compute(LocalDate.of(1990, 3, 21), LocalTime.of(14, 30), 25.0330, 121.5654)
        assertEquals(CalcLevel.L2, astroResult.calcLevel)

        // 5. Score
        val scoringEngine = ScoringEngineImpl()
        val scoringInput = ScoringInput(palmResult, astroResult, UserContext(Hand.RIGHT, false), "1.0.0")
        val scoringResult = scoringEngine.score(scoringInput)
        assertEquals(4, scoringResult.domainScores.size)
        scoringResult.domainScores.values.forEach { assertTrue(it in 0..100) }

        // 6. Compose content
        val contentComposer = ContentComposerImpl()
        val contentInput = ContentInput(scoringResult, null, Tone.SCIENTIFIC, emptySet(), CalcLevel.L2, "2026-03")
        val payloads = contentComposer.compose(contentInput)
        assertEquals(4, payloads.size)

        // 7. Safety filter
        val safetyFilter = SafetyFilterImpl()
        payloads.values.forEach { payload ->
            val check = safetyFilter.validate(payload)
            assertTrue(check.passed, "Safety check failed for ${payload.domain}: ${check.violations}")
        }

        // 8. Render all 3 tones
        val renderer = ToneRenderer()
        for (tone in Tone.entries) {
            val report = renderer.render(payloads["career"]!!, tone)
            assertEquals("career", report.domain)
            assertTrue(report.text.isNotBlank())
        }
    }

    @Test
    fun `full pipeline - L1 degradation excludes house signals`() {
        val astro = AstroEngineImpl()
        val result = astro.compute(LocalDate.of(1990, 7, 15), null, null, null)
        assertEquals(CalcLevel.L1, result.calcLevel)
        assertTrue(result.signals.none { it.signalId.contains("HOUSE") })
    }

    @Test
    fun `full pipeline - delta between two months`() {
        val scoringEngine = ScoringEngineImpl()
        val featureExtractor = PalmFeatureExtractorImpl()
        val astroEngine = AstroEngineImpl()

        val bestFrames80 = Angle.entries.associateWith {
            BestFrameResult(it, 0, QualityScores(0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 80), null)
        }
        val bestFrames85 = Angle.entries.associateWith {
            BestFrameResult(it, 0, QualityScores(0.85f, 0.85f, 0.85f, 0.85f, 0.85f, 85), null)
        }

        val palm1 = featureExtractor.extract(bestFrames80, Hand.RIGHT)
        val palm2 = featureExtractor.extract(bestFrames85, Hand.RIGHT)
        val astro = astroEngine.compute(LocalDate.of(1990, 3, 21), null, null, null)

        val score1 = scoringEngine.score(ScoringInput(palm1, astro, UserContext(Hand.RIGHT, false), "1.0.0"))
        val score2 = scoringEngine.score(ScoringInput(palm2, astro, UserContext(Hand.RIGHT, false), "1.0.0"))

        val month1 = MonthlyResult("r1", "2026-02", "s1", score1, emptyMap(), 80, 0.8f, System.currentTimeMillis())
        val month2 = MonthlyResult("r2", "2026-03", "s2", score2, emptyMap(), 85, 0.85f, System.currentTimeMillis())

        val deltaEngine = DeltaEngineImpl()
        val delta = deltaEngine.computeDelta(month1, month2)
        assertEquals("2026-02", delta.prevMonthKey)
        assertEquals("2026-03", delta.currentMonthKey)
        assertTrue(delta.comparabilityScore >= 70, "Similar scans should have high comparability")
    }

    @Test
    fun `analytics emitter filters sensitive data in pipeline`() {
        val events = mutableListOf<Pair<String, Map<String, Any>>>()
        val emitter = AnalyticsEmitterImpl { name, props -> events.add(name to props) }

        emitter.emit("scan_complete", mapOf(
            "quality" to 80,
            "palm_feature_vector" to listOf(0.1, 0.2, 0.3, 0.4),
            "hand" to "right",
            "file" to "/data/scan/frame01.jpg"
        ))

        assertEquals(1, events.size)
        val props = events[0].second
        assertEquals(80, props["quality"])
        assertEquals("right", props["hand"])
        assertTrue("palm_feature_vector" !in props)
        assertTrue("file" !in props)
    }
}
