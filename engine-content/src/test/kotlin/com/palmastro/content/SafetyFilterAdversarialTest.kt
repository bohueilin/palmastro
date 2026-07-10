package com.palmastro.content

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SafetyFilterAdversarialTest {
    private val filter = SafetyFilterImpl()

    private fun makePayload(
        domain: String = "career",
        interpretation: String = "你的職業能量正處於穩定上升期。"
    ) = SemanticPayload(
        domain = domain, monthKey = "2026-03", calcLevel = CalcLevel.L2,
        confidence = "high", observations = emptyList(),
        interpretation = Interpretation(interpretation), blindspot = "注意效率。",
        actionToday = "規劃任務。", actionWeek = "做困難的事。",
        prompt = "你有信心嗎？", safetyNotes = emptyList(),
        explainability = emptyList(),
        scoreCard = ScoreCard(72, "Stable", null, null, emptyMap())
    )

    // ------------------------------------------------------ unicode bypasses

    @Test
    fun `zero-width space between characters does not bypass investment filter`() {
        val payload = makePayload(domain = "wealth", interpretation = "建議買​入這支基金。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("買入") })
    }

    @Test
    fun `zero-width non-joiner does not bypass investment filter`() {
        val payload = makePayload(domain = "wealth", interpretation = "這支股‌票值得關注。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("股票") })
    }

    @Test
    fun `zero-width joiner does not bypass treatment filter`() {
        val payload = makePayload(domain = "health", interpretation = "可能需要藥‍物介入。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("藥物") })
    }

    @Test
    fun `soft hyphen insertion does not bypass diagnosis filter`() {
        val payload = makePayload(domain = "health", interpretation = "初步診­斷結果如下。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("診斷") })
    }

    @Test
    fun `BOM character insertion does not bypass diagnosis filter`() {
        val payload = makePayload(domain = "health", interpretation = "懷疑可能是癌﻿症。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("癌症") })
    }

    @Test
    fun `zero-width characters do not bypass self harm filter`() {
        val payload = makePayload(interpretation = "有時覺得活​不​下​去。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("self_harm") })
    }

    @Test
    fun `zero-width characters do not bypass fear fate filter`() {
        val payload = makePayload(interpretation = "你註‌定‌失‌敗。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("fear_fate_claims") })
    }

    @Test
    fun `fullwidth Latin characters are normalized and caught`() {
        val payload = makePayload(domain = "wealth", interpretation = "你應該ｂｕｙ ｓｔｏｃｋｓ現在。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("buy stock") })
    }

    @Test
    fun `mixed fullwidth and normal characters are caught`() {
        val payload = makePayload(domain = "wealth", interpretation = "投資建議：現在進場。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("投資建議") })
    }

    @Test
    fun `fullwidth profanity is folded and caught`() {
        val payload = makePayload(interpretation = "ｄａｍｎ ｙｏｕ的態度。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("profanity") })
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

    // ------------------------------------------------- cross-domain coverage

    @Test
    fun `medical term in career domain is flagged`() {
        val payload = makePayload(domain = "career", interpretation = "你可能有糖尿病的傾向。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("糖尿病") })
    }

    @Test
    fun `medical term in wealth domain is flagged`() {
        val payload = makePayload(domain = "wealth", interpretation = "癌症風險影響你的財務。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("癌症") })
    }

    @Test
    fun `financial term in health domain is flagged cross-domain`() {
        val payload = makePayload(domain = "health", interpretation = "多休息，順便買進一些股票。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("investment_advice") })
    }

    @Test
    fun `guaranteed money claim in family domain is flagged cross-domain`() {
        val payload = makePayload(domain = "family", interpretation = "家人合作保證獲利。")
        val result = filter.validate(payload)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("guaranteed_money") })
    }

    @Test
    fun `self harm phrasing is flagged in every domain`() {
        for (domain in Domains.ALL) {
            val result = filter.validate(makePayload(domain = domain, interpretation = "別傷害自己。"))
            assertFalse(result.passed, "self_harm missed in $domain")
            assertTrue(result.violations.any { it.startsWith("self_harm") })
        }
    }

    @Test
    fun `identity attack is flagged in every domain`() {
        for (domain in Domains.ALL) {
            val result = filter.validate(makePayload(domain = domain, interpretation = "你就是個失敗者。"))
            assertFalse(result.passed, "identity_attack missed in $domain")
            assertTrue(result.violations.any { it.startsWith("identity_attack") })
        }
    }

    @Test
    fun `fear fate claim is flagged in every domain`() {
        for (domain in Domains.ALL) {
            val result = filter.validate(makePayload(domain = domain, interpretation = "今年恐有厄運纏身。"))
            assertFalse(result.passed, "fear_fate_claims missed in $domain")
            assertTrue(result.violations.any { it.startsWith("fear_fate_claims") })
        }
    }

    // ----------------------------------------------------- near-miss safety

    @Test
    fun `partial character match does not trigger identity attack filter`() {
        val payload = makePayload(interpretation = "白雲飄過天空。")
        val result = filter.validate(payload)
        assertTrue(result.passed)
        assertTrue(result.violations.isEmpty())
    }

    @Test
    fun `mei shiyong is not a false positive for mei yong`() {
        val payload = makePayload(domain = "wealth", interpretation = "取消一個過去一個月沒使用的訂閱。")
        assertTrue(filter.validate(payload).passed)
    }

    @Test
    fun `rendered report with zero-width bypass is still replaced`() {
        val report = RenderedReport("wealth", Tone.SCIENTIFIC, "建議買​入這支基金。")
        val filtered = filter.filter(report, "zh-TW")
        assertNotEquals(report.text, filtered.text)
    }
}
