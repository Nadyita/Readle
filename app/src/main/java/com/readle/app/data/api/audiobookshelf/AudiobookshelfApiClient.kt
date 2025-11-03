package com.readle.app.data.api.audiobookshelf

import com.readle.app.data.model.BookEntity
import com.readle.app.data.model.ReadingCategory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

class AudiobookshelfApiClient @Inject constructor() {

    private var retrofit: Retrofit? = null
    private var apiService: AudiobookshelfApiService? = null

    fun initialize(serverUrl: String) {
        val cleanUrl = serverUrl.trim().removeSuffix("/")

        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            // Enable connection pooling with more connections for parallel requests
            .connectionPool(okhttp3.ConnectionPool(20, 5, java.util.concurrent.TimeUnit.MINUTES))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .build()
                android.util.Log.d("AudiobookshelfApi", "Request: ${request.method} ${request.url}")
                val response = chain.proceed(request)
                android.util.Log.d("AudiobookshelfApi", "Response: ${response.code} for ${request.url}")
                response
            }
            .build()

        val gson = com.google.gson.GsonBuilder()
            .setLenient()
            .create()

        retrofit = Retrofit.Builder()
            .baseUrl("$cleanUrl/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        apiService = retrofit?.create(AudiobookshelfApiService::class.java)
    }

