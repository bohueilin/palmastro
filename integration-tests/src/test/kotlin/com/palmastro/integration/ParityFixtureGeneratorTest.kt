package com.palmastro.integration

import com.palmastro.astro.AstroEngineImpl
import com.palmastro.content.ContentComposerImpl
import com.palmastro.content.GuidanceBuilder
import com.palmastro.contracts.*
import com.palmastro.scoring.ScoringEngineImpl
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * Generates the cross-platform parity fixtures consumed by
 * ios/PalmAstroKit/Tests/ParityTests (PRD §10, Workstream C). The Android
 * engines are the reference implementation; this dumps {input, expected}
 * pairs with the canonical ruleset/template resources.
 *
 * Skipped unless -Dpalmastro.fixtures.dir=<path> is set, so it never runs in
 * normal CI. Regenerate whenever ruleset/template versions bump:
 *   ./gradlew :integration-tests:test --tests '*ParityFixtureGenerator*' \
 *     -Dpalmastro.fixtures.dir=$PWD/ios/shared-fixtures
 */
class ParityFixtureGeneratorTest {

    private val json = Json { prettyPrint = true }

    private fun strongFeatures() = PalmFeatures(
        headlinePresent = true, heartlinePresent = true, lifelinePresent = true, fatelinePresent = true,
        headlineShape = "curved", heartlineShape = "curved", lifelineShape = "curved", fatelineShape = "straight",
        headlineClarity = "clear", heartlineClarity = "clear", lifelineClarity = "clear", fatelineClarity = "clear",
        headlineLength = "long", fatelineLength = "long",
        venusMountDensity = "med", jupiterMountDensity = "med", saturnMountDensity = "med",
        minorLineDensity = "low",
    )

    private fun weakFeatures() = PalmFeatures(
        headlinePresent = true, heartlinePresent = true, lifelinePresent = true, fatelinePresent = true,
        headlineShape = "straight", heartlineShape = "straight", lifelineShape = "curved", fatelineShape = "straight",
        headlineClarity = "broken", heartlineClarity = "thin", lifelineClarity = "faint", fatelineClarity = "broken",
        headlineLength = "short", fatelineLength = "short",
        venusMountDensity = "low", jupiterMountDensity = "low", saturnMountDensity = "low",
        minorLineDensity = "high",
    )

    private fun featuresJson(f: PalmFeatures) = buildJsonObject {
        put("headlinePresent", f.headlinePresent); put("heartlinePresent", f.heartlinePresent)
        put("lifelinePresent", f.lifelinePresent); put("fatelinePresent", f.fatelinePresent)
        put("headlineShape", f.headlineShape); put("heartlineShape", f.heartlineShape)
        put("lifelineShape", f.lifelineShape); put("fatelineShape", f.fatelineShape)
        put("headlineClarity", f.headlineClarity); put("heartlineClarity", f.heartlineClarity)
        put("lifelineClarity", f.lifelineClarity); put("fatelineClarity", f.fatelineClarity)
        put("headlineLength", f.headlineLength); put("fatelineLength", f.fatelineLength)
        put("venusMountDensity", f.venusMountDensity); put("jupiterMountDensity", f.jupiterMountDensity)
        put("saturnMountDensity", f.saturnMountDensity); put("minorLineDensity", f.minorLineDensity)
    }

    private fun palmResultJson(r: PalmFeatureResult) = buildJsonObject {
        put("features", featuresJson(r.features))
        put("featureCoverage", r.featureCoverage)
        put("confidence", r.confidence)
        put("extractorVersion", r.extractorVersion)
    }

    private fun astroResultJson(a: AstroResult) = buildJsonObject {
        put("calcLevel", a.calcLevel.name)
        put("signals", buildJsonArray {
            a.signals.forEach { s ->
                add(buildJsonObject {
                    put("signalId", s.signalId); put("direction", s.direction)
                    put("magnitude", s.magnitude); put("confidence", s.confidence)
                    put("safetyTag", s.safetyTag)
                })
            }
        })
        put("engineVersion", a.engineVersion)
    }

    private fun scoringInputJson(input: ScoringInput) = buildJsonObject {
        put("palmFeatures", palmResultJson(input.palmFeatures))
        put("astroResult", astroResultJson(input.astroResult))
        put("userContext", buildJsonObject {
            put("dominantHand", input.userContext.dominantHand.name)
            put("oneHandOnly", input.userContext.oneHandOnly)
        })
        put("rulesetVersion", input.rulesetVersion)
    }

    private fun scoringResultJson(r: ScoringResult) = buildJsonObject {
        put("domainScores", buildJsonObject { r.domainScores.forEach { (k, v) -> put(k, v) } })
        put("subdimScores", buildJsonObject { r.subdimScores.forEach { (k, v) -> put(k, v) } })
        put("grade", r.grade)
        put("confidence", r.confidence)
        put("confidenceReasons", buildJsonArray { r.confidenceReasons.forEach { add(it) } })
        put("explainability", buildJsonArray {
            r.explainability.forEach {
                add(buildJsonObject {
                    put("signalId", it.signalId); put("mapping", it.mapping); put("contribution", it.contribution)
                })
            }
        })
        put("matchedBuckets", buildJsonArray { r.matchedBuckets.forEach { add(it) } })
        put("rulesetVersion", r.rulesetVersion)
    }

