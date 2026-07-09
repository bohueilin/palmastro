package com.palmastro.app.viewmodel

import com.palmastro.data.entities.MonthlyResultEntity
import com.palmastro.data.repository.ResultRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val resultRepository = mockk<ResultRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun makeEntity(monthKey: String, grade: String = "Stable") = MonthlyResultEntity(
        id = "r-$monthKey", monthKey = monthKey, scanSessionId = "s-1", calcLevel = "L2",
        confidenceLevel = "high", confidenceReasonsJson = "[]",
        domainScoresJson = """{"career":72,"wealth":58,"family":65,"health":55}""",
        subdimScoresJson = "{}", grade = grade, semanticPayloadsJson = "{}",
        palmFeatureSummaryJson = "{}", astroSignalsJson = "[]", explainabilityJson = "[]",
        rulesetVersion = "1.0.0", contentVersion = "1.0.0",
        scanQualityScore = 85, featureCoverage = 0.9f,
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state is loading`() = runTest {
        coEvery { resultRepository.observeAll() } returns flowOf(emptyList())
        val vm = HistoryViewModel(resultRepository)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `empty results produces empty months`() = runTest {
        coEvery { resultRepository.observeAll() } returns flowOf(emptyList())
        val vm = HistoryViewModel(resultRepository)
        assertTrue(vm.state.value.months.isEmpty())
    }

    @Test
    fun `multiple results populate months list`() = runTest {
        val entities = listOf(makeEntity("2026-03"), makeEntity("2026-02"))
        coEvery { resultRepository.observeAll() } returns flowOf(entities)
        val vm = HistoryViewModel(resultRepository)
        assertEquals(2, vm.state.value.months.size)
        assertEquals("2026-03", vm.state.value.months[0].monthKey)
    }

    @Test
    fun `month summaries contain parsed domain scores`() = runTest {
        coEvery { resultRepository.observeAll() } returns flowOf(listOf(makeEntity("2026-03")))
        val vm = HistoryViewModel(resultRepository)
        val scores = vm.state.value.months[0].domainScores
        assertEquals(72, scores["career"])
        assertEquals(58, scores["wealth"])
    }

    @Test
    fun `malformed JSON produces empty scores`() = runTest {
        val bad = makeEntity("2026-03").copy(domainScoresJson = "broken")
        coEvery { resultRepository.observeAll() } returns flowOf(listOf(bad))
        val vm = HistoryViewModel(resultRepository)
        assertTrue(vm.state.value.months[0].domainScores.isEmpty())
    }
}
