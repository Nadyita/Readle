package com.readle.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readle.app.data.api.audiobookshelf.AudiobookshelfApiClient
import com.readle.app.data.model.BookEntity
import com.readle.app.data.model.ReadingCategory
import com.readle.app.data.preferences.SettingsDataStore
import com.readle.app.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UploadState {
    object Idle : UploadState()
    data class Progress(
        val currentBook: String,
        val currentIndex: Int,
        val totalBooks: Int
    ) : UploadState()
    data class Success(val uploaded: Int, val failed: Int) : UploadState()
    data class Error(val message: String) : UploadState()
}

// Filter states: null = any/all, true = only yes, false = only no
enum class FilterState {
    ALL,     // Any (no filter)
    YES,     // Only true
    NO       // Only false
}

fun FilterState.toBoolean(): Boolean? = when (this) {
    FilterState.ALL -> null
    FilterState.YES -> true
    FilterState.NO -> false
}

@HiltViewModel
class BookListViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val settingsDataStore: SettingsDataStore,
    private val audiobookshelfApiClient: AudiobookshelfApiClient,
    private val uploadToPocketbookUseCase: com.readle.app.domain.usecase.UploadToPocketbookUseCase
) : ViewModel() {

    // New filter states (replace category selection)
    private val _ownedFilter = MutableStateFlow(FilterState.ALL)
    val ownedFilter: StateFlow<FilterState> = _ownedFilter.asStateFlow()

    private val _readFilter = MutableStateFlow(FilterState.ALL)
    val readFilter: StateFlow<FilterState> = _readFilter.asStateFlow()

    // Filter within current view (for BookListScreen)
    private val _viewFilter = MutableStateFlow("")
    val viewFilter: StateFlow<String> = _viewFilter.asStateFlow()

    // Global search query (for SearchScreen)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBooks = MutableStateFlow<Set<Long>>(emptySet())
    val selectedBooks: StateFlow<Set<Long>> = _selectedBooks.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    // Undo state for last swipe action
    data class SwipeAction(
        val bookId: Long,
        val actionType: SwipeActionType,
        val oldValue: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    enum class SwipeActionType {
        TOGGLE_OWNED,
        TOGGLE_READ
    }
    
    private val _lastSwipeAction = MutableStateFlow<SwipeAction?>(null)
    val lastSwipeAction: StateFlow<SwipeAction?> = _lastSwipeAction.asStateFlow()

    // Books filtered by owned/read status and view filter
    val books: StateFlow<List<BookEntity>> = combine(
        _ownedFilter,
        _readFilter,
        _viewFilter,
        settingsDataStore.bookSortOrder,
        bookRepository.getAllBooks()
    ) { ownedFilter, readFilter, viewFilter, sortOrder, allBooks ->
        // Apply owned/read filters
        val filteredByStatus = allBooks.filter { book ->
            val matchesOwned = when (ownedFilter) {
                FilterState.ALL -> true
                FilterState.YES -> book.isOwned
                FilterState.NO -> !book.isOwned
            }
            val matchesRead = when (readFilter) {
                FilterState.ALL -> true
                FilterState.YES -> book.isRead
                FilterState.NO -> !book.isRead
            }
            matchesOwned && matchesRead
        }
        
        if (viewFilter.isEmpty()) {
            applySortOrder(filteredByStatus, sortOrder)
        } else {
            // Check for field-specific search prefixes: "series=", "title=", "author="
            val (searchField, searchQuery) = parseFieldFilter(viewFilter)
            
            // Two-tier matching: exact matches first, then word matches
            val exactMatches = mutableListOf<BookEntity>()
            val wordMatches = mutableListOf<BookEntity>()
            
            filteredByStatus.forEach { book ->
                val exactMatch = when (searchField) {
                    "series" -> matchesQuery(book.series, searchQuery)
                    "title" -> matchesQuery(book.title, searchQuery) || 
                               matchesQuery(book.originalTitle, searchQuery)
                    "author" -> matchesQuery(book.author, searchQuery) || 
                                matchesQuery(book.originalAuthor, searchQuery)
                    else -> // No prefix, search all fields
                        matchesQuery(book.title, searchQuery) ||
                        matchesQuery(book.author, searchQuery) ||
                        matchesQuery(book.originalTitle, searchQuery) ||
                        matchesQuery(book.originalAuthor, searchQuery) ||
                        matchesQuery(book.series, searchQuery)
                }
                
                if (exactMatch) {
                    exactMatches.add(book)
                } else if (searchField == null && matchesAllWords(book, searchQuery)) {
                    // Only do word matching for general search (no field prefix)
                    wordMatches.add(book)
                }
            }
            
            // Sort results: by series number if filtering by series, otherwise by selected sort order
            if (searchField == "series") {
                // When filtering by series, sort by series number (then by title as fallback)
                exactMatches.sortedWith(compareBy(
                    { it.seriesNumber?.toDoubleOrNull() ?: Double.MAX_VALUE },
                    { it.title.lowercase() }
                )) + wordMatches.sortedWith(compareBy(
                    { it.seriesNumber?.toDoubleOrNull() ?: Double.MAX_VALUE },
                    { it.title.lowercase() }
                ))
            } else {
                // Apply selected sort order
                applySortOrder(exactMatches, sortOrder) + applySortOrder(wordMatches, sortOrder)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Global search results (across all filters) for SearchScreen
    val searchResults: StateFlow<List<BookEntity>> = combine(
        _searchQuery,
        bookRepository.getAllBooks()
    ) { query, allBooks ->
        if (query.isEmpty()) {
            emptyList()
        } else {
            val exactMatches = mutableListOf<BookEntity>()
            val wordMatches = mutableListOf<BookEntity>()
            
            allBooks.forEach { book ->
                val exactMatch = matchesQuery(book.title, query) ||
                    matchesQuery(book.author, query) ||
                    matchesQuery(book.originalTitle, query) ||
                    matchesQuery(book.originalAuthor, query) ||
                    matchesQuery(book.series, query)
                
                if (exactMatch) {
                    exactMatches.add(book)
                } else if (matchesAllWords(book, query)) {
                    wordMatches.add(book)
                }
            }
            
            exactMatches.sortedBy { it.title.lowercase() } + wordMatches.sortedBy { it.title.lowercase() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setOwnedFilter(filter: FilterState) {
        _ownedFilter.value = filter
        _selectedBooks.value = emptySet()
    }

    fun setReadFilter(filter: FilterState) {
        _readFilter.value = filter
        _selectedBooks.value = emptySet()
    }

    fun setViewFilter(filter: String) {
        _viewFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleBookSelection(bookId: Long) {
        _selectedBooks.value = if (_selectedBooks.value.contains(bookId)) {
            _selectedBooks.value - bookId
        } else {
            _selectedBooks.value + bookId
        }
    }

    fun selectAllBooks() {
        _selectedBooks.value = books.value.map { it.id }.toSet()
    }

    fun deselectAllBooks() {
        _selectedBooks.value = emptySet()
    }

    fun deleteSelectedBooks() {
        viewModelScope.launch {
            bookRepository.deleteBooks(_selectedBooks.value.toList())
            _selectedBooks.value = emptySet()
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            bookRepository.deleteBook(book)
        }
    }

    fun toggleBookOwned(bookId: Long) {
        viewModelScope.launch {
            val book = bookRepository.getBookById(bookId)
            if (book != null) {
                // Save old state for undo
                _lastSwipeAction.value = SwipeAction(
                    bookId = bookId,
                    actionType = SwipeActionType.TOGGLE_OWNED,
                    oldValue = book.isOwned
                )
                
                val updatedBook = book.copy(isOwned = !book.isOwned)
                bookRepository.updateBook(updatedBook)
            }
        }
    }

    fun toggleBookRead(bookId: Long) {
        viewModelScope.launch {
            val book = bookRepository.getBookById(bookId)
            if (book != null) {
                // Save old state for undo
                _lastSwipeAction.value = SwipeAction(
                    bookId = bookId,
                    actionType = SwipeActionType.TOGGLE_READ,
                    oldValue = book.isRead
                )
                
                val newIsRead = !book.isRead
                val updatedBook = book.copy(
                    isRead = newIsRead,
                    dateStarted = if (!book.isRead && newIsRead && book.dateStarted == null)
                        System.currentTimeMillis() else book.dateStarted,
                    dateFinished = if (newIsRead && book.dateFinished == null)
                        System.currentTimeMillis() else book.dateFinished
                )
                bookRepository.updateBook(updatedBook)
                
                // Sync with Audiobookshelf if book is linked
                if (book.audiobookshelfId != null) {
                    syncReadStatusWithAudiobookshelf(book.audiobookshelfId, newIsRead)
                }
            }
        }
    }

    // Toggle owned status for selected books
    // Logic: If not ALL books are owned, set ALL to owned. Otherwise, set ALL to not owned.
    fun toggleSelectedBooksOwned() {
        viewModelScope.launch {
            val selectedBookIds = _selectedBooks.value
            val selectedBookEntities = books.value.filter { selectedBookIds.contains(it.id) }
            
            // If ALL books are already owned, toggle to false. Otherwise, set all to true.
            val allAreOwned = selectedBookEntities.all { it.isOwned }
            val newIsOwned = !allAreOwned
            
            selectedBookEntities.forEach { book ->
                val updatedBook = book.copy(
                    isOwned = newIsOwned,
                    dateStarted = if (newIsOwned && !book.isOwned && book.dateStarted == null)
                        System.currentTimeMillis() else book.dateStarted
                )
                bookRepository.updateBook(updatedBook)
            }
            
            // Keep selection active (removed: _selectedBooks.value = emptySet())
        }
    }

    // Toggle read status for selected books
    // Logic: If not ALL books are read, set ALL to read. Otherwise, set ALL to not read.
    fun toggleSelectedBooksRead() {
        viewModelScope.launch {
            val selectedBookIds = _selectedBooks.value
            val selectedBookEntities = books.value.filter { selectedBookIds.contains(it.id) }
            
            // If ALL books are already read, toggle to false. Otherwise, set all to true.
            val allAreRead = selectedBookEntities.all { it.isRead }
            val newIsRead = !allAreRead
            
            selectedBookEntities.forEach { book ->
                val updatedBook = book.copy(
                    isRead = newIsRead,
                    dateStarted = if (!book.isRead && newIsRead && book.dateStarted == null)
                        System.currentTimeMillis() else book.dateStarted,
                    dateFinished = if (newIsRead && book.dateFinished == null)
                        System.currentTimeMillis() else book.dateFinished
                )
                bookRepository.updateBook(updatedBook)
                
                // Sync with Audiobookshelf if book is linked
                if (book.audiobookshelfId != null) {
                    syncReadStatusWithAudiobookshelf(book.audiobookshelfId, newIsRead)
                }
            }
            
            // Keep selection active (removed: _selectedBooks.value = emptySet())
        }
    }

    fun canUploadSelectedBooks(): Boolean {
        if (_selectedBooks.value.isEmpty()) return false
        val selectedBookEntities = books.value.filter { _selectedBooks.value.contains(it.id) }
        
        // All books must have audiobookshelfId AND be eBooks (not audiobooks)
        val allHaveAudiobookshelfId = selectedBookEntities.all {
            !it.audiobookshelfId.isNullOrBlank() && it.isEBook
        }
        
        // At least one book must NOT have been uploaded yet via email
        val hasBookToUpload = selectedBookEntities.any { 
            !it.uploadedViaEmail 
        }
        
        return allHaveAudiobookshelfId && hasBookToUpload
    }

    suspend fun hasPocketbookCredentials(): Boolean {
        val smtpServer = settingsDataStore.smtpServer.first()
        val smtpUsername = settingsDataStore.smtpUsername.first()
        val smtpPassword = settingsDataStore.smtpPassword.first()
        return smtpServer.isNotBlank() && smtpUsername.isNotBlank() && smtpPassword.isNotBlank()
    }

    fun hasAlreadySentBooks(): Boolean {
        val selectedBookEntities = books.value.filter { _selectedBooks.value.contains(it.id) }
        return selectedBookEntities.any { it.uploadedViaEmail }
    }

    fun getAlreadySentBooksCount(): Int {
        val selectedBookEntities = books.value.filter { _selectedBooks.value.contains(it.id) }
        return selectedBookEntities.count { it.uploadedViaEmail }
    }

    fun uploadSelectedBooksToPocketbook(forceReupload: Boolean = false) {
        viewModelScope.launch {
            try {
                val selectedBookEntities = books.value.filter { _selectedBooks.value.contains(it.id) }

                if (selectedBookEntities.isEmpty()) {
                    _uploadState.value = UploadState.Error("No books selected")
                    return@launch
                }

                val result = uploadToPocketbookUseCase.execute(
                    books = selectedBookEntities,
                    forceReupload = forceReupload,
                    onProgress = { currentBook, currentIndex, total ->
                        _uploadState.value = UploadState.Progress(
                            currentBook = currentBook,
                            currentIndex = currentIndex,
                            totalBooks = total
                        )
                    }
                )

                result.fold(
                    onSuccess = { uploadResult ->
                        _uploadState.value = UploadState.Success(
                            uploaded = uploadResult.successfulUploads,
                            failed = uploadResult.failedUploads
                        )
                        _selectedBooks.value = emptySet()
                    },
                    onFailure = { error ->
                        _uploadState.value = UploadState.Error(
                            error.message ?: "Upload failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    e.message ?: "Upload failed"
                )
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }

    fun undoLastSwipe() {
        viewModelScope.launch {
            val action = _lastSwipeAction.value
            if (action != null) {
                val book = bookRepository.getBookById(action.bookId)
                if (book != null) {
                    when (action.actionType) {
                        SwipeActionType.TOGGLE_OWNED -> {
                            // Restore old owned state
                            val updatedBook = book.copy(isOwned = action.oldValue)
                            bookRepository.updateBook(updatedBook)
                        }
                        SwipeActionType.TOGGLE_READ -> {
                            // Restore old read state (but keep date fields if they were set)
                            val updatedBook = book.copy(isRead = action.oldValue)
                            bookRepository.updateBook(updatedBook)
                            
                            // Sync with Audiobookshelf if book is linked
                            if (book.audiobookshelfId != null) {
                                syncReadStatusWithAudiobookshelf(book.audiobookshelfId, action.oldValue)
                            }
                        }
                    }
                }
                // Clear the undo action
                _lastSwipeAction.value = null
            }
        }
    }
    
    fun clearLastSwipeAction() {
        _lastSwipeAction.value = null
    }

    @Deprecated("Use toggleBookOwned/toggleBookRead instead")
    fun updateBookCategory(bookId: Long, newCategory: ReadingCategory) {
        viewModelScope.launch {
            val book = bookRepository.getBookById(bookId)
            if (book != null) {
                val updatedBook = book.copy(
                    isOwned = newCategory != ReadingCategory.WANT,
                    isRead = newCategory == ReadingCategory.READ,
                    dateStarted = if (newCategory == ReadingCategory.OWN &&
                        book.dateStarted == null) System.currentTimeMillis() else book.dateStarted,
                    dateFinished = if (newCategory == ReadingCategory.READ &&
                        book.dateFinished == null) System.currentTimeMillis() else book.dateFinished
                )
                bookRepository.updateBook(updatedBook)
                
                // Sync with Audiobookshelf if book is linked
                if (book.audiobookshelfId != null) {
                    syncReadStatusWithAudiobookshelf(book.audiobookshelfId, newCategory == ReadingCategory.READ)
                }
            }
        }
    }
    
    private suspend fun syncReadStatusWithAudiobookshelf(libraryItemId: String, isRead: Boolean) {
        try {
            val token = settingsDataStore.audiobookshelfApiToken.first()
            val serverUrl = settingsDataStore.audiobookshelfServerUrl.first()
            if (token.isNotEmpty() && serverUrl.isNotEmpty()) {
                audiobookshelfApiClient.initialize(serverUrl)
                audiobookshelfApiClient.updateBookProgress(token, libraryItemId, isRead)
            }
        } catch (e: Exception) {
            android.util.Log.e("BookSync", "Failed to sync read status with Audiobookshelf: ${e.message}", e)
        }
    }
    
    /**
     * Parses field-specific filter syntax like "series=Foundation", "title=Der kleine", "author=Eschbach"
     * 
     * @return Pair of (fieldName, searchQuery) where fieldName is null for general search
     * 
     * Examples:
     * - "series=Foundation" -> ("series", "Foundation")
     * - "title=Der kleine Prinz" -> ("title", "Der kleine Prinz")
     * - "author=Eschbach" -> ("author", "Eschbach")
     * - "general search" -> (null, "general search")
     */
    private fun parseFieldFilter(filter: String): Pair<String?, String> {
        // Check for field prefix pattern: "field="
        val prefixes = listOf("series=", "title=", "author=")
        
        for (prefix in prefixes) {
            if (filter.startsWith(prefix, ignoreCase = true)) {
                val fieldName = prefix.substringBefore("=").lowercase()
                val searchQuery = filter.substring(prefix.length).trim()
                return Pair(fieldName, searchQuery)
            }
        }
        
        // No prefix found, return null for general search
        return Pair(null, filter)
    }
    
    /**
     * Checks if a field matches the search query.
     * Supports umlaut-to-ASCII matching: "maechte" finds "mächte"
     */
    private fun matchesQuery(field: String?, query: String): Boolean {
        if (field == null) return false
        
        // Fast path: direct match
        if (field.contains(query, ignoreCase = true)) return true
        
        // Only do ASCII conversion if the query might contain ASCII replacements
        if (!needsAsciiConversion(query)) return false
        
        // Slow path: ASCII conversion for umlaut matching
        val fieldAscii = toAscii(field)
        val queryAscii = toAscii(query)
        return fieldAscii.contains(queryAscii, ignoreCase = true)
    }
    
    /**
     * Checks if all words from the query are present in any of the book's searchable fields.
     */
    private fun matchesAllWords(book: BookEntity, query: String): Boolean {
        val words = query.trim().split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return false
        
        return words.all { word ->
            matchesQuery(book.title, word) ||
            matchesQuery(book.author, word) ||
            matchesQuery(book.originalTitle, word) ||
            matchesQuery(book.originalAuthor, word) ||
            matchesQuery(book.series, word)
        }
    }
    
    private fun needsAsciiConversion(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("ae") || lower.contains("oe") || 
               lower.contains("ue") || lower.contains("ss")
    }
    
    private fun toAscii(text: String): String {
        return text
            .replace("ä", "ae", ignoreCase = true)
            .replace("ö", "oe", ignoreCase = true)
            .replace("ü", "ue", ignoreCase = true)
            .replace("Ä", "Ae", ignoreCase = false)
            .replace("Ö", "Oe", ignoreCase = false)
            .replace("Ü", "Ue", ignoreCase = false)
            .replace("ß", "ss", ignoreCase = true)
    }
    
    /**
     * Applies the selected sort order to a list of books.
     */
    private fun applySortOrder(books: List<BookEntity>, sortOrder: com.readle.app.data.model.SortOrder): List<BookEntity> {
        return when (sortOrder) {
            com.readle.app.data.model.SortOrder.TITLE_ASC -> books.sortedBy { it.title.lowercase() }
            com.readle.app.data.model.SortOrder.TITLE_DESC -> books.sortedByDescending { it.title.lowercase() }
            com.readle.app.data.model.SortOrder.AUTHOR_ASC -> books.sortedBy { it.author.lowercase() }
            com.readle.app.data.model.SortOrder.AUTHOR_DESC -> books.sortedByDescending { it.author.lowercase() }
            com.readle.app.data.model.SortOrder.DATE_ADDED_ASC -> books.sortedBy { it.dateAdded }
            com.readle.app.data.model.SortOrder.DATE_ADDED_DESC -> books.sortedByDescending { it.dateAdded }
        }
    }
    
    /**
     * Sets the sort order for the book list.
     */
    fun setSortOrder(sortOrder: com.readle.app.data.model.SortOrder) {
        viewModelScope.launch {
            settingsDataStore.setBookSortOrder(sortOrder)
        }
    }
}
