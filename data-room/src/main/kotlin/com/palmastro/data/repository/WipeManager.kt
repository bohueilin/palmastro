package com.palmastro.data.repository

import android.content.Context
import com.palmastro.data.dao.*
import com.palmastro.data.entities.InstallIdEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WipeManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userProfileDao: UserProfileDao,
    private val monthlyResultDao: MonthlyResultDao,
    private val deltaDao: DeltaDao,
    private val entitlementDao: EntitlementDao,
    private val installIdDao: InstallIdDao,
    private val journalDao: JournalDao,
) {
    suspend fun deleteAllData() {
        userProfileDao.deleteAll()
        monthlyResultDao.deleteAll()
        deltaDao.deleteAll()
        entitlementDao.deleteAll()
        journalDao.deleteAll()
        installIdDao.deleteAll()
        deleteScanImages()
        deleteShareCache()
        clearAllSharedPreferences()
        installIdDao.upsert(InstallIdEntity(installId = UUID.randomUUID().toString()))
    }

    suspend fun deleteAllScanImages() {
        deleteScanImages()
        monthlyResultDao.clearAllScanImagePaths()
    }

    private fun deleteScanImages() {
        val scansDir = File(appContext.filesDir, "scans")
        if (scansDir.exists()) scansDir.deleteRecursively()
    }

    private fun deleteShareCache() {
        val shareDir = File(appContext.cacheDir, "share")
        if (shareDir.exists()) shareDir.deleteRecursively()
    }

    private fun clearAllSharedPreferences() {
        val prefsToWipe = listOf("palmastro_feature_flags", "palmastro_db_prefs")
        for (prefsName in prefsToWipe) {
            appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().apply()
        }
        val prefsDir = File(appContext.applicationInfo.dataDir, "shared_prefs")
        if (prefsDir.exists()) {
            prefsDir.listFiles()?.filter { it.name.startsWith("palmastro_") }?.forEach { it.delete() }
        }
    }
}
