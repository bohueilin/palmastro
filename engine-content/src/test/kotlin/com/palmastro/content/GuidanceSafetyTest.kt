package com.palmastro.content

import com.palmastro.contracts.CalcLevel
import com.palmastro.contracts.ContentInput
import com.palmastro.contracts.ExplainEntry
import com.palmastro.contracts.ScoringResult
import com.palmastro.contracts.Tone
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * PRD §12.3, §30-§32: every string the guidance layer can emit must pass the
 * safety pipeline — no disease, no investment, no fate-doom, no fear framing.
 * Scans (a) the full guidance template vocabulary in every shipped language
 * and (b) composed Guidance objects for representative payload sets in
 * en + zh-TW, field by field, through [SafetyFilterImpl.scanText].
 */
class GuidanceSafetyTest {
    private val templates = ContentTemplates.default()
    private val filter = SafetyFilterImpl()
    private val composer = ContentComposerImpl()
    private val builder = GuidanceBuilder()

    private fun assertClean(context: String, text: String) {
        val violations = filter.scanText(text)
        assertTrue(violations.isEmpty(), "$context violates safety rules $violations: \"$text\"")
    }

    private fun scanCopy(context: String, copy: GuidanceCopy) {
        mapOf("title" to copy.title, "body" to copy.body, "action" to copy.action)
            .forEach { (field, localized) ->
                localized.forEach { (lang, text) -> assertClean("$context.$field[$lang]", text) }
            }
    }

    // ------------------------------------------------ template-level scan

    @Test
    fun `every guidance template string passes the safety scan in every language`() {
        val guidance = templates.guidance
        for ((signalId, entry) in guidance.signals) {
            entry.leanInto?.let { scanCopy("signals.$signalId.leanInto", it) }
            entry.mindfulOf?.let { scanCopy("signals.$signalId.mindfulOf", it) }
        }
        for ((domain, entry) in guidance.domains) {
            entry.strengths.forEach { (bucket, copy) -> scanCopy("domains.$domain.strengths.$bucket", copy) }
            entry.mindful.forEach { (bucket, copy) -> scanCopy("domains.$domain.mindful.$bucket", copy) }
            entry.monthPlan.forEach { (key, line) ->
                line.forEach { (lang, text) -> assertClean("domains.$domain.monthPlan.$key[$lang]", text) }
            }
        }
        guidance.monthTheme.forEach { (grade, line) ->
            line.forEach { (lang, text) -> assertClean("monthTheme.$grade[$lang]", text) }
        }
    }

    // ------------------------------------------------ composed-output scan

    private fun entry(signalId: String, domain: String, contribution: Double) =
        ExplainEntry(signalId, "$signalId → $domain", contribution)

    private fun makeInput(
        scores: Map<String, Int>,
        explain: List<ExplainEntry>,
        language: String,
        grade: String,
    ) = ContentInput(
        scoringResult = ScoringResult(
            domainScores = scores, subdimScores = emptyMap(),
            grade = grade, confidence = "med", confidenceReasons = emptyList(),
            explainability = explain, matchedBuckets = emptyList(), rulesetVersion = "2.0.0",
        ),
        deltaResult = null, tone = Tone.HEALING, entitlements = emptySet(),
        calcLevel = CalcLevel.L1, monthKey = "2026-07", language = language,
    )

    /** Signal-rich, bucket-fallback, and negative-heavy readings across grades. */
    private fun representativeInputs(language: String): Map<String, ContentInput> = mapOf(
        "signal_rich" to makeInput(
            scores = mapOf("career" to 78, "wealth" to 70, "family" to 66, "health" to 58),
            explain = listOf(
                entry("PALM_HEADLINE_LONG_CLEAR", "career", 3.6),
                entry("PALM_FATELINE_STRONG", "wealth", 2.8),
                entry("PALM_HEARTLINE_STRONG", "family", 2.4),
                entry("ASTRO_SUN_FIRE", "career", 1.2),
                entry("PALM_LIFELINE_FAINT", "health", -4.9),
                entry("PALM_MINOR_LINES_DENSE", "health", -3.8),
                entry("PALM_HEADLINE_CHAINED", "career", -4.3),
            ),
            language = language, grade = "Growing",
        ),
        "bucket_high_low" to makeInput(
            scores = mapOf("career" to 90, "wealth" to 70, "family" to 55, "health" to 40),
            explain = emptyList(), language = language, grade = "Stable",
        ),
        "bucket_extremes" to makeInput(
            scores = mapOf("career" to 10, "wealth" to 35, "family" to 65, "health" to 100),
            explain = emptyList(), language = language, grade = "Watchout",
        ),
        "negative_heavy" to makeInput(
            scores = mapOf("career" to 30, "wealth" to 28, "family" to 33, "health" to 25),
            explain = listOf(
                entry("PALM_FATELINE_BREAKS", "career", -4.3),
                entry("PALM_HEARTLINE_THIN", "family", -4.3),
                entry("PALM_LIFELINE_FAINT", "health", -4.3),
                entry("PALM_HEADLINE_CHAINED", "career", -3.8),
            ),
            language = language, grade = "Watchout",
        ),
    )

    @Test
    fun `composed guidance passes the safety scan for representative readings`() {
        for (language in listOf("en", "zh-TW")) {
            for ((name, input) in representativeInputs(language)) {
                val payloads = composer.compose(input)
                val guidance = builder.build(payloads, input.scoringResult.grade, language)
                val context = "$name[$language]"

                assertTrue(guidance.monthTheme.isNotBlank(), "$context monthTheme blank")
                assertClean("$context.monthTheme", guidance.monthTheme)
                (guidance.strengths + guidance.mindful).forEachIndexed { index, item ->
                    assertClean("$context.item[$index].title(${item.domain})", item.title)
                    assertClean("$context.item[$index].body(${item.domain})", item.body)
                    assertClean("$context.item[$index].action(${item.domain})", item.action)
                }
                guidance.weekPlan.forEachIndexed { index, line ->
                    assertClean("$context.weekPlan[$index]", line)
                }
            }
        }
    }

    @Test
    fun `mindful items always pair the pointer with a concrete micro-action`() {
        for (language in listOf("en", "zh-TW")) {
            for ((name, input) in representativeInputs(language)) {
                val guidance = builder.build(composer.compose(input), input.scoringResult.grade, language)
                assertTrue(guidance.mindful.size in 2..3, "$name[$language] mindful count ${guidance.mindful.size}")
                guidance.mindful.forEach { item ->
                    assertTrue(item.action.isNotBlank(), "$name[$language] ${item.domain} mindful lacks an action")
                }
            }
        }
    }
}
