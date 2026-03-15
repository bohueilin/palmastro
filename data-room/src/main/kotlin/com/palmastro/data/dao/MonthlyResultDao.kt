package com.palmastro.data.dao

import androidx.room.*
import com.palmastro.data.entities.MonthlyResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: MonthlyResultEntity)

    @Query("SELECT * FROM monthly_result WHERE monthKey = :monthKey")
    suspend fun getByMonth(monthKey: String): MonthlyResultEntity?

    @Query("SELECT * FROM monthly_result ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MonthlyResultEntity>>

    @Query("SELECT * FROM monthly_result ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<MonthlyResultEntity>

    @Query("DELETE FROM monthly_result")
    suspend fun deleteAll()

    @Query("UPDATE monthly_result SET scanImagePath = ''")
    suspend fun clearAllScanImagePaths()
}
