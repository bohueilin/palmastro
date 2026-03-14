package com.palmastro.content

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.Renderer

class ToneRenderer : Renderer {

    override fun render(payload: SemanticPayload, tone: Tone): RenderedReport {
        val html = buildString {
            append("<div class=\"report ${tone.name.lowercase()}\">")
            append("<h2>${payload.domain}</h2>")
            append("<div class=\"score\">${payload.scoreCard.totalScore} / 100 — ${payload.scoreCard.grade}</div>")

            when (tone) {
                Tone.SCIENTIFIC -> {
                    append("<p class=\"interpretation\">${payload.interpretationZh}</p>")
                    append("<p class=\"blindspot\">盲點：${payload.blindspotZh}</p>")
                }
                Tone.HEALING -> {
                    append("<p class=\"interpretation\">親愛的，${payload.interpretationZh}</p>")
                    append("<p class=\"blindspot\">溫柔提醒：${payload.blindspotZh}</p>")
                }
                Tone.ROAST_SAFE -> {
                    append("<p class=\"interpretation\">直說了：${payload.interpretationZh}</p>")
                    append("<p class=\"blindspot\">你不想聽但該聽的：${payload.blindspotZh}</p>")
                }
            }

            append("<div class=\"actions\">")
            append("<p>今日行動：${payload.actionTodayZh}</p>")
            append("<p>本週行動：${payload.actionWeekZh}</p>")
            append("</div>")

            append("<p class=\"prompt\">反思提問：${payload.promptZh}</p>")

            payload.safetyNotesZh.forEach { note ->
                append("<p class=\"safety\">⚠️ $note</p>")
            }

            append("</div>")
        }

        return RenderedReport(domain = payload.domain, tone = tone, htmlZh = html)
    }
}
