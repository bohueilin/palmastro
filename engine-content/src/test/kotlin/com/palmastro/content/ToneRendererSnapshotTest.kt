package com.palmastro.content

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ToneRendererSnapshotTest {
    private val renderer = ToneRenderer()

    private val testPayload = SemanticPayload(
        domain = "career", monthKey = "2026-05", calcLevel = CalcLevel.L2,
        confidence = "high", observations = emptyList(),
        interpretationZh = "你的事業運勢穩定上升。", blindspotZh = "注意人際關係。",
        actionTodayZh = "完成重要報告。", actionWeekZh = "主動聯繫合作夥伴。",
        promptZh = "你的目標清楚嗎？", safetyNotesZh = listOf("此分析僅供參考"),
        explainability = emptyList(), scoreCard = ScoreCard(78, "Growing", null, null, emptyMap())
    )

    @Test fun `SCIENTIFIC - interpretation as-is`() {
        val h = renderer.render(testPayload, Tone.SCIENTIFIC).htmlZh
        assertTrue(h.contains("你的事業運勢穩定上升。"))
        assertTrue(!h.contains("親愛的，"))
        assertTrue(!h.contains("直說了："))
    }
    @Test fun `SCIENTIFIC - blindspot prefix`() = assertTrue(renderer.render(testPayload, Tone.SCIENTIFIC).htmlZh.contains("盲點：注意人際關係。"))
    @Test fun `SCIENTIFIC - score display`() { val h = renderer.render(testPayload, Tone.SCIENTIFIC).htmlZh; assertTrue(h.contains("78 / 100")); assertTrue(h.contains("Growing")) }
    @Test fun `SCIENTIFIC - actions`() { val h = renderer.render(testPayload, Tone.SCIENTIFIC).htmlZh; assertTrue(h.contains("今日行動：完成重要報告。")); assertTrue(h.contains("本週行動：主動聯繫合作夥伴。")) }
    @Test fun `SCIENTIFIC - safety notes`() = assertTrue(renderer.render(testPayload, Tone.SCIENTIFIC).htmlZh.contains("此分析僅供參考"))
    @Test fun `SCIENTIFIC - structure`() { val h = renderer.render(testPayload, Tone.SCIENTIFIC).htmlZh; assertTrue(h.startsWith("<div class=\"report scientific\">")); assertTrue(h.endsWith("</div>")) }
    @Test fun `SCIENTIFIC - reflection prompt`() = assertTrue(renderer.render(testPayload, Tone.SCIENTIFIC).htmlZh.contains("反思提問：你的目標清楚嗎？"))

    @Test fun `HEALING - prefixed interpretation`() = assertTrue(renderer.render(testPayload, Tone.HEALING).htmlZh.contains("親愛的，你的事業運勢穩定上升。"))
    @Test fun `HEALING - gentle blindspot`() = assertTrue(renderer.render(testPayload, Tone.HEALING).htmlZh.contains("溫柔提醒：注意人際關係。"))
    @Test fun `HEALING - score display`() { val h = renderer.render(testPayload, Tone.HEALING).htmlZh; assertTrue(h.contains("78 / 100")) }
    @Test fun `HEALING - actions`() { val h = renderer.render(testPayload, Tone.HEALING).htmlZh; assertTrue(h.contains("今日行動：完成重要報告。")) }
    @Test fun `HEALING - safety notes`() = assertTrue(renderer.render(testPayload, Tone.HEALING).htmlZh.contains("此分析僅供參考"))
    @Test fun `HEALING - structure`() { val h = renderer.render(testPayload, Tone.HEALING).htmlZh; assertTrue(h.startsWith("<div class=\"report healing\">")); assertTrue(h.endsWith("</div>")) }
    @Test fun `HEALING - reflection prompt`() = assertTrue(renderer.render(testPayload, Tone.HEALING).htmlZh.contains("反思提問：你的目標清楚嗎？"))

    @Test fun `ROAST_SAFE - prefixed interpretation`() = assertTrue(renderer.render(testPayload, Tone.ROAST_SAFE).htmlZh.contains("直說了：你的事業運勢穩定上升。"))
    @Test fun `ROAST_SAFE - blunt blindspot`() = assertTrue(renderer.render(testPayload, Tone.ROAST_SAFE).htmlZh.contains("你不想聽但該聽的：注意人際關係。"))
    @Test fun `ROAST_SAFE - score display`() { val h = renderer.render(testPayload, Tone.ROAST_SAFE).htmlZh; assertTrue(h.contains("78 / 100")) }
    @Test fun `ROAST_SAFE - actions`() { val h = renderer.render(testPayload, Tone.ROAST_SAFE).htmlZh; assertTrue(h.contains("今日行動：完成重要報告。")) }
    @Test fun `ROAST_SAFE - safety notes`() = assertTrue(renderer.render(testPayload, Tone.ROAST_SAFE).htmlZh.contains("此分析僅供參考"))
    @Test fun `ROAST_SAFE - structure`() { val h = renderer.render(testPayload, Tone.ROAST_SAFE).htmlZh; assertTrue(h.startsWith("<div class=\"report roast_safe\">")); assertTrue(h.endsWith("</div>")) }
    @Test fun `ROAST_SAFE - reflection prompt`() = assertTrue(renderer.render(testPayload, Tone.ROAST_SAFE).htmlZh.contains("反思提問：你的目標清楚嗎？"))
}
