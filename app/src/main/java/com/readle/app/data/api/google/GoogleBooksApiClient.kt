package com.readle.app.data.api.google

import com.readle.app.data.api.model.BookDataSource
import com.readle.app.data.api.model.BookSearchResult
import javax.inject.Inject

class GoogleBooksApiClient @Inject constructor(
    private val apiService: GoogleBooksApiService
) {

    suspend fun searchByIsbn(isbn: String): Result<List<BookSearchResult>> {
        return try {
            val cleanIsbn = isbn.replace("-", "").replace(" ", "")
            val query = "isbn:$cleanIsbn"
            val response = apiService.searchBooks(query = query)

            if (response.isSuccessful && response.body() != null) {
                val books = response.body()!!.items
                    ?.mapNotNull { it.toBookSearchResult() }
                    ?.filter { !isAudiobook(it) }
                    ?: emptyList()
                Result.success(books)
            } else {
                Result.failure(Exception("Google Books API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchByTitleAuthor(
        title: String? = null,
        author: String? = null,
        series: String? = null
    ): Result<List<BookSearchResult>> {
        return try {
            val queryParts = mutableListOf<String>()
            if (!title.isNullOrBlank()) {
                queryParts.add("intitle:${title.trim()}")
            }
            if (!author.isNullOrBlank()) {
                queryParts.add("inauthor:${author.trim()}")
            }
            if (!series.isNullOrBlank()) {
                // Google Books doesn't have a specific series field, search in title
                queryParts.add("intitle:${series.trim()}")
            }

            if (queryParts.isEmpty()) {
                return Result.success(emptyList())
            }

            val query = queryParts.joinToString("+")
            // Use higher limit for series searches to get all books in the series
            val maxResults = if (!series.isNullOrBlank()) 40 else 10
            
            
            val response = apiService.searchBooks(query = query, maxResults = maxResults, langRestrict = "de")

            if (response.isSuccessful && response.body() != null) {
                val books = response.body()!!.items
                    ?.mapNotNull { it.toBookSearchResult() }
                    ?.filter { !isAudiobook(it) }
                    ?: emptyList()
                Result.success(books)
            } else {
                Result.failure(Exception("Google Books API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun GoogleBookItem.toBookSearchResult(): BookSearchResult? {
        // Filter out audiobooks based on categories
        val categories = volumeInfo.categories?.map { it.lowercase() } ?: emptyList()
        val isAudioBook = categories.any { category ->
            category.contains("audio") ||
            category.contains("audiobook") ||
            category.contains("hörbuch") ||
            category.contains("sound recording")
        }
        
        if (isAudioBook) {
            return null
        }

        val isbn13 = volumeInfo.industryIdentifiers
            ?.firstOrNull { it.type == "ISBN_13" }?.identifier
        val isbn10 = volumeInfo.industryIdentifiers
            ?.firstOrNull { it.type == "ISBN_10" }?.identifier

        // Try to extract series info from API's seriesInfo field first
        var series: String? = null
        var seriesNumber: String? = null
        
        
        volumeInfo.seriesInfo?.volumeSeries?.firstOrNull()?.let { volumeSeries ->
            series = volumeSeries.seriesId
            seriesNumber = volumeSeries.orderNumber?.toString()
        }
        
        // If no series info from API, try to extract from title
        if (series == null) {
            val title = volumeInfo.title ?: "Unknown Title"
            
            // Pattern 1: "Title (Series #3)" or "Title (Series, Band 3)"
            val pattern1 = Regex("""(.+?)\s*\((.+?)[,\s]+(?:Book|Band|#)\s*(\d+)\)""", RegexOption.IGNORE_CASE)
            pattern1.find(title)?.let { match ->
                series = match.groupValues[2].trim()
                seriesNumber = match.groupValues[3]
            }
            
            // Pattern 2: "Series: Title" or "Series - Title"
            if (series == null) {
                val pattern2 = Regex("""^(.+?)[\s]*[:\-][\s]*(.+)$""")
                pattern2.find(title)?.let { match ->
                    val part1 = match.groupValues[1].trim()
                    // Check if part1 looks like a series (contains common keywords)
                    if (part1.contains("Reihe", ignoreCase = true) || 
                        part1.contains("Serie", ignoreCase = true) ||
                        part1.matches(Regex(""".*\s+\d+(?:\.\d+)?$"""))) {
                        series = part1.replace(Regex("""\s+\d+(?:\.\d+)?$"""), "").trim()
                        seriesNumber = Regex("""\d+(?:\.\d+)?""").find(part1)?.value
                    }
                }
            }
        }

        // Normalize title and author for consistent Unicode representation
        val normalizedTitle = java.text.Normalizer.normalize(
            volumeInfo.title ?: "Unknown Title", 
            java.text.Normalizer.Form.NFC
        )
        val normalizedAuthor = java.text.Normalizer.normalize(
            volumeInfo.authors?.joinToString("; ") ?: "Unknown Author",
            java.text.Normalizer.Form.NFC
        )
        
        val result = BookSearchResult(
            title = normalizedTitle,
            author = normalizedAuthor,
            description = volumeInfo.description,
            publisher = volumeInfo.publisher,
            publishDate = volumeInfo.publishedDate,
            language = volumeInfo.language,
            originalLanguage = null,
            series = series,
            seriesNumber = seriesNumber,
            isbn = isbn13 ?: isbn10,
            coverUrl = volumeInfo.imageLinks?.thumbnail?.replace("http://", "https://"),
            source = BookDataSource.GOOGLE_BOOKS
        )
        
        
        return result
    }

    private fun isAudiobook(book: BookSearchResult): Boolean {
        val titleLower = book.title.lowercase()
        val descriptionLower = book.description?.lowercase() ?: ""
        
        return titleLower.contains("audiobook") ||
               titleLower.contains("hörbuch") ||
               titleLower.contains("audio cd") ||
               titleLower.contains("audio-cd") ||
               titleLower.contains("ungekürzt") ||
               descriptionLower.contains("audiobook") ||
               descriptionLower.contains("hörbuch") ||
               descriptionLower.contains("gelesen von") ||
               descriptionLower.contains("narrated by")
    }
}

