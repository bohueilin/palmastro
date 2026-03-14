package com.palmastro.scan

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.QualityGate

class QualityGateImpl(
    private val passThreshold: Int = 60,
    private val weights: FloatArray = floatArrayOf(0.2f, 0.2f, 0.2f, 0.2f, 0.2f)
) : QualityGate {

    override fun scoreFrame(blur: Float, glare: Float, exposure: Float, coverage: Float, stability: Float): QualityScores {
        val components = floatArrayOf(blur, glare, exposure, coverage, stability)
        val weighted = components.zip(weights.toList()).sumOf { (c, w) -> (c * w).toDouble() }
        val composite = Math.round(weighted * 100).toInt().coerceIn(0, 100)
        return QualityScores(blur, glare, exposure, coverage, stability, composite)
    }

    override fun selectBestFrame(frames: List<QualityScores>): Int {
        require(frames.isNotEmpty()) { "frames must not be empty" }
        return frames.indices.maxWithOrNull(compareBy<Int>(
            { frames[it].composite },
            { (frames[it].coverage * 1000).toInt() },
            { (frames[it].blur * 1000).toInt() }
        ))!!
    }

    override fun evaluateAngle(angle: Angle, bestScore: QualityScores): AngleGateResult {
        return if (bestScore.composite >= passThreshold) {
            AngleGateResult(angle, passed = true, failReason = null)
        } else {
            val worst = findWorstComponent(bestScore)
            AngleGateResult(angle, passed = false, failReason = worst)
        }
    }

    private fun findWorstComponent(scores: QualityScores): String {
        val components = mapOf(
            "blur" to scores.blur,
            "glare" to scores.glare,
            "low_light" to scores.exposure,
            "low_coverage" to scores.coverage,
            "pose_unstable" to scores.stability
        )
        return components.minByOrNull { it.value }!!.key
    }
}
