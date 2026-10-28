package com.reader.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.reader.app.ui.theme.AccentGold
import com.reader.app.ui.theme.ShelfWoodDark
import com.reader.app.ui.theme.ShelfWoodHighlight
import com.reader.app.ui.theme.ShelfWoodLight
import com.reader.app.ui.theme.ShelfWoodMedium

/**
 * A draggable divider between reader panes with wood grain styling and a tactile grab handle.
 */
@Composable
fun DraggableVerticalDivider(
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(18.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        onDragEnd()
                    },
                    onDragCancel = {
                        isDragging = false
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.x)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Divider beam
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(ShelfWoodDark, ShelfWoodMedium, ShelfWoodHighlight, ShelfWoodDark)
                    )
                )
        )

        // Center pill handle
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(48.dp)
                .shadow(elevation = if (isDragging) 8.dp else 4.dp, shape = RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isDragging) AccentGold else ShelfWoodLight
                ),
            contentAlignment = Alignment.Center
        ) {
            // Three grip ridges
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(if (isDragging) ShelfWoodDark else Color(0x66FFFFFF), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun DraggableHorizontalDivider(
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        onDragEnd()
                    },
                    onDragCancel = {
                        isDragging = false
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Divider beam
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(ShelfWoodDark, ShelfWoodMedium, ShelfWoodHighlight, ShelfWoodDark)
                    )
                )
        )

        // Center pill handle
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(12.dp)
                .shadow(elevation = if (isDragging) 8.dp else 4.dp, shape = RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isDragging) AccentGold else ShelfWoodLight
                ),
            contentAlignment = Alignment.Center
        ) {
            // Horizontal grip ridges
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(4.dp)
                    .background(if (isDragging) ShelfWoodDark else Color(0x66FFFFFF), RoundedCornerShape(2.dp))
            )
        }
    }
}
