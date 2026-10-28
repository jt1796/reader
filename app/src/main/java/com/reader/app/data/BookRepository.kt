package com.reader.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.reader.app.widget.BookshelfWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.util.UUID

class BookRepository(private val context: Context) {

    private val gson = Gson()
    private val booksFile = File(context.filesDir, "books_metadata.json")
    private val booksDir = File(context.filesDir, "books").apply { if (!exists()) mkdirs() }
    private val coversDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    init {
        loadBooks()
    }

    @Synchronized
    private fun loadBooks() {
        try {
            if (booksFile.exists()) {
                FileReader(booksFile).use { reader ->
                    val type = object : TypeToken<List<Book>>() {}.type
                    val list: List<Book>? = gson.fromJson(reader, type)
                    _books.value = list?.sortedByDescending { it.lastReadTimestamp } ?: emptyList()
                }
            } else {
                _books.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Failed to load books metadata", e)
            _books.value = emptyList()
        }
    }

    @Synchronized
    private fun saveBooks(booksList: List<Book>) {
        try {
            FileWriter(booksFile).use { writer ->
                gson.toJson(booksList, writer)
            }
            _books.value = booksList.sortedByDescending { it.lastReadTimestamp }
            notifyWidgetUpdated()
        } catch (e: Exception) {
            Log.e("BookRepository", "Failed to save books metadata", e)
        }
    }

    private fun notifyWidgetUpdated() {
        try {
            BookshelfWidgetProvider.updateAllWidgets(context)
        } catch (e: Exception) {
            Log.e("BookRepository", "Failed to notify widget", e)
        }
    }

    fun getBook(id: String): Book? {
        return _books.value.find { it.id == id }
    }

    /**
     * Imports a PDF from a content Uri (SAF picker), saves it to internal storage,
     * extracts cover thumbnail from page 0, and records its metadata.
     */
    suspend fun importBook(uri: Uri): Result<Book> = withContext(Dispatchers.IO) {
        runCatching {
            val bookId = UUID.randomUUID().toString()
            var displayName = "Book_$bookId"
            var fileSize = 0L

            // 1. Query ContentResolver for file name and size
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { displayName = it }
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            // 2. Derive friendly title from filename
            val title = if (displayName.endsWith(".pdf", ignoreCase = true)) {
                displayName.substringBeforeLast(".pdf")
            } else {
                displayName
            }

            // 3. Copy file to internal app storage
            val targetPdfFile = File(booksDir, "$bookId.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetPdfFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Could not open input stream from uri: $uri")

            if (fileSize == 0L) {
                fileSize = targetPdfFile.length()
            }

            // 4. Extract total page count and page 0 cover thumbnail
            var pageCount = 0
            val targetCoverFile = File(coversDir, "$bookId.png")

            val pfd = ParcelFileDescriptor.open(targetPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pfd.use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                renderer.use { pdfRenderer ->
                    pageCount = pdfRenderer.pageCount
                    if (pageCount > 0) {
                        val page = pdfRenderer.openPage(0)
                        page.use { firstPage ->
                            // Maintain aspect ratio, width ~360px for high quality thumbnail
                            val targetWidth = 360
                            val targetHeight = ((targetWidth.toFloat() / firstPage.width) * firstPage.height).toInt().coerceAtLeast(100)
                            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                            // White background for transparent PDFs
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            firstPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            FileOutputStream(targetCoverFile).use { outStream ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 95, outStream)
                            }
                            bitmap.recycle()
                        }
                    }
                }
            }

            val newBook = Book(
                id = bookId,
                title = title,
                fileName = displayName,
                filePath = targetPdfFile.absolutePath,
                coverPath = targetCoverFile.absolutePath,
                pageCount = pageCount,
                currentPage = 0,
                scrollOffset = 0f,
                lastReadTimestamp = System.currentTimeMillis(),
                fileSizeBytes = fileSize,
                addedTimestamp = System.currentTimeMillis()
            )

            val currentList = _books.value.toMutableList()
            currentList.add(0, newBook)
            saveBooks(currentList)

            newBook
        }
    }

    /**
     * Updates reading progress for a given book.
     */
    suspend fun updateBookProgress(
        bookId: String,
        currentPage: Int,
        scrollOffset: Float = 0f
    ) = withContext(Dispatchers.IO) {
        val currentList = _books.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == bookId }
        if (index != -1) {
            val oldBook = currentList[index]
            val validPage = currentPage.coerceIn(0, (oldBook.pageCount - 1).coerceAtLeast(0))
            val updated = oldBook.copy(
                currentPage = validPage,
                scrollOffset = scrollOffset,
                lastReadTimestamp = System.currentTimeMillis()
            )
            currentList[index] = updated
            saveBooks(currentList)
        }
    }

    /**
     * Deletes a book and its associated files.
     */
    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
        val currentList = _books.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == bookId }
        if (index != -1) {
            val book = currentList.removeAt(index)
            try {
                File(book.filePath).delete()
                File(book.coverPath).delete()
            } catch (e: Exception) {
                Log.w("BookRepository", "Failed to delete files for book $bookId", e)
            }
            saveBooks(currentList)
        }
    }

    /**
     * Renames a book.
     */
    suspend fun renameBook(bookId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val currentList = _books.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == bookId }
        if (index != -1) {
            val updated = currentList[index].copy(title = newTitle.trim())
            currentList[index] = updated
            saveBooks(currentList)
        }
    }

    /**
     * Resets reading progress back to page 0.
     */
    suspend fun resetProgress(bookId: String) = withContext(Dispatchers.IO) {
        updateBookProgress(bookId, 0, 0f)
    }

    companion object {
        @Volatile
        private var INSTANCE: BookRepository? = null

        fun getInstance(context: Context): BookRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BookRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
