import Foundation
import Testing
@testable import CoreContracts

/// Verifies the Swift contracts stay JSON-compatible with the Kotlin
/// contracts module (kotlinx.serialization field names and enum names).
@Suite struct ContractsCodingTests {

    @Test func domainsMatchContract() {
        #expect(Domains.all == ["career", "wealth", "family", "health"])
    }

    @Test func productIdsMatchContract() {
        #expect(ProductIds.all == ["palmastro.pack.career", "palmastro.pack.wealth", "palmastro.pack.bundle"])
    }

    @Test func enumRawValuesMatchKotlinNames() {
        #expect(Hand.LEFT.rawValue == "LEFT")
        #expect(Angle.LEFT_TILT.rawValue == "LEFT_TILT")
        #expect(Angle.allCases.count == 7)
        #expect(CalcLevel.L2.rawValue == "L2")
        #expect(Tone.ROAST_SAFE.rawValue == "ROAST_SAFE")
        #expect(ComparabilityBucket.MED.rawValue == "MED")
    }

    /// SemanticPayload must decode Kotlin-produced JSON: nested interpretation
    /// object {pattern, trigger, cost}; missing language/confidenceReasons
    /// fall back to the Kotlin defaults.
    @Test func semanticPayloadDecodesKotlinShapeWithDefaults() throws {
        let json = """
        {
          "domain": "career",
          "monthKey": "2026-07",
          "calcLevel": "L1",
          "confidence": "med",
          "observations": [
            {"signalId": "PALM_HEADLINE_LONG_CLEAR", "displayName": "Headline Long Clear", "evidenceSummary": "Positive (strong)"}
          ],
          "interpretation": {"pattern": "p", "trigger": "t", "cost": "c"},
          "blindspot": "b",
          "actionToday": "a1",
          "actionWeek": "a2",
          "prompt": "q",
          "safetyNotes": [],
          "explainability": [
            {"signalId": "PALM_HEADLINE_LONG_CLEAR", "mapping": "PALM_HEADLINE_LONG_CLEAR → career", "contribution": 2.88}
          ],
          "scoreCard": {"totalScore": 62, "grade": "Stable", "delta": null, "comparabilityScore": null, "subdims": {}}
        }
        """
        let payload = try JSONDecoder().decode(SemanticPayload.self, from: Data(json.utf8))
        #expect(payload.language == "en")
        #expect(payload.confidenceReasons == [])
        #expect(payload.interpretation == Interpretation(pattern: "p", trigger: "t", cost: "c"))
        #expect(payload.scoreCard.totalScore == 62)
        #expect(payload.scoreCard.delta == nil)
    }

    @Test func interpretationDefaultsForMissingTriggerAndCost() throws {
        let json = #"{"pattern": "only pattern"}"#
        let interpretation = try JSONDecoder().decode(Interpretation.self, from: Data(json.utf8))
        #expect(interpretation.trigger == "")
        #expect(interpretation.cost == "")
    }

    @Test func contentInputLanguageDefaultsToEnglish() throws {
        let json = """
        {
          "scoringResult": {
            "domainScores": {"career": 60}, "subdimScores": {}, "grade": "Stable",
            "confidence": "med", "confidenceReasons": [], "explainability": [],
            "matchedBuckets": [], "rulesetVersion": "2.0.0"
          },
          "deltaResult": null,
          "tone": "SCIENTIFIC",
          "entitlements": [],
          "calcLevel": "L1",
          "monthKey": "2026-07"
        }
        """
        let input = try JSONDecoder().decode(ContentInput.self, from: Data(json.utf8))
        #expect(input.language == "en")
    }

    /// [Angle: BestFrameResult] must encode as a JSON object keyed by enum
    /// name (kotlinx Map<Angle, BestFrameResult> shape), not as an array.
    @Test func angleKeyedMapEncodesAsJsonObject() throws {
        let frame = BestFrameResult(
            angle: .FRONT,
            frameIndex: 3,
            qualityScores: QualityScores(blur: 0.9, glare: 0.8, exposure: 0.7, coverage: 0.95, stability: 0.85, composite: 84),
            fileRef: nil,
            palmMetrics: nil
        )
        let summary = ScanSessionSummary(
            sessionId: "s1", hand: .RIGHT, angleResults: [.FRONT: frame],
            overallQualityScore: 84, featureCoverage: 0.8, totalDurationMs: 1200, totalAttempts: 2
        )
        let data = try JSONEncoder().encode(summary)
        let object = try #require(try JSONSerialization.jsonObject(with: data) as? [String: Any])
        let angleResults = try #require(object["angleResults"] as? [String: Any])
        #expect(angleResults["FRONT"] != nil)

        let roundTripped = try JSONDecoder().decode(ScanSessionSummary.self, from: data)
        #expect(roundTripped == summary)
    }

    @Test func palmMetricsRoundTrip() throws {
        let metrics = PalmMetrics(
            landmarks: (0..<21).map { LandmarkPoint(x: Float($0) / 21.0, y: 0.5, z: 0) },
            lineRegions: [
                LineRegionMetrics(region: "headline", contrast: 0.7, continuity: 0.8, meanIntensity: 0.4),
                LineRegionMetrics(region: "heartline", contrast: 0.6, continuity: 0.7, meanIntensity: 0.5),
                LineRegionMetrics(region: "lifeline", contrast: 0.65, continuity: 0.75, meanIntensity: 0.45),
                LineRegionMetrics(region: "fateline", contrast: 0.3, continuity: 0.4, meanIntensity: 0.6),
            ]
        )
        let data = try JSONEncoder().encode(metrics)
        let decoded = try JSONDecoder().decode(PalmMetrics.self, from: data)
        #expect(decoded == metrics)
        #expect(decoded.landmarks.count == 21)
    }

    @Test func monthlyResultRoundTrip() throws {
        let scoring = ScoringResult(
            domainScores: ["career": 61, "wealth": 58, "family": 55, "health": 57],
            subdimScores: [:],
            grade: "Stable",
            confidence: "med",
            confidenceReasons: ["missing_birth_time"],
            explainability: [ExplainEntry(signalId: "ASTRO_SUN_FIRE", mapping: "ASTRO_SUN_FIRE → career", contribution: 0.8)],
            matchedBuckets: [],
            rulesetVersion: "2.0.0"
        )
        let payload = SemanticPayload(
            domain: "career", monthKey: "2026-07", calcLevel: .L1, confidence: "med",
            observations: [], interpretation: Interpretation(pattern: "p"),
            blindspot: "b", actionToday: "a", actionWeek: "w", prompt: "q",
            safetyNotes: [], explainability: [],
            scoreCard: ScoreCard(totalScore: 61, grade: "Stable", delta: nil, comparabilityScore: nil, subdims: [:])
        )
        let result = MonthlyResult(
            resultId: "r1", monthKey: "2026-07", scanSessionId: "s1",
            scoringResult: scoring, semanticPayloads: ["career": payload],
            scanQualityScore: 82, featureCoverage: 0.7, createdAt: 1_780_000_000_000
        )
        let data = try JSONEncoder().encode(result)
        let decoded = try JSONDecoder().decode(MonthlyResult.self, from: data)
        #expect(decoded == result)
    }
}
