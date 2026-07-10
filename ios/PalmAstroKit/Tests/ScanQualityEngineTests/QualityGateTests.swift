import Testing
import CoreContracts
@testable import ScanQualityEngine

@Suite struct QualityGateTests {

    private let gate = QualityGateImpl()

    @Test func compositeIsWeightedAverageTimesHundred() {
        let scores = gate.scoreFrame(blur: 0.8, glare: 0.8, exposure: 0.8, coverage: 0.8, stability: 0.8)
        #expect(scores.composite == 80)
    }

    @Test func compositeRoundsAndClamps() {
        let mixed = gate.scoreFrame(blur: 0.61, glare: 0.62, exposure: 0.63, coverage: 0.64, stability: 0.65)
        #expect(mixed.composite == 63) // 0.63 * 100

        let zero = gate.scoreFrame(blur: 0, glare: 0, exposure: 0, coverage: 0, stability: 0)
        #expect(zero.composite == 0)

        let full = gate.scoreFrame(blur: 1, glare: 1, exposure: 1, coverage: 1, stability: 1)
        #expect(full.composite == 100)
    }

    @Test func selectBestFramePicksHighestComposite() {
        let frames = [
            gate.scoreFrame(blur: 0.5, glare: 0.5, exposure: 0.5, coverage: 0.5, stability: 0.5),
            gate.scoreFrame(blur: 0.9, glare: 0.9, exposure: 0.9, coverage: 0.9, stability: 0.9),
            gate.scoreFrame(blur: 0.7, glare: 0.7, exposure: 0.7, coverage: 0.7, stability: 0.7),
        ]
        #expect(gate.selectBestFrame(frames: frames) == 1)
    }

    @Test func selectBestFrameBreaksTiesByCoverageThenBlur() {
        // Same composite; second frame has higher coverage.
        let a = QualityScores(blur: 0.8, glare: 0.8, exposure: 0.8, coverage: 0.7, stability: 0.9, composite: 80)
        let b = QualityScores(blur: 0.7, glare: 0.8, exposure: 0.8, coverage: 0.8, stability: 0.9, composite: 80)
        #expect(gate.selectBestFrame(frames: [a, b]) == 1)

        // Same composite and coverage; second frame has higher blur score.
        let c = QualityScores(blur: 0.7, glare: 0.9, exposure: 0.8, coverage: 0.8, stability: 0.8, composite: 80)
        let d = QualityScores(blur: 0.9, glare: 0.7, exposure: 0.8, coverage: 0.8, stability: 0.8, composite: 80)
        #expect(gate.selectBestFrame(frames: [c, d]) == 1)

        // Full tie keeps the first frame (Kotlin maxWithOrNull semantics).
        #expect(gate.selectBestFrame(frames: [a, a]) == 0)
    }

    @Test func evaluateAnglePassesAtThreshold() {
        let passing = gate.scoreFrame(blur: 0.6, glare: 0.6, exposure: 0.6, coverage: 0.6, stability: 0.6)
        let result = gate.evaluateAngle(angle: .FRONT, bestScore: passing)
        #expect(result.passed)
        #expect(result.failReason == nil)
    }

    @Test func evaluateAngleFailsWithWorstComponentKey() {
        let failing = gate.scoreFrame(blur: 0.9, glare: 0.2, exposure: 0.5, coverage: 0.5, stability: 0.5)
        let result = gate.evaluateAngle(angle: .NEAR, bestScore: failing)
        #expect(!result.passed)
        #expect(result.failReason == "glare")
    }

    @Test func worstComponentUsesStableReasonKeys() {
        let lowLight = gate.scoreFrame(blur: 0.5, glare: 0.5, exposure: 0.1, coverage: 0.5, stability: 0.5)
        #expect(gate.evaluateAngle(angle: .FAR, bestScore: lowLight).failReason == "low_light")

        let lowCoverage = gate.scoreFrame(blur: 0.5, glare: 0.5, exposure: 0.5, coverage: 0.1, stability: 0.5)
        #expect(gate.evaluateAngle(angle: .FAR, bestScore: lowCoverage).failReason == "low_coverage")

        let unstable = gate.scoreFrame(blur: 0.5, glare: 0.5, exposure: 0.5, coverage: 0.5, stability: 0.1)
        #expect(gate.evaluateAngle(angle: .FAR, bestScore: unstable).failReason == "pose_unstable")
    }

    @Test func coachingHintsReturnStableKeysNotDisplayText() {
        #expect(CoachingHints.keyFor(failReason: "blur") == "coach_blur")
        #expect(CoachingHints.keyFor(failReason: "low_light") == "coach_low_light")
        #expect(CoachingHints.keyFor(failReason: "pose_unstable") == "coach_pose_unstable")
        #expect(CoachingHints.keyFor(failReason: "hand_not_detected") == "coach_hand_not_detected")
        #expect(CoachingHints.keyFor(failReason: "nonsense") == "coach_generic")
    }
}
