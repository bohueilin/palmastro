package com.palmastro.contracts

import kotlinx.serialization.Serializable

/** Canonical domain taxonomy (PRD §46). Single source for the four domains. */
object Domains {
    const val CAREER = "career"
    const val WEALTH = "wealth"
    const val FAMILY = "family"
    const val HEALTH = "health"
    val ALL = listOf(CAREER, WEALTH, FAMILY, HEALTH)
}

/** Store product IDs shared across Google Play and App Store (PRD §22, §46). */
object ProductIds {
    const val CAREER_PACK = "palmastro.pack.career"
    const val WEALTH_PACK = "palmastro.pack.wealth"
    const val BUNDLE = "palmastro.pack.bundle"
    val ALL = listOf(CAREER_PACK, WEALTH_PACK, BUNDLE)
}

data class QualityScores(
    val blur: Float, val glare: Float, val exposure: Float,
    val coverage: Float, val stability: Float, val composite: Int
)

/** Normalized hand landmark point (MediaPipe/Vision coordinate space, 0..1). */
@Serializable
data class LandmarkPoint(val x: Float, val y: Float, val z: Float)

/**
 * Intensity statistics sampled along one canonical palm-line region.
 * Computed on-device from the captured frame; carries no raw imagery.
 */
@Serializable
data class LineRegionMetrics(
    val region: String,          // headline | heartline | lifeline | fateline
    val contrast: Float,         // 0..1 normalized dark-line contrast vs surrounding skin
    val continuity: Float,       // 0..1 fraction of sample path where the line is present
    val meanIntensity: Float     // 0..1 mean luminance along the path
)

/** Per-frame palm geometry + line-region measurements used for feature extraction. */
@Serializable
data class PalmMetrics(
    val landmarks: List<LandmarkPoint>,
    val lineRegions: List<LineRegionMetrics>
)

data class BestFrameResult(
    val angle: Angle, val frameIndex: Int,
    val qualityScores: QualityScores, val fileRef: String?,
    val palmMetrics: PalmMetrics? = null
)

data class AngleGateResult(val angle: Angle, val passed: Boolean, val failReason: String?)

data class ScanSessionSummary(
    val sessionId: String, val hand: Hand,
    val angleResults: Map<Angle, BestFrameResult>,
    val overallQualityScore: Int, val featureCoverage: Float,
    val totalDurationMs: Long, val totalAttempts: Int
)

data class PalmFeatures(
    val headlinePresent: Boolean,
    val heartlinePresent: Boolean,
    val lifelinePresent: Boolean,
    val fatelinePresent: Boolean,
    val headlineShape: String,
    val heartlineShape: String,
    val lifelineShape: String,
    val fatelineShape: String,
    val headlineClarity: String,
    val heartlineClarity: String,
    val lifelineClarity: String,
    val fatelineClarity: String,
    val headlineLength: String,
    val fatelineLength: String,
    val venusMountDensity: String,
    val jupiterMountDensity: String,
    val saturnMountDensity: String,
    val minorLineDensity: String,
)

data class PalmFeatureResult(
    val features: PalmFeatures,
    val featureCoverage: Float, val confidence: String,
    val extractorVersion: String
)

data class AstroSignal(
    val signalId: String, val direction: String, val magnitude: Int,
    val confidence: String, val safetyTag: String
)

data class AstroResult(
    val calcLevel: CalcLevel, val signals: List<AstroSignal>,
    val engineVersion: String
)

@Serializable
data class ExplainEntry(val signalId: String, val mapping: String, val contribution: Double)

data class ScoringInput(
    val palmFeatures: PalmFeatureResult, val astroResult: AstroResult,
    val userContext: UserContext, val rulesetVersion: String
)

data class UserContext(val dominantHand: Hand, val oneHandOnly: Boolean)

data class ScoringResult(
    val domainScores: Map<String, Int>,
    val subdimScores: Map<String, Int>,
    val grade: String, val confidence: String,
    val confidenceReasons: List<String>,
    val explainability: List<ExplainEntry>,
    val matchedBuckets: List<String>,
    val rulesetVersion: String
)

@Serializable
data class DeltaValue(val value: Int, val arrow: String)

@Serializable
data class GradeShift(val from: String, val to: String)

@Serializable
data class DeltaResult(
    val domainDeltas: Map<String, DeltaValue>,
    val subdimDeltas: Map<String, DeltaValue>,
    val gradeShift: GradeShift?,
    val comparabilityScore: Int,
    val comparabilityBucket: ComparabilityBucket,
    val prevMonthKey: String,
    val currentMonthKey: String
)

data class ContentInput(
    val scoringResult: ScoringResult,
    val deltaResult: DeltaResult?,
    val tone: Tone, val entitlements: Set<String>,
    val calcLevel: CalcLevel, val monthKey: String,
    val language: String = "en"
)

/** PRD Appendix B: interpretation = { pattern, trigger, cost }. */
@Serializable
data class Interpretation(val pattern: String, val trigger: String = "", val cost: String = "")

@Serializable
data class SemanticPayload(
    val domain: String,
    val monthKey: String,
    val calcLevel: CalcLevel,
    val confidence: String,
    val confidenceReasons: List<String> = emptyList(),
    val language: String = "en",
    val observations: List<Observation>,
    val interpretation: Interpretation,
    val blindspot: String,
    val actionToday: String,
    val actionWeek: String,
    val prompt: String,
    val safetyNotes: List<String>,
    val explainability: List<ExplainEntry>,
    val scoreCard: ScoreCard
)

@Serializable
data class Observation(val signalId: String, val displayName: String, val evidenceSummary: String)

@Serializable
data class ScoreCard(
    val totalScore: Int, val grade: String,
    val delta: DeltaValue?, val comparabilityScore: Int?,
    val subdims: Map<String, Int>
)

data class RenderedReport(val domain: String, val tone: Tone, val text: String)

data class MonthlyResult(
    val resultId: String, val monthKey: String,
    val scanSessionId: String, val scoringResult: ScoringResult,
    val semanticPayloads: Map<String, SemanticPayload>,
    val scanQualityScore: Int, val featureCoverage: Float,
    val createdAt: Long
)
