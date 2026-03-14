package com.palmastro.scoring

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeltaEngineTest {
    private val engine = DeltaEngineImpl()

    private fun makeResult(
        monthKey: String, careerScore: Int = 60, qualityScore: Int = 80,
        coverage: Float = 0.9f, grade: String = "Stable"
    ) = MonthlyResult(
        resultId = "r-$monthKey", monthKey = monthKey, scanSessionId = "s-$monthKey",
        scoringResult = ScoringResult(
            domainScores = mapOf("career" to careerScore, "wealth" to 55, "family" to 50, "health" to 52),
            subdimScores = emptyMap(), grade = grade, confidence = "high",
            confidenceReasons = emptyList(), explainability = emptyList(),
            matchedBuckets = emptyList(), rulesetVersion = "1.0.0"
        ),
        semanticPayloads = emptyMap(),
        scanQualityScore = qualityScore, featureCoverage = coverage,
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun `delta computes score differences per domain`() {
        val prev = makeResult("2026-02", careerScore = 60)
        val curr = makeResult("2026-03", careerScore = 68)
        val delta = engine.computeDelta(prev, curr)
        assertEquals(8, delta.domainDeltas["career"]!!.value)
        assertEquals("up", delta.domainDeltas["career"]!!.arrow)
    }

    @Test
    fun `delta down arrow for negative change`() {
        val prev = makeResult("2026-02", careerScore = 70)
        val curr = makeResult("2026-03", careerScore = 62)
        val delta = engine.computeDelta(prev, curr)
        assertEquals(-8, delta.domainDeltas["career"]!!.value)
        assertEquals("down", delta.domainDeltas["career"]!!.arrow)
    }

    @Test
    fun `delta flat arrow for no change`() {
        val prev = makeResult("2026-02", careerScore = 60)
        val curr = makeResult("2026-03", careerScore = 60)
        val delta = engine.computeDelta(prev, curr)
        assertEquals("flat", delta.domainDeltas["career"]!!.arrow)
    }

    @Test
    fun `comparability HIGH when scans are similar`() {
        val prev = makeResult("2026-02", qualityScore = 80, coverage = 0.9f)
        val curr = makeResult("2026-03", qualityScore = 82, coverage = 0.88f)
        val delta = engine.computeDelta(prev, curr)
        assertEquals(ComparabilityBucket.HIGH, delta.comparabilityBucket)
        assertTrue(delta.comparabilityScore >= 70)
    }

    @Test
    fun `comparability LOW when quality differs greatly`() {
        val prev = makeResult("2026-02", qualityScore = 90, coverage = 0.95f)
        val curr = makeResult("2026-03", qualityScore = 30, coverage = 0.3f)
        val delta = engine.computeDelta(prev, curr)
        assertEquals(ComparabilityBucket.LOW, delta.comparabilityBucket)
        assertTrue(delta.comparabilityScore < 50)
    }

    @Test
    fun `grade shift recorded when grade changes`() {
        val prev = makeResult("2026-02", grade = "Building")
        val curr = makeResult("2026-03", grade = "Stable")
        val delta = engine.computeDelta(prev, curr)
        assertEquals(GradeShift("Building", "Stable"), delta.gradeShift)
    }

    @Test
    fun `grade shift null when grade unchanged`() {
        val prev = makeResult("2026-02", grade = "Stable")
        val curr = makeResult("2026-03", grade = "Stable")
        val delta = engine.computeDelta(prev, curr)
        assertNull(delta.gradeShift)
    }

    @Test
    fun `month keys are recorded`() {
        val prev = makeResult("2026-02")
        val curr = makeResult("2026-03")
        val delta = engine.computeDelta(prev, curr)
        assertEquals("2026-02", delta.prevMonthKey)
        assertEquals("2026-03", delta.currentMonthKey)
    }
}
