package com.palmastro.data.repository

import com.palmastro.contracts.ComparabilityBucket
import com.palmastro.contracts.DeltaResult
import com.palmastro.contracts.DeltaValue
import com.palmastro.contracts.GradeShift
import com.palmastro.data.dao.DeltaDao
import com.palmastro.data.dao.MonthlyResultDao
import com.palmastro.data.entities.DeltaEntity
import com.palmastro.data.entities.MonthlyResultEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResultRepository @Inject constructor(
    private val monthlyResultDao: MonthlyResultDao,
    private val deltaDao: DeltaDao,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val deltaMapSerializer = MapSerializer(String.serializer(), DeltaValue.serializer())

    /**
     * Persists [result], REPLACING any earlier scan stored for the same month. The entity
     * PK is a fresh UUID per scan, so without the delete a same-month rescan would leave
     * two rows behind (duplicate History entries, bogus intra-month delta). Mirrors the
     * delete-then-insert pattern of [saveDelta].
     */
    suspend fun saveResult(result: MonthlyResultEntity) {
        monthlyResultDao.deleteByMonth(result.monthKey)
        monthlyResultDao.insert(result)
    }

    suspend fun getByMonth(monthKey: String): MonthlyResultEntity? = monthlyResultDao.getByMonth(monthKey)
    fun observeAll(): Flow<List<MonthlyResultEntity>> = monthlyResultDao.observeAll()
    suspend fun getRecent(limit: Int): List<MonthlyResultEntity> = monthlyResultDao.getRecent(limit)

    /** Persists the delta for [monthKey], replacing any previously stored delta for that month. */
    suspend fun saveDelta(monthKey: String, delta: DeltaResult) {
        deltaDao.deleteByMonth(monthKey)
        deltaDao.insert(delta.toEntity(monthKey))
    }

    suspend fun getDeltaFor(monthKey: String): DeltaResult? =
        deltaDao.getByMonth(monthKey)?.toDeltaResult()

    private fun DeltaResult.toEntity(monthKey: String) = DeltaEntity(
        currentMonthKey = monthKey,
        prevMonthKey = prevMonthKey,
        domainDeltasJson = json.encodeToString(deltaMapSerializer, domainDeltas),
        subdimDeltasJson = json.encodeToString(deltaMapSerializer, subdimDeltas),
        gradeShift = gradeShift?.let { json.encodeToString(GradeShift.serializer(), it) },
        comparabilityScore = comparabilityScore,
        comparabilityBucket = comparabilityBucket.name,
    )

    private fun DeltaEntity.toDeltaResult() = DeltaResult(
        domainDeltas = json.decodeFromString(deltaMapSerializer, domainDeltasJson),
        subdimDeltas = json.decodeFromString(deltaMapSerializer, subdimDeltasJson),
        gradeShift = gradeShift?.let { json.decodeFromString(GradeShift.serializer(), it) },
        comparabilityScore = comparabilityScore,
        comparabilityBucket = ComparabilityBucket.valueOf(comparabilityBucket),
        prevMonthKey = prevMonthKey,
        currentMonthKey = currentMonthKey,
    )
}
