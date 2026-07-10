package com.palmastro.scoring

import com.palmastro.contracts.Domains
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

    /**
     * Validates the ruleset invariants (PRD section 49: validated on app
     * startup). Returns this ruleset on success so callers can chain
     * `Ruleset.default().validateOrThrow()`.
     *
     * Invariants:
     * - version present, signal list non-empty, no duplicate signal IDs;
     * - every signal weights all four domains, weights in 0..1,
     *   direction is +1/-1, magnitude >= 1;
     * - every domain has at least one reachable positive AND one reachable
     *   negative contribution;
     * - confidence multipliers cover high/med/low and are in 0..1;
     * - grade thresholds cover 0..100 gap-free with no overlap.
     *
     * Failure messages are stable machine-readable keys (not UI strings).
     */
    fun validateOrThrow(): Ruleset {
        require(version.isNotBlank()) { "ruleset_version_blank" }
        require(signals.isNotEmpty()) { "ruleset_signals_empty" }

        val ids = signals.map { it.signalId }
        require(ids.size == ids.toSet().size) { "ruleset_duplicate_signal_ids" }

        val requiredDomains = Domains.ALL.toSet()
        signals.forEach { signal ->
            require(signal.domainWeights.keys.containsAll(requiredDomains)) {
                "ruleset_signal_missing_domain:${signal.signalId}"
            }
            signal.domainWeights.values.forEach { weight ->
                require(weight in 0.0..1.0) { "ruleset_domain_weight_out_of_range:${signal.signalId}" }
            }
            require(signal.direction == 1 || signal.direction == -1) {
                "ruleset_invalid_direction:${signal.signalId}"
            }
            require(signal.magnitude >= 1) { "ruleset_invalid_magnitude:${signal.signalId}" }
        }

        Domains.ALL.forEach { domain ->
            require(signals.any { it.direction > 0 && (it.domainWeights[domain] ?: 0.0) > 0.0 }) {
                "ruleset_domain_missing_positive_signal:$domain"
            }
            require(signals.any { it.direction < 0 && (it.domainWeights[domain] ?: 0.0) > 0.0 }) {
                "ruleset_domain_missing_negative_signal:$domain"
            }
        }

        require(confidenceMultipliers.keys.containsAll(setOf("high", "med", "low"))) {
            "ruleset_missing_confidence_levels"
        }
        confidenceMultipliers.values.forEach { multiplier ->
            require(multiplier in 0.0..1.0) { "ruleset_confidence_multiplier_out_of_range" }
        }

        require(gradeThresholds.isNotEmpty()) { "ruleset_grade_thresholds_empty" }
        val ranges = gradeThresholds.values.sortedBy { it.min }
        ranges.forEach { range -> require(range.min <= range.max) { "ruleset_grade_range_inverted" } }
        require(ranges.first().min == 0) { "ruleset_grades_must_start_at_0" }
        require(ranges.last().max == 100) { "ruleset_grades_must_end_at_100" }
        for (i in 0 until ranges.size - 1) {
            require(ranges[i].max + 1 == ranges[i + 1].min) { "ruleset_grade_thresholds_not_contiguous" }
        }

        return this
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

        /**
         * Loads the bundled default ruleset. The JSON resource is the single
         * source of truth (cross-platform per PRD section 49); a missing
         * resource indicates a corrupt build and fails fast.
         */
        fun default(): Ruleset {
            val resource = Ruleset::class.java.getResourceAsStream("/default-ruleset.json")
                ?: error("ruleset_resource_missing")
            return fromJson(resource.bufferedReader().readText())
        }

        fun fromJson(jsonString: String): Ruleset = json.decodeFromString(jsonString)
        fun toJson(ruleset: Ruleset): String = json.encodeToString(ruleset)
    }
}
