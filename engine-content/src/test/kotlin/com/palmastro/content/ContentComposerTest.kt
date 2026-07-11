package com.palmastro.content

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ContentComposerTest {
    private val composer = ContentComposerImpl()
    private val safetyFilter = SafetyFilterImpl()

    private fun makeInput(
        language: String = "en",
        deltaResult: DeltaResult? = null,
    ) = ContentInput(
        scoringResult = ScoringResult(
            domainScores = mapOf("career" to 72, "wealth" to 58, "family" to 65, "health" to 55),
            subdimScores = mapOf("career.focus" to 75, "wealth.risk" to 60),
            grade = "Stable", confidence = "high", confidenceReasons = listOf("full_scan"),
            explainability = listOf(
                ExplainEntry("PALM_HEADLINE_LONG_CLEAR", "PALM_HEADLINE_LONG_CLEAR → career", 3.6),
                ExplainEntry("ASTRO_JUPITER_STRONG", "ASTRO_JUPITER_STRONG → wealth", 2.1),
            ),
            matchedBuckets = emptyList(), rulesetVersion = "1.0.0"
        ),
        deltaResult = deltaResult, tone = Tone.SCIENTIFIC, entitlements = emptySet(),
        calcLevel = CalcLevel.L2, monthKey = "2026-03", language = language
    )

    @Test
    fun `compose produces payloads for all 4 domains`() {
        val payloads = composer.compose(makeInput())
        assertEquals(4, payloads.size)
        assertTrue(payloads.keys.containsAll(Domains.ALL))
    }

    @Test
    fun `payload contains correct domain score grade and subdims`() {
        val payloads = composer.compose(makeInput())
        assertEquals(72, payloads["career"]!!.scoreCard.totalScore)
        assertEquals("Stable", payloads["career"]!!.scoreCard.grade)
        assertEquals(mapOf("career.focus" to 75), payloads["career"]!!.scoreCard.subdims)
    }

    @Test
    fun `payload month key and calc level match input`() {
        val payloads = composer.compose(makeInput())
        assertEquals("2026-03", payloads["career"]!!.monthKey)
        assertEquals(CalcLevel.L2, payloads["career"]!!.calcLevel)
    }

    @Test
    fun `confidence and confidence reasons are propagated`() {
        val payloads = composer.compose(makeInput())
        assertEquals("high", payloads["family"]!!.confidence)
        assertEquals(listOf("full_scan"), payloads["family"]!!.confidenceReasons)
    }

    @Test
    fun `interpretation has pattern trigger and cost populated`() {
        val payloads = composer.compose(makeInput())
        for (domain in Domains.ALL) {
            val interpretation = payloads[domain]!!.interpretation
            assertTrue(interpretation.pattern.isNotBlank(), "$domain pattern blank")
            assertTrue(interpretation.trigger.isNotBlank(), "$domain trigger blank")
            assertTrue(interpretation.cost.isNotBlank(), "$domain cost blank")
        }
    }

    @Test
    fun `blindspot actions and prompt are populated for all domains`() {
        val payloads = composer.compose(makeInput())
        for (domain in Domains.ALL) {
            val p = payloads[domain]!!
            assertTrue(p.blindspot.isNotBlank(), "$domain blindspot blank")
            assertTrue(p.actionToday.isNotBlank(), "$domain actionToday blank")
            assertTrue(p.actionWeek.isNotBlank(), "$domain actionWeek blank")
            assertTrue(p.prompt.isNotBlank(), "$domain prompt blank")
        }
    }

    @Test
    fun `safety notes added for wealth and health domains`() {
        val payloads = composer.compose(makeInput())
        assertTrue(payloads["wealth"]!!.safetyNotes.isNotEmpty())
        assertTrue(payloads["health"]!!.safetyNotes.isNotEmpty())
    }

    @Test
    fun `compose honors requested language`() {
        val en = composer.compose(makeInput(language = "en"))
        val zh = composer.compose(makeInput(language = "zh-TW"))
        assertEquals("en", en["career"]!!.language)
        assertEquals("zh-TW", zh["career"]!!.language)
        assertNotEquals(en["career"]!!.interpretation.pattern, zh["career"]!!.interpretation.pattern)
    }

    @Test
    fun `unsupported language falls back to english`() {
        val payloads = composer.compose(makeInput(language = "fr"))
        assertEquals("en", payloads["career"]!!.language)
        assertEquals(
            composer.compose(makeInput(language = "en"))["career"]!!.interpretation.pattern,
            payloads["career"]!!.interpretation.pattern
        )
    }

    @Test
    fun `compose is deterministic`() {
        assertEquals(composer.compose(makeInput()), composer.compose(makeInput()))
    }

    @Test
    fun `score bucket drives interpretation copy`() {
        val highInput = makeInput().let {
            it.copy(scoringResult = it.scoringResult.copy(domainScores = mapOf("career" to 85)))
        }
        val lowInput = makeInput().let {
            it.copy(scoringResult = it.scoringResult.copy(domainScores = mapOf("career" to 20)))
        }
        val high = composer.compose(highInput)["career"]!!
        val low = composer.compose(lowInput)["career"]!!
        assertNotEquals(high.interpretation.pattern, low.interpretation.pattern)
        assertNotEquals(high.actionToday, low.actionToday)
    }

    @Test
    fun `known signal id maps to template observation copy`() {
        val payloads = composer.compose(makeInput(language = "zh-TW"))
        val observation = payloads["career"]!!.observations.single()
        assertEquals("PALM_HEADLINE_LONG_CLEAR", observation.signalId)
        assertEquals("頭腦線清晰而長", observation.displayName)
        assertTrue(observation.evidenceSummary.isNotBlank())
    }

    @Test
    fun `unknown signal id falls back to humanized name and generic evidence`() {
        val input = makeInput().let {
            it.copy(
                scoringResult = it.scoringResult.copy(
                    explainability = listOf(ExplainEntry("PALM_MINOR_LINES_DENSE", "PALM_MINOR_LINES_DENSE → career", 1.0))
                )
            )
        }
        val observation = composer.compose(input)["career"]!!.observations.single()
        assertEquals("Minor Lines Dense", observation.displayName)
        assertEquals("This signal contributes to your overall reading.", observation.evidenceSummary)
    }

    @Test
    fun `delta result flows into score card`() {
        val delta = DeltaResult(
            domainDeltas = mapOf("career" to DeltaValue(5, "up")),
            subdimDeltas = emptyMap(), gradeShift = null,
            comparabilityScore = 88, comparabilityBucket = ComparabilityBucket.HIGH,
            prevMonthKey = "2026-02", currentMonthKey = "2026-03"
        )
        val payloads = composer.compose(makeInput(deltaResult = delta))
        assertEquals(DeltaValue(5, "up"), payloads["career"]!!.scoreCard.delta)
        assertEquals(88, payloads["career"]!!.scoreCard.comparabilityScore)
    }

    @Test
    fun `templates version is exposed`() {
        assertEquals("2.1.0", composer.templatesVersion)
    }

    @Test
    fun `safe fallback payload is localized and passes validation`() {
        for (language in ContentTemplates.default().languages) {
            val fallback = composer.safeFallbackPayload("wealth", language)
            assertEquals("wealth", fallback.domain)
            assertEquals(language, fallback.language)
            assertTrue(fallback.interpretation.pattern.isNotBlank())
            assertTrue(fallback.safetyNotes.isNotEmpty(), "wealth fallback keeps safety notes")
            val check = safetyFilter.validate(fallback)
            assertTrue(check.passed, "fallback [$language] violated: ${check.violations}")
        }
    }

    @Test
    fun `safe fallback payload preserves base metadata when provided`() {
        val base = composer.compose(makeInput())["career"]!!
        val fallback = composer.safeFallbackPayload("career", "zh-TW", base)
        assertEquals(base.monthKey, fallback.monthKey)
        assertEquals(base.calcLevel, fallback.calcLevel)
        assertEquals(base.scoreCard, fallback.scoreCard)
        assertEquals("zh-TW", fallback.language)
    }
}
