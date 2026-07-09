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
            style = Stroke(width = 3f),
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
                strokeWidth = 3f,
            )
        }
        drawRoundRect(
            color = Color.White.copy(alpha = 0.15f),
            topLeft = Offset(size.width * 0.1f, size.height * 0.15f),
            size = Size(size.width * 0.8f, size.height * 0.7f),
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 2f),
        )
    }
}
