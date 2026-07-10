package com.palmastro.app.viewmodel

import android.content.Context
import com.palmastro.app.config.FeatureFlags
import com.palmastro.app.share.ModelManager
import com.palmastro.app.share.ModelSource
import com.palmastro.app.share.ScanError
import com.palmastro.app.share.ScanErrorException
import com.palmastro.app.ui.scan.ImageQualityAnalyzer
import com.palmastro.app.ui.scan.ImageQualityAnalyzerFactory
import com.palmastro.content.ContentComposerImpl
import com.palmastro.content.SafetyFilterImpl
import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.AnalyticsEmitter
import com.palmastro.contracts.interfaces.AstroEngine
import com.palmastro.contracts.interfaces.DeltaEngine
import com.palmastro.contracts.interfaces.PalmFeatureExtractor
import com.palmastro.contracts.interfaces.QualityGate
import com.palmastro.contracts.interfaces.SafetyCheckResult
import com.palmastro.contracts.interfaces.ScoringEngine
import com.palmastro.data.entities.MonthlyResultEntity
import com.palmastro.data.entities.UserProfileEntity
import com.palmastro.data.repository.ResultRepository
import com.palmastro.data.repository.UserRepository
import io.mockk.*
import java.io.File
import java.time.YearMonth
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {
    private val context = mockk<Context>(relaxed = true)
    private val userRepository = mockk<UserRepository>()
    private val resultRepository = mockk<ResultRepository>()
    private val qualityGate = mockk<QualityGate>(relaxed = true)
    private val palmFeatureExtractor = mockk<PalmFeatureExtractor>()
    private val astroEngine = mockk<AstroEngine>()
    private val scoringEngine = mockk<ScoringEngine>()
    private val deltaEngine = mockk<DeltaEngine>()
    private val contentComposer = mockk<ContentComposerImpl>()
    private val safetyFilter = mockk<SafetyFilterImpl>()
    private val analytics = mockk<AnalyticsEmitter>(relaxed = true)
    private val featureFlags = mockk<FeatureFlags>()
    private val analyzerFactory = mockk<ImageQualityAnalyzerFactory>()
    private val analyzer = mockk<ImageQualityAnalyzer>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private val monthKey = YearMonth.now().toString()
    private var defaultLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        clearAllMocks()
        mockkObject(ModelManager)
        every { ModelManager.isModelReady(any()) } returns true
        every { ModelManager.getModelSource(any()) } returns ModelSource.FileSystem("/models/hand_landmarker.task")
        justRun { ModelManager.deleteDownloadedModel(any()) }
        every { analyzerFactory.create(any(), any()) } returns analyzer
        every { featureFlags.strictSafetyEnabled } returns true
        stubHappyPipeline()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(ModelManager)
        Locale.setDefault(defaultLocale)
        Dispatchers.resetMain()
    }

    // ------------------------------------------------------------------ fixtures

    private fun makeProfile() = UserProfileEntity(
        dominantHand = "right",
        birthdayEpochDay = 7384,
        hasBirthTime = true,
        birthTimeMinutes = 600,
        hasBirthPlace = true,
        birthPlaceLat = 25.03,
        birthPlaceLon = 121.56,
        tone = "scientific",
        calcLevel = "L2",
    )

    private fun makeScores(composite: Int = 85) =
        QualityScores(0.8f, 0.9f, 0.9f, 0.85f, 0.9f, composite)

    private fun makePalmFeatures() = PalmFeatures(
        headlinePresent = true, heartlinePresent = true,
        lifelinePresent = true, fatelinePresent = false,
        headlineShape = "straight", heartlineShape = "curved",
        lifelineShape = "curved", fatelineShape = "unknown",
        headlineClarity = "clear", heartlineClarity = "clear",
        lifelineClarity = "faint", fatelineClarity = "unknown",
        headlineLength = "long", fatelineLength = "unknown",
        venusMountDensity = "med", jupiterMountDensity = "high",
        saturnMountDensity = "low", minorLineDensity = "med",
    )

    private fun makeScoringResult() = ScoringResult(
        domainScores = mapOf("career" to 72, "wealth" to 58, "family" to 65, "health" to 55),
        subdimScores = mapOf("career.execution" to 70, "career.opportunity" to 74),
        grade = "Stable",
        confidence = "high",
        confidenceReasons = listOf("full_coverage", "birth_time_known"),
        explainability = listOf(
            ExplainEntry("headline_long", "career.execution", 4.0),
            ExplainEntry("sun_sign_taurus", "wealth.stability", 2.0),
        ),
        matchedBuckets = listOf("b1"),
        rulesetVersion = "1.2.0",
    )

    private fun makePayload(domain: String, marker: String = "ok") = SemanticPayload(
        domain = domain, monthKey = monthKey, calcLevel = CalcLevel.L2,
        confidence = "high", language = "en",
        observations = emptyList(),
        interpretation = Interpretation("$marker interpretation for $domain"),
        blindspot = "$marker blindspot", actionToday = "$marker today",
        actionWeek = "$marker week", prompt = "$marker prompt",
        safetyNotes = emptyList(), explainability = emptyList(),
        scoreCard = ScoreCard(60, "Stable", null, null, emptyMap()),
    )

    private fun makePayloads(marker: String = "ok") =
        Domains.ALL.associateWith { makePayload(it, marker) }

    private fun makePrevEntity(prevMonthKey: String) = MonthlyResultEntity(
        id = "prev-1", monthKey = prevMonthKey, scanSessionId = "s-prev", calcLevel = "L2",
        confidenceLevel = "high", confidenceReasonsJson = "[]",
        domainScoresJson = """{"career":60,"wealth":60,"family":60,"health":60}""",
        subdimScoresJson = "{}", grade = "Stable", semanticPayloadsJson = "{}",
        palmFeatureSummaryJson = "{}", astroSignalsJson = "[]", explainabilityJson = "[]",
        rulesetVersion = "1.2.0", contentVersion = "2.0.0",
        scanQualityScore = 80, featureCoverage = 0.8f,
    )

    private fun makeDelta(prevMonthKey: String) = DeltaResult(
        domainDeltas = mapOf("career" to DeltaValue(12, "up")),
        subdimDeltas = emptyMap(),
        gradeShift = null,
        comparabilityScore = 90,
        comparabilityBucket = ComparabilityBucket.HIGH,
        prevMonthKey = prevMonthKey,
        currentMonthKey = monthKey,
    )

    private fun stubHappyPipeline() {
        coEvery { userRepository.get() } returns makeProfile()
        coEvery { resultRepository.getRecent(1) } returns emptyList()
        coJustRun { resultRepository.saveResult(any()) }
        coJustRun { resultRepository.saveDelta(any<String>(), any<DeltaResult>()) }
        every { palmFeatureExtractor.extract(any(), any()) } returns
            PalmFeatureResult(makePalmFeatures(), 0.85f, "high", "1.1.0")
        every { astroEngine.compute(any(), any(), any(), any()) } returns
            AstroResult(CalcLevel.L2, listOf(AstroSignal("moon_sign_cancer", "up", 2, "med", "none")), "1.1.0")
        every { scoringEngine.score(any()) } returns makeScoringResult()
        every { deltaEngine.computeDelta(any(), any()) } returns makeDelta("2026-06")
        every { contentComposer.compose(any()) } returns makePayloads()
        every { contentComposer.templatesVersion } returns "2.1.0"
        every { safetyFilter.validate(any()) } returns SafetyCheckResult(true, emptyList())
    }

    private fun buildViewModel() = ScanViewModel(
        appContext = context,
        userRepository = userRepository,
        resultRepository = resultRepository,
        qualityGate = qualityGate,
        palmFeatureExtractor = palmFeatureExtractor,
        astroEngine = astroEngine,
        scoringEngine = scoringEngine,
        deltaEngine = deltaEngine,
        contentComposer = contentComposer,
        safetyFilter = safetyFilter,
        analytics = analytics,
        featureFlags = featureFlags,
        analyzerFactory = analyzerFactory,
        ioDispatcher = testDispatcher,
    )

    private fun seedAllAngles(vm: ScanViewModel) {
        Angle.entries.forEach { angle ->
            vm.seedCapturedFrame(angle, makeScores(), palmMetrics = null, path = "/scans/$monthKey/${angle.name}.jpg")
        }
    }

    private fun runPipelineToCompletion(vm: ScanViewModel, testScope: TestScope): MonthlyResultEntity {
        val entitySlot = slot<MonthlyResultEntity>()
        coJustRun { resultRepository.saveResult(capture(entitySlot)) }
        seedAllAngles(vm)
        vm.runPipeline()
        testScope.advanceUntilIdle()
        return entitySlot.captured
    }

    // ------------------------------------------------------------------ model errors

    @Test
    fun `download failure surfaces typed MODEL_DOWNLOAD_FAILED and retry recovers`() = runTest {
        every { ModelManager.isModelReady(any()) } returns false
        every { ModelManager.downloadModel(any()) } returns Result.failure(
            ScanErrorException(ScanError.MODEL_DOWNLOAD_FAILED, "HTTP 503")
        )
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(ScanError.MODEL_DOWNLOAD_FAILED, vm.state.value.modelError)
        assertFalse(vm.state.value.modelReady)
        assertTrue(vm.state.value.modelError!!.retryable)

        every { ModelManager.downloadModel(any()) } returns Result.success(File("/models/hand_landmarker.task"))
        vm.retryModelDownload()
        advanceUntilIdle()
        assertTrue(vm.state.value.modelReady)
        assertNull(vm.state.value.modelError)
    }

    @Test
    fun `checksum mismatch surfaces MODEL_CORRUPT`() = runTest {
        every { ModelManager.isModelReady(any()) } returns false
        every { ModelManager.downloadModel(any()) } returns Result.failure(
            ScanErrorException(ScanError.MODEL_CORRUPT, "SHA-256 mismatch")
        )
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(ScanError.MODEL_CORRUPT, vm.state.value.modelError)
    }

    @Test
    fun `landmarker init failure deletes download and surfaces MODEL_CORRUPT`() = runTest {
        every { analyzerFactory.create(any(), any()) } throws RuntimeException("bad model bytes")
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(ScanError.MODEL_CORRUPT, vm.state.value.modelError)
        assertFalse(vm.state.value.modelReady)
        verify { ModelManager.deleteDownloadedModel(any()) }
    }

    // ------------------------------------------------------------------ persistence

    @Test
    fun `pipeline persists real scoring json instead of placeholders`() = runTest {
        val vm = buildViewModel()
        val entity = runPipelineToCompletion(vm, this)

        assertTrue(vm.state.value.isComplete)
        // Real explainability, not "[]"
        assertTrue(entity.explainabilityJson.contains("headline_long"))
        assertTrue(entity.explainabilityJson.contains("career.execution"))
        // Real subdim scores, not "{}"
        assertTrue(entity.subdimScoresJson.contains("career.execution"))
        assertTrue(entity.subdimScoresJson.contains("70"))
        // Real palm feature summary, not "{}"
        assertTrue(entity.palmFeatureSummaryJson.contains("headlinePresent"))
        assertTrue(entity.palmFeatureSummaryJson.contains("venusMountDensity"))
        // Real astro signals, not "[]"
        assertTrue(entity.astroSignalsJson.contains("moon_sign_cancer"))
        // Versions come from the engines, not hardcoded "1.0.0"
        assertEquals("2.1.0", entity.contentVersion)
        assertEquals("1.2.0", entity.rulesetVersion)
        // Confidence reasons persisted
        assertTrue(entity.confidenceReasonsJson.contains("full_coverage"))
        assertEquals("high", entity.confidenceLevel)
    }

    @Test
    fun `pipeline passes profile tone and resolved language to composer`() = runTest {
        val inputSlot = slot<ContentInput>()
        every { contentComposer.compose(capture(inputSlot)) } returns makePayloads()
        val vm = buildViewModel()
        runPipelineToCompletion(vm, this)

        assertEquals(Tone.SCIENTIFIC, inputSlot.captured.tone)
        assertEquals("en", inputSlot.captured.language)
        assertEquals(CalcLevel.L2, inputSlot.captured.calcLevel)
    }

    // ------------------------------------------------------------------ safety

    @Test
    fun `safety violation substitutes fallback payload and logs under strict flag`() = runTest {
        val fallback = makePayload("career", marker = "FALLBACK_SAFE")
        every { safetyFilter.validate(any()) } answers {
            val payload = firstArg<SemanticPayload>()
            if (payload.domain == "career") SafetyCheckResult(false, listOf("deterministic_claim"))
            else SafetyCheckResult(true, emptyList())
        }
        every { safetyFilter.safeFallbackPayload("career", "en") } returns fallback

        val vm = buildViewModel()
        val entity = runPipelineToCompletion(vm, this)

        assertTrue(entity.semanticPayloadsJson.contains("FALLBACK_SAFE"))
        assertFalse(entity.semanticPayloadsJson.contains("ok interpretation for career"))
        // Other domains untouched
        assertTrue(entity.semanticPayloadsJson.contains("ok interpretation for wealth"))
        verify { safetyFilter.safeFallbackPayload("career", "en") }
        verify { analytics.emit("inference_fail", match { it["reason"] == "safety" && it["domain"] == "career" }) }
    }

    @Test
    fun `safety violation still substitutes fallback when strict flag off but does not log`() = runTest {
        every { featureFlags.strictSafetyEnabled } returns false
        val fallback = makePayload("health", marker = "FALLBACK_SAFE")
        every { safetyFilter.validate(any()) } answers {
            val payload = firstArg<SemanticPayload>()
            if (payload.domain == "health") SafetyCheckResult(false, listOf("medical_claim"))
            else SafetyCheckResult(true, emptyList())
        }
        every { safetyFilter.safeFallbackPayload("health", "en") } returns fallback

        val vm = buildViewModel()
        val entity = runPipelineToCompletion(vm, this)

        assertTrue(entity.semanticPayloadsJson.contains("FALLBACK_SAFE"))
        verify(exactly = 0) { analytics.emit("inference_fail", match { it["reason"] == "safety" }) }
    }

    // ------------------------------------------------------------------ delta

    @Test
    fun `delta is computed against previous month and saved`() = runTest {
        val delta = makeDelta("2026-06")
        coEvery { resultRepository.getRecent(1) } returns listOf(makePrevEntity("2026-06"))
        every { deltaEngine.computeDelta(any(), any()) } returns delta
        val inputSlot = slot<ContentInput>()
        every { contentComposer.compose(capture(inputSlot)) } returns makePayloads()

        val vm = buildViewModel()
        runPipelineToCompletion(vm, this)

        coVerify { resultRepository.saveDelta(monthKey, delta) }
        assertEquals(delta, inputSlot.captured.deltaResult)
    }

    @Test
    fun `no delta saved when previous result is same month`() = runTest {
        coEvery { resultRepository.getRecent(1) } returns listOf(makePrevEntity(monthKey))

        val vm = buildViewModel()
        runPipelineToCompletion(vm, this)

        verify(exactly = 0) { deltaEngine.computeDelta(any(), any()) }
        coVerify(exactly = 0) { resultRepository.saveDelta(any<String>(), any<DeltaResult>()) }
    }

    // ------------------------------------------------------------------ error mapping

    @Test
    fun `missing profile maps to PROCESSING_FAILED with no_profile analytics`() = runTest {
        coEvery { userRepository.get() } returns null
        val vm = buildViewModel()
        seedAllAngles(vm)
        vm.runPipeline()
        advanceUntilIdle()

        assertEquals(ScanError.PROCESSING_FAILED, vm.state.value.error)
        assertFalse(vm.state.value.isProcessing)
        verify { analytics.emit("inference_fail", match { it["reason"] == "no_profile" }) }
    }

    @Test
    fun `engine exception maps to PROCESSING_FAILED without leaking message`() = runTest {
        every { scoringEngine.score(any()) } throws IllegalStateException("raw internal detail")
        val vm = buildViewModel()
        seedAllAngles(vm)
        vm.runPipeline()
        advanceUntilIdle()

        assertEquals(ScanError.PROCESSING_FAILED, vm.state.value.error)
        verify { analytics.emit("inference_fail", match { it["reason"] == "pipeline_error" }) }

        vm.dismissError()
        assertNull(vm.state.value.error)
    }

    @Test
    fun `successful pipeline emits inference_start and inference_success`() = runTest {
        val vm = buildViewModel()
        runPipelineToCompletion(vm, this)

        verify { analytics.emit("inference_start", any()) }
        verify {
            analytics.emit(
                "inference_success",
                match { it["confidence"] == "high" && it["calc_level"] == "l2" },
            )
        }
    }
}
