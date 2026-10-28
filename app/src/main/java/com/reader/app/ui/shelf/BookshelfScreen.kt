package com.reader.app.ui.shelf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reader.app.data.Book
import com.reader.app.data.BookRepository
import com.reader.app.ui.theme.AccentGold
import com.reader.app.ui.theme.AccentGoldLight
import com.reader.app.ui.theme.BookshelfBackground
import com.reader.app.ui.theme.ShelfWoodDark
import com.reader.app.ui.theme.ShelfWoodMedium
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    repository: BookRepository,
    onOpenBook: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    val books by repository.books.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isImporting by remember { mutableStateOf(false) }
    var bookToRename by remember { mutableStateOf<Book?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }

    // SAF File Picker for PDF documents
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty()) {
            isImporting = true
            scope.launch {
                var importedCount = 0
                for (uri in uris) {
                    val result = repository.importBook(uri)
                    if (result.isSuccess) {
                        importedCount++
                    }
                }
                isImporting = false
                if (importedCount > 0) {
                    snackbarHostState.showSnackbar(
                        message = if (importedCount == 1) "Imported 1 book to shelf" else "Imported $importedCount books to shelf"
                    )
                } else {
                    snackbarHostState.showSnackbar("Failed to import PDF file")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Bookshelf",
                                color = Color(0xFFFBF6EF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = 0.5.sp
                            )
                            if (books.isNotEmpty()) {
                                Text(
                                    text = "${books.size} ${if (books.size == 1) "book" else "books"}",
                                    color = Color(0xFFB89E8D),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = Color(0xFF23160D)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Import PDF",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Import PDF",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShelfWoodDark
                )
            )
        },
        floatingActionButton = {
            if (books.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    containerColor = AccentGold,
                    contentColor = Color(0xFF23160D),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Book", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = BookshelfBackground
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF291A13),
                            Color(0xFF1E130E),
                            Color(0xFF160E0A)
                        )
                    )
                )
        ) {
            if (isImporting) {
                // Loading overlay while parsing PDF & creating thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xAA000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(ShelfWoodDark, RoundedCornerShape(16.dp))
                            .padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = AccentGold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Importing book to shelf...",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (books.isEmpty()) {
                // Empty shelf state
                EmptyBookshelfView(
                    onImportClick = {
                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    }
                )
            } else {
                // Books grouped into shelves of 3 items per row
                val columns = 3
                val rows = books.chunked(columns)

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(rows.size) { rowIndex ->
                        val rowBooks = rows[rowIndex]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            // Row of books standing on the shelf
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                for (book in rowBooks) {
                                    BookItem(
                                        book = book,
                                        onBookClick = { onOpenBook(book) },
                                        onRenameClick = {
                                            bookToRename = book
                                            renameInput = book.title
                                        },
                                        onResetProgressClick = {
                                            scope.launch {
                                                repository.resetProgress(book.id)
                                                snackbarHostState.showSnackbar("Progress reset for \"${book.title}\"")
                                            }
                                        },
                                        onDeleteClick = {
                                            bookToDelete = book
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Empty spacers if row is incomplete
                                val remaining = columns - rowBooks.size
                                repeat(remaining) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            // Realistic wooden shelf plank beneath every row
                            ShelfPlank(modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }

    // Rename Dialog
    bookToRename?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToRename = null },
            title = { Text("Rename Book", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Book Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            scope.launch {
                                repository.renameBook(book.id, renameInput)
                            }
                        }
                        bookToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Save", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToRename = null }) {
                    Text("Cancel", color = Color(0xFFDDD2C6))
                }
            },
            containerColor = ShelfWoodDark
        )
    }

    // Delete Confirmation Dialog
    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Remove Book?", color = Color.White) },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${book.title}\" from your shelf? This will delete the local copy.",
                    color = Color(0xFFE2D6CB)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteBook(book.id)
                            snackbarHostState.showSnackbar("Removed \"${book.title}\"")
                        }
                        bookToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel", color = Color(0xFFDDD2C6))
                }
            },
            containerColor = ShelfWoodDark
        )
    }
}

@Composable
fun EmptyBookshelfView(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Shelf Plank graphic
        ShelfPlank(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(12.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .size(96.dp)
                .background(Color(0x33FFFFFF), RoundedCornerShape(48.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.LibraryBooks,
                contentDescription = null,
                tint = AccentGold,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Your Bookshelf is Empty",
            color = Color(0xFFFBF6EF),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Import PDF books from your device filesystem to start reading. Your reading position is saved automatically.",
            color = Color(0xFFB89E8D),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.85f),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onImportClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGold,
                contentColor = Color(0xFF23160D)
            ),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Import PDF from Files",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
        ShelfPlank(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(12.dp)
        )
    }
}
