import Foundation
import CoreContracts

/// Port of engine-scoring ScoringEngineImpl.kt — the arithmetic must stay
/// bit-for-bit equivalent so scores match across platforms (PRD §10, §18).
public final class ScoringEngineImpl: ScoringEngineProtocol {

    private let ruleset: Ruleset
    private let domains = Domains.all
    private let baseline = 50

    public init(ruleset: Ruleset) {
        self.ruleset = ruleset
    }

    /// Convenience initializer loading the bundled default ruleset.
    public convenience init() throws {
        self.init(ruleset: try Ruleset.loadDefault())
    }

    public func score(input: ScoringInput) -> ScoringResult {
        let palmSignals = SignalResolver.resolvePalmSignals(features: input.palmFeatures, ruleset: ruleset)
        let astroSignals = SignalResolver.resolveAstroSignals(astro: input.astroResult, ruleset: ruleset)

        let confMultiplier = ruleset.confidenceMultipliers[input.palmFeatures.confidence] ?? 0.5

        var domainScores: [String: Int] = [:]
        var explainability: [ExplainEntry] = []

        for domain in domains {
            var score = Double(baseline)

            for signal in palmSignals where meetsConfidence(required: signal.minConfidence, actual: input.palmFeatures.confidence) {
                let weight = signal.domainWeights[domain] ?? 0.0
                let contribution = Double(signal.direction) * Double(signal.magnitude) * weight * confMultiplier
                score += contribution
                if weight > 0.3 {
                    explainability.append(ExplainEntry(
                        signalId: signal.signalId,
                        mapping: "\(signal.signalId) → \(domain)",
                        contribution: contribution
                    ))
                }
            }

            for (signal, astro) in astroSignals {
                let astroConf = ruleset.confidenceMultipliers[astro.confidence] ?? 0.5
                let weight = signal.domainWeights[domain] ?? 0.0
                let contribution = Double(signal.direction) * Double(signal.magnitude) * weight * astroConf
                score += contribution
                if weight > 0.3 {
                    explainability.append(ExplainEntry(
                        signalId: signal.signalId,
                        mapping: "\(signal.signalId) → \(domain)",
                        contribution: contribution
                    ))
                }
            }

            domainScores[domain] = min(max(Int(score), 0), 100)
        }

        let overallScore = domainScores.isEmpty
            ? baseline
            : Int(Double(domainScores.values.reduce(0, +)) / Double(domainScores.count))
        let grade = ruleset.grade(forScore: overallScore)
        let confidence = minConfidence(input.palmFeatures.confidence, astroConfidence(input.astroResult))
        let reasons = buildConfidenceReasons(input: input)

        // Kotlin's sortedByDescending is stable; Swift's sort is not guaranteed
        // to be, so break abs-contribution ties by insertion order explicitly —
        // parity fixtures compare explainability entry order exactly.
        let sortedExplain = distinctByKey(explainability) { "\($0.signalId)-\($0.mapping)" }
            .enumerated()
            .sorted { a, b in
                let absA = abs(a.element.contribution)
                let absB = abs(b.element.contribution)
                return absA != absB ? absA > absB : a.offset < b.offset
            }
            .map(\.element)

        return ScoringResult(
            domainScores: domainScores,
            subdimScores: [:],
            grade: grade,
            confidence: confidence,
            confidenceReasons: reasons,
            explainability: sortedExplain,
            matchedBuckets: [],
            rulesetVersion: ruleset.version
        )
    }

    // MARK: - Helpers (mirroring the Kotlin implementation)

    private func meetsConfidence(required: String, actual: String) -> Bool {
        let order = ["low": 0, "med": 1, "high": 2]
        return (order[actual] ?? 0) >= (order[required] ?? 0)
    }

    private func astroConfidence(_ astro: AstroResult) -> String {
        astro.calcLevel == .L2 ? "high" : "med"
    }

    private func minConfidence(_ a: String, _ b: String) -> String {
        let order = ["low": 0, "med": 1, "high": 2]
        return (order[a] ?? 0) <= (order[b] ?? 0) ? a : b
    }

    private func buildConfidenceReasons(input: ScoringInput) -> [String] {
        var reasons: [String] = []
        if input.palmFeatures.confidence == "low" { reasons.append("scan_quality_low") }
        if input.palmFeatures.featureCoverage < 0.5 { reasons.append("low_feature_coverage") }
        if input.astroResult.calcLevel == .L1 { reasons.append("missing_birth_time") }
        return reasons
    }

    private func distinctByKey<T>(_ items: [T], key: (T) -> String) -> [T] {
        var seen = Set<String>()
        var result: [T] = []
        for item in items {
            let k = key(item)
            if seen.insert(k).inserted {
                result.append(item)
            }
        }
        return result
    }
}
