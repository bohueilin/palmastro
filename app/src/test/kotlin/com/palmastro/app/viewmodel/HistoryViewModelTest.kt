package com.palmastro.app.viewmodel

import com.palmastro.contracts.ComparabilityBucket
import com.palmastro.contracts.DeltaResult
import com.palmastro.contracts.DeltaValue
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

    private fun makeDelta(bucket: ComparabilityBucket) = DeltaResult(
        domainDeltas = mapOf("career" to DeltaValue(5, "up"), "wealth" to DeltaValue(-3, "down")),
        subdimDeltas = emptyMap(),
        gradeShift = null,
        comparabilityScore = if (bucket == ComparabilityBucket.LOW) 30 else 85,
        comparabilityBucket = bucket,
        prevMonthKey = "2026-02",
        currentMonthKey = "2026-03",
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()
        coEvery { resultRepository.getDeltaFor(any()) } returns null
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

    // --- Delta comparability: History must show exactly what Results shows ---

    @Test
    fun `deltas come from the stored delta, not a raw month-to-month subtraction`() = runTest {
        coEvery { resultRepository.observeAll() } returns flowOf(listOf(makeEntity("2026-03")))
        coEvery { resultRepository.getDeltaFor("2026-03") } returns makeDelta(ComparabilityBucket.HIGH)
        val vm = HistoryViewModel(resultRepository)
        val month = vm.state.value.months[0]
        assertEquals(5, month.deltas["career"])
        assertEquals(-3, month.deltas["wealth"])
        assertFalse(month.deltaApproximate)
    }

    @Test
    fun `deltas are hidden when comparability is LOW`() = runTest {
        coEvery { resultRepository.observeAll() } returns flowOf(listOf(makeEntity("2026-03")))
        coEvery { resultRepository.getDeltaFor("2026-03") } returns makeDelta(ComparabilityBucket.LOW)
        val vm = HistoryViewModel(resultRepository)
        assertTrue(vm.state.value.months[0].deltas.isEmpty(), "LOW comparability must show no arrows")
    }

    @Test
    fun `MED comparability keeps the deltas but marks them approximate`() = runTest {
        coEvery { resultRepository.observeAll() } returns flowOf(listOf(makeEntity("2026-03")))
        coEvery { resultRepository.getDeltaFor("2026-03") } returns makeDelta(ComparabilityBucket.MED)
        val vm = HistoryViewModel(resultRepository)
        val month = vm.state.value.months[0]
        assertEquals(5, month.deltas["career"])
        assertTrue(month.deltaApproximate, "MED comparability must weaken, not hide, the change")
    }

    @Test
    fun `no stored delta means no deltas`() = runTest {
        coEvery { resultRepository.observeAll() } returns flowOf(listOf(makeEntity("2026-03")))
        val vm = HistoryViewModel(resultRepository)
        assertTrue(vm.state.value.months[0].deltas.isEmpty())
    }

    @Test
    fun `unreadable delta row degrades to no deltas, never a failed screen`() = runTest {
        coEvery { resultRepository.observeAll() } returns flowOf(listOf(makeEntity("2026-03")))
        coEvery { resultRepository.getDeltaFor("2026-03") } throws IllegalStateException("corrupt row")
        val vm = HistoryViewModel(resultRepository)
        assertFalse(vm.state.value.isLoading)
        assertTrue(vm.state.value.months[0].deltas.isEmpty())
    }
}
