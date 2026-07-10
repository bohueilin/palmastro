package com.palmastro.content

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Per-language smoke test (PRD §19, §43): every supported content language
 * composes complete payloads for all domains, every payload passes the safety
 * filter, and every rendered tone passes the output filter.
 */
class LocalizationSmokeTest {
    private val composer = ContentComposerImpl()
    private val renderer = ToneRenderer()
    private val safetyFilter = SafetyFilterImpl()

    private fun makeInput(language: String, scores: Map<String, Int>) = ContentInput(
        scoringResult = ScoringResult(
            domainScores = scores,
            subdimScores = emptyMap(),
            grade = "Stable", confidence = "med", confidenceReasons = listOf("one_hand_only"),
            explainability = listOf(
                ExplainEntry("PALM_HEADLINE_LONG_CLEAR", "PALM_HEADLINE_LONG_CLEAR → career", 3.6),
                ExplainEntry("ASTRO_JUPITER_STRONG", "ASTRO_JUPITER_STRONG → wealth", 2.1),
                ExplainEntry("PALM_HEARTLINE_STRONG", "PALM_HEARTLINE_STRONG → family", 2.4),
                ExplainEntry("PALM_LIFELINE_FAINT", "PALM_LIFELINE_FAINT → health", -1.2),
            ),
            matchedBuckets = emptyList(), rulesetVersion = "1.0.0"
        ),
        deltaResult = null, tone = Tone.SCIENTIFIC, entitlements = emptySet(),
        calcLevel = CalcLevel.L1, monthKey = "2026-07", language = language
    )

    // Exercise every bucket across the smoke runs.
    private val scoreSets = listOf(
        mapOf("career" to 90, "wealth" to 70, "family" to 55, "health" to 40),
        mapOf("career" to 10, "wealth" to 35, "family" to 65, "health" to 100),
    )

    @Test
    fun `every supported language composes complete and safe payloads`() {
        for (language in ContentTemplates.default().languages) {
            for (scores in scoreSets) {
                val payloads = composer.compose(makeInput(language, scores))
                assertEquals(4, payloads.size, "[$language] missing domains")
                for ((domain, payload) in payloads) {
                    assertEquals(language, payload.language, "[$language/$domain] language not honored")
                    assertTrue(payload.interpretation.pattern.isNotBlank(), "[$language/$domain] pattern blank")
                    assertTrue(payload.interpretation.trigger.isNotBlank(), "[$language/$domain] trigger blank")
                    assertTrue(payload.interpretation.cost.isNotBlank(), "[$language/$domain] cost blank")
                    assertTrue(payload.blindspot.isNotBlank(), "[$language/$domain] blindspot blank")
                    assertTrue(payload.actionToday.isNotBlank(), "[$language/$domain] actionToday blank")
                    assertTrue(payload.actionWeek.isNotBlank(), "[$language/$domain] actionWeek blank")
                    assertTrue(payload.prompt.isNotBlank(), "[$language/$domain] prompt blank")
                    payload.observations.forEach {
                        assertTrue(it.displayName.isNotBlank(), "[$language/$domain] observation name blank")
                        assertTrue(it.evidenceSummary.isNotBlank(), "[$language/$domain] observation evidence blank")
                    }

                    val check = safetyFilter.validate(payload)
                    assertTrue(check.passed, "[$language/$domain] safety violations: ${check.violations}")
                }
            }
        }
    }

    @Test
    fun `every supported language renders safe output in all tones`() {
        for (language in ContentTemplates.default().languages) {
            val payloads = composer.compose(makeInput(language, scoreSets.first()))
            for ((domain, payload) in payloads) {
                for (tone in Tone.values()) {
                    val rendered = renderer.render(payload, tone)
                    assertTrue(rendered.text.isNotBlank(), "[$language/$domain/$tone] blank output")
                    assertEquals(
                        rendered, safetyFilter.filter(rendered, language),
                        "[$language/$domain/$tone] default copy was filtered"
                    )
                }
            }
        }
    }

    @Test
    fun `wealth and health payloads keep safety notes in every language`() {
        for (language in ContentTemplates.default().languages) {
            val payloads = composer.compose(makeInput(language, scoreSets.first()))
            assertTrue(payloads["wealth"]!!.safetyNotes.isNotEmpty(), "[$language] wealth notes missing")
            assertTrue(payloads["health"]!!.safetyNotes.isNotEmpty(), "[$language] health notes missing")
        }
    }
}
