package com.palmastro.content

import com.palmastro.contracts.RenderedReport
import com.palmastro.contracts.SemanticPayload
import com.palmastro.contracts.interfaces.SafetyCheckResult
import com.palmastro.contracts.interfaces.SafetyFilter
import java.text.Normalizer

/**
 * Rule-driven safety filter (PRD §30-§32). Rules load from the versioned
 * `/safety-rules.json`; every category is enforced on every domain
 * (strict_safety): medical/investment/guaranteed-money claims are blocked
 * cross-domain, and self-harm / identity attacks / fear-fate claims /
 * profanity are always blocked.
 *
 * Matching: NFC normalization + zero-width stripping + fullwidth folding,
 * then zh terms as substrings and en terms as word-boundary regex patterns
 * (kills "cure"-in-"secure" and "you have"-in-"you haven't" false positives).
 */
class SafetyFilterImpl(
    private val rules: SafetyRules = SafetyRules.default(),
    private val templates: ContentTemplates = ContentTemplates.default(),
) : SafetyFilter {

    private class CompiledCategory(
        val id: String,
        val zhTerms: List<String>,
        val enPatterns: List<Pair<String, Regex>>,
    )

    private val compiled: List<CompiledCategory> = rules.categories.map { category ->
        CompiledCategory(
            id = category.id,
            zhTerms = category.zh.map { normalize(it) },
            enPatterns = category.en.map { pattern ->
                // Explicit ASCII lookarounds, NOT \b: Java's \b is Unicode-aware and treats
                // CJK ideographs as word chars, so \b never matches at a CJK↔Latin seam
                // (e.g. "buy stocks現在"). ASCII lookarounds keep the cure-in-"secure"
                // false-positive protection while catching EN terms inside Chinese text.
                pattern to Regex("(?<![a-zA-Z0-9_])(?:$pattern)(?![a-zA-Z0-9_])", RegexOption.IGNORE_CASE)
            },
        )
    }

    /** Scans every text field of the payload (Appendix B fields + observations + notes). */
    override fun validate(payload: SemanticPayload): SafetyCheckResult {
        val fields = buildList {
            add(payload.interpretation.pattern)
            add(payload.interpretation.trigger)
            add(payload.interpretation.cost)
            add(payload.blindspot)
            add(payload.actionToday)
            add(payload.actionWeek)
            add(payload.prompt)
            payload.observations.forEach {
                add(it.displayName)
                add(it.evidenceSummary)
            }
            addAll(payload.safetyNotes)
        }
        val violations = fields.flatMap { scan(it) }.distinct()
        return SafetyCheckResult(passed = violations.isEmpty(), violations = violations)
    }

    override fun filter(rendered: RenderedReport): RenderedReport =
        filter(rendered, templates.defaultLanguage)

    /**
     * Language-aware overload: replaces a violating report with the localized
     * safe fallback text. The interface method delegates here with "en";
     * the app pipeline should pass the payload's language.
     */
    fun filter(rendered: RenderedReport, language: String): RenderedReport {
        if (scan(rendered.text).isEmpty()) return rendered
        val lang = templates.resolveLanguage(language)
        return rendered.copy(text = templates.localized(templates.fallback.filteredText, lang))
    }

    private fun scan(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val normalized = normalize(text)
        val violations = mutableListOf<String>()
        for (category in compiled) {
            for (term in category.zhTerms) {
                if (normalized.contains(term, ignoreCase = true)) {
                    violations.add("${category.id}: $term")
                }
            }
            for ((raw, regex) in category.enPatterns) {
                if (regex.containsMatchIn(normalized)) {
                    violations.add("${category.id}: $raw")
                }
            }
        }
        return violations
    }

    private fun normalize(text: String): String {
        val stripped = ZERO_WIDTH.replace(text, "")
        val nfc = Normalizer.normalize(stripped, Normalizer.Form.NFC)
        val sb = StringBuilder(nfc.length)
        for (ch in nfc) {
            val code = ch.code
            if (code in FULLWIDTH_RANGE) {
                sb.append((code - FULLWIDTH_OFFSET).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    companion object {
        private val ZERO_WIDTH = Regex("[\\u200B\\u200C\\u200D\\uFEFF\\u00AD]")
        private val FULLWIDTH_RANGE = 0xFF01..0xFF5E
        private const val FULLWIDTH_OFFSET = 0xFEE0
    }
}
