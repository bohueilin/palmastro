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
    val name: String = "",
    val gender: String? = null,
    val dominantHand: String = "right",
    val birthday: LocalDate? = null,
    val birthdayMonth: Int = 1,
    val birthdayDay: Int = 1,
    val birthdayYear: Int = 1990,
    val hasBirthTime: Boolean = false,
    val birthTimeHour: Int = 12,
    val birthTimeMinute: Int = 0,
    val hasBirthPlace: Boolean = false,
    val birthPlaceName: String = "",
    val birthPlaceLat: Double? = null,
    val birthPlaceLon: Double? = null,
    val relationshipStatus: String? = null,
    val tone: String = "scientific",
    val isComplete: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state = _state.asStateFlow()

    fun setName(name: String) = _state.update { it.copy(name = name) }
    fun setGender(gender: String) = _state.update { it.copy(gender = gender) }
    fun setHand(hand: String) = _state.update { it.copy(dominantHand = hand) }
    fun setBirthdayParts(year: Int, month: Int, day: Int) {
        try {
            val date = LocalDate.of(year, month, day)
            _state.update { it.copy(birthday = date, birthdayYear = year, birthdayMonth = month, birthdayDay = day) }
        } catch (_: Exception) {
            _state.update { it.copy(birthdayYear = year, birthdayMonth = month, birthdayDay = day) }
        }
    }
    fun setBirthday(date: LocalDate) = _state.update { it.copy(birthday = date, birthdayYear = date.year, birthdayMonth = date.monthValue, birthdayDay = date.dayOfMonth) }
    fun setTone(tone: String) = _state.update { it.copy(tone = tone) }
    fun setBirthTime(hour: Int, minute: Int) = _state.update { it.copy(hasBirthTime = true, birthTimeHour = hour, birthTimeMinute = minute) }
    fun setBirthPlace(name: String, lat: Double, lon: Double) = _state.update { it.copy(hasBirthPlace = true, birthPlaceName = name, birthPlaceLat = lat, birthPlaceLon = lon) }
    fun skipBirthDetails() = _state.update { it.copy(hasBirthTime = false, hasBirthPlace = false) }
    fun setRelationshipStatus(status: String) = _state.update { it.copy(relationshipStatus = status) }
    fun nextStep() = _state.update { it.copy(step = it.step + 1) }
    fun prevStep() = _state.update { it.copy(step = maxOf(0, it.step - 1)) }

    fun completeOnboarding() {
        val s = _state.value
        val birthday = s.birthday ?: return
        val calcLevel = if (s.hasBirthTime && s.hasBirthPlace) "L2" else "L1"
        viewModelScope.launch {
            userRepository.save(UserProfileEntity(
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
                name = s.name.ifBlank { null },
                gender = s.gender,
                relationshipStatus = s.relationshipStatus,
            ))
            _state.update { it.copy(isComplete = true) }
        }
    }
}
