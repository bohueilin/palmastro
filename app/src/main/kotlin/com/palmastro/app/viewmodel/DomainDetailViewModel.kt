package com.palmastro.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.app.config.FeatureFlags
import com.palmastro.contracts.SemanticPayload
import com.palmastro.data.repository.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class DomainDetailState(
    val isLoading: Boolean = true,
    val domain: String = "",
    val payload: SemanticPayload? = null,
    /** Quality factors surfaced on the Explainability screen (PRD 13.5). */
    val scanQualityScore: Int = 0,
    val featureCoverage: Float = 0f,
    val shareCardsEnabled: Boolean = true,
    /**
     * Non-null when loading failed. Internal diagnostic code only — the UI maps any
     * non-null value to a localized message, never displays this string directly.
     */
    val error: String? = null,
)

@HiltViewModel
class DomainDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resultRepository: ResultRepository,
    private val featureFlags: FeatureFlags,
) : ViewModel() {
    private val _state = MutableStateFlow(DomainDetailState())
    val state = _state.asStateFlow()

    private val domain: String = savedStateHandle.get<String>("domain") ?: ""
    private val monthKey: String = savedStateHandle.get<String>("monthKey") ?: ""

    private val json = Json { ignoreUnknownKeys = true }

    init { load() }

    /** Re-runs the load behind the failure state's retry button; no other state is reset. */
    fun retry() {
        _state.update { it.copy(isLoading = true, error = null) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val entity = resultRepository.getByMonth(monthKey)
                if (entity == null) {
                    _state.update { it.copy(isLoading = false, domain = domain, error = "not_found") }
                    return@launch
                }

                val payloads: Map<String, SemanticPayload> =
                    json.decodeFromString(entity.semanticPayloadsJson)
                val payload = payloads[domain]

                _state.update {
                    it.copy(
                        isLoading = false,
                        domain = domain,
                        payload = payload,
                        scanQualityScore = entity.scanQualityScore,
                        featureCoverage = entity.featureCoverage,
                        shareCardsEnabled = featureFlags.shareCardsEnabled,
                        error = if (payload == null) "domain_not_found" else null,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, domain = domain, error = e.message ?: "load_failed") }
            }
        }
    }
}
