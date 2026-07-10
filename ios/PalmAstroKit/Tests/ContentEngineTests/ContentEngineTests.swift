import Foundation
import Testing
import CoreContracts
@testable import ContentEngine

@Suite struct ContentTemplatesTests {

    private static let launchLanguages = ["en", "zh-TW", "zh-CN", "ja", "hi"]

    @Test func defaultTemplatesLoad() throws {
        let templates = try ContentTemplates.loadDefault()
        #expect(templates.version == "2.0.0")
        #expect(templates.defaultLanguage == "en")
        #expect(templates.languages == Self.launchLanguages)
        #expect(Set(templates.domains.keys) == Set(Domains.all))
        for bucket in ["peak", "rising", "transition", "building", "attention", "high", "low"] {
            #expect(templates.buckets[bucket] != nil, "missing bucket \(bucket)")
        }
        #expect(Set(templates.tones.keys) == Set(Tone.allCases.map(\.rawValue)))
    }

    @Test func languageResolutionMatchesKotlin() throws {
        let templates = try ContentTemplates.loadDefault()
        // Exact membership in `languages`, otherwise the default language —
        // no primary-subtag matching (Kotlin `resolveLanguage` parity).
        for language in Self.launchLanguages {
            #expect(templates.resolveLanguage(language) == language)
        }
        #expect(templates.resolveLanguage("fr") == "en")
        #expect(templates.resolveLanguage("zh") == "en")
        #expect(templates.resolveLanguage("en-US") == "en")
    }

