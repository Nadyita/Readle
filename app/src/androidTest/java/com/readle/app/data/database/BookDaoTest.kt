package com.readle.app.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.readle.app.data.model.BookEntity
import com.readle.app.data.model.ReadingCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDaoTest {

    private lateinit var database: ReadleDatabase
    private lateinit var bookDao: BookDao

    private val testBook1 = BookEntity(
        id = 0,
        title = "Test Book 1",
        author = "Test Author 1",
        description = "Description 1",
        publisher = "Publisher 1",
        publishDate = "2024",
        language = "de",
        originalLanguage = "en",
        series = "Series 1",
        seriesNumber = 1,
        isbn = "1234567890",
        coverPath = null,
        rating = 5,
        category = ReadingCategory.WANT_TO_READ,
        isRead = false
    )

    private val testBook2 = BookEntity(
        id = 0,
        title = "Test Book 2",
        author = "Test Author 2",
        description = "Description 2",
        publisher = "Publisher 2",
        publishDate = "2023",
        language = "en",
        originalLanguage = null,
        series = null,
        seriesNumber = null,
        isbn = "0987654321",
        coverPath = null,
        rating = 4,
        category = ReadingCategory.READ,
        isRead = true
    )

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReadleDatabase::class.java
        ).allowMainThreadQueries().build()

        bookDao = database.bookDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetBook() = runBlocking {
        val id = bookDao.insertBook(testBook1)
        val book = bookDao.getBookById(id)

        assertNotNull(book)
        assertEquals(testBook1.title, book?.title)
        assertEquals(testBook1.author, book?.author)
    }

    @Test
    fun getAllBooks() = runBlocking {
        bookDao.insertBook(testBook1)
        bookDao.insertBook(testBook2)

        val books = bookDao.getAllBooks().first()

        assertEquals(2, books.size)
    }

    @Test
    fun getBooksByCategory() = runBlocking {
        bookDao.insertBook(testBook1)
        bookDao.insertBook(testBook2)

        val wantToReadBooks = bookDao.getBooksByCategory(ReadingCategory.WANT_TO_READ).first()
        val readBooks = bookDao.getBooksByCategory(ReadingCategory.READ).first()

        assertEquals(1, wantToReadBooks.size)
        assertEquals(1, readBooks.size)
        assertEquals(testBook1.title, wantToReadBooks[0].title)
        assertEquals(testBook2.title, readBooks[0].title)
    }

    @Test
    fun getBookByIsbn() = runBlocking {
        bookDao.insertBook(testBook1)

        val book = bookDao.getBookByIsbn("1234567890")

        assertNotNull(book)
        assertEquals(testBook1.title, book?.title)
    }

    @Test
    fun searchBooks() = runBlocking {
        bookDao.insertBook(testBook1)
        bookDao.insertBook(testBook2)

        val results = bookDao.searchBooks("Book 1").first()

        assertEquals(1, results.size)
        assertEquals(testBook1.title, results[0].title)
    }

    @Test
    fun updateBook() = runBlocking {
        val id = bookDao.insertBook(testBook1)
        val book = bookDao.getBookById(id)
        assertNotNull(book)

        val updatedBook = book!!.copy(title = "Updated Title", rating = 3)
        bookDao.updateBook(updatedBook)

        val retrievedBook = bookDao.getBookById(id)
        assertEquals("Updated Title", retrievedBook?.title)
        assertEquals(3, retrievedBook?.rating)
    }

    @Test
    fun deleteBook() = runBlocking {
        val id = bookDao.insertBook(testBook1)
        var book = bookDao.getBookById(id)
        assertNotNull(book)

        bookDao.deleteBook(book!!)
        book = bookDao.getBookById(id)

        assertNull(book)
    }

    @Test
    fun deleteBooks() = runBlocking {
        val id1 = bookDao.insertBook(testBook1)
        val id2 = bookDao.insertBook(testBook2)

        bookDao.deleteBooks(listOf(id1, id2))

        val books = bookDao.getAllBooks().first()
        assertEquals(0, books.size)
    }

    @Test
    fun deleteAllBooks() = runBlocking {
        bookDao.insertBook(testBook1)
        bookDao.insertBook(testBook2)

        bookDao.deleteAllBooks()

        val books = bookDao.getAllBooks().first()
        assertEquals(0, books.size)
    }

    @Test
    fun getBookCountByCategory() = runBlocking {
        bookDao.insertBook(testBook1)
        bookDao.insertBook(testBook2)

        val wantToReadCount = bookDao.getBookCountByCategory(ReadingCategory.WANT_TO_READ).first()
        val readCount = bookDao.getBookCountByCategory(ReadingCategory.READ).first()

        assertEquals(1, wantToReadCount)
        assertEquals(1, readCount)
    }

    @Test
    fun getTotalBookCount() = runBlocking {
        bookDao.insertBook(testBook1)
        bookDao.insertBook(testBook2)

        val count = bookDao.getTotalBookCount()

        assertEquals(2, count)
    }
}

