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
    fun `coaching hint returns zh-TW string for fail reason`() {
        assertEquals("請保持手不動，等畫面穩定", CoachingHints.getHint("blur"))
        assertEquals("請避免直射光源", CoachingHints.getHint("glare"))
        assertEquals("光線不足，請移到較亮的地方", CoachingHints.getHint("low_light"))
        assertEquals("請將手掌完整放入框線內", CoachingHints.getHint("low_coverage"))
    }
}
