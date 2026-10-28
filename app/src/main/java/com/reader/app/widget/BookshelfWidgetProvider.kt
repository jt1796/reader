package com.reader.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.reader.app.MainActivity
import com.reader.app.R
import com.reader.app.data.Book
import java.io.File
import java.io.FileReader

class BookshelfWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    @Suppress("DEPRECATION")
    internal fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.bookshelf_widget)

        // Check if books exist; if so, hide header ("Bookshelf" and "+ Add") to only show books
        val hasBooks = try {
            val file = File(context.filesDir, "books_metadata.json")
            if (file.exists()) {
                FileReader(file).use { reader ->
                    val type = object : TypeToken<List<Book>>() {}.type
                    val list: List<Book>? = Gson().fromJson(reader, type)
                    !list.isNullOrEmpty()
                }
            } else false
        } catch (e: Exception) {
            false
        }

        if (hasBooks) {
            views.setViewVisibility(R.id.widget_header, android.view.View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_header, android.view.View.VISIBLE)
        }

        // Tapping widget empty background opens the app
        val rootIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val rootPendingIntent = PendingIntent.getActivity(
            context,
            100,
            rootIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, rootPendingIntent)

        // Remote adapter for collection GridView
        val serviceIntent = Intent(context, BookshelfRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.bookshelf_widget_grid, serviceIntent)
        views.setEmptyView(R.id.bookshelf_widget_grid, R.id.bookshelf_widget_empty)

        // PendingIntent template for clicking on book items in the grid
        val bookClickIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_BOOK
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val bookClickPendingIntent = PendingIntent.getActivity(
            context,
            0,
            bookClickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.bookshelf_widget_grid, bookClickPendingIntent)

        // PendingIntent for "+ Add" button in header
        val addIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_TRIGGER_IMPORT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val addPendingIntent = PendingIntent.getActivity(
            context,
            1,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_add, addPendingIntent)

        // PendingIntent for clicking widget title
        val titleIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val titlePendingIntent = PendingIntent.getActivity(
            context,
            2,
            titleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, titlePendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        @Suppress("DEPRECATION")
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BookshelfWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                val provider = BookshelfWidgetProvider()
                for (appWidgetId in appWidgetIds) {
                    provider.updateWidget(context, appWidgetManager, appWidgetId)
                }
                // Notify collection data changed
                appWidgetManager.notifyAppWidgetViewDataChanged(
                    appWidgetIds,
                    R.id.bookshelf_widget_grid
                )
            }
        }
    }
}
