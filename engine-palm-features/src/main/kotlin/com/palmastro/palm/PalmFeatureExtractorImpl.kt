package com.palmastro.palm

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.PalmFeatureExtractor

class PalmFeatureExtractorImpl(
    private val version: String = "1.0.0"
) : PalmFeatureExtractor {

    override fun extract(bestFrames: Map<Angle, BestFrameResult>, hand: Hand): PalmFeatureResult {
        val avgQuality = bestFrames.values.map { it.qualityScores.composite }.average().toInt()

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

    private fun buildFeatures(quality: Int): PalmFeatures {
        val clear = quality >= 70
        return PalmFeatures(
            headlinePresent = true,
            heartlinePresent = true,
            lifelinePresent = true,
            fatelinePresent = quality >= 50,
            headlineShape = if (clear) "curved" else "unclear",
            heartlineShape = if (clear) "curved" else "faint",
            lifelineShape = if (clear) "curved" else "unclear",
            fatelineShape = if (quality >= 50) "straight" else "faint",
            headlineClarity = if (clear) "clear" else "moderate",
            heartlineClarity = if (clear) "clear" else "moderate",
            lifelineClarity = if (clear) "clear" else "faint",
            fatelineClarity = if (quality >= 50) "moderate" else "faint",
            headlineLength = if (clear) "long" else "medium",
            fatelineLength = if (quality >= 60) "medium" else "short",
            venusMountDensity = if (quality >= 60) "med" else "low",
            jupiterMountDensity = "med",
            saturnMountDensity = "low",
            minorLineDensity = if (quality >= 70) "med" else "low",
        )
    }

    private fun computeFeatureCoverage(features: PalmFeatures): Float {
        val totalExpected = 18
        var present = 0
        if (features.headlinePresent) present++
        if (features.heartlinePresent) present++
        if (features.lifelinePresent) present++
        if (features.fatelinePresent) present++
        if (features.headlineShape != "unclear" && features.headlineShape != "faint") present++
        if (features.heartlineShape != "unclear" && features.heartlineShape != "faint") present++
        if (features.lifelineShape != "unclear" && features.lifelineShape != "faint") present++
        if (features.fatelineShape != "unclear" && features.fatelineShape != "faint") present++
        if (features.headlineClarity != "unclear" && features.headlineClarity != "faint") present++
        if (features.heartlineClarity != "unclear" && features.heartlineClarity != "faint") present++
        if (features.lifelineClarity != "unclear" && features.lifelineClarity != "faint") present++
        if (features.fatelineClarity != "unclear" && features.fatelineClarity != "faint") present++
        if (features.headlineLength != "unclear" && features.headlineLength != "faint") present++
        if (features.fatelineLength != "unclear" && features.fatelineLength != "faint") present++
        if (features.venusMountDensity != "unclear" && features.venusMountDensity != "faint") present++
        if (features.jupiterMountDensity != "unclear" && features.jupiterMountDensity != "faint") present++
        if (features.saturnMountDensity != "unclear" && features.saturnMountDensity != "faint") present++
        if (features.minorLineDensity != "unclear" && features.minorLineDensity != "faint") present++
        return (present.toFloat() / totalExpected).coerceIn(0f, 1f)
    }

    private fun deriveConfidence(scanQuality: Int, featureCoverage: Float): String = when {
        scanQuality < 40 || featureCoverage < 0.5f -> "low"
        scanQuality < 70 || featureCoverage < 0.8f -> "med"
        else -> "high"
    }
}
