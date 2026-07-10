package com.palmastro.scoring

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoringEngineTest {
    private val engine = ScoringEngineImpl()

    private fun makeFeatures(
        headlineClarity: String = "clear", headlineLength: String = "long",
        heartlineClarity: String = "clear", lifelineClarity: String = "clear",
        fatelineClarity: String = "clear", minorLineDensity: String = "med",
    ) = PalmFeatures(
        headlinePresent = true, heartlinePresent = true, lifelinePresent = true, fatelinePresent = true,
        headlineShape = "curved", heartlineShape = "curved", lifelineShape = "curved", fatelineShape = "straight",
        headlineClarity = headlineClarity, heartlineClarity = heartlineClarity,
        lifelineClarity = lifelineClarity, fatelineClarity = fatelineClarity,
        headlineLength = headlineLength, fatelineLength = "medium",
        venusMountDensity = "med", jupiterMountDensity = "med", saturnMountDensity = "low",
        minorLineDensity = minorLineDensity,
    )

    private fun makeInput(
        palmConfidence: String = "high",
        astroLevel: CalcLevel = CalcLevel.L2,
        palmFeatures: PalmFeatures = makeFeatures(),
        astroSignals: List<AstroSignal> = listOf(
            AstroSignal("ASTRO_SUN_CAPRICORN", "+", 3, "high", "SAFE_GENERAL"),
            AstroSignal("ASTRO_SUN_EARTH", "+", 2, "high", "SAFE_GENERAL"),
            AstroSignal("ASTRO_SUN_CARDINAL", "+", 1, "high", "SAFE_GENERAL")
        )
    ) = ScoringInput(
        palmFeatures = PalmFeatureResult(palmFeatures, 0.9f, palmConfidence, "2.0.0"),
        astroResult = AstroResult(astroLevel, astroSignals, "2.0.0"),
        userContext = UserContext(Hand.RIGHT, false),
        rulesetVersion = "2.0.0"
    )

    @Test
    fun `score produces 4 domain scores between 0-100`() {
        val result = engine.score(makeInput())
        assertEquals(4, result.domainScores.size)
        assertTrue(result.domainScores.keys.containsAll(listOf("career", "wealth", "family", "health")))
        result.domainScores.values.forEach { assertTrue(it in 0..100) }
    }

    @Test
    fun `grade assignment follows thresholds`() {
        val result = engine.score(makeInput())
        val validGrades = setOf("Watchout", "Building", "Stable", "Growing")
        assertTrue(result.grade in validGrades)
    }

    @Test
    fun `positive palm features push scores above baseline`() {
        val result = engine.score(makeInput())
        assertTrue(result.domainScores["career"]!! > 50, "career ${result.domainScores["career"]} should exceed baseline")
        assertTrue(result.domainScores["health"]!! > 50, "health ${result.domainScores["health"]} should exceed baseline")
    }

    @Test
    fun `negative palm features pull scores below baseline`() {
        val negativeFeatures = makeFeatures(
            headlineClarity = "broken", heartlineClarity = "thin",
            lifelineClarity = "faint", fatelineClarity = "broken",
            minorLineDensity = "high"
        )
        val result = engine.score(makeInput(palmFeatures = negativeFeatures, astroSignals = emptyList()))
        result.domainScores.forEach { (domain, score) ->
            assertTrue(score < 50, "$domain score $score should be below baseline")
        }
    }

    @Test
    fun `all negatives at high confidence reach the Watchout band`() {
        // career: 50 - (6*0.8 + 6*0.9 + 6*0.1 + 6*0.3 + 5*0.6) = 34.4 -> 34
        // health: 50 - (6*0.6 + 6*0.1 + 6*0.4 + 6*0.9 + 5*0.8) = 34.0 -> 34
        // overall avg = 35.25 -> 35 -> Watchout
        val negativeFeatures = makeFeatures(
            headlineClarity = "broken", heartlineClarity = "thin",
            lifelineClarity = "faint", fatelineClarity = "broken",
            minorLineDensity = "high"
        )
        val result = engine.score(makeInput(palmFeatures = negativeFeatures, astroSignals = emptyList()))
        assertEquals(34, result.domainScores["career"])
        assertEquals(34, result.domainScores["health"])
        assertEquals("Watchout", result.grade)
    }

    @Test
    fun `low palm confidence suppresses negative palm signals`() {
        val negativeFeatures = makeFeatures(
            headlineClarity = "broken", heartlineClarity = "thin",
            lifelineClarity = "faint", fatelineClarity = "broken",
            minorLineDensity = "high"
        )
        val result = engine.score(
            makeInput(palmConfidence = "low", palmFeatures = negativeFeatures, astroSignals = emptyList())
        )
        // All palm signals require at least "med" confidence.
        result.domainScores.values.forEach { assertEquals(50, it) }
    }

    @Test
    fun `astro element signals contribute to scores`() {
        val withAstro = engine.score(makeInput(astroSignals = listOf(AstroSignal("ASTRO_SUN_EARTH", "+", 2, "high", "SAFE_GENERAL"))))
        val withoutAstro = engine.score(makeInput(astroSignals = emptyList()))
        assertTrue(
            withAstro.domainScores["wealth"]!! > withoutAstro.domainScores["wealth"]!!,
            "earth sun should lift wealth"
        )
    }

    @Test
    fun `informational sun sign signal is score-inert`() {
        val signOnly = engine.score(makeInput(astroSignals = listOf(AstroSignal("ASTRO_SUN_CAPRICORN", "+", 3, "high", "SAFE_GENERAL"))))
        val none = engine.score(makeInput(astroSignals = emptyList()))
        assertEquals(none.domainScores, signOnly.domainScores)
    }

    @Test
    fun `overall confidence is min of palm and astro confidence`() {
        val result = engine.score(makeInput(palmConfidence = "low"))
        assertEquals("low", result.confidence)
    }

    @Test
    fun `deterministic - same inputs give same results`() {
        val input = makeInput()
        val r1 = engine.score(input)
        val r2 = engine.score(input)
        assertEquals(r1, r2)
    }

    @Test
    fun `scores are clamped to 0-100`() {
        val extremeSignals = (1..20).map { AstroSignal("ASTRO_EXTREME_$it", "+", 5, "high", "SAFE_GENERAL") }
        val input = makeInput(astroSignals = extremeSignals)
        val result = engine.score(input)
        result.domainScores.values.forEach { assertTrue(it in 0..100, "Score $it out of range") }
    }

    @Test
    fun `explainability lists contributing signals`() {
        val result = engine.score(makeInput())
        assertTrue(result.explainability.isNotEmpty())
        result.explainability.forEach { entry ->
            assertTrue(entry.signalId.isNotBlank())
        }
    }

    @Test
    fun `explainability includes negative contributions`() {
        val negativeFeatures = makeFeatures(lifelineClarity = "faint")
        val result = engine.score(makeInput(palmFeatures = negativeFeatures, astroSignals = emptyList()))
        val faintEntries = result.explainability.filter { it.signalId == "PALM_LIFELINE_FAINT" }
        assertTrue(faintEntries.isNotEmpty())
        assertTrue(faintEntries.all { it.contribution < 0 })
    }

    @Test
    fun `ruleset version is preserved in result`() {
        val result = engine.score(makeInput())
        assertEquals("2.0.0", result.rulesetVersion)
    }
}
