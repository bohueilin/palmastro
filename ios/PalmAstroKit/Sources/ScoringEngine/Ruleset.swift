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
    public func validate() throws {
        if version.isEmpty {
            throw RulesetError.invalidRuleset("missing version")
        }
        if signals.isEmpty {
            throw RulesetError.invalidRuleset("no signals")
        }
        if gradeThresholds.isEmpty {
            throw RulesetError.invalidRuleset("no grade thresholds")
        }
        for signal in signals where signal.domainWeights.isEmpty {
            throw RulesetError.invalidRuleset("signal \(signal.signalId) has no domain weights")
        }
    }

    public func grade(forScore score: Int) -> String {
        for (grade, range) in gradeThresholds where range.contains(score) {
            return grade
        }
        // Out-of-range scores clamp to the nearest band edge.
        let sorted = gradeThresholds.sorted { $0.value.min < $1.value.min }
        if let first = sorted.first, score < first.value.min { return first.key }
        return sorted.last?.key ?? ""
    }
}
