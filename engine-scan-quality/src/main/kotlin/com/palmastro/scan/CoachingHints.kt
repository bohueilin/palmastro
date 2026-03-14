package com.palmastro.scan

object CoachingHints {
    private val hints = mapOf(
        "blur" to "請保持手不動，等畫面穩定",
        "glare" to "請避免直射光源",
        "low_light" to "光線不足，請移到較亮的地方",
        "over_exposure" to "光線太強，請避開強光",
        "under_exposure" to "光線不足，請移到較亮的地方",
        "low_coverage" to "請將手掌完整放入框線內",
        "pose_unstable" to "請保持手掌穩定不動",
        "hand_not_detected" to "未偵測到手掌，請將手放入框線"
    )

    fun getHint(failReason: String): String =
        hints[failReason] ?: "請重新掃描"
}
