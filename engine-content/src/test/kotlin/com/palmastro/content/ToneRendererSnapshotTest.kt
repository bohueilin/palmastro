package com.palmastro.content

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToneRendererSnapshotTest {
    private val renderer = ToneRenderer()

    private fun makePayload(language: String = "zh-TW") = SemanticPayload(
        domain = "career", monthKey = "2026-05", calcLevel = CalcLevel.L2,
        confidence = "high", language = language, observations = emptyList(),
        interpretation = Interpretation("你的事業運勢穩定上升。", "動能出現在新的責任上。", "留意過度承諾。"),
        blindspot = "注意人際關係。",
        actionToday = "完成重要報告。", actionWeek = "主動聯繫合作夥伴。",
        prompt = "你的目標清楚嗎？", safetyNotes = listOf("此分析僅供參考。"),
        explainability = emptyList(), scoreCard = ScoreCard(78, "Growing", null, null, emptyMap())
    )

    @Test
    fun `render returns domain and tone`() {
        val report = renderer.render(makePayload(), Tone.SCIENTIFIC)
        assertEquals("career", report.domain)
        assertEquals(Tone.SCIENTIFIC, report.tone)
        assertTrue(report.text.isNotBlank())
    }

    @Test
    fun `rendered text is plain text without html tags`() {
        for (tone in Tone.values()) {
            val text = renderer.render(makePayload(), tone).text
            assertFalse(text.contains("<") || text.contains(">"), "$tone output contains markup: $text")
        }
    }

    @Test
    fun `SCIENTIFIC interpretation has no prefix`() {
        val text = renderer.render(makePayload(), Tone.SCIENTIFIC).text
        assertTrue(text.contains("你的事業運勢穩定上升。"))
        assertFalse(text.contains("親愛的，"))
        assertFalse(text.contains("直說了："))
    }

    @Test
    fun `SCIENTIFIC blindspot label`() =
        assertTrue(renderer.render(makePayload(), Tone.SCIENTIFIC).text.contains("盲點：注意人際關係。"))

    @Test
    fun `HEALING prefixed interpretation`() =
        assertTrue(renderer.render(makePayload(), Tone.HEALING).text.contains("親愛的，你的事業運勢穩定上升。"))

    @Test
    fun `HEALING gentle blindspot label`() =
        assertTrue(renderer.render(makePayload(), Tone.HEALING).text.contains("溫柔提醒：注意人際關係。"))

    @Test
    fun `ROAST_SAFE prefixed interpretation`() =
        assertTrue(renderer.render(makePayload(), Tone.ROAST_SAFE).text.contains("直說了：你的事業運勢穩定上升。"))

    @Test
    fun `ROAST_SAFE blunt blindspot label`() =
        assertTrue(renderer.render(makePayload(), Tone.ROAST_SAFE).text.contains("你不想聽但該聽的：注意人際關係。"))

    @Test
    fun `score line shows localized domain and grade`() {
        val text = renderer.render(makePayload(), Tone.SCIENTIFIC).text
        assertTrue(text.startsWith("事業 — 78/100 · 成長"))
    }

    @Test
    fun `actions prompt and safety notes carry localized labels`() {
        val text = renderer.render(makePayload(), Tone.SCIENTIFIC).text
        assertTrue(text.contains("今日行動：完成重要報告。"))
        assertTrue(text.contains("本週行動：主動聯繫合作夥伴。"))
        assertTrue(text.contains("反思提問：你的目標清楚嗎？"))
        assertTrue(text.contains("提醒：此分析僅供參考。"))
    }

    @Test
    fun `interpretation trigger and cost are rendered`() {
        val text = renderer.render(makePayload(), Tone.SCIENTIFIC).text
        assertTrue(text.contains("動能出現在新的責任上。"))
        assertTrue(text.contains("留意過度承諾。"))
    }

    @Test
    fun `english payload renders english labels`() {
        val payload = makePayload(language = "en").copy(
            interpretation = Interpretation("Career momentum is steady.", "", ""),
            blindspot = "Watch overcommitment.",
            actionToday = "Send one compliment.", actionWeek = "Take one stretch task.",
            prompt = "What would you redesign?", safetyNotes = emptyList(),
        )
        val text = renderer.render(payload, Tone.HEALING).text
        assertTrue(text.startsWith("Career — 78/100 · Growing"))
        assertTrue(text.contains("Take a breath. Career momentum is steady."))
        assertTrue(text.contains("A gentle reminder: Watch overcommitment."))
        assertTrue(text.contains("Today: Send one compliment."))
        assertTrue(text.contains("This week: Take one stretch task."))
        assertTrue(text.contains("Reflection: What would you redesign?"))
    }

    @Test
    fun `all tones render same domain`() {
        val payload = makePayload()
        val domains = Tone.values().map { renderer.render(payload, it).domain }.toSet()
        assertEquals(setOf("career"), domains)
    }
}

/**
 * Golden snapshots (PRD §50): 4 domains x en/zh-TW x 3 tones, composed from a
 * fixed input and rendered, compared against src/test/resources/golden/*.txt.
 */
class GoldenReportSnapshotTest {
    private val composer = ContentComposerImpl()
    private val renderer = ToneRenderer()

    private fun goldenInput(language: String) = ContentInput(
        scoringResult = ScoringResult(
            domainScores = mapOf("career" to 72, "wealth" to 58, "family" to 65, "health" to 40),
            subdimScores = mapOf("career.focus" to 75),
            grade = "Stable", confidence = "high", confidenceReasons = listOf("full_scan"),
            explainability = listOf(
                ExplainEntry("PALM_HEADLINE_LONG_CLEAR", "PALM_HEADLINE_LONG_CLEAR → career", 3.6),
                ExplainEntry("ASTRO_JUPITER_STRONG", "ASTRO_JUPITER_STRONG → wealth", 2.1),
                ExplainEntry("PALM_HEARTLINE_STRONG", "PALM_HEARTLINE_STRONG → family", 2.4),
                ExplainEntry("PALM_LIFELINE_CLEAR", "PALM_LIFELINE_CLEAR → health", 1.8),
            ),
            matchedBuckets = emptyList(), rulesetVersion = "2.0.0"
        ),
        deltaResult = null, tone = Tone.SCIENTIFIC, entitlements = emptySet(),
        calcLevel = CalcLevel.L2, monthKey = "2026-07", language = language
    )

    private fun golden(name: String): String =
        javaClass.getResourceAsStream("/golden/$name")?.bufferedReader()?.readText()?.trimEnd('\n')
            ?: error("missing golden file $name")

    @Test
    fun `rendered reports match golden snapshots`() {
        for (language in listOf("en", "zh-TW")) {
            val payloads = composer.compose(goldenInput(language))
            for (domain in Domains.ALL) {
                for (tone in Tone.values()) {
                    val rendered = renderer.render(payloads[domain]!!, tone)
                    val name = "report_${domain}_${language}_${tone.name.lowercase()}.txt"
                    assertEquals(golden(name), rendered.text.trimEnd('\n'), "snapshot mismatch: $name")
                }
            }
        }
    }

    @Test
    fun `golden reports pass the safety filter unchanged`() {
        val filter = SafetyFilterImpl()
        for (language in listOf("en", "zh-TW")) {
            val payloads = composer.compose(goldenInput(language))
            for (domain in Domains.ALL) {
                for (tone in Tone.values()) {
                    val rendered = renderer.render(payloads[domain]!!, tone)
                    assertEquals(rendered, filter.filter(rendered, language), "$domain/$language/$tone was filtered")
                }
            }
        }
    }
}
