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
     *
     * Leaves the month's delta row alone: a caller that computed a delta for this reading
     * may already have written it. Use the [saveResult] overload that takes the delta to
     * replace a reading and its delta together — that is the form a scan should call.
     */
    suspend fun saveResult(result: MonthlyResultEntity) {
        monthlyResultDao.deleteByMonth(result.monthKey)
        monthlyResultDao.insert(result)
    }

    /**
     * Replaces the month's reading AND its delta in one step. The stored delta describes
     * the reading it was computed against, so a replaced reading can never keep the
     * previous scan's arrows: a null [delta] (no comparable previous month) clears the row
     * instead of leaving it behind.
     */
    suspend fun saveResult(result: MonthlyResultEntity, delta: DeltaResult?) {
        saveResult(result)
        if (delta == null) clearDeltaFor(result.monthKey) else saveDelta(result.monthKey, delta)
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

    /**
     * Drops the stored delta for [monthKey]. The scan path calls this when the new reading
     * has no comparable previous month, so no stale arrows survive the rescan.
     */
    suspend fun clearDeltaFor(monthKey: String) = deltaDao.deleteByMonth(monthKey)

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
