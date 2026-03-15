package com.palmastro.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.data.repository.UserRepository
import com.palmastro.data.repository.WipeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    }

    fun setRetention(enabled: Boolean) {
        _state.update { it.copy(rawMediaRetention = enabled) }
        viewModelScope.launch {
            val profile = userRepository.get() ?: return@launch
            userRepository.save(profile.copy(rawMediaRetention = enabled, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteAllData() {
        _state.update { it.copy(isWiping = true) }
        viewModelScope.launch {
            wipeManager.deleteAllData()
            _state.update { it.copy(isWiping = false, isWipeComplete = true) }
        }
    }
}
