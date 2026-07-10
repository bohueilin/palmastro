import Foundation
import Testing
import CoreContracts
import ContentEngine
@testable import SafetyEngine

@Suite struct SafetyEngineTests {

    private func payload(
        domain: String,
        pattern: String = "A calm month for reflection.",
        trigger: String = "",
        cost: String = "",
        blindspot: String = "Watch your assumptions.",
        actionToday: String = "Take a short walk.",
        actionWeek: String = "Plan one focused hour.",
        prompt: String = "What matters most right now?",
        observations: [Observation] = [],
        safetyNotes: [String] = []
    ) -> SemanticPayload {
        SemanticPayload(
            domain: domain, monthKey: "2026-07", calcLevel: .L1, confidence: "med",
            observations: observations,
            interpretation: Interpretation(pattern: pattern, trigger: trigger, cost: cost),
            blindspot: blindspot, actionToday: actionToday, actionWeek: actionWeek,
            prompt: prompt, safetyNotes: safetyNotes, explainability: [],
            scoreCard: ScoreCard(totalScore: 50, grade: "Building", delta: nil, comparabilityScore: nil, subdims: [:])
        )
    }

    // MARK: - Rules resource

    @Test func rulesLoadFromVersionedResource() throws {
        let rules = try SafetyRules.loadDefault()
        #expect(rules.version == "2.0.0")
        #expect(rules.categories.count == 9)
        #expect(Set(rules.categories.map(\.id)) == Set(SafetyRules.categoryIds))
        for category in rules.categories {
            #expect(!category.zh.isEmpty, "\(category.id) zh list empty")
            #expect(!category.en.isEmpty, "\(category.id) en list empty")
        }
    }

    // MARK: - validate()

    @Test func cleanContentPasses() throws {
        let filter = try SafetyFilterImpl()
        for domain in Domains.all {
            let result = filter.validate(payload: payload(domain: domain))
            #expect(result.passed, "\(domain): \(result.violations)")
        }
    }

