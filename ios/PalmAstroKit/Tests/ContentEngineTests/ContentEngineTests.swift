import Foundation
import Testing
import CoreContracts
@testable import ContentEngine

@Suite struct ContentTemplatesTests {

    @Test func defaultTemplatesLoadAndValidate() throws {
        let templates = try ContentTemplates.loadDefault()
        #expect(templates.version == "2.0.0")
        #expect(templates.fallbackLanguage == "en")
        #expect(templates.languages["en"] != nil)
        // zh-TW is a launch language (PRD §19).
        #expect(templates.languages["zh-TW"] != nil)
        for domain in Domains.all {
            #expect(templates.languages["en"]?.domains[domain] != nil)
            #expect(templates.languages["zh-TW"]?.domains[domain] != nil)
        }
    }

    @Test func languageResolution() throws {
        let templates = try ContentTemplates.loadDefault()
        #expect(templates.resolveLanguage("zh-TW").tag == "zh-TW")
        #expect(templates.resolveLanguage("zh").tag == "zh-TW")
        #expect(templates.resolveLanguage("fr").tag == "en")
        #expect(templates.resolveLanguage("en-US").tag == "en")
    }

    @Test func bandSelectionPicksHighestMatchingBand() throws {
        let templates = try ContentTemplates.loadDefault()
        let career = try #require(templates.languages["en"]?.domains["career"])
        #expect(career.band(forScore: 92)?.minScore == 80)
        #expect(career.band(forScore: 65)?.minScore == 65)
        #expect(career.band(forScore: 12)?.minScore == 0)
    }
}

@Suite struct ContentComposerTests {

    private func scoring(scores: [String: Int], confidence: String = "med") -> ScoringResult {
        ScoringResult(
            domainScores: scores,
            subdimScores: ["career.focus": 61, "wealth.saving": 58],
            grade: "Stable",
            confidence: confidence,
            confidenceReasons: ["missing_birth_time"],
            explainability: [
                ExplainEntry(signalId: "PALM_HEADLINE_LONG_CLEAR", mapping: "PALM_HEADLINE_LONG_CLEAR → career", contribution: 2.88),
                ExplainEntry(signalId: "PALM_LIFELINE_FAINT", mapping: "PALM_LIFELINE_FAINT → health", contribution: -1.44),
            ],
            matchedBuckets: [],
            rulesetVersion: "2.0.0"
        )
    }

    private func contentInput(language: String, scores: [String: Int] = ["career": 72, "wealth": 48, "family": 60, "health": 33]) -> ContentInput {
        ContentInput(
            scoringResult: scoring(scores: scores),
            deltaResult: DeltaResult(
                domainDeltas: ["career": DeltaValue(value: 5, arrow: "up")],
                subdimDeltas: [:], gradeShift: nil, comparabilityScore: 82,
                comparabilityBucket: .HIGH, prevMonthKey: "2026-06", currentMonthKey: "2026-07"
            ),
            tone: .SCIENTIFIC,
            entitlements: [],
            calcLevel: .L1,
            monthKey: "2026-07",
            language: language
        )
    }

    @Test func composeProducesAllFourDomains() throws {
        let composer = try ContentComposerImpl()
        let payloads = composer.compose(input: contentInput(language: "en"))
        #expect(Set(payloads.keys) == Set(Domains.all))
        for (domain, payload) in payloads {
            #expect(payload.domain == domain)
            #expect(payload.monthKey == "2026-07")
            #expect(payload.calcLevel == .L1)
            #expect(!payload.interpretation.pattern.isEmpty)
            #expect(!payload.actionToday.isEmpty)
            #expect(!payload.actionWeek.isEmpty)
            #expect(!payload.prompt.isEmpty)
            #expect(payload.confidenceReasons == ["missing_birth_time"])
        }
    }

    @Test func languageIsHonored() throws {
        let composer = try ContentComposerImpl()
        let en = composer.compose(input: contentInput(language: "en"))
        let zh = composer.compose(input: contentInput(language: "zh-TW"))

        #expect(en["career"]?.language == "en")
        #expect(zh["career"]?.language == "zh-TW")
        #expect(en["career"]?.interpretation.pattern != zh["career"]?.interpretation.pattern)
        // zh-TW copy must actually be CJK text.
        let zhPattern = zh["career"]?.interpretation.pattern ?? ""
        #expect(zhPattern.unicodeScalars.contains { $0.value > 0x2E80 })
    }

    @Test func unsupportedLanguageFallsBackToEnglish() throws {
        let composer = try ContentComposerImpl()
        let payloads = composer.compose(input: contentInput(language: "hi"))
        #expect(payloads["career"]?.language == "en")
    }

