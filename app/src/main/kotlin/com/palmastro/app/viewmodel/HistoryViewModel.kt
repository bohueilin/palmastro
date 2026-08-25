package com.palmastro.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.contracts.ComparabilityBucket
import com.palmastro.contracts.DeltaResult
import com.palmastro.data.entities.MonthlyResultEntity
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
    /** Signed score change per domain, from the stored delta; empty when none may be shown. */
    val deltas: Map<String, Int> = emptyMap(),
    /** MED comparability: the deltas are shown, but weakened rather than stated flatly. */
    val deltaApproximate: Boolean = false,
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
                val months = entities.map { entity -> summarize(entity) }
                _state.update { it.copy(isLoading = false, months = months) }
            }
        }
    }

    /**
     * One stored month plus the delta the scan pipeline computed for it. History and
     * Results must agree, so the change comes from the same stored [DeltaResult] both
     * surfaces read — never a raw month-to-month subtraction, which would show arrows
     * for a pair of scans the engine judged too different to compare.
     */
    private suspend fun summarize(entity: MonthlyResultEntity): MonthSummary {
        val scores: Map<String, Int> = try {
            json.decodeFromString(entity.domainScoresJson)
        } catch (_: Exception) {
            emptyMap()
        }
        val delta = comparableDelta(entity.monthKey)
        return MonthSummary(
            monthKey = entity.monthKey,
            grade = entity.grade,
            confidence = entity.confidenceLevel,
            domainScores = scores,
            createdAt = entity.createdAt,
            deltas = delta?.domainDeltas?.mapValues { (_, change) -> change.value }.orEmpty(),
            deltaApproximate = delta?.comparabilityBucket == ComparabilityBucket.MED,
        )
    }

    /**
     * The stored delta, or null whenever no arrow may be drawn: LOW comparability is
     * gated off entirely (PRD delta rules), and a missing or unreadable row means
     * "no arrows", never a failed screen.
     */
    private suspend fun comparableDelta(monthKey: String): DeltaResult? = try {
        resultRepository.getDeltaFor(monthKey)
            ?.takeIf { it.comparabilityBucket != ComparabilityBucket.LOW }
    } catch (_: Exception) {
        null
    }
}
