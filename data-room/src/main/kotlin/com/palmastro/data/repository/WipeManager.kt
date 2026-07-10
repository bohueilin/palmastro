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
        deleteShareAuditLog()
        clearWipeableSharedPreferences()
        // PRD §28/§53: rotate the install id so post-wipe activity is unlinkable.
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

    private fun deleteShareAuditLog() {
        val auditLog = File(appContext.filesDir, SHARE_AUDIT_LOG_NAME)
        if (auditLog.exists()) auditLog.delete()
    }

    private fun clearWipeableSharedPreferences() {
        // DB_KEY_PREFS_NAME is intentionally PRESERVED: it holds the Keystore-wrapped
        // SQLCipher key for the still-open encrypted database. Clearing the tables IS
        // the wipe; destroying the key would brick the open DB and the app with it.
        val prefsToWipe = listOf("palmastro_feature_flags")
        for (prefsName in prefsToWipe) {
            appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().apply()
        }
        val prefsDir = File(appContext.applicationInfo.dataDir, "shared_prefs")
        if (prefsDir.exists()) {
            prefsDir.listFiles()
                ?.filter { it.name.startsWith("palmastro_") && it.name != "$DB_KEY_PREFS_NAME.xml" }
                ?.forEach { it.delete() }
        }
    }

    companion object {
        const val DB_KEY_PREFS_NAME = "palmastro_db_prefs"
        const val SHARE_AUDIT_LOG_NAME = "share_audit.log"
    }
}
