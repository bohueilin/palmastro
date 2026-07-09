package com.palmastro.app.config

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlags @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("palmastro_feature_flags", Context.MODE_PRIVATE)

    val l2AstroEnabled: Boolean get() = prefs.getBoolean(KEY_L2_ASTRO, true)
    val contentSafetyStrict: Boolean get() = prefs.getBoolean(KEY_SAFETY_STRICT, true)
    val behavioralProfiling: Boolean get() = prefs.getBoolean(KEY_BEHAVIORAL, false)
    val deltaTracking: Boolean get() = prefs.getBoolean(KEY_DELTA, true)
    val shareCards: Boolean get() = prefs.getBoolean(KEY_SHARE, true)
    val scanReminders: Boolean get() = prefs.getBoolean(KEY_REMINDERS, true)

    fun set(key: String, enabled: Boolean) {
        prefs.edit().putBoolean(key, enabled).apply()
    }

    fun allFlags(): Map<String, Boolean> = mapOf(
        KEY_L2_ASTRO to l2AstroEnabled,
        KEY_SAFETY_STRICT to contentSafetyStrict,
        KEY_BEHAVIORAL to behavioralProfiling,
        KEY_DELTA to deltaTracking,
        KEY_SHARE to shareCards,
        KEY_REMINDERS to scanReminders,
    )

    companion object {
        const val KEY_L2_ASTRO = "l2_astro"
        const val KEY_SAFETY_STRICT = "safety_strict"
        const val KEY_BEHAVIORAL = "behavioral_profiling"
        const val KEY_DELTA = "delta_tracking"
        const val KEY_SHARE = "share_cards"
        const val KEY_REMINDERS = "scan_reminders"
    }
}
