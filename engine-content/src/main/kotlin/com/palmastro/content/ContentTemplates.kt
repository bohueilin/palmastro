package com.palmastro.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ContentTemplates(
    val version: String,
    val domains: Map<String, DomainTemplate>,
) {
    fun toJson(): String = json.encodeToString(this)

    companion object {
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

        fun default(): ContentTemplates = ContentTemplates(
            version = "1.0.0",
            domains = mapOf(
                "career" to DomainTemplate(
                    interpretationHigh = "Your career energy is in a strong upward phase.",
                    interpretationLow = "Your career is currently in a building phase.",
                    blindspot = "Chasing efficiency may cause you to overlook teamwork.",
                    actionToday = "Spend 15 minutes planning your top task for the week.",
                    actionWeek = "Tackle your hardest task first thing each morning.",
                    prompt = "What decision have you felt most confident about recently?",
                ),
                "wealth" to DomainTemplate(
                    interpretationHigh = "Your financial awareness is strong — keep it up.",
                    interpretationLow = "Pay more attention to your daily spending habits.",
                    blindspot = "The need for security may lead to overly conservative choices.",
                    actionToday = "Record three expenses today and reflect on each.",
                    actionWeek = "Review your fixed expenses list this week.",
                    prompt = "What was your most reassuring purchase last week?",
                    safetyNotes = listOf("For personal growth only — not investment advice."),
                ),
                "family" to DomainTemplate(
                    interpretationHigh = "Family relationships are solid with smooth communication.",
                    interpretationLow = "Try expressing care more proactively.",
                    blindspot = "Being busy may cause you to miss small family needs.",
                    actionToday = "Share something about your day with a family member.",
                    actionWeek = "Schedule focused quality time with family this week.",
                    prompt = "Who in your family would you most like to thank?",
                ),
                "health" to DomainTemplate(
                    interpretationHigh = "Your physical and mental state is good — maintain your rhythm.",
                    interpretationLow = "Focus on rest and stress management.",
                    blindspot = "Ignoring small body signals may let them accumulate.",
                    actionToday = "Do a 5-minute deep breathing exercise today.",
                    actionWeek = "Walk at least 20 minutes every day this week.",
                    prompt = "What signal has your body been giving you most recently?",
                    safetyNotes = listOf("For self-observation only — not medical advice."),
                ),
            ),
        )

        fun fromJson(json: String): ContentTemplates = this.json.decodeFromString(json)

        fun fromResource(path: String = "/default-templates.json"): ContentTemplates {
            val text = ContentTemplates::class.java.getResourceAsStream(path)
                ?.bufferedReader()?.readText()
                ?: throw IllegalStateException("Resource not found: $path")
            return fromJson(text)
        }
    }
}

@Serializable
data class DomainTemplate(
    val interpretationHigh: String,
    val interpretationLow: String,
    val blindspot: String,
    val actionToday: String,
    val actionWeek: String,
    val prompt: String,
    val safetyNotes: List<String> = emptyList(),
)
