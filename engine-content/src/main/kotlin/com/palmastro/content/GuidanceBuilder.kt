package com.palmastro.content

import com.palmastro.contracts.Domains
import com.palmastro.contracts.SemanticPayload
import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * One "lean into" or "be mindful of" guidance card. [signalId] is the ruleset
 * signal that backed the card, or null when it came from the per-domain
 * bucket generic fallback.
 */
@Serializable
data class GuidanceItem(
    val domain: String,
    val signalId: String?,
    val title: String,
    val body: String,
    val action: String,
)

/**
 * The composed "Understand your reading" guidance layer for one monthly
 * result: a grade-keyed month theme, up to three strengths to lean into,
 * two-to-three gentle mindful pointers, and one weekly focus line per domain.
 */
@Serializable
data class Guidance(
    val monthTheme: String,
    val strengths: List<GuidanceItem>,
    val mindful: List<GuidanceItem>,
    val weekPlan: List<String>,
)

/**
 * Pure, deterministic guidance composer (PRD §11-§13, §30-§32). All copy
 * comes from [ContentTemplates.guidance]; derivation:
 *
 * - strengths: positive explainability contributions sorted descending
 *   (ties: domain order, then signalId) -> top [MAX_STRENGTHS] with distinct
 *   domains; short lists are backfilled with the domain bucket generic for
 *   the highest-scoring uncovered domains.
 * - mindful: negative contributions sorted by magnitude (same tie-breaks),
 *   deduplicated by signalId -> up to [MAX_MINDFUL]; backfilled to at least
 *   [MIN_MINDFUL] with bucket generics, preferring domains not already used
 *   as strengths, lowest score first.
 * - weekPlan: one focus line per domain in [Domains.ALL] order, choosing the
 *   "high"/"low" variant by `scoreCard.totalScore >= 65`.
 *
 * Tone is positivity-first and never fear-based: mindful items are gentle
 * attention-pointers with a concrete micro-action, never warnings of doom.
 */
