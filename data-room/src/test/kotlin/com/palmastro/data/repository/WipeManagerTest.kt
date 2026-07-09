package com.palmastro.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.palmastro.data.dao.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WipeManagerTest {
    @TempDir lateinit var filesDir: File
    @TempDir lateinit var cacheDir: File

    private val context = mockk<Context>()
    private val userProfileDao = mockk<UserProfileDao>(relaxed = true)
    private val monthlyResultDao = mockk<MonthlyResultDao>(relaxed = true)
    private val deltaDao = mockk<DeltaDao>(relaxed = true)
    private val entitlementDao = mockk<EntitlementDao>(relaxed = true)
    private val installIdDao = mockk<InstallIdDao>(relaxed = true)
    private val prefs = mockk<SharedPreferences>()
    private val prefsEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    private lateinit var wipeManager: WipeManager

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { context.filesDir } returns filesDir
        every { context.cacheDir } returns cacheDir
        every { context.applicationInfo } returns mockk { every { dataDir } returns filesDir.absolutePath }
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns prefsEditor
        every { prefsEditor.clear() } returns prefsEditor
        wipeManager = WipeManager(context, userProfileDao, monthlyResultDao, deltaDao, entitlementDao, installIdDao)
    }

    @Test
    fun `deleteAllData clears all DAOs`() = runTest {
        wipeManager.deleteAllData()
        coVerify {
            userProfileDao.deleteAll()
            monthlyResultDao.deleteAll()
            deltaDao.deleteAll()
            entitlementDao.deleteAll()
            installIdDao.deleteAll()
        }
    }

    @Test
    fun `deleteAllData deletes scan images`() = runTest {
        val scansDir = File(filesDir, "scans").apply { mkdirs() }
        File(scansDir, "test.jpg").writeText("test")
        wipeManager.deleteAllData()
        assertFalse(scansDir.exists())
    }

    @Test
    fun `deleteAllData clears share cache`() = runTest {
        val shareDir = File(cacheDir, "share").apply { mkdirs() }
        File(shareDir, "share.png").writeText("test")
        wipeManager.deleteAllData()
        assertFalse(shareDir.exists())
    }

    @Test
    fun `deleteAllData clears SharedPreferences`() = runTest {
        wipeManager.deleteAllData()
        verify(atLeast = 1) { context.getSharedPreferences(any(), any()) }
        verify(atLeast = 1) { prefsEditor.clear() }
        verify(atLeast = 1) { prefsEditor.apply() }
    }
}
