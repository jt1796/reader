package com.reader.app.ui.reader

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reader.app.data.Book
import com.reader.app.data.BookRepository
import com.reader.app.pdf.PdfManager
import com.reader.app.ui.theme.AccentGold
import com.reader.app.ui.theme.ShelfWoodDark
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    book: Book,
    repository: BookRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Initialize PdfManager (thread-safe for concurrent page rendering across panes)
    val pdfFile = remember(book.filePath) { File(book.filePath) }
    val pdfManager = remember(pdfFile) { PdfManager(pdfFile) }

    var isReady by remember { mutableStateOf(false) }
    var totalPages by remember { mutableIntStateOf(book.pageCount) }

    // Multi-pane state
    var isPane2Open by remember { mutableStateOf(book.isPane2Open) }
    var isPane3Open by remember { mutableStateOf(book.isPane3Open) }

    // Independent page positions per pane
    var pane1Page by remember { mutableIntStateOf(book.currentPage) }
    var pane2Page by remember {
        mutableIntStateOf(if (book.isPane2Open && book.pane2Page == 0) book.currentPage else book.pane2Page)
    }
    var pane3Page by remember {
        mutableIntStateOf(if (book.isPane3Open && book.pane3Page == 0) book.currentPage else book.pane3Page)
    }
    val pane1ListState = rememberLazyListState(initialFirstVisibleItemIndex = pane1Page)
    val pane2ListState = rememberLazyListState(initialFirstVisibleItemIndex = pane2Page)
    val pane3ListState = rememberLazyListState(initialFirstVisibleItemIndex = pane3Page)

    // Draggable split ratios
    var splitRatioPrimary by remember { mutableFloatStateOf(book.splitRatioPrimary.coerceIn(0.2f, 0.8f)) }
    var splitRatioSecondary by remember { mutableFloatStateOf(book.splitRatioSecondary.coerceIn(0.2f, 0.8f)) }
    var portraitRatio1 by remember { mutableFloatStateOf(book.portraitSplitRatio1.coerceIn(0.15f, 0.5f)) }
    var portraitRatio2 by remember { mutableFloatStateOf(book.portraitSplitRatio2.coerceIn(0.5f, 0.85f)) }

    DisposableEffect(pdfManager) {
        val opened = pdfManager.open()
        if (opened) {
            totalPages = pdfManager.pageCount
            isReady = true
        }
        onDispose {
            pdfManager.close()
        }
    }

    // Save all positions on system back press
    BackHandler {
        scope.launch {
            repository.updatePaneProgress(book.id, 1, pane1Page)
            repository.updatePaneProgress(book.id, 2, pane2Page)
            repository.updatePaneProgress(book.id, 3, pane3Page)
            repository.updatePaneState(book.id, isPane2Open, isPane3Open)
            repository.updateSplitRatios(
                bookId = book.id,
                splitRatioPrimary = splitRatioPrimary,
                splitRatioSecondary = splitRatioSecondary,
                portraitSplitRatio1 = portraitRatio1,
                portraitSplitRatio2 = portraitRatio2
            )
            onBack()
        }
    }

    if (!isReady || totalPages <= 0) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = AccentGold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Opening \"${book.title}\"...", color = Color.White)
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Top Navigation Bar
        Surface(
            color = ShelfWoodDark,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isPane2Open && isPane3Open) "3 Views Active"
                            else if (isPane2Open || isPane3Open) "2 Views Active"
                            else "Page ${pane1Page + 1} of $totalPages",
                            color = AccentGold,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                repository.updatePaneProgress(book.id, 1, pane1Page)
                                repository.updatePaneProgress(book.id, 2, pane2Page)
                                repository.updatePaneProgress(book.id, 3, pane3Page)
                                repository.updatePaneState(book.id, isPane2Open, isPane3Open)
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Bookshelf",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Toggle 2nd View Chip
                    FilterChip(
                        selected = isPane2Open,
                        onClick = {
                            val newOpen = !isPane2Open
                            if (newOpen && pane2Page == 0) {
                                pane2Page = pane1Page
                                scope.launch {
                                    pane2ListState.scrollToItem(pane1Page)
                                    repository.updatePaneProgress(book.id, 2, pane1Page)
                                }
                            }
                            isPane2Open = newOpen
                            scope.launch {
                                repository.updatePaneState(book.id, newOpen, isPane3Open)
                            }
                        },
                        label = {
                            Text(
                                text = if (isPane2Open) "2nd View" else "+ 2nd View",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = if (isPane2Open) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0x33FFFFFF),
                            labelColor = Color(0xFFDDD2C6),
                            selectedContainerColor = Color(0xFF2962FF),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Toggle 3rd View Chip
                    FilterChip(
                        selected = isPane3Open,
                        onClick = {
                            val newOpen = !isPane3Open
                            if (newOpen && pane3Page == 0) {
                                pane3Page = pane1Page
                                scope.launch {
                                    pane3ListState.scrollToItem(pane1Page)
                                    repository.updatePaneProgress(book.id, 3, pane1Page)
                                }
                            }
                            isPane3Open = newOpen
                            scope.launch {
                                repository.updatePaneState(book.id, isPane2Open, newOpen)
                            }
                        },
                        label = {
                            Text(
                                text = if (isPane3Open) "3rd View" else "+ 3rd View",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = if (isPane3Open) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0x33FFFFFF),
                            labelColor = Color(0xFFDDD2C6),
                            selectedContainerColor = Color(0xFF2E7D32),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }

        // Multi-Pane Content Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val totalHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            val hasPane2 = isPane2Open
            val hasPane3 = isPane3Open

            when {
                // -------------------------------------------------------------
                // CASE 1: Only 1 View (Standard Single Page View)
                // -------------------------------------------------------------
                !hasPane2 && !hasPane3 -> {
                    PdfPaneView(
                        paneIndex = 1,
                        paneTitle = "Main Text",
                        listState = pane1ListState,
                        totalPages = totalPages,
                        pdfManager = pdfManager,
                        canClose = false,
                        onPageChanged = { page ->
                            pane1Page = page
                            scope.launch { repository.updatePaneProgress(book.id, 1, page) }
                        }
                    )
                }

                // -------------------------------------------------------------
                // CASE 2: 2 Views (Pane 1 + Pane 2, or Pane 1 + Pane 3)
                // -------------------------------------------------------------
                (hasPane2 && !hasPane3) || (!hasPane2 && hasPane3) -> {
                    val secondaryIndex = if (hasPane2) 2 else 3
                    val secondaryTitle = if (hasPane2) "2nd View" else "3rd View"

                    if (isLandscape) {
                        // Landscape: One tall view on the left, 2nd view on the right
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left: Pane 1
                            Box(
                                modifier = Modifier
                                    .weight(splitRatioPrimary)
                                    .fillMaxHeight()
                            ) {
                                PdfPaneView(
                                    paneIndex = 1,
                                    paneTitle = "Main Text",
                                    listState = pane1ListState,
                                    totalPages = totalPages,
                                    pdfManager = pdfManager,
                                    canClose = false,
                                    onPageChanged = { page ->
                                        pane1Page = page
                                        scope.launch { repository.updatePaneProgress(book.id, 1, page) }
                                    }
                                )
                            }

                            // Draggable Vertical Divider
                            DraggableVerticalDivider(
                                onDragDelta = { dx ->
                                    val deltaFraction = dx / totalWidthPx
                                    splitRatioPrimary = (splitRatioPrimary + deltaFraction).coerceIn(0.2f, 0.8f)
                                },
                                onDragEnd = {
                                    scope.launch {
                                        repository.updateSplitRatios(book.id, splitRatioPrimary = splitRatioPrimary)
                                    }
                                }
                            )

                            // Right: Secondary Pane
                            Box(
                                modifier = Modifier
                                    .weight(1f - splitRatioPrimary)
                                    .fillMaxHeight()
                            ) {
                                PdfPaneView(
                                    paneIndex = secondaryIndex,
                                    paneTitle = secondaryTitle,
                                    listState = if (secondaryIndex == 2) pane2ListState else pane3ListState,
                                    totalPages = totalPages,
                                    pdfManager = pdfManager,
                                    canClose = true,
                                    onClose = {
                                        if (secondaryIndex == 2) isPane2Open = false else isPane3Open = false
                                        scope.launch {
                                            repository.updatePaneState(book.id, isPane2Open, isPane3Open)
                                        }
                                    },
                                    onPageChanged = { page ->
                                        if (secondaryIndex == 2) {
                                            pane2Page = page
                                        } else {
                                            pane3Page = page
                                        }
                                        scope.launch { repository.updatePaneProgress(book.id, secondaryIndex, page) }
                                    }
                                )
                            }
                        }
                    } else {
                        // Portrait: Stacked on top of each other
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top: Pane 1
                            Box(
                                modifier = Modifier
                                    .weight(splitRatioPrimary)
                                    .fillMaxWidth()
                            ) {
                                PdfPaneView(
                                    paneIndex = 1,
                                    paneTitle = "Main Text",
                                    listState = pane1ListState,
                                    totalPages = totalPages,
                                    pdfManager = pdfManager,
                                    canClose = false,
                                    onPageChanged = { page ->
                                        pane1Page = page
                                        scope.launch { repository.updatePaneProgress(book.id, 1, page) }
                                    }
                                )
                            }

                            // Draggable Horizontal Divider
                            DraggableHorizontalDivider(
                                onDragDelta = { dy ->
                                    val deltaFraction = dy / totalHeightPx
                                    splitRatioPrimary = (splitRatioPrimary + deltaFraction).coerceIn(0.2f, 0.8f)
                                },
                                onDragEnd = {
                                    scope.launch {
                                        repository.updateSplitRatios(book.id, splitRatioPrimary = splitRatioPrimary)
                                    }
                                }
                            )

                            // Bottom: Secondary Pane
                            Box(
                                modifier = Modifier
                                    .weight(1f - splitRatioPrimary)
                                    .fillMaxWidth()
                            ) {
                                PdfPaneView(
                                    paneIndex = secondaryIndex,
                                    paneTitle = secondaryTitle,
                                    listState = if (secondaryIndex == 2) pane2ListState else pane3ListState,
                                    totalPages = totalPages,
                                    pdfManager = pdfManager,
                                    canClose = true,
                                    onClose = {
                                        if (secondaryIndex == 2) isPane2Open = false else isPane3Open = false
                                        scope.launch {
                                            repository.updatePaneState(book.id, isPane2Open, isPane3Open)
                                        }
                                    },
                                    onPageChanged = { page ->
                                        if (secondaryIndex == 2) {
                                            pane2Page = page
                                        } else {
                                            pane3Page = page
                                        }
                                        scope.launch { repository.updatePaneProgress(book.id, secondaryIndex, page) }
                                    }
                                )
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // CASE 3: All 3 Views Active (Pane 1 + Pane 2 + Pane 3)
                // -------------------------------------------------------------
                else -> {
                    if (isLandscape) {
                        // Landscape: One tall view on the left, 2nd view on right top, 3rd view beneath 2nd view
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left: Tall Pane 1
                            Box(
                                modifier = Modifier
                                    .weight(splitRatioPrimary)
                                    .fillMaxHeight()
                            ) {
                                PdfPaneView(
                                    paneIndex = 1,
                                    paneTitle = "Main Text",
                                    listState = pane1ListState,
                                    totalPages = totalPages,
                                    pdfManager = pdfManager,
                                    canClose = false,
                                    onPageChanged = { page ->
                                        pane1Page = page
                                        scope.launch { repository.updatePaneProgress(book.id, 1, page) }
                                    }
                                )
                            }

                            // Draggable Vertical Divider between Left and Right
                            DraggableVerticalDivider(
                                onDragDelta = { dx ->
                                    val deltaFraction = dx / totalWidthPx
                                    splitRatioPrimary = (splitRatioPrimary + deltaFraction).coerceIn(0.2f, 0.8f)
                                },
                                onDragEnd = {
                                    scope.launch {
                                        repository.updateSplitRatios(book.id, splitRatioPrimary = splitRatioPrimary)
                                    }
                                }
                            )

                            // Right column: Pane 2 on top, Pane 3 beneath it
                            Column(
                                modifier = Modifier
                                    .weight(1f - splitRatioPrimary)
                                    .fillMaxHeight()
                            ) {
                                // Top right: Pane 2
                                Box(
                                    modifier = Modifier
                                        .weight(splitRatioSecondary)
                                        .fillMaxWidth()
                                ) {
                                    PdfPaneView(
                                        paneIndex = 2,
                                        paneTitle = "2nd View",
                                        listState = pane2ListState,
                                        totalPages = totalPages,
                                        pdfManager = pdfManager,
                                        canClose = true,
                                        onClose = {
                                            isPane2Open = false
                                            scope.launch {
                                                repository.updatePaneState(book.id, isPane2Open = false, isPane3Open = true)
                                            }
                                        },
                                        onPageChanged = { page ->
                                            pane2Page = page
                                            scope.launch { repository.updatePaneProgress(book.id, 2, page) }
                                        }
                                    )
                                }

                                // Draggable Horizontal Divider between Pane 2 & 3
                                DraggableHorizontalDivider(
                                    onDragDelta = { dy ->
                                        val deltaFraction = dy / totalHeightPx
                                        splitRatioSecondary = (splitRatioSecondary + deltaFraction).coerceIn(0.2f, 0.8f)
                                    },
                                    onDragEnd = {
                                        scope.launch {
                                            repository.updateSplitRatios(book.id, splitRatioSecondary = splitRatioSecondary)
                                        }
                                    }
                                )

                                // Bottom right: Pane 3
                                Box(
                                    modifier = Modifier
                                        .weight(1f - splitRatioSecondary)
                                        .fillMaxWidth()
                                ) {
                                    PdfPaneView(
                                        paneIndex = 3,
                                        paneTitle = "3rd View",
                                        listState = pane3ListState,
                                        totalPages = totalPages,
                                        pdfManager = pdfManager,
                                        canClose = true,
                                        onClose = {
                                            isPane3Open = false
                                            scope.launch {
                                                repository.updatePaneState(book.id, isPane2Open = true, isPane3Open = false)
                                            }
                                        },
                                        onPageChanged = { page ->
                                            pane3Page = page
                                            scope.launch { repository.updatePaneProgress(book.id, 3, page) }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Portrait: Stacked on top of each other!
                        // Top: Pane 1, Middle: Pane 2, Bottom: Pane 3
                        val weight1 = portraitRatio1.coerceIn(0.15f, 0.5f)
                        val weight2 = (portraitRatio2 - portraitRatio1).coerceIn(0.15f, 0.5f)
                        val weight3 = (1f - portraitRatio2).coerceIn(0.15f, 0.5f)

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top: Pane 1
                            Box(
                                modifier = Modifier
                                    .weight(weight1)
                                    .fillMaxWidth()
                            ) {
                                PdfPaneView(
                                    paneIndex = 1,
                                    paneTitle = "Main Text",
                                    listState = pane1ListState,
                                    totalPages = totalPages,
                                    pdfManager = pdfManager,
                                    canClose = false,
                                    onPageChanged = { page ->
                                        pane1Page = page
                                        scope.launch { repository.updatePaneProgress(book.id, 1, page) }
                                    }
                                )
                            }

                            // Draggable Divider 1
                            DraggableHorizontalDivider(
                                onDragDelta = { dy ->
                                    val deltaFraction = dy / totalHeightPx
                                    val newR1 = (portraitRatio1 + deltaFraction).coerceIn(0.15f, portraitRatio2 - 0.15f)
                                    portraitRatio1 = newR1
                                },
                                onDragEnd = {
                                    scope.launch {
                                        repository.updateSplitRatios(book.id, portraitSplitRatio1 = portraitRatio1)
                                    }
                                }
                            )

                            // Middle: Pane 2
                            Box(
                                modifier = Modifier
                                    .weight(weight2)
                                    .fillMaxWidth()
                            ) {
                                PdfPaneView(
                                    paneIndex = 2,
                                    paneTitle = "2nd View",
                                    listState = pane2ListState,
                                    totalPages = totalPages,
                                    pdfManager = pdfManager,
                                    canClose = true,
                                    onClose = {
                                        isPane2Open = false
                                        scope.launch {
                                            repository.updatePaneState(book.id, isPane2Open = false, isPane3Open = true)
                                        }
                                    },
                                    onPageChanged = { page ->
                                        pane2Page = page
                                        scope.launch { repository.updatePaneProgress(book.id, 2, page) }
                                    }
                                )
                            }

                            // Draggable Divider 2
                            DraggableHorizontalDivider(
                                onDragDelta = { dy ->
                                    val deltaFraction = dy / totalHeightPx
                                    val newR2 = (portraitRatio2 + deltaFraction).coerceIn(portraitRatio1 + 0.15f, 0.85f)
                                    portraitRatio2 = newR2
                                },
                                onDragEnd = {
                                    scope.launch {
                                        repository.updateSplitRatios(book.id, portraitSplitRatio2 = portraitRatio2)
                                    }
                                }
                            )

                            // Bottom: Pane 3
                            Box(
                                modifier = Modifier
                                    .weight(weight3)
                                    .fillMaxWidth()
                            ) {
                                PdfPaneView(
                                    paneIndex = 3,
                                    paneTitle = "3rd View",
                                    listState = pane3ListState,
                                    totalPages = totalPages,
                                    pdfManager = pdfManager,
                                    canClose = true,
                                    onClose = {
                                        isPane3Open = false
                                        scope.launch {
                                            repository.updatePaneState(book.id, isPane2Open = true, isPane3Open = false)
                                        }
                                    },
                                    onPageChanged = { page ->
                                        pane3Page = page
                                        scope.launch { repository.updatePaneProgress(book.id, 3, page) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
