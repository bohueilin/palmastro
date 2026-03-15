package com.palmastro.app.viewmodel

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
    private val resultRepository: ResultRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ResultsState())
    val state = _state.asStateFlow()

    init {
        loadLatest()
    }

    fun loadLatest() {
        viewModelScope.launch {
            val results = resultRepository.getRecent(1)
            val profile = userRepository.get()
            if (results.isEmpty()) {
                _state.update { it.copy(hasResults = false) }
                return@launch
            }
            val latest = results.first()
            val scores = latest.domainScoresJson.removeSurrounding("{", "}")
                .split(",")
                .filter { it.contains(":") }
                .associate { entry ->
                    val (k, v) = entry.split(":")
                    k.trim('"') to v.trim().toInt()
                }
            val domainNames = mapOf(
                "career" to "\u4e8b\u696d",
                "wealth" to "\u8ca1\u5bcc",
                "family" to "\u5bb6\u5ead",
                "health" to "\u5065\u5eb7"
            )
            _state.update {
                it.copy(
                    hasResults = true,
                    domainCards = scores.map { (domain, score) ->
                        DomainCard(
                            domain = domain,
                            displayName = domainNames[domain] ?: domain,
                            score = score,
                            grade = latest.grade
                        )
                    },
                    grade = latest.grade,
                    confidence = latest.confidenceLevel,
                    monthKey = latest.monthKey,
                    tone = profile?.tone ?: "scientific",
                )
            }
        }
    }
}
