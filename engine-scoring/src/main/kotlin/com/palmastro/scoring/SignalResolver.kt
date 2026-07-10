package com.palmastro.scoring

import com.palmastro.contracts.*

/**
 * Maps categorical [PalmFeatures] (extractor v2 vocabulary) and [AstroResult]
 * signals onto ruleset signal definitions.
 *
 * Palm clarity vocabulary (must stay aligned with PalmFeatureExtractorImpl):
 * "clear" | "medium" | "faint" | "broken" | "thin" (heartline only) |
 * "unclear" (absent lines). Densities: "high" | "med" | "low".
 */
object SignalResolver {

    private val POSITIVE_CLARITIES = setOf("clear", "medium")

    fun resolvePalmSignals(features: PalmFeatureResult, ruleset: Ruleset): List<SignalDefinition> {
        val f = features.features
        val matchedIds = mutableListOf<String>()

        // Positive signals.
        if (f.headlinePresent && f.headlineClarity == "clear" && f.headlineLength == "long") {
            matchedIds += "PALM_HEADLINE_LONG_CLEAR"
        }
        if (f.heartlinePresent && f.heartlineClarity in POSITIVE_CLARITIES) {
            matchedIds += "PALM_HEARTLINE_STRONG"
        }
        if (f.lifelinePresent && f.lifelineClarity in POSITIVE_CLARITIES) {
            matchedIds += "PALM_LIFELINE_CLEAR"
        }
        if (f.fatelinePresent && f.fatelineClarity == "clear") {
            matchedIds += "PALM_FATELINE_STRONG"
        }

        // Negative signals (ruleset v2).
        if (f.headlinePresent && f.headlineClarity == "broken") {
            matchedIds += "PALM_HEADLINE_CHAINED"
        }
        if (f.fatelinePresent && f.fatelineClarity == "broken") {
            matchedIds += "PALM_FATELINE_BREAKS"
        }
        if (f.heartlinePresent && f.heartlineClarity == "thin") {
            matchedIds += "PALM_HEARTLINE_THIN"
        }
        if (f.lifelinePresent && f.lifelineClarity == "faint") {
            matchedIds += "PALM_LIFELINE_FAINT"
        }
        if (f.minorLineDensity == "high") {
            matchedIds += "PALM_MINOR_LINES_DENSE"
        }

        return matchedIds.mapNotNull { id -> ruleset.signals.find { it.signalId == id } }
    }

    fun resolveAstroSignals(astro: AstroResult, ruleset: Ruleset): List<Pair<SignalDefinition, AstroSignal>> {
        return astro.signals.mapNotNull { signal ->
            ruleset.signals.find { it.signalId == signal.signalId }?.let { def -> def to signal }
        }
    }
}
