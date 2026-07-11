package com.palmastro.content

import com.palmastro.contracts.CalcLevel
import com.palmastro.contracts.ContentInput
import com.palmastro.contracts.ExplainEntry
import com.palmastro.contracts.ScoringResult
import com.palmastro.contracts.Tone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuidanceBuilderTest {
    private val composer = ContentComposerImpl()
    private val builder = GuidanceBuilder()
    private val templates = ContentTemplates.default()

    private fun entry(signalId: String, domain: String, contribution: Double) =
        ExplainEntry(signalId, "$signalId → $domain", contribution)

    private fun makeInput(
        scores: Map<String, Int>,
        explain: List<ExplainEntry> = emptyList(),
        language: String = "en",
        grade: String = "Stable",
    ) = ContentInput(
        scoringResult = ScoringResult(
            domainScores = scores, subdimScores = emptyMap(),
            grade = grade, confidence = "high", confidenceReasons = emptyList(),
            explainability = explain, matchedBuckets = emptyList(), rulesetVersion = "2.0.0",
        ),
        deltaResult = null, tone = Tone.SCIENTIFIC, entitlements = emptySet(),
        calcLevel = CalcLevel.L2, monthKey = "2026-07", language = language,
    )

    private val evenScores = mapOf("career" to 50, "wealth" to 50, "family" to 50, "health" to 50)

    // ------------------------------------------------------- bucket fallback

    @Test
    fun `bucket generics back the guidance when no signals contributed`() {
        val scores = mapOf("career" to 85, "wealth" to 30, "family" to 60, "health" to 45)
        val payloads = composer.compose(makeInput(scores, grade = "Building"))
        val guidance = builder.build(payloads, "Building", "en")

        assertEquals(3, guidance.strengths.size)
        assertTrue(guidance.strengths.all { it.signalId == null }, "generic strengths carry no signalId")
        assertEquals(listOf("career", "family", "health"), guidance.strengths.map { it.domain })

        assertEquals(2, guidance.mindful.size)
        assertTrue(guidance.mindful.all { it.signalId == null }, "generic mindful carry no signalId")
        assertEquals(listOf("wealth", "health"), guidance.mindful.map { it.domain })

        assertEquals(4, guidance.weekPlan.size)
        assertTrue(guidance.monthTheme.isNotBlank())
        (guidance.strengths + guidance.mindful).forEach { item ->
            assertTrue(item.title.isNotBlank(), "${item.domain} title blank")
            assertTrue(item.body.isNotBlank(), "${item.domain} body blank")
            assertTrue(item.action.isNotBlank(), "${item.domain} action blank")
        }
    }

    // -------------------------------------------------- signal-backed order

    @Test
    fun `signal-backed strengths rank by contribution with distinct domains`() {
        val scores = mapOf("career" to 80, "wealth" to 70, "family" to 68, "health" to 30)
        val explain = listOf(
            entry("PALM_HEADLINE_LONG_CLEAR", "career", 3.6),
            entry("PALM_FATELINE_STRONG", "career", 3.2),
            entry("PALM_FATELINE_STRONG", "wealth", 2.8),
            entry("PALM_HEARTLINE_STRONG", "family", 2.4),
            entry("PALM_LIFELINE_CLEAR", "health", 1.8),
            entry("PALM_LIFELINE_FAINT", "health", -4.9),
            entry("PALM_HEADLINE_CHAINED", "career", -4.3),
        )
        val guidance = builder.build(composer.compose(makeInput(scores, explain)), "Stable", "en")

        assertEquals(
            listOf(
                "PALM_HEADLINE_LONG_CLEAR" to "career",
                "PALM_FATELINE_STRONG" to "wealth",
                "PALM_HEARTLINE_STRONG" to "family",
            ),
            guidance.strengths.map { it.signalId to it.domain },
        )
        assertEquals(
            listOf(
                "PALM_LIFELINE_FAINT" to "health",
                "PALM_HEADLINE_CHAINED" to "career",
            ),
            guidance.mindful.map { it.signalId to it.domain },
        )
    }

    @Test
    fun `mindful keeps one card per signal across domains`() {
        val explain = listOf(
            entry("PALM_LIFELINE_FAINT", "health", -4.9),
            entry("PALM_LIFELINE_FAINT", "family", -3.2),
            entry("PALM_HEARTLINE_THIN", "family", -4.5),
        )
        val guidance = builder.build(composer.compose(makeInput(evenScores, explain)), "Stable", "en")
        assertEquals(
            listOf("PALM_LIFELINE_FAINT" to "health", "PALM_HEARTLINE_THIN" to "family"),
            guidance.mindful.map { it.signalId to it.domain },
        )
    }

    @Test
    fun `positive signal without guidance copy falls through to the next candidate`() {
        val explain = listOf(
            entry("ASTRO_JUPITER_STRONG", "wealth", 5.0),
            entry("PALM_HEADLINE_LONG_CLEAR", "career", 3.6),
        )
        val guidance = builder.build(composer.compose(makeInput(evenScores, explain)), "Stable", "en")
        assertEquals("PALM_HEADLINE_LONG_CLEAR", guidance.strengths.first().signalId)
        assertNull(guidance.strengths.first { it.domain == "wealth" }.signalId, "wealth backfilled generically")
    }

    @Test
    fun `build is deterministic`() {
        val explain = listOf(
            entry("PALM_HEADLINE_LONG_CLEAR", "career", 3.6),
            entry("PALM_LIFELINE_FAINT", "health", -4.9),
        )
        val payloads = composer.compose(makeInput(evenScores, explain))
        assertEquals(builder.build(payloads, "Stable", "en"), builder.build(payloads, "Stable", "en"))
    }

    // -------------------------------------------------------------- weekPlan

    @Test
    fun `week plan switches high and low variants at score 65`() {
        val scores = mapOf("career" to 65, "wealth" to 64, "family" to 90, "health" to 10)
        val guidance = builder.build(composer.compose(makeInput(scores)), "Stable", "en")
        assertEquals(4, guidance.weekPlan.size)
        val domains = templates.guidance.domains
        assertEquals(templates.localized(domains["career"]!!.monthPlan["high"]!!, "en"), guidance.weekPlan[0])
        assertEquals(templates.localized(domains["wealth"]!!.monthPlan["low"]!!, "en"), guidance.weekPlan[1])
        assertEquals(templates.localized(domains["family"]!!.monthPlan["high"]!!, "en"), guidance.weekPlan[2])
        assertEquals(templates.localized(domains["health"]!!.monthPlan["low"]!!, "en"), guidance.weekPlan[3])
    }

    // ------------------------------------------------------------ monthTheme

    @Test
    fun `month theme follows the overall grade and stays positive for Watchout`() {
        val payloads = composer.compose(makeInput(evenScores))
        val themes = listOf("Growing", "Stable", "Building", "Watchout")
            .associateWith { builder.build(payloads, it, "en").monthTheme }
        assertEquals(4, themes.values.toSet().size, "each grade has its own theme")
        assertTrue(themes.getValue("Watchout").contains("gentle rebuilding"))
    }

    @Test
    fun `unknown grade falls back to the Stable theme`() {
        val payloads = composer.compose(makeInput(evenScores))
        assertEquals(
            builder.build(payloads, "Stable", "en").monthTheme,
            builder.build(payloads, "NotAGrade", "en").monthTheme,
        )
    }

    // ------------------------------------------------------ language fallback

    @Test
    fun `unsupported language falls back to english entirely`() {
        val payloads = composer.compose(makeInput(evenScores))
        assertEquals(builder.build(payloads, "Stable", "en"), builder.build(payloads, "Stable", "fr"))
    }

    @Test
    fun `languages without guidance copy fall back per-field to english`() {
        // ja is a supported template language, but guidance copy ships en + zh-TW.
        val payloads = composer.compose(makeInput(evenScores))
        val en = builder.build(payloads, "Stable", "en")
        val ja = builder.build(payloads, "Stable", "ja")
        assertEquals(en.monthTheme, ja.monthTheme)
        assertEquals(en.strengths.map { it.title }, ja.strengths.map { it.title })
        assertEquals(en.weekPlan, ja.weekPlan)
    }

    @Test
    fun `zh-TW guidance uses traditional chinese copy`() {
        val payloads = composer.compose(makeInput(evenScores))
        val zh = builder.build(payloads, "Stable", "zh-TW")
        assertTrue(zh.monthTheme.contains("月份"), "zh-TW theme localized: ${zh.monthTheme}")
        assertTrue(zh.strengths.first().title != builder.build(payloads, "Stable", "en").strengths.first().title)
    }
}

