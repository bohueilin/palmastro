package com.palmastro.app.ui.scan

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Corner-bracket framing guide for the live capture preview.
 *
 * Honest by design (PRD 12.2 "transparency beats mystery"): no analysis runs during
 * preview — quality feedback arrives from the real quality gate after capture — so
 * this overlay draws only a passive composition guide. It deliberately has no
 * simulated "detection dots" and no scanning sweep, which would imply on-screen
 * work that is not happening.
 *
 * Reduced motion (PRD §41): the brackets render at a steady alpha and the pulse
 * transition is never created, which also saves GPU work during camera preview.
 */
@Composable
fun CaptureFramingOverlay(visible: Boolean, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    if (!visible) return
    if (reduceMotion) {
        Canvas(modifier = modifier) { drawBrackets(alpha = STATIC_BRACKET_ALPHA) }
    } else {
        PulsingBracketsCanvas(modifier)
    }
}

@Composable
private fun PulsingBracketsCanvas(modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "capture_framing")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = PULSE_MIN_ALPHA, targetValue = PULSE_MAX_ALPHA,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = PULSE_DURATION_MS, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "bracket_pulse",
    )
    Canvas(modifier = modifier) { drawBrackets(alpha = pulseAlpha) }
}

private fun DrawScope.drawBrackets(alpha: Float) {
    val w = size.width
    val h = size.height
    val bracketColor = Color.White.copy(alpha = alpha)
    val bracketLen = w * BRACKET_LEN_FRACTION
    val left = w * FRAME_LEFT_FRACTION
    val right = w * FRAME_RIGHT_FRACTION
    val top = h * FRAME_TOP_FRACTION
    val bottom = h * FRAME_BOTTOM_FRACTION
    val strokeW = BRACKET_STROKE_PX

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

private const val STATIC_BRACKET_ALPHA = 0.4f
private const val PULSE_MIN_ALPHA = 0.1f
private const val PULSE_MAX_ALPHA = 0.4f
private const val PULSE_DURATION_MS = 1000

// Framing geometry as fractions of the preview size (unchanged from the original guide).
private const val FRAME_LEFT_FRACTION = 0.15f
private const val FRAME_RIGHT_FRACTION = 0.85f
private const val FRAME_TOP_FRACTION = 0.2f
private const val FRAME_BOTTOM_FRACTION = 0.75f
private const val BRACKET_LEN_FRACTION = 0.08f
private const val BRACKET_STROKE_PX = 3f