    @Test func bucketTextSelectsTheContainingBucket() throws {
        let templates = try ContentTemplates.loadDefault()
        let career = try #require(templates.domains["career"])
        let pattern = career.interpretation.pattern
        #expect(templates.bucketText(pattern, score: 92, language: "en")
                == templates.localized(try #require(pattern["peak"]), language: "en"))
        #expect(templates.bucketText(pattern, score: 65, language: "en")
                == templates.localized(try #require(pattern["rising"]), language: "en"))
        #expect(templates.bucketText(pattern, score: 12, language: "en")
                == templates.localized(try #require(pattern["attention"]), language: "en"))
        // trigger/cost and action fields use the high (>= 65) / low (<= 64) split.
        let trigger = career.interpretation.trigger
        #expect(templates.bucketText(trigger, score: 65, language: "en")
                == templates.localized(try #require(trigger["high"]), language: "en"))
        #expect(templates.bucketText(trigger, score: 64, language: "en")
                == templates.localized(try #require(trigger["low"]), language: "en"))
    }

    @Test func everyScoreMapsToABucketForEveryDomainField() throws {
        let templates = try ContentTemplates.loadDefault()
        for (domain, template) in templates.domains {
            for score in 0...100 {
                #expect(!templates.bucketText(template.interpretation.pattern, score: score, language: "en").isBlank,
                        "\(domain) pattern uncovered at score \(score)")
                #expect(!templates.bucketText(template.actionToday, score: score, language: "en").isBlank,
                        "\(domain) actionToday uncovered at score \(score)")
            }
        }
    }

    @Test func everyDomainFieldBucketAndLanguageHasCopy() throws {
        let templates = try ContentTemplates.loadDefault()
        for (domain, t) in templates.domains {
            let fields: [String: [String: LocalizedText]] = [
                "interpretation.pattern": t.interpretation.pattern,
                "interpretation.trigger": t.interpretation.trigger,
                "interpretation.cost": t.interpretation.cost,
                "blindspot": t.blindspot,
                "actionToday": t.actionToday,
                "actionWeek": t.actionWeek,
                "prompt": t.prompt,
            ]
            for (name, field) in fields {
                #expect(!field.isEmpty, "\(domain).\(name) has no buckets")
                for (bucket, localized) in field {
                    #expect(templates.buckets[bucket] != nil, "\(domain).\(name) references unknown bucket \(bucket)")
                    for lang in Self.launchLanguages {
                        #expect(!(localized[lang] ?? "").isBlank, "\(domain).\(name)[\(bucket)][\(lang)] is blank")
                    }
                }
            }
            for lang in Self.launchLanguages {
                #expect(!(t.displayName[lang] ?? "").isBlank, "\(domain).displayName[\(lang)] blank")
            }
        }
    }

    @Test func roundtripThroughJSONPreservesTheLibrary() throws {
        let original = try ContentTemplates.loadDefault()
        let data = try JSONEncoder().encode(original)
        #expect(try ContentTemplates.fromJSON(data) == original)
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
            #expect(!payload.interpretation.pattern.isBlank, "\(domain) pattern blank")
            #expect(!payload.interpretation.trigger.isBlank, "\(domain) trigger blank")
            #expect(!payload.interpretation.cost.isBlank, "\(domain) cost blank")
            #expect(!payload.blindspot.isBlank, "\(domain) blindspot blank")
            #expect(!payload.actionToday.isBlank, "\(domain) actionToday blank")
            #expect(!payload.actionWeek.isBlank, "\(domain) actionWeek blank")
            #expect(!payload.prompt.isBlank, "\(domain) prompt blank")
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

    @Test func allLaunchLanguagesAreHonored() throws {
        let composer = try ContentComposerImpl()
        for language in ["en", "zh-TW", "zh-CN", "ja", "hi"] {
            let payloads = composer.compose(input: contentInput(language: language))
            #expect(payloads["career"]?.language == language)
        }
    }

    @Test func unsupportedLanguageFallsBackToEnglish() throws {
        let composer = try ContentComposerImpl()
        let payloads = composer.compose(input: contentInput(language: "fr"))
        #expect(payloads["career"]?.language == "en")
        #expect(payloads["career"]?.interpretation.pattern
                == composer.compose(input: contentInput(language: "en"))["career"]?.interpretation.pattern)
    }

    @Test func composeIsDeterministic() throws {
        let composer = try ContentComposerImpl()
        #expect(composer.compose(input: contentInput(language: "en"))
                == composer.compose(input: contentInput(language: "en")))
    }

    @Test func templatesVersionIsExposed() throws {
        #expect(try ContentComposerImpl().templatesVersion == "2.0.0")
    }

    @Test func safetyNotesPresentForWealthAndHealth() throws {
        let composer = try ContentComposerImpl()
        for language in ["en", "zh-TW"] {
            let payloads = composer.compose(input: contentInput(language: language))
            #expect(payloads["wealth"]?.safetyNotes.isEmpty == false, "\(language) wealth needs a soft-only note")
            #expect(payloads["health"]?.safetyNotes.isEmpty == false, "\(language) health needs a soft-only note")
            #expect(payloads["career"]?.safetyNotes.isEmpty == true)
            #expect(payloads["family"]?.safetyNotes.isEmpty == true)
        }
    }

    @Test func scoreCardCarriesDeltaAndDomainSubdims() throws {
        let composer = try ContentComposerImpl()
        let payloads = composer.compose(input: contentInput(language: "en"))
        let career = try #require(payloads["career"])
        #expect(career.scoreCard.totalScore == 72)
        #expect(career.scoreCard.grade == "Stable")
        #expect(career.scoreCard.delta == DeltaValue(value: 5, arrow: "up"))
        #expect(career.scoreCard.comparabilityScore == 82)
        #expect(career.scoreCard.subdims == ["career.focus": 61])
        let wealth = try #require(payloads["wealth"])
        #expect(wealth.scoreCard.delta == nil)
        #expect(wealth.scoreCard.subdims == ["wealth.saving": 58])
    }

    @Test func observationsComeFromDomainExplainability() throws {
        let templates = try ContentTemplates.loadDefault()
        let composer = ContentComposerImpl(templates: templates)
        let payloads = composer.compose(input: contentInput(language: "en"))
        let career = try #require(payloads["career"])
        #expect(career.observations.count == 1)
        #expect(career.observations[0].signalId == "PALM_HEADLINE_LONG_CLEAR")
        #expect(career.observations[0].displayName == "Head line — long and clear")
        let expectedEvidence = templates.localized(
            try #require(templates.observations["PALM_HEADLINE_LONG_CLEAR"]).evidenceSummary, language: "en"
        )
        #expect(!expectedEvidence.isBlank)
        #expect(career.observations[0].evidenceSummary == expectedEvidence)

        let health = try #require(payloads["health"])
        #expect(health.observations[0].signalId == "PALM_LIFELINE_FAINT")
        #expect(health.observations[0].displayName == "Life line — faint")
        #expect(!health.observations[0].evidenceSummary.isBlank)
    }

    @Test func unknownSignalIdFallsBackToHumanizedNameAndGenericEvidence() throws {
        let composer = try ContentComposerImpl()
        var input = contentInput(language: "en")
        input = ContentInput(
            scoringResult: ScoringResult(
                domainScores: input.scoringResult.domainScores,
                subdimScores: input.scoringResult.subdimScores,
                grade: input.scoringResult.grade,
                confidence: input.scoringResult.confidence,
                confidenceReasons: input.scoringResult.confidenceReasons,
                explainability: [
                    ExplainEntry(signalId: "PALM_MINOR_LINES_DENSE", mapping: "PALM_MINOR_LINES_DENSE → career", contribution: 1.0)
                ],
                matchedBuckets: [],
                rulesetVersion: "2.0.0"
            ),
            deltaResult: input.deltaResult, tone: input.tone, entitlements: input.entitlements,
            calcLevel: input.calcLevel, monthKey: input.monthKey, language: input.language
        )
        let observation = try #require(composer.compose(input: input)["career"]?.observations.first)
        #expect(observation.displayName == "Minor Lines Dense")
        #expect(observation.evidenceSummary == "This signal contributes to your overall reading.")
    }

    @Test func scoreBucketDrivesInterpretationCopy() throws {
        let composer = try ContentComposerImpl()
        let high = composer.compose(input: contentInput(language: "en", scores: ["career": 90, "wealth": 90, "family": 90, "health": 90]))
        let low = composer.compose(input: contentInput(language: "en", scores: ["career": 20, "wealth": 20, "family": 20, "health": 20]))
        #expect(high["career"]?.interpretation.pattern != low["career"]?.interpretation.pattern)
        #expect(high["career"]?.interpretation.trigger != low["career"]?.interpretation.trigger)
        #expect(high["career"]?.actionToday != low["career"]?.actionToday)
        #expect(high["career"]?.blindspot != low["career"]?.blindspot)
    }

    @Test func domainPlaceholderIsSubstituted() throws {
        let composer = try ContentComposerImpl()
        // zh-CN peak career copy carries a literal {domain} in the canonical JSON.
        let payloads = composer.compose(input: contentInput(language: "zh-CN", scores: ["career": 90, "wealth": 90, "family": 90, "health": 90]))
        for (domain, payload) in payloads {
            #expect(!payload.interpretation.pattern.contains("{domain}"), "\(domain) leaked the {domain} placeholder")
        }
        #expect(payloads["career"]?.interpretation.pattern.contains("事业") == true)
    }

    @Test func safeFallbackPayloadIsLocalizedAndPreservesBaseMetadata() throws {
        let composer = try ContentComposerImpl()
        let base = try #require(composer.compose(input: contentInput(language: "en"))["career"])
        let fallback = composer.safeFallbackPayload(domain: "career", language: "zh-TW", base: base)
        #expect(fallback.language == "zh-TW")
        #expect(fallback.monthKey == base.monthKey)
        #expect(fallback.calcLevel == base.calcLevel)
        #expect(fallback.scoreCard == base.scoreCard)
        #expect(!fallback.interpretation.pattern.isBlank)
        #expect(fallback.observations.isEmpty)
        #expect(fallback.explainability.isEmpty)

        let bare = composer.safeFallbackPayload(domain: "wealth", language: "en")
        #expect(bare.monthKey == "")
        #expect(bare.confidence == "low")
        #expect(bare.safetyNotes.isEmpty == false, "wealth fallback keeps safety notes")
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
        #expect(rendered.text.hasPrefix("Career — 72/100 · Stable"))
        #expect(rendered.text.contains(p.interpretation.pattern))
        #expect(rendered.text.contains(p.interpretation.trigger))
        #expect(rendered.text.contains(p.interpretation.cost))
        #expect(rendered.text.contains("Today: \(p.actionToday)"))
        #expect(rendered.text.contains("This week: \(p.actionWeek)"))
        #expect(rendered.text.contains("Reflection: \(p.prompt)"))
        #expect(rendered.text.contains("Blindspot: \(p.blindspot)"))
    }

    @Test func tonePrefixesComeFromTemplates() throws {
        let renderer = try ToneRendererImpl()
        let p = try payload(language: "en")
        let scientific = renderer.render(payload: p, tone: .SCIENTIFIC).text
        // SCIENTIFIC has an empty prefix — the pattern starts its line unadorned.
        #expect(scientific.contains("\n\(p.interpretation.pattern)"))
        let healing = renderer.render(payload: p, tone: .HEALING).text
        #expect(healing.contains("Take a breath. \(p.interpretation.pattern)"))
        #expect(healing.contains("A gentle reminder: \(p.blindspot)"))
        let roast = renderer.render(payload: p, tone: .ROAST_SAFE).text
        #expect(roast.contains("Straight talk: \(p.interpretation.pattern)"))
        #expect(roast.contains("The part you'd rather skip: \(p.blindspot)"))
    }

    @Test func renderingUsesPayloadLanguageLabels() throws {
        let renderer = try ToneRendererImpl()
        let zh = try payload(language: "zh-TW")
        let rendered = renderer.render(payload: zh, tone: .HEALING)
        #expect(rendered.text.hasPrefix("事業 — 72/100 · 穩定"))
        #expect(rendered.text.contains("今日行動："))
        #expect(rendered.text.contains("本週行動："))
        #expect(rendered.text.contains("反思提問："))
        #expect(rendered.text.contains("親愛的，"))
        #expect(rendered.text.contains("溫柔提醒："))
    }

    @Test func renderedTextIsPlainTextWithoutMarkup() throws {
        let renderer = try ToneRendererImpl()
        let p = try payload(language: "en")
        for tone in Tone.allCases {
            let text = renderer.render(payload: p, tone: tone).text
            #expect(!text.contains("<") && !text.contains(">"), "\(tone) output contains markup")
        }
    }
}
