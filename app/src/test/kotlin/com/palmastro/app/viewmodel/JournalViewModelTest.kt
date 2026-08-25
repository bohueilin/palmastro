package com.palmastro.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.palmastro.data.entities.JournalEntryEntity
import com.palmastro.data.repository.JournalRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModelTest {
    private val journalRepository = mockk<JournalRepository>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach fun setUp() { Dispatchers.setMain(testDispatcher); clearAllMocks() }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    private fun createViewModel(
        monthKey: String = "2026-03",
        domain: String? = null,
        existing: JournalEntryEntity? = null,
        entries: List<JournalEntryEntity> = emptyList(),
    ): JournalViewModel {
        val handle = SavedStateHandle(buildMap {
            put("monthKey", monthKey)
            if (domain != null) put("domain", domain)
        })
        coEvery { journalRepository.getByMonth(any()) } returns entries
        coEvery { journalRepository.getByMonthAndDomain(any(), any()) } returns existing
        return JournalViewModel(handle, journalRepository)
    }

    @Test
    fun `initial state has correct monthKey and domain`() {
        val vm = createViewModel("2026-03", "career")
        assertEquals("2026-03", vm.state.value.monthKey)
        assertEquals("career", vm.state.value.domain)
    }

    @Test
    fun `updateText updates text and charCount`() {
        val vm = createViewModel()
        vm.updateText("Hello reflection")
        assertEquals("Hello reflection", vm.state.value.text)
        assertEquals(16, vm.state.value.charCount)
    }

    @Test
    fun `updateText truncates at MAX_CHARS`() {
        val vm = createViewModel()
        val longText = "a".repeat(600)
        vm.updateText(longText)
        assertEquals(JournalRepository.MAX_CHARS, vm.state.value.charCount)
    }

    @Test
    fun `updateText clears isSaved flag`() {
        val vm = createViewModel()
        vm.updateText("test")
        assertFalse(vm.state.value.isSaved)
    }

    @Test
    fun `save calls repository and sets isSaved`() = runTest {
        val vm = createViewModel("2026-03", "career")
        vm.updateText("My reflection")
        vm.save()
        assertTrue(vm.state.value.isSaved)
        coVerify { journalRepository.saveEntry("2026-03", "career", "My reflection") }
    }

    @Test
    fun `loads existing entry on init`() = runTest {
        val existing = JournalEntryEntity("j-1", "2026-03", "career", "Previous entry")
        val vm = createViewModel("2026-03", "career", existing = existing, entries = listOf(existing))
        assertEquals("Previous entry", vm.state.value.text)
        assertEquals(1, vm.state.value.existingEntries.size)
    }

    @Test
    fun `an entry opened only to be re-read has no unsaved text`() = runTest {
        val existing = JournalEntryEntity("j-1", "2026-03", "career", "Previous entry")
        val vm = createViewModel("2026-03", "career", existing = existing, entries = listOf(existing))
        assertFalse(vm.state.value.hasUnsavedText)
        vm.updateText("Previous entry, extended")
        assertTrue(vm.state.value.hasUnsavedText)
    }

    @Test
    fun `save leaves no unsaved changes behind`() = runTest {
        val vm = createViewModel("2026-03", "career")
        vm.updateText("My reflection")
        assertTrue(vm.state.value.hasUnsavedText)
        vm.save()
        assertFalse(vm.state.value.hasUnsavedText)
    }

    @Test
    fun `saveAndExit writes before signalling the screen to leave`() = runTest {
        val vm = createViewModel("2026-03", "career")
        vm.updateText("My reflection")
        vm.saveAndExit()
        coVerify { journalRepository.saveEntry("2026-03", "career", "My reflection") }
        assertTrue(vm.state.value.exitRequested)
        assertFalse(vm.state.value.hasUnsavedText)
    }

    @Test
    fun `max chars matches repository constant`() {
        val vm = createViewModel()
        assertEquals(500, vm.state.value.maxChars)
    }
}
