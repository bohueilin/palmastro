package com.palmastro.data.dao

import androidx.room.*
import com.palmastro.data.entities.DeltaEntity

@Dao
interface DeltaDao {
    @Insert
    suspend fun insert(delta: DeltaEntity)

    @Query("SELECT * FROM delta WHERE currentMonthKey = :monthKey")
    suspend fun getByMonth(monthKey: String): DeltaEntity?

    @Query("DELETE FROM delta")
    suspend fun deleteAll()
}
