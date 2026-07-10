package com.palmastro.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * One safety category (PRD §30): `zh` entries are matched as normalized
 * substrings (Traditional + Simplified variants both listed); `en` entries are
 * regex fragments compiled with word boundaries to avoid substring
 * false positives ("cure" in "secure", "you have" in "you haven't").
 */
@Serializable
data class SafetyCategory(
    val id: String,
    val zh: List<String> = emptyList(),
    val en: List<String> = emptyList(),
)

/** Versioned safety ruleset loaded from `/safety-rules.json` (PRD §30-§32, §49). */
@Serializable
data class SafetyRules(
    val version: String,
    val categories: List<SafetyCategory> = emptyList(),
) {

    fun toJson(): String = json.encodeToString(this)

    companion object {
        const val RESOURCE_PATH = "/safety-rules.json"

        val CATEGORY_IDS = listOf(
            "medical_diagnosis", "treatment", "disease_prediction",
            "investment_advice", "guaranteed_money", "fear_fate_claims",
            "self_harm", "profanity", "identity_attack",
        )

        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

        private val defaultRules: SafetyRules by lazy { fromResource() }

        fun default(): SafetyRules = defaultRules

        fun fromJson(jsonText: String): SafetyRules = json.decodeFromString(jsonText)

        fun fromResource(path: String = RESOURCE_PATH): SafetyRules {
            val text = SafetyRules::class.java.getResourceAsStream(path)
                ?.bufferedReader()?.readText()
                ?: throw IllegalStateException("Resource not found: $path")
            return fromJson(text)
        }
    }
}
