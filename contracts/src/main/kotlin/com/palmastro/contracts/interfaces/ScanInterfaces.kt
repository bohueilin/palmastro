package com.palmastro.contracts.interfaces

import com.palmastro.contracts.*

interface QualityGate {
    fun scoreFrame(blur: Float, glare: Float, exposure: Float, coverage: Float, stability: Float): QualityScores
    fun selectBestFrame(frames: List<QualityScores>): Int
    fun evaluateAngle(angle: Angle, bestScore: QualityScores): AngleGateResult
}
