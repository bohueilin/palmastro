package com.palmastro.scan

/**
 * Maps quality-gate fail reasons to stable coaching keys.
 *
 * The UI must localize via [keyFor]: each key corresponds to a string
 * resource (e.g. `coach_blur`) provided by the app in `values/` and
 * `values-zh-rTW/`. [getHint] keeps a reference English text map for engine
 * tests only - it is not a display string source.
 */
object CoachingHints {

    private const val GENERIC_KEY = "coach_generic"

    private val keys = mapOf(
        "blur" to "coach_blur",
        "glare" to "coach_glare",
        "low_light" to "coach_low_light",
        "over_exposure" to "coach_over_exposure",
        // Same coaching advice as low_light; the UI ships one string for both.
        "under_exposure" to "coach_low_light",
        "low_coverage" to "coach_low_coverage",
        "pose_unstable" to "coach_pose_unstable",
        "hand_not_detected" to "coach_hand_not_detected"
    )

    /** Stable resource key for a fail reason; unknown reasons map to [GENERIC_KEY]. */
    fun keyFor(failReason: String): String = keys[failReason] ?: GENERIC_KEY

    // Reference text (English) kept for engine tests.
    private val hints = mapOf(
        "blur" to "Hold still until the image stabilizes",
        "glare" to "Avoid direct light sources",
        "low_light" to "Not enough light — move somewhere brighter",
        "over_exposure" to "Too bright — move away from the light",
        "under_exposure" to "Not enough light — move somewhere brighter",
        "low_coverage" to "Place your entire palm within the guide",
        "pose_unstable" to "Keep your palm steady",
        "hand_not_detected" to "No palm detected — place your hand in the frame"
    )

    fun getHint(failReason: String): String =
        hints[failReason] ?: "Please try again"
}
