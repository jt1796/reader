package com.reader.app.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfManager(private val pdfFile: File) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private val lock = Any()

    // 15 MB bitmap cache for rendered pages
    private val maxCacheSize = 25 * 1024 * 1024
    private val pageCache = object : LruCache<Int, Bitmap>(maxCacheSize) {
        override fun sizeOf(key: Int, value: Bitmap): Int {
            return value.byteCount
        }
    }

    val pageCount: Int
        get() = synchronized(lock) {
            pdfRenderer?.pageCount ?: 0
        }

    fun getPageAspectRatio(pageIndex: Int): Float = synchronized(lock) {
        val renderer = pdfRenderer ?: return 1f / 1.414f
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return 1f / 1.414f
        try {
            val page = renderer.openPage(pageIndex)
            val ratio = page.width.toFloat() / page.height.toFloat()
            page.close()
            ratio
        } catch (e: Exception) {
            1f / 1.414f
        }
    }

    fun open(): Boolean {
        synchronized(lock) {
            return try {
                if (!pdfFile.exists()) return false
                fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor!!)
                true
            } catch (e: Exception) {
                Log.e("PdfManager", "Error opening PDF: ${pdfFile.absolutePath}", e)
                close()
                false
            }
        }
    }

    suspend fun getPageBitmap(pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap? = withContext(Dispatchers.IO) {
        // Check cache first
        pageCache.get(pageIndex)?.let {
            if (!it.isRecycled) return@withContext it
        }

        synchronized(lock) {
            val renderer = pdfRenderer ?: return@withContext null
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

            try {
                val page = renderer.openPage(pageIndex)
                val originalWidth = page.width
                val originalHeight = page.height

                val scale = if (targetHeight > 0) {
                    minOf(
                        targetWidth.toFloat() / originalWidth,
                        targetHeight.toFloat() / originalHeight
                    ).coerceAtLeast(1.0f)
                } else {
                    (targetWidth.toFloat() / originalWidth).coerceAtLeast(1.0f)
                }

                val renderWidth = (originalWidth * scale).toInt().coerceIn(100, 2400)
                val renderHeight = (originalHeight * scale).toInt().coerceIn(100, 3600)

                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                pageCache.put(pageIndex, bitmap)
                bitmap
            } catch (e: Exception) {
                Log.e("PdfManager", "Error rendering page $pageIndex", e)
                null
            }
        }
    }

    fun close() {
        synchronized(lock) {
            try {
                pageCache.evictAll()
                pdfRenderer?.close()
                pdfRenderer = null
                fileDescriptor?.close()
                fileDescriptor = null
            } catch (e: Exception) {
                Log.e("PdfManager", "Error closing PDF resources", e)
            }
        }
    }
}
