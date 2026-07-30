package com.palmastro.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.content.Guidance
import com.palmastro.content.GuidanceBuilder
import com.palmastro.contracts.SemanticPayload
import com.palmastro.data.repository.ResultRepository
import com.palmastro.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject

data class GuidanceState(
    val isLoading: Boolean = true,
    val monthKey: String = "",
    val grade: String = "",
    /** Deterministic guidance built from the stored payloads; null while loading or on error. */
    val guidance: Guidance? = null,
    /**
     * Non-null when loading failed. Internal diagnostic code only — the UI maps any
     * non-null value to a localized message, never displays this string directly.
     */
    val error: String? = null,
)

/**
 * Loads the "Understand your reading" guidance layer (PRD §§11–13, 30–32) for one month.
 * Same decode pattern as [DomainDetailViewModel]; language resolution mirrors the scan
 * pipeline so guidance always matches the language the payloads were composed in.
 */
@HiltViewModel
class GuidanceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resultRepository: ResultRepository,
    private val userRepository: UserRepository,
    private val guidanceBuilder: GuidanceBuilder,
) : ViewModel() {
    private val _state = MutableStateFlow(GuidanceState())
    val state = _state.asStateFlow()

    private val monthKey: String = savedStateHandle.get<String>("monthKey") ?: ""

    private val json = Json { ignoreUnknownKeys = true }

    init { load() }

    private fun load() {
        viewModelScope.launch {
            try {
                val entity = resultRepository.getByMonth(monthKey)
                if (entity == null) {
                    _state.update { it.copy(isLoading = false, monthKey = monthKey, error = "not_found") }
                    return@launch
                }

                val payloads: Map<String, SemanticPayload> =
                    json.decodeFromString(entity.semanticPayloadsJson)
                val language = storedPayloadLanguage(payloads)
                    ?: resolveContentLanguage(userRepository.get()?.language)
                val guidance = guidanceBuilder.build(payloads, entity.grade, language)

                _state.update {
                    it.copy(
                        isLoading = false,
                        monthKey = entity.monthKey,
                        grade = entity.grade,
                        guidance = guidance,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, monthKey = monthKey, error = e.message ?: "load_failed")
                }
            }
        }
    }
}

/**
 * The language the stored payloads were COMPOSED in, or null when absent. Guidance built
 * from stored payloads must use this language: re-resolving from the current profile or
 * device locale can disagree with historical payloads (profile/locale changed since the
 * scan) and would render mixed-language guidance.
 */
internal fun storedPayloadLanguage(payloads: Map<String, SemanticPayload>): String? =
    payloads.values.firstOrNull()?.language?.takeIf { it.isNotBlank() }

/**
 * Resolves the content language: explicit profile choice wins; "system" (the v3 default)
 * follows the device locale, restricted to the engine-supported set. Mirrors the private
 * resolver in ScanViewModel. Fallback only — payloads that carry their own language win
 * via [storedPayloadLanguage].
 */
internal fun resolveContentLanguage(profileLanguage: String?): String {
    val explicit = profileLanguage?.takeUnless { it.isBlank() || it == "system" }
    if (explicit != null) return explicit
    val locale = Locale.getDefault()
    return when {
        locale.language == "zh" &&
            (locale.script == "Hant" || locale.country in setOf("TW", "HK", "MO")) -> "zh-TW"
        locale.language == "zh" -> "zh-CN"
        locale.language == "ja" -> "ja"
        locale.language == "hi" -> "hi"
        else -> "en"
    }
}
