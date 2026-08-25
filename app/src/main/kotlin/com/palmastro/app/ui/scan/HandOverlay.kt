package com.palmastro.app.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.palmastro.app.R

@Composable
fun HandOverlay(modifier: Modifier = Modifier) {
    val desc = stringResource(R.string.scan_hand_overlay)
    Canvas(
        modifier = modifier.semantics { contentDescription = desc }
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val palmWidth = size.width * 0.45f
        val palmHeight = size.height * 0.35f
        val palmTop = centerY - palmHeight * 0.3f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset(centerX - palmWidth / 2, palmTop),
            size = Size(palmWidth, palmHeight),
            cornerRadius = CornerRadius(palmWidth * 0.4f, palmHeight * 0.4f),
            style = Stroke(width = PALM_STROKE_DP.dp.toPx()),
        )
        val fingerSpacing = palmWidth / 6
        val fingerStartX = centerX - palmWidth / 2 + fingerSpacing
        val fingerBaseY = palmTop
        val fingerLengths = listOf(0.08f, 0.12f, 0.14f, 0.12f, 0.08f)
        for (i in 0 until 5) {
            val x = fingerStartX + i * fingerSpacing
            val length = size.height * fingerLengths[i]
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(x, fingerBaseY),
                end = Offset(x, fingerBaseY - length),
                strokeWidth = FINGER_STROKE_DP.dp.toPx(),
            )
        }
        drawRoundRect(
            color = Color.White.copy(alpha = 0.15f),
            topLeft = Offset(size.width * 0.1f, size.height * 0.15f),
            size = Size(size.width * 0.8f, size.height * 0.7f),
            cornerRadius = CornerRadius(FRAME_CORNER_DP.dp.toPx(), FRAME_CORNER_DP.dp.toPx()),
            style = Stroke(width = FRAME_STROKE_DP.dp.toPx()),
        )
    }
}

// Stroke widths are dp, not raw pixels: DrawScope takes pixels, so the bare 3f these
// used to pass rendered a 1dp hairline on a 3x screen and a different weight on every
// device — on the only positioning help the capture flow offers.
private const val PALM_STROKE_DP = 3f
private const val FINGER_STROKE_DP = 3f
private const val FRAME_STROKE_DP = 2f
// 8dp ≈ the 24px radius this frame has always drawn at typical densities.
private const val FRAME_CORNER_DP = 8f
