package com.palmastro.app.viewmodel

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
import com.palmastro.scan.QualityGateImpl
import com.palmastro.scoring.DeltaEngineImpl
import com.palmastro.scoring.ScoringEngineImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

data class ScanState(
    val currentAngleIndex: Int = 0,
    val completedAngles: Set<Angle> = emptySet(),
    val isScanning: Boolean = false,
    val isProcessing: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val resultRepository: ResultRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ScanState())
    val state = _state.asStateFlow()

    private val angles = Angle.entries
    private val qualityGate = QualityGateImpl()

    fun startAngleScan() {
        _state.update { it.copy(isScanning = true) }
        viewModelScope.launch {
            delay(2000)
            val angle = angles[_state.value.currentAngleIndex]
            _state.update {
                it.copy(
                    isScanning = false,
                    completedAngles = it.completedAngles + angle,
                    currentAngleIndex = it.currentAngleIndex + 1,
                )
            }
            if (_state.value.currentAngleIndex >= angles.size) {
                runPipeline()
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    private fun runPipeline() {
        _state.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                val profile = userRepository.get()
                    ?: throw IllegalStateException("No user profile found")

                val scores = qualityGate.scoreFrame(0.85f, 0.9f, 0.8f, 0.88f, 0.92f)
                val bestFrames = Angle.entries.associateWith {
                    BestFrameResult(it, 0, scores, null)
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
                    val currentMonthly = MonthlyResult(
                        resultId = "temp",
                        monthKey = monthKey,
                        scanSessionId = "s1",
                        scoringResult = scoringResult,
                        semanticPayloads = emptyMap(),
                        scanQualityScore = scores.composite,
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
                        semanticPayloadsJson = "{}",
                        palmFeatureSummaryJson = "{}",
                        astroSignalsJson = "[]",
                        explainabilityJson = "[]",
                        rulesetVersion = scoringResult.rulesetVersion,
                        contentVersion = "1.0.0",
                        scanQualityScore = scores.composite,
                        featureCoverage = palmResult.featureCoverage,
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
