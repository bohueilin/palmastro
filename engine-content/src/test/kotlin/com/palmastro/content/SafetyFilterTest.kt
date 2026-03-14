package com.palmastro.content

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.SafetyCheckResult
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class SafetyFilterTest {
    private val filter = SafetyFilterImpl()

    private fun makePayload(
        domain: String = "career",
        interpretation: String = "你的職業能量正處於穩定上升期。"
    ) = SemanticPayload(
        domain = domain, monthKey = "2026-03", calcLevel = CalcLevel.L2,
        confidence = "high", observations = emptyList(),
        interpretationZh = interpretation, blindspotZh = "注意效率。",
        actionTodayZh = "規劃任務。", actionWeekZh = "做困難的事。",
        promptZh = "你有信心嗎？", safetyNotesZh = emptyList(),
        explainability = emptyList(),
        scoreCard = ScoreCard(72, "Stable", null, null, emptyMap())
    )

    @Test
    fun `validate passes safe content`() {
        val result = filter.validate(makePayload())
        assertTrue(result.passed)
        assertTrue(result.violations.isEmpty())
    }

    @Test
    fun `validate catches investment advice in wealth domain`() {
        val payload = makePayload(domain = "wealth", interpretation = "建議買入台積電股票，保證獲利。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.isNotEmpty())
    }

    @Test
    fun `validate catches medical claims in health domain`() {
        val payload = makePayload(domain = "health", interpretation = "你可能有糖尿病的風險，建議服用降血糖藥物。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
    }

    @Test
    fun `validate catches identity attacks`() {
        val payload = makePayload(interpretation = "你很爛，你沒救了。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
    }

    @Test
    fun `filter replaces prohibited content in rendered report`() {
        val report = RenderedReport("wealth", Tone.SCIENTIFIC, "<p>建議買入股票保證獲利</p>")
        val filtered = filter.filter(report)
        assertNotEquals(report.htmlZh, filtered.htmlZh)
    }
}
