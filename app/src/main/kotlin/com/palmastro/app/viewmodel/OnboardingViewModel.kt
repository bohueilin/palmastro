package com.palmastro.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.data.entities.UserProfileEntity
import com.palmastro.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class OnboardingState(
    val step: Int = 0,
    val dominantHand: String = "right",
    val birthday: LocalDate? = null,
    val hasBirthTime: Boolean = false,
    val birthTimeHour: Int = 12,
    val birthTimeMinute: Int = 0,
    val hasBirthPlace: Boolean = false,
    val birthPlaceName: String = "",
    val birthPlaceLat: Double? = null,
    val birthPlaceLon: Double? = null,
    val tone: String = "scientific",
    val isComplete: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state = _state.asStateFlow()

    fun setHand(hand: String) = _state.update { it.copy(dominantHand = hand) }
    fun setBirthday(date: LocalDate) = _state.update { it.copy(birthday = date) }
    fun setTone(tone: String) = _state.update { it.copy(tone = tone) }
    fun setBirthTime(hour: Int, minute: Int) = _state.update {
        it.copy(hasBirthTime = true, birthTimeHour = hour, birthTimeMinute = minute)
    }
    fun setBirthPlace(name: String, lat: Double, lon: Double) = _state.update {
        it.copy(hasBirthPlace = true, birthPlaceName = name, birthPlaceLat = lat, birthPlaceLon = lon)
    }
    fun skipBirthDetails() = _state.update { it.copy(hasBirthTime = false, hasBirthPlace = false) }

    fun nextStep() = _state.update { it.copy(step = it.step + 1) }
    fun prevStep() = _state.update { it.copy(step = maxOf(0, it.step - 1)) }

    fun completeOnboarding() {
        val s = _state.value
        val birthday = s.birthday ?: return
        val calcLevel = if (s.hasBirthTime && s.hasBirthPlace) "L2" else "L1"

        viewModelScope.launch {
            userRepository.save(
                UserProfileEntity(
                    dominantHand = s.dominantHand,
                    birthdayEpochDay = birthday.toEpochDay(),
                    hasBirthTime = s.hasBirthTime,
                    birthTimeMinutes = if (s.hasBirthTime) s.birthTimeHour * 60 + s.birthTimeMinute else null,
                    hasBirthPlace = s.hasBirthPlace,
                    birthPlaceLat = s.birthPlaceLat,
                    birthPlaceLon = s.birthPlaceLon,
                    birthPlaceName = s.birthPlaceName.ifBlank { null },
                    tone = s.tone,
                    calcLevel = calcLevel,
                )
            )
            _state.update { it.copy(isComplete = true) }
        }
    }
}
