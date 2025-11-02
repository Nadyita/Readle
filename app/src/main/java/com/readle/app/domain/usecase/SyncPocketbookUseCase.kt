package com.readle.app.domain.usecase

import com.readle.app.data.api.pocketbook.PocketbookBook
import com.readle.app.data.api.pocketbook.PocketbookCloudApiClient
import com.readle.app.data.model.ReadingCategory
import com.readle.app.data.repository.BookRepository
import com.readle.app.data.preferences.SettingsDataStore
import com.readle.app.util.TextNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SyncPocketbookUseCase @Inject constructor(
    private val pocketbookClient: PocketbookCloudApiClient,
    private val bookRepository: BookRepository,
    private val audiobookshelfClient: com.readle.app.data.api.audiobookshelf.AudiobookshelfApiClient,
    private val settingsDataStore: SettingsDataStore
) {

    data class SyncResult(
        val totalBooksScanned: Int,
        val booksUpdated: Int,
        val booksNotFound: Int,
        val unmatchedBooks: List<PocketbookBook>,
        val audiobookshelfSynced: Int
    )

    suspend fun execute(
        email: String,
        password: String,
        audiobookshelfToken: String? = null
    ): Result<SyncResult> {
        return withContext(Dispatchers.IO) {
            try {
                // Get ALL books from Pocketbook Cloud (not just read ones)
                val syncResult = pocketbookClient.syncReadingProgress(email, password)

                if (syncResult.isFailure) {
                    return@withContext Result.failure(
                        syncResult.exceptionOrNull() ?: Exception("Sync failed")
                    )
                }

                val pocketbookBooks = syncResult.getOrNull() ?: emptyList()
                var booksUpdated = 0
                var booksNotFound = 0
                var audiobookshelfSynced = 0
                val unmatchedBooks = mutableListOf<PocketbookBook>()
                val matchedLocalBookIds = mutableSetOf<Long>()

                // Initialize Audiobookshelf API if token is available
                if (!audiobookshelfToken.isNullOrBlank()) {
                    val serverUrl = settingsDataStore.audiobookshelfServerUrl.first()
                    if (serverUrl.isNotBlank()) {
                        audiobookshelfClient.initialize(serverUrl)
                        android.util.Log.d(
                            "PocketbookSync",
                            "Initialized Audiobookshelf API with server: $serverUrl"
                        )
                    } else {
                        android.util.Log.w(
                            "PocketbookSync",
                            "Audiobookshelf token available but no server URL configured"
                        )
                    }
                }

                // Get all local books
                var localBooks = bookRepository.getAllBooksSnapshot()

                // First: Clear CLOUD API status from ALL books
                // This ensures we always reflect the current state of Pocketbook Cloud
                // EMAIL uploads are NOT touched (they don't appear in Cloud API)
                android.util.Log.d("PocketbookSync", "Clearing Cloud API status from all books before sync...")
                for (localBook in localBooks) {
                    if (localBook.uploadedToCloudApi) {
                        bookRepository.updateBook(
                            localBook.copy(uploadedToCloudApi = false)
                        )
                    }
                }

                // Reload local books after clearing to get fresh state
                localBooks = bookRepository.getAllBooksSnapshot()

                // For each Pocketbook book (any reading progress)
                for (pbBook in pocketbookBooks) {
                    // Try matching strategies in order of reliability
                    
                    // Strategy 1: Match by normalized title and author (same as Audiobookshelf)
                    val normalizedPbTitle = TextNormalizer.normalizeTitle(
                        pbBook.metadata.title ?: pbBook.title
                    )
                    val normalizedPbAuthor = TextNormalizer.normalizeAuthor(
                        pbBook.metadata.authors ?: ""
                    )

                    var matchedBook = localBooks.firstOrNull { localBook ->
                        val normalizedLocalTitle = TextNormalizer.normalizeTitle(localBook.title)
                        val normalizedLocalAuthor = TextNormalizer.normalizeAuthor(localBook.author)

                        normalizedLocalTitle == normalizedPbTitle &&
                                normalizedLocalAuthor == normalizedPbAuthor
                    }

                    // Strategy 2: If no match, try ISBN
                    if (matchedBook == null) {
                        val pbIsbn = pbBook.metadata.isbn?.trim()
                        if (!pbIsbn.isNullOrEmpty()) {
                            matchedBook = localBooks.firstOrNull { localBook ->
                                val localIsbn = localBook.isbn?.trim()
                                localIsbn != null && localIsbn.equals(pbIsbn, ignoreCase = true)
                            }
                            
                            if (matchedBook != null) {
                                android.util.Log.d(
                                    "PocketbookSync",
                                    "Matched '${pbBook.title}' by ISBN: $pbIsbn"
                                )
                            }
                        }
                    }
                    
                    // Strategy 3: If no match, try series name + series number (for books in series)
                    if (matchedBook == null) {
                        val pbSeriesName = pbBook.metadata.series?.trim()
                        // Try series_ord first (preferred), then sequence as fallback
                        val pbSeriesNumber = pbBook.metadata.seriesOrd?.trim() 
                            ?: pbBook.metadata.sequence?.toString()?.trim()
                        
                        if (!pbSeriesName.isNullOrEmpty() && !pbSeriesNumber.isNullOrEmpty()) {
                            matchedBook = localBooks.firstOrNull { localBook ->
                                val localSeries = localBook.series?.trim()
                                val localNumber = localBook.seriesNumber?.trim()
                                
                                !localSeries.isNullOrEmpty() && 
                                !localNumber.isNullOrEmpty() &&
                                localSeries.equals(pbSeriesName, ignoreCase = true) &&
                                localNumber == pbSeriesNumber
                            }
                            
                            if (matchedBook != null) {
                                android.util.Log.d(
                                    "PocketbookSync",
                                    "Matched '${pbBook.title}' by series: $pbSeriesName #$pbSeriesNumber"
                                )
                            }
                        }
                    }
                    
                    // Strategy 4: If in series but no sequence number, try series + similar title
                    if (matchedBook == null) {
                        val pbSeriesName = pbBook.metadata.series?.trim()
                        
                        if (!pbSeriesName.isNullOrEmpty()) {
                            // Get all books in the same series
                            val seriesBooks = localBooks.filter { localBook ->
                                val localSeries = localBook.series?.trim()
                                !localSeries.isNullOrEmpty() && 
                                localSeries.equals(pbSeriesName, ignoreCase = true)
                            }
                            
                            // Try to match by title similarity within the series
                            if (seriesBooks.isNotEmpty()) {
                                matchedBook = seriesBooks.firstOrNull { localBook ->
                                    // Check if titles are similar enough
                                    val pbTitleWords = normalizedPbTitle.split(" ").filter { it.length > 3 }
                                    val localTitleWords = TextNormalizer.normalizeTitle(localBook.title)
                                        .split(" ").filter { it.length > 3 }
                                    
                                    // If at least 50% of significant words match, consider it a match
                                    val commonWords = pbTitleWords.count { pbWord ->
                                        localTitleWords.any { it.contains(pbWord, ignoreCase = true) }
                                    }
                                    
                                    commonWords >= (pbTitleWords.size / 2)
                                }
                                
                                if (matchedBook != null) {
                                    android.util.Log.d(
                                        "PocketbookSync",
                                        "Matched '${pbBook.title}' by series + title similarity: $pbSeriesName"
                                    )
                                }
                            }
                        }
                    }

                    if (matchedBook != null) {
                        // Mark this book as being in Pocketbook Cloud (via API)
                        matchedLocalBookIds.add(matchedBook.id)
                        
                        // Prepare updated book entity (always mark as in cloud via API)
                        var updatedBook = matchedBook.copy(
                            uploadedToCloudApi = true,
                            inPocketbookCloud = true // Keep for backward compatibility
                        )
                        
                        android.util.Log.d(
                            "PocketbookSync",
                            "Marking '${matchedBook.title}' as in Pocketbook Cloud (via API)"
                        )
                        
                        // If book has >= 90% progress in Pocketbook, mark as READ
                        if (pbBook.readPercent >= 90) {
                            if (!matchedBook.isRead) {
                                updatedBook = updatedBook.copy(isRead = true, isOwned = true)
                                booksUpdated++
                                android.util.Log.d(
                                    "PocketbookSync",
                                    "Marked '${matchedBook.title}' as READ (${pbBook.readPercent}% in Pocketbook)"
                                )
                                
                                // Also update Audiobookshelf if book is linked and token is available
                                val absId = matchedBook.audiobookshelfId
                                if (!absId.isNullOrBlank() && !audiobookshelfToken.isNullOrBlank()) {
                                    android.util.Log.d(
                                        "PocketbookSync",
                                        "Attempting to sync '${matchedBook.title}' to Audiobookshelf (ID: $absId)"
                                    )
                                    try {
                                        val absResult = audiobookshelfClient.updateBookProgress(
                                            token = audiobookshelfToken,
                                            libraryItemId = absId,
                                            isFinished = true
                                        )
                                        if (absResult.isSuccess) {
                                            audiobookshelfSynced++
                                            android.util.Log.d(
                                                "PocketbookSync",
                                                "Successfully synced '${matchedBook.title}' to Audiobookshelf"
                                            )
                                        } else {
                                            val error = absResult.exceptionOrNull()
                                            android.util.Log.w(
                                                "PocketbookSync",
                                                "Failed to sync '${matchedBook.title}' to Audiobookshelf: ${error?.message}"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e(
                                            "PocketbookSync",
                                            "Error syncing '${matchedBook.title}' to Audiobookshelf: ${e.message}",
                                            e
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Always update book (cloud status + possibly READ status)
                        bookRepository.updateBook(updatedBook)
                        android.util.Log.d(
                            "PocketbookSync",
                            "Updated '${matchedBook.title}' in database (inPocketbookCloud: true, isOwned: ${updatedBook.isOwned}, isRead: ${updatedBook.isRead})"
                        )
                    } else {
                        // No match found
                        // Only add to unmatched list if book has high progress (for user to manually add)
                        if (pbBook.readPercent >= 90) {
                            unmatchedBooks.add(pbBook)
                        }
                        booksNotFound++
                        
                        // Build detailed debug info
                        val debugInfo = buildString {
                            append("'${pbBook.title}' by '${pbBook.metadata.authors}'")
                            pbBook.metadata.isbn?.let { append(" | ISBN: $it") }
                            if (!pbBook.metadata.series.isNullOrEmpty()) {
                                append(" | Series: ${pbBook.metadata.series}")
                                // Show series_ord if available, otherwise sequence
                                val seriesNum = pbBook.metadata.seriesOrd ?: pbBook.metadata.sequence?.toString()
                                seriesNum?.let { append(" #$it") }
                            }
                            append(" (${pbBook.readPercent}% read)")
                        }
                        
                        android.util.Log.d(
                            "PocketbookSync",
                            "No match for Pocketbook book: $debugInfo"
                        )
                    }
                }

                android.util.Log.d(
                    "PocketbookSync",
                    "Sync summary: ${matchedLocalBookIds.size} books marked as in Pocketbook Cloud"
                )

                val result = SyncResult(
                    totalBooksScanned = pocketbookBooks.size,
                    booksUpdated = booksUpdated,
                    booksNotFound = booksNotFound,
                    unmatchedBooks = unmatchedBooks,
                    audiobookshelfSynced = audiobookshelfSynced
                )

                Result.success(result)
            } catch (e: Exception) {
                android.util.Log.e("PocketbookSync", "Sync failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}

