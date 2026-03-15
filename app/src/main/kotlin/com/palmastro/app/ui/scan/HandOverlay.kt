package com.palmastro.app.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun HandOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // Palm oval
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

        // Finger lines (5 lines extending upward from palm)
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

        // Guide text frame — rounded rect border around the entire overlay area
        drawRoundRect(
            color = Color.White.copy(alpha = 0.15f),
            topLeft = Offset(size.width * 0.1f, size.height * 0.15f),
            size = Size(size.width * 0.8f, size.height * 0.7f),
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 2f),
        )
    }
}
