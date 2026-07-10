package com.palmastro.content

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SafetyFilterTest {
    private val filter = SafetyFilterImpl()

    private fun makePayload(
        domain: String = "career",
        interpretation: String = "你的職業能量正處於穩定上升期。",
        trigger: String = "",
        cost: String = "",
        blindspot: String = "注意效率。",
        actionToday: String = "規劃任務。",
        actionWeek: String = "做困難的事。",
        prompt: String = "你有信心嗎？",
        observations: List<Observation> = emptyList(),
        safetyNotes: List<String> = emptyList(),
    ) = SemanticPayload(
        domain = domain, monthKey = "2026-03", calcLevel = CalcLevel.L2,
        confidence = "high", observations = observations,
        interpretation = Interpretation(interpretation, trigger, cost),
        blindspot = blindspot,
        actionToday = actionToday, actionWeek = actionWeek,
        prompt = prompt, safetyNotes = safetyNotes,
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
        val result = filter.validate(makePayload("wealth", "建議買入台積電股票，保證獲利。"))
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("investment_advice") })
        assertTrue(result.violations.any { it.startsWith("guaranteed_money") })
    }

    @Test
    fun `validate catches medical claims in health domain`() {
        val result = filter.validate(makePayload("health", "你可能有糖尿病的風險，建議服用降血糖藥物。"))
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("medical_diagnosis") })
        assertTrue(result.violations.any { it.startsWith("treatment") })
    }

    @Test
    fun `validate catches identity attacks`() {
        val result = filter.validate(makePayload(interpretation = "你很爛，你沒救了。"))
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("identity_attack") })
    }

    @Test
    fun `validate catches fear fate claims`() {
        val result = filter.validate(makePayload(interpretation = "你的事業註定失敗，厄運將至。"))
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("fear_fate_claims") })
    }

    @Test
    fun `validate catches self harm content`() {
        val result = filter.validate(makePayload(interpretation = "覺得活不下去的時候更要撐住。"))
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("self_harm") })
    }

    @Test
    fun `validate catches profanity`() {
        val result = filter.validate(makePayload(interpretation = "他媽的，這個月很難。"))
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("profanity") })
    }

    @Test
    fun `validate catches disease prediction`() {
        val result = filter.validate(makePayload("health", "掌紋顯示你會生病。"))
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("disease_prediction") })
    }

    @Test
    fun `validate scans trigger and cost fields`() {
        val result = filter.validate(makePayload(trigger = "股票下跌時要買入。", cost = ""))
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.contains("買入") })
    }

    @Test
    fun `validate scans observation fields`() {
        val observation = Observation("PALM_X", "保證獲利的紋路", "這代表穩賺")
        val result = filter.validate(makePayload(observations = listOf(observation)))
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("guaranteed_money") })
    }

    @Test
    fun `validate scans safety notes`() {
        val result = filter.validate(makePayload(safetyNotes = listOf("記得服用藥物。")))
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.startsWith("treatment") })
    }

    @Test
    fun `default composed safety notes pass validation`() {
        val composer = ContentComposerImpl()
        val input = ContentInput(
            scoringResult = ScoringResult(
                domainScores = mapOf("career" to 72, "wealth" to 58, "family" to 65, "health" to 55),
                subdimScores = emptyMap(), grade = "Stable", confidence = "high",
                confidenceReasons = emptyList(), explainability = emptyList(),
                matchedBuckets = emptyList(), rulesetVersion = "1.0.0"
            ),
            deltaResult = null, tone = Tone.SCIENTIFIC, entitlements = emptySet(),
            calcLevel = CalcLevel.L2, monthKey = "2026-03", language = "zh-TW"
        )
        for ((domain, payload) in composer.compose(input)) {
            val check = filter.validate(payload)
            assertTrue(check.passed, "$domain violated: ${check.violations}")
        }
    }

    @Test
    fun `filter replaces prohibited content in rendered report`() {
        val report = RenderedReport("wealth", Tone.SCIENTIFIC, "建議買入股票保證獲利")
        val filtered = filter.filter(report)
        assertNotEquals(report.text, filtered.text)
        assertTrue(filter.filter(filtered).text == filtered.text, "replacement text must be safe")
    }

    @Test
    fun `filter keeps safe rendered report unchanged`() {
        val report = RenderedReport("career", Tone.HEALING, "本月適合穩健地累積能量。")
        assertEquals(report, filter.filter(report))
    }

    @Test
    fun `filter localizes the replacement text`() {
        val report = RenderedReport("wealth", Tone.SCIENTIFIC, "保證獲利！")
        val zh = filter.filter(report, "zh-TW")
        val en = filter.filter(report, "en")
        assertNotEquals(report.text, zh.text)
        assertNotEquals(zh.text, en.text)
        assertTrue(zh.text.contains("內容準則"))
    }

    @Test
    fun `filter catches profanity in rendered output`() {
        val report = RenderedReport("career", Tone.ROAST_SAFE, "說真的，別當個 loser。")
        assertNotEquals(report.text, filter.filter(report).text)
    }

    @Test
    fun `rules load from versioned resource`() {
        val rules = SafetyRules.default()
        assertEquals("2.0.0", rules.version)
        assertEquals(SafetyRules.CATEGORY_IDS.toSet(), rules.categories.map { it.id }.toSet())
        for (category in rules.categories) {
            assertTrue(category.zh.isNotEmpty(), "${category.id} zh list empty")
            assertTrue(category.en.isNotEmpty(), "${category.id} en list empty")
        }
    }
}
