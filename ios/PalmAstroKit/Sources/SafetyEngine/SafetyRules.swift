import Foundation
import CoreContracts

/// Schema for safety-rules.json (PRD §30, §57). Terms are grouped in
/// categories; ASCII terms match on word boundaries, CJK terms match as
/// substrings. Localized fallback copy ships in the same file so the engine
/// never hardcodes display text.
public struct SafetyRules: Codable, Equatable, Sendable {

    public struct Category: Codable, Equatable, Sendable {
        /// Stable id used in violation strings, e.g. "wealth_prohibited".
        public let id: String
        /// Domains whose payloads this category applies to.
        public let appliesToDomains: [String]
        /// When true the category is enforced on every domain (e.g. health
        /// terms may not appear anywhere; identity attacks never allowed).
        public let crossDomain: Bool
        public let terms: [String]

        public init(id: String, appliesToDomains: [String], crossDomain: Bool, terms: [String]) {
            self.id = id
            self.appliesToDomains = appliesToDomains
            self.crossDomain = crossDomain
            self.terms = terms
        }
    }

    public struct FallbackPayloadText: Codable, Equatable, Sendable {
        public let pattern: String
        public let blindspot: String
        public let actionToday: String
        public let actionWeek: String
        public let prompt: String

        public init(pattern: String, blindspot: String, actionToday: String, actionWeek: String, prompt: String) {
            self.pattern = pattern
            self.blindspot = blindspot
            self.actionToday = actionToday
            self.actionWeek = actionWeek
            self.prompt = prompt
        }
    }

    public let version: String
    public let fallbackLanguage: String
    /// Replacement text for filtered rendered reports, per language.
    public let fallbackText: [String: String]
    /// Replacement payload used when validate() fails, per language.
    public let fallbackPayload: [String: FallbackPayloadText]
    public let categories: [Category]

    public init(
        version: String,
        fallbackLanguage: String,
        fallbackText: [String: String],
        fallbackPayload: [String: FallbackPayloadText],
        categories: [Category]
    ) {
        self.version = version
        self.fallbackLanguage = fallbackLanguage
        self.fallbackText = fallbackText
        self.fallbackPayload = fallbackPayload
        self.categories = categories
    }

    public enum SafetyRulesError: Error, Equatable {
        case resourceMissing
        case invalidRules(String)
    }

    /// Loads the bundled safety-rules.json (canonical copy lives in the
    /// Android engine-content resources; see Resources/SYNC.md).
    public static func loadDefault() throws -> SafetyRules {
        guard let url = Bundle.module.url(forResource: "safety-rules", withExtension: "json") else {
            throw SafetyRulesError.resourceMissing
        }
        let rules = try JSONDecoder().decode(SafetyRules.self, from: Data(contentsOf: url))
        try rules.validate()
        return rules
    }

    public func validate() throws {
        if version.isEmpty { throw SafetyRulesError.invalidRules("missing version") }
        if categories.isEmpty { throw SafetyRulesError.invalidRules("no categories") }
        if fallbackText[fallbackLanguage] == nil {
            throw SafetyRulesError.invalidRules("missing fallback text for \(fallbackLanguage)")
        }
        if fallbackPayload[fallbackLanguage] == nil {
            throw SafetyRulesError.invalidRules("missing fallback payload for \(fallbackLanguage)")
        }
        for category in categories where category.terms.isEmpty {
            throw SafetyRulesError.invalidRules("category \(category.id) has no terms")
        }
    }

    public func resolvedFallbackText(language: String) -> String {
        fallbackText[language] ?? fallbackText[fallbackLanguage] ?? ""
    }

    public func resolvedFallbackPayload(language: String) -> FallbackPayloadText? {
        fallbackPayload[language] ?? fallbackPayload[fallbackLanguage]
    }
}
