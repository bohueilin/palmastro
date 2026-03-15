package com.palmastro.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.data.repository.ResultRepository
import com.palmastro.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

            val scores = entity.domainScoresJson.removeSurrounding("{", "}")
                .split(",")
                .filter { it.contains(":") }
                .associate { entry ->
                    val (k, v) = entry.split(":")
                    k.trim('"') to v.trim().toInt()
                }
            val domainNames = mapOf(
                "career" to "事業",
                "wealth" to "財富",
                "family" to "家庭",
                "health" to "健康"
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
                    confidence = entity.confidenceLevel,
                    monthKey = entity.monthKey,
                    tone = profile?.tone ?: "scientific",
                )
            }
        }
    }
}
