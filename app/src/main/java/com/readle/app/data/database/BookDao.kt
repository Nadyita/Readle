package com.readle.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.readle.app.data.model.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY title ASC")
    fun getAllBooks(): Flow<List<BookEntity>>

    // Filter by owned and/or read status
    // null means "any", true means "only yes", false means "only no"
    @Query("""
        SELECT * FROM books 
        WHERE (:isOwned IS NULL OR isOwned = :isOwned)
          AND (:isRead IS NULL OR isRead = :isRead)
        ORDER BY title ASC
    """)
    fun getBooksFiltered(isOwned: Boolean?, isRead: Boolean?): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE " +
        "title LIKE '%' || :query || '%' OR " +
        "author LIKE '%' || :query || '%' OR " +
        "originalTitle LIKE '%' || :query || '%' OR " +
        "originalAuthor LIKE '%' || :query || '%' OR " +
        "description LIKE '%' || :query || '%'")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id IN (:bookIds)")
    suspend fun deleteBooks(bookIds: List<Long>)

    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getTotalBookCount(): Int
    
    @Query("SELECT COUNT(*) FROM books WHERE isOwned = 1")
    suspend fun getOwnedBookCount(): Int
    
    @Query("SELECT COUNT(*) FROM books WHERE isRead = 1")
    suspend fun getReadBookCount(): Int

    @Query("SELECT * FROM books")
    suspend fun getAllBooksSnapshot(): List<BookEntity>
}

