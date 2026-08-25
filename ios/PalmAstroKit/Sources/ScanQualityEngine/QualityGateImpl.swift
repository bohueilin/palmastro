import CoreContracts

/// Port of engine-scan-quality QualityGateImpl.kt.
/// Weights, threshold, tie-breaking and worst-component keys are identical so
/// the same frame stream gates identically on Android and iOS.
public final class QualityGateImpl: QualityGate {

    public let passThreshold: Int
    public let weights: [Float]

    public init(passThreshold: Int = 60, weights: [Float] = [0.2, 0.2, 0.2, 0.2, 0.2]) {
        precondition(weights.count == 5, "weights must have 5 components")
        self.passThreshold = passThreshold
        self.weights = weights
    }

    public func scoreFrame(blur: Float, glare: Float, exposure: Float, coverage: Float, stability: Float) -> QualityScores {
        let components: [Float] = [blur, glare, exposure, coverage, stability]
        let weighted = zip(components, weights).reduce(0.0) { acc, pair in
            acc + Double(pair.0) * Double(pair.1)
        }
        let composite = min(max(Int((weighted * 100).rounded()), 0), 100)
        return QualityScores(
            blur: blur, glare: glare, exposure: exposure,
            coverage: coverage, stability: stability, composite: composite
        )
    }

    /// Returns the index of the first maximal frame ordered by
    /// (composite, coverage, blur) — same tie-breaking as the Kotlin engine.
    public func selectBestFrame(frames: [QualityScores]) -> Int {
        precondition(!frames.isEmpty, "frames must not be empty")
        var bestIndex = 0
        for i in 1..<frames.count where compareFrames(frames[i], frames[bestIndex]) > 0 {
            bestIndex = i
        }
        return bestIndex
    }

    public func evaluateAngle(angle: Angle, bestScore: QualityScores) -> AngleGateResult {
        // Zero coverage means no hand in frame at all - always a failure and a
        // distinct coaching reason from a partially visible palm (low_coverage).
        if bestScore.coverage <= 0 {
            return AngleGateResult(angle: angle, passed: false, failReason: "hand_not_detected")
        }
        if bestScore.composite >= passThreshold {
            return AngleGateResult(angle: angle, passed: true, failReason: nil)
        }
        return AngleGateResult(angle: angle, passed: false, failReason: findWorstComponent(bestScore))
    }

    private func compareFrames(_ a: QualityScores, _ b: QualityScores) -> Int {
        if a.composite != b.composite { return a.composite > b.composite ? 1 : -1 }
        let aCoverage = Int(a.coverage * 1000), bCoverage = Int(b.coverage * 1000)
        if aCoverage != bCoverage { return aCoverage > bCoverage ? 1 : -1 }
        let aBlur = Int(a.blur * 1000), bBlur = Int(b.blur * 1000)
        if aBlur != bBlur { return aBlur > bBlur ? 1 : -1 }
        return 0
    }

    /// Stable failure-reason keys shared with the Android engine and used by
    /// the app layer to select localized coaching strings.
    private func findWorstComponent(_ scores: QualityScores) -> String {
        let components: [(String, Float)] = [
            ("blur", scores.blur),
            ("glare", scores.glare),
            ("low_light", scores.exposure),
            ("low_coverage", scores.coverage),
            ("pose_unstable", scores.stability),
        ]
        var worst = components[0]
        for candidate in components.dropFirst() where candidate.1 < worst.1 {
            worst = candidate
        }
        return worst.0
    }
}