    @Test func guaranteedMoneyCaught() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "wealth", pattern: "This is a guaranteed return on your effort."))
        #expect(!result.passed)
        #expect(result.violations.contains { $0.hasPrefix("guaranteed_money: ") })
    }

    @Test func allCategoriesAreEnforcedOnEveryDomain() throws {
        // strict_safety: every category applies to every domain's payload.
        let filter = try SafetyFilterImpl()
        let money = filter.validate(payload: payload(domain: "career", pattern: "This is a guaranteed return on your effort."))
        #expect(!money.passed)
        #expect(money.violations.contains { $0.hasPrefix("guaranteed_money: ") })

        let medical = filter.validate(payload: payload(domain: "career", prompt: "Ask for a diagnosis of your team's issues."))
        #expect(!medical.passed)
        #expect(medical.violations.contains { $0.hasPrefix("medical_diagnosis: ") })

        let invest = filter.validate(payload: payload(domain: "health", pattern: "Rest more and buy stocks while you wait."))
        #expect(!invest.passed)
        #expect(invest.violations.contains { $0.hasPrefix("investment_advice: ") })
    }

    @Test func identityAttackCaught() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "family", blindspot: "Honestly, you are hopeless at this."))
        #expect(!result.passed)
        #expect(result.violations.contains { $0.hasPrefix("identity_attack: ") })
    }

    @Test func fearFateClaimsCaught() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "career", pattern: "Sadly, you are doomed to fail this year."))
        #expect(!result.passed)
        #expect(result.violations.contains { $0.hasPrefix("fear_fate_claims: ") })
    }

    @Test func selfHarmCaught() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "family", pattern: "Some people feel life is not worth living."))
        #expect(!result.passed)
        #expect(result.violations.contains { $0.hasPrefix("self_harm: ") })
    }

    @Test func profanityCaught() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "career", pattern: "This month was a shitty ride."))
        #expect(!result.passed)
        #expect(result.violations.contains { $0.hasPrefix("profanity: ") })
    }

    @Test func diseasePredictionCaught() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "health", pattern: "掌紋顯示你會生病。"))
        #expect(!result.passed)
        #expect(result.violations.contains { $0.hasPrefix("disease_prediction: ") })
    }

    @Test func chineseTermsMatchAsSubstrings() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "wealth", pattern: "建議你考慮股票操作來提升收入。"))
        #expect(!result.passed)
        // Violation format: "<category_id>: <term>".
        #expect(result.violations.contains("investment_advice: 股票"))
    }

    @Test func validateScansTriggerAndCostFields() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "career", trigger: "股票下跌時要買入。"))
        #expect(!result.passed)
        #expect(result.violations.contains { $0.contains("買入") })
    }

    @Test func validateScansObservationFields() throws {
        let filter = try SafetyFilterImpl()
        let observation = Observation(signalId: "PALM_X", displayName: "保證獲利的紋路", evidenceSummary: "這代表穩賺")
        let result = filter.validate(payload: payload(domain: "career", observations: [observation]))
        #expect(!result.passed)
        #expect(result.violations.contains { $0.hasPrefix("guaranteed_money: ") })
    }

    @Test func validateScansSafetyNotes() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "career", safetyNotes: ["記得服用藥物。"]))
        #expect(!result.passed)
        #expect(result.violations.contains { $0.hasPrefix("treatment: ") })
    }

    // MARK: - English word boundaries (no substring false positives)

    @Test func englishWordBoundaries() throws {
        let filter = try SafetyFilterImpl()
        // "cures?" is prohibited; "secure"/"curious"/"procured" must NOT match.
        let secure = filter.validate(payload: payload(domain: "health", pattern: "Build a secure and curious daily routine."))
        #expect(secure.passed, "\(secure.violations)")
        let procured = filter.validate(payload: payload(domain: "career", pattern: "Resources were procured for the team."))
        #expect(procured.passed, "\(procured.violations)")
        // "losers?" is prohibited; "closer" must NOT match.
        let closer = filter.validate(payload: payload(domain: "family", pattern: "You are growing closer to your family."))
        #expect(closer.passed, "\(closer.violations)")

        let cure = filter.validate(payload: payload(domain: "health", pattern: "This will cure your fatigue."))
        #expect(!cure.passed)
        #expect(cure.violations.contains { $0.hasPrefix("treatment: ") })
    }

    @Test func youHaventDoesNotTriggerYouHaveRules() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "health", pattern: "Schedule a wellness check if you haven't had one recently."))
        #expect(result.passed, "false positive: \(result.violations)")
    }

    @Test func defaultEnHealthActionWeekPassesValidate() throws {
        let templates = try ContentTemplates.loadDefault()
        let filter = SafetyFilterImpl(rules: try SafetyRules.loadDefault(), templates: templates)
        let health = try #require(templates.domains["health"])
        let actionWeek = templates.bucketText(health.actionWeek, score: 40, language: "en")
        #expect(!actionWeek.isEmpty)
        let result = filter.validate(payload: payload(domain: "health", actionWeek: actionWeek))
        #expect(result.passed, "default health actionWeek flagged: \(result.violations)")
    }

    @Test func englishTermsMatchInsideCJKText() throws {
        // ICU treats CJK ideographs as word characters, so plain \b would fail
        // at CJK↔Latin boundaries; the ASCII lookarounds must still match.
        let filter = try SafetyFilterImpl()
        let buyStocks = filter.validate(payload: payload(domain: "wealth", pattern: "你應該buy stocks現在。"))
        #expect(!buyStocks.passed, "buy stocks inside CJK text must be caught")
        #expect(buyStocks.violations.contains { $0.hasPrefix("investment_advice: ") })

        let damnYou = filter.validate(payload: payload(domain: "career", pattern: "damn you的態度。"))
        #expect(!damnYou.passed, "damn you inside CJK text must be caught")
        #expect(damnYou.violations.contains { $0.hasPrefix("profanity: ") })
    }

    // MARK: - Adversarial normalization

    @Test func zeroWidthInjectionDefeated() throws {
        let filter = try SafetyFilterImpl()
        let sneaky = "This is a guaran\u{200B}teed re\u{200C}turn."
        let result = filter.validate(payload: payload(domain: "wealth", pattern: sneaky))
        #expect(!result.passed)
    }

    @Test func fullwidthFoldingDefeated() throws {
        let filter = try SafetyFilterImpl()
        let fullwidth = "ｇｕａｒａｎｔｅｅｄ ｒｅｔｕｒｎ every month"
        let result = filter.validate(payload: payload(domain: "wealth", pattern: fullwidth))
        #expect(!result.passed)
    }

    @Test func fullwidthFoldingInsideCJKTextDefeated() throws {
        let filter = try SafetyFilterImpl()
        let buyStocks = filter.validate(payload: payload(domain: "wealth", pattern: "你應該ｂｕｙ ｓｔｏｃｋｓ現在。"))
        #expect(!buyStocks.passed, "fullwidth buy stocks inside CJK text must be caught")
        #expect(buyStocks.violations.contains { $0.hasPrefix("investment_advice: ") })

        let damnYou = filter.validate(payload: payload(domain: "career", pattern: "ｄａｍｎ ｙｏｕ的態度。"))
        #expect(!damnYou.passed, "fullwidth damn you inside CJK text must be caught")
        #expect(damnYou.violations.contains { $0.hasPrefix("profanity: ") })
    }

    @Test func caseInsensitiveMatching() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "wealth", pattern: "GUARANTEED RETURN for you"))
        #expect(!result.passed)
    }

    @Test func nfcNormalizationApplied() throws {
        let filter = try SafetyFilterImpl()
        // Decomposed "é" must not break scanning around it.
        let text = "Cafe\u{0301} visits will cure you."
        let result = filter.validate(payload: payload(domain: "health", pattern: text))
        #expect(!result.passed)
    }

    // MARK: - filter(rendered:)

    @Test func filterReplacesProhibitedRenderedText() throws {
        let templates = try ContentTemplates.loadDefault()
        let filter = SafetyFilterImpl(rules: try SafetyRules.loadDefault(), templates: templates)
        let rendered = RenderedReport(domain: "wealth", tone: .SCIENTIFIC, text: "Buy stocks now for a guaranteed profit!")
        let filtered = filter.filter(rendered: rendered)
        #expect(filtered.text != rendered.text)
        #expect(filtered.text == templates.localized(templates.fallback.filteredText, language: "en"))
        #expect(filtered.domain == "wealth")
        #expect(filtered.tone == .SCIENTIFIC)
        // The replacement copy must itself be safe.
        #expect(filter.filter(rendered: filtered).text == filtered.text)
    }

    @Test func filterHonorsLanguageForReplacementCopy() throws {
        let filter = try SafetyFilterImpl()
        let rendered = RenderedReport(domain: "health", tone: .HEALING, text: "記得服用藥物。")
        let zh = filter.filter(rendered: rendered, language: "zh-TW")
        let en = filter.filter(rendered: rendered, language: "en")
        #expect(zh.text != rendered.text)
        #expect(zh.text != en.text)
        #expect(zh.text.contains("內容準則"))
    }

    @Test func filterKeepsCleanText() throws {
        let filter = try SafetyFilterImpl()
        let rendered = RenderedReport(domain: "career", tone: .ROAST_SAFE, text: "Focus on one hard thing this week.")
        #expect(filter.filter(rendered: rendered) == rendered)
    }

    // MARK: - Composer integration (safe canonical copy + fallback payload)

    private func composedInput(language: String) -> ContentInput {
        ContentInput(
            scoringResult: ScoringResult(
                domainScores: ["career": 72, "wealth": 58, "family": 65, "health": 55],
                subdimScores: [:], grade: "Stable", confidence: "high",
                confidenceReasons: [], explainability: [], matchedBuckets: [], rulesetVersion: "2.0.0"
            ),
            deltaResult: nil, tone: .SCIENTIFIC, entitlements: [], calcLevel: .L2,
            monthKey: "2026-07", language: language
        )
    }

    @Test func composedCanonicalPayloadsPassValidation() throws {
        let composer = try ContentComposerImpl()
        let filter = try SafetyFilterImpl()
        for language in ["en", "zh-TW", "zh-CN", "ja", "hi"] {
            for (domain, payload) in composer.compose(input: composedInput(language: language)) {
                let check = filter.validate(payload: payload)
                #expect(check.passed, "\(domain) [\(language)] violated: \(check.violations)")
            }
        }
    }

    @Test func composedCanonicalReportsPassTheFilterUnchanged() throws {
        let composer = try ContentComposerImpl()
        let renderer = try ToneRendererImpl()
        let filter = try SafetyFilterImpl()
        for language in ["en", "zh-TW"] {
            let payloads = composer.compose(input: composedInput(language: language))
            for domain in Domains.all {
                for tone in Tone.allCases {
                    let rendered = renderer.render(payload: try #require(payloads[domain]), tone: tone)
                    #expect(filter.filter(rendered: rendered, language: language) == rendered,
                            "\(domain)/\(language)/\(tone) was filtered")
                }
            }
        }
    }

    @Test func safeFallbackPayloadIsCleanAndLocalized() throws {
        let templates = try ContentTemplates.loadDefault()
        let composer = ContentComposerImpl(templates: templates)
        let filter = SafetyFilterImpl(rules: try SafetyRules.loadDefault(), templates: templates)
        for language in templates.languages {
            let fallback = composer.safeFallbackPayload(domain: "wealth", language: language)
            #expect(fallback.domain == "wealth")
            #expect(fallback.language == language)
            #expect(!fallback.interpretation.pattern.isEmpty)
            #expect(!fallback.safetyNotes.isEmpty, "wealth fallback keeps safety notes")
            let check = filter.validate(payload: fallback)
            #expect(check.passed, "fallback [\(language)] violated: \(check.violations)")
        }
    }
}
