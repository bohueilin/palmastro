package com.palmastro.content

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafetyFilterEnglishTest {
    private val filter = SafetyFilterImpl()

    private fun makePayload(
        domain: String = "career",
        interpretation: String = "Your career energy is rising steadily."
    ) = SemanticPayload(
        domain = domain, monthKey = "2026-03", calcLevel = CalcLevel.L2,
        confidence = "high", observations = emptyList(),
        interpretation = Interpretation(interpretation), blindspot = "Watch your efficiency.",
        actionToday = "Plan tasks.", actionWeek = "Do hard things.",
        prompt = "Are you confident?", safetyNotes = emptyList(),
        explainability = emptyList(),
        scoreCard = ScoreCard(72, "Stable", null, null, emptyMap())
    )

    @Test
    fun `passes safe English content`() {
        assertTrue(filter.validate(makePayload()).passed)
    }

    @Test
    fun `catches guaranteed return in English wealth content`() {
        val p = makePayload("wealth", "This is a guaranteed return opportunity.")
        val result = filter.validate(p)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("guaranteed_money") })
    }

    @Test
    fun `catches investment advice in English wealth content`() {
        val p = makePayload("wealth", "Here is some investment advice for you.")
        val result = filter.validate(p)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("investment_advice") })
    }

    @Test
    fun `catches diagnosis in English health content`() {
        val p = makePayload("health", "Based on your palm, the diagnosis is clear.")
        val result = filter.validate(p)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("medical_diagnosis") })
    }

    @Test
    fun `catches medication advice in English health content`() {
        val p = makePayload("health", "You should take medication for this.")
        val result = filter.validate(p)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("treatment") })
    }

    @Test
    fun `catches you are worthless in English career content`() {
        val p = makePayload("career", "Honestly, you are worthless at this.")
        val result = filter.validate(p)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("identity_attack") })
    }

    @Test
    fun `catches give up in English content`() {
        val p = makePayload("career", "You should just give up on your career.")
        assertFalse(filter.validate(p).passed)
    }

    @Test
    fun `catches self harm phrasing in English content`() {
        val p = makePayload("family", "Some people feel life is not worth living.")
        val result = filter.validate(p)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("self_harm") })
    }

    @Test
    fun `catches fear fate claims in English content`() {
        val p = makePayload("career", "Sadly, you are doomed to fail this year.")
        val result = filter.validate(p)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("fear_fate_claims") })
    }

    @Test
    fun `catches profanity in English content`() {
        val p = makePayload("career", "This month was a shitty ride.")
        val result = filter.validate(p)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("profanity") })
    }

    @Test
    fun `catches medical terms outside the health domain`() {
        val p = makePayload("career", "Your career stress needs a treatment plan.")
        val result = filter.validate(p)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("treatment") })
    }

    @Test
    fun `catches investment terms outside the wealth domain`() {
        val p = makePayload("health", "Rest more and buy stocks while you wait.")
        val result = filter.validate(p)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("investment_advice") })
    }

    // ------------------------------------------- word-boundary false positives

    @Test
    fun `you haven't does not trigger the you-have rule`() {
        val p = makePayload("health", "Schedule a wellness check if you haven't had one recently.")
        val result = filter.validate(p)
        assertTrue(result.passed, "false positive: ${result.violations}")
    }

    @Test
    fun `default en health actionWeek passes validate`() {
        val templates = ContentTemplates.default()
        val actionWeek = templates.bucketText(templates.domains["health"]!!.actionWeek, 40, "en")
        assertTrue(actionWeek.isNotBlank())
        val p = makePayload("health", "Your energy is steady.").copy(actionWeek = actionWeek)
        val result = filter.validate(p)
        assertTrue(result.passed, "default health actionWeek flagged: ${result.violations}")
    }

    @Test
    fun `secure does not trigger the cure rule`() {
        val p = makePayload("wealth", "Feeling secure about money takes time.")
        assertTrue(filter.validate(p).passed)
    }

    @Test
    fun `closer does not trigger the loser rule`() {
        val p = makePayload("family", "You are growing closer to your family.")
        assertTrue(filter.validate(p).passed)
    }

    @Test
    fun `procured does not trigger the cure rule`() {
        val p = makePayload("career", "Resources were procured for the team.")
        assertTrue(filter.validate(p).passed)
    }

    @Test
    fun `safe English health content passes`() {
        val p = makePayload("health", "Your energy levels are stable. Keep resting well.")
        assertTrue(filter.validate(p).passed)
    }

    @Test
    fun `safe English wealth content passes`() {
        val p = makePayload("wealth", "Your financial awareness is growing. Stay mindful of spending.")
        assertTrue(filter.validate(p).passed)
    }
}
