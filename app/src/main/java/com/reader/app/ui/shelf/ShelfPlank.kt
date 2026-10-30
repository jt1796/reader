package com.reader.app.ui.shelf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A photorealistic wooden shelf ledge drawn with gradients, highlight bevel, and drop shadow.
 */
@Composable
fun ShelfPlank(
    modifier: Modifier = Modifier,
    height: Dp = 34.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val totalWidth = size.width
            val totalHeight = size.height
            val surfaceHeight = totalHeight * 0.22f
            val frontHeight = totalHeight * 0.62f
            val shadowHeight = totalHeight - surfaceHeight - frontHeight

            // 1. Shelf Top Surface (Perspective recession)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF7A461F),
                        Color(0xFFB87838),
                        Color(0xFF8C5127)
                    ),
                    startY = 0f,
                    endY = surfaceHeight
                ),
                topLeft = Offset(0f, 0f),
                size = Size(totalWidth, surfaceHeight)
            )

            // Top highlight line (rim light on front edge of top surface)
            drawLine(
                color = Color(0xFFFFD18A),
                start = Offset(0f, surfaceHeight),
                end = Offset(totalWidth, surfaceHeight),
                strokeWidth = 2.5f
            )

            // 2. Shelf Front Face (Vertical wood plank with gradient)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFC78343), Color(0xFFAA682F), Color(0xFF70401F)),
                    startY = surfaceHeight,
                    endY = surfaceHeight + frontHeight
                ),
                topLeft = Offset(0f, surfaceHeight),
                size = Size(totalWidth, frontHeight)
            )

            // Long grain lines make the front face read as a solid timber board.
            val grainYs = listOf(0.45f, 0.59f, 0.73f, 0.86f).map { surfaceHeight + totalHeight * it }
            grainYs.forEachIndexed { index, grainY ->
                val path = Path().apply {
                    moveTo(0f, grainY)
                    cubicTo(totalWidth * 0.28f, grainY - 2f, totalWidth * 0.62f, grainY + 2f, totalWidth, grainY - 1f)
                }
                drawPath(path, Color(if (index % 2 == 0) 0x44F5BD75 else 0x33451F0C), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4f))
            }

            // Small warm highlights on the front corners suggest a finished, solid edge.
            val studY = surfaceHeight + frontHeight * 0.52f
            listOf(totalWidth * 0.025f, totalWidth * 0.975f).forEach { x ->
                drawCircle(Color(0xAA5A3217), radius = 3.dp.toPx(), center = Offset(x, studY))
                drawCircle(Color(0x66FFD18A), radius = 1.dp.toPx(), center = Offset(x - 0.7.dp.toPx(), studY - 0.7.dp.toPx()))
            }

            // Bottom shelf edge line
            drawLine(
                color = Color(0xFF4B2814),
                start = Offset(0f, surfaceHeight + frontHeight),
                end = Offset(totalWidth, surfaceHeight + frontHeight),
                strokeWidth = 1.5f
            )

            // 3. Drop Shadow below shelf
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x99000000),
                        Color(0x33000000),
                        Color.Transparent
                    ),
                    startY = surfaceHeight + frontHeight,
                    endY = totalHeight
                ),
                topLeft = Offset(0f, surfaceHeight + frontHeight),
                size = Size(totalWidth, shadowHeight)
            )
        }
    }
}
