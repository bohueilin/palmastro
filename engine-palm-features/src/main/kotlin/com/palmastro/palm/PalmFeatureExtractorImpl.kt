package com.palmastro.palm

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.PalmFeatureExtractor
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Palm feature extractor v2 (PRD section 16).
 *
 * Derives categorical, explainable, non-biometric features from real
 * per-frame measurements ([PalmMetrics]): 21 normalized hand landmarks plus
 * per-line-region intensity statistics (contrast / continuity / meanIntensity,
 * all 0..1) for headline, heartline, lifeline and fateline.
 *
 * Aggregation: metrics are median-aggregated across all captured angles that
 * carry palmMetrics (landmarks per coordinate, region stats per field). The
 * across-angle contrast spread (max - min) is kept as a brokenness proxy: a
 * line whose measured contrast varies strongly between angles reads as
 * chained/interrupted rather than uniformly faint.
 *
 * When no frame carries palmMetrics the extractor falls back to conservative
 * neutral features with confidence "low" (PRD 13.2: a low quality scan may
 * still produce a conservative result).
 *
 * Output values are categorical strings/booleans only - never raw
 * measurements - and are deterministic for identical inputs.
 */
class PalmFeatureExtractorImpl(
    private val version: String = "2.0.0"
) : PalmFeatureExtractor {

    override fun extract(bestFrames: Map<Angle, BestFrameResult>, hand: Hand): PalmFeatureResult {
        val avgQuality = if (bestFrames.isEmpty()) 0
        else bestFrames.values.map { it.qualityScores.composite }.average().toInt()

        // Deterministic frame order regardless of map implementation.
        val frameMetrics = Angle.entries
            .mapNotNull { bestFrames[it]?.palmMetrics }
            .filter { it.landmarks.size == LANDMARK_COUNT }

        if (frameMetrics.isEmpty()) {
            return PalmFeatureResult(
                features = conservativeFallbackFeatures(),
                featureCoverage = FALLBACK_COVERAGE,
                confidence = "low",
                extractorVersion = version
            )
        }

        val landmarks = medianLandmarks(frameMetrics)
        val geometry = PalmGeometry.from(landmarks)
        val regions = REGIONS.associateWith { region -> aggregateRegion(frameMetrics, region) }

        val headline = lineFeatures(
            regions[REGION_HEADLINE],
            curved = geometry.headlineCurvature > MCP_ARC_CURVED_THRESHOLD,
            pathRatio = geometry.headlinePathRatio, isHeartline = false
        )
        val heartline = lineFeatures(
            regions[REGION_HEARTLINE],
            curved = geometry.heartlineCurvature > MCP_ARC_CURVED_THRESHOLD,
            pathRatio = geometry.heartlinePathRatio, isHeartline = true
        )
        val lifeline = lineFeatures(
            regions[REGION_LIFELINE],
            curved = geometry.lifelineSweep > LIFELINE_SWEEP_CURVED_THRESHOLD,
            pathRatio = geometry.lifelinePathRatio, isHeartline = false
        )
        val fateline = lineFeatures(
            regions[REGION_FATELINE],
            curved = geometry.fatelineSlant >= FATELINE_SLANT_THRESHOLD,
            pathRatio = geometry.fatelinePathRatio, isHeartline = false
        )

        val features = PalmFeatures(
            headlinePresent = headline.present,
            heartlinePresent = heartline.present,
            lifelinePresent = lifeline.present,
            fatelinePresent = fateline.present,
            headlineShape = headline.shape,
            heartlineShape = heartline.shape,
            lifelineShape = lifeline.shape,
            fatelineShape = fateline.shape,
            headlineClarity = headline.clarity,
            heartlineClarity = heartline.clarity,
            lifelineClarity = lifeline.clarity,
            fatelineClarity = fateline.clarity,
            headlineLength = headline.length,
            fatelineLength = fateline.length,
            venusMountDensity = mountDensity(regions[REGION_LIFELINE]),
            jupiterMountDensity = mountDensity(regions[REGION_HEARTLINE]),
            saturnMountDensity = mountDensity(regions[REGION_FATELINE]),
            minorLineDensity = minorLineDensity(regions.values.filterNotNull()),
        )

        val featureCoverage = computeFeatureCoverage(features, regions)
        val confidence = deriveConfidence(avgQuality, featureCoverage)

        return PalmFeatureResult(
            features = features,
            featureCoverage = featureCoverage,
            confidence = confidence,
            extractorVersion = version
        )
    }

    // ----- Aggregation ------------------------------------------------------

    internal data class AggregatedRegion(
        val contrast: Float,
        val continuity: Float,
        val meanIntensity: Float,
        val contrastSpread: Float
    )

    private fun aggregateRegion(frames: List<PalmMetrics>, region: String): AggregatedRegion? {
        val samples = frames.flatMap { frame -> frame.lineRegions.filter { it.region == region } }
        if (samples.isEmpty()) return null
        val contrasts = samples.map { it.contrast }
        return AggregatedRegion(
            contrast = median(contrasts),
            continuity = median(samples.map { it.continuity }),
            meanIntensity = median(samples.map { it.meanIntensity }),
            contrastSpread = (contrasts.max() - contrasts.min())
        )
    }

    private fun medianLandmarks(frames: List<PalmMetrics>): List<LandmarkPoint> =
        (0 until LANDMARK_COUNT).map { i ->
            LandmarkPoint(
                x = median(frames.map { it.landmarks[i].x }),
                y = median(frames.map { it.landmarks[i].y }),
                z = median(frames.map { it.landmarks[i].z })
            )
        }

    internal fun median(values: List<Float>): Float {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2f
    }

    // ----- Geometry (curvature / length proxies from the 21 landmarks) ------

    /**
     * Curvature and path-length proxies derived from MediaPipe landmark
     * geometry, normalized by palm width (distance index-MCP..pinky-MCP).
     */
    internal data class PalmGeometry(
        val headlineCurvature: Float,
        val heartlineCurvature: Float,
        val lifelineSweep: Float,
        val fatelineSlant: Float,
        val headlinePathRatio: Float,
        val heartlinePathRatio: Float,
        val lifelinePathRatio: Float,
        val fatelinePathRatio: Float
    ) {
        companion object {
            fun from(l: List<LandmarkPoint>): PalmGeometry {
                val palmWidth = distance(l[5], l[17]).coerceAtLeast(MIN_PALM_WIDTH)
                // Curvature proxies: perpendicular deviation of the ring/middle
                // MCPs from the index-MCP..pinky-MCP chord (the "MCP arc").
                val headCurve = perpendicularDistance(l[13], l[5], l[17]) / palmWidth
                val heartCurve = perpendicularDistance(l[9], l[5], l[17]) / palmWidth
                // Lifeline sweep: how far the thumb MCP swings from the
                // wrist..index-MCP midline (wide thenar arc = curved lifeline).
                val lifeSweep = distance(l[2], midpoint(l[0], l[5])) / palmWidth
                // Fateline slant: deviation of the middle MCP from the
                // wrist..palm-center axis (aligned = straight fate line).
                val center = centroid(listOf(l[0], l[5], l[9], l[13], l[17]))
                val fateSlant = perpendicularDistance(l[9], l[0], center) / palmWidth
                // Path-length ratios of the canonical sampling paths (see
                // integration spec) relative to palm width.
                val headlineStart = midpoint(l[5], l[9])
                val headlineEnd = towards(l[17], l[0], 0.12f)
                val heartlineStart = towards(l[17], l[0], 0.12f)
                val heartlineEnd = towards(l[5], l[0], 0.12f)
                val lifelineStart = midpoint(l[1], l[2])
                return PalmGeometry(
                    headlineCurvature = headCurve,
                    heartlineCurvature = heartCurve,
                    lifelineSweep = lifeSweep,
                    fatelineSlant = fateSlant,
                    headlinePathRatio = distance(headlineStart, headlineEnd) / palmWidth,
                    heartlinePathRatio = distance(heartlineStart, heartlineEnd) / palmWidth,
                    lifelinePathRatio = distance(lifelineStart, l[0]) / palmWidth,
                    fatelinePathRatio = distance(l[0], l[9]) / palmWidth
                )
            }

            private fun distance(a: LandmarkPoint, b: LandmarkPoint): Float {
                val dx = a.x - b.x
                val dy = a.y - b.y
                return sqrt(dx * dx + dy * dy)
            }

            private fun midpoint(a: LandmarkPoint, b: LandmarkPoint): LandmarkPoint =
                LandmarkPoint((a.x + b.x) / 2f, (a.y + b.y) / 2f, (a.z + b.z) / 2f)

            private fun towards(from: LandmarkPoint, to: LandmarkPoint, fraction: Float): LandmarkPoint =
                LandmarkPoint(
                    from.x + (to.x - from.x) * fraction,
                    from.y + (to.y - from.y) * fraction,
                    from.z + (to.z - from.z) * fraction
                )

            private fun centroid(points: List<LandmarkPoint>): LandmarkPoint =
                LandmarkPoint(
                    points.map { it.x }.sum() / points.size,
                    points.map { it.y }.sum() / points.size,
                    points.map { it.z }.sum() / points.size
                )

            /** Perpendicular distance of [p] from the line through [a] and [b]. */
            private fun perpendicularDistance(p: LandmarkPoint, a: LandmarkPoint, b: LandmarkPoint): Float {
                val cx = b.x - a.x
                val cy = b.y - a.y
                val chordLength = sqrt(cx * cx + cy * cy).coerceAtLeast(MIN_PALM_WIDTH)
                val wx = p.x - a.x
                val wy = p.y - a.y
                return abs(cx * wy - cy * wx) / chordLength
            }
        }
    }

    // ----- Per-line categorical buckets --------------------------------------

    internal data class LineFeatures(
        val present: Boolean,
        val clarity: String,
        val shape: String,
        val length: String
    )

    internal fun lineFeatures(
        region: AggregatedRegion?,
        curved: Boolean,
        pathRatio: Float,
        isHeartline: Boolean
    ): LineFeatures {
        if (region == null || region.continuity <= PRESENCE_CONTINUITY) {
            return LineFeatures(present = false, clarity = "unclear", shape = "unclear", length = "short")
        }
        return LineFeatures(
            present = true,
            clarity = clarityBucket(region, isHeartline),
            shape = if (curved) "curved" else "straight",
            length = lengthBucket(region.continuity, pathRatio)
        )
    }

    /**
     * Clarity vocabulary (aligned with SignalResolver in engine-scoring):
     * "clear" | "medium" | "faint" | "broken" | "thin" (heartline only) |
     * "unclear" (absent lines only).
     *
     * Broken family: mid continuity with either strong contrast (a dark line
     * that keeps disappearing) or high across-angle contrast spread.
     */
    internal fun clarityBucket(region: AggregatedRegion, isHeartline: Boolean): String = when {
        region.continuity <= BROKEN_CONTINUITY &&
            (region.contrast >= BROKEN_CONTRAST || region.contrastSpread > BROKEN_SPREAD) -> "broken"
        region.continuity <= BROKEN_CONTINUITY -> "faint"
        isHeartline && region.contrast < THIN_CONTRAST -> "thin"
        region.contrast >= CLEAR_CONTRAST && region.continuity > CLEAR_CONTINUITY -> "clear"
        region.contrast >= MEDIUM_CONTRAST -> "medium"
        else -> "faint"
    }

    /** Effective length = continuity x (sampling path length / palm width). */
    internal fun lengthBucket(continuity: Float, pathRatio: Float): String {
        val normalizedLength = continuity * pathRatio
        return when {
            normalizedLength >= LENGTH_LONG -> "long"
            normalizedLength >= LENGTH_MEDIUM -> "medium"
            else -> "short"
        }
    }

    // ----- Mounts + minor lines ----------------------------------------------

    /**
     * Mount texture density from the intensity statistics of the adjacent
     * line region: darker skin (low mean intensity) with strong local
     * contrast reads as densely textured.
     */
    internal fun mountDensity(region: AggregatedRegion?): String {
        if (region == null) return "low"
        val texture = (1f - region.meanIntensity) * 0.6f + region.contrast * 0.4f
        return when {
            texture >= MOUNT_HIGH -> "high"
            texture >= MOUNT_MED -> "med"
            else -> "low"
        }
    }

    /**
     * Minor line density from residual contrast: contrast that is not
     * explained by continuous major lines (high contrast + low continuity
     * along the canonical paths implies many small crossing lines).
     */
    internal fun minorLineDensity(regions: List<AggregatedRegion>): String {
        if (regions.isEmpty()) return "med"
        val avgContrast = regions.map { it.contrast }.average().toFloat()
        val avgContinuity = regions.map { it.continuity }.average().toFloat()
        val residual = avgContrast * (1.25f - avgContinuity)
        return when {
            residual >= MINOR_HIGH -> "high"
            residual >= MINOR_MED -> "med"
            else -> "low"
        }
    }

    // ----- Coverage / confidence / fallback -----------------------------------

    private fun computeFeatureCoverage(
        features: PalmFeatures,
        regions: Map<String, AggregatedRegion?>
    ): Float {
        var present = 0
        if (features.headlinePresent) present++
        if (features.heartlinePresent) present++
        if (features.lifelinePresent) present++
        if (features.fatelinePresent) present++
        listOf(features.headlineShape, features.heartlineShape, features.lifelineShape, features.fatelineShape)
            .forEach { if (it != "unclear") present++ }
        listOf(features.headlineClarity, features.heartlineClarity, features.lifelineClarity, features.fatelineClarity)
            .forEach { if (it != "unclear") present++ }
        if (features.headlinePresent) present++ // headlineLength informative
        if (features.fatelinePresent) present++ // fatelineLength informative
        if (regions[REGION_LIFELINE] != null) present++  // venus
        if (regions[REGION_HEARTLINE] != null) present++ // jupiter
        if (regions[REGION_FATELINE] != null) present++  // saturn
        if (regions.values.any { it != null }) present++ // minor lines
        return (present.toFloat() / TOTAL_FEATURES).coerceIn(0f, 1f)
    }

    private fun deriveConfidence(scanQuality: Int, featureCoverage: Float): String = when {
        scanQuality < 40 || featureCoverage < 0.5f -> "low"
        scanQuality < 70 || featureCoverage < 0.8f -> "med"
        else -> "high"
    }

    /**
     * Neutral conservative features: nothing here matches a negative signal
     * and the mandatory "low" confidence keeps the resolver's positive
     * matches out of scoring (palm signals require at least "med").
     */
    private fun conservativeFallbackFeatures() = PalmFeatures(
        headlinePresent = true,
        heartlinePresent = true,
        lifelinePresent = true,
        fatelinePresent = false,
        headlineShape = "unclear",
        heartlineShape = "unclear",
        lifelineShape = "unclear",
        fatelineShape = "unclear",
        headlineClarity = "medium",
        heartlineClarity = "medium",
        lifelineClarity = "medium",
        fatelineClarity = "unclear",
        headlineLength = "medium",
        fatelineLength = "short",
        venusMountDensity = "med",
        jupiterMountDensity = "med",
        saturnMountDensity = "med",
        minorLineDensity = "med",
    )

    companion object {
        private const val LANDMARK_COUNT = 21
        private const val TOTAL_FEATURES = 18
        private const val FALLBACK_COVERAGE = 0.4f
        private const val MIN_PALM_WIDTH = 1e-4f

        internal const val REGION_HEADLINE = "headline"
        internal const val REGION_HEARTLINE = "heartline"
        internal const val REGION_LIFELINE = "lifeline"
        internal const val REGION_FATELINE = "fateline"
        internal val REGIONS = listOf(REGION_HEADLINE, REGION_HEARTLINE, REGION_LIFELINE, REGION_FATELINE)

        // Presence / clarity thresholds (0..1 region metrics).
        internal const val PRESENCE_CONTINUITY = 0.35f
        internal const val BROKEN_CONTINUITY = 0.6f
        internal const val BROKEN_CONTRAST = 0.5f
        internal const val BROKEN_SPREAD = 0.15f
        internal const val THIN_CONTRAST = 0.35f
        internal const val CLEAR_CONTRAST = 0.55f
        internal const val CLEAR_CONTINUITY = 0.75f
        internal const val MEDIUM_CONTRAST = 0.35f

        // Geometry thresholds (normalized by palm width).
        internal const val MCP_ARC_CURVED_THRESHOLD = 0.055f
        internal const val LIFELINE_SWEEP_CURVED_THRESHOLD = 0.30f
        internal const val FATELINE_SLANT_THRESHOLD = 0.18f

        // Length buckets (continuity x path/palm-width ratio).
        internal const val LENGTH_LONG = 0.70f
        internal const val LENGTH_MEDIUM = 0.45f

        // Mount texture buckets.
        internal const val MOUNT_HIGH = 0.55f
        internal const val MOUNT_MED = 0.35f

        // Minor line residual-contrast buckets.
        internal const val MINOR_HIGH = 0.40f
        internal const val MINOR_MED = 0.22f
    }
}
