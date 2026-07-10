import Foundation
import CoreContracts

/// Schema for content-templates.json (versioned, localized, safety-tagged —
/// PRD §50). One canonical JSON serves both platforms; the loader tolerates
/// unknown keys so the file can grow without code changes.
public struct ContentTemplates: Codable, Equatable, Sendable {

    public struct Band: Codable, Equatable, Sendable {
        public let minScore: Int
        public let pattern: String
        public let trigger: String
        public let cost: String

        public init(minScore: Int, pattern: String, trigger: String, cost: String) {
            self.minScore = minScore
            self.pattern = pattern
            self.trigger = trigger
            self.cost = cost
        }
    }

    public struct DomainTemplate: Codable, Equatable, Sendable {
        public let displayName: String
        public let bands: [Band]
        public let blindspotHigh: String
        public let blindspotLow: String
        public let actionTodayHigh: String
        public let actionTodayLow: String
        public let actionWeekHigh: String
        public let actionWeekLow: String
        public let promptHigh: String
        public let promptLow: String
        public let safetyNotes: [String]

        public init(
            displayName: String,
            bands: [Band],
            blindspotHigh: String,
            blindspotLow: String,
            actionTodayHigh: String,
            actionTodayLow: String,
            actionWeekHigh: String,
            actionWeekLow: String,
            promptHigh: String,
            promptLow: String,
            safetyNotes: [String]
        ) {
            self.displayName = displayName
            self.bands = bands
            self.blindspotHigh = blindspotHigh
            self.blindspotLow = blindspotLow
            self.actionTodayHigh = actionTodayHigh
            self.actionTodayLow = actionTodayLow
            self.actionWeekHigh = actionWeekHigh
            self.actionWeekLow = actionWeekLow
            self.promptHigh = promptHigh
            self.promptLow = promptLow
            self.safetyNotes = safetyNotes
        }

        /// First band whose minScore is met, scanning highest-first.
        public func band(forScore score: Int) -> Band? {
            bands.sorted { $0.minScore > $1.minScore }
                .first { score >= $0.minScore }
        }
    }

    public struct LanguageBundle: Codable, Equatable, Sendable {
        public let labels: [String: String]
        public let tonePrefixes: [String: String]
        public let toneBlindspotLabels: [String: String]
        public let evidence: [String: String]
        public let signalNames: [String: String]
        public let domains: [String: DomainTemplate]

        public init(
            labels: [String: String],
            tonePrefixes: [String: String],
            toneBlindspotLabels: [String: String],
            evidence: [String: String],
            signalNames: [String: String],
            domains: [String: DomainTemplate]
        ) {
            self.labels = labels
            self.tonePrefixes = tonePrefixes
            self.toneBlindspotLabels = toneBlindspotLabels
            self.evidence = evidence
            self.signalNames = signalNames
            self.domains = domains
        }
    }

    public let version: String
    public let fallbackLanguage: String
    public let languages: [String: LanguageBundle]

    public init(version: String, fallbackLanguage: String, languages: [String: LanguageBundle]) {
        self.version = version
        self.fallbackLanguage = fallbackLanguage
        self.languages = languages
    }

    public enum TemplatesError: Error, Equatable {
        case resourceMissing
        case invalidTemplates(String)
    }

    /// Loads the bundled content-templates.json (canonical copy lives in the
    /// Android engine-content resources; see Resources/SYNC.md).
    public static func loadDefault() throws -> ContentTemplates {
        guard let url = Bundle.module.url(forResource: "content-templates", withExtension: "json") else {
            throw TemplatesError.resourceMissing
        }
        let templates = try JSONDecoder().decode(ContentTemplates.self, from: Data(contentsOf: url))
        try templates.validate()
        return templates
    }

    public func validate() throws {
        if version.isEmpty { throw TemplatesError.invalidTemplates("missing version") }
        guard let fallback = languages[fallbackLanguage] else {
            throw TemplatesError.invalidTemplates("fallback language \(fallbackLanguage) missing")
        }
        for domain in Domains.all where fallback.domains[domain] == nil {
            throw TemplatesError.invalidTemplates("fallback bundle missing domain \(domain)")
        }
        for (tag, bundle) in languages {
            for (domain, template) in bundle.domains where template.bands.isEmpty {
                throw TemplatesError.invalidTemplates("\(tag)/\(domain) has no bands")
            }
        }
    }

    /// Resolves the best language bundle for a BCP-47-ish tag: exact match,
    /// then primary-subtag match (e.g. "zh" -> "zh-TW"), then the fallback.
    public func resolveLanguage(_ requested: String) -> (tag: String, bundle: LanguageBundle) {
        if let exact = languages[requested] {
            return (requested, exact)
        }
        let primary = requested.split(separator: "-").first.map(String.init) ?? requested
        if !primary.isEmpty {
            let candidates = languages.keys
                .filter { $0 == primary || $0.hasPrefix("\(primary)-") }
                .sorted()
            if let tag = candidates.first, let bundle = languages[tag] {
                return (tag, bundle)
            }
        }
        // validate() guarantees the fallback bundle exists.
        return (fallbackLanguage, languages[fallbackLanguage]!)
    }
}
