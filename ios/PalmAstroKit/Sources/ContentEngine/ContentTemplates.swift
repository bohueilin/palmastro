import Foundation
import CoreContracts

// Mirrors engine-content/src/main/kotlin/com/palmastro/content/ContentTemplates.kt
// exactly. The JSON resource is the canonical cross-platform file (see
// Resources/SYNC.md) — the Android engine is the reference implementation and
// this schema must not drift from it.

/// Text keyed by BCP-47-ish language tag ("en", "zh-TW", "zh-CN", "ja", "hi").
public typealias LocalizedText = [String: String]

/// List of texts keyed by language tag.
public typealias LocalizedList = [String: [String]]

/// Inclusive score range a bucket id maps to (PRD §19, §50).
public struct ScoreBucket: Codable, Equatable, Sendable {
    /// Kotlin default: 0. Missing key decodes to 0.
    public let min: Int
    /// Kotlin default: 100. Missing key decodes to 100.
    public let max: Int

    public init(min: Int = 0, max: Int = 100) {
        self.min = min
        self.max = max
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        min = try c.decodeIfPresent(Int.self, forKey: .min) ?? 0
        max = try c.decodeIfPresent(Int.self, forKey: .max) ?? 100
    }

    public func contains(_ score: Int) -> Bool { score >= min && score <= max }
}

/// PRD Appendix B: interpretation = { pattern, trigger, cost }, each score-bucketed.
public struct InterpretationTemplate: Codable, Equatable, Sendable {
    public let pattern: [String: LocalizedText]
    public let trigger: [String: LocalizedText]
    public let cost: [String: LocalizedText]

    public init(
        pattern: [String: LocalizedText] = [:],
        trigger: [String: LocalizedText] = [:],
        cost: [String: LocalizedText] = [:]
    ) {
        self.pattern = pattern
        self.trigger = trigger
        self.cost = cost
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        pattern = try c.decodeIfPresent([String: LocalizedText].self, forKey: .pattern) ?? [:]
        trigger = try c.decodeIfPresent([String: LocalizedText].self, forKey: .trigger) ?? [:]
        cost = try c.decodeIfPresent([String: LocalizedText].self, forKey: .cost) ?? [:]
    }
}

/// Per-domain template library entry. Field maps are keyed by bucket id.
public struct DomainTemplate: Codable, Equatable, Sendable {
    public let displayName: LocalizedText
    public let interpretation: InterpretationTemplate
    public let blindspot: [String: LocalizedText]
    public let actionToday: [String: LocalizedText]
    public let actionWeek: [String: LocalizedText]
    public let prompt: [String: LocalizedText]
    public let safetyNotes: LocalizedList

    public init(
        displayName: LocalizedText = [:],
        interpretation: InterpretationTemplate = InterpretationTemplate(),
        blindspot: [String: LocalizedText] = [:],
        actionToday: [String: LocalizedText] = [:],
        actionWeek: [String: LocalizedText] = [:],
        prompt: [String: LocalizedText] = [:],
        safetyNotes: LocalizedList = [:]
    ) {
        self.displayName = displayName
        self.interpretation = interpretation
        self.blindspot = blindspot
        self.actionToday = actionToday
        self.actionWeek = actionWeek
        self.prompt = prompt
        self.safetyNotes = safetyNotes
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        displayName = try c.decodeIfPresent(LocalizedText.self, forKey: .displayName) ?? [:]
        interpretation = try c.decodeIfPresent(InterpretationTemplate.self, forKey: .interpretation)
            ?? InterpretationTemplate()
        blindspot = try c.decodeIfPresent([String: LocalizedText].self, forKey: .blindspot) ?? [:]
        actionToday = try c.decodeIfPresent([String: LocalizedText].self, forKey: .actionToday) ?? [:]
        actionWeek = try c.decodeIfPresent([String: LocalizedText].self, forKey: .actionWeek) ?? [:]
        prompt = try c.decodeIfPresent([String: LocalizedText].self, forKey: .prompt) ?? [:]
        safetyNotes = try c.decodeIfPresent(LocalizedList.self, forKey: .safetyNotes) ?? [:]
    }
}

/// Display copy for a known signal id (PRD Appendix A).
public struct ObservationTemplate: Codable, Equatable, Sendable {
    public let displayName: LocalizedText
    public let evidenceSummary: LocalizedText

