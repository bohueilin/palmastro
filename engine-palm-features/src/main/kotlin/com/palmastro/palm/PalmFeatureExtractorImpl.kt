package com.palmastro.palm

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.PalmFeatureExtractor

class PalmFeatureExtractorImpl(
    private val version: String = "1.0.0"
) : PalmFeatureExtractor {

    override fun extract(bestFrames: Map<Angle, BestFrameResult>, hand: Hand): PalmFeatureResult {
        val avgQuality = bestFrames.values.map { it.qualityScores.composite }.average().toInt()
        val avgCoverage = bestFrames.values.map { it.qualityScores.coverage }.average().toFloat()

        val features = buildFeatures(avgQuality)
        val featureCoverage = computeFeatureCoverage(features)
        val confidence = deriveConfidence(avgQuality, featureCoverage)

        return PalmFeatureResult(
            features = features,
            featureCoverage = featureCoverage,
            confidence = confidence,
            extractorVersion = version
        )
    }

    private fun buildFeatures(quality: Int): Map<String, Any> {
        val clear = quality >= 70
        return mapOf(
            "headline_present" to true,
            "heartline_present" to true,
            "lifeline_present" to true,
            "fateline_present" to (quality >= 50),
            "headline_shape" to if (clear) "curved" else "unclear",
            "heartline_shape" to if (clear) "curved" else "faint",
            "lifeline_shape" to if (clear) "curved" else "unclear",
            "fateline_shape" to if (quality >= 50) "straight" else "faint",
            "headline_clarity" to if (clear) "clear" else "moderate",
            "heartline_clarity" to if (clear) "clear" else "moderate",
            "lifeline_clarity" to if (clear) "clear" else "faint",
            "fateline_clarity" to if (quality >= 50) "moderate" else "faint",
            "headline_length" to if (clear) "long" else "medium",
            "fateline_length" to if (quality >= 60) "medium" else "short",
            "venus_mount_density" to if (quality >= 60) "med" else "low",
            "jupiter_mount_density" to "med",
            "saturn_mount_density" to "low",
            "minor_line_density" to if (quality >= 70) "med" else "low",
        )
    }

    private fun computeFeatureCoverage(features: Map<String, Any>): Float {
        val totalExpected = 18
        val present = features.count { (_, v) ->
            when (v) {
                is Boolean -> v
                is String -> v != "unclear" && v != "faint"
                else -> false
            }
        }
        return (present.toFloat() / totalExpected).coerceIn(0f, 1f)
    }

    private fun deriveConfidence(scanQuality: Int, featureCoverage: Float): String = when {
        scanQuality < 40 || featureCoverage < 0.5f -> "low"
        scanQuality < 70 || featureCoverage < 0.8f -> "med"
        else -> "high"
    }
}
