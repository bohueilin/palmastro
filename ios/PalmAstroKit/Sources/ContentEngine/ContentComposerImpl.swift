import Foundation
import CoreContracts

/// Template-driven content composer (PRD §19, Appendix B).
/// All display text comes from content-templates.json — nothing user-visible
/// is hardcoded here. `ContentInput.language` is honored with fallback to the
/// template file's fallback language.
public final class ContentComposerImpl: ContentComposer {

    /// Score at or above which the "high" variants are used (matches the
    /// Android composer's behaviour split).
    public static let highScoreThreshold = 65

    private let templates: ContentTemplates

    public init(templates: ContentTemplates) {
        self.templates = templates
    }

    /// Convenience initializer loading the bundled default templates.
    public convenience init() throws {
        self.init(templates: try ContentTemplates.loadDefault())
    }

    public func compose(input: ContentInput) -> [String: SemanticPayload] {
        let (languageTag, bundle) = templates.resolveLanguage(input.language)

        var payloads: [String: SemanticPayload] = [:]
        for domain in Domains.all {
            guard let template = bundle.domains[domain] else { continue }

            let score = input.scoringResult.domainScores[domain] ?? 50
            let grade = input.scoringResult.grade
            let high = score >= Self.highScoreThreshold
            let domainExplain = input.scoringResult.explainability.filter { $0.mapping.contains(domain) }
            let delta = input.deltaResult?.domainDeltas[domain]

            let observations = domainExplain.prefix(3).map { entry in
                Observation(
                    signalId: entry.signalId,
                    displayName: displayName(for: entry.signalId, bundle: bundle),
                    evidenceSummary: evidenceSummary(for: entry.contribution, bundle: bundle)
                )
            }

            let band = template.band(forScore: score)
            let interpretation = Interpretation(
                pattern: band?.pattern ?? "",
                trigger: band?.trigger ?? "",
                cost: band?.cost ?? ""
            )

            let subdims = input.scoringResult.subdimScores.filter { $0.key.hasPrefix("\(domain).") }

            payloads[domain] = SemanticPayload(
                domain: domain,
                monthKey: input.monthKey,
                calcLevel: input.calcLevel,
                confidence: input.scoringResult.confidence,
                confidenceReasons: input.scoringResult.confidenceReasons,
                language: languageTag,
                observations: Array(observations),
                interpretation: interpretation,
                blindspot: high ? template.blindspotHigh : template.blindspotLow,
                actionToday: high ? template.actionTodayHigh : template.actionTodayLow,
                actionWeek: high ? template.actionWeekHigh : template.actionWeekLow,
                prompt: high ? template.promptHigh : template.promptLow,
                safetyNotes: template.safetyNotes,
                explainability: domainExplain,
                scoreCard: ScoreCard(
                    totalScore: score,
                    grade: grade,
                    delta: delta,
                    comparabilityScore: input.deltaResult?.comparabilityScore,
                    subdims: subdims
                )
            )
        }
        return payloads
    }

    // MARK: - Observation helpers

    private func displayName(for signalId: String, bundle: ContentTemplates.LanguageBundle) -> String {
        if let named = bundle.signalNames[signalId] {
            return named
        }
        // Fallback: prettify the id the same way the Android composer does.
        return signalId
            .replacingOccurrences(of: "PALM_", with: "")
            .replacingOccurrences(of: "ASTRO_", with: "")
            .replacingOccurrences(of: "_", with: " ")
            .lowercased()
            .split(separator: " ")
            .map { $0.prefix(1).uppercased() + $0.dropFirst() }
            .joined(separator: " ")
    }

    /// Buckets a signed contribution into the localized evidence phrases
    /// (same thresholds as the Android composer: 3 strong / 1.5 moderate).
    private func evidenceSummary(for contribution: Double, bundle: ContentTemplates.LanguageBundle) -> String {
        let direction = contribution > 0 ? "positive" : "attention"
        let strength: String
        switch abs(contribution) {
        case let v where v > 3: strength = "strong"
        case let v where v > 1.5: strength = "moderate"
        default: strength = "subtle"
        }
        let key = "\(direction)_\(strength)"
        return bundle.evidence[key] ?? key
    }
}
