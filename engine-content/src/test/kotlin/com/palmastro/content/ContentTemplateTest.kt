package com.palmastro.content

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentTemplateTest {

    private val languages = listOf("en", "zh-TW", "zh-CN", "ja", "hi")

    @Test
    fun `default library is version 2 with five languages`() {
        val templates = ContentTemplates.default()
        assertEquals("2.1.0", templates.version)
        assertEquals(languages, templates.languages)
        assertEquals("en", templates.defaultLanguage)
    }

    @Test
    fun `default library has all 4 domains`() {
        assertEquals(setOf("career", "wealth", "family", "health"), ContentTemplates.default().domains.keys)
    }

    @Test
    fun `every domain field bucket and language has non-blank copy`() {
        val templates = ContentTemplates.default()
        for ((domain, t) in templates.domains) {
            val fields = mapOf(
                "interpretation.pattern" to t.interpretation.pattern,
                "interpretation.trigger" to t.interpretation.trigger,
                "interpretation.cost" to t.interpretation.cost,
                "blindspot" to t.blindspot,
                "actionToday" to t.actionToday,
                "actionWeek" to t.actionWeek,
                "prompt" to t.prompt,
            )
            for ((name, field) in fields) {
                assertTrue(field.isNotEmpty(), "$domain.$name has no buckets")
                for ((bucket, localized) in field) {
                    assertTrue(
                        templates.buckets.containsKey(bucket),
                        "$domain.$name references unknown bucket $bucket"
                    )
                    for (lang in languages) {
                        assertTrue(
                            !localized[lang].isNullOrBlank(),
                            "$domain.$name[$bucket][$lang] is blank"
                        )
                    }
                }
            }
            for (lang in languages) {
                assertTrue(!t.displayName[lang].isNullOrBlank(), "$domain.displayName[$lang] blank")
            }
        }
    }

    @Test
    fun `every score maps to a bucket for every field`() {
        val templates = ContentTemplates.default()
        for ((domain, t) in templates.domains) {
            for (score in 0..100) {
                assertTrue(
                    templates.bucketText(t.interpretation.pattern, score, "en").isNotBlank(),
                    "$domain pattern uncovered at score $score"
                )
                assertTrue(
                    templates.bucketText(t.actionToday, score, "en").isNotBlank(),
                    "$domain actionToday uncovered at score $score"
                )
            }
        }
    }

    @Test
    fun `wealth and health carry localized safety notes`() {
        val templates = ContentTemplates.default()
        for (domain in listOf("wealth", "health")) {
            for (lang in languages) {
                val notes = templates.localizedList(templates.domains[domain]!!.safetyNotes, lang)
                assertTrue(notes.isNotEmpty(), "$domain safetyNotes[$lang] empty")
            }
        }
    }

    @Test
    fun `all three tones have localized prefixes and labels`() {
        val templates = ContentTemplates.default()
        assertEquals(setOf("SCIENTIFIC", "HEALING", "ROAST_SAFE"), templates.tones.keys)
        for ((tone, t) in templates.tones) {
            for (lang in languages) {
                assertTrue(t.interpretationPrefix.containsKey(lang), "$tone prefix missing [$lang]")
                assertTrue(!t.blindspotLabel[lang].isNullOrBlank(), "$tone blindspotLabel blank [$lang]")
            }
        }
    }

    @Test
    fun `observation templates exist for known signals in all languages`() {
        val templates = ContentTemplates.default()
        val expected = listOf(
            "PALM_HEADLINE_LONG_CLEAR", "PALM_HEARTLINE_STRONG",
            "PALM_LIFELINE_CLEAR", "PALM_FATELINE_STRONG",
            "ASTRO_SUN_FIRE", "ASTRO_SATURN_STRONG", "ASTRO_JUPITER_STRONG",
        )
        for (signalId in expected) {
            val t = templates.observations[signalId]
                ?: error("missing observation template for $signalId")
            for (lang in languages) {
                assertTrue(!t.displayName[lang].isNullOrBlank(), "$signalId displayName[$lang] blank")
                assertTrue(!t.evidenceSummary[lang].isNullOrBlank(), "$signalId evidence[$lang] blank")
            }
        }
    }

    @Test
    fun `roundtrip to json and back preserves the library`() {
        val original = ContentTemplates.default()
        assertEquals(original, ContentTemplates.fromJson(original.toJson()))
    }

    @Test
    fun `fromResource parses the shipped resource`() {
        val templates = ContentTemplates.fromResource()
        assertEquals("2.1.0", templates.version)
        assertEquals(4, templates.domains.size)
    }

    // ------------------------------------------------------------- injection

    private fun localizedAll(text: String): LocalizedText =
        listOf("en", "zh-TW", "zh-CN", "ja", "hi").associateWith { "$text-$it" }

    private fun customDomain(tag: String) = DomainTemplate(
        displayName = localizedAll("${tag}_NAME"),
        interpretation = InterpretationTemplate(
            pattern = mapOf("high" to localizedAll("${tag}_PATTERN_HIGH"), "low" to localizedAll("${tag}_PATTERN_LOW")),
            trigger = mapOf("high" to localizedAll("${tag}_TRIGGER_HIGH"), "low" to localizedAll("${tag}_TRIGGER_LOW")),
            cost = mapOf("high" to localizedAll("${tag}_COST_HIGH"), "low" to localizedAll("${tag}_COST_LOW")),
        ),
        blindspot = mapOf("high" to localizedAll("${tag}_BLIND_HIGH"), "low" to localizedAll("${tag}_BLIND_LOW")),
        actionToday = mapOf("high" to localizedAll("${tag}_TODAY_HIGH"), "low" to localizedAll("${tag}_TODAY_LOW")),
        actionWeek = mapOf("high" to localizedAll("${tag}_WEEK_HIGH"), "low" to localizedAll("${tag}_WEEK_LOW")),
        prompt = mapOf("high" to localizedAll("${tag}_PROMPT_HIGH"), "low" to localizedAll("${tag}_PROMPT_LOW")),
        safetyNotes = if (tag == "W" || tag == "H") mapOf("en" to listOf("${tag}_NOTE")) else emptyMap(),
    )

    private fun customTemplates() = ContentTemplates(
        version = "custom-9.9.9",
        defaultLanguage = "en",
        languages = listOf("en", "zh-TW", "zh-CN", "ja", "hi"),
        buckets = mapOf(
            "high" to ScoreBucket(65, 100),
            "low" to ScoreBucket(0, 64),
        ),
        domains = mapOf(
            "career" to customDomain("C"),
            "wealth" to customDomain("W"),
            "family" to customDomain("F"),
            "health" to customDomain("H"),
        ),
        observations = mapOf(
            "PALM_HEADLINE_LONG_CLEAR" to ObservationTemplate(
                displayName = localizedAll("OBS_NAME"),
                evidenceSummary = localizedAll("OBS_EVIDENCE"),
            )
        ),
        observationFallbackEvidence = localizedAll("OBS_FALLBACK"),
    )

    private fun makeInput(language: String = "en") = ContentInput(
        scoringResult = ScoringResult(
            domainScores = mapOf("career" to 80, "wealth" to 40, "family" to 50, "health" to 90),
            subdimScores = emptyMap(),
            grade = "Stable", confidence = "high", confidenceReasons = emptyList(),
            explainability = listOf(
                ExplainEntry("PALM_HEADLINE_LONG_CLEAR", "PALM_HEADLINE_LONG_CLEAR → career", 3.6)
            ),
            matchedBuckets = emptyList(), rulesetVersion = "1.0.0"
        ),
        deltaResult = null, tone = Tone.SCIENTIFIC, entitlements = emptySet(),
        calcLevel = CalcLevel.L2, monthKey = "2026-05", language = language
    )

    @Test
    fun `composer uses injected custom templates per score bucket`() {
        val composer = ContentComposerImpl(customTemplates())
        val payloads = composer.compose(makeInput())

        assertEquals("C_PATTERN_HIGH-en", payloads["career"]!!.interpretation.pattern)
        assertEquals("C_TRIGGER_HIGH-en", payloads["career"]!!.interpretation.trigger)
        assertEquals("C_COST_HIGH-en", payloads["career"]!!.interpretation.cost)
        assertEquals("C_WEEK_HIGH-en", payloads["career"]!!.actionWeek)
        assertEquals("W_PATTERN_LOW-en", payloads["wealth"]!!.interpretation.pattern)
        assertEquals("W_PROMPT_LOW-en", payloads["wealth"]!!.prompt)
        assertEquals("F_BLIND_LOW-en", payloads["family"]!!.blindspot)
        assertEquals("H_TODAY_HIGH-en", payloads["health"]!!.actionToday)
        assertEquals(listOf("W_NOTE"), payloads["wealth"]!!.safetyNotes)
        assertEquals("custom-9.9.9", composer.templatesVersion)
    }

    @Test
    fun `composer localizes injected custom templates`() {
        val composer = ContentComposerImpl(customTemplates())
        val payloads = composer.compose(makeInput(language = "zh-TW"))
        assertEquals("C_PATTERN_HIGH-zh-TW", payloads["career"]!!.interpretation.pattern)
        assertEquals("OBS_NAME-zh-TW", payloads["career"]!!.observations.single().displayName)
    }

    @Test
    fun `missing domain in custom templates falls back to blank copy`() {
        val partial = customTemplates().copy(domains = mapOf("career" to customDomain("C")))
        val composer = ContentComposerImpl(partial)
        val payloads = composer.compose(makeInput())

        assertEquals("C_PATTERN_HIGH-en", payloads["career"]!!.interpretation.pattern)
        assertEquals(4, payloads.size)
        assertEquals("", payloads["wealth"]!!.interpretation.pattern)
        assertEquals("", payloads["wealth"]!!.blindspot)
        assertEquals("", payloads["family"]!!.actionToday)
        assertTrue(payloads["health"]!!.safetyNotes.isEmpty())
    }

    @Test
    fun `domain placeholder is substituted with localized display name`() {
        val withPlaceholder = customTemplates().let { base ->
            base.copy(
                domains = base.domains + ("career" to customDomain("C").copy(
                    interpretation = InterpretationTemplate(
                        pattern = mapOf("high" to mapOf("en" to "Signals for {domain} are strong.")),
                        trigger = mapOf("high" to mapOf("en" to "t")),
                        cost = mapOf("high" to mapOf("en" to "c")),
                    ),
                    displayName = mapOf("en" to "Career"),
                ))
            )
        }
        val payloads = ContentComposerImpl(withPlaceholder).compose(makeInput())
        assertEquals("Signals for Career are strong.", payloads["career"]!!.interpretation.pattern)
    }
}
