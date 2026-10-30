package com.reader.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.reader.app.data.Book
import com.reader.app.data.BookRepository
import com.reader.app.ui.reader.PdfReaderScreen
import com.reader.app.ui.shelf.BookshelfScreen
import com.reader.app.ui.theme.BookshelfBackground
import com.reader.app.ui.theme.ReaderTheme
import com.reader.app.widget.BookshelfWidgetProvider

class MainActivity : ComponentActivity() {

    private lateinit var repository: BookRepository
    private var pendingBookIdToOpen = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = BookRepository.getInstance(applicationContext)
        BookshelfWidgetProvider.updateAllWidgets(applicationContext)
        handleIntent(intent)

        setContent {
            ReaderTheme {
                val books by repository.books.collectAsState()
                var currentBook by remember { mutableStateOf<Book?>(null) }

                // Check if an intent opened a specific book (from widget)
                val bookIdToOpen by pendingBookIdToOpen
                LaunchedEffect(bookIdToOpen, books) {
                    bookIdToOpen?.let { id ->
                        val found = books.find { it.id == id }
                        if (found != null) {
                            currentBook = found
                            pendingBookIdToOpen.value = null
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BookshelfBackground
                ) {
                    val activeBook = currentBook
                    if (activeBook != null) {
                        key(activeBook.id) {
                            PdfReaderScreen(
                                book = activeBook,
                                repository = repository,
                                onBack = {
                                    currentBook = null
                                }
                            )
                        }
                    } else {
                        BookshelfScreen(
                            repository = repository,
                            onOpenBook = { book ->
                                currentBook = book
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BookshelfWidgetProvider.updateAllWidgets(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val bookId = intent.getStringExtra(EXTRA_BOOK_ID)
        if (!bookId.isNullOrBlank()) {
            pendingBookIdToOpen.value = bookId
        }
    }

    companion object {
        const val ACTION_OPEN_BOOK = "com.reader.app.ACTION_OPEN_BOOK"
        const val ACTION_TRIGGER_IMPORT = "com.reader.app.ACTION_TRIGGER_IMPORT"
        const val EXTRA_BOOK_ID = "extra_book_id"
    }
}
