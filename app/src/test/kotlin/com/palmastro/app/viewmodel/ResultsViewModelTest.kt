package com.palmastro.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.palmastro.data.entities.MonthlyResultEntity
import com.palmastro.data.entities.UserProfileEntity
import com.palmastro.data.repository.ResultRepository
import com.palmastro.data.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ResultsViewModelTest {
    private val resultRepository = mockk<ResultRepository>()
    private val userRepository = mockk<UserRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun makeEntity(monthKey: String = "2026-03", grade: String = "Stable") = MonthlyResultEntity(
        id = "r-1", monthKey = monthKey, scanSessionId = "s-1", calcLevel = "L2",
        confidenceLevel = "high", confidenceReasonsJson = "[]",
        domainScoresJson = """{"career":72,"wealth":58,"family":65,"health":55}""",
        subdimScoresJson = "{}", grade = grade, semanticPayloadsJson = "{}",
        palmFeatureSummaryJson = "{}", astroSignalsJson = "[]", explainabilityJson = "[]",
        rulesetVersion = "1.0.0", contentVersion = "1.0.0",
        scanQualityScore = 85, featureCoverage = 0.9f,
    )

    private fun makeProfile() = UserProfileEntity(
        dominantHand = "right", birthdayEpochDay = 7384, tone = "scientific",
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `load sets hasResults false when no results`() = runTest {
        coEvery { resultRepository.getRecent(1) } returns emptyList()
        coEvery { userRepository.get() } returns makeProfile()
        val vm = ResultsViewModel(SavedStateHandle(), resultRepository, userRepository)
        assertFalse(vm.state.value.hasResults)
    }

    @Test
    fun `load populates domain cards from latest result`() = runTest {
        coEvery { resultRepository.getRecent(1) } returns listOf(makeEntity())
        coEvery { userRepository.get() } returns makeProfile()
        val vm = ResultsViewModel(SavedStateHandle(), resultRepository, userRepository)
        assertTrue(vm.state.value.hasResults)
        assertEquals(4, vm.state.value.domainCards.size)
        assertEquals("Stable", vm.state.value.grade)
        assertEquals("2026-03", vm.state.value.monthKey)
    }

    @Test
    fun `domain cards have correct display names`() = runTest {
        coEvery { resultRepository.getRecent(1) } returns listOf(makeEntity())
        coEvery { userRepository.get() } returns makeProfile()
        val vm = ResultsViewModel(SavedStateHandle(), resultRepository, userRepository)
        val names = vm.state.value.domainCards.map { it.displayName }.toSet()
        assertEquals(setOf("事業", "財富", "家庭", "健康"), names)
    }

    @Test
    fun `loads specific month when monthKey in SavedStateHandle`() = runTest {
        val handle = SavedStateHandle(mapOf("monthKey" to "2026-02"))
        coEvery { resultRepository.getByMonth("2026-02") } returns makeEntity("2026-02")
        coEvery { userRepository.get() } returns makeProfile()
        val vm = ResultsViewModel(handle, resultRepository, userRepository)
        assertEquals("2026-02", vm.state.value.monthKey)
    }

    @Test
    fun `tone defaults to scientific when no profile`() = runTest {
        coEvery { resultRepository.getRecent(1) } returns listOf(makeEntity())
        coEvery { userRepository.get() } returns null
        val vm = ResultsViewModel(SavedStateHandle(), resultRepository, userRepository)
        assertEquals("scientific", vm.state.value.tone)
    }

    @Test
    fun `tone from profile is used`() = runTest {
        coEvery { resultRepository.getRecent(1) } returns listOf(makeEntity())
        coEvery { userRepository.get() } returns makeProfile().copy(tone = "healing")
        val vm = ResultsViewModel(SavedStateHandle(), resultRepository, userRepository)
        assertEquals("healing", vm.state.value.tone)
    }

    @Test
    fun `malformed JSON produces empty domain cards`() = runTest {
        val badEntity = makeEntity().copy(domainScoresJson = "not json")
        coEvery { resultRepository.getRecent(1) } returns listOf(badEntity)
        coEvery { userRepository.get() } returns makeProfile()
        val vm = ResultsViewModel(SavedStateHandle(), resultRepository, userRepository)
        assertTrue(vm.state.value.domainCards.isEmpty())
    }
}
