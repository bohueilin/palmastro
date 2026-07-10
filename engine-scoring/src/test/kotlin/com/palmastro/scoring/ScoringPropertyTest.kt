package com.palmastro.scoring

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoringPropertyTest {
    private val engine = ScoringEngineImpl()

    // Extractor v2 vocabulary.
    private val shapes = listOf("curved", "straight", "unclear")
    private val clarities = listOf("clear", "medium", "faint", "broken", "thin", "unclear")
    private val lengths = listOf("long", "medium", "short")
    private val densities = listOf("high", "med", "low")
    private val confidences = listOf("high", "med", "low")

    private fun randomPalmFeatures(rng: kotlin.random.Random) = PalmFeatures(
        headlinePresent = rng.nextBoolean(), heartlinePresent = rng.nextBoolean(),
        lifelinePresent = rng.nextBoolean(), fatelinePresent = rng.nextBoolean(),
        headlineShape = shapes.random(rng), heartlineShape = shapes.random(rng),
        lifelineShape = shapes.random(rng), fatelineShape = shapes.random(rng),
        headlineClarity = clarities.random(rng), heartlineClarity = clarities.random(rng),
        lifelineClarity = clarities.random(rng), fatelineClarity = clarities.random(rng),
        headlineLength = lengths.random(rng), fatelineLength = lengths.random(rng),
        venusMountDensity = densities.random(rng), jupiterMountDensity = densities.random(rng),
        saturnMountDensity = densities.random(rng), minorLineDensity = densities.random(rng),
    )

    private fun randomInput(rng: kotlin.random.Random): ScoringInput {
        val signalNames = listOf(
            "ASTRO_SUN_FIRE", "ASTRO_SUN_EARTH", "ASTRO_SUN_AIR", "ASTRO_SUN_WATER",
            "ASTRO_SUN_CARDINAL", "ASTRO_SUN_FIXED", "ASTRO_SUN_MUTABLE",
            "ASTRO_MOON_WATER", "ASTRO_ASC_FIRE", "ASTRO_SUN_ARIES"
        )
        val signalCount = rng.nextInt(0, 6)
        val astroSignals = (1..signalCount).map {
            AstroSignal(signalNames.random(rng), "+", rng.nextInt(1, 6), confidences.random(rng), "SAFE_GENERAL")
        }
        return ScoringInput(
            palmFeatures = PalmFeatureResult(randomPalmFeatures(rng), rng.nextFloat().coerceIn(0.1f, 1.0f), confidences.random(rng), "2.0.0"),
            astroResult = AstroResult(if (rng.nextBoolean()) CalcLevel.L2 else CalcLevel.L1, astroSignals, "2.0.0"),
            userContext = UserContext(if (rng.nextBoolean()) Hand.RIGHT else Hand.LEFT, false),
            rulesetVersion = "2.0.0"
        )
    }

    @Test fun `scores always 0-100 across 50 random inputs`() {
        val rng = kotlin.random.Random(42)
        repeat(50) { i -> engine.score(randomInput(rng)).domainScores.forEach { (d, s) -> assertTrue(s in 0..100, "Run $i $d: $s") } }
    }
    @Test fun `grades are monotonic with score thresholds`() {
        val rng = kotlin.random.Random(123)
        val gradeRanges = mapOf("Watchout" to 0..35, "Building" to 36..55, "Stable" to 56..75, "Growing" to 76..100)
        repeat(50) {
            val r = engine.score(randomInput(rng)); val avg = r.domainScores.values.average().toInt()
            assertTrue(avg in gradeRanges[r.grade]!!, "Grade ${r.grade} does not match avg $avg")
        }
    }
    @Test fun `determinism - same input 10 times`() {
        val rng = kotlin.random.Random(999); val input = randomInput(rng); val first = engine.score(input)
        repeat(9) { assertEquals(first, engine.score(input)) }
    }
    @Test fun `confidence ordering - high ge low for same positive signals`() {
        val pf = PalmFeatures(true, true, true, true, "curved", "curved", "curved", "straight", "clear", "clear", "clear", "clear", "long", "medium", "med", "med", "low", "med")
        val astro = listOf(AstroSignal("ASTRO_SUN_FIRE", "+", 2, "high", "SAFE_GENERAL"))
        val high = engine.score(ScoringInput(PalmFeatureResult(pf, 0.9f, "high", "2.0.0"), AstroResult(CalcLevel.L2, astro, "2.0.0"), UserContext(Hand.RIGHT, false), "2.0.0"))
        val low = engine.score(ScoringInput(PalmFeatureResult(pf, 0.9f, "low", "2.0.0"), AstroResult(CalcLevel.L2, astro, "2.0.0"), UserContext(Hand.RIGHT, false), "2.0.0"))
        high.domainScores.forEach { (d, hs) -> assertTrue(hs >= low.domainScores[d]!!, "$d: high $hs < low ${low.domainScores[d]}") }
    }
    @Test fun `negative direction - all-negative palm never exceeds baseline`() {
        val pf = PalmFeatures(true, true, true, true, "straight", "straight", "straight", "straight", "broken", "thin", "faint", "broken", "short", "short", "low", "low", "low", "high")
        val r = engine.score(ScoringInput(PalmFeatureResult(pf, 0.9f, "high", "2.0.0"), AstroResult(CalcLevel.L1, emptyList(), "2.0.0"), UserContext(Hand.RIGHT, false), "2.0.0"))
        r.domainScores.forEach { (d, s) -> assertTrue(s < 50, "$d: $s should be below baseline") }
    }
    @Test fun `baseline with no signals - scores exactly 50`() {
        val pf = PalmFeatures(false, false, false, false, "unclear", "unclear", "unclear", "unclear", "unclear", "unclear", "unclear", "unclear", "short", "short", "low", "low", "low", "low")
        val r = engine.score(ScoringInput(PalmFeatureResult(pf, 0.1f, "low", "2.0.0"), AstroResult(CalcLevel.L1, emptyList(), "2.0.0"), UserContext(Hand.RIGHT, false), "2.0.0"))
        r.domainScores.forEach { (d, s) -> assertTrue(s in 45..55, "$d: $s not near 50") }
    }
    @Test fun `explainability non-empty when strong signals match`() {
        val pf = PalmFeatures(true, true, true, true, "curved", "curved", "curved", "straight", "clear", "clear", "clear", "clear", "long", "medium", "med", "med", "low", "med")
        val r = engine.score(ScoringInput(PalmFeatureResult(pf, 0.95f, "high", "2.0.0"), AstroResult(CalcLevel.L2, emptyList(), "2.0.0"), UserContext(Hand.RIGHT, false), "2.0.0"))
        assertTrue(r.explainability.isNotEmpty())
    }
    @Test fun `domain coverage - every result has 4 domains`() {
        val rng = kotlin.random.Random(777); val expected = setOf("career", "wealth", "family", "health")
        repeat(50) { assertEquals(expected, engine.score(randomInput(rng)).domainScores.keys) }
    }
    @Test fun `ruleset version preserved`() {
        val rng = kotlin.random.Random(555)
        repeat(50) { assertEquals("2.0.0", engine.score(randomInput(rng)).rulesetVersion) }
    }
}
