package com.palmastro.scoring

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class GradeRange(val min: Int, val max: Int) {
    fun toIntRange(): IntRange = min..max
}

@Serializable
data class SignalDefinition(
    val signalId: String,
    val source: String,
    val direction: Int,
    val magnitude: Int,
    val minConfidence: String,
    val domainWeights: Map<String, Double>,
    val safetyTag: String
)

@Serializable
data class Ruleset(
    val version: String,
    val signals: List<SignalDefinition>,
    val gradeThresholds: Map<String, GradeRange>,
    val confidenceMultipliers: Map<String, Double>
) {
    val gradeIntRanges: Map<String, IntRange>
        get() = gradeThresholds.mapValues { it.value.toIntRange() }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

        fun default(): Ruleset {
            val resource = Ruleset::class.java.getResourceAsStream("/default-ruleset.json")
            if (resource != null) {
                return fromJson(resource.bufferedReader().readText())
            }
            return Ruleset(
                version = "1.0.0",
                signals = defaultSignals(),
                gradeThresholds = mapOf(
                    "Watchout" to GradeRange(0, 35),
                    "Building" to GradeRange(36, 55),
                    "Stable" to GradeRange(56, 75),
                    "Growing" to GradeRange(76, 100)
                ),
                confidenceMultipliers = mapOf("high" to 1.0, "med" to 0.8, "low" to 0.5)
            )
        }

        fun fromJson(jsonString: String): Ruleset = json.decodeFromString(jsonString)
        fun toJson(ruleset: Ruleset): String = json.encodeToString(ruleset)

        private fun defaultSignals() = listOf(
            SignalDefinition("PALM_HEADLINE_LONG_CLEAR", "PALM", 1, 4, "med",
                mapOf("career" to 0.9, "wealth" to 0.6, "family" to 0.2, "health" to 0.2), "SAFE_CAREER"),
            SignalDefinition("PALM_HEARTLINE_STRONG", "PALM", 1, 3, "med",
                mapOf("career" to 0.2, "wealth" to 0.2, "family" to 0.8, "health" to 0.5), "SAFE_FAMILY"),
            SignalDefinition("PALM_LIFELINE_CLEAR", "PALM", 1, 3, "med",
                mapOf("career" to 0.3, "wealth" to 0.3, "family" to 0.3, "health" to 0.9), "SAFE_HEALTH_SOFT_ONLY"),
            SignalDefinition("PALM_FATELINE_STRONG", "PALM", 1, 4, "med",
                mapOf("career" to 0.8, "wealth" to 0.7, "family" to 0.2, "health" to 0.2), "SAFE_CAREER"),
            SignalDefinition("ASTRO_SUN_FIRE", "ASTRO", 1, 2, "low",
                mapOf("career" to 0.5, "wealth" to 0.3, "family" to 0.3, "health" to 0.4), "SAFE_GENERAL"),
            SignalDefinition("ASTRO_SATURN_STRONG", "ASTRO", 1, 3, "med",
                mapOf("career" to 0.8, "wealth" to 0.5, "family" to 0.2, "health" to 0.3), "SAFE_CAREER"),
            SignalDefinition("ASTRO_JUPITER_STRONG", "ASTRO", 1, 3, "med",
                mapOf("career" to 0.4, "wealth" to 0.8, "family" to 0.5, "health" to 0.3), "SAFE_WEALTH_SOFT_ONLY"),
        )
    }
}
