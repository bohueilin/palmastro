package com.palmastro.scan

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class QualityGateTest {
    private val gate = QualityGateImpl()

    @Test
    fun `scoreFrame computes composite as weighted average scaled to 0-100`() {
        val scores = gate.scoreFrame(blur = 0.8f, glare = 0.9f, exposure = 0.7f, coverage = 0.85f, stability = 0.9f)
        // equal weights: (0.8+0.9+0.7+0.85+0.9)/5 = 0.83 → 83
        assertEquals(83, scores.composite)
    }

    @Test
    fun `scoreFrame clamps composite to 0-100`() {
        val low = gate.scoreFrame(0f, 0f, 0f, 0f, 0f)
        assertEquals(0, low.composite)
        val high = gate.scoreFrame(1f, 1f, 1f, 1f, 1f)
        assertEquals(100, high.composite)
    }

    @Test
    fun `selectBestFrame returns index of highest composite`() {
        val frames = listOf(
            QualityScores(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 50),
            QualityScores(0.9f, 0.9f, 0.9f, 0.9f, 0.9f, 90),
            QualityScores(0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 70),
        )
        assertEquals(1, gate.selectBestFrame(frames))
    }

    @Test
    fun `selectBestFrame tiebreaks by coverage then blur`() {
        val frames = listOf(
            QualityScores(0.7f, 0.8f, 0.8f, 0.85f, 0.8f, 80),
            QualityScores(0.9f, 0.8f, 0.7f, 0.8f, 0.8f, 80),
        )
        assertEquals(0, gate.selectBestFrame(frames))
    }

    @Test
    fun `evaluateAngle passes when composite ge 60`() {
        val passing = QualityScores(0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 80)
        val result = gate.evaluateAngle(Angle.FRONT, passing)
        assertTrue(result.passed)
        assertEquals(null, result.failReason)
    }

    @Test
    fun `evaluateAngle fails when composite lt 60 with worst component as reason`() {
        val failing = QualityScores(0.2f, 0.8f, 0.8f, 0.8f, 0.8f, 50)
        val result = gate.evaluateAngle(Angle.FRONT, failing)
        assertFalse(result.passed)
        assertEquals("blur", result.failReason)
    }

    @Test
    fun `zero coverage fails with hand_not_detected`() {
        val noHand = QualityScores(0.2f, 0.3f, 0.3f, 0.0f, 0.3f, 20)
        val result = gate.evaluateAngle(Angle.FRONT, noHand)
        assertFalse(result.passed)
        assertEquals("hand_not_detected", result.failReason)
    }

    @Test
    fun `zero coverage fails even when other components would pass the gate`() {
        // 0.2 * (1.0 * 4) = 80 composite, but there is no hand in frame.
        val noHand = gate.scoreFrame(blur = 1f, glare = 1f, exposure = 1f, coverage = 0f, stability = 1f)
        val result = gate.evaluateAngle(Angle.FRONT, noHand)
        assertFalse(result.passed)
        assertEquals("hand_not_detected", result.failReason)
    }

    @Test
    fun `partial coverage failure stays low_coverage, distinct from hand_not_detected`() {
        val partial = QualityScores(0.8f, 0.8f, 0.8f, 0.1f, 0.8f, 50)
        val result = gate.evaluateAngle(Angle.FRONT, partial)
        assertFalse(result.passed)
        assertEquals("low_coverage", result.failReason)
    }

    @Test
    fun `coaching keys are stable for every fail reason`() {
        assertEquals("coach_blur", CoachingHints.keyFor("blur"))
        assertEquals("coach_glare", CoachingHints.keyFor("glare"))
        assertEquals("coach_low_light", CoachingHints.keyFor("low_light"))
        assertEquals("coach_over_exposure", CoachingHints.keyFor("over_exposure"))
        assertEquals("coach_low_light", CoachingHints.keyFor("under_exposure"))
        assertEquals("coach_low_coverage", CoachingHints.keyFor("low_coverage"))
        assertEquals("coach_pose_unstable", CoachingHints.keyFor("pose_unstable"))
        assertEquals("coach_hand_not_detected", CoachingHints.keyFor("hand_not_detected"))
    }

    @Test
    fun `unknown fail reason maps to generic coaching key`() {
        assertEquals("coach_generic", CoachingHints.keyFor("something_new"))
    }

    @Test
    fun `every gate fail reason has a non-generic coaching key`() {
        val reasons = listOf("blur", "glare", "low_light", "low_coverage", "pose_unstable", "hand_not_detected")
        reasons.forEach { reason ->
            assertTrue(CoachingHints.keyFor(reason) != "coach_generic", "missing key for $reason")
        }
    }

    @Test
    fun `reference hint text exists for fail reasons`() {
        assertEquals("Hold still until the image stabilizes", CoachingHints.getHint("blur"))
        assertEquals("Avoid direct light sources", CoachingHints.getHint("glare"))
        assertEquals("Place your entire palm within the guide", CoachingHints.getHint("low_coverage"))
        assertEquals("No palm detected — place your hand in the frame", CoachingHints.getHint("hand_not_detected"))
        assertEquals("Please try again", CoachingHints.getHint("something_new"))
    }
}
