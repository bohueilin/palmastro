package com.palmastro.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.palmastro.app.config.FeatureFlags
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DomainDetailViewModelTest {
    private val resultRepository = mockk<ResultRepository>()
    private val featureFlags = mockk<FeatureFlags>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun makePayload(
        domain: String,
        explainability: List<ExplainEntry> = emptyList(),
    ) = SemanticPayload(
        domain = domain, monthKey = "2026-03", calcLevel = CalcLevel.L2, confidence = "high",
        confidenceReasons = listOf("full scan coverage"),
        observations = emptyList(), interpretation = Interpretation("測試分析", "測試觸發", "測試代價"),
        blindspot = "測試盲點",
        actionToday = "今天行動", actionWeek = "本週行動", prompt = "反思問題",
        safetyNotes = emptyList(), explainability = explainability,
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

    @BeforeEach fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()
        every { featureFlags.shareCardsEnabled } returns true
    }

    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    private fun createViewModel(domain: String = "career") = DomainDetailViewModel(
        SavedStateHandle(mapOf("domain" to domain, "monthKey" to "2026-03")),
        resultRepository,
        featureFlags,
    )

    @Test
    fun `loads payload for specified domain`() = runTest {
        val payloads = mapOf("career" to makePayload("career"))
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(payloads)
        val vm = createViewModel()
        assertNotNull(vm.state.value.payload)
        assertEquals("career", vm.state.value.domain)
    }

    @Test
    fun `sets error when result not found`() = runTest {
        coEvery { resultRepository.getByMonth("2026-03") } returns null
        val vm = createViewModel()
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.payload)
    }

    @Test
    fun `sets error when domain not in payloads`() = runTest {
        val payloads = mapOf("career" to makePayload("career"))
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(payloads)
        val vm = createViewModel(domain = "wealth")
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun `payload contains interpretation pattern trigger and cost`() = runTest {
        val payloads = mapOf("career" to makePayload("career"))
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(payloads)
        val vm = createViewModel()
        assertEquals("測試分析", vm.state.value.payload?.interpretation?.pattern)
        assertEquals("測試觸發", vm.state.value.payload?.interpretation?.trigger)
        assertEquals("測試代價", vm.state.value.payload?.interpretation?.cost)
    }

    @Test
    fun `payload exposes explainability entries for the explainability screen`() = runTest {
        val explainability = listOf(
            ExplainEntry("PALM_HEADLINE_LONG_CLEAR", "PALM_HEADLINE_LONG_CLEAR → career", 3.5),
            ExplainEntry("ASTRO_SUN_FIRE", "ASTRO_SUN_FIRE → career", -1.2),
        )
        val payloads = mapOf("career" to makePayload("career", explainability = explainability))
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(payloads)
        val vm = createViewModel()
        val loaded = vm.state.value.payload?.explainability
        assertNotNull(loaded)
        assertEquals(2, loaded.size)
        assertEquals("PALM_HEADLINE_LONG_CLEAR", loaded[0].signalId)
        assertEquals(3.5, loaded[0].contribution)
        assertTrue(vm.state.value.payload!!.confidenceReasons.isNotEmpty())
    }

    @Test
    fun `quality factors are propagated from the entity`() = runTest {
        val payloads = mapOf("career" to makePayload("career"))
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(payloads)
        val vm = createViewModel()
        assertEquals(85, vm.state.value.scanQualityScore)
        assertEquals(0.9f, vm.state.value.featureCoverage)
    }

    @Test
    fun `share cards flag is read from feature flags`() = runTest {
        every { featureFlags.shareCardsEnabled } returns false
        val payloads = mapOf("career" to makePayload("career"))
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(payloads)
        val vm = createViewModel()
        assertFalse(vm.state.value.shareCardsEnabled)
    }

    @Test
    fun `isLoading starts true and becomes false`() = runTest {
        coEvery { resultRepository.getByMonth("2026-03") } returns null
        val vm = createViewModel()
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
        val vm = createViewModel()
        assertNotNull(vm.state.value.error)
    }
}
