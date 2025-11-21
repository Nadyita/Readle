package com.readle.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readle.app.data.api.model.BookSearchResult
import com.readle.app.data.model.BookEntity
import com.readle.app.data.repository.BookRepository
import com.readle.app.data.repository.BookSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddBookUiState {
    object Idle : AddBookUiState()
    object Loading : AddBookUiState()
    data class SearchResults(
        val results: List<BookSearchResult>,
        val alreadyInLibrary: Map<BookSearchResult, Boolean> = emptyMap(),
        val existingBookStatus: Map<BookSearchResult, Pair<Boolean, Boolean>?> = emptyMap()  // (isOwned, isRead)
    ) : AddBookUiState()
    data class Error(val message: String) : AddBookUiState()
    object Success : AddBookUiState()
}

@HiltViewModel
class AddBookViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val bookSearchRepository: BookSearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddBookUiState>(AddBookUiState.Idle)
    val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

    private val _selectedBook = MutableStateFlow<BookSearchResult?>(null)
    val selectedBook: StateFlow<BookSearchResult?> = _selectedBook.asStateFlow()

    fun searchByIsbn(isbn: String) {
        viewModelScope.launch {
            _uiState.value = AddBookUiState.Loading
            try {
                val rawResults = bookSearchRepository.searchByIsbn(isbn)
                
                // Normalize results before displaying
                val normalizedResults = rawResults.map { normalizeSearchResult(it) }
                
                // Remove duplicates based on normalized title and author
                val results = deduplicateResults(normalizedResults)
                
                if (results.isEmpty()) {
                    _uiState.value = AddBookUiState.Error("No books found")
                } else {
                    val existingBooks = bookRepository.getAllBooksSnapshot()
                    val alreadyInLibrary = mutableMapOf<BookSearchResult, Boolean>()
                    val existingBookStatus = mutableMapOf<BookSearchResult, Pair<Boolean, Boolean>?>()
                    
                    results.forEach { result ->
                        val existingBook = findExistingBook(result, existingBooks)
                        alreadyInLibrary[result] = existingBook != null
                        existingBookStatus[result] = existingBook?.let { Pair(it.isOwned, it.isRead) }
                    }
                    
                    _uiState.value = AddBookUiState.SearchResults(results, alreadyInLibrary, existingBookStatus)
                }
            } catch (e: Exception) {
                _uiState.value = AddBookUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun searchByTitleAuthor(title: String?, author: String?, series: String? = null) {
        viewModelScope.launch {
            _uiState.value = AddBookUiState.Loading
            try {
                val rawResults = bookSearchRepository.searchByTitleAuthor(title, author, series)
                
                // Normalize results before displaying
                val normalizedResults = rawResults.map { normalizeSearchResult(it) }
                
                // Remove duplicates based on normalized title and author
                val results = deduplicateResults(normalizedResults)
                
                if (results.isEmpty()) {
                    _uiState.value = AddBookUiState.Error("No books found")
                } else {
                    val existingBooks = bookRepository.getAllBooksSnapshot()
                    val alreadyInLibrary = mutableMapOf<BookSearchResult, Boolean>()
                    val existingBookStatus = mutableMapOf<BookSearchResult, Pair<Boolean, Boolean>?>()
                    
                    results.forEach { result ->
                        val existingBook = findExistingBook(result, existingBooks)
                        alreadyInLibrary[result] = existingBook != null
                        existingBookStatus[result] = existingBook?.let { Pair(it.isOwned, it.isRead) }
                    }
                    
                    _uiState.value = AddBookUiState.SearchResults(results, alreadyInLibrary, existingBookStatus)
                }
            } catch (e: Exception) {
                _uiState.value = AddBookUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Removes duplicate search results based on normalized title and author.
     * Keeps the first occurrence of each unique book (by title + author combination).
     */
    private fun deduplicateResults(results: List<BookSearchResult>): List<BookSearchResult> {
        val seen = mutableSetOf<Pair<String, String>>()
        return results.filter { result ->
            val key = Pair(result.title.lowercase().trim(), result.author.lowercase().trim())
            seen.add(key)
        }
    }
    
    /**
     * Normalizes a search result by cleaning up and normalizing the title and author.
     * This is applied to search results before displaying them to the user.
     */
    private fun normalizeSearchResult(result: BookSearchResult): BookSearchResult {
        // Clean up title (removes quotes and ": Roman" suffix)
        val cleanedTitle = com.readle.app.util.TextNormalizer.cleanupManualImportTitle(result.title)
        
        // Normalize title and author
        val normalizedTitle = com.readle.app.util.TextNormalizer.normalizeTitle(cleanedTitle)
        val normalizedAuthor = com.readle.app.util.TextNormalizer.normalizeAuthor(result.author)
        
        return result.copy(
            title = normalizedTitle,
            author = normalizedAuthor
        )
    }
    
    /**
     * Finds an existing book in the library that matches the search result.
     * For books with multiple authors (separated by ";"), it's enough if
     * at least one author matches along with the title.
     * Returns the existing BookEntity if found, null otherwise.
     */
    private fun findExistingBook(result: BookSearchResult, existingBooks: List<BookEntity>): BookEntity? {
        val normalizedSearchTitle = java.text.Normalizer.normalize(
            result.title.lowercase().trim(), 
            java.text.Normalizer.Form.NFC
        )
        val searchAuthors = result.author.split(";").map { it.trim().lowercase() }
        
        return existingBooks.find { existing ->
            val normalizedExistingTitle = java.text.Normalizer.normalize(
                existing.title.lowercase().trim(),
                java.text.Normalizer.Form.NFC
            )
            
            // Collect all author variants from the existing book
            val existingAuthors = buildList {
                addAll(existing.author.split(";").map { it.trim().lowercase() })
                existing.originalAuthor?.let { original ->
                    addAll(original.split(";").map { it.trim().lowercase() })
                }
            }
            
            // Title must match (accounting for article position differences)
            val titleMatches = titlesMatch(normalizedSearchTitle, normalizedExistingTitle)
            
            // At least one author must match
            val authorMatches = searchAuthors.any { searchAuthor ->
                existingAuthors.any { existingAuthor ->
                    searchAuthor == existingAuthor ||
                    searchAuthor.contains(existingAuthor) || 
                    existingAuthor.contains(searchAuthor)
                }
            }
            
            // Series + Number match (for books with different titles but same series)
            // Also accept series names with minor typos (Levenshtein distance <= 2)
            val seriesMatches = !result.series.isNullOrBlank() && 
                                !existing.series.isNullOrBlank() &&
                                result.seriesNumber != null &&
                                existing.seriesNumber != null &&
                                result.seriesNumber == existing.seriesNumber &&
                                (result.series.equals(existing.series, ignoreCase = true) ||
                                 levenshteinDistance(
                                     result.series.lowercase(), 
                                     existing.series.lowercase()
                                 ) <= 2)
            
            (titleMatches || seriesMatches) && authorMatches
        }
    }
    
    /**
     * Compares two titles, accounting for different article positions.
     * Examples:
     * - "die last der krone" matches "last der krone, die"
     * - "dunkle mächte" matches "dunkle mächte"
     */
    private fun titlesMatch(title1: String, title2: String): Boolean {
        // Direct match
        if (title1 == title2) return true
        
        // Normalize both titles by removing articles
        val normalized1 = removeArticle(title1)
        val normalized2 = removeArticle(title2)
        
        // Compare without articles
        if (normalized1 == normalized2) return true
        
        // Partial match (one contains the other)
        if (title1.contains(title2) || title2.contains(title1)) return true
        if (normalized1.contains(normalized2) || normalized2.contains(normalized1)) return true
        
        return false
    }
    
    /**
     * Removes leading or trailing articles from a title.
     * Examples:
     * - "die last der krone" -> "last der krone"
     * - "last der krone, die" -> "last der krone"
     */
    private fun removeArticle(title: String): String {
        val trimmed = title.trim()
        
        // Articles at the end (", Der", ", Die", ", Das", ", The")
        val endArticles = listOf(", der", ", die", ", das", ", the", ", le", ", la", ", les", ", l'")
        for (article in endArticles) {
            if (trimmed.endsWith(article)) {
                return trimmed.substring(0, trimmed.length - article.length).trim()
            }
        }
        
        // Articles at the beginning ("Der ", "Die ", "Das ", "The ")
        val startArticles = listOf("der ", "die ", "das ", "the ", "le ", "la ", "les ", "l'")
        for (article in startArticles) {
            if (trimmed.startsWith(article)) {
                return trimmed.substring(article.length).trim()
            }
        }
        
        return trimmed
    }
    
    /**
     * Calculates the Levenshtein distance between two strings.
     * Returns the minimum number of single-character edits (insertions, deletions, substitutions)
     * required to change one string into the other.
     * 
     * Used for fuzzy matching of series names with minor typos.
     * Example: levenshteinDistance("Wahrheit", "Wahrhheit") = 1
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        // Create a matrix to store distances
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        // Initialize first row and column
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        // Fill the matrix
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[len1][len2]
    }

    fun selectBook(book: BookSearchResult) {
        _selectedBook.value = book
    }

    fun addBookToLibrary(
        book: BookSearchResult,
        isOwned: Boolean = true,
        isRead: Boolean = false,
        rating: Int = 0
    ) {
        viewModelScope.launch {
            _uiState.value = AddBookUiState.Loading
            try {
                // Book is already normalized in the search results,
                // but store the original title/author for comparison
                val originalTitle = book.title
                val originalAuthor = book.author
                
                val bookEntity = BookEntity(
                    title = book.title,
                    author = book.author,
                    isbn = book.isbn,
                    description = book.description,
                    publishDate = book.publishDate,
                    language = book.language,
                    originalLanguage = book.originalLanguage,
                    series = book.series,
                    seriesNumber = book.seriesNumber,
                    rating = rating,
                    isOwned = isOwned,
                    isRead = isRead,
                    dateAdded = System.currentTimeMillis(),
                    dateStarted = if (isOwned && !isRead)
                        System.currentTimeMillis() else null,
                    dateFinished = if (isRead)
                        System.currentTimeMillis() else null,
                    titleSort = com.readle.app.util.TextNormalizer.normalizeTitleForSorting(
                        book.title,
                        book.language
                    ),
                    // Store original values for search/comparison
                    originalTitle = originalTitle,
                    originalAuthor = originalAuthor
                )

                bookRepository.insertBook(bookEntity)
                _uiState.value = AddBookUiState.Success
            } catch (e: Exception) {
                _uiState.value = AddBookUiState.Error(e.message ?: "Failed to add book")
            }
        }
    }

    fun addManualBook(
        title: String,
        author: String,
        isOwned: Boolean = true,
        isRead: Boolean = false,
        isbn: String? = null,
        description: String? = null,
        publishDate: String? = null,
        language: String? = null,
        originalLanguage: String? = null,
        series: String? = null,
        seriesNumber: String? = null,
        rating: Int = 0
    ) {
        viewModelScope.launch {
            _uiState.value = AddBookUiState.Loading
            try {
                val bookEntity = BookEntity(
                    title = title,
                    author = author,
                    isbn = isbn,
                    description = description,
                    publishDate = publishDate,
                    language = language,
                    originalLanguage = originalLanguage,
                    series = series,
                    seriesNumber = seriesNumber,
                    rating = rating,
                    isOwned = isOwned,
                    isRead = isRead,
                    dateAdded = System.currentTimeMillis(),
                    dateStarted = if (isOwned && !isRead)
                        System.currentTimeMillis() else null,
                    dateFinished = if (isRead)
                        System.currentTimeMillis() else null,
                    titleSort = com.readle.app.util.TextNormalizer.normalizeTitleForSorting(
                        title,
                        language
                    )
                )

                bookRepository.insertBook(bookEntity)
                _uiState.value = AddBookUiState.Success
            } catch (e: Exception) {
                _uiState.value = AddBookUiState.Error(e.message ?: "Failed to add book")
            }
        }
    }

    fun resetState() {
        _uiState.value = AddBookUiState.Idle
        _selectedBook.value = null
    }
}

