package com.palmastro.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.data.repository.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class MonthSummary(
    val monthKey: String,
    val grade: String,
    val confidence: String,
    val domainScores: Map<String, Int>,
    val createdAt: Long,
)

data class HistoryState(
    val isLoading: Boolean = true,
    val months: List<MonthSummary> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val resultRepository: ResultRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HistoryState())
    val state = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            resultRepository.observeAll().collect { entities ->
                val months = entities.map { entity ->
                    val scores: Map<String, Int> = try {
                        Json.decodeFromString(entity.domainScoresJson)
                    } catch (_: Exception) {
                        emptyMap()
                    }
                    MonthSummary(
                        monthKey = entity.monthKey,
                        grade = entity.grade,
                        confidence = entity.confidenceLevel,
                        domainScores = scores,
                        createdAt = entity.createdAt,
                    )
                }
                _state.update { it.copy(isLoading = false, months = months) }
            }
        }
    }
}
