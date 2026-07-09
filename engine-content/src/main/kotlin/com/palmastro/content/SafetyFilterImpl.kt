package com.palmastro.content

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.SafetyCheckResult
import com.palmastro.contracts.interfaces.SafetyFilter
import java.text.Normalizer

class SafetyFilterImpl : SafetyFilter {

    private val wealthProhibited = listOf(
        "買入", "賣出", "股票", "保證獲利", "投資建議", "買進", "放空",
        "期貨", "基金推薦", "穩賺", "報酬率", "年化收益",
        "guaranteed return", "investment advice", "financial advice",
        "buy stock", "sell stock", "trading signal", "guaranteed profit",
        "get rich", "guaranteed income", "insider tip", "pump and dump",
    )

    private val healthProhibited = listOf(
        "糖尿病", "癌症", "診斷", "服用", "藥物", "處方", "治療方案",
        "高血壓", "心臟病", "憂鬱症", "焦慮症", "病症", "症狀",
        "diagnosis", "treatment plan", "medication", "disease prediction",
        "prescribe", "medical advice", "clinical", "prognosis",
        "you have", "you suffer from", "your condition", "take medication",
        "stop taking", "cure", "terminal", "malignant",
    )

    private val identityAttacks = listOf(
        "你很爛", "你沒救了", "廢物", "白痴", "你一定會", "你就是",
        "沒用", "失敗者", "你完了", "放棄吧", "你不行", "笨蛋",
        "you are worthless", "you are hopeless", "give up",
        "you will fail", "you are stupid", "you are pathetic",
        "loser", "waste of", "you deserve",
    )

    private val safeFallback = "This content has been adjusted for safety. Please see other suggestions."

    private val zeroWidthPattern = Regex("[\\u200B\\u200C\\u200D\\uFEFF\\u00AD]")

    private fun normalize(text: String): String {
        val stripped = zeroWidthPattern.replace(text, "")
        val nfc = Normalizer.normalize(stripped, Normalizer.Form.NFC)
        val sb = StringBuilder(nfc.length)
        for (ch in nfc) {
            val code = ch.code
            if (code in 0xFF01..0xFF5E) {
                sb.append((code - 0xFEE0).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    override fun validate(payload: SemanticPayload): SafetyCheckResult {
        val violations = mutableListOf<String>()
        val rawText = listOf(
            payload.interpretationZh, payload.blindspotZh,
            payload.actionTodayZh, payload.actionWeekZh, payload.promptZh,
        ).joinToString(" ")
        val allText = normalize(rawText)

        if (payload.domain == "wealth") {
            for (term in wealthProhibited) {
                if (allText.contains(normalize(term), ignoreCase = true)) {
                    violations.add("wealth_prohibited: $term")
                }
            }
        }

        if (payload.domain == "health") {
            for (term in healthProhibited) {
                if (allText.contains(normalize(term), ignoreCase = true)) {
                    violations.add("health_prohibited: $term")
                }
            }
        }

        for (term in healthProhibited) {
            if (allText.contains(normalize(term), ignoreCase = true) && payload.domain != "health") {
                violations.add("cross_domain_health: $term")
            }
        }

        for (term in identityAttacks) {
            if (allText.contains(normalize(term), ignoreCase = true)) {
                violations.add("identity_attack: $term")
            }
        }

        return SafetyCheckResult(passed = violations.isEmpty(), violations = violations)
    }

    override fun filter(rendered: RenderedReport): RenderedReport {
        val normalizedHtml = normalize(rendered.htmlZh)
        val prohibited = when (rendered.domain) {
            "wealth" -> wealthProhibited
            "health" -> healthProhibited
            else -> emptyList()
        } + healthProhibited + identityAttacks

        for (term in prohibited) {
            if (normalizedHtml.contains(normalize(term), ignoreCase = true)) {
                return rendered.copy(htmlZh = safeFallback)
            }
        }

        return rendered
    }
}
