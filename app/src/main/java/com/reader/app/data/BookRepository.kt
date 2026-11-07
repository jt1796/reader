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
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private data class LibraryBackupManifest(
    val format: String = "reader-library-backup",
    val version: Int = 1,
    val bookCount: Int
)

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

    /** Writes books, PDFs, covers, reading history, and pane configuration to one ZIP backup. */
    suspend fun exportBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val currentBooks = _books.value.toList()
            val output = context.contentResolver.openOutputStream(uri, "w")
                ?: throw IllegalStateException("Could not open backup destination")

            ZipOutputStream(output.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("backup.json"))
                zip.write(gson.toJson(LibraryBackupManifest(bookCount = currentBooks.size)).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("books_metadata.json"))
                zip.write(gson.toJson(currentBooks).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                for (book in currentBooks) {
                    require(book.id.matches(Regex("[A-Za-z0-9_-]{1,128}"))) { "Invalid book ID in library" }
                    val pdf = File(book.filePath)
                    require(pdf.isFile) { "Missing PDF for ${book.title}" }

                    zip.putNextEntry(ZipEntry("books/${book.id}.pdf"))
                    pdf.inputStream().buffered().use { it.copyTo(zip) }
                    zip.closeEntry()

                    val cover = File(book.coverPath)
                    if (cover.isFile) {
                        zip.putNextEntry(ZipEntry("covers/${book.id}.png"))
                        cover.inputStream().buffered().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
            currentBooks.size
        }
    }

    /** Validates and restores a complete backup, replacing the current bookshelf only after staging succeeds. */
    suspend fun importBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val stagingRoot = File(context.filesDir, ".restore-${UUID.randomUUID()}").apply {
                check(mkdirs()) { "Could not create restore staging folder" }
            }
            try {
                val stagedBooks = File(stagingRoot, "books").apply { mkdirs() }
                val stagedCovers = File(stagingRoot, "covers").apply { mkdirs() }
                var metadataJson: String? = null
                var manifest: LibraryBackupManifest? = null
                var expandedBytes = 0L
                val seenEntries = mutableSetOf<String>()
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Could not open backup file")

                ZipInputStream(input.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) {
                            zip.closeEntry()
                            continue
                        }
                        require(seenEntries.add(entry.name)) { "Backup contains duplicate entries" }
                        when {
                            entry.name == "backup.json" -> {
                                val data = zip.readBytesLimited(1024 * 1024)
                                manifest = gson.fromJson(String(data, Charsets.UTF_8), LibraryBackupManifest::class.java)
                            }
                            entry.name == "books_metadata.json" -> {
                                metadataJson = String(zip.readBytesLimited(32 * 1024 * 1024), Charsets.UTF_8)
                            }
                            entry.name.matches(Regex("books/[A-Za-z0-9_-]{1,128}\\.pdf")) -> {
                                val destination = File(stagedBooks, entry.name.substringAfter("books/"))
                                expandedBytes += zip.copyToLimited(destination)
                            }
                            entry.name.matches(Regex("covers/[A-Za-z0-9_-]{1,128}\\.png")) -> {
                                val destination = File(stagedCovers, entry.name.substringAfter("covers/"))
                                expandedBytes += zip.copyToLimited(destination)
                            }
                            else -> throw IllegalArgumentException("Unsupported backup entry: ${entry.name}")
                        }
                        require(expandedBytes <= MAX_BACKUP_BYTES) { "Backup is too large" }
                        zip.closeEntry()
                    }
                }

                val validManifest = requireNotNull(manifest) { "Backup manifest is missing" }
                require(validManifest.format == "reader-library-backup" && validManifest.version == 1) {
                    "Unsupported backup format"
                }
                val json = requireNotNull(metadataJson) { "Bookshelf data is missing" }
                val bookType = object : TypeToken<List<Book>>() {}.type
                val importedBooks: List<Book> = requireNotNull(gson.fromJson(json, bookType)) { "Bookshelf data is invalid" }
                require(importedBooks.size == validManifest.bookCount) { "Backup book count does not match" }
                require(importedBooks.map { it.id }.distinct().size == importedBooks.size) { "Backup contains duplicate book IDs" }

                val restoredBooks = importedBooks.map { book ->
                    require(book.id.matches(Regex("[A-Za-z0-9_-]{1,128}"))) { "Backup contains an invalid book ID" }
                    val pdf = File(stagedBooks, "${book.id}.pdf")
                    require(pdf.isFile && pdf.length() > 0L) { "Backup is missing a PDF for ${book.title}" }
                    book.copy(
                        filePath = File(booksDir, "${book.id}.pdf").absolutePath,
                        coverPath = File(coversDir, "${book.id}.png").absolutePath
                    )
                }

                val nextBooksDir = File(stagingRoot, "books-next")
                val nextCoversDir = File(stagingRoot, "covers-next")
                check(stagedBooks.renameTo(nextBooksDir)) { "Could not prepare restored PDFs" }
                check(stagedCovers.renameTo(nextCoversDir)) { "Could not prepare restored covers" }
                val nextMetadata = File(stagingRoot, "books_metadata.json")
                FileWriter(nextMetadata).use { gson.toJson(restoredBooks, it) }

                synchronized(this@BookRepository) {
                    installRestoredLibrary(stagingRoot, nextBooksDir, nextCoversDir, nextMetadata, restoredBooks)
                }
                restoredBooks.size
            } finally {
                stagingRoot.deleteRecursively()
            }
        }
    }

    private fun installRestoredLibrary(
        stagingRoot: File,
        nextBooksDir: File,
        nextCoversDir: File,
        nextMetadata: File,
        restoredBooks: List<Book>
    ) {
        val oldBooksDir = File(stagingRoot, "books-old")
        val oldCoversDir = File(stagingRoot, "covers-old")
        val oldMetadata = File(stagingRoot, "books_metadata-old.json")
        var oldBooksMoved = false
        var oldCoversMoved = false
        var oldMetadataMoved = false
        var newBooksInstalled = false
        var newCoversInstalled = false
        var newMetadataInstalled = false
        try {
            check(booksDir.renameTo(oldBooksDir)) { "Could not stage the current PDFs" }
            oldBooksMoved = true
            check(coversDir.renameTo(oldCoversDir)) { "Could not stage the current covers" }
            oldCoversMoved = true
            check(nextBooksDir.renameTo(booksDir)) { "Could not install restored PDFs" }
            newBooksInstalled = true
            check(nextCoversDir.renameTo(coversDir)) { "Could not install restored covers" }
            newCoversInstalled = true
            if (booksFile.exists()) {
                check(booksFile.renameTo(oldMetadata)) { "Could not stage current bookshelf data" }
                oldMetadataMoved = true
            }
            check(nextMetadata.renameTo(booksFile)) { "Could not install restored bookshelf data" }
            newMetadataInstalled = true
            _books.value = restoredBooks.sortedByDescending { it.lastReadTimestamp }
            notifyWidgetUpdated()
        } catch (error: Throwable) {
            if (newMetadataInstalled) booksFile.delete()
            if (oldMetadataMoved) oldMetadata.renameTo(booksFile)
            if (newCoversInstalled) coversDir.deleteRecursively()
            if (newBooksInstalled) booksDir.deleteRecursively()
            if (oldCoversMoved) oldCoversDir.renameTo(coversDir)
            if (oldBooksMoved) oldBooksDir.renameTo(booksDir)
            throw error
        }
    }

    private fun ZipInputStream.readBytesLimited(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            require(total <= limit) { "Backup metadata is too large" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun ZipInputStream.copyToLimited(destination: File): Long {
        var total = 0L
        destination.outputStream().buffered().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_BACKUP_BYTES) { "Backup entry is too large" }
                output.write(buffer, 0, count)
            }
        }
        return total
    }

    /** Applies a metadata change against the latest list so concurrent page updates cannot overwrite each other. */
    private inline fun updateBooks(transform: (MutableList<Book>) -> Boolean) {
        synchronized(this) {
            val currentList = _books.value.toMutableList()
            if (transform(currentList)) saveBooks(currentList)
        }
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
        updateBooks { currentList ->
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
            true
            } else false
        }
    }

    /**
     * Updates reading page for a specific pane (1, 2, or 3).
     */
    suspend fun updatePaneProgress(
        bookId: String,
        paneIndex: Int,
        page: Int
    ) = withContext(Dispatchers.IO) {
        updateBooks { currentList ->
            val index = currentList.indexOfFirst { it.id == bookId }
            if (index != -1) {
            val oldBook = currentList[index]
            val validPage = page.coerceIn(0, (oldBook.pageCount - 1).coerceAtLeast(0))
            val updated = when (paneIndex) {
                1 -> oldBook.copy(currentPage = validPage, lastReadTimestamp = System.currentTimeMillis())
                2 -> oldBook.copy(pane2Page = validPage)
                3 -> oldBook.copy(pane3Page = validPage)
                else -> oldBook
            }
            currentList[index] = updated
            true
            } else false
        }
    }

    /**
     * Updates open/closed visibility state for Panes 2 and 3.
     */
    suspend fun updatePaneState(
        bookId: String,
        isPane2Open: Boolean,
        isPane3Open: Boolean
    ) = withContext(Dispatchers.IO) {
        updateBooks { currentList ->
            val index = currentList.indexOfFirst { it.id == bookId }
            if (index != -1) {
            val oldBook = currentList[index]
            val updated = oldBook.copy(
                isPane2Open = isPane2Open,
                isPane3Open = isPane3Open
            )
            currentList[index] = updated
            true
            } else false
        }
    }

    /**
     * Persists draggable split divider ratios.
     */
    suspend fun updateSplitRatios(
        bookId: String,
        splitRatioPrimary: Float? = null,
        splitRatioSecondary: Float? = null,
        portraitSplitRatio1: Float? = null,
        portraitSplitRatio2: Float? = null
    ) = withContext(Dispatchers.IO) {
        updateBooks { currentList ->
            val index = currentList.indexOfFirst { it.id == bookId }
            if (index != -1) {
            val oldBook = currentList[index]
            val updated = oldBook.copy(
                splitRatioPrimary = splitRatioPrimary ?: oldBook.splitRatioPrimary,
                splitRatioSecondary = splitRatioSecondary ?: oldBook.splitRatioSecondary,
                portraitSplitRatio1 = portraitSplitRatio1 ?: oldBook.portraitSplitRatio1,
                portraitSplitRatio2 = portraitSplitRatio2 ?: oldBook.portraitSplitRatio2
            )
            currentList[index] = updated
            true
            } else false
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
        private const val MAX_BACKUP_BYTES = 8L * 1024 * 1024 * 1024

        @Volatile
        private var INSTANCE: BookRepository? = null

        fun getInstance(context: Context): BookRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BookRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
