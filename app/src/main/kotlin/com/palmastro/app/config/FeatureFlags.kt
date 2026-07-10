package com.palmastro.app.config

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Launch feature flags (PRD §69). Defaults are the production launch configuration;
 * flags for surfaces that do not ship at launch act as kill-switch guards for the
 * code paths that would mount them.
 */
@Singleton
class FeatureFlags @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("palmastro_feature_flags", Context.MODE_PRIVATE)

    val dailyInsightsEnabled: Boolean get() = prefs.getBoolean(KEY_DAILY_INSIGHTS, false)
    val llmInterpretationsEnabled: Boolean get() = prefs.getBoolean(KEY_LLM, false)
    val iapEnabled: Boolean get() = prefs.getBoolean(KEY_IAP, false)
    val wearEnabled: Boolean get() = prefs.getBoolean(KEY_WEAR, false)
    val widgetEnabled: Boolean get() = prefs.getBoolean(KEY_WIDGET, false)
    val shareCardsEnabled: Boolean get() = prefs.getBoolean(KEY_SHARE_CARDS, true)
    val strictSafetyEnabled: Boolean get() = prefs.getBoolean(KEY_STRICT_SAFETY, true)
    val debugScanBypassEnabled: Boolean get() = prefs.getBoolean(KEY_DEBUG_SCAN_BYPASS, false)
    val scanRemindersEnabled: Boolean get() = prefs.getBoolean(KEY_SCAN_REMINDERS, true)
    val l2AstroEnabled: Boolean get() = prefs.getBoolean(KEY_L2_ASTRO, true)
    val deltaTrackingEnabled: Boolean get() = prefs.getBoolean(KEY_DELTA, true)

    fun set(key: String, enabled: Boolean) {
        prefs.edit().putBoolean(key, enabled).apply()
    }

    fun allFlags(): Map<String, Boolean> = mapOf(
        KEY_DAILY_INSIGHTS to dailyInsightsEnabled,
        KEY_LLM to llmInterpretationsEnabled,
        KEY_IAP to iapEnabled,
        KEY_WEAR to wearEnabled,
        KEY_WIDGET to widgetEnabled,
        KEY_SHARE_CARDS to shareCardsEnabled,
        KEY_STRICT_SAFETY to strictSafetyEnabled,
        KEY_DEBUG_SCAN_BYPASS to debugScanBypassEnabled,
        KEY_SCAN_REMINDERS to scanRemindersEnabled,
        KEY_L2_ASTRO to l2AstroEnabled,
        KEY_DELTA to deltaTrackingEnabled,
    )

    companion object {
        const val KEY_DAILY_INSIGHTS = "daily_insights_enabled"
        const val KEY_LLM = "llm_interpretations_enabled"
        const val KEY_IAP = "iap_enabled"
        const val KEY_WEAR = "wear_enabled"
        const val KEY_WIDGET = "widget_enabled"
        const val KEY_SHARE_CARDS = "share_cards_enabled"
        const val KEY_STRICT_SAFETY = "strict_safety_enabled"
        const val KEY_DEBUG_SCAN_BYPASS = "debug_scan_bypass_enabled"
        const val KEY_SCAN_REMINDERS = "scan_reminders_enabled"
        const val KEY_L2_ASTRO = "l2_astro_enabled"
        const val KEY_DELTA = "delta_tracking_enabled"
    }
}
