package com.palmastro.analytics

import com.palmastro.contracts.interfaces.AnalyticsEmitter

/**
 * Privacy-safe analytics wrapper (PRD §29, §51, Appendix C).
 * Enforcement order: event-name allowlist → property-key allowlist → denylist scan →
 * type validation. Free-form text can never pass: string values must match an
 * enumerated-token shape and are capped in length.
 */
class AnalyticsEmitterImpl(
    private val sink: (String, Map<String, Any>) -> Unit = { _, _ -> }
) : AnalyticsEmitter {

    private val allowlist = setOf(
        "onboarding_start", "onboarding_step_view", "onboarding_submit", "onboarding_complete",
        "demo_start", "demo_step_view", "demo_complete", "demo_skip",
        "scan_start", "scan_angle_prompt_view", "scan_angle_quality_fail", "scan_angle_pass", "scan_complete",
        "inference_start", "inference_success", "inference_fail",
        "results_view", "domain_card_tap", "why_drawer_open", "delta_view",
        "journal_saved",
        "paywall_view", "purchase_start", "purchase_success", "purchase_fail",
        "restore_start", "restore_success", "restore_fail",
        "settings_view", "tone_change", "reminders_change", "retention_toggle_change",
        "language_change",
        "dominant_hand_change", "delete_all_data_click", "delete_all_data_confirm",
        "app_crash", "performance_sample"
    )

    /** PRD §51: allowlist properties. Only these keys may ever leave the wrapper. */
    private val propertyAllowlist = setOf(
        "step", "angle", "reason", "domain", "tone", "language", "frequency",
        "enabled", "calc_level", "confidence", "duration_ms", "attempt",
        "quality_bucket", "product_id", "error_code", "screen", "metric", "value"
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
        Regex("^purchase_token$"),
        Regex("^receipt.*"),
    )

    private val denylistValuePatterns = listOf(
        Regex(".*/scan/.*"),
        Regex(".*/frames/.*"),
        Regex(".*/media/.*"),
    )

    /**
     * PRD §51 "No free text": string values must look like enumerated tokens
     * (lowercase snake/kebab identifiers, locale tags, or product ids), max 64 chars.
     */
    private val tokenShape = Regex("^[a-z0-9][a-z0-9_.\\-]{0,63}$")

    override fun emit(eventName: String, props: Map<String, Any>) {
        if (eventName !in allowlist) return

        val filtered = props.filter { (key, value) ->
            key in propertyAllowlist &&
                !isDeniedKey(key) &&
                !isDeniedValue(value) &&
                isValidType(value)
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

    private fun isValidType(value: Any): Boolean = when (value) {
        is Boolean -> true
        is Int, is Long -> true
        is Float, is Double -> true
        is String -> tokenShape.matches(value)
        else -> false
    }
}