/**
 * Golden snapshot (same fixed input as [GoldenReportSnapshotTest]): one full
 * Guidance in en + zh-TW compared structurally against
 * src/test/resources/golden/guidance_<lang>.json.
 */
class GuidanceGoldenSnapshotTest {
    private val composer = ContentComposerImpl()
    private val builder = GuidanceBuilder()
    private val json = Json

    private fun goldenInput(language: String) = ContentInput(
        scoringResult = ScoringResult(
            domainScores = mapOf("career" to 72, "wealth" to 58, "family" to 65, "health" to 40),
            subdimScores = mapOf("career.focus" to 75),
            grade = "Stable", confidence = "high", confidenceReasons = listOf("full_scan"),
            explainability = listOf(
                ExplainEntry("PALM_HEADLINE_LONG_CLEAR", "PALM_HEADLINE_LONG_CLEAR → career", 3.6),
                ExplainEntry("ASTRO_JUPITER_STRONG", "ASTRO_JUPITER_STRONG → wealth", 2.1),
                ExplainEntry("PALM_HEARTLINE_STRONG", "PALM_HEARTLINE_STRONG → family", 2.4),
                ExplainEntry("PALM_LIFELINE_CLEAR", "PALM_LIFELINE_CLEAR → health", 1.8),
            ),
            matchedBuckets = emptyList(), rulesetVersion = "2.0.0",
        ),
        deltaResult = null, tone = Tone.SCIENTIFIC, entitlements = emptySet(),
        calcLevel = CalcLevel.L2, monthKey = "2026-07", language = language,
    )

