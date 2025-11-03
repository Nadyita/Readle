package com.readle.app.ui.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readle.app.data.model.BookEntity
import com.readle.app.data.preferences.ExportFormat
import com.readle.app.data.preferences.ScannerLibrary
import com.readle.app.data.preferences.SettingsDataStore
import com.readle.app.data.preferences.ThemeMode
import com.readle.app.data.repository.BookRepository
import com.readle.app.domain.usecase.ExportBooksUseCase
import com.readle.app.domain.usecase.ImportBooksUseCase
import com.readle.app.service.AudiobookshelfImportService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class ImportExportState {
    object Idle : ImportExportState()
    object Loading : ImportExportState()
    data class ExportSuccess(val file: File) : ImportExportState()
    data class ImportSuccess(val bookCount: Int) : ImportExportState()
    data class Error(val message: String) : ImportExportState()
}

sealed class AudiobookshelfLoginState {
    object Idle : AudiobookshelfLoginState()
    object Loading : AudiobookshelfLoginState()
    object Success : AudiobookshelfLoginState()
    data class Error(val message: String) : AudiobookshelfLoginState()
}

sealed class AudiobookshelfImportState {
    object Idle : AudiobookshelfImportState()
    data class Loading(val current: Int = 0, val total: Int = 0) : AudiobookshelfImportState()
    data class Success(val imported: Int, val updated: Int, val total: Int) : AudiobookshelfImportState()
    data class Error(val message: String) : AudiobookshelfImportState()
}

sealed class PocketbookSyncState {
    object Idle : PocketbookSyncState()
    object Loading : PocketbookSyncState()
    data class Success(
        val scanned: Int,
        val updated: Int,
        val notFound: Int,
        val unmatchedBooks: List<com.readle.app.data.api.pocketbook.PocketbookBook>,
        val audiobookshelfSynced: Int = 0
    ) : PocketbookSyncState()
    data class Error(val message: String) : PocketbookSyncState()
}

