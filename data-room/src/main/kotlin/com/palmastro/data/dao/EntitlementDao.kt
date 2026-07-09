package com.palmastro.data.dao

import androidx.room.*
import com.palmastro.data.entities.EntitlementEntity

@Dao
interface EntitlementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entitlement: EntitlementEntity)

    @Query("SELECT * FROM entitlement WHERE productId = :productId")
    suspend fun getByProductId(productId: String): EntitlementEntity?

    @Query("SELECT * FROM entitlement WHERE isOwned = 1")
    suspend fun getOwned(): List<EntitlementEntity>

    @Query("DELETE FROM entitlement")
    suspend fun deleteAll()
}
