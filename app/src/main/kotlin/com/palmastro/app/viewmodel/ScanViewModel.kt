package com.palmastro.app.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.app.config.FeatureFlags
import com.palmastro.app.di.IoDispatcher
import com.palmastro.app.share.ModelManager
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
import com.palmastro.contracts.interfaces.ScoringEngine
import com.palmastro.data.entities.MonthlyResultEntity
import com.palmastro.data.repository.ResultRepository
import com.palmastro.data.repository.UserRepository
import com.palmastro.scan.CoachingHints
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class ScanState(
    val currentAngleIndex: Int = 0,
    val completedAngles: Set<Angle> = emptySet(),
    val isCapturing: Boolean = false,
    val isProcessing: Boolean = false,
    val isComplete: Boolean = false,
    /** Transient pipeline error; UI maps [ScanError.key] to a string resource. */
    val error: ScanError? = null,
    /** Coaching hint key from CoachingHints.keyFor (coach_blur, coach_glare, ...). */
    val coachingHintKey: String? = null,
    val showFlash: Boolean = false,
    val modelReady: Boolean = false,
    val modelDownloading: Boolean = false,
    /** Blocking model-acquisition error; UI shows retry via [retryModelDownload]. */
    val modelError: ScanError? = null,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userRepository: UserRepository,
    private val resultRepository: ResultRepository,
    private val qualityGate: QualityGate,
    private val palmFeatureExtractor: PalmFeatureExtractor,
    private val astroEngine: AstroEngine,
    private val scoringEngine: ScoringEngine,
    private val deltaEngine: DeltaEngine,
    private val contentComposer: ContentComposerImpl,
    private val safetyFilter: SafetyFilterImpl,
    private val analytics: AnalyticsEmitter,
    private val featureFlags: FeatureFlags,
    private val analyzerFactory: ImageQualityAnalyzerFactory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _state = MutableStateFlow(ScanState())
    val state = _state.asStateFlow()

    private val angles = Angle.entries
    private val capturedPaths = mutableMapOf<Angle, String>()
    private val qualityResults = mutableMapOf<Angle, QualityScores>()
    private val palmMetricsByAngle = mutableMapOf<Angle, PalmMetrics?>()
    private var analyzer: ImageQualityAnalyzer? = null

    /** Serializes [analyzeGuarded] against [onCleared]: close waits for in-flight analysis. */
    private val analyzerLock = Any()

    @Volatile
    private var analyzerClosed = false
    private var scanStartEmitted = false
    private var scanStartTimeMs = 0L

    private val json = Json { encodeDefaults = true }

    // Lazy: CameraX objects cannot be constructed on the JVM, and the UI only needs
    // this once the preview is bound. The resolution cap keeps captures near 2MP:
    // full-sensor 12MP frames would cost IntArray(w*h)+FloatArray(w*h) in analysis
    // (OOM risk) and seconds of per-frame work for no quality-gate benefit.
    val imageCapture: ImageCapture by lazy {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(CAPTURE_TARGET_WIDTH, CAPTURE_TARGET_HEIGHT),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                        )
                    )
                    .build()
            )
            .build()
    }

    init {
        checkAndDownloadModel()
    }

    private fun checkAndDownloadModel() {
        if (ModelManager.isModelReady(appContext)) {
            initAnalyzer()
            return
        }
        _state.update { it.copy(modelDownloading = true, modelError = null) }
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                ModelManager.downloadModel(appContext)
            }
            result.fold(
                onSuccess = { initAnalyzer() },
                onFailure = { t ->
                    _state.update {
                        it.copy(
                            modelDownloading = false,
                            modelError = (t as? ScanErrorException)?.scanError
                                ?: ScanError.MODEL_DOWNLOAD_FAILED,
                        )
                    }
                },
            )
        }
    }

    private fun initAnalyzer() {
        try {
            val source = ModelManager.getModelSource(appContext)
            analyzer = analyzerFactory.create(appContext, source)
            _state.update { it.copy(modelReady = true, modelDownloading = false, modelError = null) }
        } catch (t: Throwable) {
            // Landmarker refused the model file (truncated/corrupt): delete the download
            // so retry fetches fresh bytes, and surface a typed corrupt-model state.
            ModelManager.deleteDownloadedModel(appContext)
            analyzer = null
            _state.update {
                it.copy(modelReady = false, modelDownloading = false, modelError = ScanError.MODEL_CORRUPT)
            }
        }
    }

    fun retryModelDownload() {
        checkAndDownloadModel()
    }

    fun captureCurrentAngle() {
        if (_state.value.isCapturing) return
        val idx = _state.value.currentAngleIndex
        // No angle left to capture: a stray tap during the 200ms flash window after the
        // last angle (or after PROCESSING_FAILED) must be a no-op, not an IndexOutOfBounds.
        if (idx >= angles.size) return
        if (!scanStartEmitted) {
            scanStartEmitted = true
            scanStartTimeMs = System.currentTimeMillis()
            analytics.emit("scan_start", emptyMap())
        }
        _state.update { it.copy(isCapturing = true, coachingHintKey = null) }

        val angle = angles[idx]
        val monthKey = YearMonth.now().toString()
        val scanDir = File(appContext.filesDir, "scans/$monthKey")
        scanDir.mkdirs()
        val outputFile = File(scanDir, "${angle.name}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(appContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onFrameCaptured(angle, outputFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    _state.update {
                        it.copy(isCapturing = false, error = ScanError.PROCESSING_FAILED)
                    }
                }
            }
        )
    }

    private fun onFrameCaptured(angle: Angle, file: File) {
        viewModelScope.launch {
            val metrics = withContext(ioDispatcher) {
                val bitmap = decodeSampledBitmap(file.absolutePath)
                    ?: return@withContext null
                val result = analyzeGuarded(bitmap)
                bitmap.recycle()
                result
            }

            if (metrics == null) {
                _state.update {
                    it.copy(isCapturing = false, error = ScanError.PROCESSING_FAILED)
                }
                return@launch
            }

            val scores = qualityGate.scoreFrame(
                metrics.blur, metrics.glare, metrics.exposure,
                metrics.coverage, metrics.stability,
            )
            val gateResult = qualityGate.evaluateAngle(angle, scores)

            if (!gateResult.passed) {
                val reason = gateResult.failReason ?: "hand_not_detected"
                analytics.emit(
                    "scan_angle_quality_fail",
                    mapOf("angle" to angle.name.lowercase(Locale.ROOT), "reason" to reason),
                )
                file.delete()
                _state.update {
                    it.copy(isCapturing = false, coachingHintKey = CoachingHints.keyFor(reason))
                }
                return@launch
            }

            capturedPaths[angle] = file.absolutePath
            qualityResults[angle] = scores
            palmMetricsByAngle[angle] = metrics.palmMetrics
            analytics.emit("scan_angle_pass", mapOf("angle" to angle.name.lowercase(Locale.ROOT)))

            _state.update {
                it.copy(
                    isCapturing = false,
                    showFlash = true,
                    completedAngles = it.completedAngles + angle,
                    currentAngleIndex = it.currentAngleIndex + 1,
                    coachingHintKey = null,
                )
            }

            kotlinx.coroutines.delay(200)
            _state.update { it.copy(showFlash = false) }

            if (_state.value.currentAngleIndex >= angles.size) {
                analytics.emit(
                    "scan_complete",
                    mapOf("duration_ms" to (System.currentTimeMillis() - scanStartTimeMs)),
                )
                runPipeline()
            }
        }
    }

    fun retakePreviousAngle() {
        val newIndex = (_state.value.currentAngleIndex - 1).coerceAtLeast(0)
        val angle = angles[newIndex]
        capturedPaths.remove(angle)
        qualityResults.remove(angle)
        palmMetricsByAngle.remove(angle)
        _state.update {
            it.copy(
                currentAngleIndex = newIndex,
                completedAngles = it.completedAngles - angle,
                coachingHintKey = null,
            )
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    /**
     * Error action for PROCESSING_FAILED: when all angles are already captured the
     * failure happened in the pipeline, so retry it directly instead of stranding the
     * user on the capture screen with nothing left to capture. Mid-scan failures just
     * clear the error and return to the capture flow.
     */
    fun retryProcessing() {
        _state.update { it.copy(error = null) }
        if (_state.value.currentAngleIndex >= angles.size) {
            runPipeline()
        }
    }

    fun dismissCoachingHint() = _state.update { it.copy(coachingHintKey = null) }

    override fun onCleared() {
        super.onCleared()
        synchronized(analyzerLock) {
            analyzerClosed = true
            analyzer?.close()
            analyzer = null
        }
    }

    /**
     * Two-pass decode: read bounds only, then decode with an inSampleSize that caps the
     * analyzed bitmap near [MAX_ANALYSIS_PIXELS] regardless of what the camera produced
     * (the ResolutionSelector is a request, not a guarantee).
     */
    private fun decodeSampledBitmap(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while ((bounds.outWidth / sample).toLong() * (bounds.outHeight / sample) > MAX_ANALYSIS_PIXELS) {
            sample *= 2
        }
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /**
     * Runs the analyzer under [analyzerLock] so [onCleared] cannot free the underlying
     * HandLandmarker while a frame is mid-analysis; after close this returns null
     * gracefully instead of touching a released native handle.
     */
    @VisibleForTesting
    internal fun analyzeGuarded(bitmap: Bitmap): ImageQualityMetrics? = synchronized(analyzerLock) {
        if (analyzerClosed) return@synchronized null
        val currentAnalyzer = analyzer ?: return@synchronized null
        runCatching { currentAnalyzer.analyze(bitmap) }.getOrNull()
    }

    /** Test seam: registers a captured frame without going through CameraX. */
    @VisibleForTesting
    internal fun seedCapturedFrame(
        angle: Angle,
        scores: QualityScores,
        palmMetrics: PalmMetrics? = null,
        path: String = "",
    ) {
        capturedPaths[angle] = path
        qualityResults[angle] = scores
        palmMetricsByAngle[angle] = palmMetrics
    }

    /** Test seam: places the UI at a given angle index without driving CameraX. */
    @VisibleForTesting
    internal fun seedAngleIndex(index: Int) {
        _state.update { it.copy(currentAngleIndex = index) }
    }

    @VisibleForTesting
    internal fun runPipeline() {
        _state.update { it.copy(isProcessing = true) }
        analytics.emit("inference_start", emptyMap())
        val inferenceStartMs = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                val profile = userRepository.get()
                if (profile == null) {
                    analytics.emit("inference_fail", mapOf("reason" to "no_profile"))
                    _state.update { it.copy(isProcessing = false, error = ScanError.PROCESSING_FAILED) }
                    return@launch
                }

                val bestFrames = angles.associateWith { angle ->
                    val scores = qualityResults[angle]
                        ?: qualityGate.scoreFrame(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
                    BestFrameResult(
                        angle = angle,
                        frameIndex = 0,
                        qualityScores = scores,
                        fileRef = capturedPaths[angle],
                        palmMetrics = palmMetricsByAngle[angle],
                    )
                }
                val hand = if (profile.dominantHand == "left") Hand.LEFT else Hand.RIGHT

                val palmResult = palmFeatureExtractor.extract(bestFrames, hand)

                val birthday = LocalDate.ofEpochDay(profile.birthdayEpochDay)
                val minutes = profile.birthTimeMinutes
                val birthTime = if (profile.hasBirthTime && minutes != null)
                    LocalTime.of(minutes / 60, minutes % 60)
                else null
                val astroResult = astroEngine.compute(
                    birthday, birthTime, profile.birthPlaceLat, profile.birthPlaceLon
                )

                val scoringResult = scoringEngine.score(
                    ScoringInput(
                        palmResult, astroResult,
                        UserContext(hand, false),
                        "1.0.0"
                    )
                )

                val monthKey = YearMonth.now().toString()
                val avgQuality = qualityResults.values
                    .map { it.composite }.average().toInt()

                val prevEntity = resultRepository.getRecent(1).firstOrNull()
                var deltaResult: DeltaResult? = null
                if (prevEntity != null && prevEntity.monthKey != monthKey) {
                    val currentMonthly = MonthlyResult(
                        resultId = "pending",
                        monthKey = monthKey,
                        scanSessionId = "pending",
                        scoringResult = scoringResult,
                        semanticPayloads = emptyMap(),
                        scanQualityScore = avgQuality,
                        featureCoverage = palmResult.featureCoverage,
                        createdAt = System.currentTimeMillis()
                    )
                    deltaResult = deltaEngine.computeDelta(prevEntity.toMonthlyResult(), currentMonthly)
                }

                val tone = Tone.valueOf(profile.tone.uppercase(Locale.ROOT))
                val calcLevel = if (profile.calcLevel == "L2") CalcLevel.L2 else CalcLevel.L1
                val language = resolveLanguage(profile.language)
                val payloads = contentComposer.compose(
                    ContentInput(
                        scoringResult, deltaResult, tone, emptySet(), calcLevel, monthKey,
                        language = language,
                    )
                )

                // Safety gate: every payload is validated; violating payloads are ALWAYS
                // replaced by the engine's safe fallback. strict_safety additionally logs.
                val safePayloads = payloads.mapValues { (domain, payload) ->
                    val check = safetyFilter.validate(payload)
                    if (check.passed) {
                        payload
                    } else {
                        if (featureFlags.strictSafetyEnabled) {
                            analytics.emit(
                                "inference_fail",
                                mapOf("reason" to "safety", "domain" to domain),
                            )
                        }
                        contentComposer.safeFallbackPayload(domain, language, base = payload)
                    }
                }

                deltaResult?.let { delta ->
                    resultRepository.saveDelta(monthKey, delta)
                }

                val resultId = UUID.randomUUID().toString()
                resultRepository.saveResult(
                    MonthlyResultEntity(
                        id = resultId,
                        monthKey = monthKey,
                        scanSessionId = UUID.randomUUID().toString(),
                        calcLevel = profile.calcLevel,
                        confidenceLevel = scoringResult.confidence,
                        confidenceReasonsJson = json.encodeToString(scoringResult.confidenceReasons),
                        domainScoresJson = json.encodeToString(scoringResult.domainScores),
                        subdimScoresJson = json.encodeToString(scoringResult.subdimScores),
                        grade = scoringResult.grade,
                        semanticPayloadsJson = json.encodeToString(safePayloads),
                        palmFeatureSummaryJson = palmFeaturesToJson(palmResult),
                        astroSignalsJson = astroSignalsToJson(astroResult),
                        explainabilityJson = json.encodeToString(scoringResult.explainability),
                        rulesetVersion = scoringResult.rulesetVersion,
                        contentVersion = contentComposer.templatesVersion,
                        scanQualityScore = avgQuality,
                        featureCoverage = palmResult.featureCoverage,
                        scanImagePath = "scans/$monthKey",
                    )
                )

                analytics.emit(
                    "inference_success",
                    mapOf(
                        "confidence" to scoringResult.confidence.lowercase(Locale.ROOT),
                        "calc_level" to profile.calcLevel.lowercase(Locale.ROOT),
                        "duration_ms" to (System.currentTimeMillis() - inferenceStartMs),
                    ),
                )
                _state.update { it.copy(isProcessing = false, isComplete = true) }
            } catch (e: Exception) {
                analytics.emit("inference_fail", mapOf("reason" to "pipeline_error"))
                _state.update { it.copy(isProcessing = false, error = ScanError.PROCESSING_FAILED) }
            }
        }
    }

    /**
     * Resolves the content language: explicit profile choice wins; "system" (the v3
     * default) follows the device locale, restricted to the engine-supported set.
     */
    private fun resolveLanguage(profileLanguage: String?): String {
        val explicit = profileLanguage?.takeUnless { it.isBlank() || it == "system" }
        if (explicit != null) return explicit
        val locale = Locale.getDefault()
        return when {
            locale.language == "zh" &&
                (locale.script == "Hant" || locale.country in setOf("TW", "HK", "MO")) -> "zh-TW"
            locale.language == "zh" -> "zh-CN"
            locale.language == "ja" -> "ja"
            locale.language == "hi" -> "hi"
            else -> "en"
        }
    }

    /** PalmFeatures is not @Serializable (contracts frozen); build the JSON by hand. */
    private fun palmFeaturesToJson(palmResult: PalmFeatureResult): String {
        val f = palmResult.features
        return buildJsonObject {
            put("headlinePresent", f.headlinePresent)
            put("heartlinePresent", f.heartlinePresent)
            put("lifelinePresent", f.lifelinePresent)
            put("fatelinePresent", f.fatelinePresent)
            put("headlineShape", f.headlineShape)
            put("heartlineShape", f.heartlineShape)
            put("lifelineShape", f.lifelineShape)
            put("fatelineShape", f.fatelineShape)
            put("headlineClarity", f.headlineClarity)
            put("heartlineClarity", f.heartlineClarity)
            put("lifelineClarity", f.lifelineClarity)
            put("fatelineClarity", f.fatelineClarity)
            put("headlineLength", f.headlineLength)
            put("fatelineLength", f.fatelineLength)
            put("venusMountDensity", f.venusMountDensity)
            put("jupiterMountDensity", f.jupiterMountDensity)
            put("saturnMountDensity", f.saturnMountDensity)
            put("minorLineDensity", f.minorLineDensity)
            put("featureCoverage", palmResult.featureCoverage)
            put("confidence", palmResult.confidence)
            put("extractorVersion", palmResult.extractorVersion)
        }.toString()
    }

    /** AstroSignal is not @Serializable (contracts frozen); build the JSON by hand. */
    private fun astroSignalsToJson(astroResult: AstroResult): String {
        return buildJsonArray {
            astroResult.signals.forEach { signal ->
                add(
                    buildJsonObject {
                        put("signalId", signal.signalId)
                        put("direction", signal.direction)
                        put("magnitude", signal.magnitude)
                        put("confidence", signal.confidence)
                        put("safetyTag", signal.safetyTag)
                    }
                )
            }
        }.toString()
    }

    private fun MonthlyResultEntity.toMonthlyResult(): MonthlyResult {
        val domainScores = runCatching {
            json.decodeFromString<Map<String, Int>>(domainScoresJson)
        }.getOrDefault(emptyMap())
        val subdimScores = runCatching {
            json.decodeFromString<Map<String, Int>>(subdimScoresJson)
        }.getOrDefault(emptyMap())
        return MonthlyResult(
            resultId = id,
            monthKey = monthKey,
            scanSessionId = scanSessionId,
            scoringResult = ScoringResult(
                domainScores = domainScores,
                subdimScores = subdimScores,
                grade = grade,
                confidence = confidenceLevel,
                confidenceReasons = emptyList(),
                explainability = emptyList(),
                matchedBuckets = emptyList(),
                rulesetVersion = rulesetVersion
            ),
            semanticPayloads = emptyMap(),
            scanQualityScore = scanQualityScore,
            featureCoverage = featureCoverage,
            createdAt = createdAt
        )
    }

    private companion object {
        /** Capture target (~2MP): plenty for the quality gate + palm-line sampling. */
        const val CAPTURE_TARGET_WIDTH = 1600
        const val CAPTURE_TARGET_HEIGHT = 1200
        /** Hard cap on the decoded analysis bitmap, whatever the camera delivered. */
        const val MAX_ANALYSIS_PIXELS = 2_000_000L
    }
}
