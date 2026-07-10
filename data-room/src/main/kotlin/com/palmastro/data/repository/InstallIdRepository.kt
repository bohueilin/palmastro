package com.palmastro.data.repository

import com.palmastro.data.dao.InstallIdDao
import com.palmastro.data.entities.InstallIdEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the anonymous install identifier (PRD §52/§53): created lazily on first
 * app start and rotated by [WipeManager] on delete-all-data.
 */
@Singleton
class InstallIdRepository @Inject constructor(
    private val installIdDao: InstallIdDao,
) {
    suspend fun getOrCreate(): String {
        installIdDao.get()?.let { return it.installId }
        val created = InstallIdEntity(installId = UUID.randomUUID().toString())
        installIdDao.upsert(created)
        return created.installId
    }
}
