package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ProGold
import kotlinx.coroutines.delay

@Composable
fun FocusReticle(
    focusPoint: Offset?,
    onAnimationEnd: () -> Unit
) {
    if (focusPoint == null) return

    val scale = remember(focusPoint) { Animatable(1.6f) }
    val alpha = remember(focusPoint) { Animatable(1f) }

    LaunchedEffect(focusPoint) {
        scale.snapTo(1.6f)
        alpha.snapTo(1f)
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )
        delay(700)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300)
        )
        onAnimationEnd()
    }

    val density = LocalDensity.current
    val boxSizePx = with(density) { 64.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        val x = (focusPoint.x - (boxSizePx / 2f)).toInt()
        val y = (focusPoint.y - (boxSizePx / 2f)).toInt()

        Canvas(
            modifier = Modifier
                .offset { IntOffset(x, y) }
                .size(64.dp)
        ) {
            val strokeW = 2.dp.toPx()
            val currentScale = scale.value
            val currentAlpha = alpha.value
            val reticleColor = ProGold.copy(alpha = currentAlpha)

            val width = size.width * currentScale
            val height = size.height * currentScale
            val topLeftOffset = Offset((size.width - width) / 2f, (size.height - height) / 2f)

            // Draw center focus brackets
            drawRect(
                color = reticleColor,
                topLeft = topLeftOffset,
                size = Size(width, height),
                style = Stroke(width = strokeW)
            )

            // Draw center point
            drawCircle(
                color = reticleColor,
                radius = 2.dp.toPx(),
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }
    }
}