    @Test
    fun `generate parity fixtures`() {
        val dir = System.getProperty("palmastro.fixtures.dir")
        assumeTrue(dir != null, "set -Dpalmastro.fixtures.dir to generate fixtures")
        val root = File(dir!!)
        val scoringEngine = ScoringEngineImpl()
        val astroEngine = AstroEngineImpl()
        val composer = ContentComposerImpl()

        // -- scoring fixtures --------------------------------------------------
        val l2Astro = astroEngine.compute(LocalDate.of(1990, 7, 15), LocalTime.of(10, 0), 25.0330, 121.5654)
        val l1Astro = astroEngine.compute(LocalDate.of(1985, 11, 2), null, null, null)

        val scenarios = mapOf(
            "high_quality_l2" to ScoringInput(
                PalmFeatureResult(strongFeatures(), 0.92f, "high", "2.0.0"), l2Astro,
                UserContext(Hand.RIGHT, false), "2.0.0",
            ),
            "negative_palm_l1" to ScoringInput(
                PalmFeatureResult(weakFeatures(), 0.85f, "high", "2.0.0"), l1Astro,
                UserContext(Hand.LEFT, false), "2.0.0",
            ),
        )
        scenarios.forEach { (name, input) ->
            val result = scoringEngine.score(input)
            val fixture = buildJsonObject {
                put("input", scoringInputJson(input))
                put("expected", scoringResultJson(result))
            }
            File(root, "scoring/$name.json").apply { parentFile.mkdirs() }
                .writeText(json.encodeToString(fixture))
        }

        // -- content fixtures --------------------------------------------------
        val scoringHigh = scoringEngine.score(scenarios.getValue("high_quality_l2"))
        val scoringLow = scoringEngine.score(scenarios.getValue("negative_palm_l1"))
        val contentScenarios = mapOf(
            "growing_l2_en" to ContentInput(scoringHigh, null, Tone.SCIENTIFIC, emptySet(), CalcLevel.L2, "2026-07", "en"),
            "watchout_l1_zh" to ContentInput(scoringLow, null, Tone.HEALING, emptySet(), CalcLevel.L1, "2026-07", "zh-TW"),
        )
        contentScenarios.forEach { (name, input) ->
            val payloads = composer.compose(input)
            val fixture = buildJsonObject {
                put("input", buildJsonObject {
                    put("scoringResult", scoringResultJson(input.scoringResult))
                    put("tone", input.tone.name)
                    put("entitlements", buildJsonArray { })
                    put("calcLevel", input.calcLevel.name)
                    put("monthKey", input.monthKey)
                    put("language", input.language)
                })
                put("expected", json.parseToJsonElement(json.encodeToString(payloads)))
            }
            File(root, "content/$name.json").apply { parentFile.mkdirs() }
                .writeText(json.encodeToString(fixture))
        }

        // -- guidance fixtures ---------------------------------------------
        writeGuidanceFixtures(root, composer, contentScenarios.getValue("growing_l2_en"), scoringHigh.grade, scoringLow)
    }

    /**
     * Guidance fixture category (consumed from the guidance/ subdir):
     * signal_rich_en derives strengths/mindful from real explainability
     * entries of the growing L2 scenario; bucket_fallback_zh strips the low
     * reading's explainability so every card comes from the per-domain
     * bucket generics.
     */
    private fun writeGuidanceFixtures(
        root: File,
        composer: ContentComposerImpl,
        signalRichInput: ContentInput,
        signalRichGrade: String,
        scoringLow: ScoringResult,
    ) {
        val guidanceBuilder = GuidanceBuilder()
        val bucketScoring = scoringLow.copy(explainability = emptyList())
        val scenarios = mapOf(
            "signal_rich_en" to Triple(composer.compose(signalRichInput), signalRichGrade, "en"),
            "bucket_fallback_zh" to Triple(
                composer.compose(
                    ContentInput(bucketScoring, null, Tone.HEALING, emptySet(), CalcLevel.L1, "2026-07", "zh-TW"),
                ),
                bucketScoring.grade, "zh-TW",
            ),
        )
        scenarios.forEach { (name, scenario) ->
            val (payloads, overallGrade, language) = scenario
            val guidance = guidanceBuilder.build(payloads, overallGrade, language)
            val fixture = buildJsonObject {
                put("input", buildJsonObject {
                    put("payloads", json.parseToJsonElement(json.encodeToString(payloads)))
                    put("overallGrade", overallGrade)
                    put("language", language)
                })
                put("expected", json.parseToJsonElement(json.encodeToString(guidance)))
            }
            File(root, "guidance/$name.json").apply { parentFile.mkdirs() }
                .writeText(json.encodeToString(fixture))
        }
    }
}
