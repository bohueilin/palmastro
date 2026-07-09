package com.palmastro.scan

object CoachingHints {
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
