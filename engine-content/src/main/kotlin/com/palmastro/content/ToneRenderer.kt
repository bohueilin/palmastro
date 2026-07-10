package com.palmastro.content

import com.palmastro.contracts.RenderedReport
import com.palmastro.contracts.SemanticPayload
import com.palmastro.contracts.Tone
import com.palmastro.contracts.interfaces.Renderer

/**
 * Renders a [SemanticPayload] to a plain-text report (no HTML) in the
 * payload's language, applying tone prefixes/labels from the versioned
 * template library (PRD §19, §45, §50).
 */
class ToneRenderer(
    private val templates: ContentTemplates = ContentTemplates.default()
) : Renderer {

    override fun render(payload: SemanticPayload, tone: Tone): RenderedReport {
        val lang = templates.resolveLanguage(payload.language)
        val toneTemplate = templates.tones[tone.name]
        val prefix = toneTemplate?.let { templates.localized(it.interpretationPrefix, lang) } ?: ""
        val blindspotLabel = toneTemplate?.let { templates.localized(it.blindspotLabel, lang) } ?: ""
        val labels = templates.labels
        val domainName = templates.domains[payload.domain]?.displayName
            ?.let { templates.localized(it, lang) }
            ?.takeIf { it.isNotBlank() }
            ?: payload.domain
        val grade = labels.grades[payload.scoreCard.grade]
            ?.let { templates.localized(it, lang) }
            ?.takeIf { it.isNotBlank() }
            ?: payload.scoreCard.grade

        val lines = buildList {
            if (grade.isBlank()) {
                add("$domainName — ${payload.scoreCard.totalScore}/100")
            } else {
                add("$domainName — ${payload.scoreCard.totalScore}/100 · $grade")
            }
            add("")
            add("$prefix${payload.interpretation.pattern}")
            if (payload.interpretation.trigger.isNotBlank()) add(payload.interpretation.trigger)
            if (payload.interpretation.cost.isNotBlank()) add(payload.interpretation.cost)
            if (payload.blindspot.isNotBlank()) {
                add("")
                add("$blindspotLabel${payload.blindspot}")
            }
            add("")
            add("${templates.localized(labels.actionToday, lang)}${payload.actionToday}")
            add("${templates.localized(labels.actionWeek, lang)}${payload.actionWeek}")
            add("")
            add("${templates.localized(labels.prompt, lang)}${payload.prompt}")
            payload.safetyNotes.forEach { note ->
                add("")
                add("${templates.localized(labels.safetyNote, lang)}$note")
            }
        }

        return RenderedReport(domain = payload.domain, tone = tone, text = lines.joinToString("\n"))
    }
}
