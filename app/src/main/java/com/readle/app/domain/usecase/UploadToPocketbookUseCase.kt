package com.readle.app.domain.usecase

import android.content.Context
import com.readle.app.data.api.audiobookshelf.AudiobookshelfApiClient
import com.readle.app.data.api.pocketbook.PocketbookEmailService
import com.readle.app.data.model.BookEntity
import com.readle.app.data.preferences.SettingsDataStore
import com.readle.app.data.repository.BookRepository
import com.readle.app.domain.util.EpubMetadataPatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class UploadToPocketbookUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audiobookshelfClient: AudiobookshelfApiClient,
    private val pocketbookEmailService: PocketbookEmailService,
    private val settingsDataStore: SettingsDataStore,
    private val bookRepository: BookRepository
) {

    data class UploadResult(
        val totalBooks: Int,
        val successfulUploads: Int,
        val failedUploads: Int,
        val failedBooks: List<Pair<BookEntity, String>>
    )

    suspend fun execute(
        books: List<BookEntity>,
        forceReupload: Boolean = false,
        onProgress: suspend (currentBook: String, currentIndex: Int, total: Int) -> Unit = { _, _, _ -> }
    ): Result<UploadResult> {
        return withContext(Dispatchers.IO) {
            try {
                // Get credentials
                val absToken = settingsDataStore.audiobookshelfApiToken.first()
                val absServerUrl = settingsDataStore.audiobookshelfServerUrl.first()

                if (absToken.isBlank() || absServerUrl.isBlank()) {
                    return@withContext Result.failure(
                        Exception("Audiobookshelf credentials not configured")
                    )
                }

                // Initialize Audiobookshelf API
                audiobookshelfClient.initialize(absServerUrl)

                // Get SMTP settings (only email upload method is supported now)
                val smtpServer = settingsDataStore.smtpServer.first()
                val smtpPort = settingsDataStore.smtpPort.first()
                val smtpUsername = settingsDataStore.smtpUsername.first()
                val smtpPassword = settingsDataStore.smtpPassword.first()
                val smtpFromEmail = settingsDataStore.smtpFromEmail.first()
                val pocketbookEmail = settingsDataStore.pocketbookSendToEmail.first()

                if (smtpServer.isBlank() || smtpUsername.isBlank() || smtpPassword.isBlank() || 
                    smtpFromEmail.isBlank() || pocketbookEmail.isBlank()) {
                    return@withContext Result.failure(
                        Exception("Email configuration incomplete. Please configure SMTP settings.")
                    )
                }

                android.util.Log.d(
                    "PocketbookUpload",
                    "Using email upload: $smtpServer:$smtpPort -> $pocketbookEmail"
                )

                // Process each book
                var successfulUploads = 0
                var failedUploads = 0
                val failedBooks = mutableListOf<Pair<BookEntity, String>>()

                // Create temp directory for downloads
                val tempDir = File(context.cacheDir, "ebook_uploads").apply {
                    if (!exists()) mkdirs()
                }

                for ((index, book) in books.withIndex()) {
                    // Report progress
                    onProgress(book.title, index + 1, books.size)
                    
                    // Skip books that are already sent via email (unless force reupload is enabled)
                    if (book.uploadedViaEmail && !forceReupload) {
                        android.util.Log.d(
                            "PocketbookUpload",
                            "Skipping '${book.title}': Already sent via Email"
                        )
                        successfulUploads++
                        continue
                    }
                    
                    if (book.uploadedViaEmail && forceReupload) {
                        android.util.Log.d(
                            "PocketbookUpload",
                            "Re-uploading '${book.title}' via Email (force reupload enabled)"
                        )
                    }
                    
                    // CRITICAL: Never upload audiobooks, only eBooks!
                    if (!book.isEBook) {
                        failedBooks.add(book to "Not an eBook (audiobook detected)")
                        failedUploads++
                        android.util.Log.w(
                            "PocketbookUpload",
                            "Skipping '${book.title}': Not an eBook (audiobook)"
                        )
                        continue
                    }

                    val audiobookshelfId = book.audiobookshelfId
                    if (audiobookshelfId.isNullOrBlank()) {
                        failedBooks.add(book to "No Audiobookshelf ID")
                        failedUploads++
                        continue
                    }

                    var tempFile: File? = null
                    var fileToUpload: File? = null
                    try {
                        android.util.Log.d(
                            "PocketbookUpload",
                            "Getting eBook file info for '${book.title}'..."
                        )

                        // First, get the ebookFile.ino
                        val inoResult = audiobookshelfClient.getEbookFileIno(
                            absToken,
                            audiobookshelfId
                        )
                        
                        if (inoResult.isFailure) {
                            val error = inoResult.exceptionOrNull()?.message ?: "No eBook file found"
                            failedBooks.add(book to error)
                            failedUploads++
                            android.util.Log.e(
                                "PocketbookUpload",
                                "Failed to get eBook file INO for '${book.title}': $error"
                            )
                            continue
                        }
                        
                        val ebookFileIno = inoResult.getOrNull()!!
                        
                        android.util.Log.d(
                            "PocketbookUpload",
                            "Downloading '${book.title}' from Audiobookshelf (INO: $ebookFileIno)..."
                        )

                        // Create temporary file
                        val safeFileName = book.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                        tempFile = File(tempDir, "$safeFileName.epub")

                        // Download from Audiobookshelf to temp file (streaming)
                        val downloadResult = audiobookshelfClient.downloadEbookToFile(
                            absToken,
                            audiobookshelfId,
                            ebookFileIno,
                            tempFile
                        )
                        if (downloadResult.isFailure) {
                            val error = downloadResult.exceptionOrNull()?.message ?: "Download failed"
                            failedBooks.add(book to error)
                            failedUploads++
                            android.util.Log.e(
                                "PocketbookUpload",
                                "Failed to download '${book.title}': $error"
                            )
                            continue
                        }

                        val fileSize = downloadResult.getOrNull() ?: 0L

                        android.util.Log.d(
                            "PocketbookUpload",
                            "Downloaded '${book.title}' ($fileSize bytes)"
                        )

                        // Check if title cleaning is enabled
                        val cleanTitles = settingsDataStore.pocketbookCleanTitles.first()

                        // Patch EPUB metadata for Pocketbook compatibility (if series exists OR title cleaning is enabled)
                        fileToUpload = tempFile
                        if (book.series != null || cleanTitles) {
                            android.util.Log.d(
                                "PocketbookUpload",
                                "Patching EPUB metadata for '${book.title}' (series: ${book.series}, cleanTitles: $cleanTitles)..."
                            )
                            val patchResult = EpubMetadataPatcher.patchEpubForPocketbook(
                                tempFile,
                                cleanedTitle = if (cleanTitles) book.title else null
                            )
                            if (patchResult.isSuccess) {
                                val patchedFile = patchResult.getOrNull()!!
                                if (patchedFile != tempFile) {
                                    // Delete original and use patched version
                                    tempFile.delete()
                                    fileToUpload = patchedFile
                                    android.util.Log.d(
                                        "PocketbookUpload",
                                        "EPUB metadata patched successfully"
                                    )
                                }
                            } else {
                                android.util.Log.w(
                                    "PocketbookUpload",
                                    "Failed to patch EPUB metadata: ${patchResult.exceptionOrNull()?.message}"
                                )
                            }
                        }

                        // Upload to Pocketbook via Email
                        android.util.Log.d(
                            "PocketbookUpload",
                            "Sending '${book.title}' via email to $pocketbookEmail..."
                        )
                        val uploadResult = pocketbookEmailService.sendBookByEmail(
                            epubFile = fileToUpload!!,
                            bookTitle = book.title,
                            smtpServer = smtpServer,
                            smtpPort = smtpPort,
                            username = smtpUsername,
                            password = smtpPassword,
                            fromEmail = smtpFromEmail,
                            toEmail = pocketbookEmail
                        )

                        if (uploadResult.isFailure) {
                            val error = uploadResult.exceptionOrNull()?.message ?: "Upload failed"
                            failedBooks.add(book to error)
                            failedUploads++
                            android.util.Log.e(
                                "PocketbookUpload",
                                "Failed to upload '${book.title}': $error"
                            )
                            continue
                        }

                        successfulUploads++
                        android.util.Log.d(
                            "PocketbookUpload",
                            "Successfully uploaded '${book.title}' via Email"
                        )

                        // Mark book as sent via email
                        try {
                            val updatedBook = book.copy(
                                uploadedViaEmail = true,
                                inPocketbookCloud = true // Keep for backward compatibility
                            )
                            bookRepository.updateBook(updatedBook)
                            android.util.Log.d(
                                "PocketbookUpload",
                                "Marked '${book.title}' as sent via Email"
                            )
                        } catch (e: Exception) {
                            android.util.Log.w(
                                "PocketbookUpload",
                                "Failed to mark book as sent: ${e.message}"
                            )
                        }
                    } catch (e: Exception) {
                        failedBooks.add(book to (e.message ?: "Unknown error"))
                        failedUploads++
                        android.util.Log.e(
                            "PocketbookUpload",
                            "Error processing '${book.title}': ${e.message}",
                            e
                        )
                    } finally {
                        // Clean up temp file (and patched file if different)
                        try {
                            tempFile?.delete()
                            // If fileToUpload is different from tempFile, delete it too
                            if (fileToUpload != null && fileToUpload != tempFile) {
                                fileToUpload?.delete()
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("PocketbookUpload", "Failed to delete temp file: ${e.message}")
                        }
                    }
                }

                // Clean up temp directory
                try {
                    tempDir.deleteRecursively()
                } catch (e: Exception) {
                    android.util.Log.w("PocketbookUpload", "Failed to delete temp directory: ${e.message}")
                }

                val result = UploadResult(
                    totalBooks = books.size,
                    successfulUploads = successfulUploads,
                    failedUploads = failedUploads,
                    failedBooks = failedBooks
                )

                Result.success(result)
            } catch (e: Exception) {
                android.util.Log.e("PocketbookUpload", "Upload process failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}

