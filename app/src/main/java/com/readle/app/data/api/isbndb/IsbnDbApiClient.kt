package com.readle.app.data.api.isbndb

import com.readle.app.data.api.model.BookDataSource
import com.readle.app.data.api.model.BookSearchResult
import javax.inject.Inject

class IsbnDbApiClient @Inject constructor(
    private val apiService: IsbnDbApiService
) {

    private val apiKey = ""

    suspend fun searchByIsbn(isbn: String): Result<List<BookSearchResult>> {
        if (apiKey.isBlank()) {
            return Result.success(emptyList())
        }

        return try {
            val cleanIsbn = isbn.replace("-", "").replace(" ", "")
            val response = apiService.getBookByIsbn(apiKey, cleanIsbn)

            if (response.isSuccessful && response.body() != null) {
                val book = response.body()!!.toBookSearchResult()
                Result.success(listOf(book))
            } else {
                Result.failure(Exception("ISBNdb API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchByTitleAuthor(title: String? = null, author: String? = null):
        Result<List<BookSearchResult>> {
        if (apiKey.isBlank()) {
            return Result.success(emptyList())
        }

        return try {
            val response = apiService.searchBooks(
                apiKey = apiKey,
                title = title?.trim(),
                author = author?.trim()
            )

            if (response.isSuccessful && response.body() != null) {
                val books = response.body()!!.books?.map { it.toBookSearchResult() } ?: emptyList()
                Result.success(books)
            } else {
                Result.failure(Exception("ISBNdb API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun IsbnDbBook.toBookSearchResult(): BookSearchResult {
        return BookSearchResult(
            title = title ?: "Unknown Title",
            author = authors?.joinToString(", ") ?: "Unknown Author",
            description = synopsis,
            publisher = publisher,
            publishDate = datePublished,
            language = language,
            originalLanguage = null,
            series = null,
            seriesNumber = null,
            isbn = isbn13 ?: isbn,
            coverUrl = image,
            source = BookDataSource.ISBN_DB
        )
    }
}

