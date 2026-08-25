package com.palmastro.app.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.palmastro.app.share.ModelSource
import com.palmastro.contracts.LandmarkPoint
import com.palmastro.contracts.LineRegionMetrics
import com.palmastro.contracts.PalmMetrics
import java.io.Closeable
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

data class ImageQualityMetrics(
    val blur: Float,
    val glare: Float,
    val exposure: Float,
    val coverage: Float,
    val stability: Float = 0.9f,
    /**
     * Mean frame luminance 0..255. [exposure] is symmetric around mid-grey, so it alone
     * cannot say whether a weak score means too dark or too bright; this carries the
     * direction to the coaching layer. Neutral default keeps existing callers unbiased.
     */
    val meanBrightness: Float = 127f,
    val landmarks: List<NormalizedLandmark>? = null,
    val palmMetrics: PalmMetrics? = null,
)

/**
 * Indirection so the ViewModel never touches MediaPipe classes directly and unit
 * tests can substitute a fake. The production factory is bound in EngineModule.
 */
fun interface ImageQualityAnalyzerFactory {
    fun create(context: Context, source: ModelSource): ImageQualityAnalyzer
}

class ImageQualityAnalyzer(context: Context, source: ModelSource) : Closeable {

    private val handLandmarker: HandLandmarker

    init {
        val baseOptions = when (source) {
            is ModelSource.Asset -> BaseOptions.builder().setModelAssetPath(source.path).build()
            is ModelSource.FileSystem -> {
                val bytes = java.io.File(source.path).readBytes()
                val buffer = ByteBuffer.allocateDirect(bytes.size).apply { put(bytes, 0, bytes.size); rewind() }
                BaseOptions.builder().setModelAssetBuffer(buffer).build()
            }
            is ModelSource.NotAvailable -> error("No model available")
        }
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setNumHands(1)
            .build()
        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    fun analyze(bitmap: Bitmap): ImageQualityMetrics {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Single luminance plane reused by blur/glare/exposure and line-region sampling.
        val luma = FloatArray(width * height)
        var hotPixels = 0
        var brightnessSum = 0.0
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = Color.red(p); val g = Color.green(p); val b = Color.blue(p)
            luma[i] = 0.299f * r + 0.587f * g + 0.114f * b
            if (r > 240 && g > 240 && b > 240) hotPixels++
            brightnessSum += (r + g + b) / 3.0
        }

        val blur = analyzeBlur(luma, width, height)
        val glare = (1.0f - (hotPixels.toFloat() / pixels.size / 0.05f)).coerceIn(0.0f, 1.0f)
        val mean = if (pixels.isNotEmpty()) (brightnessSum / pixels.size).toFloat() else 127f
        val exposure = (1.0f - abs(mean - 127f) / 127f).coerceIn(0.0f, 1.0f)

        val (coverage, landmarks) = analyzeCoverageAndLandmarks(bitmap)
        val palmMetrics = landmarks?.let { buildPalmMetrics(it, luma, width, height) }

        return ImageQualityMetrics(
            blur, glare, exposure, coverage,
            meanBrightness = mean, landmarks = landmarks, palmMetrics = palmMetrics,
        )
    }