sealed class EmailTestState {
    object Idle : EmailTestState()
    object Loading : EmailTestState()
    object Success : EmailTestState()
    data class Error(val message: String) : EmailTestState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val bookRepository: BookRepository,
    private val exportBooksUseCase: ExportBooksUseCase,
    private val importBooksUseCase: ImportBooksUseCase,
    private val audiobookshelfApiClient: com.readle.app.data.api.audiobookshelf.AudiobookshelfApiClient,
    private val syncPocketbookUseCase: com.readle.app.domain.usecase.SyncPocketbookUseCase,
    private val pocketbookEmailService: com.readle.app.data.api.pocketbook.PocketbookEmailService
) : ViewModel() {

    private val importBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AudiobookshelfImportService.ACTION_PROGRESS -> {
                    val current = intent.getIntExtra(AudiobookshelfImportService.EXTRA_CURRENT, 0)
                    val total = intent.getIntExtra(AudiobookshelfImportService.EXTRA_TOTAL, 0)
                    _audiobookshelfImportState.value = AudiobookshelfImportState.Loading(current, total)
                }
                AudiobookshelfImportService.ACTION_COMPLETE -> {
                    val imported = intent.getIntExtra(AudiobookshelfImportService.EXTRA_IMPORTED, 0)
                    val updated = intent.getIntExtra(AudiobookshelfImportService.EXTRA_UPDATED, 0)
                    val total = intent.getIntExtra(AudiobookshelfImportService.EXTRA_TOTAL, 0)
                    _audiobookshelfImportState.value = AudiobookshelfImportState.Success(imported, updated, total)
                }
                AudiobookshelfImportService.ACTION_ERROR -> {
                    val message = intent.getStringExtra(AudiobookshelfImportService.EXTRA_ERROR_MESSAGE) ?: "Import failed"
                    _audiobookshelfImportState.value = AudiobookshelfImportState.Error(message)
                }
            }
        }
    }

    init {
        // Register broadcast receiver for import updates
        val filter = IntentFilter().apply {
            addAction(AudiobookshelfImportService.ACTION_PROGRESS)
            addAction(AudiobookshelfImportService.ACTION_COMPLETE)
            addAction(AudiobookshelfImportService.ACTION_ERROR)
        }
        context.registerReceiver(importBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onCleared() {
        super.onCleared()
        context.unregisterReceiver(importBroadcastReceiver)
    }

    val themeMode: StateFlow<ThemeMode> = settingsDataStore.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    val exportFormat: StateFlow<ExportFormat> = settingsDataStore.exportFormat.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExportFormat.JSON
    )

    val scannerLibrary: StateFlow<ScannerLibrary> = settingsDataStore.scannerLibrary.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScannerLibrary.ML_KIT
    )

    val dnbApiEnabled: StateFlow<Boolean> = settingsDataStore.dnbApiEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val googleBooksApiEnabled: StateFlow<Boolean> = settingsDataStore.googleBooksApiEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val isbnDbApiEnabled: StateFlow<Boolean> = settingsDataStore.isbnDbApiEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val audiobookshelfServerUrl: StateFlow<String> = settingsDataStore.audiobookshelfServerUrl.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val audiobookshelfApiToken: StateFlow<String> = settingsDataStore.audiobookshelfApiToken.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val pocketbookEmail: StateFlow<String> = settingsDataStore.pocketbookEmail.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val pocketbookPassword: StateFlow<String> = settingsDataStore.pocketbookPassword.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    // Email settings for "Send to Pocketbook"
    val smtpServer: StateFlow<String> = settingsDataStore.smtpServer.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val smtpPort: StateFlow<Int> = settingsDataStore.smtpPort.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 587
    )

    val smtpUsername: StateFlow<String> = settingsDataStore.smtpUsername.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val smtpPassword: StateFlow<String> = settingsDataStore.smtpPassword.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val smtpFromEmail: StateFlow<String> = settingsDataStore.smtpFromEmail.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val pocketbookSendToEmail: StateFlow<String> = settingsDataStore.pocketbookSendToEmail.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val pocketbookUploadMethod: StateFlow<com.readle.app.data.preferences.PocketbookUploadMethod> = 
        settingsDataStore.pocketbookUploadMethod.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.readle.app.data.preferences.PocketbookUploadMethod.CLOUD
        )

    val pocketbookCleanTitles: StateFlow<Boolean> =
        settingsDataStore.pocketbookCleanTitles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _importExportState = MutableStateFlow<ImportExportState>(ImportExportState.Idle)
    val importExportState: StateFlow<ImportExportState> = _importExportState.asStateFlow()

    private val _audiobookshelfLoginState = MutableStateFlow<AudiobookshelfLoginState>(AudiobookshelfLoginState.Idle)
    val audiobookshelfLoginState: StateFlow<AudiobookshelfLoginState> = _audiobookshelfLoginState.asStateFlow()

    private val _audiobookshelfImportState = MutableStateFlow<AudiobookshelfImportState>(AudiobookshelfImportState.Idle)
    val audiobookshelfImportState: StateFlow<AudiobookshelfImportState> = _audiobookshelfImportState.asStateFlow()

    private val _pocketbookSyncState = MutableStateFlow<PocketbookSyncState>(PocketbookSyncState.Idle)
    val pocketbookSyncState: StateFlow<PocketbookSyncState> = _pocketbookSyncState.asStateFlow()

    private val _emailTestState = MutableStateFlow<EmailTestState>(EmailTestState.Idle)
    val emailTestState: StateFlow<EmailTestState> = _emailTestState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun setExportFormat(format: ExportFormat) {
        viewModelScope.launch {
            settingsDataStore.setExportFormat(format)
        }
    }

    fun setScannerLibrary(library: ScannerLibrary) {
        viewModelScope.launch {
            settingsDataStore.setScannerLibrary(library)
        }
    }

    fun setDnbApiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setDnbApiEnabled(enabled)
        }
    }

    fun setGoogleBooksApiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setGoogleBooksApiEnabled(enabled)
        }
    }

    fun setIsbnDbApiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setIsbnDbApiEnabled(enabled)
        }
    }

    fun setAudiobookshelfServerUrl(url: String) {
        viewModelScope.launch {
            settingsDataStore.setAudiobookshelfServerUrl(url)
            if (url.isNotBlank()) {
                audiobookshelfApiClient.initialize(url)
            }
        }
    }

    fun loginToAudiobookshelf(username: String, password: String) {
        viewModelScope.launch {
            _audiobookshelfLoginState.value = AudiobookshelfLoginState.Loading
            try {
                val serverUrl = audiobookshelfServerUrl.value
                if (serverUrl.isBlank()) {
                    _audiobookshelfLoginState.value = AudiobookshelfLoginState.Error(
                        "Please set server URL first"
                    )
                    return@launch
                }

                audiobookshelfApiClient.initialize(serverUrl)
                val result = audiobookshelfApiClient.login(username, password)

                result.fold(
                    onSuccess = { token ->
                        settingsDataStore.setAudiobookshelfApiToken(token)
                        _audiobookshelfLoginState.value = AudiobookshelfLoginState.Success
                    },
                    onFailure = { error ->
                        _audiobookshelfLoginState.value = AudiobookshelfLoginState.Error(
                            error.message ?: "Login failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _audiobookshelfLoginState.value = AudiobookshelfLoginState.Error(
                    e.message ?: "Login failed"
                )
            }
        }
    }

    fun resetAudiobookshelfLoginState() {
        _audiobookshelfLoginState.value = AudiobookshelfLoginState.Idle
    }

    fun logoutFromAudiobookshelf() {
        viewModelScope.launch {
            settingsDataStore.setAudiobookshelfApiToken("")
        }
    }

    fun exportBooks() {
        viewModelScope.launch {
            _importExportState.value = ImportExportState.Loading
            try {
                val books = bookRepository.getAllBooks().first()
                val result = exportBooksUseCase.execute(books, exportFormat.value)
                
                result.fold(
                    onSuccess = { file ->
                        _importExportState.value = ImportExportState.ExportSuccess(file)
                    },
                    onFailure = { error ->
                        android.util.Log.e("SettingsViewModel", "Export failed: ${error.message}", error)
                        _importExportState.value = ImportExportState.Error(
                            error.message ?: "Export failed"
                        )
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Export exception: ${e.message}", e)
                _importExportState.value = ImportExportState.Error(
                    e.message ?: "Export failed"
                )
            }
        }
    }

    fun importBooks(file: File, replaceExisting: Boolean) {
        viewModelScope.launch {
            _importExportState.value = ImportExportState.Loading
            try {
                val result = importBooksUseCase.execute(file, replaceExisting)
                result.fold(
                    onSuccess = { count ->
                        _importExportState.value = ImportExportState.ImportSuccess(count)
                    },
                    onFailure = { error ->
                        _importExportState.value = ImportExportState.Error(
                            error.message ?: "Import failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _importExportState.value = ImportExportState.Error(
                    e.message ?: "Import failed"
                )
            }
        }
    }

    fun resetImportExportState() {
        _importExportState.value = ImportExportState.Idle
    }

    fun importFromAudiobookshelf() {
        viewModelScope.launch {
            try {
                val token = audiobookshelfApiToken.value
                if (token.isBlank()) {
                    _audiobookshelfImportState.value = AudiobookshelfImportState.Error(
                        "Not logged in. Please login first."
                    )
                    return@launch
                }

                val serverUrl = audiobookshelfServerUrl.value
                if (serverUrl.isBlank()) {
                    _audiobookshelfImportState.value = AudiobookshelfImportState.Error(
                        "Server URL not set."
                    )
                    return@launch
                }

                // Start foreground service for import
                _audiobookshelfImportState.value = AudiobookshelfImportState.Loading()
                AudiobookshelfImportService.start(context, serverUrl, token)
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to start import service: ${e.message}", e)
                _audiobookshelfImportState.value = AudiobookshelfImportState.Error(
                    e.message ?: "Failed to start import"
                )
            }
        }
    }

    fun resetAudiobookshelfImportState() {
        _audiobookshelfImportState.value = AudiobookshelfImportState.Idle
    }

    fun deleteAllBooks() {
        viewModelScope.launch {
            try {
                bookRepository.deleteAllBooks()
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error deleting all books: ${e.message}", e)
            }
        }
    }

    fun setPocketbookEmail(email: String) {
        viewModelScope.launch {
            settingsDataStore.setPocketbookEmail(email)
        }
    }

    fun setPocketbookPassword(password: String) {
        viewModelScope.launch {
            settingsDataStore.setPocketbookPassword(password)
        }
    }

    // Email settings setters
    fun setSmtpServer(server: String) {
        viewModelScope.launch {
            android.util.Log.d("SettingsViewModel", "setSmtpServer: Saving '$server' to DataStore")
            settingsDataStore.setSmtpServer(server)
            android.util.Log.d("SettingsViewModel", "setSmtpServer: Saved successfully")
        }
    }

    fun setSmtpPort(port: Int) {
        viewModelScope.launch {
            android.util.Log.d("SettingsViewModel", "setSmtpPort: Saving '$port' to DataStore")
            settingsDataStore.setSmtpPort(port)
            android.util.Log.d("SettingsViewModel", "setSmtpPort: Saved successfully")
        }
    }

    fun setSmtpUsername(username: String) {
        viewModelScope.launch {
            android.util.Log.d("SettingsViewModel", "setSmtpUsername: Saving '$username' to DataStore")
            settingsDataStore.setSmtpUsername(username)
            android.util.Log.d("SettingsViewModel", "setSmtpUsername: Saved successfully")
        }
    }

    fun setSmtpPassword(password: String) {
        viewModelScope.launch {
            android.util.Log.d("SettingsViewModel", "setSmtpPassword: Saving password to DataStore")
            settingsDataStore.setSmtpPassword(password)
            android.util.Log.d("SettingsViewModel", "setSmtpPassword: Saved successfully")
        }
    }

    fun setSmtpFromEmail(email: String) {
        viewModelScope.launch {
            android.util.Log.d("SettingsViewModel", "setSmtpFromEmail: Saving '$email' to DataStore")
            settingsDataStore.setSmtpFromEmail(email)
            android.util.Log.d("SettingsViewModel", "setSmtpFromEmail: Saved successfully")
        }
    }

    fun setPocketbookSendToEmail(email: String) {
        viewModelScope.launch {
            android.util.Log.d("SettingsViewModel", "setPocketbookSendToEmail: Saving '$email' to DataStore")
            settingsDataStore.setPocketbookSendToEmail(email)
            android.util.Log.d("SettingsViewModel", "setPocketbookSendToEmail: Saved successfully")
        }
    }

    fun setPocketbookUploadMethod(method: com.readle.app.data.preferences.PocketbookUploadMethod) {
        viewModelScope.launch {
            settingsDataStore.setPocketbookUploadMethod(method)
        }
    }

    fun setPocketbookCleanTitles(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setPocketbookCleanTitles(enabled)
        }
    }

    fun syncWithPocketbook(askForUnknownBooks: Boolean = true) {
        viewModelScope.launch {
            _pocketbookSyncState.value = PocketbookSyncState.Loading
            try {
                val email = pocketbookEmail.value
                val password = pocketbookPassword.value

                if (email.isBlank() || password.isBlank()) {
                    _pocketbookSyncState.value = PocketbookSyncState.Error(
                        "Please set email and password first"
                    )
                    return@launch
                }

                // Get Audiobookshelf token if available
                val absToken = audiobookshelfApiToken.value.takeIf { it.isNotBlank() }

                val result = syncPocketbookUseCase.execute(email, password, absToken)

                result.fold(
                    onSuccess = { syncResult ->
                        // If we should ask for unknown books and there are some, show dialog
                        if (askForUnknownBooks && syncResult.unmatchedBooks.isNotEmpty()) {
                            _pocketbookSyncState.value = PocketbookSyncState.Success(
                                scanned = syncResult.totalBooksScanned,
                                updated = syncResult.booksUpdated,
                                notFound = syncResult.booksNotFound,
                                unmatchedBooks = syncResult.unmatchedBooks,
                                audiobookshelfSynced = syncResult.audiobookshelfSynced
                            )
                        } else {
                            // Silent success - no dialog for unknown books
                            android.util.Log.d(
                                "SettingsViewModel",
                                "Sync completed${if (askForUnknownBooks) "" else " (auto)"}: " +
                                "${syncResult.booksUpdated} updated, ${syncResult.booksNotFound} not found, " +
                                "${syncResult.audiobookshelfSynced} synced to Audiobookshelf"
                            )
                            
                            if (askForUnknownBooks) {
                                // Manual sync - show success in UI
                                _pocketbookSyncState.value = PocketbookSyncState.Success(
                                    scanned = syncResult.totalBooksScanned,
                                    updated = syncResult.booksUpdated,
                                    notFound = syncResult.booksNotFound,
                                    unmatchedBooks = emptyList(),
                                    audiobookshelfSynced = syncResult.audiobookshelfSynced
                                )
                            } else {
                                // Auto-sync - don't show anything in UI
                                _pocketbookSyncState.value = PocketbookSyncState.Idle
                            }
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("SettingsViewModel", "Pocketbook sync failed: ${error.message}", error)
                        if (askForUnknownBooks) {
                            // Manual sync - show error in UI
                            _pocketbookSyncState.value = PocketbookSyncState.Error(
                                error.message ?: "Sync failed"
                            )
                        } else {
                            // Auto-sync - don't show error in UI
                            _pocketbookSyncState.value = PocketbookSyncState.Idle
                        }
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Pocketbook sync exception: ${e.message}", e)
                if (askForUnknownBooks) {
                    _pocketbookSyncState.value = PocketbookSyncState.Error(
                        e.message ?: "Sync failed"
                    )
                } else {
                    _pocketbookSyncState.value = PocketbookSyncState.Idle
                }
            }
        }
    }

    fun resetPocketbookSyncState() {
        _pocketbookSyncState.value = PocketbookSyncState.Idle
    }

    fun testEmailConnection() {
        viewModelScope.launch {
            _emailTestState.value = EmailTestState.Loading
            try {
                val server = smtpServer.value
                val port = smtpPort.value
                val username = smtpUsername.value
                val password = smtpPassword.value

                if (server.isBlank() || username.isBlank() || password.isBlank()) {
                    _emailTestState.value = EmailTestState.Error("Bitte füllen Sie alle Felder aus")
                    return@launch
                }

                val result = pocketbookEmailService.testEmailConfiguration(
                    smtpServer = server,
                    smtpPort = port,
                    username = username,
                    password = password
                )

                result.fold(
                    onSuccess = {
                        _emailTestState.value = EmailTestState.Success
                    },
                    onFailure = { error ->
                        _emailTestState.value = EmailTestState.Error(
                            error.message ?: "Verbindung fehlgeschlagen"
                        )
                    }
                )
            } catch (e: Exception) {
                _emailTestState.value = EmailTestState.Error(
                    e.message ?: "Unbekannter Fehler"
                )
            }
        }
    }

    fun createBooksFromPocketbook(pocketbookBooks: List<com.readle.app.data.api.pocketbook.PocketbookBook>) {
        viewModelScope.launch {
            try {
                for (pbBook in pocketbookBooks) {
                    val normalizedTitle = com.readle.app.util.TextNormalizer.normalizeTitle(
                        pbBook.metadata.title ?: pbBook.title
                    )
                    val normalizedAuthor = com.readle.app.util.TextNormalizer.normalizeAuthor(
                        pbBook.metadata.authors ?: ""
                    )

                    val book = com.readle.app.data.model.BookEntity(
                        title = normalizedTitle,
                        author = normalizedAuthor,
                        isbn = pbBook.metadata.isbn,
                        originalTitle = pbBook.metadata.title ?: pbBook.title,
                        originalAuthor = pbBook.metadata.authors,
                        description = null,
                        publishDate = pbBook.metadata.year?.toString(),
                        language = null,
                        originalLanguage = null,
                        series = null,
                        seriesNumber = null,
                        isEBook = true,
                        comments = null,
                        rating = 0,
                        isOwned = true,
                        isRead = true,
                        dateAdded = System.currentTimeMillis(),
                        dateStarted = null,
                        dateFinished = System.currentTimeMillis(),
                        audiobookshelfId = null
                    )

                    bookRepository.insertBook(book)
                    android.util.Log.d(
                        "SettingsViewModel",
                        "Created book from Pocketbook: '${book.title}' by '${book.author}'"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error creating books: ${e.message}", e)
            }
        }
    }

    init {
        // Auto-sync Pocketbook on app start if credentials are available
        viewModelScope.launch {
            try {
                val email = settingsDataStore.pocketbookEmail.first()
                val password = settingsDataStore.pocketbookPassword.first()
                val token = settingsDataStore.pocketbookAccessToken.first()
                
                // Only auto-sync if credentials exist AND we have a cached token
                // (means user has successfully logged in before)
                if (email.isNotBlank() && password.isNotBlank() && token.isNotBlank()) {
                    android.util.Log.d(
                        "SettingsViewModel",
                        "Auto-syncing Pocketbook on app start (email: $email, has token: true)..."
                    )
                    
                    // Get Audiobookshelf token if available
                    val absToken = settingsDataStore.audiobookshelfApiToken.first().takeIf { it.isNotBlank() }
                    
                    // Call sync UseCase directly (don't use syncWithPocketbook which checks .value)
                    val result = syncPocketbookUseCase.execute(email, password, absToken)
                    result.fold(
                        onSuccess = { syncResult ->
                            android.util.Log.d(
                                "SettingsViewModel",
                                "Auto-sync completed: ${syncResult.booksUpdated} updated, " +
                                "${syncResult.booksNotFound} not found, " +
                                "${syncResult.audiobookshelfSynced} synced to Audiobookshelf"
                            )
                        },
                        onFailure = { error ->
                            android.util.Log.w(
                                "SettingsViewModel",
                                "Auto-sync failed: ${error.message}"
                            )
                        }
                    )
                } else {
                    android.util.Log.d(
                        "SettingsViewModel",
                        "Skipping auto-sync (email blank: ${email.isBlank()}, password blank: ${password.isBlank()}, token blank: ${token.isBlank()})"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("SettingsViewModel", "Error during auto-sync: ${e.message}", e)
            }
        }
    }
}

