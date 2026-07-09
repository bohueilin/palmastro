package com.palmastro.data.repository

import com.palmastro.data.dao.JournalDao
import com.palmastro.data.entities.JournalEntryEntity
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JournalRepositoryTest {
    private val journalDao = mockk<JournalDao>(relaxed = true)
    private lateinit var repo: JournalRepository

    @BeforeEach fun setUp() { clearAllMocks(); repo = JournalRepository(journalDao) }

    @Test
    fun `saveEntry creates new entry when none exists`() = runTest {
        coEvery { journalDao.getByMonthAndDomain("2026-03", "career") } returns null
        repo.saveEntry("2026-03", "career", "My reflection")
        coVerify { journalDao.upsert(match { it.text == "My reflection" && it.domain == "career" }) }
    }

    @Test
    fun `saveEntry updates existing entry`() = runTest {
        val existing = JournalEntryEntity("j-1", "2026-03", "career", "Old text")
        coEvery { journalDao.getByMonthAndDomain("2026-03", "career") } returns existing
        repo.saveEntry("2026-03", "career", "New text")
        coVerify { journalDao.upsert(match { it.id == "j-1" && it.text == "New text" }) }
    }

    @Test
    fun `saveEntry truncates text at MAX_CHARS`() = runTest {
        coEvery { journalDao.getByMonthAndDomain(any(), any()) } returns null
        repo.saveEntry("2026-03", "career", "a".repeat(600))
        coVerify { journalDao.upsert(match { it.text.length == JournalRepository.MAX_CHARS }) }
    }

    @Test
    fun `saveEntry ignores blank text`() = runTest {
        repo.saveEntry("2026-03", "career", "   ")
        coVerify(exactly = 0) { journalDao.upsert(any()) }
    }

    @Test
    fun `MAX_CHARS is 500`() {
        assertEquals(500, JournalRepository.MAX_CHARS)
    }
}
