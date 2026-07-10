package com.palmastro.content

import com.palmastro.contracts.CalcLevel
import com.palmastro.contracts.ContentInput
import com.palmastro.contracts.Domains
import com.palmastro.contracts.ExplainEntry
import com.palmastro.contracts.Interpretation
import com.palmastro.contracts.Observation
import com.palmastro.contracts.ScoreCard
import com.palmastro.contracts.SemanticPayload
import com.palmastro.contracts.interfaces.ContentComposer

/**
 * Deterministic template-driven composer (PRD §19, §50, Appendix B).
 *
 * All copy comes from the injected [ContentTemplates] (default:
 * `/content-templates.json`, version [templatesVersion]). `compose(input)` is
 * the only composition API; it honors [ContentInput.language] and falls back
 * to the template default language ("en") for unsupported requests.
 */
class ContentComposerImpl(
    private val templates: ContentTemplates = ContentTemplates.default()
) : ContentComposer {

    /** Persist this alongside results instead of a hardcoded literal. */
    val templatesVersion: String get() = templates.version

    override fun compose(input: ContentInput): Map<String, SemanticPayload> {
        val language = templates.resolveLanguage(input.language)
        return Domains.ALL.associateWith { domain -> composeDomain(input, domain, language) }
    }

    /**
     * Engine-provided safe payload the app substitutes when `validate()` fails
     * (EXECUTION_SPEC safety pipeline). Pass the failing payload as [base] to
     * preserve its scoreCard / monthKey / calcLevel metadata.
     */
    fun safeFallbackPayload(
        domain: String,
        language: String,
        base: SemanticPayload? = null,
    ): SemanticPayload {
        val lang = templates.resolveLanguage(language)
        val f = templates.fallback
        return SemanticPayload(
            domain = domain,
            monthKey = base?.monthKey ?: "",
            calcLevel = base?.calcLevel ?: CalcLevel.L1,
            confidence = base?.confidence ?: "low",
            confidenceReasons = emptyList(),
            language = lang,
            observations = emptyList(),
            interpretation = Interpretation(
                pattern = templates.localized(f.interpretationPattern, lang),
                trigger = templates.localized(f.interpretationTrigger, lang),
                cost = templates.localized(f.interpretationCost, lang),
            ),
            blindspot = templates.localized(f.blindspot, lang),
            actionToday = templates.localized(f.actionToday, lang),
            actionWeek = templates.localized(f.actionWeek, lang),
            prompt = templates.localized(f.prompt, lang),
            safetyNotes = templates.domains[domain]
                ?.let { templates.localizedList(it.safetyNotes, lang) }
                ?: emptyList(),
            explainability = emptyList(),
            scoreCard = base?.scoreCard ?: ScoreCard(0, "", null, null, emptyMap()),
        )
    }

    private fun composeDomain(input: ContentInput, domain: String, language: String): SemanticPayload {
        val score = input.scoringResult.domainScores[domain] ?: DEFAULT_SCORE
        val template = templates.domains[domain]
        val displayName = template?.displayName
            ?.let { templates.localized(it, language) }
            ?.takeIf { it.isNotBlank() }
            ?: domain
        val domainExplain = input.scoringResult.explainability.filter { it.mapping.contains(domain) }
        val delta = input.deltaResult?.domainDeltas?.get(domain)

        fun text(field: Map<String, LocalizedText>?): String =
            field?.let { templates.bucketText(it, score, language) }
                ?.replace(DOMAIN_PLACEHOLDER, displayName)
                ?: ""

        return SemanticPayload(
            domain = domain,
            monthKey = input.monthKey,
            calcLevel = input.calcLevel,
            confidence = input.scoringResult.confidence,
            confidenceReasons = input.scoringResult.confidenceReasons,
            language = language,
            observations = domainExplain.take(MAX_OBSERVATIONS).map { observation(it, language) },
            interpretation = Interpretation(
                pattern = text(template?.interpretation?.pattern),
                trigger = text(template?.interpretation?.trigger),
                cost = text(template?.interpretation?.cost),
            ),
            blindspot = text(template?.blindspot),
            actionToday = text(template?.actionToday),
            actionWeek = text(template?.actionWeek),
            prompt = text(template?.prompt),
            safetyNotes = template?.let { templates.localizedList(it.safetyNotes, language) }
                ?: emptyList(),
            explainability = domainExplain,
            scoreCard = ScoreCard(
                totalScore = score,
                grade = input.scoringResult.grade,
                delta = delta,
                comparabilityScore = input.deltaResult?.comparabilityScore,
                subdims = input.scoringResult.subdimScores.filterKeys { it.startsWith("$domain.") },
            ),
        )
    }

    private fun observation(entry: ExplainEntry, language: String): Observation {
        val template = templates.observations[entry.signalId]
        val displayName = template?.let { templates.localized(it.displayName, language) }
            ?.takeIf { it.isNotBlank() }
            ?: humanize(entry.signalId)
        val evidence = template?.let { templates.localized(it.evidenceSummary, language) }
            ?.takeIf { it.isNotBlank() }
            ?: templates.localized(templates.observationFallbackEvidence, language)
        return Observation(entry.signalId, displayName, evidence)
    }

    /** Stable-key transformation for unknown signal ids (no display copy in code). */
    private fun humanize(signalId: String): String = signalId
        .removePrefix("PALM_").removePrefix("ASTRO_")
        .split("_")
        .joinToString(" ") { part -> part.lowercase().replaceFirstChar { it.uppercase() } }

    companion object {
        private const val DEFAULT_SCORE = 50
        private const val MAX_OBSERVATIONS = 3
        private const val DOMAIN_PLACEHOLDER = "{domain}"
    }
}