    public init(displayName: LocalizedText = [:], evidenceSummary: LocalizedText = [:]) {
        self.displayName = displayName
        self.evidenceSummary = evidenceSummary
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        displayName = try c.decodeIfPresent(LocalizedText.self, forKey: .displayName) ?? [:]
        evidenceSummary = try c.decodeIfPresent(LocalizedText.self, forKey: .evidenceSummary) ?? [:]
    }
}

/// Safe replacement copy used when generated content fails validation (PRD §30).
public struct FallbackTemplate: Codable, Equatable, Sendable {
    public let interpretationPattern: LocalizedText
    public let interpretationTrigger: LocalizedText
    public let interpretationCost: LocalizedText
    public let blindspot: LocalizedText
    public let actionToday: LocalizedText
    public let actionWeek: LocalizedText
    public let prompt: LocalizedText
    public let filteredText: LocalizedText

    public init(
        interpretationPattern: LocalizedText = [:],
        interpretationTrigger: LocalizedText = [:],
        interpretationCost: LocalizedText = [:],
        blindspot: LocalizedText = [:],
        actionToday: LocalizedText = [:],
        actionWeek: LocalizedText = [:],
        prompt: LocalizedText = [:],
        filteredText: LocalizedText = [:]
    ) {
        self.interpretationPattern = interpretationPattern
        self.interpretationTrigger = interpretationTrigger
        self.interpretationCost = interpretationCost
        self.blindspot = blindspot
        self.actionToday = actionToday
        self.actionWeek = actionWeek
        self.prompt = prompt
        self.filteredText = filteredText
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        interpretationPattern = try c.decodeIfPresent(LocalizedText.self, forKey: .interpretationPattern) ?? [:]
        interpretationTrigger = try c.decodeIfPresent(LocalizedText.self, forKey: .interpretationTrigger) ?? [:]
        interpretationCost = try c.decodeIfPresent(LocalizedText.self, forKey: .interpretationCost) ?? [:]
        blindspot = try c.decodeIfPresent(LocalizedText.self, forKey: .blindspot) ?? [:]
        actionToday = try c.decodeIfPresent(LocalizedText.self, forKey: .actionToday) ?? [:]
        actionWeek = try c.decodeIfPresent(LocalizedText.self, forKey: .actionWeek) ?? [:]
        prompt = try c.decodeIfPresent(LocalizedText.self, forKey: .prompt) ?? [:]
        filteredText = try c.decodeIfPresent(LocalizedText.self, forKey: .filteredText) ?? [:]
    }
}

/// Localized prefixes/labels for one tone (PRD §45). Keyed by `Tone` raw value.
public struct ToneTemplate: Codable, Equatable, Sendable {
    public let interpretationPrefix: LocalizedText
    public let blindspotLabel: LocalizedText

    public init(interpretationPrefix: LocalizedText = [:], blindspotLabel: LocalizedText = [:]) {
        self.interpretationPrefix = interpretationPrefix
        self.blindspotLabel = blindspotLabel
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        interpretationPrefix = try c.decodeIfPresent(LocalizedText.self, forKey: .interpretationPrefix) ?? [:]
        blindspotLabel = try c.decodeIfPresent(LocalizedText.self, forKey: .blindspotLabel) ?? [:]
    }
}

/// Localized section labels used by the renderer for plain-text reports.
public struct ReportLabels: Codable, Equatable, Sendable {
    public let actionToday: LocalizedText
    public let actionWeek: LocalizedText
    public let prompt: LocalizedText
    public let safetyNote: LocalizedText
    public let grades: [String: LocalizedText]

    public init(
        actionToday: LocalizedText = [:],
        actionWeek: LocalizedText = [:],
        prompt: LocalizedText = [:],
        safetyNote: LocalizedText = [:],
        grades: [String: LocalizedText] = [:]
    ) {
        self.actionToday = actionToday
        self.actionWeek = actionWeek
        self.prompt = prompt
        self.safetyNote = safetyNote
        self.grades = grades
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        actionToday = try c.decodeIfPresent(LocalizedText.self, forKey: .actionToday) ?? [:]
        actionWeek = try c.decodeIfPresent(LocalizedText.self, forKey: .actionWeek) ?? [:]
        prompt = try c.decodeIfPresent(LocalizedText.self, forKey: .prompt) ?? [:]
        safetyNote = try c.decodeIfPresent(LocalizedText.self, forKey: .safetyNote) ?? [:]
        grades = try c.decodeIfPresent([String: LocalizedText].self, forKey: .grades) ?? [:]
    }
}

