package com.readle.app.data.repository

import com.readle.app.data.database.BookDao
import com.readle.app.data.model.BookEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao
) {

    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    // New: Filter books by owned and/or read status
    // null = any, true = only yes, false = only no
    fun getBooksFiltered(isOwned: Boolean?, isRead: Boolean?): Flow<List<BookEntity>> =
        bookDao.getBooksFiltered(isOwned, isRead)

    suspend fun getBookById(id: Long): BookEntity? = bookDao.getBookById(id)

    fun searchBooks(query: String): Flow<List<BookEntity>> = bookDao.searchBooks(query)

    suspend fun insertBook(book: BookEntity): Long = bookDao.insertBook(book)

    suspend fun insertBooks(books: List<BookEntity>) = bookDao.insertBooks(books)

    suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)

    suspend fun deleteBook(book: BookEntity) = bookDao.deleteBook(book)

    suspend fun deleteBooks(bookIds: List<Long>) = bookDao.deleteBooks(bookIds)

    suspend fun deleteAllBooks() = bookDao.deleteAllBooks()

    suspend fun getTotalBookCount(): Int = bookDao.getTotalBookCount()
    
    suspend fun getOwnedBookCount(): Int = bookDao.getOwnedBookCount()
    
    suspend fun getReadBookCount(): Int = bookDao.getReadBookCount()

    suspend fun getAllBooksSnapshot(): List<BookEntity> = bookDao.getAllBooksSnapshot()
}

