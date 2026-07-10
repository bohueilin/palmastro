package com.palmastro.scoring

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RulesetTest {
    private val ruleset = Ruleset.default()
    private val requiredDomains = setOf("career", "wealth", "family", "health")

    @Test
    fun `default ruleset is version 2`() {
        assertEquals("2.0.0", ruleset.version)
    }

    @Test
    fun `default ruleset has the v2 signal set`() {
        val expectedIds = setOf(
            // Palm positives (kept from v1).
            "PALM_HEADLINE_LONG_CLEAR", "PALM_HEARTLINE_STRONG",
            "PALM_LIFELINE_CLEAR", "PALM_FATELINE_STRONG",
            // Palm negatives (new in v2).
            "PALM_HEADLINE_CHAINED", "PALM_FATELINE_BREAKS",
            "PALM_HEARTLINE_THIN", "PALM_LIFELINE_FAINT", "PALM_MINOR_LINES_DENSE",
            // Astro: sun elements + modalities (L1), moon + asc elements (L2).
            "ASTRO_SUN_FIRE", "ASTRO_SUN_EARTH", "ASTRO_SUN_AIR", "ASTRO_SUN_WATER",
            "ASTRO_SUN_CARDINAL", "ASTRO_SUN_FIXED", "ASTRO_SUN_MUTABLE",
            "ASTRO_MOON_FIRE", "ASTRO_MOON_EARTH", "ASTRO_MOON_AIR", "ASTRO_MOON_WATER",
            "ASTRO_ASC_FIRE", "ASTRO_ASC_EARTH", "ASTRO_ASC_AIR", "ASTRO_ASC_WATER"
        )
        assertEquals(expectedIds, ruleset.signals.map { it.signalId }.toSet())
        assertEquals(24, ruleset.signals.size)
    }

    @Test
    fun `fabricated planetary signals are gone`() {
        val ids = ruleset.signals.map { it.signalId }
        assertTrue(ids.none { it.contains("SATURN") || it.contains("JUPITER") || it.contains("MARS") })
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
    fun `every domain has reachable positive and negative contributions`() {
        requiredDomains.forEach { domain ->
            assertTrue(
                ruleset.signals.any { it.direction > 0 && (it.domainWeights[domain] ?: 0.0) > 0.0 },
                "Domain $domain has no positive signal"
            )
            assertTrue(
                ruleset.signals.any { it.direction < 0 && (it.domainWeights[domain] ?: 0.0) > 0.0 },
                "Domain $domain has no negative signal"
            )
        }
    }

    @Test
    fun `negative palm signals have direction -1 and health ones are soft-tagged`() {
        val negatives = ruleset.signals.filter { it.direction == -1 }.associateBy { it.signalId }
        assertEquals(
            setOf(
                "PALM_HEADLINE_CHAINED", "PALM_FATELINE_BREAKS",
                "PALM_HEARTLINE_THIN", "PALM_LIFELINE_FAINT", "PALM_MINOR_LINES_DENSE"
            ),
            negatives.keys
        )
        listOf("PALM_HEADLINE_CHAINED", "PALM_LIFELINE_FAINT", "PALM_MINOR_LINES_DENSE").forEach {
            assertEquals("SAFE_HEALTH_SOFT_ONLY", negatives[it]!!.safetyTag, "$it must be health-soft")
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
    fun `grade thresholds unchanged and cover full 0-100 range with no gaps`() {
        val thresholds = ruleset.gradeThresholds
        assertEquals(GradeRange(0, 35), thresholds["Watchout"])
        assertEquals(GradeRange(36, 55), thresholds["Building"])
        assertEquals(GradeRange(56, 75), thresholds["Stable"])
        assertEquals(GradeRange(76, 100), thresholds["Growing"])
        val allValues = thresholds.values.flatMap { it.toIntRange().toList() }.sorted()
        assertEquals(0, allValues.first(), "Thresholds must start at 0")
        assertEquals(100, allValues.last(), "Thresholds must end at 100")
        val ranges = thresholds.values.sortedBy { it.min }
        for (i in 0 until ranges.size - 1) {
            assertEquals(
                ranges[i].max + 1, ranges[i + 1].min,
                "Gap between ${ranges[i]} and ${ranges[i + 1]}"
            )
        }
    }

    @Nested
    inner class ValidateOrThrow {
        @Test
        fun `default ruleset passes validation and returns itself`() {
            assertSame(ruleset, ruleset.validateOrThrow())
        }

        @Test
        fun `duplicate signal IDs throw`() {
            val bad = ruleset.copy(signals = ruleset.signals + ruleset.signals.first())
            val ex = assertThrows<IllegalArgumentException> { bad.validateOrThrow() }
            assertEquals("ruleset_duplicate_signal_ids", ex.message)
        }

        @Test
        fun `signal missing a domain weight throws`() {
            val crippled = ruleset.signals.first().let {
                it.copy(signalId = "X_TEST", domainWeights = it.domainWeights - "health")
            }
            val bad = ruleset.copy(signals = ruleset.signals + crippled)
            val ex = assertThrows<IllegalArgumentException> { bad.validateOrThrow() }
            assertEquals("ruleset_signal_missing_domain:X_TEST", ex.message)
        }

        @Test
        fun `domain weight above 1 throws`() {
            val crippled = ruleset.signals.first().let {
                it.copy(signalId = "X_TEST", domainWeights = it.domainWeights + ("career" to 1.5))
            }
            val bad = ruleset.copy(signals = ruleset.signals + crippled)
            assertThrows<IllegalArgumentException> { bad.validateOrThrow() }
        }

        @Test
        fun `invalid direction throws`() {
            val crippled = ruleset.signals.first().copy(signalId = "X_TEST", direction = 0)
            val bad = ruleset.copy(signals = ruleset.signals + crippled)
            assertThrows<IllegalArgumentException> { bad.validateOrThrow() }
        }

        @Test
        fun `confidence multiplier out of range throws`() {
            val bad = ruleset.copy(confidenceMultipliers = ruleset.confidenceMultipliers + ("high" to 1.2))
            assertThrows<IllegalArgumentException> { bad.validateOrThrow() }
        }

        @Test
        fun `missing confidence level throws`() {
            val bad = ruleset.copy(confidenceMultipliers = ruleset.confidenceMultipliers - "low")
            assertThrows<IllegalArgumentException> { bad.validateOrThrow() }
        }

        @Test
        fun `gapped grade thresholds throw`() {
            val bad = ruleset.copy(
                gradeThresholds = mapOf(
                    "Low" to GradeRange(0, 40),
                    "High" to GradeRange(45, 100) // gap 41..44
                )
            )
            val ex = assertThrows<IllegalArgumentException> { bad.validateOrThrow() }
            assertEquals("ruleset_grade_thresholds_not_contiguous", ex.message)
        }

        @Test
        fun `thresholds not reaching 100 throw`() {
            val bad = ruleset.copy(
                gradeThresholds = mapOf(
                    "Low" to GradeRange(0, 40),
                    "High" to GradeRange(41, 99)
                )
            )
            assertThrows<IllegalArgumentException> { bad.validateOrThrow() }
        }

        @Test
        fun `empty signals throw`() {
            val bad = ruleset.copy(signals = emptyList())
            assertThrows<IllegalArgumentException> { bad.validateOrThrow() }
        }

        @Test
        fun `all-positive ruleset throws for missing negatives`() {
            val bad = ruleset.copy(signals = ruleset.signals.filter { it.direction > 0 })
            val ex = assertThrows<IllegalArgumentException> { bad.validateOrThrow() }
            assertTrue(ex.message!!.startsWith("ruleset_domain_missing_negative_signal:"))
        }
    }
}
