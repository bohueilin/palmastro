package com.palmastro.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.data.repository.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class MonthSummary(
    val monthKey: String,
    val grade: String,
    val confidence: String,
    val domainScores: Map<String, Int>,
    val createdAt: Long,
    /** Signed score change per domain vs the previous (chronological) record; empty for the oldest record. */
    val deltas: Map<String, Int> = emptyMap(),
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

    private val json = Json { ignoreUnknownKeys = true }

    init { load() }

    private fun load() {
        viewModelScope.launch {
            resultRepository.observeAll().collect { entities ->
                // observeAll() is ordered createdAt DESC: index i+1 is the previous month.
                val summaries = entities.map { entity ->
                    val scores: Map<String, Int> = try {
                        json.decodeFromString(entity.domainScoresJson)
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
                val withDeltas = summaries.mapIndexed { index, month ->
                    val previous = summaries.getOrNull(index + 1) ?: return@mapIndexed month
                    month.copy(
                        deltas = month.domainScores.mapNotNull { (domain, score) ->
                            previous.domainScores[domain]?.let { prevScore -> domain to (score - prevScore) }
                        }.toMap(),
                    )
                }
                _state.update { it.copy(isLoading = false, months = withDeltas) }
            }
        }
    }
}
