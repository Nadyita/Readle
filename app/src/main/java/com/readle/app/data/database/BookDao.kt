package com.readle.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.readle.app.data.model.BookEntity
import com.readle.app.data.model.ReadingCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY title ASC")
    fun getAllBooks(): Flow<List<BookEntity>>

    // New: Filter by owned and/or read status
    // null means "any", true means "only yes", false means "only no"
    @Query("""
        SELECT * FROM books 
        WHERE (:isOwned IS NULL OR isOwned = :isOwned)
          AND (:isRead IS NULL OR isRead = :isRead)
        ORDER BY title ASC
    """)
    fun getBooksFiltered(isOwned: Boolean?, isRead: Boolean?): Flow<List<BookEntity>>

    // Removed: category column no longer exists after migration to v10
    // Use getBooksFiltered instead
    // Kept only for compilation compatibility - returns empty list
    @Deprecated("Use getBooksFiltered instead - category column removed in v10", ReplaceWith("getBooksFiltered(null, null)"))
    fun getBooksByCategory(category: ReadingCategory): Flow<List<BookEntity>> {
        // Return empty flow - this method is no longer functional
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }

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

    @Deprecated("Use count queries with isOwned/isRead instead - category column removed in v10", ReplaceWith("flowOf(0)"))
    fun getBookCountByCategory(category: ReadingCategory): Flow<Int> {
        return kotlinx.coroutines.flow.flowOf(0)
    }

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getTotalBookCount(): Int
    
    @Query("SELECT COUNT(*) FROM books WHERE isOwned = 1")
    suspend fun getOwnedBookCount(): Int
    
    @Query("SELECT COUNT(*) FROM books WHERE isRead = 1")
    suspend fun getReadBookCount(): Int

    @Query("SELECT * FROM books")
    suspend fun getAllBooksSnapshot(): List<BookEntity>
}

