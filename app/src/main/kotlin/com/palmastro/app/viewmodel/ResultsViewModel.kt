package com.palmastro.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.app.config.FeatureFlags
import com.palmastro.content.GuidanceBuilder
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

/** Compact preview of the guidance layer shown on the Results "This month" entry card. */
data class GuidanceSummary(
    val monthTheme: String,
    /** Title of the first "lean into" strength; empty when none. */
    val firstStrengthTitle: String,
    /** Title of the first "be mindful of" item; empty when none. */
    val firstMindfulTitle: String,
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
    /** Guidance entry-card preview; null when guidance could not be built. */
    val guidance: GuidanceSummary? = null,
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resultRepository: ResultRepository,
    private val userRepository: UserRepository,
    private val featureFlags: FeatureFlags,
    // Default keeps existing direct constructions compiling; Hilt injects the singleton.
    private val guidanceBuilder: GuidanceBuilder = GuidanceBuilder(),
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
            val cards = buildDomainCards(entity, scores, payloads)
            val guidanceSummary = buildGuidanceSummary(entity.grade, payloads, profile?.language)

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
                    guidance = guidanceSummary,
                )
            }
        }
    }

    private suspend fun buildDomainCards(
        entity: com.palmastro.data.entities.MonthlyResultEntity,
        scores: Map<String, Int>,
        payloads: Map<String, SemanticPayload>,
    ): List<DomainCard> {
        val delta = try {
            resultRepository.getDeltaFor(entity.monthKey)
        } catch (_: Exception) {
            null
        }
        // Comparability gate (PRD delta rules): LOW-comparability deltas are not shown.
        val deltaComparable = delta != null && delta.comparabilityBucket != ComparabilityBucket.LOW
        val orderedDomains = Domains.ALL.filter { scores.containsKey(it) } +
            scores.keys.filterNot { it in Domains.ALL }
        return orderedDomains.map { domain ->
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
    }

    /**
     * Guidance preview for the "This month" entry card. Deterministic build from
     * the stored payloads; any failure degrades to "no card", never an error state.
     * Uses the language the payloads were composed in (falling back to the resolver)
     * so the preview never mixes languages with the stored content.
     */
    private fun buildGuidanceSummary(
        grade: String,
        payloads: Map<String, SemanticPayload>,
        profileLanguage: String?,
    ): GuidanceSummary? = runCatching {
        val language = storedPayloadLanguage(payloads) ?: resolveContentLanguage(profileLanguage)
        val guidance = guidanceBuilder.build(payloads, grade, language)
        GuidanceSummary(
            monthTheme = guidance.monthTheme,
            firstStrengthTitle = guidance.strengths.firstOrNull()?.title.orEmpty(),
            firstMindfulTitle = guidance.mindful.firstOrNull()?.title.orEmpty(),
        )
    }.getOrNull()

    companion object {
        private val SENTENCE_ENDINGS = charArrayOf('.', '!', '?', '。', '！', '？')

        fun firstSentence(text: String): String {
            val trimmed = text.trim()
            val idx = trimmed.indexOfFirst { it in SENTENCE_ENDINGS }
            return if (idx == -1) trimmed else trimmed.substring(0, idx + 1)
        }
    }
}
