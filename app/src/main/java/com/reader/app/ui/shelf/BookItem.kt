package com.reader.app.ui.shelf

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reader.app.data.Book
import com.reader.app.ui.theme.AccentGold
import com.reader.app.ui.theme.BookPageEdge
import com.reader.app.ui.theme.BookSpineShadow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun BookItem(
    book: Book,
    onBookClick: () -> Unit,
    onRenameClick: () -> Unit,
    onResetProgressClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Load cover bitmap asynchronously
    val coverBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, book.coverPath) {
        value = withContext(Dispatchers.IO) {
            try {
                val file = File(book.coverPath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Book Container with 3D spine and shadow
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(0.70f) // Standard 1:1.4 book cover aspect ratio
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                    spotColor = Color.Black
                )
                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                .clickable { onBookClick() }
        ) {
            // Book cover image or fallback
            if (coverBitmap != null) {
                Image(
                    bitmap = coverBitmap!!,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback styled book cover
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF3B2317), Color(0xFF5D3823), Color(0xFF2B1910))
                            )
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = book.title,
                            color = Color(0xFFF3ECE0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Book binding 3D spine overlay & page edge effect
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        drawContent()

                        // Spine shadow on left edge
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0x99000000),
                                    Color(0x55000000),
                                    Color(0x22000000),
                                    Color.Transparent
                                ),
                                startX = 0f,
                                endX = size.width * 0.12f
                            ),
                            size = Size(size.width * 0.12f, size.height)
                        )

                        // Spine hinge groove line
                        drawLine(
                            color = Color(0x66000000),
                            start = Offset(size.width * 0.08f, 0f),
                            end = Offset(size.width * 0.08f, size.height),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color(0x33FFFFFF),
                            start = Offset(size.width * 0.085f, 0f),
                            end = Offset(size.width * 0.085f, size.height),
                            strokeWidth = 1f
                        )

                        // Right edge pages highlight
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x22000000),
                                    Color(0x66000000)
                                ),
                                startX = size.width * 0.96f,
                                endX = size.width
                            ),
                            topLeft = Offset(size.width * 0.96f, 0f),
                            size = Size(size.width * 0.04f, size.height)
                        )
                    }
            )

            // Reading progress ribbon at bottom of book
            if (book.pageCount > 0 && book.currentPage > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0xDD1B120E))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "p. ${book.currentPage + 1}/${book.pageCount}",
                                color = AccentGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${book.progressPercentage}%",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { book.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(1.dp)),
                            color = AccentGold,
                            trackColor = Color(0x55FFFFFF),
                        )
                    }
                }
            }

            // Options menu trigger
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            ) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(0x88000000), shape = RoundedCornerShape(12.dp))
                            .padding(1.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open Book") },
                        onClick = {
                            menuExpanded = false
                            onBookClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuExpanded = false
                            onRenameClick()
                        }
                    )
                    if (book.currentPage > 0) {
                        DropdownMenuItem(
                            text = { Text("Reset Progress (Page 1)") },
                            onClick = {
                                menuExpanded = false
                                onResetProgressClick()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }

        // Title and reading status below book
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = book.title,
            color = Color(0xFFF3ECE0),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
