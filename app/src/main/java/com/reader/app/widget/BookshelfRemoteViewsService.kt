package com.reader.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.reader.app.MainActivity
import com.reader.app.R
import com.reader.app.data.Book
import java.io.File
import java.io.FileReader

class BookshelfRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return BookshelfRemoteViewsFactory(applicationContext)
    }
}

class BookshelfRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private val gson = Gson()
    private var booksList: List<Book> = emptyList()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        val identityToken = android.os.Binder.clearCallingIdentity()
        try {
            loadData()
        } finally {
            android.os.Binder.restoreCallingIdentity(identityToken)
        }
    }

    private fun loadData() {
        try {
            val file = File(context.filesDir, "books_metadata.json")
            if (file.exists()) {
                FileReader(file).use { reader ->
                    val type = object : TypeToken<List<Book>>() {}.type
                    val list: List<Book>? = gson.fromJson(reader, type)
                    booksList = list?.sortedByDescending { it.lastReadTimestamp } ?: emptyList()
                }
            } else {
                booksList = emptyList()
            }
        } catch (e: Exception) {
            Log.e("BookshelfWidget", "Error loading books for widget", e)
            booksList = emptyList()
        }
    }

    override fun onDestroy() {
        booksList = emptyList()
    }

    override fun getCount(): Int = booksList.size

    override fun getViewAt(position: Int): RemoteViews? {
        val identityToken = android.os.Binder.clearCallingIdentity()
        try {
            if (position < 0 || position >= booksList.size) return null

            val book = booksList[position]
            val rv = RemoteViews(context.packageName, R.layout.bookshelf_widget_item)

            // Set title
            rv.setTextViewText(R.id.widget_book_title, book.title)

            // Set progress
            if (book.pageCount > 0 && book.currentPage > 0) {
                rv.setTextViewText(
                    R.id.widget_book_progress,
                    "p. ${book.currentPage + 1}/${book.pageCount} (${book.progressPercentage}%)"
                )
                rv.setViewVisibility(R.id.widget_book_progress, android.view.View.VISIBLE)
            } else if (book.pageCount > 0) {
                rv.setTextViewText(R.id.widget_book_progress, "${book.pageCount} pages")
                rv.setViewVisibility(R.id.widget_book_progress, android.view.View.VISIBLE)
            } else {
                rv.setViewVisibility(R.id.widget_book_progress, android.view.View.GONE)
            }

            // Load thumbnail safely scaled for RemoteViews memory limit
            try {
                val coverFile = File(book.coverPath)
                if (coverFile.exists()) {
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565 // memory efficient
                        inSampleSize = 2
                    }
                    val bitmap = BitmapFactory.decodeFile(coverFile.absolutePath, options)
                    if (bitmap != null) {
                        rv.setImageViewBitmap(R.id.widget_book_cover, bitmap)
                    } else {
                        rv.setImageViewResource(R.id.widget_book_cover, R.drawable.ic_bookshelf_widget)
                    }
                } else {
                    rv.setImageViewResource(R.id.widget_book_cover, R.drawable.ic_bookshelf_widget)
                }
            } catch (e: Exception) {
                Log.e("BookshelfWidget", "Error setting cover image", e)
                rv.setImageViewResource(R.id.widget_book_cover, R.drawable.ic_bookshelf_widget)
            }

            // Fill-in Intent for clicking this book
            val fillInIntent = Intent().apply {
                putExtra(MainActivity.EXTRA_BOOK_ID, book.id)
            }
            rv.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

            return rv
        } finally {
            android.os.Binder.restoreCallingIdentity(identityToken)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}
