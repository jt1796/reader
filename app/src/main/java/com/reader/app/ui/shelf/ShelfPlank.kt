package com.reader.app.ui.shelf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reader.app.ui.theme.ShelfWoodDark
import com.reader.app.ui.theme.ShelfWoodHighlight
import com.reader.app.ui.theme.ShelfWoodLight
import com.reader.app.ui.theme.ShelfWoodMedium
import com.reader.app.ui.theme.ShelfWoodShadow
import com.reader.app.ui.theme.ShelfWoodSurface

/**
 * A photorealistic wooden shelf ledge drawn with gradients, highlight bevel, and drop shadow.
 */
@Composable
fun ShelfPlank(
    modifier: Modifier = Modifier,
    height: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val totalWidth = size.width
            val totalHeight = size.height
            val surfaceHeight = totalHeight * 0.35f
            val frontHeight = totalHeight * 0.45f
            val shadowHeight = totalHeight - surfaceHeight - frontHeight

            // 1. Shelf Top Surface (Perspective recession)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ShelfWoodShadow,
                        ShelfWoodSurface,
                        ShelfWoodMedium
                    ),
                    startY = 0f,
                    endY = surfaceHeight
                ),
                topLeft = Offset(0f, 0f),
                size = Size(totalWidth, surfaceHeight)
            )

            // Top highlight line (rim light on front edge of top surface)
            drawLine(
                color = ShelfWoodHighlight,
                start = Offset(0f, surfaceHeight),
                end = Offset(totalWidth, surfaceHeight),
                strokeWidth = 2.5f
            )

            // 2. Shelf Front Face (Vertical wood plank with gradient)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ShelfWoodLight,
                        ShelfWoodMedium,
                        ShelfWoodDark
                    ),
                    startY = surfaceHeight,
                    endY = surfaceHeight + frontHeight
                ),
                topLeft = Offset(0f, surfaceHeight),
                size = Size(totalWidth, frontHeight)
            )

            // Bottom shelf edge line
            drawLine(
                color = Color(0x33000000),
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
