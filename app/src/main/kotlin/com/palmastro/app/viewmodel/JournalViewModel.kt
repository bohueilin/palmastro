package com.palmastro.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.data.entities.JournalEntryEntity
import com.palmastro.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JournalState(
    val monthKey: String = "",
    val domain: String? = null,
    val text: String = "",
    /**
     * What the repository actually holds, so an entry opened only to be re-read is
     * never mistaken for an unsaved draft.
     */
    val savedText: String = "",
    val existingEntries: List<JournalEntryEntity> = emptyList(),
    val isSaved: Boolean = false,
    val charCount: Int = 0,
    val maxChars: Int = JournalRepository.MAX_CHARS,
    /** Set once a save requested on the way out has actually been written. */
    val exitRequested: Boolean = false,
) {
    /** Edits the repository would actually store — a blank field has nothing to lose. */
    val hasUnsavedText: Boolean get() = text.isNotBlank() && text != savedText
}

@HiltViewModel
class JournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(JournalState())
    val state = _state.asStateFlow()

    private val monthKey: String = savedStateHandle.get<String>("monthKey") ?: ""
    private val domain: String? = savedStateHandle.get<String>("domain")

    init {
        _state.update { it.copy(monthKey = monthKey, domain = domain) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val existing = if (domain != null) {
                journalRepository.getByMonthAndDomain(monthKey, domain)
            } else {
                null
            }
            val allEntries = journalRepository.getByMonth(monthKey)

            _state.update {
                it.copy(
                    text = existing?.text ?: "",
                    // Seeded from the stored text, not the draft: saveEntry trims and
                    // truncates, so anything else would leave the entry always dirty.
                    savedText = existing?.text ?: "",
                    charCount = existing?.text?.length ?: 0,
                    existingEntries = allEntries,
                )
            }
        }
    }

    fun updateText(newText: String) {
        val truncated = newText.take(JournalRepository.MAX_CHARS)
        _state.update { it.copy(text = truncated, charCount = truncated.length, isSaved = false) }
    }

    fun save() {
        viewModelScope.launch {
            journalRepository.saveEntry(monthKey, domain, _state.value.text)
            // Marked clean before the re-read so a slow load() cannot flash a dirty state.
            _state.update { it.copy(isSaved = true, savedText = it.text) }
            load()
        }
    }

    /**
     * Saves, then tells the screen it may leave. Navigating away first would cancel
     * viewModelScope mid-write, which is exactly the loss this flow exists to prevent.
     */
    fun saveAndExit() {
        viewModelScope.launch {
            journalRepository.saveEntry(monthKey, domain, _state.value.text)
            _state.update { it.copy(isSaved = true, savedText = it.text, exitRequested = true) }
        }
    }

    /** Deletes one entry and refreshes the list without clobbering in-progress edits. */
    fun deleteEntry(id: String) {
        viewModelScope.launch {
            journalRepository.deleteEntry(id)
            val entries = journalRepository.getByMonth(monthKey)
            _state.update { it.copy(existingEntries = entries) }
        }
    }
}
