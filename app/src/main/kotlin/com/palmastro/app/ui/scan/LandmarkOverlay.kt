package com.palmastro.app.ui.scan

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Scanning frame overlay for the live capture preview: corner brackets, palm-line
 * detection dots, and (motion only) a sweeping scan line.
 *
 * Reduced motion (PRD §41): renders the same guides as a settled static frame —
 * brackets and dots at steady alpha, no sweep — and none of the three infinite
 * transitions are even created, which also saves GPU work during camera preview.
 */
@Composable
fun ScanningOverlay(isScanning: Boolean, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    if (!isScanning) return
    if (reduceMotion) {
        Canvas(modifier = modifier) {
            drawBrackets(pulseAlpha = STATIC_BRACKET_ALPHA)
            drawDetectionDots(dotAlpha = STATIC_DOT_ALPHA)
        }
    } else {
        AnimatedScanningCanvas(modifier)
    }
}

@Composable
private fun AnimatedScanningCanvas(modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")

    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanLine",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dots",
    )

    Canvas(modifier = modifier) {
        drawScanLine(scanLineY)
        drawBrackets(pulseAlpha)
        drawDetectionDots(dotAlpha)
    }
}

private fun DrawScope.drawScanLine(scanLineY: Float) {
    val w = size.width
    val centerY = size.height / 2
    // Scanning line sweeping across the palm area
    val lineY = centerY * 0.4f + (centerY * 1.2f * scanLineY)
    drawLine(
        color = Color(0xFF7E57C2).copy(alpha = 0.6f),
        start = Offset(w * 0.15f, lineY),
        end = Offset(w * 0.85f, lineY),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
    )
    // Glow around scan line
    drawLine(
        color = Color(0xFF7E57C2).copy(alpha = 0.15f),
        start = Offset(w * 0.1f, lineY),
        end = Offset(w * 0.9f, lineY),
        strokeWidth = 20f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawBrackets(pulseAlpha: Float) {
    val w = size.width
    val h = size.height
    // Corner brackets (scanning frame)
    val bracketColor = Color.White.copy(alpha = pulseAlpha)
    val bracketLen = w * 0.08f
    val left = w * 0.15f
    val right = w * 0.85f
    val top = h * 0.2f
    val bottom = h * 0.75f
    val strokeW = 3f

    // Top-left
    drawLine(bracketColor, Offset(left, top), Offset(left + bracketLen, top), strokeW, StrokeCap.Round)
    drawLine(bracketColor, Offset(left, top), Offset(left, top + bracketLen), strokeW, StrokeCap.Round)
    // Top-right
    drawLine(bracketColor, Offset(right, top), Offset(right - bracketLen, top), strokeW, StrokeCap.Round)
    drawLine(bracketColor, Offset(right, top), Offset(right, top + bracketLen), strokeW, StrokeCap.Round)
    // Bottom-left
    drawLine(bracketColor, Offset(left, bottom), Offset(left + bracketLen, bottom), strokeW, StrokeCap.Round)
    drawLine(bracketColor, Offset(left, bottom), Offset(left, bottom - bracketLen), strokeW, StrokeCap.Round)
    // Bottom-right
    drawLine(bracketColor, Offset(right, bottom), Offset(right - bracketLen, bottom), strokeW, StrokeCap.Round)
    drawLine(bracketColor, Offset(right, bottom), Offset(right, bottom - bracketLen), strokeW, StrokeCap.Round)
}

private fun DrawScope.drawDetectionDots(dotAlpha: Float) {
    val w = size.width
    val h = size.height
    val centerX = w / 2
    val centerY = h / 2
    // Simulated detection dots on palm line paths
    val dotColor = Color(0xFF4CAF50).copy(alpha = dotAlpha)
    val dotSize = 4f
    // Head line area dots
    for (i in 0..6) {
        val t = i / 6f
        val x = centerX - w * 0.15f + (w * 0.3f * t)
        val y = centerY - h * 0.02f + (h * 0.04f * kotlin.math.sin(t * 3.14f))
        drawCircle(dotColor, dotSize, Offset(x, y))
    }
    // Heart line area dots
    for (i in 0..5) {
        val t = i / 5f
        val x = centerX - w * 0.12f + (w * 0.24f * t)
        val y = centerY - h * 0.08f + (h * 0.02f * kotlin.math.sin(t * 3.14f))
        drawCircle(dotColor, dotSize, Offset(x, y))
    }
    // Life line curve dots
    for (i in 0..5) {
        val t = i / 5f
        val x = centerX - w * 0.1f - (w * 0.08f * t)
        val y = centerY + (h * 0.15f * t)
        drawCircle(dotColor, dotSize, Offset(x, y))
    }
}

private const val STATIC_BRACKET_ALPHA = 0.4f
private const val STATIC_DOT_ALPHA = 0.8f
