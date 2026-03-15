package com.palmastro.data.repository

import com.palmastro.data.dao.*
import com.palmastro.data.entities.InstallIdEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WipeManager @Inject constructor(
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
        // Regenerate install_id
        installIdDao.upsert(InstallIdEntity(installId = UUID.randomUUID().toString()))
    }
}
