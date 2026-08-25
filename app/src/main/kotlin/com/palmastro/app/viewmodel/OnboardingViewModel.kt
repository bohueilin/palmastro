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

/** Onboarding step indices per PRD 13.1 (12 launch steps folded into 10 screens). */
object OnboardingSteps {
    const val WELCOME = 0
    const val PRIVACY = 1
    const val NAME = 2
    const val BIRTHDAY = 3
    const val HAND = 4
    const val BIRTH_DETAILS = 5
    const val TONE = 6
    const val LANGUAGE = 7
    const val SUMMARY = 8
    const val CAMERA = 9
    const val TOTAL = 10
}

data class OnboardingState(
    val step: Int = OnboardingSteps.WELCOME,
    val name: String = "",
    val gender: String? = null,
    /** No silent default — the user must make an explicit choice. */
    val dominantHand: String? = null,
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
    /** "system" | "en" | "zh-TW" — persisted to profile.language. */
    val language: String = "system",
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
    fun setLanguage(language: String) = _state.update { it.copy(language = language) }
    fun setBirthTime(hour: Int, minute: Int) = _state.update { it.copy(hasBirthTime = true, birthTimeHour = hour, birthTimeMinute = minute) }
    fun setBirthPlace(name: String, lat: Double, lon: Double) = _state.update { it.copy(hasBirthPlace = true, birthPlaceName = name, birthPlaceLat = lat, birthPlaceLon = lon) }
    /**
     * Confirming the optional birth step states both fields at once: an hour or a place
     * the user cleared on a return visit must actually clear, and an hour never chosen
     * must not be recorded as a real birth time — that is what promotes a reading to L2.
     */
    fun setBirthDetails(hour: Int?, minute: Int?, placeName: String?, lat: Double?, lon: Double?) = _state.update {
        it.copy(
            hasBirthTime = hour != null,
            birthTimeHour = hour ?: it.birthTimeHour,
            birthTimeMinute = minute ?: it.birthTimeMinute,
            hasBirthPlace = placeName != null,
            birthPlaceName = placeName ?: "",
            birthPlaceLat = lat,
            birthPlaceLon = lon,
        )
    }
    fun skipBirthDetails() = _state.update { it.copy(hasBirthTime = false, hasBirthPlace = false) }
    fun setRelationshipStatus(status: String) = _state.update { it.copy(relationshipStatus = status) }
    fun nextStep() = _state.update { it.copy(step = minOf(OnboardingSteps.TOTAL - 1, it.step + 1)) }
    fun prevStep() = _state.update { it.copy(step = maxOf(0, it.step - 1)) }

    /**
     * Required/optional gating per PRD 13.1 acceptance criteria:
     * birthday and dominant hand are required; everything else is optional.
     */
    fun canProceedFrom(step: Int): Boolean = when (step) {
        OnboardingSteps.BIRTHDAY -> _state.value.birthday != null
        OnboardingSteps.HAND -> _state.value.dominantHand != null
        else -> true
    }

    fun canComplete(): Boolean =
        _state.value.birthday != null && _state.value.dominantHand != null

    fun completeOnboarding() {
        val s = _state.value
        val birthday = s.birthday ?: return
        val hand = s.dominantHand ?: return
        val calcLevel = if (s.hasBirthTime && s.hasBirthPlace) "L2" else "L1"
        viewModelScope.launch {
            userRepository.save(UserProfileEntity(
                dominantHand = hand,
                birthdayEpochDay = birthday.toEpochDay(),
                hasBirthTime = s.hasBirthTime,
                birthTimeMinutes = if (s.hasBirthTime) s.birthTimeHour * 60 + s.birthTimeMinute else null,
                hasBirthPlace = s.hasBirthPlace,
                birthPlaceLat = s.birthPlaceLat,
                birthPlaceLon = s.birthPlaceLon,
                birthPlaceName = s.birthPlaceName.ifBlank { null },
                tone = s.tone,
                calcLevel = calcLevel,
                name = s.name.trim().ifBlank { null },
                gender = s.gender,
                relationshipStatus = s.relationshipStatus,
                language = s.language,
            ))
            _state.update { it.copy(isComplete = true) }
        }
    }
}
