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
    private val installIdDao: InstallIdDao,
) {
    suspend fun deleteAllData() {
        userProfileDao.deleteAll()
        monthlyResultDao.deleteAll()
        deltaDao.deleteAll()
        installIdDao.deleteAll()
        deleteScanImages()
        // Regenerate install_id
        installIdDao.upsert(InstallIdEntity(installId = UUID.randomUUID().toString()))
    }

    suspend fun deleteAllScanImages() {
        deleteScanImages()
        monthlyResultDao.clearAllScanImagePaths()
    }

    private fun deleteScanImages() {
        val scansDir = File(appContext.filesDir, "scans")
        if (scansDir.exists()) {
            scansDir.deleteRecursively()
        }
    }
}
