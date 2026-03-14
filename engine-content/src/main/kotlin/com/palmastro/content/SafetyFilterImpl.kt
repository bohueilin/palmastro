package com.palmastro.content

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.SafetyCheckResult
import com.palmastro.contracts.interfaces.SafetyFilter

class SafetyFilterImpl : SafetyFilter {

    private val wealthProhibited = listOf(
        "買入", "賣出", "股票", "保證獲利", "投資建議", "買進", "放空",
        "guaranteed return", "investment advice", "buy", "sell"
    )

    private val healthProhibited = listOf(
        "糖尿病", "癌症", "診斷", "服用", "藥物", "處方", "治療方案",
        "diagnosis", "treatment", "medication", "disease prediction"
    )

    private val identityAttacks = listOf(
        "你很爛", "你沒救了", "廢物", "白痴", "你一定會", "你就是"
    )

    private val safeFallback = "（此段內容因安全審查已調整，請參考其他建議。）"

    override fun validate(payload: SemanticPayload): SafetyCheckResult {
        val violations = mutableListOf<String>()
        val allText = listOf(
            payload.interpretationZh, payload.blindspotZh,
            payload.actionTodayZh, payload.actionWeekZh, payload.promptZh
        ).joinToString(" ")

        if (payload.domain == "wealth") {
            for (term in wealthProhibited) {
                if (allText.contains(term, ignoreCase = true)) {
                    violations.add("wealth_prohibited: $term")
                }
            }
        }

        if (payload.domain == "health") {
            for (term in healthProhibited) {
                if (allText.contains(term, ignoreCase = true)) {
                    violations.add("health_prohibited: $term")
                }
            }
        }

        for (term in identityAttacks) {
            if (allText.contains(term, ignoreCase = true)) {
                violations.add("identity_attack: $term")
            }
        }

        return SafetyCheckResult(passed = violations.isEmpty(), violations = violations)
    }

    override fun filter(rendered: RenderedReport): RenderedReport {
        var html = rendered.htmlZh
        val prohibited = when (rendered.domain) {
            "wealth" -> wealthProhibited
            "health" -> healthProhibited
            else -> emptyList()
        } + identityAttacks

        for (term in prohibited) {
            if (html.contains(term, ignoreCase = true)) {
                html = safeFallback
                break
            }
        }

        return rendered.copy(htmlZh = html)
    }
}
