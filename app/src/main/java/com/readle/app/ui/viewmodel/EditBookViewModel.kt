package com.readle.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readle.app.data.model.BookEntity
import com.readle.app.data.model.ReadingCategory
import com.readle.app.data.preferences.SettingsDataStore
import com.readle.app.data.repository.BookRepository
import com.readle.app.domain.usecase.UploadToPocketbookUseCase
import com.readle.app.util.ImageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EditBookUiState {
    object Idle : EditBookUiState()
    object Loading : EditBookUiState()
    data class BookLoaded(val book: BookEntity) : EditBookUiState()
    object Success : EditBookUiState()
    data class Error(val message: String) : EditBookUiState()
}

sealed class EditBookUploadState {
    object Idle : EditBookUploadState()
    object Uploading : EditBookUploadState()
    object Success : EditBookUploadState()
    data class Error(val message: String) : EditBookUploadState()
}

@HiltViewModel
class EditBookViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val uploadToPocketbookUseCase: UploadToPocketbookUseCase,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditBookUiState>(EditBookUiState.Idle)
    val uiState: StateFlow<EditBookUiState> = _uiState.asStateFlow()

    private val _uploadState = MutableStateFlow<EditBookUploadState>(EditBookUploadState.Idle)
    val uploadState: StateFlow<EditBookUploadState> = _uploadState.asStateFlow()

    private val _isEmailConfigured = MutableStateFlow(false)
    val isEmailConfigured: StateFlow<Boolean> = _isEmailConfigured.asStateFlow()

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            _uiState.value = EditBookUiState.Loading
            try {
                val book = bookRepository.getBookById(bookId)
                if (book != null) {
                    _uiState.value = EditBookUiState.BookLoaded(book)
                } else {
                    _uiState.value = EditBookUiState.Error("Book not found")
                }
                
                // Check email configuration
                checkEmailConfiguration()
            } catch (e: Exception) {
                _uiState.value = EditBookUiState.Error(e.message ?: "Failed to load book")
            }
        }
    }

    private suspend fun checkEmailConfiguration() {
        val smtpServer = settingsDataStore.smtpServer.first()
        val smtpUsername = settingsDataStore.smtpUsername.first()
        val smtpPassword = settingsDataStore.smtpPassword.first()
        val smtpFromEmail = settingsDataStore.smtpFromEmail.first()
        val pocketbookEmail = settingsDataStore.pocketbookSendToEmail.first()
        
        _isEmailConfigured.value = smtpServer.isNotBlank() && 
                                    smtpUsername.isNotBlank() && 
                                    smtpPassword.isNotBlank() &&
                                    smtpFromEmail.isNotBlank() && 
                                    pocketbookEmail.isNotBlank()
    }

    fun updateBook(
        bookId: Long,
        title: String,
        author: String,
        description: String? = null,
        publishDate: String? = null,
        language: String? = null,
        originalLanguage: String? = null,
        series: String? = null,
        seriesNumber: String? = null,
        isEBook: Boolean = false,
        comments: String? = null,
        rating: Int = 0,
        isOwned: Boolean = true,
        isRead: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.value = EditBookUiState.Loading
            try {
                val existingBook = bookRepository.getBookById(bookId)
                if (existingBook != null) {
                    val normalizedTitle = com.readle.app.util.TextNormalizer.normalizeTitle(title)
                    val updatedBook = existingBook.copy(
                        title = normalizedTitle,
                        author = com.readle.app.util.TextNormalizer.normalizeAuthor(author),
                        description = description,
                        publishDate = publishDate,
                        language = language,
                        originalLanguage = originalLanguage,
                        series = series,
                        seriesNumber = seriesNumber,
                        isEBook = isEBook,
                        comments = comments,
                        rating = rating,
                        isOwned = isOwned,
                        isRead = isRead,
                        titleSort = com.readle.app.util.TextNormalizer.normalizeTitleForSorting(
                            normalizedTitle,
                            language
                        )
                    )
                    bookRepository.updateBook(updatedBook)
                    _uiState.value = EditBookUiState.Success
                } else {
                    _uiState.value = EditBookUiState.Error("Book not found")
                }
            } catch (e: Exception) {
                _uiState.value = EditBookUiState.Error(e.message ?: "Failed to update book")
            }
        }
    }

    fun updateRating(bookId: Long, rating: Int) {
        viewModelScope.launch {
            try {
                val existingBook = bookRepository.getBookById(bookId)
                if (existingBook != null) {
                    val updatedBook = existingBook.copy(rating = rating)
                    bookRepository.updateBook(updatedBook)
                    // Don't change UI state - just silently update the book
                }
            } catch (e: Exception) {
                // Silently fail - rating update is not critical
            }
        }
    }

    fun resetState() {
        _uiState.value = EditBookUiState.Idle
    }

    fun uploadBookToPocketbook(bookId: Long, forceReupload: Boolean = false) {
        viewModelScope.launch {
            _uploadState.value = EditBookUploadState.Uploading
            try {
                val book = bookRepository.getBookById(bookId)
                if (book != null) {
                    val result = uploadToPocketbookUseCase.execute(
                        books = listOf(book),
                        forceReupload = forceReupload
                    )
                    result.fold(
                        onSuccess = { uploadResult ->
                            if (uploadResult.failedUploads > 0) {
                                val error = uploadResult.failedBooks.firstOrNull()?.second
                                    ?: "Upload failed"
                                _uploadState.value = EditBookUploadState.Error(error)
                            } else {
                                _uploadState.value = EditBookUploadState.Success
                            }
                        },
                        onFailure = { error ->
                            _uploadState.value = EditBookUploadState.Error(
                                error.message ?: "Upload failed"
                            )
                        }
                    )
                } else {
                    _uploadState.value = EditBookUploadState.Error("Book not found")
                }
            } catch (e: Exception) {
                _uploadState.value = EditBookUploadState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = EditBookUploadState.Idle
    }
}

