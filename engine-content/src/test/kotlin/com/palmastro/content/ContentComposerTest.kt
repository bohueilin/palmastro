package com.palmastro.content

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentComposerTest {
    private val composer = ContentComposerImpl()

    private fun makeInput(tone: Tone = Tone.SCIENTIFIC, entitlements: Set<String> = emptySet()) = ContentInput(
        scoringResult = ScoringResult(
            domainScores = mapOf("career" to 72, "wealth" to 58, "family" to 65, "health" to 55),
            subdimScores = mapOf("career.focus" to 75),
            grade = "Stable", confidence = "high", confidenceReasons = emptyList(),
            explainability = listOf(ExplainEntry("PALM_HEADLINE_LONG_CLEAR", "headline → career", 3.6)),
            matchedBuckets = emptyList(), rulesetVersion = "1.0.0"
        ),
        deltaResult = null, tone = tone, entitlements = entitlements,
        calcLevel = CalcLevel.L2, monthKey = "2026-03"
    )

    @Test
    fun `compose produces payloads for all 4 domains`() {
        val payloads = composer.compose(makeInput())
        assertEquals(4, payloads.size)
        assertTrue(payloads.keys.containsAll(listOf("career", "wealth", "family", "health")))
    }

    @Test
    fun `payload contains correct domain score and grade`() {
        val payloads = composer.compose(makeInput())
        assertEquals(72, payloads["career"]!!.scoreCard.totalScore)
        assertEquals("Stable", payloads["career"]!!.scoreCard.grade)
    }

    @Test
    fun `payload month key matches input`() {
        val payloads = composer.compose(makeInput())
        assertEquals("2026-03", payloads["career"]!!.monthKey)
    }

    @Test
    fun `payload calc level matches input`() {
        val payloads = composer.compose(makeInput())
        assertEquals(CalcLevel.L2, payloads["career"]!!.calcLevel)
    }

    @Test
    fun `safety notes added for wealth domain`() {
        val payloads = composer.compose(makeInput())
        assertTrue(payloads["wealth"]!!.safetyNotesZh.isNotEmpty())
    }

    @Test
    fun `safety notes added for health domain`() {
        val payloads = composer.compose(makeInput())
        assertTrue(payloads["health"]!!.safetyNotesZh.isNotEmpty())
    }
}

class ToneRendererTest {
    private val renderer = ToneRenderer()

    private fun makePayload() = SemanticPayload(
        domain = "career", monthKey = "2026-03", calcLevel = CalcLevel.L2,
        confidence = "high", observations = listOf(
            Observation("PALM_HEADLINE_LONG_CLEAR", "清晰長頭線", "頭線清晰且長，顯示強大的思維專注力")
        ),
        interpretationZh = "你的思維清晰度高，適合策略性工作。",
        blindspotZh = "過度分析可能導致行動延遲。",
        actionTodayZh = "今天花15分鐘規劃本週最重要的任務。",
        actionWeekZh = "本週嘗試每天早晨做一件最困難的事。",
        promptZh = "你覺得自己最近在哪方面的決策最有信心？",
        safetyNotesZh = emptyList(),
        explainability = listOf(ExplainEntry("PALM_HEADLINE_LONG_CLEAR", "headline → career", 3.6)),
        scoreCard = ScoreCard(72, "Stable", null, null, mapOf("career.focus" to 75))
    )

    @Test
    fun `render scientific tone`() {
        val report = renderer.render(makePayload(), Tone.SCIENTIFIC)
        assertEquals(Tone.SCIENTIFIC, report.tone)
        assertEquals("career", report.domain)
        assertTrue(report.htmlZh.isNotBlank())
    }

    @Test
    fun `render healing tone`() {
        val report = renderer.render(makePayload(), Tone.HEALING)
        assertEquals(Tone.HEALING, report.tone)
    }

    @Test
    fun `render roast_safe tone`() {
        val report = renderer.render(makePayload(), Tone.ROAST_SAFE)
        assertEquals(Tone.ROAST_SAFE, report.tone)
    }

    @Test
    fun `all tones render same domain`() {
        val payload = makePayload()
        val s = renderer.render(payload, Tone.SCIENTIFIC)
        val h = renderer.render(payload, Tone.HEALING)
        val r = renderer.render(payload, Tone.ROAST_SAFE)
        assertEquals(s.domain, h.domain)
        assertEquals(h.domain, r.domain)
    }
}
