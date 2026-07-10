package com.palmastro.scoring

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.ScoringEngine

class ScoringEngineImpl(
    private val ruleset: Ruleset = Ruleset.default()
) : ScoringEngine {

    private val domains = listOf("career", "wealth", "family", "health")
    private val baseline = 50

    override fun score(input: ScoringInput): ScoringResult {
        val palmSignals = SignalResolver.resolvePalmSignals(input.palmFeatures, ruleset)
        val astroSignals = SignalResolver.resolveAstroSignals(input.astroResult, ruleset)

        val confMultiplier = ruleset.confidenceMultipliers[input.palmFeatures.confidence] ?: 0.5

        val domainScores = mutableMapOf<String, Int>()
        val explainability = mutableListOf<ExplainEntry>()

        for (domain in domains) {
            var score = baseline.toDouble()

            for (signal in palmSignals) {
                if (meetsConfidence(signal.minConfidence, input.palmFeatures.confidence)) {
                    val weight = signal.domainWeights[domain] ?: 0.0
                    val contribution = signal.direction * signal.magnitude * weight * confMultiplier
                    score += contribution
                    if (weight > 0.3) {
                        explainability.add(ExplainEntry(signal.signalId, "${signal.signalId} → $domain", contribution))
                    }
                }
            }

            for ((signal, astro) in astroSignals) {
                val astroConf = ruleset.confidenceMultipliers[astro.confidence] ?: 0.5
                val weight = signal.domainWeights[domain] ?: 0.0
                val contribution = signal.direction * signal.magnitude * weight * astroConf
                score += contribution
                if (weight > 0.3) {
                    explainability.add(ExplainEntry(signal.signalId, "${signal.signalId} → $domain", contribution))
                }
            }

            domainScores[domain] = score.toInt().coerceIn(0, 100)
        }

        val overallScore = domainScores.values.average().toInt()
        val grade = assignGrade(overallScore)
        val confidence = minConfidence(input.palmFeatures.confidence, astroConfidence(input.astroResult))
        val reasons = buildConfidenceReasons(input)

        val sortedExplain = explainability
            .distinctBy { "${it.signalId}-${it.mapping}" }
            .sortedByDescending { kotlin.math.abs(it.contribution) }

        return ScoringResult(
            domainScores = domainScores,
            subdimScores = emptyMap(),
            grade = grade,
            confidence = confidence,
            confidenceReasons = reasons,
            explainability = sortedExplain,
            matchedBuckets = emptyList(),
            rulesetVersion = ruleset.version
        )
    }

    private fun assignGrade(score: Int): String {
        for ((grade, range) in ruleset.gradeIntRanges) {
            if (score in range) return grade
        }
        return ruleset.gradeIntRanges.keys.last()
    }

    private fun meetsConfidence(required: String, actual: String): Boolean {
        val order = mapOf("low" to 0, "med" to 1, "high" to 2)
        return (order[actual] ?: 0) >= (order[required] ?: 0)
    }

    private fun astroConfidence(astro: AstroResult): String =
        if (astro.calcLevel == CalcLevel.L2) "high" else "med"

    private fun minConfidence(a: String, b: String): String {
        val order = mapOf("low" to 0, "med" to 1, "high" to 2)
        return if ((order[a] ?: 0) <= (order[b] ?: 0)) a else b
    }

    private fun buildConfidenceReasons(input: ScoringInput): List<String> {
        val reasons = mutableListOf<String>()
        if (input.palmFeatures.confidence == "low") reasons.add("scan_quality_low")
        if (input.palmFeatures.featureCoverage < 0.5f) reasons.add("low_feature_coverage")
        if (input.astroResult.calcLevel == CalcLevel.L1) reasons.add("missing_birth_time")
        return reasons
    }
}
