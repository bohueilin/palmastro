package com.palmastro.data.dao

import androidx.room.*
import com.palmastro.data.entities.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Upsert
    suspend fun upsert(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}
