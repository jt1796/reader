package com.reader.app.widget

import android.content.Context
import android.content.res.Configuration
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
        val widgetId = intent.getIntExtra(
            android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,
            android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return BookshelfRemoteViewsFactory(applicationContext, widgetId)
    }
}

class BookshelfRemoteViewsFactory(
    private val context: Context,
    private val widgetId: Int
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

    private fun columnCount(): Int {
        if (widgetId == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) return 1
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val options = manager.getAppWidgetOptions(widgetId)
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val widthKey = if (isPortrait) {
            android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
        } else {
            android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
        }
        val widthDp = options.getInt(widthKey, 180)
        val availableWidth = (widthDp - 16).coerceAtLeast(85)
        return (availableWidth / 85).coerceIn(1, 8)
    }

    private fun visibleRowCount(): Int {
        if (widgetId == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) return 1
        val options = android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId)
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val heightKey = if (isPortrait) {
            android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
        } else {
            android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
        }
        val heightDp = options.getInt(heightKey, options.getInt(
            android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
            110
        ))
        // Each cell is about 122dp tall, with 6dp between GridView rows.
        val gridHeight = (heightDp - 16).coerceAtLeast(1)
        return ((gridHeight + 6) / 128).coerceAtLeast(1)
    }

    override fun getCount(): Int {
        if (booksList.isEmpty()) return 0
        val columns = columnCount()
        val bookRows = (booksList.size + columns - 1) / columns
        val rows = maxOf(bookRows, visibleRowCount())
        return rows * columns
    }

    override fun getViewAt(position: Int): RemoteViews? {
        val identityToken = android.os.Binder.clearCallingIdentity()
        try {
            val rv = RemoteViews(context.packageName, R.layout.bookshelf_widget_item)

            // Fill unused cells in the last grid row with invisible books so that
            // their full-width shelf ledges join the ledge beneath the real covers.
            if (position < 0 || position >= booksList.size) {
                rv.setViewVisibility(R.id.widget_book_frame_container, android.view.View.INVISIBLE)
                rv.setViewVisibility(R.id.widget_book_progress_bar, android.view.View.GONE)
                rv.setViewVisibility(R.id.widget_book_title, android.view.View.INVISIBLE)
                return rv
            }

            val book = booksList[position]

            // Set title
            rv.setTextViewText(R.id.widget_book_title, book.title)

            // Set progress
            if (book.pageCount > 0) {
                rv.setProgressBar(
                    R.id.widget_book_progress_bar,
                    100,
                    book.progressPercentage,
                    false
                )
                rv.setViewVisibility(R.id.widget_book_progress_bar, android.view.View.VISIBLE)
            } else {
                rv.setViewVisibility(R.id.widget_book_progress_bar, android.view.View.GONE)
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