/// Versioned, localized content template library (PRD §19, §50).
/// Loaded from the bundled `content-templates.json`; all composed copy comes
/// from here.
public struct ContentTemplates: Codable, Equatable, Sendable {
    public let version: String
    public let defaultLanguage: String
    public let languages: [String]
    public let buckets: [String: ScoreBucket]
    public let domains: [String: DomainTemplate]
    public let observations: [String: ObservationTemplate]
    public let observationFallbackEvidence: LocalizedText
    public let fallback: FallbackTemplate
    public let tones: [String: ToneTemplate]
    public let labels: ReportLabels

    public init(
        version: String,
        defaultLanguage: String = "en",
        languages: [String] = ["en"],
        buckets: [String: ScoreBucket] = [:],
        domains: [String: DomainTemplate] = [:],
        observations: [String: ObservationTemplate] = [:],
        observationFallbackEvidence: LocalizedText = [:],
        fallback: FallbackTemplate = FallbackTemplate(),
        tones: [String: ToneTemplate] = [:],
        labels: ReportLabels = ReportLabels()
    ) {
        self.version = version
        self.defaultLanguage = defaultLanguage
        self.languages = languages
        self.buckets = buckets
        self.domains = domains
        self.observations = observations
        self.observationFallbackEvidence = observationFallbackEvidence
        self.fallback = fallback
        self.tones = tones
        self.labels = labels
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        version = try c.decode(String.self, forKey: .version)
        defaultLanguage = try c.decodeIfPresent(String.self, forKey: .defaultLanguage) ?? "en"
        languages = try c.decodeIfPresent([String].self, forKey: .languages) ?? ["en"]
        buckets = try c.decodeIfPresent([String: ScoreBucket].self, forKey: .buckets) ?? [:]
        domains = try c.decodeIfPresent([String: DomainTemplate].self, forKey: .domains) ?? [:]
        observations = try c.decodeIfPresent([String: ObservationTemplate].self, forKey: .observations) ?? [:]
        observationFallbackEvidence = try c.decodeIfPresent(LocalizedText.self, forKey: .observationFallbackEvidence) ?? [:]
        fallback = try c.decodeIfPresent(FallbackTemplate.self, forKey: .fallback) ?? FallbackTemplate()
        tones = try c.decodeIfPresent([String: ToneTemplate].self, forKey: .tones) ?? [:]
        labels = try c.decodeIfPresent(ReportLabels.self, forKey: .labels) ?? ReportLabels()
    }

    // MARK: - Lookup helpers (Kotlin parity)

    /// Supported language or the default ("en") — composer honors ContentInput.language.
    public func resolveLanguage(_ requested: String) -> String {
        languages.contains(requested) ? requested : defaultLanguage
    }

    /// Localized text with fallback to the default language.
    public func localized(_ text: LocalizedText, language: String) -> String {
        text[language] ?? text[defaultLanguage] ?? ""
    }

    /// Localized list with fallback to the default language.
    public func localizedList(_ lists: LocalizedList, language: String) -> [String] {
        lists[language] ?? lists[defaultLanguage] ?? []
    }

    /// Resolves a bucketed field: the entry whose bucket range contains `score`
    /// wins. Deterministic; returns "" when nothing matches.
    ///
    /// Kotlin scans entries in JSON order; Swift dictionaries are unordered, so
    /// candidate keys are scanned in sorted order. Bucket ranges within one
    /// field are disjoint in the canonical data, so at most one entry matches
    /// and both implementations agree.
    public func bucketText(_ field: [String: LocalizedText], score: Int, language: String) -> String {
        let match = field.keys.sorted().first { bucketId in
            buckets[bucketId]?.contains(score) == true
        }
        guard let bucketId = match, let text = field[bucketId] else { return "" }
        return localized(text, language: language)
    }

    // MARK: - Loading

    public enum TemplatesError: Error, Equatable {
        case resourceMissing
    }

    /// Loads the bundled content-templates.json (canonical copy lives in the
    /// Android engine-content resources; see Resources/SYNC.md).
    public static func loadDefault() throws -> ContentTemplates {
        guard let url = Bundle.module.url(forResource: "content-templates", withExtension: "json") else {
            throw TemplatesError.resourceMissing
        }
        return try fromJSON(Data(contentsOf: url))
    }

    public static func fromJSON(_ data: Data) throws -> ContentTemplates {
        try JSONDecoder().decode(ContentTemplates.self, from: data)
    }
}
