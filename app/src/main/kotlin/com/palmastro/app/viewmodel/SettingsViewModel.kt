package com.palmastro.app.viewmodel

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.palmastro.app.worker.ScanReminderWorker
import com.palmastro.data.repository.UserRepository
import com.palmastro.data.repository.WipeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
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
                runCatching { WorkManager.getInstance(appContext).cancelUniqueWork(ScanReminderWorker.WORK_NAME) }
                _state.update { it.copy(isWiping = false, isWipeComplete = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isWiping = false, wipeError = true) }
            }
        }
    }

    fun dismissWipeError() {
        _state.update { it.copy(wipeError = false) }
    }

    private fun scheduleReminder(reminders: String) {
        // runCatching: WorkManager is not initialized in JVM unit tests.
        runCatching {
            val workManager = WorkManager.getInstance(appContext)

            if (reminders == "off") {
                workManager.cancelUniqueWork(ScanReminderWorker.WORK_NAME)
                return@runCatching
            }

            val request = PeriodicWorkRequestBuilder<ScanReminderWorker>(30, TimeUnit.DAYS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                ScanReminderWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
