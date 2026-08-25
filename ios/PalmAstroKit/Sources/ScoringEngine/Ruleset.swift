import Foundation
import CoreContracts

/// Mirrors engine-scoring Ruleset.kt. The JSON schema is identical so one
/// canonical `default-ruleset.json` serves both platforms (PRD §49).
public struct GradeRange: Codable, Equatable, Sendable {
    public let min: Int
    public let max: Int

    public init(min: Int, max: Int) {
        self.min = min
        self.max = max
    }

    public func contains(_ value: Int) -> Bool {
        value >= min && value <= max
    }
}

public struct SignalDefinition: Codable, Equatable, Sendable {
    public let signalId: String
    public let source: String
    public let direction: Int
    public let magnitude: Int
    public let minConfidence: String
    public let domainWeights: [String: Double]
    public let safetyTag: String

    public init(
        signalId: String,
        source: String,
        direction: Int,
        magnitude: Int,
        minConfidence: String,
        domainWeights: [String: Double],
        safetyTag: String
    ) {
        self.signalId = signalId
        self.source = source
        self.direction = direction
        self.magnitude = magnitude
        self.minConfidence = minConfidence
        self.domainWeights = domainWeights
        self.safetyTag = safetyTag
    }
}

public struct Ruleset: Codable, Equatable, Sendable {
    public let version: String
    public let signals: [SignalDefinition]
    /// Insertion order matters for grade assignment; JSON objects don't
    /// guarantee order, so grade lookup uses the ranges themselves.
    public let gradeThresholds: [String: GradeRange]
    public let confidenceMultipliers: [String: Double]

    public init(
        version: String,
        signals: [SignalDefinition],
        gradeThresholds: [String: GradeRange],
        confidenceMultipliers: [String: Double]
    ) {
        self.version = version
        self.signals = signals
        self.gradeThresholds = gradeThresholds
        self.confidenceMultipliers = confidenceMultipliers
    }

    public enum RulesetError: Error, Equatable {
        case resourceMissing
        case invalidRuleset(String)
    }

    /// Loads the bundled default-ruleset.json (canonical copy lives in the
    /// Android engine-scoring resources; see Resources/SYNC.md).
    public static func loadDefault() throws -> Ruleset {
        guard let url = Bundle.module.url(forResource: "default-ruleset", withExtension: "json") else {
            throw RulesetError.resourceMissing
        }
        let data = try Data(contentsOf: url)
        let ruleset = try JSONDecoder().decode(Ruleset.self, from: data)
        try ruleset.validate()
        return ruleset
    }

    public static func fromJson(_ data: Data) throws -> Ruleset {
        let ruleset = try JSONDecoder().decode(Ruleset.self, from: data)
        try ruleset.validate()
        return ruleset
    }

    /// Startup validation per PRD §49 (rulesets must be versioned, validated).
    /// Same invariants and the same stable machine-readable keys as
    /// `Ruleset.validateOrThrow()` on Android, so a ruleset either loads on
    /// both platforms or fails on both with a comparable message:
    /// - version present, signal list non-empty, no duplicate signal IDs;
    /// - every signal weights all four domains, weights in 0...1,
    ///   direction is +1/-1, magnitude >= 1;
    /// - every domain has at least one reachable positive AND one reachable
    ///   negative contribution;
    /// - confidence multipliers cover high/med/low and are in 0...1;
    /// - grade thresholds cover 0...100 gap-free with no overlap.
    public func validate() throws {
        try require(!version.isBlank, "ruleset_version_blank")
        try require(!signals.isEmpty, "ruleset_signals_empty")

        let ids = signals.map(\.signalId)
        try require(ids.count == Set(ids).count, "ruleset_duplicate_signal_ids")

        let requiredDomains = Set(Domains.all)
        for signal in signals {
            try require(
                requiredDomains.isSubset(of: Set(signal.domainWeights.keys)),
                "ruleset_signal_missing_domain:\(signal.signalId)"
            )
            for weight in signal.domainWeights.values {
                try require(
                    weight >= 0.0 && weight <= 1.0,
                    "ruleset_domain_weight_out_of_range:\(signal.signalId)"
                )
            }
            try require(
                signal.direction == 1 || signal.direction == -1,
                "ruleset_invalid_direction:\(signal.signalId)"
            )
            try require(signal.magnitude >= 1, "ruleset_invalid_magnitude:\(signal.signalId)")
        }

        for domain in Domains.all {
            try require(
                signals.contains { $0.direction > 0 && ($0.domainWeights[domain] ?? 0.0) > 0.0 },
                "ruleset_domain_missing_positive_signal:\(domain)"
            )
            try require(
                signals.contains { $0.direction < 0 && ($0.domainWeights[domain] ?? 0.0) > 0.0 },
                "ruleset_domain_missing_negative_signal:\(domain)"
            )
        }

        try require(
            Set(["high", "med", "low"]).isSubset(of: Set(confidenceMultipliers.keys)),
            "ruleset_missing_confidence_levels"
        )
        for multiplier in confidenceMultipliers.values {
            try require(
                multiplier >= 0.0 && multiplier <= 1.0,
                "ruleset_confidence_multiplier_out_of_range"
            )
        }

        try require(!gradeThresholds.isEmpty, "ruleset_grade_thresholds_empty")
        let ranges = gradeThresholds.values.sorted { $0.min < $1.min }
        for range in ranges {
            try require(range.min <= range.max, "ruleset_grade_range_inverted")
        }
        try require(ranges[0].min == 0, "ruleset_grades_must_start_at_0")
        try require(ranges[ranges.count - 1].max == 100, "ruleset_grades_must_end_at_100")
        for i in 0..<(ranges.count - 1) {
            try require(
                ranges[i].max + 1 == ranges[i + 1].min,
                "ruleset_grade_thresholds_not_contiguous"
            )
        }
    }

    private func require(_ condition: Bool, _ key: String) throws {
        if !condition { throw RulesetError.invalidRuleset(key) }
    }

    public func grade(forScore score: Int) -> String {
        // Validation guarantees the bands are disjoint, so this unordered walk
        // finds exactly one match — the same grade Kotlin's ordered walk finds.
        for (grade, range) in gradeThresholds where range.contains(score) {
            return grade
        }
        // Out-of-range scores clamp to the nearest band edge.
        let sorted = gradeThresholds.sorted { $0.value.min < $1.value.min }
        if let first = sorted.first, score < first.value.min { return first.key }
        return sorted.last?.key ?? ""
    }
}

private extension String {
    /// Kotlin `isBlank()` parity: empty or whitespace-only.
    var isBlank: Bool { allSatisfy { $0.isWhitespace } }
}
