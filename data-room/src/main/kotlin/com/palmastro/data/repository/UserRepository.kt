package com.palmastro.data.repository

import com.palmastro.data.dao.UserProfileDao
import com.palmastro.data.entities.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val dao: UserProfileDao
) {
    fun observe(): Flow<UserProfileEntity?> = dao.observe()
    suspend fun get(): UserProfileEntity? = dao.get()
    suspend fun save(profile: UserProfileEntity) = dao.upsert(profile)
    suspend fun exists(): Boolean = dao.get() != null
}
