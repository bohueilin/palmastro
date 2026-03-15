package com.palmastro.data.dao

import androidx.room.*
import com.palmastro.data.entities.InstallIdEntity

@Dao
interface InstallIdDao {
    @Query("SELECT * FROM install_id WHERE id = 1")
    suspend fun get(): InstallIdEntity?

    @Upsert
    suspend fun upsert(entity: InstallIdEntity)

    @Query("DELETE FROM install_id")
    suspend fun deleteAll()
}
