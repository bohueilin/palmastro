import Foundation
import Testing
import CoreContracts
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
        prompt: String = "What matters most right now?"
    ) -> SemanticPayload {
        SemanticPayload(
            domain: domain, monthKey: "2026-07", calcLevel: .L1, confidence: "med",
            observations: [],
            interpretation: Interpretation(pattern: pattern, trigger: trigger, cost: cost),
            blindspot: blindspot, actionToday: actionToday, actionWeek: actionWeek,
            prompt: prompt, safetyNotes: [], explainability: [],
            scoreCard: ScoreCard(totalScore: 50, grade: "Building", delta: nil, comparabilityScore: nil, subdims: [:])
        )
    }

    @Test func rulesLoadAndValidate() throws {
        let rules = try SafetyRules.loadDefault()
        #expect(rules.version == "2.0.0")
        #expect(rules.categories.count == 3)
        #expect(!rules.resolvedFallbackText(language: "en").isEmpty)
        #expect(rules.resolvedFallbackPayload(language: "zh-TW") != nil)
    }

    @Test func cleanContentPasses() throws {
        let filter = try SafetyFilterImpl()
        for domain in Domains.all {
            let result = filter.validate(payload: payload(domain: domain))
            #expect(result.passed, "\(domain): \(result.violations)")
        }
    }

    @Test func wealthProhibitedTermCaught() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "wealth", pattern: "This is a guaranteed return on your effort."))
        #expect(!result.passed)
        #expect(result.violations.contains { $0.hasPrefix("wealth_prohibited:") })
    }

    @Test func wealthTermsDoNotApplyToCareerDomain() throws {
        let filter = try SafetyFilterImpl()
        // wealth_prohibited is not cross-domain; the same phrase in career passes.
        let result = filter.validate(payload: payload(domain: "career", pattern: "This is a guaranteed return on your effort."))
        #expect(result.passed)
    }

    @Test func healthTermsAreCrossDomain() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "career", prompt: "Ask for a diagnosis of your team's issues."))
        #expect(!result.passed)
        #expect(result.violations.contains { $0.hasPrefix("health_prohibited:") })
    }

    @Test func identityAttackCaught() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "family", blindspot: "Honestly, you are hopeless at this."))
        #expect(!result.passed)
        #expect(result.violations.contains { $0.hasPrefix("identity_attack:") })
    }

    @Test func chineseTermsMatchAsSubstrings() throws {
        let filter = try SafetyFilterImpl()
        let result = filter.validate(payload: payload(domain: "wealth", pattern: "建議你考慮股票操作來提升收入。"))
        #expect(!result.passed)
        #expect(result.violations.contains("wealth_prohibited: 股票"))
    }

    @Test func englishWordBoundaries() throws {
        let filter = try SafetyFilterImpl()
        // "cure" is prohibited; "secure"/"curious" must NOT match.
        let secure = filter.validate(payload: payload(domain: "health", pattern: "Build a secure and curious daily routine."))
        #expect(secure.passed, "\(secure.violations)")

        let cure = filter.validate(payload: payload(domain: "health", pattern: "This will cure your fatigue."))
        #expect(!cure.passed)
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
        let filter = try SafetyFilterImpl()
        let rendered = RenderedReport(domain: "wealth", tone: .SCIENTIFIC, text: "Buy stock now for guaranteed profit!")
        let filtered = filter.filter(rendered: rendered)
        #expect(filtered.text != rendered.text)
        #expect(filtered.text == (try SafetyRules.loadDefault().resolvedFallbackText(language: "en")))
        #expect(filtered.domain == "wealth")
        #expect(filtered.tone == .SCIENTIFIC)
    }

    @Test func filterHonorsLanguageForReplacementCopy() throws {
        let filter = try SafetyFilterImpl()
        let rendered = RenderedReport(domain: "health", tone: .HEALING, text: "記得服用藥物。")
        let filtered = filter.filter(rendered: rendered, language: "zh-TW")
        #expect(filtered.text == (try SafetyRules.loadDefault().resolvedFallbackText(language: "zh-TW")))
    }

    @Test func filterKeepsCleanText() throws {
        let filter = try SafetyFilterImpl()
        let rendered = RenderedReport(domain: "career", tone: .ROAST_SAFE, text: "Focus on one hard thing this week.")
        #expect(filter.filter(rendered: rendered) == rendered)
    }

    // MARK: - Fallback payload

    @Test func safeFallbackPayloadIsCleanAndLocalized() throws {
        let filter = try SafetyFilterImpl()
        let card = ScoreCard(totalScore: 50, grade: "Building", delta: nil, comparabilityScore: nil, subdims: [:])
        for language in ["en", "zh-TW"] {
            let fallback = filter.safeFallbackPayload(
                domain: "health", monthKey: "2026-07", calcLevel: .L1, language: language, scoreCard: card
            )
            #expect(fallback.language == language)
            #expect(fallback.confidenceReasons == ["safety_fallback"])
            #expect(!fallback.interpretation.pattern.isEmpty)
            let check = filter.validate(payload: fallback)
            #expect(check.passed, "fallback payload must itself be safe: \(check.violations)")
        }
    }
}
