package com.palmastro.contracts.interfaces

import com.palmastro.contracts.*

interface PalmFeatureExtractor {
    fun extract(bestFrames: Map<Angle, BestFrameResult>, hand: Hand): PalmFeatureResult
}

interface AstroEngine {
    fun compute(birthday: java.time.LocalDate, birthTime: java.time.LocalTime?, birthPlaceLat: Double?, birthPlaceLon: Double?): AstroResult
}

interface ScoringEngine {
    fun score(input: ScoringInput): ScoringResult
}

interface DeltaEngine {
    fun computeDelta(prev: MonthlyResult, current: MonthlyResult): DeltaResult
}

interface ContentComposer {
    fun compose(input: ContentInput): Map<String, SemanticPayload>
}

interface Renderer {
    fun render(payload: SemanticPayload, tone: Tone): RenderedReport
}

interface SafetyFilter {
    fun validate(payload: SemanticPayload): SafetyCheckResult
    fun filter(rendered: RenderedReport): RenderedReport
}

data class SafetyCheckResult(val passed: Boolean, val violations: List<String>)