    @Test func safetyNotesPresentForWealthAndHealth() throws {
        let composer = try ContentComposerImpl()
        for language in ["en", "zh-TW"] {
            let payloads = composer.compose(input: contentInput(language: language))
            #expect(payloads["wealth"]?.safetyNotes.isEmpty == false, "\(language) wealth needs a soft-only note")
            #expect(payloads["health"]?.safetyNotes.isEmpty == false, "\(language) health needs a soft-only note")
            #expect(payloads["career"]?.safetyNotes.isEmpty == true)
        }
    }

    @Test func scoreCardCarriesDeltaAndDomainSubdims() throws {
        let composer = try ContentComposerImpl()
        let payloads = composer.compose(input: contentInput(language: "en"))
        let career = try #require(payloads["career"])
        #expect(career.scoreCard.totalScore == 72)
        #expect(career.scoreCard.delta == DeltaValue(value: 5, arrow: "up"))
        #expect(career.scoreCard.comparabilityScore == 82)
        #expect(career.scoreCard.subdims == ["career.focus": 61])
        let wealth = try #require(payloads["wealth"])
        #expect(wealth.scoreCard.delta == nil)
        #expect(wealth.scoreCard.subdims == ["wealth.saving": 58])
    }

    @Test func observationsComeFromDomainExplainability() throws {
        let composer = try ContentComposerImpl()
        let payloads = composer.compose(input: contentInput(language: "en"))
        let career = try #require(payloads["career"])
        #expect(career.observations.count == 1)
        #expect(career.observations[0].signalId == "PALM_HEADLINE_LONG_CLEAR")
        #expect(career.observations[0].displayName == "Head line long and clear")
        #expect(career.observations[0].evidenceSummary == "Positive (moderate)")

        let health = try #require(payloads["health"])
        #expect(health.observations[0].evidenceSummary == "Attention (subtle)")
    }

    @Test func highAndLowBandsDiffer() throws {
        let composer = try ContentComposerImpl()
        let payloads = composer.compose(input: contentInput(language: "en"))
        // career 72 -> high variants; health 33 -> low variants.
        #expect(payloads["career"]?.blindspot != payloads["health"]?.blindspot)
        let high = composer.compose(input: contentInput(language: "en", scores: ["career": 90, "wealth": 90, "family": 90, "health": 90]))
        let low = composer.compose(input: contentInput(language: "en", scores: ["career": 20, "wealth": 20, "family": 20, "health": 20]))
        #expect(high["career"]?.interpretation.pattern != low["career"]?.interpretation.pattern)
        #expect(high["career"]?.actionToday != low["career"]?.actionToday)
    }
}

@Suite struct ToneRendererTests {

    private func payload(language: String) throws -> SemanticPayload {
        let composer = try ContentComposerImpl()
        let input = ContentInput(
            scoringResult: ScoringResult(
                domainScores: ["career": 72, "wealth": 50, "family": 60, "health": 55],
                subdimScores: [:], grade: "Stable", confidence: "med",
                confidenceReasons: [], explainability: [], matchedBuckets: [], rulesetVersion: "2.0.0"
            ),
            deltaResult: nil, tone: .SCIENTIFIC, entitlements: [], calcLevel: .L1,
            monthKey: "2026-07", language: language
        )
        return try #require(composer.compose(input: input)["career"])
    }

    @Test func tonesProduceDistinctRenderings() throws {
        let renderer = try ToneRendererImpl()
        let p = try payload(language: "en")
        let scientific = renderer.render(payload: p, tone: .SCIENTIFIC)
        let healing = renderer.render(payload: p, tone: .HEALING)
        let roast = renderer.render(payload: p, tone: .ROAST_SAFE)

        #expect(scientific.text != healing.text)
        #expect(healing.text != roast.text)
        #expect(scientific.tone == .SCIENTIFIC)
        #expect(roast.domain == "career")
    }

    @Test func renderingContainsPayloadContentAndScore() throws {
        let renderer = try ToneRendererImpl()
        let p = try payload(language: "en")
        let rendered = renderer.render(payload: p, tone: .SCIENTIFIC)
        #expect(rendered.text.contains(p.interpretation.pattern))
        #expect(rendered.text.contains("72 / 100"))
        #expect(rendered.text.contains(p.actionToday))
        #expect(rendered.text.contains(p.prompt))
    }

    @Test func renderingUsesPayloadLanguageLabels() throws {
        let renderer = try ToneRendererImpl()
        let zh = try payload(language: "zh-TW")
        let rendered = renderer.render(payload: zh, tone: .HEALING)
        #expect(rendered.text.contains("今日行動"))
        #expect(rendered.text.contains("親愛的"))
    }
}
