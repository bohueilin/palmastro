package com.palmastro.app.lib

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.ContentComposer
import com.palmastro.data.entities.MonthlyResultEntity
import com.palmastro.data.repository.ResultRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ContentVersionMigrator(
    private val contentComposer: ContentComposer,
    private val resultRepository: ResultRepository,
    private val currentVersion: String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun migrateIfNeeded(entity: MonthlyResultEntity): MonthlyResultEntity {
        if (entity.contentVersion == currentVersion) return entity

        val domainScores: Map<String, Int> = json.decodeFromString(entity.domainScoresJson)
        val scoringResult = ScoringResult(
            domainScores = domainScores,
            subdimScores = emptyMap(),
            grade = entity.grade,
            confidence = entity.confidenceLevel,
            confidenceReasons = emptyList(),
            explainability = emptyList(),
            matchedBuckets = emptyList(),
            rulesetVersion = entity.rulesetVersion,
        )

        val calcLevel = if (entity.calcLevel == "L2") CalcLevel.L2 else CalcLevel.L1
        val contentInput = ContentInput(
            scoringResult = scoringResult,
            deltaResult = null,
            tone = Tone.SCIENTIFIC,
            entitlements = emptySet(),
            calcLevel = calcLevel,
            monthKey = entity.monthKey,
        )

        val newPayloads = contentComposer.compose(contentInput)
        val payloadsJson = Json.encodeToString<Map<String, SemanticPayload>>(newPayloads)
        val updated = entity.copy(
            semanticPayloadsJson = payloadsJson,
            contentVersion = currentVersion,
        )
        resultRepository.saveResult(updated)
        return updated
    }

    suspend fun migrateAll() {
        val allResults = resultRepository.getRecent(100)
        allResults.filter { it.contentVersion != currentVersion }.forEach { migrateIfNeeded(it) }
    }
}
