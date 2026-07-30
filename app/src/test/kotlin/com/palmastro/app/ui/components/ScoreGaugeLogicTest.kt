package com.palmastro.app.ui.components

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Pure-JVM tests for [ScoreGaugeMath]: arc angles, clamping, averaging, and the
 * reduced-motion animation start. No Compose or Android dependencies.
 */
class ScoreGaugeLogicTest {

    private val tolerance = 0.0001f

    // ── Arc angle at score milestones ──

    @Test
    fun `score 0 sweeps 0 degrees`() {
        assertEquals(0f, ScoreGaugeMath.sweepAngle(ScoreGaugeMath.targetFraction(0)), tolerance)
    }

    @Test
    fun `score 50 sweeps half of the 270 degree arc`() {
        assertEquals(135f, ScoreGaugeMath.sweepAngle(ScoreGaugeMath.targetFraction(50)), tolerance)
    }

    @Test
    fun `score 100 sweeps the full 270 degree arc`() {
        assertEquals(270f, ScoreGaugeMath.sweepAngle(ScoreGaugeMath.targetFraction(100)), tolerance)
    }

    // ── Clamping ──

    @Test
    fun `scores are clamped into 0 to 100`() {
        assertEquals(0, ScoreGaugeMath.clampScore(-5))
        assertEquals(100, ScoreGaugeMath.clampScore(150))
        assertEquals(72, ScoreGaugeMath.clampScore(72))
    }

    @Test
    fun `out-of-range fractions never under- or over-sweep`() {
        assertEquals(0f, ScoreGaugeMath.sweepAngle(-0.5f), tolerance)
        assertEquals(270f, ScoreGaugeMath.sweepAngle(1.5f), tolerance)
    }

    // ── Average for the Results overall gauge ──

    @Test
    fun `average rounds half up`() {
        // (72 + 65 + 50 + 35) / 4 = 55.5 -> 56
        assertEquals(56, ScoreGaugeMath.averageScore(listOf(72, 65, 50, 35)))
    }

    @Test
    fun `average of a single score is that score`() {
        assertEquals(72, ScoreGaugeMath.averageScore(listOf(72)))
    }

    @Test
    fun `average of empty list is 0`() {
        assertEquals(0, ScoreGaugeMath.averageScore(emptyList()))
    }

    @Test
    fun `average clamps out-of-range inputs before averaging`() {
        // -40 -> 0 and 140 -> 100, mean 50
        assertEquals(50, ScoreGaugeMath.averageScore(listOf(-40, 140)))
    }

    // ── Animation target calculation ──

    @Test
    fun `reduced motion starts at the target so the gauge lands instantly`() {
        assertEquals(0.72f, ScoreGaugeMath.initialFraction(72, reduceMotion = true), tolerance)
    }

    @Test
    fun `full motion starts at zero and animates toward the target`() {
        assertEquals(0f, ScoreGaugeMath.initialFraction(72, reduceMotion = false), tolerance)
        assertEquals(0.72f, ScoreGaugeMath.targetFraction(72), tolerance)
    }

    @Test
    fun `target fraction clamps out-of-range scores`() {
        assertEquals(0f, ScoreGaugeMath.targetFraction(-10), tolerance)
        assertEquals(1f, ScoreGaugeMath.targetFraction(250), tolerance)
    }

    // ── Font-scale aware diameter ──

    @Test
    fun `diameter scale tracks the font scale between 1x and 1_6x`() {
        assertEquals(1f, ScoreGaugeMath.diameterScale(1f), tolerance)
        assertEquals(1.3f, ScoreGaugeMath.diameterScale(1.3f), tolerance)
        assertEquals(1.6f, ScoreGaugeMath.diameterScale(1.6f), tolerance)
    }

    @Test
    fun `diameter never shrinks below 1x for small font scales`() {
        assertEquals(1f, ScoreGaugeMath.diameterScale(0.85f), tolerance)
    }

    @Test
    fun `diameter growth is capped at 1_6x for a 2x font scale`() {
        // fontScale 2.0 must not double the gauge — text fits because the numeral cap
        // and the ring grow together up to the 1.6x ceiling.
        assertEquals(ScoreGaugeMath.MAX_DIAMETER_SCALE, ScoreGaugeMath.diameterScale(2f), tolerance)
    }

    // ── Tip marker geometry ──

    @Test
    fun `tip angle follows the arc from 135 to 405 degrees`() {
        assertEquals(135f, ScoreGaugeMath.tipAngleDegrees(0f), tolerance)
        assertEquals(270f, ScoreGaugeMath.tipAngleDegrees(0.5f), tolerance)
        assertEquals(405f, ScoreGaugeMath.tipAngleDegrees(1f), tolerance)
    }

    @Test
    fun `tip at half sweep sits at the top of the gauge`() {
        val (dx, dy) = ScoreGaugeMath.tipOffset(fraction = 0.5f, radius = 100f)
        assertEquals(0f, dx, absoluteTolerance = 0.001f)
        assertEquals(-100f, dy, absoluteTolerance = 0.001f)
    }
}
