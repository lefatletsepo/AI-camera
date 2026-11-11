package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CameraGrid(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.25f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.dp.toPx()

        // Vertical lines (1/3 and 2/3)
        drawLine(
            color = color,
            start = Offset(w / 3f, 0f),
            end = Offset(w / 3f, h),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = color,
            start = Offset(w * 2f / 3f, 0f),
            end = Offset(w * 2f / 3f, h),
            strokeWidth = strokeWidth
        )

        // Horizontal lines (1/3 and 2/3)
        drawLine(
            color = color,
            start = Offset(0f, h / 3f),
            end = Offset(w, h / 3f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = color,
            start = Offset(0f, h * 2f / 3f),
            end = Offset(w, h * 2f / 3f),
            strokeWidth = strokeWidth
        )
    }
}
