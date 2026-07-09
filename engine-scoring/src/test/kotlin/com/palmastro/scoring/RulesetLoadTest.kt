package com.palmastro.scoring

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class RulesetLoadTest {
    @Test fun `fromJson parses default JSON resource`() {
        val json = Ruleset::class.java.getResourceAsStream("/default-ruleset.json")!!.bufferedReader().readText()
        val r = Ruleset.fromJson(json)
        assertEquals("1.0.0", r.version); assertEquals(7, r.signals.size); assertEquals(4, r.gradeThresholds.size)
    }
    @Test fun `toJson then fromJson roundtrip`() {
        val original = Ruleset.default(); val restored = Ruleset.fromJson(Ruleset.toJson(original))
        assertEquals(original, restored)
    }
    @Test fun `invalid JSON throws`() { assertThrows<Exception> { Ruleset.fromJson("not json") } }
    @Test fun `custom ruleset works`() {
        val r = Ruleset.fromJson("""{"version":"2.0","signals":[{"signalId":"X","source":"TEST","direction":1,"magnitude":1,"minConfidence":"low","domainWeights":{"career":1.0},"safetyTag":"SAFE"}],"gradeThresholds":{"Low":{"min":0,"max":50},"High":{"min":51,"max":100}},"confidenceMultipliers":{"high":1.0}}""")
        assertEquals("2.0", r.version); assertEquals(1, r.signals.size); assertEquals(0..50, r.gradeIntRanges["Low"])
    }
    @Test fun `default matches resource file`() {
        val fromDefault = Ruleset.default()
        val fromFile = Ruleset.fromJson(Ruleset::class.java.getResourceAsStream("/default-ruleset.json")!!.bufferedReader().readText())
        assertEquals(fromDefault, fromFile)
    }
}
