package com.palmastro.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Text keyed by BCP-47-ish language tag ("en", "zh-TW", "zh-CN", "ja", "hi"). */
typealias LocalizedText = Map<String, String>

/** List of texts keyed by language tag. */
typealias LocalizedList = Map<String, List<String>>

/** Inclusive score range a bucket id maps to (PRD §19, §50). */
@Serializable
data class ScoreBucket(val min: Int = 0, val max: Int = 100) {
    operator fun contains(score: Int): Boolean = score in min..max
}

/** PRD Appendix B: interpretation = { pattern, trigger, cost }, each score-bucketed. */
@Serializable
data class InterpretationTemplate(
    val pattern: Map<String, LocalizedText> = emptyMap(),
    val trigger: Map<String, LocalizedText> = emptyMap(),
    val cost: Map<String, LocalizedText> = emptyMap(),
)

/** Per-domain template library entry. Field maps are keyed by bucket id. */
@Serializable
data class DomainTemplate(
    val displayName: LocalizedText = emptyMap(),
    val interpretation: InterpretationTemplate = InterpretationTemplate(),
    val blindspot: Map<String, LocalizedText> = emptyMap(),
    val actionToday: Map<String, LocalizedText> = emptyMap(),
    val actionWeek: Map<String, LocalizedText> = emptyMap(),
    val prompt: Map<String, LocalizedText> = emptyMap(),
    val safetyNotes: LocalizedList = emptyMap(),
)

/** Display copy for a known signal id (PRD Appendix A). */
@Serializable
data class ObservationTemplate(
    val displayName: LocalizedText = emptyMap(),
    val evidenceSummary: LocalizedText = emptyMap(),
)

/** Safe replacement copy used when generated content fails validation (PRD §30). */
@Serializable
data class FallbackTemplate(
    val interpretationPattern: LocalizedText = emptyMap(),
    val interpretationTrigger: LocalizedText = emptyMap(),
    val interpretationCost: LocalizedText = emptyMap(),
    val blindspot: LocalizedText = emptyMap(),
    val actionToday: LocalizedText = emptyMap(),
    val actionWeek: LocalizedText = emptyMap(),
    val prompt: LocalizedText = emptyMap(),
    val filteredText: LocalizedText = emptyMap(),
)

/** Localized prefixes/labels for one tone (PRD §45). Keyed by [com.palmastro.contracts.Tone] name. */
@Serializable
data class ToneTemplate(
    val interpretationPrefix: LocalizedText = emptyMap(),
    val blindspotLabel: LocalizedText = emptyMap(),
)

/** One guidance card: short title, one-to-two-sentence body, concrete micro-action. */
@Serializable
data class GuidanceCopy(
    val title: LocalizedText = emptyMap(),
    val body: LocalizedText = emptyMap(),
    val action: LocalizedText = emptyMap(),
)

/**
 * Per-signal guidance vocabulary (PRD §11-§13): [leanInto] for what to feel
 * positive about, [mindfulOf] for gentle attention-pointers. Positive-direction
 * palm signals carry only leanInto, negative-direction ones only mindfulOf,
 * astro element/modality signals carry both (a strength plus a soft nuance).
 */
@Serializable
data class GuidanceSignalTemplate(
    val leanInto: GuidanceCopy? = null,
    val mindfulOf: GuidanceCopy? = null,
)

/**
 * Per-domain guidance: bucket-keyed generic [strengths] and [mindful] fallbacks
 * (peak/rising/transition/building/attention) used when no signal-backed entry
 * exists, plus [monthPlan] weekly focus lines keyed "high"/"low".
 */
@Serializable
data class GuidanceDomainTemplate(
    val strengths: Map<String, GuidanceCopy> = emptyMap(),
    val mindful: Map<String, GuidanceCopy> = emptyMap(),
    val monthPlan: Map<String, LocalizedText> = emptyMap(),
)

/**
 * "Understand your reading" guidance layer (templates v2.1.0, PRD §11-§13,
 * §30-§32). [monthTheme] is keyed by overall grade. Guidance copy ships
 * complete in en + zh-TW; zh-CN/ja/hi resolve through the existing chain
 * ([ContentTemplates.resolveLanguage] keeps them supported, then
 * [ContentTemplates.localized] falls back per-field to the default "en").
 */
@Serializable
data class GuidanceTemplates(
    val signals: Map<String, GuidanceSignalTemplate> = emptyMap(),
    val domains: Map<String, GuidanceDomainTemplate> = emptyMap(),
    val monthTheme: Map<String, LocalizedText> = emptyMap(),
)

/** Localized section labels used by the renderer for plain-text reports. */
@Serializable
data class ReportLabels(
    val actionToday: LocalizedText = emptyMap(),
    val actionWeek: LocalizedText = emptyMap(),
    val prompt: LocalizedText = emptyMap(),
    val safetyNote: LocalizedText = emptyMap(),
    val grades: Map<String, LocalizedText> = emptyMap(),
)

/**
 * Versioned, localized content template library (PRD §19, §50).
 * Loaded from `/content-templates.json`; all composed copy comes from here.
 */
@Serializable
data class ContentTemplates(
    val version: String,
    val defaultLanguage: String = "en",
    val languages: List<String> = listOf("en"),
    val buckets: Map<String, ScoreBucket> = emptyMap(),
    val domains: Map<String, DomainTemplate> = emptyMap(),
    val observations: Map<String, ObservationTemplate> = emptyMap(),
    val observationFallbackEvidence: LocalizedText = emptyMap(),
    val fallback: FallbackTemplate = FallbackTemplate(),
    val tones: Map<String, ToneTemplate> = emptyMap(),
    val labels: ReportLabels = ReportLabels(),
    val guidance: GuidanceTemplates = GuidanceTemplates(),
) {

    /** Supported language or the default ("en") — composer honors ContentInput.language. */
    fun resolveLanguage(requested: String): String =
        if (requested in languages) requested else defaultLanguage

    /** Localized text with fallback to the default language. */
    fun localized(text: LocalizedText, language: String): String =
        text[language] ?: text[defaultLanguage] ?: ""

    /** Localized list with fallback to the default language. */
    fun localizedList(lists: LocalizedList, language: String): List<String> =
        lists[language] ?: lists[defaultLanguage] ?: emptyList()

    /**
     * Resolves a bucketed field: first entry (in JSON order) whose bucket range
     * contains [score] wins. Deterministic; returns "" when nothing matches.
     */
    fun bucketText(field: Map<String, LocalizedText>, score: Int, language: String): String {
        val entry = field.entries.firstOrNull { (bucketId, _) ->
            buckets[bucketId]?.contains(score) == true
        } ?: return ""
        return localized(entry.value, language)
    }

    fun toJson(): String = json.encodeToString(this)

    companion object {
        const val RESOURCE_PATH = "/content-templates.json"

        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

        private val defaultTemplates: ContentTemplates by lazy { fromResource() }

        fun default(): ContentTemplates = defaultTemplates

        fun fromJson(jsonText: String): ContentTemplates = json.decodeFromString(jsonText)

        fun fromResource(path: String = RESOURCE_PATH): ContentTemplates {
            val text = ContentTemplates::class.java.getResourceAsStream(path)
                ?.bufferedReader()?.readText()
                ?: throw IllegalStateException("Resource not found: $path")
            return fromJson(text)
        }
    }
}
