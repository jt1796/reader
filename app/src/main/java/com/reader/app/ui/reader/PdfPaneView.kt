package com.reader.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * An independent PDF viewport pane with its own page state, zoom/pan controls, and mini scrubber.
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

    val safeInitialPage = remember(initialPage, totalPages) {
        initialPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    }

    val pagerState = rememberPagerState(
        initialPage = safeInitialPage,
        pageCount = { totalPages.coerceAtLeast(1) }
    )

    // Notify whenever this pane's page changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
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
                        text = "p. ${pagerState.currentPage + 1} / $totalPages",
                        color = Color(0xFFF3ECE0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Controls: Previous, Slider toggle, Next, Close
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Page",
                            tint = if (pagerState.currentPage > 0) Color.White else Color(0x44FFFFFF),
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
                            if (pagerState.currentPage < totalPages - 1) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage < totalPages - 1,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Page",
                            tint = if (pagerState.currentPage < totalPages - 1) Color.White else Color(0x44FFFFFF),
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
                    value = (pagerState.currentPage + 1).toFloat(),
                    onValueChange = { targetVal ->
                        val targetPage = (targetVal.toInt() - 1).coerceIn(0, totalPages - 1)
                        scope.launch {
                            pagerState.scrollToPage(targetPage)
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

        // PDF Page view area with zoom and pan
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { pageIndex ->
                PdfPageView(
                    pdfManager = pdfManager,
                    pageIndex = pageIndex,
                    onTap = { /* Tap handled */ }
                )
            }
        }
    }
}
