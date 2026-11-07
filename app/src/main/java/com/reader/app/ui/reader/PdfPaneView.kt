package com.reader.app.ui.reader

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reader.app.pdf.PdfManager
import com.reader.app.ui.theme.AccentGold
import com.reader.app.ui.theme.ShelfWoodDark
import com.reader.app.ui.theme.ShelfWoodMedium
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * A continuous vertical scrolling PDF viewport with pinch-to-zoom, pan,
 * independent page state memory, and scrubber controls.
 */
@Composable
fun PdfPaneView(
    paneIndex: Int,
    paneTitle: String,
    initialPage: Int,
    totalPages: Int,
    pdfManager: PdfManager,
    canClose: Boolean,
    onClose: () -> Unit = {},
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var showSlider by remember { mutableStateOf(false) }

    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val safeInitialPage = remember(initialPage, totalPages) {
        initialPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = safeInitialPage
    )

    // Notify whenever the current visible page changes during continuous vertical scrolling
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { page ->
                onPageChanged(page)
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
    ) {
        // Mini Header bar for this pane
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ShelfWoodDark)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pane Title & Page info badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                when (paneIndex) {
                                    1 -> AccentGold
                                    2 -> Color(0xFF64B5F6)
                                    else -> Color(0xFF81C784)
                                },
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = paneTitle,
                            color = Color(0xFF1E130E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "p. ${(listState.firstVisibleItemIndex + 1).coerceAtMost(totalPages)} / $totalPages",
                        color = Color(0xFFF3ECE0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Zoom indicator badge when zoomed in
                    if (kotlin.math.abs(scale - 1f) > 0.05f) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0x55FFFFFF), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "%.1f×".format(scale),
                                color = AccentGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Controls: Previous, Slider toggle, Next, Close
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (kotlin.math.abs(scale - 1f) > 0.05f) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                panOffset = Offset.Zero
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOutMap,
                                contentDescription = "Reset Zoom (1x)",
                                tint = AccentGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (listState.firstVisibleItemIndex > 0) {
                                scope.launch {
                                    listState.animateScrollToItem(listState.firstVisibleItemIndex - 1)
                                }
                            }
                        },
                        enabled = listState.firstVisibleItemIndex > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Page",
                            tint = if (listState.firstVisibleItemIndex > 0) Color.White else Color(0x44FFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSlider = !showSlider },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LinearScale,
                            contentDescription = "Toggle Page Scrubber",
                            tint = if (showSlider) AccentGold else Color(0xFFB89E8D),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (listState.firstVisibleItemIndex < totalPages - 1) {
                                scope.launch {
                                    listState.animateScrollToItem(listState.firstVisibleItemIndex + 1)
                                }
                            }
                        },
                        enabled = listState.firstVisibleItemIndex < totalPages - 1,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Page",
                            tint = if (listState.firstVisibleItemIndex < totalPages - 1) Color.White else Color(0x44FFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (canClose) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close View",
                                tint = Color(0xFFFF8A80),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Expandable Scrubber Slider for this pane
        if (showSlider && totalPages > 1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ShelfWoodMedium)
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Slider(
                    value = (listState.firstVisibleItemIndex + 1).toFloat(),
                    onValueChange = { targetVal ->
                        val targetPage = (targetVal.toInt() - 1).coerceIn(0, totalPages - 1)
                        scope.launch {
                            listState.scrollToItem(targetPage)
                        }
                    },
                    valueRange = 1f..totalPages.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = AccentGold,
                        activeTrackColor = AccentGold,
                        inactiveTrackColor = Color(0x55FFFFFF)
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        // Continuous Scrolling Area with Pinch-to-Zoom and Pan
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1E1E1E))
                .pointerInput(Unit) {
                    // Double tap to toggle zoom between fit width and 2.2x
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.2f) {
                                scale = 1f
                                panOffset = Offset.Zero
                            } else {
                                scale = 2.2f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    // Pinch to zoom and 2D pan
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(0.5f, 4.5f)
                        scale = newScale

                        if (newScale > 1.05f) {
                            // Pan limits bounded by current zoom level
                            val maxPanX = (size.width.toFloat() * (newScale - 1f)) / 2f
                            val maxPanY = (size.height.toFloat() * (newScale - 1f)) / 2f
                            panOffset = Offset(
                                x = (panOffset.x + pan.x * newScale).coerceIn(-maxPanX, maxPanX),
                                y = (panOffset.y + pan.y * newScale).coerceIn(-maxPanY, maxPanY)
                            )
                        } else {
                            panOffset = Offset.Zero
                        }
                    }
                }
        ) {
            val paneWidthPx = constraints.maxWidth

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
            ) {
                items(totalPages, key = { it }) { pageIndex ->
                    ContinuousPdfPageItem(
                        pdfManager = pdfManager,
                        pageIndex = pageIndex,
                        targetWidthPx = paneWidthPx,
                        totalPages = totalPages
                    )
                }
            }
        }
    }
}

/**
 * Renders an individual PDF page within the continuous scroll column.
 */
@Composable
fun ContinuousPdfPageItem(
    pdfManager: PdfManager,
    pageIndex: Int,
    targetWidthPx: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    val aspectRatio = remember(pageIndex) {
        pdfManager.getPageAspectRatio(pageIndex)
    }

    val pageBitmap by produceState<Bitmap?>(initialValue = null, pageIndex, targetWidthPx) {
        value = pdfManager.getPageBitmap(pageIndex, targetWidthPx, 0)
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(2.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .shadow(4.dp, RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (pageBitmap != null) {
                Image(
                    bitmap = pageBitmap!!.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                // Page loading placeholder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = AccentGold,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Page ${pageIndex + 1} of $totalPages",
                        color = Color(0xFF888888),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
