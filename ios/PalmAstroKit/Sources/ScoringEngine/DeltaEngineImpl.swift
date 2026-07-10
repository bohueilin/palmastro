import Foundation
import CoreContracts

/// Port of engine-scoring DeltaEngineImpl.kt (month-over-month deltas +
/// comparability bucketing).
public final class DeltaEngineImpl: DeltaEngine {

    public init() {}

    public func computeDelta(prev: MonthlyResult, current: MonthlyResult) -> DeltaResult {
        var domainDeltas: [String: DeltaValue] = [:]
        for domain in Domains.all {
            let prevScore = prev.scoringResult.domainScores[domain] ?? 50
            let currScore = current.scoringResult.domainScores[domain] ?? 50
            let diff = currScore - prevScore
            let arrow = diff > 0 ? "up" : (diff < 0 ? "down" : "flat")
            domainDeltas[domain] = DeltaValue(value: diff, arrow: arrow)
        }

        let gradeShift: GradeShift? = prev.scoringResult.grade != current.scoringResult.grade
            ? GradeShift(from: prev.scoringResult.grade, to: current.scoringResult.grade)
            : nil

        let comparabilityScore = computeComparability(prev: prev, current: current)
        let bucket: ComparabilityBucket
        switch comparabilityScore {
        case 70...: bucket = .HIGH
        case 50...: bucket = .MED
        default: bucket = .LOW
        }

        return DeltaResult(
            domainDeltas: domainDeltas,
            subdimDeltas: [:],
            gradeShift: gradeShift,
            comparabilityScore: comparabilityScore,
            comparabilityBucket: bucket,
            prevMonthKey: prev.monthKey,
            currentMonthKey: current.monthKey
        )
    }

    private func computeComparability(prev: MonthlyResult, current: MonthlyResult) -> Int {
        let qualityDiff = abs(current.scanQualityScore - prev.scanQualityScore)
        let qualityFactor = Int(max(0.0, 1.0 - Double(qualityDiff) / 50.0) * 100)

        let coverageDiff = abs(Double(current.featureCoverage - prev.featureCoverage))
        let coverageFactor = Int(max(0.0, 1.0 - coverageDiff / 0.3) * 100)

        let handMatchFactor = 100
        let calcLevelFactor = 100
        let timeGapFactor = 100

        let weighted = Double(qualityFactor) * 0.30 + Double(coverageFactor) * 0.25
            + Double(handMatchFactor) * 0.20 + Double(calcLevelFactor) * 0.10
            + Double(timeGapFactor) * 0.15
        return min(max(Int(weighted), 0), 100)
    }
}
