package com.palmastro.scoring

import com.palmastro.contracts.*

object SignalResolver {
    fun resolvePalmSignals(features: PalmFeatureResult, ruleset: Ruleset): List<SignalDefinition> {
        val f = features.features
        val matched = mutableListOf<SignalDefinition>()

        if (f.headlinePresent && f.headlineClarity == "clear" && f.headlineLength == "long") {
            ruleset.signals.find { it.signalId == "PALM_HEADLINE_LONG_CLEAR" }?.let { matched.add(it) }
        }
        if (f.heartlinePresent && f.heartlineClarity in listOf("clear", "moderate")) {
            ruleset.signals.find { it.signalId == "PALM_HEARTLINE_STRONG" }?.let { matched.add(it) }
        }
        if (f.lifelinePresent && f.lifelineClarity in listOf("clear", "moderate")) {
            ruleset.signals.find { it.signalId == "PALM_LIFELINE_CLEAR" }?.let { matched.add(it) }
        }
        if (f.fatelinePresent && f.fatelineShape == "straight") {
            ruleset.signals.find { it.signalId == "PALM_FATELINE_STRONG" }?.let { matched.add(it) }
        }

        return matched
    }

    fun resolveAstroSignals(astro: AstroResult, ruleset: Ruleset): List<Pair<SignalDefinition, AstroSignal>> {
        return astro.signals.mapNotNull { signal ->
            ruleset.signals.find { it.signalId == signal.signalId }?.let { def -> def to signal }
        }
    }
}
