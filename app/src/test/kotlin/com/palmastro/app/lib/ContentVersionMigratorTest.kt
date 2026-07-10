package com.palmastro.app.lib

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.ContentComposer
import com.palmastro.data.entities.MonthlyResultEntity
import com.palmastro.data.repository.ResultRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ContentVersionMigratorTest {
    private val contentComposer = mockk<ContentComposer>()
    private val resultRepository = mockk<ResultRepository>()
    private val currentVersion = "2.0.0"
    private lateinit var migrator: ContentVersionMigrator

    private fun makeEntity(contentVersion: String = "1.0.0", grade: String = "Stable") = MonthlyResultEntity(
        id = "r-001", monthKey = "2026-03", scanSessionId = "s-001", calcLevel = "L2",
        confidenceLevel = "high", confidenceReasonsJson = "[]",
        domainScoresJson = """{"career":72,"wealth":58,"family":65,"health":55}""",
        subdimScoresJson = "{}", grade = grade, semanticPayloadsJson = "{}",
        palmFeatureSummaryJson = "{}", astroSignalsJson = "[]", explainabilityJson = "[]",
        rulesetVersion = "1.0.0", contentVersion = contentVersion,
        scanQualityScore = 85, featureCoverage = 0.9f,
    )

    private fun makePayloads() = mapOf("career" to SemanticPayload(
        domain = "career", monthKey = "2026-03", calcLevel = CalcLevel.L2, confidence = "high",
        observations = emptyList(), interpretation = Interpretation("migrated"), blindspot = "", actionToday = "",
        actionWeek = "", prompt = "", safetyNotes = emptyList(), explainability = emptyList(),
        scoreCard = ScoreCard(72, "Stable", null, null, emptyMap()),
    ))

    @BeforeEach fun setUp() { clearAllMocks(); migrator = ContentVersionMigrator(contentComposer, resultRepository, currentVersion) }

    @Test fun `entity with current version is not modified`() = runTest {
        val entity = makeEntity(contentVersion = currentVersion)
        assertSame(entity, migrator.migrateIfNeeded(entity))
        verify(exactly = 0) { contentComposer.compose(any()) }
    }

    @Test fun `entity with old version gets re-composed`() = runTest {
        every { contentComposer.compose(any()) } returns makePayloads()
        coEvery { resultRepository.saveResult(any()) } just Runs
        val result = migrator.migrateIfNeeded(makeEntity("1.0.0"))
        assertEquals(currentVersion, result.contentVersion)
        verify(exactly = 1) { contentComposer.compose(any()) }
    }

    @Test fun `migrateAll processes only outdated entries`() = runTest {
        coEvery { resultRepository.getRecent(100) } returns listOf(makeEntity(currentVersion), makeEntity("1.0.0").copy(id = "r-002"))
        every { contentComposer.compose(any()) } returns makePayloads()
        coEvery { resultRepository.saveResult(any()) } just Runs
        migrator.migrateAll()
        coVerify(exactly = 1) { resultRepository.saveResult(any()) }
    }

    @Test fun `migration preserves scoring data`() = runTest {
        every { contentComposer.compose(any()) } returns makePayloads()
        coEvery { resultRepository.saveResult(any()) } just Runs
        val result = migrator.migrateIfNeeded(makeEntity("1.0.0", grade = "Growing"))
        assertEquals("Growing", result.grade)
        assertEquals("1.0.0", result.rulesetVersion)
    }
}
