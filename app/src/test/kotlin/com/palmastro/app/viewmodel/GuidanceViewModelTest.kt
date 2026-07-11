package com.palmastro.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.palmastro.content.Guidance
import com.palmastro.content.GuidanceBuilder
import com.palmastro.content.GuidanceItem
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GuidanceViewModelTest {
    private val resultRepository = mockk<ResultRepository>()
    private val userRepository = mockk<UserRepository>()
    private val guidanceBuilder = mockk<GuidanceBuilder>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var savedLocale: Locale

    private fun makePayload(domain: String) = SemanticPayload(
        domain = domain, monthKey = "2026-03", calcLevel = CalcLevel.L2, confidence = "high",
        observations = emptyList(), interpretation = Interpretation("穩定的分析"), blindspot = "盲點",
        actionToday = "今天行動", actionWeek = "本週行動", prompt = "反思問題",
        safetyNotes = emptyList(), explainability = emptyList(),
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

    private fun makeProfile(language: String = "system") = UserProfileEntity(
        dominantHand = "right", birthdayEpochDay = 7384, tone = "scientific", language = language,
    )

    private fun makeGuidance() = Guidance(
        monthTheme = "穩定累積的一個月",
        strengths = listOf(GuidanceItem("career", "PALM_HEADLINE_LONG_CLEAR", "專注力是你的優勢", "內容", "行動")),
        mindful = listOf(GuidanceItem("health", null, "留意休息節奏", "內容", "行動")),
        weekPlan = listOf("第一步", "第二步"),
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()
        savedLocale = Locale.getDefault()
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(savedLocale)
        Dispatchers.resetMain()
    }

    private fun createViewModel(monthKey: String = "2026-03") = GuidanceViewModel(
        SavedStateHandle(mapOf("monthKey" to monthKey)),
        resultRepository,
        userRepository,
        guidanceBuilder,
    )

    // --- Happy path ---

    @Test
    fun `loads guidance for the requested month`() = runTest {
        val payloads = mapOf("career" to makePayload("career"))
        val guidance = makeGuidance()
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(payloads)
        coEvery { userRepository.get() } returns makeProfile(language = "zh-TW")
        every { guidanceBuilder.build(any(), any(), any()) } returns guidance

        val vm = createViewModel()

        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.error)
        assertEquals(guidance, vm.state.value.guidance)
        assertEquals("2026-03", vm.state.value.monthKey)
        assertEquals("Stable", vm.state.value.grade)
    }

    @Test
    fun `passes decoded payloads and grade to the builder`() = runTest {
        val payloads = mapOf("career" to makePayload("career"), "health" to makePayload("health"))
        val payloadsSlot = slot<Map<String, SemanticPayload>>()
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(payloads)
        coEvery { userRepository.get() } returns makeProfile(language = "en")
        every { guidanceBuilder.build(capture(payloadsSlot), any(), any()) } returns makeGuidance()

        createViewModel()

        verify(exactly = 1) { guidanceBuilder.build(any(), "Stable", "en") }
        assertEquals(setOf("career", "health"), payloadsSlot.captured.keys)
        assertEquals("career", payloadsSlot.captured.getValue("career").domain)
    }

    // --- Missing month ---

    @Test
    fun `sets error when month not found`() = runTest {
        coEvery { resultRepository.getByMonth("2026-03") } returns null

        val vm = createViewModel()

        assertFalse(vm.state.value.isLoading)
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.guidance)
        verify(exactly = 0) { guidanceBuilder.build(any(), any(), any()) }
    }

    // --- Language resolution ---

    @Test
    fun `explicit profile language wins over device locale`() = runTest {
        Locale.setDefault(Locale.US)
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(mapOf("career" to makePayload("career")))
        coEvery { userRepository.get() } returns makeProfile(language = "zh-TW")
        every { guidanceBuilder.build(any(), any(), any()) } returns makeGuidance()

        createViewModel()

        verify { guidanceBuilder.build(any(), any(), "zh-TW") }
    }

    @Test
    fun `system language follows Taiwan device locale`() = runTest {
        Locale.setDefault(Locale.TAIWAN)
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(mapOf("career" to makePayload("career")))
        coEvery { userRepository.get() } returns makeProfile(language = "system")
        every { guidanceBuilder.build(any(), any(), any()) } returns makeGuidance()

        createViewModel()

        verify { guidanceBuilder.build(any(), any(), "zh-TW") }
    }

    @Test
    fun `system language falls back to english for unsupported locales`() = runTest {
        Locale.setDefault(Locale.FRANCE)
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(mapOf("career" to makePayload("career")))
        coEvery { userRepository.get() } returns makeProfile(language = "system")
        every { guidanceBuilder.build(any(), any(), any()) } returns makeGuidance()

        createViewModel()

        verify { guidanceBuilder.build(any(), any(), "en") }
    }

    @Test
    fun `missing profile resolves language from device locale`() = runTest {
        Locale.setDefault(Locale.US)
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(mapOf("career" to makePayload("career")))
        coEvery { userRepository.get() } returns null
        every { guidanceBuilder.build(any(), any(), any()) } returns makeGuidance()

        createViewModel()

        verify { guidanceBuilder.build(any(), any(), "en") }
    }

    // --- Failure handling ---

    @Test
    fun `handles malformed payloads JSON gracefully`() = runTest {
        val entity = makeEntity(emptyMap()).copy(semanticPayloadsJson = "not json")
        coEvery { resultRepository.getByMonth("2026-03") } returns entity
        coEvery { userRepository.get() } returns makeProfile()

        val vm = createViewModel()

        assertFalse(vm.state.value.isLoading)
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.guidance)
    }

    @Test
    fun `builder failure surfaces as error not crash`() = runTest {
        coEvery { resultRepository.getByMonth("2026-03") } returns makeEntity(mapOf("career" to makePayload("career")))
        coEvery { userRepository.get() } returns makeProfile(language = "en")
        every { guidanceBuilder.build(any(), any(), any()) } throws IllegalStateException("boom")

        val vm = createViewModel()

        assertFalse(vm.state.value.isLoading)
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.guidance)
    }
}
