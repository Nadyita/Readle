package com.readle.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readle.app.data.model.BookEntity
import com.readle.app.data.model.ReadingCategory
import com.readle.app.data.repository.BookRepository
import com.readle.app.util.ImageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EditBookUiState {
    object Idle : EditBookUiState()
    object Loading : EditBookUiState()
    data class BookLoaded(val book: BookEntity) : EditBookUiState()
    object Success : EditBookUiState()
    data class Error(val message: String) : EditBookUiState()
}

@HiltViewModel
class EditBookViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditBookUiState>(EditBookUiState.Idle)
    val uiState: StateFlow<EditBookUiState> = _uiState.asStateFlow()

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
            } catch (e: Exception) {
                _uiState.value = EditBookUiState.Error(e.message ?: "Failed to load book")
            }
        }
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
                    val updatedBook = existingBook.copy(
                        title = com.readle.app.util.TextNormalizer.normalizeTitle(title),
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
                        isRead = isRead
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

    fun resetState() {
        _uiState.value = EditBookUiState.Idle
    }
}

