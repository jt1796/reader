package com.reader.app

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.reader.app.data.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BookDataTest {

    @Test
    fun testBookProgressCalculations() {
        val book = Book(
            id = "test-1",
            title = "Sample PDF",
            fileName = "sample.pdf",
            filePath = "/path/to/sample.pdf",
            coverPath = "/path/to/cover.png",
            pageCount = 100,
            currentPage = 24 // 25th page
        )

        assertEquals(0.25f, book.progressFraction, 0.001f)
        assertEquals(25, book.progressPercentage)
        assertEquals("Page 25 of 100", book.progressString)
    }

    @Test
    fun testBookProgressAtStart() {
        val book = Book(
            id = "test-2",
            title = "New Book",
            fileName = "new.pdf",
            filePath = "/path/to/new.pdf",
            coverPath = "/path/to/cover.png",
            pageCount = 50,
            currentPage = 0
        )

        assertEquals(0.02f, book.progressFraction, 0.001f)
        assertEquals(2, book.progressPercentage)
        assertEquals("Page 1 of 50", book.progressString)
    }

    @Test
    fun testBookProgressAtEnd() {
        val book = Book(
            id = "test-3",
            title = "Finished Book",
            fileName = "finished.pdf",
            filePath = "/path/to/finished.pdf",
            coverPath = "/path/to/cover.png",
            pageCount = 80,
            currentPage = 79
        )

        assertEquals(1.0f, book.progressFraction, 0.001f)
        assertEquals(100, book.progressPercentage)
        assertEquals("Page 80 of 80", book.progressString)
    }

    @Test
    fun testBookJsonSerialization() {
        val gson = Gson()
        val originalList = listOf(
            Book(
                id = "id-1",
                title = "The Art of Computer Programming",
                fileName = "taocp.pdf",
                filePath = "/books/id-1.pdf",
                coverPath = "/covers/id-1.png",
                pageCount = 650,
                currentPage = 120
            ),
            Book(
                id = "id-2",
                title = "Clean Architecture",
                fileName = "ca.pdf",
                filePath = "/books/id-2.pdf",
                coverPath = "/covers/id-2.png",
                pageCount = 300,
                currentPage = 45
            )
        )

        val json = gson.toJson(originalList)
        assertNotNull(json)

        val type = object : TypeToken<List<Book>>() {}.type
        val deserialized: List<Book> = gson.fromJson(json, type)

        assertEquals(2, deserialized.size)
        assertEquals("id-1", deserialized[0].id)
        assertEquals("The Art of Computer Programming", deserialized[0].title)
        assertEquals(120, deserialized[0].currentPage)
        assertEquals(650, deserialized[0].pageCount)

        assertEquals("id-2", deserialized[1].id)
        assertEquals(45, deserialized[1].currentPage)
    }

    @Test
    fun testBookProgressEdgeCases() {
        // 0 pages book
        val emptyBook = Book(
            id = "empty",
            title = "Empty",
            fileName = "empty.pdf",
            filePath = "/path/empty.pdf",
            coverPath = "/path/empty.png",
            pageCount = 0,
            currentPage = 0
        )
        assertEquals(0f, emptyBook.progressFraction, 0.001f)
        assertEquals(0, emptyBook.progressPercentage)
        assertEquals("0 pages", emptyBook.progressString)

        // Single page book
        val singlePage = Book(
            id = "single",
            title = "Single",
            fileName = "single.pdf",
            filePath = "/path/single.pdf",
            coverPath = "/path/single.png",
            pageCount = 1,
            currentPage = 0
        )
        assertEquals(1.0f, singlePage.progressFraction, 0.001f)
        assertEquals(100, singlePage.progressPercentage)
        assertEquals("Page 1 of 1", singlePage.progressString)
    }
}