    private fun analyzeBlur(luma: FloatArray, width: Int, height: Int): Float {
        var varianceSum = 0.0
        var count = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val laplacian = 4 * luma[idx] - luma[(y - 1) * width + x] - luma[(y + 1) * width + x] -
                    luma[y * width + (x - 1)] - luma[y * width + (x + 1)]
                varianceSum += laplacian * laplacian
                count++
            }
        }
        val variance = if (count > 0) varianceSum / count else 0.0
        return (variance / 500.0).coerceIn(0.0, 1.0).toFloat()
    }

    private fun analyzeCoverageAndLandmarks(bitmap: Bitmap): Pair<Float, List<NormalizedLandmark>?> {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = handLandmarker.detect(mpImage)
        if (result.landmarks().isEmpty()) return 0.0f to null
        val landmarks = result.landmarks()[0]
        var minX = 1f; var maxX = 0f; var minY = 1f; var maxY = 0f
        for (lm in landmarks) {
            if (lm.x() < minX) minX = lm.x(); if (lm.x() > maxX) maxX = lm.x()
            if (lm.y() < minY) minY = lm.y(); if (lm.y() > maxY) maxY = lm.y()
        }
        val coverage = ((maxX - minX) * (maxY - minY) / 0.2f).coerceIn(0f, 1f)
        return coverage to landmarks
    }

    // ---------------------------------------------------------------------------------
    // Palm-line region metrics (contracts.PalmMetrics)
    // ---------------------------------------------------------------------------------

    private data class P(val x: Float, val y: Float)

    /**
     * Builds [PalmMetrics] from the 21 MediaPipe landmarks plus intensity samples taken
     * along four canonical palm-line paths on the already-computed luminance plane.
     * A palm crease reads as a local luminance dip versus the skin immediately beside it,
     * so per sample we compare the on-path value against perpendicular-offset neighbors.
     */
    private fun buildPalmMetrics(
        landmarks: List<NormalizedLandmark>,
        luma: FloatArray,
        width: Int,
        height: Int,
    ): PalmMetrics? {
        if (landmarks.size < 21) return null
        val points = landmarks.map { LandmarkPoint(it.x(), it.y(), it.z()) }
        val lm = { i: Int -> P(landmarks[i].x(), landmarks[i].y()) }

        val wrist = lm(0)
        val indexMcp = lm(5)
        val middleMcp = lm(9)
        val ringMcp = lm(13)
        val pinkyMcp = lm(17)
        val thumbCmc = lm(1)
        val thumbMcp = lm(2)

        // "Below" an MCP = shifted from the knuckle toward the wrist, into the palm.
        fun below(p: P, t: Float) = lerp(p, wrist, t)

        // Headline: from midpoint(lm5, lm9) across the palm center toward below lm17.
        val headline = sampleLine(
            start = mid(indexMcp, middleMcp),
            end = below(pinkyMcp, 0.30f),
            luma = luma, width = width, height = height,
        )
        // Heartline: arc under the MCP row, from below lm17 to below lm5.
        val heartline = sampleQuad(
            start = below(pinkyMcp, 0.18f),
            control = below(mid(middleMcp, ringMcp), 0.30f),
            end = below(indexMcp, 0.18f),
            luma = luma, width = width, height = height,
        )
        // Lifeline: arc from midpoint(lm1, lm2) curving around the thumb ball to the wrist.
        val lifelineStart = mid(thumbCmc, thumbMcp)
        val lifeline = sampleQuad(
            start = lifelineStart,
            control = lerp(mid(lifelineStart, wrist), middleMcp, 0.20f),
            end = wrist,
            luma = luma, width = width, height = height,
        )
        // Fateline: straight from the wrist up to the middle-finger MCP.
        val fateline = sampleLine(
            start = wrist,
            end = middleMcp,
            luma = luma, width = width, height = height,
        )

        return PalmMetrics(
            landmarks = points,
            lineRegions = listOf(
                headline.toRegion("headline"),
                heartline.toRegion("heartline"),
                lifeline.toRegion("lifeline"),
                fateline.toRegion("fateline"),
            ),
        )
    }

    private data class PathStats(val meanIntensity: Float, val contrast: Float, val continuity: Float)

    private fun PathStats.toRegion(name: String) =
        LineRegionMetrics(region = name, contrast = contrast, continuity = continuity, meanIntensity = meanIntensity)

    private fun mid(a: P, b: P) = P((a.x + b.x) / 2f, (a.y + b.y) / 2f)

    private fun lerp(a: P, b: P, t: Float) = P(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

    private fun sampleLine(start: P, end: P, luma: FloatArray, width: Int, height: Int): PathStats =
        samplePath(luma, width, height) { t -> lerp(start, end, t) }

    private fun sampleQuad(start: P, control: P, end: P, luma: FloatArray, width: Int, height: Int): PathStats =
        samplePath(luma, width, height) { t ->
            val u = 1f - t
            P(
                u * u * start.x + 2f * u * t * control.x + t * t * end.x,
                u * u * start.y + 2f * u * t * control.y + t * t * end.y,
            )
        }

    private fun samplePath(
        luma: FloatArray,
        width: Int,
        height: Int,
        path: (Float) -> P,
    ): PathStats {
        var intensitySum = 0f
        var dipSum = 0f
        var continuousCount = 0
        var validCount = 0

        // Perpendicular neighborhood offset: a few pixels of skin beside the crease.
        val offsetPx = max(2f, width / 100f)

        for (i in 0 until SAMPLES_PER_PATH) {
            val t = i / (SAMPLES_PER_PATH - 1f)
            val p = path(t)
            // Local tangent via a small forward difference along the path.
            val ahead = path((t + 0.02f).coerceAtMost(1f))
            val behind = path((t - 0.02f).coerceAtLeast(0f))
            var dx = (ahead.x - behind.x) * width
            var dy = (ahead.y - behind.y) * height
            val len = sqrt(dx * dx + dy * dy)
            if (len < 1e-3f) { dx = 1f; dy = 0f } else { dx /= len; dy /= len }
            // Perpendicular unit vector in pixel space.
            val px = -dy
            val py = dx

            val cx = p.x * width
            val cy = p.y * height
            val center = lumaAt(luma, width, height, cx, cy) ?: continue
            val n1 = lumaAt(luma, width, height, cx + px * offsetPx, cy + py * offsetPx)
            val n2 = lumaAt(luma, width, height, cx - px * offsetPx, cy - py * offsetPx)
            val neighborhood = when {
                n1 != null && n2 != null -> (n1 + n2) / 2f
                else -> n1 ?: n2 ?: continue
            }

            validCount++
            intensitySum += center / 255f
            val dip = ((neighborhood - center) / 255f).coerceAtLeast(0f)
            dipSum += dip
            if (dip > CONTINUITY_DIP_THRESHOLD) continuousCount++
        }

        if (validCount == 0) return PathStats(0f, 0f, 0f)
        return PathStats(
            meanIntensity = (intensitySum / validCount).coerceIn(0f, 1f),
            // Typical crease dips are shallow (a few % of full range); rescale so a
            // clearly-etched line lands near 1.0.
            contrast = (dipSum / validCount * CONTRAST_SCALE).coerceIn(0f, 1f),
            continuity = (continuousCount.toFloat() / validCount).coerceIn(0f, 1f),
        )
    }

    private fun lumaAt(luma: FloatArray, width: Int, height: Int, x: Float, y: Float): Float? {
        val xi = x.toInt()
        val yi = y.toInt()
        if (xi < 0 || yi < 0 || xi >= width || yi >= height) return null
        return luma[yi * width + xi]
    }

    override fun close() { handLandmarker.close() }

    private companion object {
        const val SAMPLES_PER_PATH = 24
        const val CONTINUITY_DIP_THRESHOLD = 0.04f
        const val CONTRAST_SCALE = 4f
    }
}
