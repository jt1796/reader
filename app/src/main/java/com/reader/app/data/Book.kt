package com.reader.app.data

import java.util.UUID

/**
 * Represents a PDF book stored in the app.
 */
data class Book(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val fileName: String,
    val filePath: String,
    val coverPath: String,
    val pageCount: Int,
    val currentPage: Int = 0,
    val scrollOffset: Float = 0f,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val addedTimestamp: Long = System.currentTimeMillis(),
    // Multi-pane state for textbooks
    val pane2Page: Int = 0,
    val pane3Page: Int = 0,
    val isPane2Open: Boolean = false,
    val isPane3Open: Boolean = false,
    val splitRatioPrimary: Float = 0.5f,
    val splitRatioSecondary: Float = 0.5f,
    val portraitSplitRatio1: Float = 0.33f,
    val portraitSplitRatio2: Float = 0.66f
) {
    /**
     * Reading progress as a fraction between 0.0 and 1.0.
     */
    val progressFraction: Float
        get() = if (pageCount > 0) {
            ((currentPage + 1).toFloat() / pageCount.toFloat()).coerceIn(0f, 1f)
        } else 0f

    /**
     * Reading progress as a percentage string (e.g., "45%").
     */
    val progressPercentage: Int
        get() = (progressFraction * 100).toInt()

    /**
     * Human-readable page progress string (e.g. "Page 12 of 150").
     */
    val progressString: String
        get() = if (pageCount > 0) {
            "Page ${currentPage + 1} of $pageCount"
        } else {
            "0 pages"
        }
}
