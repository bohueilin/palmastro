package com.palmastro.app.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.palmastro.app.share.ModelSource
import java.io.Closeable
import java.nio.ByteBuffer

data class ImageQualityMetrics(
    val blur: Float,
    val glare: Float,
    val exposure: Float,
    val coverage: Float,
    val stability: Float = 0.9f,
    val landmarks: List<NormalizedLandmark>? = null,
)

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
        val blur = analyzeBlur(bitmap)
        val glare = analyzeGlare(bitmap)
        val exposure = analyzeExposure(bitmap)
        val (coverage, landmarks) = analyzeCoverageAndLandmarks(bitmap)
        return ImageQualityMetrics(blur, glare, exposure, coverage, landmarks = landmarks)
    }

    private fun analyzeBlur(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val gray = FloatArray(width * height)
        for (i in pixels.indices) {
            gray[i] = 0.299f * Color.red(pixels[i]) + 0.587f * Color.green(pixels[i]) + 0.114f * Color.blue(pixels[i])
        }
        var varianceSum = 0.0
        var count = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val laplacian = 4 * gray[idx] - gray[(y-1)*width+x] - gray[(y+1)*width+x] - gray[y*width+(x-1)] - gray[y*width+(x+1)]
                varianceSum += laplacian * laplacian
                count++
            }
        }
        val variance = if (count > 0) varianceSum / count else 0.0
        return (variance / 500.0).coerceIn(0.0, 1.0).toFloat()
    }

    private fun analyzeGlare(bitmap: Bitmap): Float {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var hotPixels = 0
        for (pixel in pixels) {
            if (Color.red(pixel) > 240 && Color.green(pixel) > 240 && Color.blue(pixel) > 240) hotPixels++
        }
        return (1.0f - (hotPixels.toFloat() / pixels.size / 0.05f)).coerceIn(0.0f, 1.0f)
    }

    private fun analyzeExposure(bitmap: Bitmap): Float {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var brightnessSum = 0L
        for (pixel in pixels) { brightnessSum += (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3 }
        val mean = if (pixels.isNotEmpty()) brightnessSum.toFloat() / pixels.size else 127f
        return (1.0f - Math.abs(mean - 127f) / 127f).coerceIn(0.0f, 1.0f)
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

    override fun close() { handLandmarker.close() }
}
