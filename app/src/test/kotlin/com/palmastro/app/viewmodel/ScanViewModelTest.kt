package com.palmastro.app.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModelStore
import com.palmastro.app.config.FeatureFlags
import com.palmastro.app.share.ModelManager
import com.palmastro.app.share.ModelSource
import com.palmastro.app.share.ScanError
import com.palmastro.app.share.ScanErrorException
import com.palmastro.app.ui.scan.ImageQualityAnalyzer
import com.palmastro.app.ui.scan.ImageQualityAnalyzerFactory
import com.palmastro.app.ui.scan.ImageQualityMetrics
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
import com.palmastro.scan.CoachingHints
import io.mockk.*
import java.io.File
import java.nio.file.Files
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

    // Real directory: the ViewModel reads and deletes scan files through appContext.filesDir.
    private lateinit var filesDir: File

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        clearAllMocks()
        filesDir = Files.createTempDirectory("palmastro-scan").toFile()
        every { context.filesDir } returns filesDir
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
        filesDir.deleteRecursively()
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
        coEvery { resultRepository.getRecent(2) } returns emptyList()
        coJustRun { resultRepository.saveResult(any(), any()) }
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
        coJustRun { resultRepository.saveResult(capture(entitySlot), any()) }
        seedAllAngles(vm)
        vm.runPipeline()
        testScope.advanceUntilIdle()
        return entitySlot.captured
    }

    private fun makePalmMetrics() = PalmMetrics(
        landmarks = listOf(LandmarkPoint(0.11f, 0.22f, 0.33f)),
        lineRegions = listOf(LineRegionMetrics("headline", 0.5f, 0.6f, 0.7f)),
    )

    /** Writes frames plus the sidecar exactly as a session killed mid-scan would leave them. */
    private fun seedKilledSession(scanDir: File, captured: List<Angle>) {
        scanDir.mkdirs()
        val entries = captured.joinToString(",") { angle ->
            val frame = File(scanDir, "${angle.name}.jpg").apply { writeText("jpeg") }
            "{\"angle\":\"${angle.name}\",\"path\":\"${frame.absolutePath}\"," +
                "\"blur\":0.8,\"glare\":0.9,\"exposure\":0.9," +
                "\"coverage\":0.85,\"stability\":0.9,\"composite\":85}"
        }
        File(scanDir, "progress.json").writeText("[$entries]")
    }

    /** Drives ViewModel.onCleared through the real androidx path. */
    private fun clearViewModel(vm: ScanViewModel) {
        ViewModelStore().apply {
            put("scan", vm)
            clear()
        }
    }

    // ------------------------------------------------------------------ model errors

    @Test
    fun `download failure surfaces typed MODEL_DOWNLOAD_FAILED and retry recovers`() = runTest {
        every { ModelManager.isModelReady(any()) } returns false
        every { ModelManager.downloadModel(any(), any()) } returns Result.failure(
            ScanErrorException(ScanError.MODEL_DOWNLOAD_FAILED, "HTTP 503")
        )
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(ScanError.MODEL_DOWNLOAD_FAILED, vm.state.value.modelError)
        assertFalse(vm.state.value.modelReady)
        assertTrue(vm.state.value.modelError!!.retryable)

        every { ModelManager.downloadModel(any(), any()) } returns Result.success(File("/models/hand_landmarker.task"))
        vm.retryModelDownload()
        advanceUntilIdle()
        assertTrue(vm.state.value.modelReady)
        assertNull(vm.state.value.modelError)
    }

    @Test
    fun `checksum mismatch surfaces MODEL_CORRUPT`() = runTest {
        every { ModelManager.isModelReady(any()) } returns false
        every { ModelManager.downloadModel(any(), any()) } returns Result.failure(
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
        every { contentComposer.safeFallbackPayload("career", "en", any()) } returns fallback

        val vm = buildViewModel()
        val entity = runPipelineToCompletion(vm, this)

        assertTrue(entity.semanticPayloadsJson.contains("FALLBACK_SAFE"))
        assertFalse(entity.semanticPayloadsJson.contains("ok interpretation for career"))
        // Other domains untouched
        assertTrue(entity.semanticPayloadsJson.contains("ok interpretation for wealth"))
        verify { contentComposer.safeFallbackPayload("career", "en", any()) }
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
        every { contentComposer.safeFallbackPayload("health", "en", any()) } returns fallback

        val vm = buildViewModel()
        val entity = runPipelineToCompletion(vm, this)

        assertTrue(entity.semanticPayloadsJson.contains("FALLBACK_SAFE"))
        verify(exactly = 0) { analytics.emit("inference_fail", match { it["reason"] == "safety" }) }
    }

    // ------------------------------------------------------------------ delta

    @Test
    fun `delta is computed against previous month and saved`() = runTest {
        val delta = makeDelta("2026-06")
        coEvery { resultRepository.getRecent(2) } returns listOf(makePrevEntity("2026-06"))
        every { deltaEngine.computeDelta(any(), any()) } returns delta
        val inputSlot = slot<ContentInput>()
        every { contentComposer.compose(capture(inputSlot)) } returns makePayloads()

        val vm = buildViewModel()
        runPipelineToCompletion(vm, this)

        coVerify { resultRepository.saveResult(any(), delta) }
        assertEquals(delta, inputSlot.captured.deltaResult)
    }

    @Test
    fun `same-month rescan recomputes the delta against the prior month`() = runTest {
        val delta = makeDelta("2026-06")
        coEvery { resultRepository.getRecent(2) } returns
            listOf(makePrevEntity(monthKey), makePrevEntity("2026-06"))
        every { deltaEngine.computeDelta(any(), any()) } returns delta

        val vm = buildViewModel()
        runPipelineToCompletion(vm, this)

        // Without this the rescan leaves the previous scan's arrows next to new scores.
        coVerify { resultRepository.saveResult(any(), delta) }
    }

    @Test
    fun `no delta saved when no earlier month exists`() = runTest {
        coEvery { resultRepository.getRecent(2) } returns listOf(makePrevEntity(monthKey))

        val vm = buildViewModel()
        runPipelineToCompletion(vm, this)

        verify(exactly = 0) { deltaEngine.computeDelta(any(), any()) }
        // A null delta clears the row rather than leaving a stale one behind.
        coVerify { resultRepository.saveResult(any(), null) }
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

    // ------------------------------------------------------------------ capture guard

    @Test
    fun `capture past the last angle is a no-op instead of IndexOutOfBounds`() = runTest {
        val vm = buildViewModel()
        seedAllAngles(vm)
        vm.seedAngleIndex(Angle.entries.size)

        vm.captureCurrentAngle()
        advanceUntilIdle()

        assertFalse(vm.state.value.isCapturing)
        assertEquals(Angle.entries.size, vm.state.value.currentAngleIndex)
        verify(exactly = 0) { analytics.emit("scan_start", any()) }
    }

    // ------------------------------------------------------------------ retry processing

    @Test
    fun `retryProcessing re-runs the pipeline when all angles are captured`() = runTest {
        every { scoringEngine.score(any()) } throws IllegalStateException("transient failure")
        val vm = buildViewModel()
        seedAllAngles(vm)
        vm.seedAngleIndex(Angle.entries.size)
        vm.runPipeline()
        advanceUntilIdle()
        assertEquals(ScanError.PROCESSING_FAILED, vm.state.value.error)

        every { scoringEngine.score(any()) } returns makeScoringResult()
        vm.retryProcessing()
        advanceUntilIdle()

        assertNull(vm.state.value.error)
        assertTrue(vm.state.value.isComplete)
        coVerify { resultRepository.saveResult(any(), any()) }
    }

    @Test
    fun `retryProcessing mid-scan clears the error without running the pipeline`() = runTest {
        val vm = buildViewModel()
        vm.retryProcessing()
        advanceUntilIdle()

        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.isProcessing)
        verify(exactly = 0) { analytics.emit("inference_start", any()) }
    }

    // ------------------------------------------------------------------ raw media retention

    @Test
    fun `retention off empties the image path and deletes the captured frames`() = runTest {
        coEvery { userRepository.get() } returns makeProfile().copy(rawMediaRetention = false)
        val scanDir = File(filesDir, "scans/$monthKey").apply { mkdirs() }
        val frame = File(scanDir, "FRONT.jpg").apply { writeText("jpeg") }

        val vm = buildViewModel()
        val entity = runPipelineToCompletion(vm, this)

        assertEquals("", entity.scanImagePath)
        assertFalse(frame.exists())
        assertFalse(scanDir.exists())
    }

    @Test
    fun `retention on keeps the captured frames and records the image path`() = runTest {
        val scanDir = File(filesDir, "scans/$monthKey").apply { mkdirs() }
        val frame = File(scanDir, "FRONT.jpg").apply { writeText("jpeg") }

        val vm = buildViewModel()
        val entity = runPipelineToCompletion(vm, this)

        assertEquals("scans/$monthKey", entity.scanImagePath)
        assertTrue(frame.exists())
    }

    // ------------------------------------------------------------------ coaching direction

    @Test
    fun `an over-exposed frame is coached away from the light, not toward it`() = runTest {
        val vm = buildViewModel()
        val bright = ImageQualityMetrics(
            blur = 0.45f, glare = 0.55f, exposure = 0.40f, coverage = 0.60f, meanBrightness = 205f,
        )
        val dark = bright.copy(meanBrightness = 35f)

        assertEquals("over_exposure", vm.coachingReasonFor("low_light", bright))
        assertEquals("coach_over_exposure", CoachingHints.keyFor(vm.coachingReasonFor("low_light", bright)))
        assertEquals("low_light", vm.coachingReasonFor("low_light", dark))
        assertEquals("blur", vm.coachingReasonFor("blur", bright))
    }

    // ------------------------------------------------------------------ analyzer close race

    @Test
    fun `analyze delegates before close and returns null after close without throwing`() = runTest {
        val metrics = ImageQualityMetrics(blur = 0.8f, glare = 0.9f, exposure = 0.9f, coverage = 0.85f)
        every { analyzer.analyze(any()) } returns metrics
        val bitmap = mockk<Bitmap>()
        val vm = buildViewModel()

        assertEquals(metrics, vm.analyzeGuarded(bitmap))

        clearViewModel(vm)

        assertNull(vm.analyzeGuarded(bitmap))
        verify(exactly = 1) { analyzer.close() }
        verify(exactly = 1) { analyzer.analyze(any()) }
    }

    // ------------------------------------------------------------------ capture progress

    @Test
    fun `the capture sidecar keeps palm landmark data out of plaintext`() = runTest {
        val angle = Angle.entries.first()
        val scanDir = File(filesDir, "scans/$monthKey")
        val frame = File(scanDir.apply { mkdirs() }, "${angle.name}.jpg").apply { writeText("jpeg") }
        val vm = buildViewModel()
        vm.seedCapturedFrame(angle, makeScores(), makePalmMetrics(), frame.absolutePath)

        vm.persistCaptureProgress()
        advanceUntilIdle()

        val sidecar = File(scanDir, "progress.json").readText()
        assertTrue(sidecar.contains("\"angle\":\"${angle.name}\""))
        // Landmarks and line-region statistics are palm-derived biometric data: they belong
        // in the encrypted store only, and are re-derived from the frame on restore.
        assertFalse(sidecar.contains("landmarks"))
        assertFalse(sidecar.contains("lineRegions"))
        assertFalse(sidecar.contains("palmMetrics"))
    }

    @Test
    fun `restore leaves an angle to capture and never starts the pipeline by itself`() = runTest {
        seedKilledSession(File(filesDir, "scans/$monthKey"), Angle.entries)

        val vm = buildViewModel()
        advanceUntilIdle()

        // One angle short by construction: a restore into "nothing left to capture" would
        // strand the user with no way to reach — or escape — the pipeline.
        assertEquals(Angle.entries.size - 1, vm.state.value.completedAngles.size)
        assertEquals(Angle.entries.size - 1, vm.state.value.currentAngleIndex)
        assertFalse(vm.state.value.isProcessing)
        assertFalse(vm.state.value.isComplete)
        verify(exactly = 0) { analytics.emit("inference_start", any()) }
        coVerify(exactly = 0) { resultRepository.saveResult(any(), any()) }
    }

    @Test
    fun `retake deletes the rejected frame so a stale snapshot cannot resurrect it`() = runTest {
        val kept = Angle.entries[0]
        val retaken = Angle.entries[1]
        val scanDir = File(filesDir, "scans/$monthKey")
        seedKilledSession(scanDir, listOf(kept, retaken))
        val retakenFrame = File(scanDir, "${retaken.name}.jpg")

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.retakePreviousAngle()
        advanceUntilIdle()

        assertFalse(retakenFrame.exists())
        assertTrue(File(scanDir, "${kept.name}.jpg").exists())
        val sidecar = File(scanDir, "progress.json").readText()
        assertFalse(sidecar.contains("\"angle\":\"${retaken.name}\""))
        assertTrue(sidecar.contains("\"angle\":\"${kept.name}\""))
        assertEquals(1, vm.state.value.currentAngleIndex)
    }
}
