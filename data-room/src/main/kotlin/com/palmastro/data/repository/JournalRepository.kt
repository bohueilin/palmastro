package com.palmastro.data.repository

import com.palmastro.data.dao.JournalDao
import com.palmastro.data.entities.JournalEntryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepository @Inject constructor(
    private val journalDao: JournalDao,
) {
    companion object {
        const val MAX_CHARS = 500
    }

    suspend fun saveEntry(monthKey: String, domain: String?, text: String) {
        val sanitized = text.take(MAX_CHARS).trim()
        if (sanitized.isBlank()) return

        val existing = if (domain != null) {
            journalDao.getByMonthAndDomain(monthKey, domain)
        } else {
            journalDao.getGeneralByMonth(monthKey)
        }

        val entry = existing?.copy(
            text = sanitized,
            updatedAt = System.currentTimeMillis(),
        ) ?: JournalEntryEntity(
            id = UUID.randomUUID().toString(),
            monthKey = monthKey,
            domain = domain,
            text = sanitized,
        )
        journalDao.upsert(entry)
    }

    suspend fun getByMonth(monthKey: String): List<JournalEntryEntity> =
        journalDao.getByMonth(monthKey)

    suspend fun getByMonthAndDomain(monthKey: String, domain: String): JournalEntryEntity? =
        journalDao.getByMonthAndDomain(monthKey, domain)

    fun observeAll(): Flow<List<JournalEntryEntity>> =
        journalDao.observeAll()

    suspend fun deleteEntry(id: String) =
        journalDao.deleteById(id)
}
