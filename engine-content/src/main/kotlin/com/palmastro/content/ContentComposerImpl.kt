package com.palmastro.content

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.ContentComposer

class ContentComposerImpl : ContentComposer {

    private val domainSafetyNotes = mapOf(
        "wealth" to listOf("本內容僅供自我成長參考，不構成任何投資建議。"),
        "health" to listOf("本內容僅供自我觀察參考，不構成任何醫療診斷或建議。")
    )

    override fun compose(input: ContentInput): Map<String, SemanticPayload> {
        val domains = listOf("career", "wealth", "family", "health")
        return domains.associateWith { domain ->
            val score = input.scoringResult.domainScores[domain] ?: 50
            val grade = input.scoringResult.grade
            val relevantExplain = input.scoringResult.explainability
                .filter { it.mappingZh.contains(domain) }

            val observations = relevantExplain.take(3).map { entry ->
                Observation(entry.signalId, entry.signalId, entry.mappingZh)
            }

            val delta = input.deltaResult?.domainDeltas?.get(domain)

            SemanticPayload(
                domain = domain,
                monthKey = input.monthKey,
                calcLevel = input.calcLevel,
                confidence = input.scoringResult.confidence,
                observations = observations,
                interpretationZh = generateInterpretation(domain, score),
                blindspotZh = generateBlindspot(domain),
                actionTodayZh = generateActionToday(domain),
                actionWeekZh = generateActionWeek(domain),
                promptZh = generatePrompt(domain),
                safetyNotesZh = domainSafetyNotes[domain] ?: emptyList(),
                explainability = relevantExplain,
                scoreCard = ScoreCard(
                    totalScore = score,
                    grade = grade,
                    delta = delta,
                    comparabilityScore = input.deltaResult?.comparabilityScore,
                    subdims = input.scoringResult.subdimScores
                        .filter { it.key.startsWith("$domain.") }
                )
            )
        }
    }

    private fun generateInterpretation(domain: String, score: Int): String = when (domain) {
        "career" -> if (score >= 70) "你的職業能量正處於穩定上升期。" else "職業方面目前正在累積能量。"
        "wealth" -> if (score >= 70) "財務規劃意識強，持續保持。" else "建議關注日常開支的覺察。"
        "family" -> if (score >= 70) "家庭關係穩固，溝通順暢。" else "嘗試更多主動的關心表達。"
        "health" -> if (score >= 70) "身心狀態良好，保持現有節奏。" else "注意休息與壓力管理。"
        else -> ""
    }

    private fun generateBlindspot(domain: String): String = when (domain) {
        "career" -> "過度追求效率可能忽略了團隊合作的重要性。"
        "wealth" -> "安全感需求可能導致過於保守的選擇。"
        "family" -> "忙碌可能讓你忽略家人的小小需求。"
        "health" -> "忽視微小的身體訊號可能累積成問題。"
        else -> ""
    }

    private fun generateActionToday(domain: String): String = when (domain) {
        "career" -> "今天花15分鐘規劃本週最重要的任務。"
        "wealth" -> "今天記錄三筆支出並思考其必要性。"
        "family" -> "今天主動和一位家人分享你的近況。"
        "health" -> "今天做5分鐘的深呼吸練習。"
        else -> ""
    }

    private fun generateActionWeek(domain: String): String = when (domain) {
        "career" -> "本週嘗試每天早晨做一件最困難的事。"
        "wealth" -> "本週整理一次你的固定開支清單。"
        "family" -> "本週安排一次與家人的專注互動時間。"
        "health" -> "本週每天至少步行20分鐘。"
        else -> ""
    }

    private fun generatePrompt(domain: String): String = when (domain) {
        "career" -> "你覺得自己最近在哪方面的決策最有信心？"
        "wealth" -> "上週最讓你安心的一筆花費是什麼？"
        "family" -> "你最近最想感謝家中的哪一個人？"
        "health" -> "你的身體最近最常給你什麼訊號？"
        else -> ""
    }
}
