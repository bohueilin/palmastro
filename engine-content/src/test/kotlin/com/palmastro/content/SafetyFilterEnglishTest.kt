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
        interpretationZh = interpretation, blindspotZh = "Watch your efficiency.",
        actionTodayZh = "Plan tasks.", actionWeekZh = "Do hard things.",
        promptZh = "Are you confident?", safetyNotesZh = emptyList(),
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
        assertFalse(filter.validate(p).passed)
    }

    @Test
    fun `catches investment advice in English wealth content`() {
        val p = makePayload("wealth", "Here is some investment advice for you.")
        assertFalse(filter.validate(p).passed)
    }

    @Test
    fun `catches diagnosis in English health content`() {
        val p = makePayload("health", "Based on your palm, the diagnosis is clear.")
        assertFalse(filter.validate(p).passed)
    }

    @Test
    fun `catches medication in English health content`() {
        val p = makePayload("health", "You should take medication for this.")
        assertFalse(filter.validate(p).passed)
    }

    @Test
    fun `catches you are worthless in English career content`() {
        val p = makePayload("career", "Honestly, you are worthless at this.")
        assertFalse(filter.validate(p).passed)
    }

    @Test
    fun `catches give up in English content`() {
        val p = makePayload("career", "You should just give up on your career.")
        assertFalse(filter.validate(p).passed)
    }

    @Test
    fun `catches cross-domain English health terms in career`() {
        val p = makePayload("career", "Your career stress needs medical treatment plan.")
        val result = filter.validate(p)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("cross_domain_health") })
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
