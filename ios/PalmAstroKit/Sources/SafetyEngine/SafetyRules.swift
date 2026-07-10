import Foundation
import CoreContracts

// Mirrors engine-content/src/main/kotlin/com/palmastro/content/SafetyRules.kt
// exactly. The JSON resource is the canonical cross-platform file (see
// Resources/SYNC.md) — the Android engine is the reference implementation and
// this schema must not drift from it.

/// One safety category (PRD §30): `zh` entries are matched as normalized
/// substrings (Traditional + Simplified variants both listed); `en` entries are
/// regex fragments compiled with ASCII word boundaries to avoid substring
/// false positives ("cure" in "secure", "you have" in "you haven't").
public struct SafetyCategory: Codable, Equatable, Sendable {
    public let id: String
    public let zh: [String]
    public let en: [String]

    public init(id: String, zh: [String] = [], en: [String] = []) {
        self.id = id
        self.zh = zh
        self.en = en
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(String.self, forKey: .id)
        zh = try c.decodeIfPresent([String].self, forKey: .zh) ?? []
        en = try c.decodeIfPresent([String].self, forKey: .en) ?? []
    }
}

/// Versioned safety ruleset loaded from the bundled `safety-rules.json`
/// (PRD §30-§32, §49).
public struct SafetyRules: Codable, Equatable, Sendable {
    public let version: String
    public let categories: [SafetyCategory]

    /// The nine canonical category ids (Kotlin `SafetyRules.CATEGORY_IDS`).
    public static let categoryIds = [
        "medical_diagnosis", "treatment", "disease_prediction",
        "investment_advice", "guaranteed_money", "fear_fate_claims",
        "self_harm", "profanity", "identity_attack",
    ]

    public init(version: String, categories: [SafetyCategory] = []) {
        self.version = version
        self.categories = categories
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        version = try c.decode(String.self, forKey: .version)
        categories = try c.decodeIfPresent([SafetyCategory].self, forKey: .categories) ?? []
    }

    public enum SafetyRulesError: Error, Equatable {
        case resourceMissing
    }

    /// Loads the bundled safety-rules.json (canonical copy lives in the
    /// Android engine-content resources; see Resources/SYNC.md).
    public static func loadDefault() throws -> SafetyRules {
        guard let url = Bundle.module.url(forResource: "safety-rules", withExtension: "json") else {
            throw SafetyRulesError.resourceMissing
        }
        return try fromJSON(Data(contentsOf: url))
    }

    public static func fromJSON(_ data: Data) throws -> SafetyRules {
        try JSONDecoder().decode(SafetyRules.self, from: data)
    }
}
