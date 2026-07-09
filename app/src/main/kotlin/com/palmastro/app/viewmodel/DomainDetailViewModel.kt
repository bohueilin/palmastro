package com.palmastro.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.contracts.SemanticPayload
import com.palmastro.data.repository.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class DomainDetailState(
    val isLoading: Boolean = true,
    val domain: String = "",
    val displayName: String = "",
    val payload: SemanticPayload? = null,
    val error: String? = null,
)

@HiltViewModel
class DomainDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resultRepository: ResultRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DomainDetailState())
    val state = _state.asStateFlow()

    private val domain: String = savedStateHandle.get<String>("domain") ?: ""
    private val monthKey: String = savedStateHandle.get<String>("monthKey") ?: ""

    private val domainNames = mapOf(
        "career" to "Career", "wealth" to "Wealth", "family" to "Family", "health" to "Health"
    )

    private val json = Json { ignoreUnknownKeys = true }

    init { load() }

    private fun load() {
        viewModelScope.launch {
            try {
                val entity = resultRepository.getByMonth(monthKey)
                if (entity == null) {
                    _state.update { it.copy(isLoading = false, error = "Results not found") }
                    return@launch
                }

                val payloads: Map<String, SemanticPayload> =
                    json.decodeFromString(entity.semanticPayloadsJson)
                val payload = payloads[domain]

                _state.update {
                    it.copy(
                        isLoading = false,
                        domain = domain,
                        displayName = domainNames[domain] ?: domain,
                        payload = payload,
                        error = if (payload == null) "No analysis for this domain" else null,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
