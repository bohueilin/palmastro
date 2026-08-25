package com.palmastro.app.viewmodel

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.app.BuildConfig
import com.palmastro.app.config.FeatureFlags
import com.palmastro.app.worker.ScanReminderWorker
import com.palmastro.data.repository.UserRepository
import com.palmastro.data.repository.WipeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val tone: String = "scientific",
    val reminders: String = "off",
    val rawMediaRetention: Boolean = true,
    val dominantHand: String = "right",
    /** "system" | "en" | "zh-TW" — persisted on the profile, applied via per-app locales. */
    val language: String = "system",
    val isWiping: Boolean = false,
    val isWipeComplete: Boolean = false,
    /** True when the last wipe attempt failed; UI shows a localized error dialog. */
    val wipeError: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userRepository: UserRepository,
    private val wipeManager: WipeManager,
    private val featureFlags: FeatureFlags,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val profile = userRepository.get() ?: return@launch
            _state.update {
                it.copy(
                    tone = profile.tone,
                    reminders = profile.reminders,
                    rawMediaRetention = profile.rawMediaRetention,
                    dominantHand = profile.dominantHand,
                    language = profile.language,
                )
            }
            // Re-applies the cadence so a schedule left by an older build — which ran
            // "1st of each month" as a plain 30-day loop — is corrected. Idempotent:
            // the 30-day path updates in place and the calendar path recomputes the
            // same target date.
            scheduleReminder(profile.reminders)
        }
    }

    fun setTone(tone: String) {
        _state.update { it.copy(tone = tone) }
        viewModelScope.launch {
            val profile = userRepository.get() ?: return@launch
            userRepository.save(profile.copy(tone = tone, updatedAt = System.currentTimeMillis()))
        }
    }

    fun setLanguage(language: String) {
        _state.update { it.copy(language = language) }
        viewModelScope.launch {
            val profile = userRepository.get() ?: return@launch
            userRepository.save(profile.copy(language = language, updatedAt = System.currentTimeMillis()))
        }
        applyAppLocale(language)
    }

    /**
     * Applies the per-app locale (AppCompat 1.6+ backport of Android 13 per-app
     * languages). Wrapped in runCatching so JVM unit tests and non-AppCompat hosts do
     * not crash — the choice is still persisted on the profile and re-applied by the
     * Activity on next start.
     */
    private fun applyAppLocale(language: String) {
        runCatching {
            val locales = if (language == "system") {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(language)
            }
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    fun setReminders(reminders: String) {
        _state.update { it.copy(reminders = reminders) }
        viewModelScope.launch {
            val profile = userRepository.get() ?: return@launch
            userRepository.save(profile.copy(reminders = reminders, updatedAt = System.currentTimeMillis()))
        }
        scheduleReminder(reminders)
    }

    fun setRetention(enabled: Boolean) {
        _state.update { it.copy(rawMediaRetention = enabled) }
        viewModelScope.launch {
            val profile = userRepository.get() ?: return@launch
            userRepository.save(profile.copy(rawMediaRetention = enabled, updatedAt = System.currentTimeMillis()))
            if (!enabled) {
                wipeManager.deleteAllScanImages()
            }
        }
    }

    fun deleteAllData() {
        _state.update { it.copy(isWiping = true, wipeError = false) }
        viewModelScope.launch {
            try {
                wipeManager.deleteAllData()
                runCatching { ScanReminderWorker.cancel(appContext) }
                _state.update { it.copy(isWiping = false, isWipeComplete = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isWiping = false, wipeError = true) }
            }
        }
    }

    fun dismissWipeError() {
        _state.update { it.copy(wipeError = false) }
    }

    /**
     * Support diagnostics (PRD §13.8). Deliberately limited to PRD §26's lower-risk
     * fields — build, locale, device, flags. Nothing derived from a reading belongs
     * here: no birthday, birth place, palm features, scores, insights or journal text.
     * The user sees the whole string before it is sent (PRD §13.7).
     */
    fun buildDiagnosticReport(): String {
        val s = _state.value
        val flags = featureFlags.allFlags().entries.joinToString("\n") { "  ${it.key}=${it.value}" }
        return buildString {
            appendLine("PalmAstro diagnostics")
            appendLine("app: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("language pref: ${s.language}")
            appendLine("locale: ${resolvedLocaleTag()}")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("flags:")
            append(flags)
        }
    }

    /**
     * The locale actually in force, which is what a support ticket needs: the profile
     * preference reads "system" for most users and says nothing about what they saw.
     * runCatching so a host without real resources still produces a usable report.
     */
    private fun resolvedLocaleTag(): String = runCatching {
        appContext.resources.configuration.locales[0].toLanguageTag()
    }.getOrDefault("unknown")

    private fun scheduleReminder(reminders: String) {
        // runCatching: WorkManager is not initialized in JVM unit tests.
        runCatching { ScanReminderWorker.schedule(appContext, reminders) }
    }
}