class GuidanceBuilder(
    private val templates: ContentTemplates = ContentTemplates.default(),
) {

    private data class Candidate(
        val domain: String,
        val domainIndex: Int,
        val signalId: String,
        val contribution: Double,
    )

    fun build(
        payloads: Map<String, SemanticPayload>,
        overallGrade: String,
        language: String,
    ): Guidance {
        val lang = templates.resolveLanguage(language)
        val candidates = collectCandidates(payloads)
        val strengths = buildStrengths(candidates, payloads, lang)
        val mindful = buildMindful(candidates, payloads, strengths, lang)
        return Guidance(
            monthTheme = monthTheme(overallGrade, lang),
            strengths = strengths,
            mindful = mindful,
            weekPlan = weekPlan(payloads, lang),
        )
    }

    private fun collectCandidates(payloads: Map<String, SemanticPayload>): List<Candidate> =
        Domains.ALL.flatMapIndexed { index, domain ->
            payloads[domain]?.explainability.orEmpty()
                .filter { it.mapping.contains(domain) }
                .map { Candidate(domain, index, it.signalId, it.contribution) }
        }

    private fun buildStrengths(
        candidates: List<Candidate>,
        payloads: Map<String, SemanticPayload>,
        lang: String,
    ): List<GuidanceItem> {
        val positives = candidates.filter { it.contribution > 0 }.sortedWith(
            compareByDescending<Candidate> { it.contribution }
                .thenBy { it.domainIndex }
                .thenBy { it.signalId },
        )
        val items = pickSignalItems(positives, MAX_STRENGTHS, lang, keyFor = { it.domain }) {
            templates.guidance.signals[it.signalId]?.leanInto
        }
        val usedDomains = items.map { it.domain }.toSet()
        val backfillOrder = Domains.ALL.withIndex()
            .filter { (_, domain) -> domain !in usedDomains && domain in payloads }
            .sortedWith(
                compareByDescending<IndexedValue<String>> { score(payloads, it.value) }
                    .thenBy { it.index },
            )
            .map { it.value }
        return backfill(items, backfillOrder, MAX_STRENGTHS, lang) { domain ->
            bucketCopy(templates.guidance.domains[domain]?.strengths, score(payloads, domain))
        }
    }

    private fun buildMindful(
        candidates: List<Candidate>,
        payloads: Map<String, SemanticPayload>,
        strengths: List<GuidanceItem>,
        lang: String,
    ): List<GuidanceItem> {
        val negatives = candidates.filter { it.contribution < 0 }.sortedWith(
            compareByDescending<Candidate> { abs(it.contribution) }
                .thenBy { it.domainIndex }
                .thenBy { it.signalId },
        )
        val items = pickSignalItems(negatives, MAX_MINDFUL, lang, keyFor = { it.signalId }) {
            templates.guidance.signals[it.signalId]?.mindfulOf
        }
        val usedDomains = items.map { it.domain }.toSet()
        val strengthDomains = strengths.map { it.domain }.toSet()
        val backfillOrder = Domains.ALL.withIndex()
            .filter { (_, domain) -> domain !in usedDomains && domain in payloads }
            .sortedWith(
                compareBy<IndexedValue<String>> { it.value in strengthDomains }
                    .thenBy { score(payloads, it.value) }
                    .thenBy { it.index },
            )
            .map { it.value }
        return backfill(items, backfillOrder, MIN_MINDFUL, lang) { domain ->
            bucketCopy(templates.guidance.domains[domain]?.mindful, score(payloads, domain))
        }
    }

    /**
     * Walks [sorted] in order, keeping at most [cap] items; a candidate is
     * skipped when its [keyFor] key was already used or [copyFor] has no copy
     * for it. Keys are marked used only on a successful pick, matching the
     * derivation contract mirrored on iOS.
     */
    private fun pickSignalItems(
        sorted: List<Candidate>,
        cap: Int,
        lang: String,
        keyFor: (Candidate) -> String,
        copyFor: (Candidate) -> GuidanceCopy?,
    ): List<GuidanceItem> {
        val used = mutableSetOf<String>()
        val items = mutableListOf<GuidanceItem>()
        sorted.asSequence()
            .takeWhile { items.size < cap }
            .forEach { candidate ->
                val key = keyFor(candidate)
                if (key !in used) {
                    copyFor(candidate)?.let { copy ->
                        items += item(candidate.domain, candidate.signalId, copy, lang)
                        used += key
                    }
                }
            }
        return items
    }

    /** Appends domain-generic items from [order] until [picked] reaches [target]. */
    private fun backfill(
        picked: List<GuidanceItem>,
        order: List<String>,
        target: Int,
        lang: String,
        copyFor: (String) -> GuidanceCopy?,
    ): List<GuidanceItem> {
        val items = picked.toMutableList()
        order.asSequence()
            .takeWhile { items.size < target }
            .forEach { domain ->
                copyFor(domain)?.let { items += item(domain, null, it, lang) }
            }
        return items
    }

    private fun weekPlan(payloads: Map<String, SemanticPayload>, lang: String): List<String> =
        Domains.ALL.mapNotNull { domain ->
            val payload = payloads[domain] ?: return@mapNotNull null
            val key = if (payload.scoreCard.totalScore >= HIGH_THRESHOLD) "high" else "low"
            templates.guidance.domains[domain]?.monthPlan?.get(key)
                ?.let { templates.localized(it, lang) }
                ?.takeIf { it.isNotBlank() }
                ?.replace(DOMAIN_PLACEHOLDER, displayName(domain, lang))
        }

    private fun monthTheme(overallGrade: String, lang: String): String {
        val theme = templates.guidance.monthTheme[overallGrade]
            ?: templates.guidance.monthTheme[DEFAULT_GRADE]
            ?: emptyMap()
        return templates.localized(theme, lang)
    }

    private fun item(domain: String, signalId: String?, copy: GuidanceCopy, lang: String): GuidanceItem {
        val name = displayName(domain, lang)
        fun resolve(text: LocalizedText): String =
            templates.localized(text, lang).replace(DOMAIN_PLACEHOLDER, name)
        return GuidanceItem(
            domain = domain,
            signalId = signalId,
            title = resolve(copy.title),
            body = resolve(copy.body),
            action = resolve(copy.action),
        )
    }

    private fun displayName(domain: String, lang: String): String =
        templates.domains[domain]?.displayName
            ?.let { templates.localized(it, lang) }
            ?.takeIf { it.isNotBlank() }
            ?: domain

    private fun score(payloads: Map<String, SemanticPayload>, domain: String): Int =
        payloads[domain]?.scoreCard?.totalScore ?: DEFAULT_SCORE

    /** First bucket (in template JSON order) whose range contains [score]. */
    private fun bucketCopy(field: Map<String, GuidanceCopy>?, score: Int): GuidanceCopy? =
        field?.entries
            ?.firstOrNull { (bucketId, _) -> templates.buckets[bucketId]?.contains(score) == true }
            ?.value

    companion object {
        private const val MAX_STRENGTHS = 3
        private const val MAX_MINDFUL = 3
        private const val MIN_MINDFUL = 2
        private const val HIGH_THRESHOLD = 65
        private const val DEFAULT_SCORE = 50
        private const val DEFAULT_GRADE = "Stable"
        private const val DOMAIN_PLACEHOLDER = "{domain}"
    }
}