    private fun golden(name: String): String =
        javaClass.getResourceAsStream("/golden/$name")?.bufferedReader()?.readText()
            ?: error("missing golden file $name")

    @Test
    fun `guidance matches golden snapshots in en and zh-TW`() {
        for (language in listOf("en", "zh-TW")) {
            val payloads = composer.compose(goldenInput(language))
            val guidance = builder.build(payloads, "Stable", language)
            assertEquals(
                json.parseToJsonElement(golden("guidance_$language.json")),
                json.encodeToJsonElement(guidance),
                "guidance snapshot mismatch [$language]",
            )
        }
    }
}

/** Completeness of the guidance vocabulary shipped in content-templates 2.1.0. */
class GuidanceTemplateTest {
    private val templates = ContentTemplates.default()
    private val guidanceLanguages = listOf("en", "zh-TW")

    private val positivePalm = listOf(
        "PALM_HEADLINE_LONG_CLEAR", "PALM_HEARTLINE_STRONG",
        "PALM_LIFELINE_CLEAR", "PALM_FATELINE_STRONG",
    )
    private val negativePalm = listOf(
        "PALM_HEADLINE_CHAINED", "PALM_FATELINE_BREAKS",
        "PALM_HEARTLINE_THIN", "PALM_LIFELINE_FAINT", "PALM_MINOR_LINES_DENSE",
    )
    private val astroScored = listOf(
        "ASTRO_SUN_FIRE", "ASTRO_SUN_EARTH", "ASTRO_SUN_AIR", "ASTRO_SUN_WATER",
        "ASTRO_SUN_CARDINAL", "ASTRO_SUN_FIXED", "ASTRO_SUN_MUTABLE",
        "ASTRO_MOON_FIRE", "ASTRO_MOON_EARTH", "ASTRO_MOON_AIR", "ASTRO_MOON_WATER",
        "ASTRO_ASC_FIRE", "ASTRO_ASC_EARTH", "ASTRO_ASC_AIR", "ASTRO_ASC_WATER",
    )
    private val sunSigns = listOf(
        "ARIES", "TAURUS", "GEMINI", "CANCER", "LEO", "VIRGO",
        "LIBRA", "SCORPIO", "SAGITTARIUS", "CAPRICORN", "AQUARIUS", "PISCES",
    ).map { "ASTRO_SUN_$it" }