    suspend fun login(username: String, password: String): Result<String> {
        return try {
            val service = apiService ?: return Result.failure(
                Exception("Audiobookshelf API not initialized. Please set server URL first.")
            )

            val request = AudiobookshelfLoginRequest(username, password)
            val response = service.login(request)

            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!.user.token
                Result.success(token)
            } else {
                android.util.Log.e("AudiobookshelfApi", "Login failed: ${response.code()} ${response.message()}")
                Result.failure(Exception("Login failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AudiobookshelfApi", "Login exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun importEbooks(
        token: String, 
        existingBooks: List<BookEntity>,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Result<List<BookEntity>> {
        return try {
            val service = apiService ?: return Result.failure(
                Exception("Audiobookshelf API not initialized.")
            )

            // Get all libraries
            val authHeader = "Bearer $token"
            val librariesResponse = service.getLibraries(authHeader)

            if (!librariesResponse.isSuccessful || librariesResponse.body() == null) {
                return Result.failure(Exception("Failed to fetch libraries: ${librariesResponse.code()}"))
            }

            val libraries = librariesResponse.body()!!.libraries

            // Get user progress data from authorize endpoint
            val progressMap = mutableMapOf<String, Boolean>() // libraryItemId -> isFinished
            try {
                val authorizeResponse = service.authorize(authHeader)
                if (authorizeResponse.isSuccessful && authorizeResponse.body() != null) {
                    val mediaProgressList = authorizeResponse.body()!!.user.mediaProgress
                    mediaProgressList.forEach { progress ->
                        progressMap[progress.libraryItemId] = progress.isFinished
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AudiobookshelfImport", "Failed to load progress data: ${e.message}", e)
            }

            // Filter only book libraries (not podcasts)
            val bookLibraries = libraries.filter { it.mediaType == "book" }

            val allImportedBooks = mutableListOf<BookEntity>()

            // Count total eBooks across all libraries first
            var totalEbooks = 0
            val ebooksByLibrary = mutableMapOf<String, List<AudiobookshelfLibraryItem>>()
            
            for (library in bookLibraries) {
                val itemsResponse = service.getLibraryItems(library.id, authHeader)
                if (itemsResponse.isSuccessful && itemsResponse.body() != null) {
                    val items = itemsResponse.body()!!.results
                    val ebooks = items.filter { item ->
                        item.mediaType == "book" && !item.media.ebookFormat.isNullOrBlank()
                    }
                    ebooksByLibrary[library.id] = ebooks
                    totalEbooks += ebooks.size
                }
            }
            
            android.util.Log.d("AudiobookshelfImport", "Total eBooks to import: $totalEbooks")
            
            // Send initial progress update
            onProgress?.invoke(0, totalEbooks)
            
            var processedCount = 0

            // For each book library, get all items
            for (library in bookLibraries) {
                val ebooks = ebooksByLibrary[library.id] ?: emptyList()
                
                if (ebooks.isNotEmpty()) {
                    android.util.Log.d("AudiobookshelfImport", 
                        "Processing ${ebooks.size} eBooks from library ${library.name}"
                    )

                    // Identify which books need full details (new books only)
                    val booksNeedingDetails = ebooks.filter { item ->
                        existingBooks.none { it.audiobookshelfId == item.id }
                    }
                    
                    android.util.Log.d("AudiobookshelfImport", 
                        "Need full details for ${booksNeedingDetails.size} new books, ${ebooks.size - booksNeedingDetails.size} existing books"
                    )
                    
                    // Fetch full details in parallel for new books (limit concurrency to avoid overwhelming server)
                    val itemsWithDetails = if (booksNeedingDetails.isNotEmpty()) {
                        coroutineScope {
                            val chunkSize = 10 // Process 10 books at a time
                            val allDetails = mutableMapOf<String, AudiobookshelfLibraryItem>()
                            
                            for (chunk in booksNeedingDetails.chunked(chunkSize)) {
                                val deferredDetails = chunk.map { item ->
                                    async {
                                        try {
                                            val detailResponse = service.getLibraryItem(item.id, authHeader)
                                            if (detailResponse.isSuccessful && detailResponse.body() != null) {
                                                item.id to detailResponse.body()!!
                                            } else {
                                                android.util.Log.w("AudiobookshelfImport", 
                                                    "Failed to fetch details for ${item.id}, using list data"
                                                )
                                                item.id to item
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.w("AudiobookshelfImport", 
                                                "Exception fetching details for ${item.id}: ${e.message}"
                                            )
                                            item.id to item
                                        }
                                    }
                                }
                                
                                // Wait for this chunk to complete and update progress
                                deferredDetails.forEach { deferred ->
                                    val (id, details) = deferred.await()
                                    allDetails[id] = details
                                    
                                    // Update progress for fetched details
                                    processedCount++
                                    onProgress?.invoke(processedCount, totalEbooks)
                                }
                            }
                            
                            allDetails
                        }
                    } else {
                        emptyMap()
                    }
                    
                    // Now process all books sequentially for progress reporting
                    for (item in ebooks) {
                        try {
                            // Only increment for books we haven't already counted during detail fetching
                            if (!itemsWithDetails.containsKey(item.id)) {
                                processedCount++
                                onProgress?.invoke(processedCount, totalEbooks)
                            }
                            
                            android.util.Log.d("AudiobookshelfImport", 
                                "Processing $processedCount/$totalEbooks: ${item.media.metadata.title}"
                            )
                            
                            // Use detailed data if available, otherwise use list data
                            val itemWithDetails = itemsWithDetails[item.id] ?: item
                            
                            val book = convertToBookEntity(itemWithDetails, existingBooks, progressMap)
                            allImportedBooks.add(book)
                            
                            // Sync back to ABS if local book is READ but ABS says not finished
                            val absIsFinished = progressMap[item.id] ?: false
                            if (book.isRead && !absIsFinished) {
                                updateBookProgress(token, item.id, isFinished = true)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AudiobookshelfImport", "Failed to convert item ${item.id}: ${e.message}")
                        }
                    }
                }
            }
            Result.success(allImportedBooks)
        } catch (e: Exception) {
            android.util.Log.e("AudiobookshelfImport", "Import failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateBookProgress(token: String, libraryItemId: String, isFinished: Boolean): Result<Unit> {
        return try {
            val service = apiService ?: return Result.failure(
                Exception("Audiobookshelf API not initialized.")
            )
            
            val authHeader = "Bearer $token"
            val request = AudiobookshelfUpdateProgressRequest(isFinished = isFinished)
            val response = service.updateProgress(libraryItemId, authHeader, request)
            
            if (response.isSuccessful) {
                android.util.Log.d("AudiobookshelfSync", "Successfully updated progress for item $libraryItemId")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "No error body"
                android.util.Log.e(
                    "AudiobookshelfSync",
                    "Failed to update progress for $libraryItemId: ${response.code()} ${response.message()} - $errorBody"
                )
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()} - $errorBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AudiobookshelfSync", "Error updating progress: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getEbookFileIno(
        token: String,
        libraryItemId: String
    ): Result<String> {
        return try {
            val service = apiService ?: return Result.failure(
                Exception("Audiobookshelf API not initialized.")
            )
            
            val authHeader = "Bearer $token"
            val response = service.getLibraryItem(libraryItemId, authHeader)
            
            if (response.isSuccessful && response.body() != null) {
                val ino = response.body()!!.media.ebookFile?.ino
                if (ino != null) {
                    Result.success(ino)
                } else {
                    Result.failure(Exception("No eBook file found in item"))
                }
            } else {
                Result.failure(Exception("Failed to get library item: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AudiobookshelfApi", "Error getting ebook INO: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun downloadEbookToFile(
        token: String,
        libraryItemId: String,
        ebookFileIno: String,
        outputFile: java.io.File
    ): Result<Long> {
        return try {
            val service = apiService ?: return Result.failure(
                Exception("Audiobookshelf API not initialized.")
            )
            
            val authHeader = "Bearer $token"
            val response = service.downloadEbookFile(libraryItemId, ebookFileIno, authHeader)
            
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.failure(
                    Exception("Empty response body")
                )
                
                // Stream to file to avoid loading entire file into memory
                var totalBytes = 0L
                body.byteStream().use { inputStream ->
                    outputFile.outputStream().use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                        }
                    }
                }
                
                android.util.Log.d(
                    "AudiobookshelfDownload",
                    "Downloaded ebook $libraryItemId to ${outputFile.name} ($totalBytes bytes)"
                )
                Result.success(totalBytes)
            } else {
                val errorBody = response.errorBody()?.string() ?: "No error body"
                android.util.Log.e(
                    "AudiobookshelfDownload",
                    "Failed to download ebook $libraryItemId: ${response.code()} ${response.message()} - $errorBody"
                )
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()} - $errorBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AudiobookshelfDownload", "Error downloading ebook: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun convertToBookEntity(
        item: AudiobookshelfLibraryItem,
        existingBooks: List<BookEntity>,
        progressMap: Map<String, Boolean> = emptyMap()
    ): BookEntity {
        val metadata = item.media.metadata
        
        // Determine the best dateAdded timestamp
        // Priority: 1. mtimeMs from ebook file (most reliable)
        //           2. addedAt from library item
        //           3. Current time as fallback
        val dateAdded = determineBestDateAdded(item)
        
        // Get original (non-normalized) values for search
        val originalTitle = metadata.title ?: "Unknown Title"
        val originalAuthor = when {
            !metadata.authorName.isNullOrBlank() -> com.readle.app.util.AuthorSeparatorConverter.convertAuthorSeparator(metadata.authorName!!, isLastNameFirst = false)
            !metadata.authors.isNullOrEmpty() -> metadata.authors.joinToString("; ") { it.name }
            else -> "Unknown Author"
        }
        
        // Get normalized values (prefer already normalized fields from ABS)
        val rawTitle = metadata.titleIgnorePrefix ?: metadata.title ?: "Unknown Title"
        
        val rawAuthor = when {
            !metadata.authorNameLF.isNullOrBlank() -> com.readle.app.util.AuthorSeparatorConverter.convertAuthorSeparator(metadata.authorNameLF!!, isLastNameFirst = true)
            !metadata.authors.isNullOrEmpty() -> metadata.authors.joinToString("; ") { it.name }
            !metadata.authorName.isNullOrBlank() -> com.readle.app.util.AuthorSeparatorConverter.convertAuthorSeparator(metadata.authorName!!, isLastNameFirst = false)
            else -> "Unknown Author"
        }
        
        // Normalize title and author using our TextNormalizer
        val title = com.readle.app.util.TextNormalizer.normalizeTitle(rawTitle)
        val author = com.readle.app.util.TextNormalizer.normalizeAuthor(rawAuthor)
        
        // Trim description and remove "Kurzbeschreibung" prefix with any surrounding whitespace
        val description = metadata.description?.trim()
            ?.replace(Regex("^\\n?[kK]urzbeschreibung\\s*\\n+"), "")
            ?.trim()
        
        // Try multiple series fields
        var series = when {
            !metadata.series.isNullOrEmpty() -> metadata.series.first().name
            !metadata.seriesName.isNullOrBlank() -> metadata.seriesName
            else -> null
        }
        
        // Check if series is actually the author name (Audiobookshelf bug)
        // This happens when Audiobookshelf has no series but sets the author name in seriesName
        if (series != null) {
            val isAuthorName = series.equals(metadata.authorName, ignoreCase = true) ||
                               series.equals(metadata.authorNameLF, ignoreCase = true) ||
                               metadata.authors?.any { it.name.equals(series, ignoreCase = true) } == true
            if (isAuthorName) {
                series = null
            }
        }
        
        var seriesNumber = metadata.series?.firstOrNull()?.sequence
        
        // If series contains a number pattern like "#7" or "Band 7", extract it (including decimals like "4.5")
        if (series != null && seriesNumber.isNullOrBlank()) {
            // Pattern: "Series Name #7" or "Series Name, Band 7" or "Series Name - 7" (supports decimals)
            val patterns = listOf(
                Regex("""^(.+?)\s*#\s*(\d+(?:\.\d+)?)$"""),           // "Alea Aquarius #7" or "#7.5"
                Regex("""^(.+?)\s*,?\s*Band\s+(\d+(?:\.\d+)?)$"""),   // "Alea Aquarius, Band 7"
                Regex("""^(.+?)\s*-\s*(\d+(?:\.\d+)?)$"""),           // "Alea Aquarius - 7"
                Regex("""^(.+?)\s*\((\d+(?:\.\d+)?)\)$"""),           // "Alea Aquarius (7)"
                Regex("""^(.+?)\s*:\s*(\d+(?:\.\d+)?)$""")            // "Alea Aquarius: 7"
            )
            
            for (pattern in patterns) {
                val match = pattern.find(series!!)
                if (match != null) {
                    series = match.groupValues[1].trim()
                    seriesNumber = match.groupValues[2]
                    break
                }
            }
        }
        
        // Clean up title: Remove redundant series name and number prefix
        // Special handling for series with articles that Audiobookshelf moves to the end
        var cleanedTitle = title
        
        // DEBUG: Log input (only for "Kristall" books)
        val isDebugBook = rawTitle.contains("Kristall", ignoreCase = true)
        if (isDebugBook) {
            android.util.Log.d("TitleCleanup", "=== START ===")
            android.util.Log.d("TitleCleanup", "rawTitle: $rawTitle")
            android.util.Log.d("TitleCleanup", "title (normalized): $title")
            android.util.Log.d("TitleCleanup", "series: $series")
            android.util.Log.d("TitleCleanup", "seriesNumber: $seriesNumber")
        }
        
        if (series != null && seriesNumber != null) {
            // Check if series starts with an article
            val articlePattern = Regex("""^(Der|Die|Das|The|A|An)\s+(.+)$""", RegexOption.IGNORE_CASE)
            val articleMatch = articlePattern.find(series)
            
            var workingTitle = rawTitle
            
            if (articleMatch != null) {
                // Series starts with article (e.g., "Der Donnerstagsmordclub")
                val seriesArticle = articleMatch.groupValues[1]
                var seriesWithoutArticle = articleMatch.groupValues[2]
                
                // Remove trailing article from title (e.g., ", Der")
                val trailingArticlePattern = Regex(""",\s+${Regex.escape(seriesArticle)}$""", RegexOption.IGNORE_CASE)
                workingTitle = workingTitle.replace(trailingArticlePattern, "")
                
                // Try to remove series name (without article) + number/range from beginning
                var normalizedSeriesWithoutArticle = com.readle.app.util.TextNormalizer.normalizeTitle(seriesWithoutArticle)
                
                if (isDebugBook) {
                    android.util.Log.d("TitleCleanup", "Branch: with article")
                    android.util.Log.d("TitleCleanup", "workingTitle: $workingTitle")
                    android.util.Log.d("TitleCleanup", "normalizedSeriesWithoutArticle: $normalizedSeriesWithoutArticle")
                }
                
                var seriesPattern = Regex(
                    """^${Regex.escape(normalizedSeriesWithoutArticle)}\s*(?:-\s*)?#?\s*0*${Regex.escape(seriesNumber)}(?:-\d+(?:\.\d+)?)?\s*[-:]\s*(.+)$""",
                    RegexOption.IGNORE_CASE
                )
                var match = seriesPattern.find(workingTitle)
                if (isDebugBook) {
                    android.util.Log.d("TitleCleanup", "Main pattern matched: ${match != null}")
                }
                
                // Fallback 1: Try without series suffix (e.g., "-Saga", "-Anthologie")
                if (match == null) {
                    val suffixPattern = Regex("""-(Saga|Anthologie|Anthology|Reihe|Serie|Series|Sammlung|Collection)$""", RegexOption.IGNORE_CASE)
                    val suffixMatch = suffixPattern.find(seriesWithoutArticle)
                    if (suffixMatch != null) {
                        seriesWithoutArticle = seriesWithoutArticle.replace(suffixPattern, "")
                        normalizedSeriesWithoutArticle = com.readle.app.util.TextNormalizer.normalizeTitle(seriesWithoutArticle)
                        seriesPattern = Regex(
                            """^${Regex.escape(normalizedSeriesWithoutArticle)}\s*(?:-\s*)?#?\s*0*${Regex.escape(seriesNumber)}(?:-\d+(?:\.\d+)?)?\s*[-:]\s*(.+)$""",
                            RegexOption.IGNORE_CASE
                        )
                        match = seriesPattern.find(workingTitle)
                    }
                }
                
                // Fallback 2: For decimal numbers (e.g., "7.5"), try with letter suffix (e.g., "7a", "07a")
                if (match == null && seriesNumber.contains(".")) {
                    val intPart = seriesNumber.substringBefore(".")
                    seriesPattern = Regex(
                        """^${Regex.escape(normalizedSeriesWithoutArticle)}\s*(?:-\s*)?#?\s*0*${Regex.escape(intPart)}[a-z]?\s*[-:]\s*(.+)$""",
                        RegexOption.IGNORE_CASE
                    )
                    match = seriesPattern.find(workingTitle)
                }
                
                // Fallback 3: For book #1 ONLY, if title is just "Series 1" without additional title
                // E.g., "Seiten der Welt 1, Die" -> "Seiten der Welt, Die"
                // BUT: Don't match "Series 1-3" (omnibus/collection) - keep the full title
                if (match == null && seriesNumber == "1") {
                    if (isDebugBook) {
                        android.util.Log.d("TitleCleanup", "Fallback 3: book #1 check")
                    }
                    // First check if it's an omnibus (e.g., "Series 1-3" or "Series 1-4")
                    val omnibusPattern = Regex(
                        """^(${Regex.escape(normalizedSeriesWithoutArticle)})\s*(?:-\s*)?#?\s*0*1-\d+$""",
                        RegexOption.IGNORE_CASE
                    )
                    val omnibusMatch = omnibusPattern.find(workingTitle)
                    if (isDebugBook) {
                        android.util.Log.d("TitleCleanup", "Omnibus pattern matched: ${omnibusMatch != null}")
                    }
                    if (omnibusMatch != null) {
                        // It's an omnibus - keep the full title including the range
                        cleanedTitle = com.readle.app.util.TextNormalizer.normalizeTitle(workingTitle + ", $seriesArticle")
                        if (isDebugBook) {
                            android.util.Log.d("TitleCleanup", "Omnibus detected, cleanedTitle: $cleanedTitle")
                        }
                    } else {
                        // Check if it's just "Series 1" without range
                        seriesPattern = Regex(
                            """^(${Regex.escape(normalizedSeriesWithoutArticle)})\s*(?:-\s*)?#?\s*0*1$""",
                            RegexOption.IGNORE_CASE
                        )
                        val bookOneMatch = seriesPattern.find(workingTitle)
                        if (isDebugBook) {
                            android.util.Log.d("TitleCleanup", "Book #1 pattern matched: ${bookOneMatch != null}")
                        }
                        if (bookOneMatch != null) {
                            // Just remove the "1" and keep series name
                            cleanedTitle = com.readle.app.util.TextNormalizer.normalizeTitle(bookOneMatch.groupValues[1].trim() + ", $seriesArticle")
                            if (isDebugBook) {
                                android.util.Log.d("TitleCleanup", "Book #1 detected, cleanedTitle: $cleanedTitle")
                            }
                        }
                    }
                }
                
                if (match != null) {
                    val extractedTitle = match.groupValues[1].trim()
                    
                    // Check if extracted title starts with lowercase letter
                    if (extractedTitle.isNotEmpty() && extractedTitle[0].isLowerCase()) {
                        // Special case: Only remove the number/range, keep series name
                        // Remove only number/range: "Donnerstagsmordclub 5: " -> "Donnerstagsmordclub und ..."
                        val numberPattern = if (seriesNumber.contains(".")) {
                            // For decimals, also match letter suffix (e.g., "7a" for "7.5")
                            val intPart = seriesNumber.substringBefore(".")
                            Regex("""^(${Regex.escape(normalizedSeriesWithoutArticle)})\s*(?:-\s*)?#?\s*0*${Regex.escape(intPart)}[a-z]?(?:-\d+(?:\.\d+)?)?\s*[-:]\s*""", RegexOption.IGNORE_CASE)
                        } else {
                            Regex("""^(${Regex.escape(normalizedSeriesWithoutArticle)})\s*(?:-\s*)?#?\s*0*${Regex.escape(seriesNumber)}(?:-\d+(?:\.\d+)?)?\s*[-:]\s*""", RegexOption.IGNORE_CASE)
                        }
                        val titleWithSeries = workingTitle.replace(numberPattern, "$1 ")
                        // Add article at the end: "..., Der"
                        cleanedTitle = com.readle.app.util.TextNormalizer.normalizeTitle("$titleWithSeries, $seriesArticle")
                    } else {
                        // Normal case: Title starts with uppercase, use extracted title without article
                        cleanedTitle = com.readle.app.util.TextNormalizer.normalizeTitle(extractedTitle)
                    }
                }
            } else {
                // Series doesn't start with article
                var workingSeries = series
                var normalizedSeries = com.readle.app.util.TextNormalizer.normalizeTitle(workingSeries)
                
                if (isDebugBook) {
                    android.util.Log.d("TitleCleanup", "Branch: without article")
                    android.util.Log.d("TitleCleanup", "rawTitle: $rawTitle")
                    android.util.Log.d("TitleCleanup", "normalizedSeries: $normalizedSeries")
                }
                
                // Special case: Check for omnibus/collection FIRST (e.g., "Series 1-3" where seriesNumber="1")
                // This must be checked BEFORE the main pattern to prevent the "-" being interpreted as a separator
                var match: MatchResult? = null
                var isOmnibus = false
                var seriesPattern: Regex
                
                if (seriesNumber == "1") {
                    val omnibusCheckPattern = Regex(
                        """^${Regex.escape(normalizedSeries)}\s*(?:-\s*)?#?\s*0*1-\d+$""",
                        RegexOption.IGNORE_CASE
                    )
                    val omnibusCheck = omnibusCheckPattern.find(rawTitle)
                    if (isDebugBook) {
                        android.util.Log.d("TitleCleanup", "Omnibus check pattern matched: ${omnibusCheck != null}")
                    }
                    if (omnibusCheck != null) {
                        // It's an omnibus - keep the full title as-is
                        cleanedTitle = com.readle.app.util.TextNormalizer.normalizeTitle(rawTitle)
                        isOmnibus = true
                        if (isDebugBook) {
                            android.util.Log.d("TitleCleanup", "Omnibus detected early, keeping full title: $cleanedTitle")
                        }
                    }
                }
                
                // Only run normal pattern matching if omnibus wasn't detected
                if (!isOmnibus) {
                    seriesPattern = Regex(
                        """^${Regex.escape(normalizedSeries)}\s*(?:-\s*)?#?\s*0*${Regex.escape(seriesNumber)}(?:-\d+(?:\.\d+)?)?\s*[-:]\s*(.+)$""",
                        RegexOption.IGNORE_CASE
                    )
                    match = seriesPattern.find(rawTitle)
                    
                    if (isDebugBook) {
                        android.util.Log.d("TitleCleanup", "Main pattern matched: ${match != null}")
                        if (match != null) {
                            android.util.Log.d("TitleCleanup", "Extracted: '${match.groupValues[1]}'")
                        }
                    }
                }
                
                // Fallback 1: Try without series suffix (e.g., "-Saga", "-Anthologie")
                if (!isOmnibus && match == null) {
                    val suffixPattern = Regex("""-(Saga|Anthologie|Anthology|Reihe|Serie|Series|Sammlung|Collection)$""", RegexOption.IGNORE_CASE)
                    val suffixMatch = suffixPattern.find(workingSeries)
                    if (suffixMatch != null) {
                        workingSeries = workingSeries.replace(suffixPattern, "")
                        normalizedSeries = com.readle.app.util.TextNormalizer.normalizeTitle(workingSeries)
                        seriesPattern = Regex(
                            """^${Regex.escape(normalizedSeries)}\s*(?:-\s*)?#?\s*0*${Regex.escape(seriesNumber)}(?:-\d+(?:\.\d+)?)?\s*[-:]\s*(.+)$""",
                            RegexOption.IGNORE_CASE
                        )
                        match = seriesPattern.find(rawTitle)
                    }
                }
                
                // Fallback 2: For decimal numbers (e.g., "7.5"), try with letter suffix (e.g., "7a", "07a")
                if (!isOmnibus && match == null && seriesNumber.contains(".")) {
                    val intPart = seriesNumber.substringBefore(".")
                    seriesPattern = Regex(
                        """^${Regex.escape(normalizedSeries)}\s*(?:-\s*)?#?\s*0*${Regex.escape(intPart)}[a-z]?\s*[-:]\s*(.+)$""",
                        RegexOption.IGNORE_CASE
                    )
                    match = seriesPattern.find(rawTitle)
                }
                
                // Fallback 3: For book #1 ONLY, if title is just "Series 1" without additional title
                // E.g., "Foundation 1" -> "Foundation"
                // BUT: Don't match "Foundation 1-3" (omnibus/collection) - keep the full title
                // NOTE: Omnibus is already checked above, so we can skip this entirely if isOmnibus is true
                if (!isOmnibus && match == null && seriesNumber == "1") {
                    // First check if it's an omnibus (e.g., "Series 1-3" or "Series 1-4")
                    val omnibusPattern = Regex(
                        """^(${Regex.escape(normalizedSeries)})\s*(?:-\s*)?#?\s*0*1-\d+$""",
                        RegexOption.IGNORE_CASE
                    )
                    val omnibusMatch = omnibusPattern.find(rawTitle)
                    if (omnibusMatch != null) {
                        // It's an omnibus - keep the full title including the range
                        cleanedTitle = com.readle.app.util.TextNormalizer.normalizeTitle(rawTitle)
                    } else {
                        // Check if it's just "Series 1" without range
                        seriesPattern = Regex(
                            """^(${Regex.escape(normalizedSeries)})\s*(?:-\s*)?#?\s*0*1$""",
                            RegexOption.IGNORE_CASE
                        )
                        val bookOneMatch = seriesPattern.find(rawTitle)
                        if (bookOneMatch != null) {
                            // Just remove the "1" and keep series name
                            cleanedTitle = com.readle.app.util.TextNormalizer.normalizeTitle(bookOneMatch.groupValues[1].trim())
                        }
                    }
                }
                
            if (!isOmnibus && match != null) {
                val extractedTitle = match.groupValues[1].trim()
                    
                    // Check if extracted title starts with lowercase letter
                    if (extractedTitle.isNotEmpty() && extractedTitle[0].isLowerCase()) {
                        // Special case: Only remove the number/range, keep series name
                        val numberPattern = if (seriesNumber.contains(".")) {
                            // For decimals, also match letter suffix (e.g., "7a" for "7.5")
                            val intPart = seriesNumber.substringBefore(".")
                            Regex("""^(${Regex.escape(normalizedSeries)})\s*(?:-\s*)?#?\s*0*${Regex.escape(intPart)}[a-z]?(?:-\d+(?:\.\d+)?)?\s*[-:]\s*""", RegexOption.IGNORE_CASE)
                        } else {
                            Regex("""^(${Regex.escape(normalizedSeries)})\s*(?:-\s*)?#?\s*0*${Regex.escape(seriesNumber)}(?:-\d+(?:\.\d+)?)?\s*[-:]\s*""", RegexOption.IGNORE_CASE)
                        }
                        val titleWithSeries = rawTitle.replace(numberPattern, "$1 ")
                        cleanedTitle = com.readle.app.util.TextNormalizer.normalizeTitle(titleWithSeries)
                    } else {
                        // Normal case: Title starts with uppercase
                cleanedTitle = com.readle.app.util.TextNormalizer.normalizeTitle(extractedTitle)
                    }
                }
            }
        }
        
        if (isDebugBook) {
            android.util.Log.d("TitleCleanup", "=== RESULT ===")
            android.util.Log.d("TitleCleanup", "Final cleanedTitle: $cleanedTitle")
            android.util.Log.d("TitleCleanup", "Final series: $series")
            android.util.Log.d("TitleCleanup", "Final seriesNumber: $seriesNumber")
        }
        
        // Check if book is finished in Audiobookshelf
        // Try userMediaProgress first, then fall back to progressMap
        val isFinished = item.userMediaProgress?.isFinished 
            ?: progressMap[item.id] 
            ?: false

        // Find existing book by audiobookshelfId or by title+author
        val existingBook = existingBooks.firstOrNull { it.audiobookshelfId == item.id }
            ?: existingBooks.firstOrNull { 
                normalizeForComparison(it.title) == normalizeForComparison(cleanedTitle) &&
                normalizeForComparison(it.author) == normalizeForComparison(author)
            }

        // Determine isOwned and isRead flags
        // Rule: Never downgrade isRead (user decision takes precedence)
        val isOwned = true // Always owned if in Audiobookshelf
        val isRead = if (existingBook != null && existingBook.isRead) {
            // Keep READ status - never downgrade even if ABS says not finished
            true
        } else {
            // Use ABS finished status
            isFinished
        }

        // Normalize title and author for consistent Unicode representation
        val normalizedTitle = java.text.Normalizer.normalize(cleanedTitle, java.text.Normalizer.Form.NFC)
        val normalizedAuthor = java.text.Normalizer.normalize(author, java.text.Normalizer.Form.NFC)
        val normalizedOriginalTitle = java.text.Normalizer.normalize(originalTitle, java.text.Normalizer.Form.NFC)
        val normalizedOriginalAuthor = java.text.Normalizer.normalize(originalAuthor, java.text.Normalizer.Form.NFC)
        
        // Calculate titleSort using titleIgnorePrefix from Audiobookshelf if available
        // Otherwise use the cleaned title
        val titleForSorting = metadata.titleIgnorePrefix ?: cleanedTitle
        val titleSort = com.readle.app.util.TextNormalizer.normalizeTitleForSorting(
            titleForSorting,
            null // No language info from Audiobookshelf, will use English as fallback
        )

        return if (existingBook != null) {
            // Update existing book with Audiobookshelf data (authoritative!)
            existingBook.copy(
                title = normalizedTitle,  // Audiobookshelf is authoritative (with cleaned title)
                author = normalizedAuthor,  // Audiobookshelf is authoritative
                isbn = metadata.isbn,  // Audiobookshelf is authoritative
                originalTitle = normalizedOriginalTitle,  // Store original for bidirectional search
                originalAuthor = normalizedOriginalAuthor,  // Store original for bidirectional search
                description = description,  // Audiobookshelf is authoritative
                series = series,  // Audiobookshelf is authoritative
                seriesNumber = seriesNumber,  // Audiobookshelf is authoritative
                isOwned = isOwned,
                isRead = isRead,
                audiobookshelfId = item.id,
                isEBook = true,
                // Keep existing dateAdded - never change after initial import
                dateAdded = existingBook.dateAdded,
                dateFinished = if (isRead && existingBook.dateFinished == null)
                    System.currentTimeMillis() else existingBook.dateFinished,
                dateStarted = if (isOwned && existingBook.dateStarted == null)
                    System.currentTimeMillis() else existingBook.dateStarted,
                titleSort = titleSort
            )
        } else {
            // Create new book
            BookEntity(
                title = normalizedTitle,
                author = normalizedAuthor,
                isbn = metadata.isbn,
                originalTitle = normalizedOriginalTitle,  // Store original for bidirectional search
                originalAuthor = normalizedOriginalAuthor,  // Store original for bidirectional search
                description = description,
                series = series,
                seriesNumber = seriesNumber,
                isOwned = isOwned,
                isRead = isRead,
                audiobookshelfId = item.id,
                isEBook = true,
                dateAdded = dateAdded,
                dateStarted = if (isOwned) System.currentTimeMillis() else null,
                dateFinished = if (isRead) System.currentTimeMillis() else null,
                titleSort = titleSort
            )
        }
    }

    /**
     * Determines the best dateAdded timestamp from available sources.
     * Priority:
     * 1. mtimeMs from ebook file metadata (most reliable - actual file modification time)
     * 2. addedAt from library item (when added to Audiobookshelf)
     * 3. Current time as fallback
     */
    private fun determineBestDateAdded(item: AudiobookshelfLibraryItem): Long {
        // Try to get mtimeMs from ebook file first (most reliable)
        val ebookMtime = item.media.ebookFile?.metadata?.mtimeMs
        
        android.util.Log.d("DateDebug", "Book: ${item.media.metadata.title}")
        android.util.Log.d("DateDebug", "  ebookFile.metadata.mtimeMs: $ebookMtime")
        android.util.Log.d("DateDebug", "  item.addedAt: ${item.addedAt}")
        android.util.Log.d("DateDebug", "  item.mtimeMs: ${item.mtimeMs}")
        
        if (ebookMtime != null && ebookMtime > 0) {
            android.util.Log.d("DateDebug", "  -> Using ebookMtime: $ebookMtime")
            return ebookMtime
        }
        
        // Fallback to addedAt from library item
        val addedAt = item.addedAt
        if (addedAt != null && addedAt > 0) {
            android.util.Log.d("DateDebug", "  -> Using addedAt: $addedAt")
            return addedAt
        }
        
        // Last resort: current time
        val currentTime = System.currentTimeMillis()
        android.util.Log.d("DateDebug", "  -> Using current time: $currentTime")
        return currentTime
    }

    /**
     * Converts author separator from ", " to "; " for multiple authors.
     * Handles two formats from Audiobookshelf:
     * 
     * Format 1 (authorName): "FirstName LastName, FirstName LastName"
     * - Example: "Ina Linger, Doska Palifin" -> "Ina Linger; Doska Palifin"
     * - Logic: Replace all ", " with "; "
     * 
     * Format 2 (authorNameLF): "LastName, FirstName, LastName, FirstName"
     * - Example: "Linger, Ina, Palifin, Doska" -> "Linger, Ina; Palifin, Doska"
     * - Logic: Replace every 2nd, 4th, 6th... comma with ";"
     * 
     * Detection: Odd number of commas = Format 2, Even/1 comma = Format 1
     */
    private fun convertAuthorSeparator(authorString: String, isLastNameFirst: Boolean): String {
        val commaCount = authorString.count { it == ',' }
        
        // No commas or single author
        if (commaCount == 0) {
            return authorString
        }
        
        // authorName format: "FirstName LastName, FirstName LastName"
        // Just replace all commas with semicolons
        if (!isLastNameFirst) {
            return authorString.replace(", ", "; ")
        }
        
        // authorNameLF format: "LastName, FirstName, LastName, FirstName"
        // Only replace every even comma (2nd, 4th, etc.) with semicolon
        if (commaCount == 1) {
            // Single author with "LastName, FirstName"
            return authorString
        }
        
        // Multiple authors in LastName First format
        var result = authorString
        var commaIndex = 0
        var searchStart = 0
        
        while (searchStart < result.length) {
            val nextCommaPos = result.indexOf(',', searchStart)
            if (nextCommaPos == -1) break
            
            commaIndex++
            // Replace every even comma (2nd, 4th, 6th...) with semicolon
            if (commaIndex % 2 == 0) {
                result = result.substring(0, nextCommaPos) + ";" + result.substring(nextCommaPos + 1)
            }
            
            searchStart = nextCommaPos + 1
        }
        
        return result
    }

    private fun normalizeForComparison(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .take(50)
    }
}

