package com.palmastro.scoring

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RulesetTest {
    private val ruleset = Ruleset.default()
    private val requiredDomains = setOf("career", "wealth", "family", "health")

    @Test
    fun `default ruleset has exactly 7 signals`() {
        assertEquals(7, ruleset.signals.size)
        val expectedIds = setOf(
            "PALM_HEADLINE_LONG_CLEAR", "PALM_HEARTLINE_STRONG",
            "PALM_LIFELINE_CLEAR", "PALM_FATELINE_STRONG",
            "ASTRO_SUN_FIRE", "ASTRO_SATURN_STRONG", "ASTRO_JUPITER_STRONG"
        )
        assertEquals(expectedIds, ruleset.signals.map { it.signalId }.toSet())
    }

    @Test
    fun `no duplicate signal IDs in default ruleset`() {
        val ids = ruleset.signals.map { it.signalId }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `all 4 domains covered in every signal domainWeights`() {
        ruleset.signals.forEach { signal ->
            assertTrue(
                signal.domainWeights.keys.containsAll(requiredDomains),
                "Signal ${signal.signalId} missing domains: ${requiredDomains - signal.domainWeights.keys}"
            )
        }
    }

    @Test
    fun `confidence multipliers cover high med and low`() {
        val requiredLevels = setOf("high", "med", "low")
        assertTrue(
            ruleset.confidenceMultipliers.keys.containsAll(requiredLevels),
            "Missing confidence levels: ${requiredLevels - ruleset.confidenceMultipliers.keys}"
        )
        ruleset.confidenceMultipliers.values.forEach { multiplier ->
            assertTrue(multiplier in 0.0..1.0, "Multiplier $multiplier out of 0-1 range")
        }
    }

    @Test
    fun `grade thresholds cover full 0-100 range with no gaps`() {
        val thresholds = ruleset.gradeThresholds
        assertTrue(thresholds.isNotEmpty())
        val allValues = thresholds.values.flatMap { it.toList() }.sorted()
        assertEquals(0, allValues.first(), "Thresholds must start at 0")
        assertEquals(100, allValues.last(), "Thresholds must end at 100")
        val ranges = thresholds.values.sortedBy { it.first }
        for (i in 0 until ranges.size - 1) {
            assertEquals(
                ranges[i].last + 1, ranges[i + 1].first,
                "Gap between ${ranges[i]} and ${ranges[i + 1]}"
            )
        }
    }
}