    private fun assertComplete(context: String, copy: GuidanceCopy) {
        for (lang in guidanceLanguages) {
            assertTrue(!copy.title[lang].isNullOrBlank(), "$context.title[$lang] blank")
            assertTrue(!copy.body[lang].isNullOrBlank(), "$context.body[$lang] blank")
            assertTrue(!copy.action[lang].isNullOrBlank(), "$context.action[$lang] blank")
        }
    }

    @Test
    fun `all 24 ruleset signals have direction-appropriate guidance copy`() {
        val signals = templates.guidance.signals
        for (id in positivePalm) {
            assertNotNull(signals[id]?.leanInto, "$id missing leanInto").let { assertComplete("$id.leanInto", it) }
        }
        for (id in negativePalm) {
            assertNotNull(signals[id]?.mindfulOf, "$id missing mindfulOf").let { assertComplete("$id.mindfulOf", it) }
        }
        for (id in astroScored) {
            assertNotNull(signals[id]?.leanInto, "$id missing leanInto").let { assertComplete("$id.leanInto", it) }
            assertNotNull(signals[id]?.mindfulOf, "$id missing mindfulOf").let { assertComplete("$id.mindfulOf", it) }
        }
    }

    @Test
    fun `score-inert sun sign ids carry leanInto copy`() {
        for (id in sunSigns) {
            assertNotNull(templates.guidance.signals[id]?.leanInto, "$id missing leanInto")
                .let { assertComplete("$id.leanInto", it) }
        }
    }

    @Test
    fun `every score maps to a strengths and a mindful generic in every domain`() {
        for (domain in listOf("career", "wealth", "family", "health")) {
            val entry = assertNotNull(templates.guidance.domains[domain], "$domain missing guidance")
            for (score in 0..100) {
                assertNotNull(bucketCopy(entry.strengths, score), "$domain.strengths uncovered at $score")
                assertNotNull(bucketCopy(entry.mindful, score), "$domain.mindful uncovered at $score")
            }
            assertComplete("$domain.strengths samples", bucketCopy(entry.strengths, 0)!!)
            assertComplete("$domain.mindful samples", bucketCopy(entry.mindful, 100)!!)
        }
    }

    @Test
    fun `month plan has high and low focus lines per domain`() {
        for (domain in listOf("career", "wealth", "family", "health")) {
            val plan = templates.guidance.domains[domain]!!.monthPlan
            for (key in listOf("high", "low")) {
                val line = assertNotNull(plan[key], "$domain.monthPlan.$key missing")
                for (lang in guidanceLanguages) {
                    assertTrue(!line[lang].isNullOrBlank(), "$domain.monthPlan.$key[$lang] blank")
                }
            }
        }
    }

    @Test
    fun `month theme covers all four overall grades`() {
        for (grade in listOf("Growing", "Stable", "Building", "Watchout")) {
            val theme = assertNotNull(templates.guidance.monthTheme[grade], "monthTheme.$grade missing")
            for (lang in guidanceLanguages) {
                assertTrue(!theme[lang].isNullOrBlank(), "monthTheme.$grade[$lang] blank")
            }
        }
    }

    private fun bucketCopy(field: Map<String, GuidanceCopy>, score: Int): GuidanceCopy? =
        field.entries.firstOrNull { (id, _) -> templates.buckets[id]?.contains(score) == true }?.value
}
