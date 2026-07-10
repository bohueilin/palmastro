package com.palmastro.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
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
    private val journalDao = mockk<JournalDao>(relaxed = true)
    private val prefs = mockk<SharedPreferences>()
    private val prefsEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    private lateinit var wipeManager: WipeManager

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { context.filesDir } returns filesDir
        every { context.cacheDir } returns cacheDir
        // ApplicationInfo.dataDir is a plain field, so it is assigned directly
        // (stubbing a field access with `every` fails at runtime).
        val applicationInfo = mockk<ApplicationInfo>()
        applicationInfo.dataDir = filesDir.absolutePath
        every { context.applicationInfo } returns applicationInfo
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns prefsEditor
        every { prefsEditor.clear() } returns prefsEditor
        wipeManager = WipeManager(context, userProfileDao, monthlyResultDao, deltaDao, entitlementDao, installIdDao, journalDao)
    }

    @Test
    fun `deleteAllData clears all DAOs`() = runTest {
        wipeManager.deleteAllData()
        coVerify {
            userProfileDao.deleteAll()
            monthlyResultDao.deleteAll()
            deltaDao.deleteAll()
            entitlementDao.deleteAll()
            journalDao.deleteAll()
            installIdDao.deleteAll()
        }
    }

    @Test
    fun `deleteAllData rotates the install id`() = runTest {
        wipeManager.deleteAllData()
        coVerifyOrder {
            installIdDao.deleteAll()
            installIdDao.upsert(match { it.installId.isNotBlank() })
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
    fun `deleteAllData deletes the share audit log`() = runTest {
        val auditLog = File(filesDir, WipeManager.SHARE_AUDIT_LOG_NAME).apply { writeText("shared card") }
        wipeManager.deleteAllData()
        assertFalse(auditLog.exists())
    }

    @Test
    fun `deleteAllData clears SharedPreferences`() = runTest {
        wipeManager.deleteAllData()
        verify(atLeast = 1) { context.getSharedPreferences(any(), any()) }
        verify(atLeast = 1) { prefsEditor.clear() }
        verify(atLeast = 1) { prefsEditor.apply() }
    }

    @Test
    fun `deleteAllData never clears the database key prefs`() = runTest {
        wipeManager.deleteAllData()
        verify(exactly = 0) { context.getSharedPreferences(WipeManager.DB_KEY_PREFS_NAME, any()) }
    }

    @Test
    fun `deleteAllData preserves the database key prefs file but removes other app prefs files`() = runTest {
        val prefsDir = File(filesDir, "shared_prefs").apply { mkdirs() }
        val dbKeyPrefs = File(prefsDir, "${WipeManager.DB_KEY_PREFS_NAME}.xml").apply { writeText("<map/>") }
        val flagPrefs = File(prefsDir, "palmastro_feature_flags.xml").apply { writeText("<map/>") }
        val otherPrefs = File(prefsDir, "palmastro_misc.xml").apply { writeText("<map/>") }

        wipeManager.deleteAllData()

        assertTrue(dbKeyPrefs.exists(), "DB key prefs must survive wipe so the open SQLCipher DB stays usable")
        assertFalse(flagPrefs.exists())
        assertFalse(otherPrefs.exists())
    }

    @Test
    fun `deleteAllScanImages clears images and stored paths`() = runTest {
        val scansDir = File(filesDir, "scans").apply { mkdirs() }
        File(scansDir, "2026-07").apply { mkdirs() }
        wipeManager.deleteAllScanImages()
        assertFalse(scansDir.exists())
        coVerify { monthlyResultDao.clearAllScanImagePaths() }
    }
}
