package com.readle.app.data.repository

import com.readle.app.data.api.dnb.DnbApiClient
import com.readle.app.data.api.google.GoogleBooksApiClient
import com.readle.app.data.api.isbndb.IsbnDbApiClient
import com.readle.app.data.api.openlibrary.OpenLibraryApiClient
import com.readle.app.data.api.model.BookSearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookSearchRepository @Inject constructor(
    private val dnbApiClient: DnbApiClient,
    private val googleBooksApiClient: GoogleBooksApiClient,
    private val isbnDbApiClient: IsbnDbApiClient,
    private val openLibraryApiClient: OpenLibraryApiClient,
    private val settingsDataStore: com.readle.app.data.preferences.SettingsDataStore
) {

    suspend fun searchByIsbn(isbn: String): List<BookSearchResult> = coroutineScope {
        val results = mutableListOf<BookSearchResult>()

        val dnbEnabled = settingsDataStore.dnbApiEnabled.first()
        val googleEnabled = settingsDataStore.googleBooksApiEnabled.first()
        val isbnDbEnabled = settingsDataStore.isbnDbApiEnabled.first()
        val openLibraryEnabled = settingsDataStore.openLibraryApiEnabled.first()

        // DNB with fallback (sequential, but with timeout)
        if (dnbEnabled) {
            try {
                withTimeout(15_000) {  // DNB + fallback needs more time (two API calls)
                    val dnbResult = dnbApiClient.searchByIsbn(isbn)
                    val dnbBooks = dnbResult.getOrNull() ?: emptyList()
                    results.addAll(dnbBooks)

                    // DNB Fallback: If no description, try title+author search
                    val hasDescription = dnbBooks.any { !it.description.isNullOrBlank() }
                    if (!hasDescription && dnbBooks.isNotEmpty()) {
                        val book = dnbBooks.first()
                        val fallbackResult = dnbApiClient.searchByTitleAuthor(
                            title = book.title,
                            author = book.author,
                            series = null
                        )
                        val fallbackBooks = fallbackResult.getOrNull() ?: emptyList()

                        // Find first result with a description
                        val bookWithDescription = fallbackBooks.firstOrNull { !it.description.isNullOrBlank() }
                        if (bookWithDescription != null) {
                            // Update original book with the found description
                            val updatedBook = book.copy(description = bookWithDescription.description)
                            results.remove(book)
                            results.add(updatedBook)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BookSearchRepository", "DNB search failed: ${e.message}")
            }
        }

        // Other APIs in parallel with timeout
        val deferreds = mutableListOf<kotlinx.coroutines.Deferred<List<BookSearchResult>>>()

        if (googleEnabled) {
            deferreds.add(async {
                try {
                    withTimeout(20_000) {  // Google Books is often slow, give it more time
                        googleBooksApiClient.searchByIsbn(isbn).getOrNull() ?: emptyList()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BookSearchRepository", "Google Books search failed: ${e.message}")
                    emptyList()
                }
            })
        }

        if (isbnDbEnabled) {
            deferreds.add(async {
                try {
                    withTimeout(10_000) {
                        isbnDbApiClient.searchByIsbn(isbn).getOrNull() ?: emptyList()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BookSearchRepository", "ISBNdb search failed: ${e.message}")
                    emptyList()
                }
            })
        }

        if (openLibraryEnabled) {
            deferreds.add(async {
                try {
                    withTimeout(10_000) {
                        openLibraryApiClient.searchByIsbn(isbn).getOrNull() ?: emptyList()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BookSearchRepository", "Open Library search failed: ${e.message}")
                    emptyList()
                }
            })
        }

        // Collect all parallel results
        deferreds.forEach { deferred ->
            results.addAll(deferred.await())
        }

        return@coroutineScope removeDuplicates(results)
    }

    suspend fun searchByTitleAuthor(
        title: String? = null,
        author: String? = null,
        series: String? = null
    ): List<BookSearchResult> = coroutineScope {
        val results = mutableListOf<BookSearchResult>()

        val dnbEnabled = settingsDataStore.dnbApiEnabled.first()
        val googleEnabled = settingsDataStore.googleBooksApiEnabled.first()
        val isbnDbEnabled = settingsDataStore.isbnDbApiEnabled.first()

        // DNB is primary for title/author search
        if (dnbEnabled) {
            try {
                withTimeout(15_000) {  // DNB can be slow for title/author searches
                    val dnbResult = dnbApiClient.searchByTitleAuthor(title, author, series)
                    dnbResult.getOrNull()?.let { results.addAll(it) }
                }
            } catch (e: Exception) {
                android.util.Log.e("BookSearchRepository", "DNB title/author search failed: ${e.message}")
            }
        }

        // Other APIs in parallel
        val deferreds = mutableListOf<kotlinx.coroutines.Deferred<List<BookSearchResult>>>()

        if (googleEnabled) {
            deferreds.add(async {
                try {
                    withTimeout(20_000) {  // Google Books is often slow, give it more time
                        googleBooksApiClient.searchByTitleAuthor(title, author, series).getOrNull() ?: emptyList()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BookSearchRepository", "Google Books title/author search failed: ${e.message}")
                    emptyList()
                }
            })
        }

        if (isbnDbEnabled) {
            deferreds.add(async {
                try {
                    withTimeout(10_000) {
                        isbnDbApiClient.searchByTitleAuthor(title, author).getOrNull() ?: emptyList()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BookSearchRepository", "ISBNdb title/author search failed: ${e.message}")
                    emptyList()
                }
            })
        }

        // Collect all parallel results
        deferreds.forEach { deferred ->
            results.addAll(deferred.await())
        }

        return@coroutineScope removeDuplicates(results)
    }

    private fun removeDuplicates(results: List<BookSearchResult>): List<BookSearchResult> {
        // Group books by any overlapping ISBN
        val isbnGroups = mutableListOf<MutableList<BookSearchResult>>()
        val processed = mutableSetOf<BookSearchResult>()
        
        for (book in results) {
            if (book in processed) continue
            
            val normalizedIsbns = book.allIsbns.map { normalizeIsbn(it) }.toSet()
            
            if (normalizedIsbns.isEmpty()) {
                // Books without ISBN go into their own group
                isbnGroups.add(mutableListOf(book))
                processed.add(book)
            } else {
                // Find existing group with overlapping ISBNs
                val matchingGroup = isbnGroups.firstOrNull { group ->
                    group.any { existingBook ->
                        val existingIsbns = existingBook.allIsbns.map { normalizeIsbn(it) }.toSet()
                        normalizedIsbns.intersect(existingIsbns).isNotEmpty()
                    }
                }
                
                if (matchingGroup != null) {
                    matchingGroup.add(book)
                } else {
                    isbnGroups.add(mutableListOf(book))
                }
                processed.add(book)
            }
        }
        
        
        // Merge each group
        val merged = isbnGroups.map { group ->
            if (group.size == 1) {
                group.first()
            } else {
                mergeBookResults(group)
            }
        }
        
        // Deduplicate books without ISBN by title+author
        val withIsbn = merged.filter { it.allIsbns.isNotEmpty() }
        val withoutIsbn = merged.filter { it.allIsbns.isEmpty() }
        
        val seen = mutableSetOf<String>()
        val deduplicatedWithoutIsbn = withoutIsbn.filter { book ->
            val key = normalizeForComparison(book.title) + "|" + normalizeForComparison(book.author)
            seen.add(key)
        }
        
        return withIsbn + deduplicatedWithoutIsbn
    }
    
    private fun normalizeIsbn(isbn: String): String {
        return isbn.replace(Regex("[^0-9X]"), "").uppercase()
    }
    
    private fun mergeBookResults(books: List<BookSearchResult>): BookSearchResult {
        // Prefer DNB for series information, as it's more reliable for German books
        val dnbBook = books.firstOrNull { it.source == com.readle.app.data.api.model.BookDataSource.DNB }
        val googleBook = books.firstOrNull { it.source == com.readle.app.data.api.model.BookDataSource.GOOGLE_BOOKS }
        val isbnDbBook = books.firstOrNull { it.source == com.readle.app.data.api.model.BookDataSource.ISBN_DB }
        val openLibraryBook = books.firstOrNull {
            it.source == com.readle.app.data.api.model.BookDataSource.OPEN_LIBRARY
        }
        
        // Prefer shortest title (usually the main title without series info in the title string)
        val bestTitle = books.minByOrNull { it.title.length }?.title ?: books.first().title
        
        // Prefer series with number over series without number
        val seriesWithNumber = books.firstOrNull { !it.series.isNullOrBlank() && it.seriesNumber != null }
        val seriesWithoutNumber = books.firstOrNull { !it.series.isNullOrBlank() && it.seriesNumber == null }
        val bestSeries = seriesWithNumber?.series ?: seriesWithoutNumber?.series
        val bestSeriesNumber = seriesWithNumber?.seriesNumber
        
        // Collect ALL ISBNs from all books
        val allIsbns = books.flatMap { it.allIsbns }.distinct().filter { it.isNotBlank() }
        val primaryIsbn = allIsbns.firstOrNull()
        
        // Prefer descriptions from Open Library and Google Books (better quality)
        val bestDescription = openLibraryBook?.description?.takeIf { it.isNotBlank() }
            ?: googleBook?.description?.takeIf { it.isNotBlank() }
            ?: dnbBook?.description?.takeIf { it.isNotBlank() }
            ?: isbnDbBook?.description?.takeIf { it.isNotBlank() }
            ?: books.firstOrNull { !it.description.isNullOrBlank() }?.description
        
        val merged = BookSearchResult(
            title = bestTitle,
            author = books.first().author,
            description = bestDescription,
            publisher = books.firstOrNull { !it.publisher.isNullOrBlank() }?.publisher,
            publishDate = books.firstOrNull { !it.publishDate.isNullOrBlank() }?.publishDate,
            language = books.firstOrNull { !it.language.isNullOrBlank() }?.language,
            originalLanguage = books.firstOrNull { it.originalLanguage != null }?.originalLanguage,
            series = bestSeries,
            seriesNumber = bestSeriesNumber,
            isbn = primaryIsbn,
            allIsbns = allIsbns,
            coverUrl = books.firstOrNull { !it.coverUrl.isNullOrBlank() }?.coverUrl,
            source = dnbBook?.source ?: googleBook?.source ?: isbnDbBook?.source ?:
                openLibraryBook?.source ?: books.first().source
        )
        
        return merged
    }

    private fun normalizeForComparison(text: String?): String {
        return text?.lowercase()
            ?.replace(Regex("[^a-z0-9]"), "")
            ?.take(50)
            ?: ""
    }
}

