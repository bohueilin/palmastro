package com.palmastro.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.data.repository.ResultRepository
import com.palmastro.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class DomainCard(
    val domain: String,
    val displayName: String,
    val score: Int,
    val grade: String,
)

data class ResultsState(
    val hasResults: Boolean = false,
    val domainCards: List<DomainCard> = emptyList(),
    val grade: String = "",
    val confidence: String = "",
    val monthKey: String = "",
    val tone: String = "scientific",
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resultRepository: ResultRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ResultsState())
    val state = _state.asStateFlow()

    private val targetMonthKey: String? = savedStateHandle.get<String>("monthKey")

    init { load() }

    fun load() {
        viewModelScope.launch {
            val entity = if (targetMonthKey != null) {
                resultRepository.getByMonth(targetMonthKey)
            } else {
                resultRepository.getRecent(1).firstOrNull()
            }
            val profile = userRepository.get()

            if (entity == null) {
                _state.update { it.copy(hasResults = false) }
                return@launch
            }

            val scores: Map<String, Int> = try {
                Json.decodeFromString(entity.domainScoresJson)
            } catch (_: Exception) {
                emptyMap()
            }
            val domainNames = mapOf(
                "career" to "Career",
                "wealth" to "Wealth",
                "family" to "Family",
                "health" to "Health"
            )
            _state.update {
                it.copy(
                    hasResults = true,
                    domainCards = scores.map { (domain, score) ->
                        DomainCard(
                            domain = domain,
                            displayName = domainNames[domain] ?: domain,
                            score = score,
                            grade = entity.grade
                        )
                    },
                    grade = entity.grade,
                    confidence = when (entity.confidenceLevel) { "high" -> "High"; "med" -> "Medium"; "low" -> "Low"; else -> entity.confidenceLevel },
                    monthKey = entity.monthKey,
                    tone = profile?.tone ?: "scientific",
                )
            }
        }
    }
}
