package com.palmastro.analytics

import com.palmastro.contracts.interfaces.AnalyticsEmitter

class AnalyticsEmitterImpl(
    private val sink: (String, Map<String, Any>) -> Unit = { _, _ -> }
) : AnalyticsEmitter {

    private val allowlist = setOf(
        "onboarding_start", "onboarding_step_view", "onboarding_submit", "onboarding_complete",
        "demo_start", "demo_step_view", "demo_complete", "demo_skip",
        "scan_start", "scan_angle_prompt_view", "scan_angle_quality_fail", "scan_angle_pass", "scan_complete",
        "inference_start", "inference_success", "inference_fail",
        "results_view", "domain_card_tap", "why_drawer_open", "delta_view",
        "paywall_view", "purchase_start", "purchase_success", "purchase_fail",
        "restore_start", "restore_success", "restore_fail",
        "settings_view", "tone_change", "reminders_change", "retention_toggle_change",
        "dominant_hand_change", "delete_all_data_click", "delete_all_data_confirm",
        "app_crash", "performance_sample"
    )

    private val denylistKeyPatterns = listOf(
        Regex("^palm_feature_.*"),
        Regex("^biometric_.*"),
        Regex("^embedding_.*"),
        Regex("^journal_text$"),
        Regex("^journal_entry$"),
        Regex("^reflection_text$"),
        Regex("^birthday_value$"),
        Regex("^birth_date$"),
        Regex("^dob$"),
        Regex("^birth_time_value$"),
        Regex("^birth_place_value$"),
    )

    private val denylistValuePatterns = listOf(
        Regex(".*/scan/.*"),
        Regex(".*/frames/.*"),
        Regex(".*/media/.*"),
    )

    override fun emit(eventName: String, props: Map<String, Any>) {
        if (eventName !in allowlist) return

        val filtered = props.filter { (key, value) ->
            !isDeniedKey(key) && !isDeniedValue(value)
        }

        sink(eventName, filtered)
    }

    private fun isDeniedKey(key: String): Boolean =
        denylistKeyPatterns.any { it.matches(key) }

    private fun isDeniedValue(value: Any): Boolean {
        if (value is String && denylistValuePatterns.any { it.matches(value) }) return true
        if (value is List<*> && value.size > 3 && value.all { it is Number }) return true
        return false
    }
}
