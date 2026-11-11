package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ProGold
import com.example.ui.theme.PureWhite

@Composable
fun BeforeAfterSlider(
    original: Bitmap,
    enhanced: Bitmap?,
    modifier: Modifier = Modifier
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("before_after_slider")
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val newSplit = (splitFraction + (dragAmount.x / size.width)).coerceIn(0.02f, 0.98f)
                    splitFraction = newSplit
                }
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val splitX = width * splitFraction

        val origImage = remember(original) { original.asImageBitmap() }
        val enhImage = remember(enhanced) { enhanced?.asImageBitmap() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width.toInt()
            val canvasH = size.height.toInt()

            // 1. Draw Original (left side)
            clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
                drawImage(
                    image = origImage,
                    dstSize = IntSize(canvasW, canvasH)
                )
            }

            // 2. Draw Enhanced (right side)
            if (enhImage != null) {
                clipRect(left = splitX, top = 0f, right = size.width, bottom = size.height) {
                    drawImage(
                        image = enhImage,
                        dstSize = IntSize(canvasW, canvasH)
                    )
                }
            }

            // 3. Draw Divider Line
            drawLine(
                color = PureWhite,
                start = Offset(splitX, 0f),
                end = Offset(splitX, size.height),
                strokeWidth = 3.dp.toPx()
            )

            // 4. Draw Center Circular Handle
            drawCircle(
                color = PureWhite,
                radius = 16.dp.toPx(),
                center = Offset(splitX, size.height / 2f)
            )
            drawCircle(
                color = CharcoalSurface,
                radius = 12.dp.toPx(),
                center = Offset(splitX, size.height / 2f)
            )
            drawCircle(
                color = ProGold,
                radius = 5.dp.toPx(),
                center = Offset(splitX, size.height / 2f)
            )
        }

        // Top labels
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "ORIGINAL",
                color = PureWhite.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "AI PRO ENHANCED",
                color = ProGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
