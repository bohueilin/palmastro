package com.palmastro.content

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.SafetyCheckResult
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class SafetyFilterAdversarialTest {
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
    fun `zero-width space between characters does not bypass wealth filter`() {
        val payload = makePayload(domain = "wealth", interpretation = "建議買​入這支基金。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("買入") })
    }

    @Test
    fun `zero-width non-joiner does not bypass wealth filter`() {
        val payload = makePayload(domain = "wealth", interpretation = "這支股‌票值得關注。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("股票") })
    }

    @Test
    fun `zero-width joiner does not bypass health filter`() {
        val payload = makePayload(domain = "health", interpretation = "可能需要藥‍物介入。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("藥物") })
    }

    @Test
    fun `soft hyphen insertion does not bypass health filter`() {
        val payload = makePayload(domain = "health", interpretation = "初步診­斷結果如下。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("診斷") })
    }

    @Test
    fun `BOM character insertion does not bypass health filter`() {
        val payload = makePayload(domain = "health", interpretation = "懷疑可能是癌﻿症。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("癌症") })
    }

    @Test
    fun `fullwidth Latin characters are normalized and caught in wealth domain`() {
        val payload = makePayload(domain = "wealth", interpretation = "你應該ｂｕｙ更多基金。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("buy") })
    }

    @Test
    fun `mixed fullwidth and normal characters are caught in wealth domain`() {
        val payload = makePayload(domain = "wealth", interpretation = "投資ｂｕｙ建議很重要。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("投資建議") || it.contains("buy") })
    }

    @Test
    fun `NFC and NFD decomposition both catch prohibited terms`() {
        val nfcForm = java.text.Normalizer.normalize("診斷", java.text.Normalizer.Form.NFC)
        val nfdForm = java.text.Normalizer.normalize("診斷", java.text.Normalizer.Form.NFD)
        val resultNfc = filter.validate(makePayload(domain = "health", interpretation = "初步${nfcForm}結果。"))
        val resultNfd = filter.validate(makePayload(domain = "health", interpretation = "初步${nfdForm}結果。"))
        assertFalse(resultNfc.passed)
        assertFalse(resultNfd.passed)
    }

    @Test
    fun `very long string with prohibited term buried in middle is caught`() {
        val padding = "這是一段安全的內容。".repeat(500)
        val payload = makePayload(domain = "wealth", interpretation = "${padding}保證獲利${padding}")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("保證獲利") })
    }

    @Test
    fun `empty string input passes validation`() {
        val payload = makePayload(interpretation = "")
        val result = filter.validate(payload)
        assertTrue(result.passed)
        assertTrue(result.violations.isEmpty())
    }

    @Test
    fun `string of only zero-width characters passes validation`() {
        val payload = makePayload(interpretation = "​‌‍﻿­")
        val result = filter.validate(payload)
        assertTrue(result.passed)
        assertTrue(result.violations.isEmpty())
    }

    @Test
    fun `medical term in career domain is flagged as cross-domain health violation`() {
        val payload = makePayload(domain = "career", interpretation = "你可能有糖尿病的傾向。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("糖尿病") })
    }

    @Test
    fun `medical term in wealth domain is flagged as cross-domain health violation`() {
        val payload = makePayload(domain = "wealth", interpretation = "癌症風險影響你的財務。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("癌症") })
    }

    @Test
    fun `financial term in health domain is not flagged`() {
        val payload = makePayload(domain = "health", interpretation = "你的健康能量穩定，股票無關。")
        val result = filter.validate(payload)
        assertTrue(result.passed)
    }

    @Test
    fun `partial character match does not trigger identity attack filter`() {
        val payload = makePayload(interpretation = "白雲飄過天空。")
        val result = filter.validate(payload)
        assertTrue(result.passed)
        assertTrue(result.violations.isEmpty())
    }
}
