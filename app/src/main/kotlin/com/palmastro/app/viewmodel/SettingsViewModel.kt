package com.palmastro.app.viewmodel

import android.content.Context
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
    val reminders: String = "monthly",
    val rawMediaRetention: Boolean = true,
    val dominantHand: String = "right",
    val isWiping: Boolean = false,
    val isWipeComplete: Boolean = false,
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
        _state.update { it.copy(isWiping = true) }
        viewModelScope.launch {
            wipeManager.deleteAllData()
            WorkManager.getInstance(appContext).cancelUniqueWork(ScanReminderWorker.WORK_NAME)
            _state.update { it.copy(isWiping = false, isWipeComplete = true) }
        }
    }

    private fun scheduleReminder(reminders: String) {
        val workManager = WorkManager.getInstance(appContext)

        if (reminders == "off") {
            workManager.cancelUniqueWork(ScanReminderWorker.WORK_NAME)
            return
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
