package com.palmastro.app.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import java.io.Closeable

data class ImageQualityMetrics(
    val blur: Float,
    val glare: Float,
    val exposure: Float,
    val coverage: Float,
    val stability: Float = 0.9f,
)

class ImageQualityAnalyzer(context: Context) : Closeable {

    private val handLandmarker: HandLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()
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
        val coverage = analyzeCoverage(bitmap)
        return ImageQualityMetrics(blur, glare, exposure, coverage)
    }

    private fun analyzeBlur(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val gray = FloatArray(width * height)
        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            gray[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        var varianceSum = 0.0
        var count = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val laplacian = 4 * gray[idx] -
                    gray[(y - 1) * width + x] -
                    gray[(y + 1) * width + x] -
                    gray[y * width + (x - 1)] -
                    gray[y * width + (x + 1)]
                varianceSum += laplacian * laplacian
                count++
            }
        }

        val variance = if (count > 0) varianceSum / count else 0.0
        return (variance / 500.0).coerceIn(0.0, 1.0).toFloat()
    }

    private fun analyzeGlare(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var hotPixels = 0
        for (pixel in pixels) {
            if (Color.red(pixel) > 240 && Color.green(pixel) > 240 && Color.blue(pixel) > 240) {
                hotPixels++
            }
        }

        val ratio = hotPixels.toFloat() / pixels.size
        return (1.0f - (ratio / 0.05f)).coerceIn(0.0f, 1.0f)
    }

    private fun analyzeExposure(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var brightnessSum = 0L
        for (pixel in pixels) {
            brightnessSum += (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
        }

        val meanBrightness = if (pixels.isNotEmpty()) brightnessSum.toFloat() / pixels.size else 127f
        val deviation = Math.abs(meanBrightness - 127f) / 127f
        return (1.0f - deviation).coerceIn(0.0f, 1.0f)
    }

    private fun analyzeCoverage(bitmap: Bitmap): Float {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = handLandmarker.detect(mpImage)

        if (result.landmarks().isEmpty()) {
            return 0.0f
        }

        val landmarks = result.landmarks()[0]
        var minX = 1.0f
        var maxX = 0.0f
        var minY = 1.0f
        var maxY = 0.0f

        for (landmark in landmarks) {
            if (landmark.x() < minX) minX = landmark.x()
            if (landmark.x() > maxX) maxX = landmark.x()
            if (landmark.y() < minY) minY = landmark.y()
            if (landmark.y() > maxY) maxY = landmark.y()
        }

        val bboxArea = (maxX - minX) * (maxY - minY)
        return (bboxArea / 0.2f).coerceIn(0.0f, 1.0f)
    }

    override fun close() {
        handLandmarker.close()
    }
}
