package com.palmastro.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.palmastro.app.config.FeatureFlags
import com.palmastro.contracts.*
import com.palmastro.data.entities.MonthlyResultEntity
import com.palmastro.data.entities.UserProfileEntity
import com.palmastro.data.repository.ResultRepository
import com.palmastro.data.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ResultsViewModelTest {
    private val resultRepository = mockk<ResultRepository>()
    private val userRepository = mockk<UserRepository>()
    private val featureFlags = mockk<FeatureFlags>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun makePayload(domain: String, pattern: String) = SemanticPayload(
        domain = domain, monthKey = "2026-03", calcLevel = CalcLevel.L2, confidence = "med",
        observations = emptyList(), interpretation = Interpretation(pattern), blindspot = "b",
        actionToday = "t", actionWeek = "w", prompt = "p",
        safetyNotes = emptyList(), explainability = emptyList(),
        scoreCard = ScoreCard(72, "Stable", null, null, emptyMap()),
    )

    private fun makeEntity(
        monthKey: String = "2026-03",
        grade: String = "Stable",
        payloads: Map<String, SemanticPayload> = emptyMap(),
    ) = MonthlyResultEntity(
        id = "r-1", monthKey = monthKey, scanSessionId = "s-1", calcLevel = "L2",
        confidenceLevel = "high", confidenceReasonsJson = "[]",
        domainScoresJson = """{"career":72,"wealth":58,"family":65,"health":55}""",
        subdimScoresJson = "{}", grade = grade,
        semanticPayloadsJson = if (payloads.isEmpty()) "{}" else Json.encodeToString(payloads),
        palmFeatureSummaryJson = "{}", astroSignalsJson = "[]", explainabilityJson = "[]",
        rulesetVersion = "1.0.0", contentVersion = "1.0.0",
        scanQualityScore = 85, featureCoverage = 0.9f,
    )

    private fun makeProfile() = UserProfileEntity(
        dominantHand = "right", birthdayEpochDay = 7384, tone = "scientific",
    )

    private fun makeDelta(bucket: ComparabilityBucket) = DeltaResult(
        domainDeltas = mapOf(
            "career" to DeltaValue(5, "up"),
            "wealth" to DeltaValue(-3, "down"),
            "family" to DeltaValue(0, "flat"),
            "health" to DeltaValue(2, "up"),
        ),
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
        every { featureFlags.shareCardsEnabled } returns true
        coEvery { resultRepository.getDeltaFor(any()) } returns null
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun createViewModel(handle: SavedStateHandle = SavedStateHandle()) =
        ResultsViewModel(handle, resultRepository, userRepository, featureFlags)

    @Test
    fun `load sets hasResults false when no results`() = runTest {
        coEvery { resultRepository.getRecent(1) } returns emptyList()
        coEvery { userRepository.get() } returns makeProfile()
        val vm = createViewModel()
        assertFalse(vm.state.value.hasResults)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `load populates domain cards from latest result`() = runTest {
        coEvery { resultRepository.getRecent(1) } returns listOf(makeEntity())
        coEvery { userRepository.get() } returns makeProfile()
        val vm = createViewModel()
        assertTrue(vm.state.value.hasResults)
        assertEquals(4, vm.state.value.domainCards.size)
        assertEquals("Stable", vm.state.value.grade)
        assertEquals("2026-03", vm.state.value.monthKey)
        assertEquals(85, vm.state.value.scanQualityScore)
        assertEquals("career", vm.state.value.topDomain)
    }

    @Test
    fun `delta shown on cards when comparability is HIGH`() = runTest {
        coEvery { resultRepository.getRecent(1) } returns listOf(makeEntity())
        coEvery { userRepository.get() } returns makeProfile()
        coEvery { resultRepository.getDeltaFor("2026-03") } returns makeDelta(ComparabilityBucket.HIGH)
        val vm = createViewModel()
        val career = vm.state.value.domainCards.first { it.domain == "career" }
        assertEquals(5, career.delta)
        assertEquals("up", career.deltaArrow)
        val wealth = vm.state.value.domainCards.first { it.domain == "wealth" }
        assertEquals(-3, wealth.delta)
        assertEquals("down", wealth.deltaArrow)
    }

    @Test
    fun `delta hidden when comparability is LOW`() = runTest {
        coEvery { resultRepository.getRecent(1) } returns listOf(makeEntity())
        coEvery { userRepository.get() } returns makeProfile()
        coEvery { resultRepository.getDeltaFor("2026-03") } returns makeDelta(ComparabilityBucket.LOW)
        val vm = createViewModel()
        vm.state.value.domainCards.forEach { card ->
            assertNull(card.delta, "delta for ${card.domain} must be gated off at LOW comparability")
            assertNull(card.deltaArrow)
        }
    }

    @Test
    fun `delta hidden when no delta record exists`() = runTest {
        coEvery { resultRepository.getRecent(1) } returns listOf(makeEntity())
        coEvery { userRepository.get() } returns makeProfile()
        coEvery { resultRepository.getDeltaFor("2026-03") } returns null
        val vm = createViewModel()
        vm.state.value.domainCards.forEach { card ->
            assertNull(card.delta)
        }
    }

    @Test
    fun `insight is first sentence of interpretation pattern`() = runTest {
        val payloads = mapOf("career" to makePayload("career", "First insight. Second sentence continues."))
        coEvery { resultRepository.getRecent(1) } returns listOf(makeEntity(payloads = payloads))
        coEvery { userRepository.get() } returns makeProfile()
        val vm = createViewModel()
        val career = vm.state.value.domainCards.first { it.domain == "career" }
        assertEquals("First insight.", career.insight)
        assertEquals("med", career.confidence)
    }

    @Test
    fun `firstSentence handles CJK punctuation and missing terminator`() {
        assertEquals("這個月很穩定。", ResultsViewModel.firstSentence("這個月很穩定。後面還有更多。"))
        assertEquals("no terminator at all", ResultsViewModel.firstSentence("no terminator at all"))
    }

    @Test
    fun `loads specific month when monthKey in SavedStateHandle`() = runTest {
        val handle = SavedStateHandle(mapOf("monthKey" to "2026-02"))
        coEvery { resultRepository.getByMonth("2026-02") } returns makeEntity("2026-02")
        coEvery { userRepository.get() } returns makeProfile()
        val vm = createViewModel(handle)
        assertEquals("2026-02", vm.state.value.monthKey)
    }

    @Test
    fun `tone defaults to scientific when no profile`() = runTest {
        coEvery { resultRepository.getRecent(1) } returns listOf(makeEntity())
        coEvery { userRepository.get() } returns null
        val vm = createViewModel()
        assertEquals("scientific", vm.state.value.tone)
    }

    @Test
    fun `malformed JSON produces empty domain cards`() = runTest {
        val badEntity = makeEntity().copy(domainScoresJson = "not json")
        coEvery { resultRepository.getRecent(1) } returns listOf(badEntity)
        coEvery { userRepository.get() } returns makeProfile()
        val vm = createViewModel()
        assertTrue(vm.state.value.domainCards.isEmpty())
    }
}
