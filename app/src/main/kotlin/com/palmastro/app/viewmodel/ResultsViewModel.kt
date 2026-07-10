package com.palmastro.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.app.config.FeatureFlags
import com.palmastro.contracts.ComparabilityBucket
import com.palmastro.contracts.Domains
import com.palmastro.contracts.SemanticPayload
import com.palmastro.data.repository.ResultRepository
import com.palmastro.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class DomainCard(
    val domain: String,
    val score: Int,
    val grade: String,
    val confidence: String = "",
    /** First sentence of the payload interpretation pattern; empty when unavailable. */
    val insight: String = "",
    /** Signed month-over-month change; null when unavailable or comparability is LOW. */
    val delta: Int? = null,
    /** "up" | "down" | "flat"; null when delta is hidden. */
    val deltaArrow: String? = null,
)

data class ResultsState(
    val isLoading: Boolean = true,
    val hasResults: Boolean = false,
    val domainCards: List<DomainCard> = emptyList(),
    val grade: String = "",
    val confidence: String = "",
    val monthKey: String = "",
    val tone: String = "scientific",
    val scanQualityScore: Int = 0,
    /** Domain key with the highest score; drives the month-theme line. */
    val topDomain: String? = null,
    val shareCardsEnabled: Boolean = true,
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resultRepository: ResultRepository,
    private val userRepository: UserRepository,
    private val featureFlags: FeatureFlags,
) : ViewModel() {
    private val _state = MutableStateFlow(ResultsState())
    val state = _state.asStateFlow()

    private val targetMonthKey: String? = savedStateHandle.get<String>("monthKey")
    private val json = Json { ignoreUnknownKeys = true }

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
                _state.update { it.copy(isLoading = false, hasResults = false) }
                return@launch
            }

            val scores: Map<String, Int> = try {
                json.decodeFromString(entity.domainScoresJson)
            } catch (_: Exception) {
                emptyMap()
            }
            val payloads: Map<String, SemanticPayload> = try {
                json.decodeFromString(entity.semanticPayloadsJson)
            } catch (_: Exception) {
                emptyMap()
            }
            val delta = try {
                resultRepository.getDeltaFor(entity.monthKey)
            } catch (_: Exception) {
                null
            }
            // Comparability gate (PRD delta rules): LOW-comparability deltas are not shown.
            val deltaComparable = delta != null && delta.comparabilityBucket != ComparabilityBucket.LOW

            val orderedDomains = Domains.ALL.filter { scores.containsKey(it) } +
                scores.keys.filterNot { it in Domains.ALL }

            val cards = orderedDomains.map { domain ->
                val payload = payloads[domain]
                val deltaValue = if (deltaComparable) delta?.domainDeltas?.get(domain) else null
                DomainCard(
                    domain = domain,
                    score = scores.getValue(domain),
                    grade = entity.grade,
                    confidence = payload?.confidence ?: entity.confidenceLevel,
                    insight = payload?.interpretation?.pattern?.let(::firstSentence).orEmpty(),
                    delta = deltaValue?.value,
                    deltaArrow = deltaValue?.arrow,
                )
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    hasResults = true,
                    domainCards = cards,
                    grade = entity.grade,
                    confidence = entity.confidenceLevel,
                    monthKey = entity.monthKey,
                    tone = profile?.tone ?: "scientific",
                    scanQualityScore = entity.scanQualityScore,
                    topDomain = scores.maxByOrNull { entry -> entry.value }?.key,
                    shareCardsEnabled = featureFlags.shareCardsEnabled,
                )
            }
        }
    }

    companion object {
        private val SENTENCE_ENDINGS = charArrayOf('.', '!', '?', '。', '！', '？')

        fun firstSentence(text: String): String {
            val trimmed = text.trim()
            val idx = trimmed.indexOfFirst { it in SENTENCE_ENDINGS }
            return if (idx == -1) trimmed else trimmed.substring(0, idx + 1)
        }
    }
}
