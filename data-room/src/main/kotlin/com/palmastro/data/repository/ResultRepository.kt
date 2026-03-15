package com.palmastro.data.repository

import com.palmastro.data.dao.DeltaDao
import com.palmastro.data.dao.MonthlyResultDao
import com.palmastro.data.entities.DeltaEntity
import com.palmastro.data.entities.MonthlyResultEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResultRepository @Inject constructor(
    private val monthlyResultDao: MonthlyResultDao,
    private val deltaDao: DeltaDao,
) {
    suspend fun saveResult(result: MonthlyResultEntity) = monthlyResultDao.insert(result)
    suspend fun getByMonth(monthKey: String): MonthlyResultEntity? = monthlyResultDao.getByMonth(monthKey)
    fun observeAll(): Flow<List<MonthlyResultEntity>> = monthlyResultDao.observeAll()
    suspend fun getRecent(limit: Int): List<MonthlyResultEntity> = monthlyResultDao.getRecent(limit)

    suspend fun saveDelta(delta: DeltaEntity) = deltaDao.insert(delta)
    suspend fun getDelta(monthKey: String): DeltaEntity? = deltaDao.getByMonth(monthKey)
}
