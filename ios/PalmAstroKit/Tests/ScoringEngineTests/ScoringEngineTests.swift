import Foundation
import Testing
import CoreContracts
@testable import ScoringEngine

@Suite struct RulesetTests {

    @Test func defaultRulesetLoadsAndValidates() throws {
        let ruleset = try Ruleset.loadDefault()
        #expect(ruleset.version == "2.0.0")
        #expect(ruleset.confidenceMultipliers["high"] == 1.0)
        #expect(ruleset.confidenceMultipliers["med"] == 0.8)
        #expect(ruleset.confidenceMultipliers["low"] == 0.5)
    }

    @Test func rulesetContainsAppendixA1NegativePalmSignals() throws {
        // Canonical v2 palm signal set (Kotlin RulesetTest parity):
        // four positives kept from v1 plus five negatives new in v2.
        let ruleset = try Ruleset.loadDefault()
        let ids = Set(ruleset.signals.map(\.signalId))
        for required in [
            "PALM_HEADLINE_LONG_CLEAR", "PALM_HEADLINE_CHAINED",
            "PALM_FATELINE_STRONG", "PALM_FATELINE_BREAKS",
            "PALM_HEARTLINE_STRONG", "PALM_HEARTLINE_THIN",
            "PALM_LIFELINE_CLEAR", "PALM_LIFELINE_FAINT",
            "PALM_MINOR_LINES_DENSE",
        ] {
            #expect(ids.contains(required), "missing \(required)")
        }
        #expect(ruleset.signals.count == 24, "canonical v2 ruleset carries exactly 24 signals")
        let negatives = ruleset.signals.filter { $0.direction < 0 }
        #expect(negatives.count >= 4, "v2 ruleset must include negative palm signals")
    }

    @Test func rulesetCoversV2AstroSignalSet() throws {
        let ruleset = try Ruleset.loadDefault()
        let ids = Set(ruleset.signals.map(\.signalId))
        for element in ["FIRE", "EARTH", "AIR", "WATER"] {
            #expect(ids.contains("ASTRO_SUN_\(element)"))
            #expect(ids.contains("ASTRO_MOON_\(element)"))
            #expect(ids.contains("ASTRO_ASC_\(element)"))
        }
        for modality in ["CARDINAL", "FIXED", "MUTABLE"] {
            #expect(ids.contains("ASTRO_SUN_\(modality)"))
        }
        // Fabricated planetary-strength signals must be gone in v2.
        #expect(!ids.contains("ASTRO_SATURN_STRONG"))
        #expect(!ids.contains("ASTRO_JUPITER_STRONG"))
    }

    @Test func gradeAssignment() throws {
        let ruleset = try Ruleset.loadDefault()
        #expect(ruleset.grade(forScore: 0) == "Watchout")
        #expect(ruleset.grade(forScore: 35) == "Watchout")
        #expect(ruleset.grade(forScore: 36) == "Building")
        #expect(ruleset.grade(forScore: 56) == "Stable")
        #expect(ruleset.grade(forScore: 75) == "Stable")
        #expect(ruleset.grade(forScore: 76) == "Growing")
        #expect(ruleset.grade(forScore: 100) == "Growing")
    }

    @Test func invalidRulesetRejected() {
        let empty = Ruleset(version: "", signals: [], gradeThresholds: [:], confidenceMultipliers: [:])
        #expect(throws: (any Error).self) {
            try empty.validate()
        }
    }
}

@Suite struct ScoringEngineTests {

    private func features(
        headlineClarity: String = "clear",
        headlineLength: String = "long",
        headlineShape: String = "smooth",
        heartlineClarity: String = "clear",
        lifelineClarity: String = "moderate",
        fatelineShape: String = "smooth",
        fatelineClarity: String = "clear",
        confidence: String = "high",
        coverage: Float = 0.8
    ) -> PalmFeatureResult {
        PalmFeatureResult(
            features: PalmFeatures(
                headlinePresent: true, heartlinePresent: true, lifelinePresent: true, fatelinePresent: true,
                headlineShape: headlineShape, heartlineShape: "smooth", lifelineShape: "smooth", fatelineShape: fatelineShape,
                headlineClarity: headlineClarity, heartlineClarity: heartlineClarity,
                lifelineClarity: lifelineClarity, fatelineClarity: fatelineClarity,
                headlineLength: headlineLength, fatelineLength: "medium",
                venusMountDensity: "unknown", jupiterMountDensity: "unknown",
                saturnMountDensity: "unknown", minorLineDensity: "unknown"
            ),
            featureCoverage: coverage,
            confidence: confidence,
            extractorVersion: "2.0.0"
        )
    }

    private func astro(calcLevel: CalcLevel = .L2, signals: [AstroSignal] = []) -> AstroResult {
        AstroResult(calcLevel: calcLevel, signals: signals, engineVersion: "2.0.0")
    }

    private func input(palm: PalmFeatureResult, astro: AstroResult) -> ScoringInput {
        ScoringInput(
            palmFeatures: palm,
            astroResult: astro,
            userContext: UserContext(dominantHand: .RIGHT, oneHandOnly: false),
            rulesetVersion: "2.0.0"
        )
    }

    @Test func positiveSignalsRaiseScoresAboveBaseline() throws {
        let engine = try ScoringEngineImpl()
        let result = engine.score(input: input(
            palm: features(),
            astro: astro(signals: [AstroSignal(signalId: "ASTRO_SUN_FIRE", direction: "+", magnitude: 3, confidence: "high", safetyTag: "SAFE_GENERAL")])
        ))
        #expect((result.domainScores["career"] ?? 0) > 50)
        #expect((result.domainScores["health"] ?? 0) > 50)
        #expect(result.rulesetVersion == "2.0.0")
        #expect(Set(result.domainScores.keys) == Set(Domains.all))
    }

