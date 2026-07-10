package com.palmastro.data.repository

import com.palmastro.data.dao.InstallIdDao
import com.palmastro.data.entities.InstallIdEntity
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstallIdRepositoryTest {
    private val installIdDao = mockk<InstallIdDao>(relaxed = true)
    private lateinit var repo: InstallIdRepository

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        repo = InstallIdRepository(installIdDao)
    }

    @Test
    fun `getOrCreate returns the existing id without writing`() = runTest {
        coEvery { installIdDao.get() } returns InstallIdEntity(installId = "existing-id")
        assertEquals("existing-id", repo.getOrCreate())
        coVerify(exactly = 0) { installIdDao.upsert(any()) }
    }

    @Test
    fun `getOrCreate creates and persists a new id when missing`() = runTest {
        coEvery { installIdDao.get() } returns null
        val id = repo.getOrCreate()
        assertTrue(id.isNotBlank())
        coVerify { installIdDao.upsert(match { it.installId == id }) }
    }
}
