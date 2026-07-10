/// Maps quality-gate fail reasons to stable coaching keys (parity with the
/// Android engine-scan-quality CoachingHints). Engines never return display
/// English (launch rule): the SwiftUI layer resolves these keys through
/// Localizable.strings (en + zh-Hant).
public enum CoachingHints {

    public static let genericKey = "coach_generic"

    private static let keys: [String: String] = [
        "blur": "coach_blur",
        "glare": "coach_glare",
        "low_light": "coach_low_light",
        "over_exposure": "coach_over_exposure",
        "under_exposure": "coach_under_exposure",
        "low_coverage": "coach_low_coverage",
        "pose_unstable": "coach_pose_unstable",
        "hand_not_detected": "coach_hand_not_detected",
    ]

    /// Stable resource key for a fail reason; unknown reasons map to `genericKey`.
    public static func keyFor(failReason: String) -> String {
        keys[failReason] ?? genericKey
    }
}