    @Test func negativeSignalsLowerScores() throws {
        let engine = try ScoringEngineImpl()
        // Chained headline + faint lifeline + faint heartline, nothing positive.
        let negativePalm = features(
            headlineClarity: "moderate", headlineLength: "medium", headlineShape: "chained",
            heartlineClarity: "faint", lifelineClarity: "faint",
            fatelineShape: "chained", fatelineClarity: "moderate"
        )
        let result = engine.score(input: input(palm: negativePalm, astro: astro()))
        #expect((result.domainScores["career"] ?? 100) < 50)
        #expect((result.domainScores["health"] ?? 100) < 50)
        #expect((result.domainScores["family"] ?? 100) < 50)
    }

    @Test func scoresAreClampedTo0To100() throws {
        let engine = try ScoringEngineImpl()
        let result = engine.score(input: input(palm: features(), astro: astro()))
        for (_, score) in result.domainScores {
            #expect(score >= 0 && score <= 100)
        }
    }

    @Test func deterministicScoring() throws {
        let engine = try ScoringEngineImpl()
        let scoringInput = input(
            palm: features(),
            astro: astro(signals: [AstroSignal(signalId: "ASTRO_MOON_WATER", direction: "+", magnitude: 2, confidence: "high", safetyTag: "SAFE_HEALTH_SOFT_ONLY")])
        )
        #expect(engine.score(input: scoringInput) == engine.score(input: scoringInput))
    }

    @Test func lowPalmConfidenceGatesMedSignalsAndAddsReasons() throws {
        let engine = try ScoringEngineImpl()
        let lowConfidence = features(confidence: "low", coverage: 0.3)
        let result = engine.score(input: input(palm: lowConfidence, astro: astro(calcLevel: .L1)))
        // All palm signals in the ruleset need minConfidence "med": none apply.
        #expect(result.domainScores["career"] == 50)
        #expect(result.confidence == "low")
        #expect(result.confidenceReasons.contains("scan_quality_low"))
        #expect(result.confidenceReasons.contains("low_feature_coverage"))
        #expect(result.confidenceReasons.contains("missing_birth_time"))
    }

    @Test func explainabilitySortedByAbsoluteContribution() throws {
        let engine = try ScoringEngineImpl()
        let result = engine.score(input: input(
            palm: features(),
            astro: astro(signals: [AstroSignal(signalId: "ASTRO_SUN_EARTH", direction: "+", magnitude: 2, confidence: "high", safetyTag: "SAFE_GENERAL")])
        ))
        let contributions = result.explainability.map { abs($0.contribution) }
        #expect(contributions == contributions.sorted(by: >))
        #expect(!result.explainability.isEmpty)
        // Mapping strings keep the Kotlin "SIGNAL → domain" format.
        #expect(result.explainability.allSatisfy { $0.mapping.contains(" → ") })
    }

    @Test func unknownAstroSignalsAreIgnored() throws {
        let engine = try ScoringEngineImpl()
        let withUnknown = input(
            palm: features(),
            astro: astro(signals: [AstroSignal(signalId: "ASTRO_NOT_IN_RULESET", direction: "+", magnitude: 9, confidence: "high", safetyTag: "SAFE_GENERAL")])
        )
        let without = input(palm: features(), astro: astro())
        #expect(engine.score(input: withUnknown).domainScores == engine.score(input: without).domainScores)
    }
}

@Suite struct DeltaEngineTests {

    private func monthly(monthKey: String, scores: [String: Int], grade: String, quality: Int, coverage: Float) -> MonthlyResult {
        MonthlyResult(
            resultId: "r-\(monthKey)", monthKey: monthKey, scanSessionId: "s-\(monthKey)",
            scoringResult: ScoringResult(
                domainScores: scores, subdimScores: [:], grade: grade, confidence: "med",
                confidenceReasons: [], explainability: [], matchedBuckets: [], rulesetVersion: "2.0.0"
            ),
            semanticPayloads: [:],
            scanQualityScore: quality, featureCoverage: coverage, createdAt: 0
        )
    }

    @Test func domainDeltasAndArrows() {
        let prev = monthly(monthKey: "2026-06", scores: ["career": 50, "wealth": 60, "family": 55, "health": 40], grade: "Building", quality: 80, coverage: 0.7)
        let curr = monthly(monthKey: "2026-07", scores: ["career": 58, "wealth": 52, "family": 55, "health": 47], grade: "Stable", quality: 82, coverage: 0.72)
        let delta = DeltaEngineImpl().computeDelta(prev: prev, current: curr)

        #expect(delta.domainDeltas["career"] == DeltaValue(value: 8, arrow: "up"))
        #expect(delta.domainDeltas["wealth"] == DeltaValue(value: -8, arrow: "down"))
        #expect(delta.domainDeltas["family"] == DeltaValue(value: 0, arrow: "flat"))
        #expect(delta.gradeShift == GradeShift(from: "Building", to: "Stable"))
        #expect(delta.prevMonthKey == "2026-06")
        #expect(delta.currentMonthKey == "2026-07")
        #expect(delta.comparabilityBucket == .HIGH)
    }

    @Test func comparabilityDropsWithQualityGap() {
        let prev = monthly(monthKey: "2026-06", scores: [:], grade: "Stable", quality: 90, coverage: 0.9)
        let curr = monthly(monthKey: "2026-07", scores: [:], grade: "Stable", quality: 45, coverage: 0.4)
        let delta = DeltaEngineImpl().computeDelta(prev: prev, current: curr)
        #expect(delta.comparabilityScore < 70)
        #expect(delta.gradeShift == nil)
    }
}
