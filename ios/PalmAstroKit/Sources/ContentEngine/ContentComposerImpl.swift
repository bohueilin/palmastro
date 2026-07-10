import Foundation
import CoreContracts

// Mirrors engine-content/src/main/kotlin/com/palmastro/content/ContentComposerImpl.kt.

/// Deterministic template-driven composer (PRD §19, §50, Appendix B).
///
/// All copy comes from the injected `ContentTemplates` (default: the bundled
/// canonical `content-templates.json`, version `templatesVersion`).
/// `compose(input:)` is the only composition API; it honors
/// `ContentInput.language` and falls back to the template default language
/// ("en") for unsupported requests.
public final class ContentComposerImpl: ContentComposer {

    private static let defaultScore = 50
    private static let maxObservations = 3
    private static let domainPlaceholder = "{domain}"

    private let templates: ContentTemplates

    /// Persist this alongside results instead of a hardcoded literal.
    public var templatesVersion: String { templates.version }

    public init(templates: ContentTemplates) {
        self.templates = templates
    }

    /// Convenience initializer loading the bundled default templates.
    public convenience init() throws {
        self.init(templates: try ContentTemplates.loadDefault())
    }

    public func compose(input: ContentInput) -> [String: SemanticPayload] {
        let language = templates.resolveLanguage(input.language)
        var payloads: [String: SemanticPayload] = [:]
        for domain in Domains.all {
            payloads[domain] = composeDomain(input: input, domain: domain, language: language)
        }
        return payloads
    }

    /// Engine-provided safe payload the app substitutes when `validate()` fails
    /// (EXECUTION_SPEC safety pipeline). Pass the failing payload as `base` to
    /// preserve its scoreCard / monthKey / calcLevel metadata.
    public func safeFallbackPayload(
        domain: String,
        language: String,
        base: SemanticPayload? = nil
    ) -> SemanticPayload {
        let lang = templates.resolveLanguage(language)
        let f = templates.fallback
        return SemanticPayload(
            domain: domain,
            monthKey: base?.monthKey ?? "",
            calcLevel: base?.calcLevel ?? .L1,
            confidence: base?.confidence ?? "low",
            confidenceReasons: [],
            language: lang,
            observations: [],
            interpretation: Interpretation(
                pattern: templates.localized(f.interpretationPattern, language: lang),
                trigger: templates.localized(f.interpretationTrigger, language: lang),
                cost: templates.localized(f.interpretationCost, language: lang)
            ),
            blindspot: templates.localized(f.blindspot, language: lang),
            actionToday: templates.localized(f.actionToday, language: lang),
            actionWeek: templates.localized(f.actionWeek, language: lang),
            prompt: templates.localized(f.prompt, language: lang),
            safetyNotes: templates.domains[domain]
                .map { templates.localizedList($0.safetyNotes, language: lang) }
                ?? [],
            explainability: [],
            scoreCard: base?.scoreCard
                ?? ScoreCard(totalScore: 0, grade: "", delta: nil, comparabilityScore: nil, subdims: [:])
        )
    }

    // MARK: - Per-domain composition

    private func composeDomain(input: ContentInput, domain: String, language: String) -> SemanticPayload {
        let score = input.scoringResult.domainScores[domain] ?? Self.defaultScore
        let template = templates.domains[domain]
        let displayName = template
            .map { templates.localized($0.displayName, language: language) }
            .flatMap { $0.isBlank ? nil : $0 }
            ?? domain
        let domainExplain = input.scoringResult.explainability.filter { $0.mapping.contains(domain) }
        let delta = input.deltaResult?.domainDeltas[domain]

        func text(_ field: [String: LocalizedText]?) -> String {
            guard let field else { return "" }
            return templates.bucketText(field, score: score, language: language)
                .replacingOccurrences(of: Self.domainPlaceholder, with: displayName)
        }

        return SemanticPayload(
            domain: domain,
            monthKey: input.monthKey,
            calcLevel: input.calcLevel,
            confidence: input.scoringResult.confidence,
            confidenceReasons: input.scoringResult.confidenceReasons,
            language: language,
            observations: domainExplain.prefix(Self.maxObservations).map { observation(entry: $0, language: language) },
            interpretation: Interpretation(
                pattern: text(template?.interpretation.pattern),
                trigger: text(template?.interpretation.trigger),
                cost: text(template?.interpretation.cost)
            ),
            blindspot: text(template?.blindspot),
            actionToday: text(template?.actionToday),
            actionWeek: text(template?.actionWeek),
            prompt: text(template?.prompt),
            safetyNotes: template
                .map { templates.localizedList($0.safetyNotes, language: language) }
                ?? [],
            explainability: domainExplain,
            scoreCard: ScoreCard(
                totalScore: score,
                grade: input.scoringResult.grade,
                delta: delta,
                comparabilityScore: input.deltaResult?.comparabilityScore,
                subdims: input.scoringResult.subdimScores.filter { $0.key.hasPrefix("\(domain).") }
            )
        )
    }

    private func observation(entry: ExplainEntry, language: String) -> Observation {
        let template = templates.observations[entry.signalId]
        let displayName = template
            .map { templates.localized($0.displayName, language: language) }
            .flatMap { $0.isBlank ? nil : $0 }
            ?? humanize(entry.signalId)
        let evidence = template
            .map { templates.localized($0.evidenceSummary, language: language) }
            .flatMap { $0.isBlank ? nil : $0 }
            ?? templates.localized(templates.observationFallbackEvidence, language: language)
        return Observation(signalId: entry.signalId, displayName: displayName, evidenceSummary: evidence)
    }

    /// Stable-key transformation for unknown signal ids (no display copy in code).
    private func humanize(_ signalId: String) -> String {
        var id = Substring(signalId)
        if id.hasPrefix("PALM_") { id = id.dropFirst("PALM_".count) }
        if id.hasPrefix("ASTRO_") { id = id.dropFirst("ASTRO_".count) }
        return id.split(separator: "_")
            .map { part -> String in
                let lower = part.lowercased()
                return lower.prefix(1).uppercased() + lower.dropFirst()
            }
            .joined(separator: " ")
    }
}

extension String {
    /// Kotlin `isBlank()` parity: empty or whitespace-only.
    var isBlank: Bool { allSatisfy { $0.isWhitespace } }
}
