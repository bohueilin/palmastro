package com.palmastro.contracts

data class QualityScores(
    val blur: Float, val glare: Float, val exposure: Float,
    val coverage: Float, val stability: Float, val composite: Int
)

data class BestFrameResult(
    val angle: Angle, val frameIndex: Int,
    val qualityScores: QualityScores, val fileRef: String?
)

data class AngleGateResult(val angle: Angle, val passed: Boolean, val failReason: String?)

data class ScanSessionSummary(
    val sessionId: String, val hand: Hand,
    val angleResults: Map<Angle, BestFrameResult>,
    val overallQualityScore: Int, val featureCoverage: Float,
    val totalDurationMs: Long, val totalAttempts: Int
)

data class PalmFeatureResult(
    val features: Map<String, Any>,
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

data class ExplainEntry(val signalId: String, val mappingZh: String, val contribution: Double)

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

data class DeltaValue(val value: Int, val arrow: String)

data class GradeShift(val from: String, val to: String)

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
    val calcLevel: CalcLevel, val monthKey: String
)

data class SemanticPayload(
    val domain: String,
    val monthKey: String,
    val calcLevel: CalcLevel,
    val confidence: String,
    val observations: List<Observation>,
    val interpretationZh: String,
    val blindspotZh: String,
    val actionTodayZh: String,
    val actionWeekZh: String,
    val promptZh: String,
    val safetyNotesZh: List<String>,
    val explainability: List<ExplainEntry>,
    val scoreCard: ScoreCard
)

data class Observation(val signalId: String, val displayNameZh: String, val evidenceSummaryZh: String)

data class ScoreCard(
    val totalScore: Int, val grade: String,
    val delta: DeltaValue?, val comparabilityScore: Int?,
    val subdims: Map<String, Int>
)

data class RenderedReport(val domain: String, val tone: Tone, val htmlZh: String)

data class MonthlyResult(
    val resultId: String, val monthKey: String,
    val scanSessionId: String, val scoringResult: ScoringResult,
    val semanticPayloads: Map<String, SemanticPayload>,
    val scanQualityScore: Int, val featureCoverage: Float,
    val createdAt: Long
)
