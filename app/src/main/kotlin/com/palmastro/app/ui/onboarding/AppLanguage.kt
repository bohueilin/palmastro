package com.palmastro.app.ui.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Applies the persisted profile language ("system" | "en" | "zh-TW") as the
 * per-app locale. Requires the hosting activity to be an AppCompatActivity
 * so pre-API-33 devices get the compat behavior.
 */
object AppLanguage {
    const val SYSTEM = "system"
    const val ENGLISH = "en"
    const val TRADITIONAL_CHINESE = "zh-TW"

    val SUPPORTED = listOf(SYSTEM, ENGLISH, TRADITIONAL_CHINESE)

    fun apply(code: String) {
        val target = localesFor(code)
        // Avoid redundant activity recreation on startup re-application.
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != target.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(target)
        }
    }

    private fun localesFor(code: String): LocaleListCompat = when (code) {
        ENGLISH -> LocaleListCompat.forLanguageTags("en")
        TRADITIONAL_CHINESE -> LocaleListCompat.forLanguageTags("zh-TW")
        else -> LocaleListCompat.getEmptyLocaleList()
    }
}
