package com.palmastro.scoring

import com.palmastro.contracts.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignalResolverTest {
    private val ruleset = Ruleset.default()

    private fun makePalmFeatures(
        headlinePresent: Boolean = false, headlineClarity: String = "unclear", headlineLength: String = "medium",
        heartlinePresent: Boolean = false, heartlineClarity: String = "unclear",
        lifelinePresent: Boolean = false, lifelineClarity: String = "unclear",
        fatelinePresent: Boolean = false, fatelineClarity: String = "unclear",
        minorLineDensity: String = "low",
        confidence: String = "high",
    ) = PalmFeatureResult(
        features = PalmFeatures(
            headlinePresent = headlinePresent, heartlinePresent = heartlinePresent,
            lifelinePresent = lifelinePresent, fatelinePresent = fatelinePresent,
            headlineShape = "curved", heartlineShape = "curved", lifelineShape = "curved", fatelineShape = "straight",
            headlineClarity = headlineClarity, heartlineClarity = heartlineClarity,
            lifelineClarity = lifelineClarity, fatelineClarity = fatelineClarity,
            headlineLength = headlineLength, fatelineLength = "medium",
            venusMountDensity = "low", jupiterMountDensity = "low", saturnMountDensity = "low",
            minorLineDensity = minorLineDensity,
        ),
        featureCoverage = 0.9f, confidence = confidence, extractorVersion = "2.0.0"
    )

    private fun ids(f: PalmFeatureResult) = SignalResolver.resolvePalmSignals(f, ruleset).map { it.signalId }

    @Nested
    inner class PositivePalmSignals {
        @Test fun `headline long and clear matches PALM_HEADLINE_LONG_CLEAR`() {
            val f = makePalmFeatures(headlinePresent = true, headlineClarity = "clear", headlineLength = "long")
            assertEquals(listOf("PALM_HEADLINE_LONG_CLEAR"), ids(f))
        }
        @Test fun `headline present but medium clarity produces no headline positive`() {
            val f = makePalmFeatures(headlinePresent = true, headlineClarity = "medium", headlineLength = "long")
            assertTrue("PALM_HEADLINE_LONG_CLEAR" !in ids(f))
        }
        @Test fun `headline present and clear but not long produces no match`() {
            val f = makePalmFeatures(headlinePresent = true, headlineClarity = "clear", headlineLength = "short")
            assertTrue(ids(f).isEmpty())
        }
        @Test fun `heartline present and clear matches PALM_HEARTLINE_STRONG`() {
            val f = makePalmFeatures(heartlinePresent = true, heartlineClarity = "clear")
            assertEquals(listOf("PALM_HEARTLINE_STRONG"), ids(f))
        }
        @Test fun `heartline present and medium matches PALM_HEARTLINE_STRONG`() {
            val f = makePalmFeatures(heartlinePresent = true, heartlineClarity = "medium")
            assertEquals(listOf("PALM_HEARTLINE_STRONG"), ids(f))
        }
        @Test fun `lifeline present and clear matches PALM_LIFELINE_CLEAR`() {
            val f = makePalmFeatures(lifelinePresent = true, lifelineClarity = "clear")
            assertEquals(listOf("PALM_LIFELINE_CLEAR"), ids(f))
        }
        @Test fun `fateline present and clear matches PALM_FATELINE_STRONG`() {
            val f = makePalmFeatures(fatelinePresent = true, fatelineClarity = "clear")
            assertEquals(listOf("PALM_FATELINE_STRONG"), ids(f))
        }
        @Test fun `all positive features matching produces 4 palm signals`() {
            val f = makePalmFeatures(
                headlinePresent = true, headlineClarity = "clear", headlineLength = "long",
                heartlinePresent = true, heartlineClarity = "clear",
                lifelinePresent = true, lifelineClarity = "clear",
                fatelinePresent = true, fatelineClarity = "clear"
            )
            assertEquals(4, ids(f).size)
        }
    }

    @Nested
    inner class NegativePalmSignals {
        @Test fun `broken headline matches PALM_HEADLINE_CHAINED`() {
            val f = makePalmFeatures(headlinePresent = true, headlineClarity = "broken")
            assertEquals(listOf("PALM_HEADLINE_CHAINED"), ids(f))
        }
        @Test fun `broken fateline matches PALM_FATELINE_BREAKS`() {
            val f = makePalmFeatures(fatelinePresent = true, fatelineClarity = "broken")
            assertEquals(listOf("PALM_FATELINE_BREAKS"), ids(f))
        }
        @Test fun `thin heartline matches PALM_HEARTLINE_THIN`() {
            val f = makePalmFeatures(heartlinePresent = true, heartlineClarity = "thin")
            assertEquals(listOf("PALM_HEARTLINE_THIN"), ids(f))
        }
        @Test fun `faint lifeline matches PALM_LIFELINE_FAINT`() {
            val f = makePalmFeatures(lifelinePresent = true, lifelineClarity = "faint")
            assertEquals(listOf("PALM_LIFELINE_FAINT"), ids(f))
        }
        @Test fun `high minor line density matches PALM_MINOR_LINES_DENSE`() {
            val f = makePalmFeatures(minorLineDensity = "high")
            assertEquals(listOf("PALM_MINOR_LINES_DENSE"), ids(f))
        }
        @Test fun `med minor line density produces no match`() {
            val f = makePalmFeatures(minorLineDensity = "med")
            assertTrue(ids(f).isEmpty())
        }
        @Test fun `absent lines never trigger negatives`() {
            val f = makePalmFeatures(
                headlinePresent = false, headlineClarity = "broken",
                heartlinePresent = false, heartlineClarity = "thin",
                lifelinePresent = false, lifelineClarity = "faint",
                fatelinePresent = false, fatelineClarity = "broken"
            )
            assertTrue(ids(f).isEmpty())
        }
        @Test fun `resolved negatives carry direction -1 from ruleset`() {
            val f = makePalmFeatures(lifelinePresent = true, lifelineClarity = "faint")
            val signals = SignalResolver.resolvePalmSignals(f, ruleset)
            assertEquals(-1, signals.single().direction)
        }
        @Test fun `all negative features matching produces 5 palm signals`() {
            val f = makePalmFeatures(
                headlinePresent = true, headlineClarity = "broken",
                heartlinePresent = true, heartlineClarity = "thin",
                lifelinePresent = true, lifelineClarity = "faint",
                fatelinePresent = true, fatelineClarity = "broken",
                minorLineDensity = "high"
            )
            assertEquals(5, ids(f).size)
        }
    }

    @Test fun `no features matching produces empty list`() {
        assertTrue(ids(makePalmFeatures()).isEmpty())
    }

    @Nested
    inner class AstroSignals {
        @Test fun `astro signals with matching IDs resolve correctly`() {
            val astro = AstroResult(CalcLevel.L2, listOf(
                AstroSignal("ASTRO_SUN_FIRE", "+", 2, "high", "SAFE_GENERAL"),
                AstroSignal("ASTRO_MOON_WATER", "+", 2, "high", "SAFE_HEALTH_SOFT_ONLY"),
                AstroSignal("ASTRO_ASC_EARTH", "+", 2, "high", "SAFE_GENERAL")), "2.0.0")
            val resolved = SignalResolver.resolveAstroSignals(astro, ruleset)
            assertEquals(3, resolved.size)
        }
        @Test fun `sun modality signals resolve`() {
            val astro = AstroResult(CalcLevel.L1, listOf(
                AstroSignal("ASTRO_SUN_CARDINAL", "+", 2, "high", "SAFE_GENERAL")), "2.0.0")
            assertEquals(1, SignalResolver.resolveAstroSignals(astro, ruleset).size)
        }
        @Test fun `informational sun sign signal does not resolve to a ruleset entry`() {
            val astro = AstroResult(CalcLevel.L1, listOf(
                AstroSignal("ASTRO_SUN_ARIES", "+", 3, "high", "SAFE_GENERAL")), "2.0.0")
            assertTrue(SignalResolver.resolveAstroSignals(astro, ruleset).isEmpty())
        }
        @Test fun `removed planetary signals no longer resolve`() {
            val astro = AstroResult(CalcLevel.L2, listOf(
                AstroSignal("ASTRO_SATURN_STRONG", "+", 3, "med", "SAFE_CAREER"),
                AstroSignal("ASTRO_JUPITER_STRONG", "+", 3, "med", "SAFE_WEALTH_SOFT_ONLY")), "2.0.0")
            assertTrue(SignalResolver.resolveAstroSignals(astro, ruleset).isEmpty())
        }
        @Test fun `astro signals with non-matching IDs return empty`() {
            val astro = AstroResult(CalcLevel.L2, listOf(AstroSignal("NONEXISTENT", "+", 2, "low", "SAFE_GENERAL")), "2.0.0")
            assertTrue(SignalResolver.resolveAstroSignals(astro, ruleset).isEmpty())
        }
    }
}
