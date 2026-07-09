package com.palmastro.content

import com.palmastro.contracts.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ContentTemplateTest {

    @Test
    fun `default templates have all 4 domains`() {
        val templates = ContentTemplates.default()
        assertEquals(setOf("career", "wealth", "family", "health"), templates.domains.keys)
    }

    @Test
    fun `every domain template has non-blank strings for all fields`() {
        val templates = ContentTemplates.default()
        for ((domain, t) in templates.domains) {
            assertTrue(t.interpretationHigh.isNotBlank(), "$domain interpretationHigh is blank")
            assertTrue(t.interpretationLow.isNotBlank(), "$domain interpretationLow is blank")
            assertTrue(t.blindspot.isNotBlank(), "$domain blindspot is blank")
            assertTrue(t.actionToday.isNotBlank(), "$domain actionToday is blank")
            assertTrue(t.actionWeek.isNotBlank(), "$domain actionWeek is blank")
            assertTrue(t.prompt.isNotBlank(), "$domain prompt is blank")
        }
    }

    @Test
    fun `fromJson parses the resource file`() {
        val templates = ContentTemplates.fromResource("/default-templates.json")
        assertEquals(4, templates.domains.size)
        assertEquals("1.0.0", templates.version)
    }

    @Test
    fun `roundtrip default to json and back preserves all fields`() {
        val original = ContentTemplates.default()
        val json = original.toJson()
        val restored = ContentTemplates.fromJson(json)
        assertEquals(original, restored)
    }

    @Test
    fun `ContentComposerImpl with custom templates uses custom strings`() {
        val custom = ContentTemplates(
            version = "custom",
            domains = mapOf(
                "career" to DomainTemplate(
                    interpretationHigh = "CUSTOM_HIGH",
                    interpretationLow = "CUSTOM_LOW",
                    blindspot = "CUSTOM_BLIND",
                    actionToday = "CUSTOM_TODAY",
                    actionWeek = "CUSTOM_WEEK",
                    prompt = "CUSTOM_PROMPT",
                ),
                "wealth" to DomainTemplate(
                    interpretationHigh = "W_HIGH",
                    interpretationLow = "W_LOW",
                    blindspot = "W_BLIND",
                    actionToday = "W_TODAY",
                    actionWeek = "W_WEEK",
                    prompt = "W_PROMPT",
                ),
                "family" to DomainTemplate(
                    interpretationHigh = "F_HIGH",
                    interpretationLow = "F_LOW",
                    blindspot = "F_BLIND",
                    actionToday = "F_TODAY",
                    actionWeek = "F_WEEK",
                    prompt = "F_PROMPT",
                ),
                "health" to DomainTemplate(
                    interpretationHigh = "H_HIGH",
                    interpretationLow = "H_LOW",
                    blindspot = "H_BLIND",
                    actionToday = "H_TODAY",
                    actionWeek = "H_WEEK",
                    prompt = "H_PROMPT",
                ),
            ),
        )

        val composer = ContentComposerImpl(custom)
        val input = ContentInput(
            scoringResult = ScoringResult(
                domainScores = mapOf("career" to 80, "wealth" to 40, "family" to 50, "health" to 90),
                subdimScores = emptyMap(),
                grade = "Stable",
                confidence = "high",
                confidenceReasons = emptyList(),
                explainability = emptyList(),
                matchedBuckets = emptyList(),
                rulesetVersion = "1.0.0"
            ),
            deltaResult = null,
            tone = Tone.SCIENTIFIC,
            entitlements = emptySet(),
            calcLevel = CalcLevel.L2,
            monthKey = "2026-05"
        )

        val payloads = composer.compose(input)
        assertEquals("CUSTOM_HIGH", payloads["career"]!!.interpretationZh)
        assertEquals("W_LOW", payloads["wealth"]!!.interpretationZh)
        assertEquals("F_BLIND", payloads["family"]!!.blindspotZh)
        assertEquals("H_TODAY", payloads["health"]!!.actionTodayZh)
        assertEquals("CUSTOM_WEEK", payloads["career"]!!.actionWeekZh)
        assertEquals("W_PROMPT", payloads["wealth"]!!.promptZh)
    }

    @Test
    fun `missing domain in templates falls back gracefully`() {
        val partial = ContentTemplates(
            version = "partial",
            domains = mapOf(
                "career" to DomainTemplate(
                    interpretationHigh = "YES_HIGH",
                    interpretationLow = "YES_LOW",
                    blindspot = "YES_BLIND",
                    actionToday = "YES_TODAY",
                    actionWeek = "YES_WEEK",
                    prompt = "YES_PROMPT",
                ),
            ),
        )

        val composer = ContentComposerImpl(partial)
        val input = ContentInput(
            scoringResult = ScoringResult(
                domainScores = mapOf("career" to 80, "wealth" to 40, "family" to 50, "health" to 90),
                subdimScores = emptyMap(),
                grade = "Stable",
                confidence = "high",
                confidenceReasons = emptyList(),
                explainability = emptyList(),
                matchedBuckets = emptyList(),
                rulesetVersion = "1.0.0"
            ),
            deltaResult = null,
            tone = Tone.SCIENTIFIC,
            entitlements = emptySet(),
            calcLevel = CalcLevel.L2,
            monthKey = "2026-05"
        )

        val payloads = composer.compose(input)
        assertEquals("YES_HIGH", payloads["career"]!!.interpretationZh)
        assertEquals("", payloads["wealth"]!!.interpretationZh)
        assertEquals("", payloads["wealth"]!!.blindspotZh)
        assertEquals("", payloads["family"]!!.actionTodayZh)
        assertTrue(payloads["health"]!!.safetyNotesZh.isEmpty())
    }
}
