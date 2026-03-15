package com.palmastro.app.viewmodel

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmastro.astro.AstroEngineImpl
import com.palmastro.content.ContentComposerImpl
import com.palmastro.content.SafetyFilterImpl
import com.palmastro.contracts.*
import com.palmastro.data.entities.MonthlyResultEntity
import com.palmastro.data.repository.ResultRepository
import com.palmastro.data.repository.UserRepository
import com.palmastro.palm.PalmFeatureExtractorImpl
import com.palmastro.scan.CoachingHints
import com.palmastro.scan.QualityGateImpl
import com.palmastro.scoring.DeltaEngineImpl
import com.palmastro.scoring.ScoringEngineImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.graphics.BitmapFactory
import com.palmastro.app.ui.scan.ImageQualityAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

data class ScanState(
    val currentAngleIndex: Int = 0,
    val completedAngles: Set<Angle> = emptySet(),
    val isCapturing: Boolean = false,
    val isProcessing: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null,
    val coachingHint: String? = null,
    val showFlash: Boolean = false,
    val modelReady: Boolean = false,
    val modelDownloading: Boolean = false,
    val modelError: String? = null,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userRepository: UserRepository,
    private val resultRepository: ResultRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ScanState())
    val state = _state.asStateFlow()

    private val angles = Angle.entries
    private val qualityGate = QualityGateImpl()
    private val capturedPaths = mutableMapOf<Angle, String>()
    private val qualityResults = mutableMapOf<Angle, QualityScores>()
    private var analyzer: ImageQualityAnalyzer? = null

    val imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    init {
        checkAndDownloadModel()
    }

    private fun checkAndDownloadModel() {
        if (com.palmastro.app.share.ModelManager.isModelReady(appContext)) {
            initAnalyzer()
            return
        }
        _state.update { it.copy(modelDownloading = true, modelError = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                com.palmastro.app.share.ModelManager.downloadModel(appContext)
            }
            result.fold(
                onSuccess = { initAnalyzer() },
                onFailure = {
                    _state.update {
                        it.copy(
                            modelDownloading = false,
                            modelError = "需要下載分析模型，請確認網路連線",
                        )
                    }
                },
            )
        }
    }

    private fun initAnalyzer() {
        val path = com.palmastro.app.share.ModelManager.getModelPath(appContext)
        analyzer = ImageQualityAnalyzer(appContext, path)
        _state.update { it.copy(modelReady = true, modelDownloading = false, modelError = null) }
    }

    fun retryModelDownload() {
        checkAndDownloadModel()
    }

    fun captureCurrentAngle() {
        if (_state.value.isCapturing) return
        _state.update { it.copy(isCapturing = true, coachingHint = null) }

        val angle = angles[_state.value.currentAngleIndex]
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
                        it.copy(isCapturing = false, error = "拍攝失敗：${exception.message}")
                    }
                }
            }
        )
    }

    private fun onFrameCaptured(angle: Angle, file: File) {
        viewModelScope.launch {
            val metrics = withContext(Dispatchers.Default) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    ?: return@withContext null
                val result = analyzer!!.analyze(bitmap)
                bitmap.recycle()
                result
            }

            if (metrics == null) {
                _state.update {
                    it.copy(isCapturing = false, error = "無法讀取拍攝的影像")
                }
                return@launch
            }

            val scores = qualityGate.scoreFrame(
                metrics.blur, metrics.glare, metrics.exposure,
                metrics.coverage, metrics.stability,
            )
            val gateResult = qualityGate.evaluateAngle(angle, scores)

            if (!gateResult.passed) {
                val hint = CoachingHints.getHint(gateResult.failReason ?: "blur")
                file.delete()
                _state.update { it.copy(isCapturing = false, coachingHint = hint) }
                return@launch
            }

            capturedPaths[angle] = file.absolutePath
            qualityResults[angle] = scores

            _state.update {
                it.copy(
                    isCapturing = false,
                    showFlash = true,
                    completedAngles = it.completedAngles + angle,
                    currentAngleIndex = it.currentAngleIndex + 1,
                    coachingHint = null,
                )
            }

            kotlinx.coroutines.delay(200)
            _state.update { it.copy(showFlash = false) }

            if (_state.value.currentAngleIndex >= angles.size) {
                runPipeline()
            }
        }
    }

    fun retakePreviousAngle() {
        val newIndex = (_state.value.currentAngleIndex - 1).coerceAtLeast(0)
        val angle = angles[newIndex]
        capturedPaths.remove(angle)
        qualityResults.remove(angle)
        _state.update {
            it.copy(
                currentAngleIndex = newIndex,
                completedAngles = it.completedAngles - angle,
                coachingHint = null,
            )
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun dismissCoachingHint() = _state.update { it.copy(coachingHint = null) }

    override fun onCleared() {
        super.onCleared()
        analyzer?.close()
    }

    private fun runPipeline() {
        _state.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                val profile = userRepository.get()
                    ?: throw IllegalStateException("No user profile found")

                val bestFrames = angles.associateWith { angle ->
                    val scores = qualityResults[angle]
                        ?: qualityGate.scoreFrame(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
                    BestFrameResult(angle, 0, scores, null)
                }
                val hand = if (profile.dominantHand == "left") Hand.LEFT else Hand.RIGHT

                val palmResult = PalmFeatureExtractorImpl().extract(bestFrames, hand)

                val birthday = LocalDate.ofEpochDay(profile.birthdayEpochDay)
                val minutes = profile.birthTimeMinutes
                val birthTime = if (profile.hasBirthTime && minutes != null)
                    LocalTime.of(minutes / 60, minutes % 60)
                else null
                val astroResult = AstroEngineImpl().compute(
                    birthday, birthTime, profile.birthPlaceLat, profile.birthPlaceLon
                )

                val scoringResult = ScoringEngineImpl().score(
                    ScoringInput(
                        palmResult, astroResult,
                        UserContext(hand, false),
                        "1.0.0"
                    )
                )

                val monthKey = YearMonth.now().toString()
                val prevResults = resultRepository.getRecent(1)
                val prevEntity = prevResults.firstOrNull()
                var deltaResult: DeltaResult? = null

                if (prevEntity != null && prevEntity.monthKey != monthKey) {
                    val prevMonthly = prevEntity.toMonthlyResult()
                    val avgScore = qualityResults.values
                        .map { it.composite }.average().toInt()
                    val currentMonthly = MonthlyResult(
                        resultId = "temp",
                        monthKey = monthKey,
                        scanSessionId = "s1",
                        scoringResult = scoringResult,
                        semanticPayloads = emptyMap(),
                        scanQualityScore = avgScore,
                        featureCoverage = palmResult.featureCoverage,
                        createdAt = System.currentTimeMillis()
                    )
                    deltaResult = DeltaEngineImpl().computeDelta(prevMonthly, currentMonthly)
                }

                val tone = Tone.valueOf(profile.tone.uppercase())
                val calcLevel = if (profile.calcLevel == "L2") CalcLevel.L2 else CalcLevel.L1
                val payloads = ContentComposerImpl().compose(
                    ContentInput(scoringResult, deltaResult, tone, emptySet(), calcLevel, monthKey)
                )

                val safetyFilter = SafetyFilterImpl()
                payloads.values.forEach { payload -> safetyFilter.validate(payload) }

                val avgQuality = qualityResults.values
                    .map { it.composite }.average().toInt()

                val resultId = UUID.randomUUID().toString()
                resultRepository.saveResult(
                    MonthlyResultEntity(
                        id = resultId,
                        monthKey = monthKey,
                        scanSessionId = UUID.randomUUID().toString(),
                        calcLevel = profile.calcLevel,
                        confidenceLevel = scoringResult.confidence,
                        confidenceReasonsJson = scoringResult.confidenceReasons
                            .joinToString(",", "[", "]") { "\"$it\"" },
                        domainScoresJson = scoringResult.domainScores.entries
                            .joinToString(",", "{", "}") { "\"${it.key}\":${it.value}" },
                        subdimScoresJson = "{}",
                        grade = scoringResult.grade,
                        semanticPayloadsJson = Json.encodeToString(payloads),
                        palmFeatureSummaryJson = "{}",
                        astroSignalsJson = "[]",
                        explainabilityJson = "[]",
                        rulesetVersion = scoringResult.rulesetVersion,
                        contentVersion = "1.0.0",
                        scanQualityScore = avgQuality,
                        featureCoverage = palmResult.featureCoverage,
                        scanImagePath = "scans/$monthKey",
                    )
                )

                _state.update { it.copy(isProcessing = false, isComplete = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }

    private fun MonthlyResultEntity.toMonthlyResult(): MonthlyResult {
        val scores = domainScoresJson.removeSurrounding("{", "}")
            .split(",")
            .filter { it.contains(":") }
            .associate { entry ->
                val (k, v) = entry.split(":")
                k.trim('"') to v.trim().toInt()
            }
        return MonthlyResult(
            resultId = id,
            monthKey = monthKey,
            scanSessionId = scanSessionId,
            scoringResult = ScoringResult(
                domainScores = scores,
                subdimScores = emptyMap(),
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
}
