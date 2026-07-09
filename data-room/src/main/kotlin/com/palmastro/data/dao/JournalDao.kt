package com.palmastro.data.dao

import androidx.room.*
import com.palmastro.data.entities.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: JournalEntryEntity)

    @Query("SELECT * FROM journal WHERE monthKey = :monthKey ORDER BY domain ASC")
    suspend fun getByMonth(monthKey: String): List<JournalEntryEntity>

    @Query("SELECT * FROM journal WHERE monthKey = :monthKey AND domain = :domain LIMIT 1")
    suspend fun getByMonthAndDomain(monthKey: String, domain: String): JournalEntryEntity?

    @Query("SELECT * FROM journal WHERE monthKey = :monthKey AND domain IS NULL LIMIT 1")
    suspend fun getGeneralByMonth(monthKey: String): JournalEntryEntity?

    @Query("SELECT * FROM journal ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<JournalEntryEntity>

    @Query("DELETE FROM journal")
    suspend fun deleteAll()

    @Query("DELETE FROM journal WHERE id = :id")
    suspend fun deleteById(id: String)
}
