package com.reader.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.reader.app.MainActivity
import com.reader.app.R

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
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.bookshelf_widget)

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
                // Notify collection data changed
                appWidgetManager.notifyAppWidgetViewDataChanged(
                    appWidgetIds,
                    R.id.bookshelf_widget_grid
                )
            }
        }
    }
}
