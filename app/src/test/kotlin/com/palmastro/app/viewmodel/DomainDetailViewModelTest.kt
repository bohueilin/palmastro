package com.palmastro.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.palmastro.contracts.*
import com.palmastro.data.entities.MonthlyResultEntity
import com.palmastro.data.repository.ResultRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DomainDetailViewModelTest {
    private val resultRepository = mockk<ResultRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun makePayload(domain: String) = SemanticPayload(
        domain = domain, monthKey = "2026-03", calcLevel = CalcLevel.L2, confidence = "high",
        observations = emptyList(), interpretationZh = "測試分析", blindspotZh = "測試盲點",
        actionTodayZh = "今天行動", actionWeekZh = "本週行動", promptZh = "反思問題",
        safetyNotesZh = emptyList(), explainability = emptyList(),
        scoreCard = ScoreCard(72, "Stable", null, null, emptyMap()),
    )

    private fun makeEntity(payloads: Map<String, SemanticPayload>) = MonthlyResultEntity(
        id = "r-1", monthKey = "2026-03", scanSessionId = "s-1", calcLevel = "L2",
        confidenceLevel = "high", confidenceReasonsJson = "[]",
        domainScoresJson = """{"career":72}""", subdimScoresJson = "{}",
        grade = "Stable", semanticPayloadsJson = Json.encodeToString(payloads),
        palmFeatureSummaryJson = "{}", astroSignalsJson = "[]", explainabilityJson = "[]",
        rulesetVersion = "1.0.0", contentVersion = "1.0.0",
        scanQualityScore = 85, featureCoverage = 0.9f,
    )

    @BeforeEach fun setUp() { Dispatchers.setMain(testDispatcher); clearAllMocks() }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loads payload for specified domain`() = runTest {
        val payloads = mapOf("career" to makePayload("career"))
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(payloads)
        val vm = DomainDetailViewModel(SavedStateHandle(mapOf("domain" to "career", "monthKey" to "2026-03")), resultRepository)
        assertNotNull(vm.state.value.payload)
        assertEquals("career", vm.state.value.domain)
        assertEquals("事業", vm.state.value.displayName)
    }

    @Test
    fun `sets error when result not found`() = runTest {
        coEvery { resultRepository.getByMonth("2026-03") } returns null
        val vm = DomainDetailViewModel(SavedStateHandle(mapOf("domain" to "career", "monthKey" to "2026-03")), resultRepository)
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.payload)
    }

    @Test
    fun `sets error when domain not in payloads`() = runTest {
        val payloads = mapOf("career" to makePayload("career"))
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(payloads)
        val vm = DomainDetailViewModel(SavedStateHandle(mapOf("domain" to "wealth", "monthKey" to "2026-03")), resultRepository)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun `payload contains correct interpretation`() = runTest {
        val payloads = mapOf("career" to makePayload("career"))
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(payloads)
        val vm = DomainDetailViewModel(SavedStateHandle(mapOf("domain" to "career", "monthKey" to "2026-03")), resultRepository)
        assertEquals("測試分析", vm.state.value.payload?.interpretationZh)
    }

    @Test
    fun `isLoading starts true and becomes false`() = runTest {
        coEvery { resultRepository.getByMonth("2026-03") } returns null
        val vm = DomainDetailViewModel(SavedStateHandle(mapOf("domain" to "career", "monthKey" to "2026-03")), resultRepository)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `handles malformed payloads JSON gracefully`() = runTest {
        val entity = MonthlyResultEntity(
            id = "r-1", monthKey = "2026-03", scanSessionId = "s-1", calcLevel = "L2",
            confidenceLevel = "high", confidenceReasonsJson = "[]",
            domainScoresJson = """{"career":72}""", subdimScoresJson = "{}",
            grade = "Stable", semanticPayloadsJson = "not json",
            palmFeatureSummaryJson = "{}", astroSignalsJson = "[]", explainabilityJson = "[]",
            rulesetVersion = "1.0.0", contentVersion = "1.0.0",
            scanQualityScore = 85, featureCoverage = 0.9f,
        )
        coEvery { resultRepository.getByMonth("2026-03") } returns entity
        val vm = DomainDetailViewModel(SavedStateHandle(mapOf("domain" to "career", "monthKey" to "2026-03")), resultRepository)
        assertNotNull(vm.state.value.error)
    }
}
