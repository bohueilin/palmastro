package com.palmastro.scoring

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoringEngineTest {
    private val engine = ScoringEngineImpl()

    private fun makeInput(
        palmConfidence: String = "high",
        astroLevel: CalcLevel = CalcLevel.L2,
        palmFeatures: PalmFeatures = PalmFeatures(
            headlinePresent = true, heartlinePresent = true, lifelinePresent = true, fatelinePresent = true,
            headlineShape = "curved", heartlineShape = "curved", lifelineShape = "curved", fatelineShape = "straight",
            headlineClarity = "clear", heartlineClarity = "clear", lifelineClarity = "clear", fatelineClarity = "moderate",
            headlineLength = "long", fatelineLength = "medium",
            venusMountDensity = "med", jupiterMountDensity = "med", saturnMountDensity = "low", minorLineDensity = "med",
        ),
        astroSignals: List<AstroSignal> = listOf(
            AstroSignal("ASTRO_SUN_ARIES", "+", 3, "high", "SAFE_GENERAL"),
            AstroSignal("ASTRO_SATURN_STRONG", "+", 3, "med", "SAFE_CAREER")
        )
    ) = ScoringInput(
        palmFeatures = PalmFeatureResult(palmFeatures, 0.9f, palmConfidence, "1.0.0"),
        astroResult = AstroResult(astroLevel, astroSignals, "1.0.0"),
        userContext = UserContext(Hand.RIGHT, false),
        rulesetVersion = "1.0.0"
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
    fun `ruleset version is preserved in result`() {
        val result = engine.score(makeInput())
        assertEquals("1.0.0", result.rulesetVersion)
    }
}
