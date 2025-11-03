package com.readle.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val EXPORT_FORMAT = stringPreferencesKey("export_format")
        val SCANNER_LIBRARY = stringPreferencesKey("scanner_library")
        val DNB_API_ENABLED = booleanPreferencesKey("dnb_api_enabled")
        val GOOGLE_BOOKS_API_ENABLED = booleanPreferencesKey("google_books_api_enabled")
        val ISBN_DB_API_ENABLED = booleanPreferencesKey("isbn_db_api_enabled")
        val OPEN_LIBRARY_API_ENABLED = booleanPreferencesKey("open_library_api_enabled")
        val AUDIOBOOKSHELF_SERVER_URL = stringPreferencesKey("audiobookshelf_server_url")
        val AUDIOBOOKSHELF_API_TOKEN = stringPreferencesKey("audiobookshelf_api_token")
        val POCKETBOOK_EMAIL = stringPreferencesKey("pocketbook_email")
        val POCKETBOOK_PASSWORD = stringPreferencesKey("pocketbook_password")
        val POCKETBOOK_ACCESS_TOKEN = stringPreferencesKey("pocketbook_access_token")
        val POCKETBOOK_TOKEN_EXPIRY = longPreferencesKey("pocketbook_token_expiry")
        
        // Email settings for "Send to Pocketbook"
        val SMTP_SERVER = stringPreferencesKey("smtp_server")
        val SMTP_PORT = intPreferencesKey("smtp_port")
        val SMTP_USERNAME = stringPreferencesKey("smtp_username")
        val SMTP_PASSWORD = stringPreferencesKey("smtp_password")
        val SMTP_FROM_EMAIL = stringPreferencesKey("smtp_from_email")
        val POCKETBOOK_SEND_TO_EMAIL = stringPreferencesKey("pocketbook_send_to_email")
        val POCKETBOOK_UPLOAD_METHOD = stringPreferencesKey("pocketbook_upload_method")
        val POCKETBOOK_CLEAN_TITLES = booleanPreferencesKey("pocketbook_clean_titles")
        val BOOK_SORT_ORDER = stringPreferencesKey("book_sort_order")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val themeString = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        ThemeMode.valueOf(themeString)
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    val exportFormat: Flow<ExportFormat> = context.dataStore.data.map { preferences ->
        val formatString = preferences[PreferencesKeys.EXPORT_FORMAT] ?: ExportFormat.JSON.name
        ExportFormat.valueOf(formatString)
    }

    suspend fun setExportFormat(format: ExportFormat) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXPORT_FORMAT] = format.name
        }
    }

    val scannerLibrary: Flow<ScannerLibrary> = context.dataStore.data.map { preferences ->
        val libString = preferences[PreferencesKeys.SCANNER_LIBRARY] ?: ScannerLibrary.ML_KIT.name
        try {
            ScannerLibrary.valueOf(libString)
        } catch (e: IllegalArgumentException) {
            // Fallback for removed EXTERNAL option
            ScannerLibrary.ML_KIT
        }
    }

    suspend fun setScannerLibrary(library: ScannerLibrary) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCANNER_LIBRARY] = library.name
        }
    }

    val dnbApiEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DNB_API_ENABLED] ?: true
    }

    suspend fun setDnbApiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DNB_API_ENABLED] = enabled
        }
    }

    val googleBooksApiEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GOOGLE_BOOKS_API_ENABLED] ?: true
    }

    suspend fun setGoogleBooksApiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GOOGLE_BOOKS_API_ENABLED] = enabled
        }
    }

    val isbnDbApiEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ISBN_DB_API_ENABLED] ?: true
    }

    suspend fun setIsbnDbApiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ISBN_DB_API_ENABLED] = enabled
        }
    }

    val openLibraryApiEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.OPEN_LIBRARY_API_ENABLED] ?: true
    }

    suspend fun setOpenLibraryApiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OPEN_LIBRARY_API_ENABLED] = enabled
        }
    }

    val audiobookshelfServerUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUDIOBOOKSHELF_SERVER_URL] ?: ""
    }

    suspend fun setAudiobookshelfServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIOBOOKSHELF_SERVER_URL] = url
        }
    }

    val audiobookshelfApiToken: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUDIOBOOKSHELF_API_TOKEN] ?: ""
    }

    suspend fun setAudiobookshelfApiToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIOBOOKSHELF_API_TOKEN] = token
        }
    }

    val pocketbookEmail: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.POCKETBOOK_EMAIL] ?: ""
    }

    suspend fun setPocketbookEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.POCKETBOOK_EMAIL] = email
        }
    }

    val pocketbookPassword: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.POCKETBOOK_PASSWORD] ?: ""
    }

    suspend fun setPocketbookPassword(password: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.POCKETBOOK_PASSWORD] = password
        }
    }

    val pocketbookAccessToken: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.POCKETBOOK_ACCESS_TOKEN] ?: ""
    }

    suspend fun setPocketbookAccessToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.POCKETBOOK_ACCESS_TOKEN] = token
        }
    }

    val pocketbookTokenExpiry: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.POCKETBOOK_TOKEN_EXPIRY] ?: 0L
    }

    suspend fun setPocketbookTokenExpiry(expiry: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.POCKETBOOK_TOKEN_EXPIRY] = expiry
        }
    }

    suspend fun clearPocketbookToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.POCKETBOOK_ACCESS_TOKEN)
            preferences.remove(PreferencesKeys.POCKETBOOK_TOKEN_EXPIRY)
        }
    }

    // Email settings for "Send to Pocketbook"
    val smtpServer: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SMTP_SERVER] ?: ""
    }

    suspend fun setSmtpServer(server: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMTP_SERVER] = server
        }
    }

    val smtpPort: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SMTP_PORT] ?: 587 // Default SMTP port
    }

    suspend fun setSmtpPort(port: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMTP_PORT] = port
        }
    }

    val smtpUsername: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SMTP_USERNAME] ?: ""
    }

    suspend fun setSmtpUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMTP_USERNAME] = username
        }
    }

    val smtpPassword: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SMTP_PASSWORD] ?: ""
    }

    suspend fun setSmtpPassword(password: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMTP_PASSWORD] = password
        }
    }

    val smtpFromEmail: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SMTP_FROM_EMAIL] ?: ""
    }

    suspend fun setSmtpFromEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMTP_FROM_EMAIL] = email
        }
    }

    val pocketbookSendToEmail: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.POCKETBOOK_SEND_TO_EMAIL] ?: ""
    }

    suspend fun setPocketbookSendToEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.POCKETBOOK_SEND_TO_EMAIL] = email
        }
    }

    val pocketbookUploadMethod: Flow<PocketbookUploadMethod> = context.dataStore.data.map { preferences ->
        val methodString = preferences[PreferencesKeys.POCKETBOOK_UPLOAD_METHOD] 
            ?: PocketbookUploadMethod.EMAIL.name
        try {
            PocketbookUploadMethod.valueOf(methodString)
        } catch (e: IllegalArgumentException) {
            PocketbookUploadMethod.EMAIL
        }
    }

    suspend fun setPocketbookUploadMethod(method: PocketbookUploadMethod) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.POCKETBOOK_UPLOAD_METHOD] = method.name
        }
    }

    val pocketbookCleanTitles: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.POCKETBOOK_CLEAN_TITLES] ?: false
    }

    suspend fun setPocketbookCleanTitles(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.POCKETBOOK_CLEAN_TITLES] = enabled
        }
    }

    val bookSortOrder: Flow<com.readle.app.data.model.SortOrder> = context.dataStore.data.map { preferences ->
        val sortString = preferences[PreferencesKeys.BOOK_SORT_ORDER]
        com.readle.app.data.model.SortOrder.fromString(sortString)
    }

    suspend fun setBookSortOrder(sortOrder: com.readle.app.data.model.SortOrder) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BOOK_SORT_ORDER] = sortOrder.name
        }
    }
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class ExportFormat {
    JSON,
    XML
}

enum class ScannerLibrary {
    ML_KIT,
    ZXING
}

enum class PocketbookUploadMethod {
    CLOUD,  // Upload via Pocketbook Cloud API
    EMAIL   // Send via SMTP to Pocketbook email
}

