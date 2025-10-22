package com.readle.app.data.api.dnb

import com.readle.app.data.api.model.BookDataSource
import com.readle.app.data.api.model.BookSearchResult
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

class DnbApiClient @Inject constructor(
    private val apiService: DnbApiService
) {

    suspend fun searchByIsbn(isbn: String): Result<List<BookSearchResult>> {
        return try {
            val cleanIsbn = isbn.replace("-", "").replace(" ", "")
            val query = "isbn=$cleanIsbn and (mat=books or mat=book or mat=Bücher)"
            val response = apiService.searchByIsbn(query = query)

            if (response.isSuccessful && response.body() != null) {
                val books = parseMarcXml(response.body()!!)
                Result.success(books)
            } else {
                Result.failure(Exception("DNB API error: ${response.code()}"))
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
                queryParts.add("tit=\"${title.trim()}\"")
            }
            if (!author.isNullOrBlank()) {
                queryParts.add("per=\"${author.trim()}\"")
            }
            // DNB doesn't support 'ser' index, so search series in title instead
            if (!series.isNullOrBlank() && title.isNullOrBlank()) {
                queryParts.add("tit=\"${series.trim()}\"")
            }

            if (queryParts.isEmpty()) {
                return Result.success(emptyList())
            }

            queryParts.add("(mat=books or mat=book or mat=Bücher)")
            val query = queryParts.joinToString(" and ")
            
            // Use higher limit for series searches to get all books in the series
            val maxRecords = if (!series.isNullOrBlank()) 999 else 50
            val response = apiService.searchByTitleAuthor(query = query, maximumRecords = maxRecords)

            if (response.isSuccessful && response.body() != null) {
                val books = parseMarcXml(response.body()!!)
                Result.success(books)
            } else {
                Result.failure(Exception("DNB API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun parseMarcXml(xmlString: String): List<BookSearchResult> {
        val allRecords = mutableListOf<BookSearchResult>()

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val inputSource = InputSource(StringReader(xmlString))
            val doc = builder.parse(inputSource)

            val recordNodes = doc.getElementsByTagNameNS("*", "record")

            for (i in 0 until recordNodes.length) {
                val record = recordNodes.item(i) as? Element ?: continue
                val book = parseRecord(record, filterOnlineResources = false)
                if (book != null) {
                    allRecords.add(book)
                } else {
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Merge records with same ISBN, prioritizing physical books but keeping all data
        val merged = mergeRecordsByIsbn(allRecords)
        return merged
    }

    private fun mergeRecordsByIsbn(records: List<BookSearchResult>): List<BookSearchResult> {
        // Group by ISBN (or title+author if no ISBN)
        val groupedByKey = records.groupBy { book ->
            book.isbn?.trim() ?: "${book.title}|${book.author}"
        }

        return groupedByKey.map { (key, booksWithSameIsbn) ->
            if (booksWithSameIsbn.size == 1) {
                booksWithSameIsbn.first()
            } else {
                // Multiple records for same ISBN - merge them
                mergeBookRecords(booksWithSameIsbn)
            }
        }
    }

    private fun mergeBookRecords(books: List<BookSearchResult>): BookSearchResult {
        books.forEachIndexed { index, book ->
        }
        
        // Sort: prefer shorter titles (physical books) over longer titles (online resources with full series title)
        // "Im Tal des Todes" (physical) vs "Tee? Kaffee? Mord! Im Tal des Todes" (online)
        val sorted = books.sortedBy { it.title.length }
        
        // Find the best series info: prefer series WITH number over series without number
        val seriesWithNumber = books.firstOrNull { it.seriesNumber != null }
        val seriesWithoutNumber = books.firstOrNull { !it.series.isNullOrBlank() && it.seriesNumber == null }
        
        val bestSeries = seriesWithNumber?.series ?: seriesWithoutNumber?.series
        val bestSeriesNumber = seriesWithNumber?.seriesNumber
        
        // Collect ALL ISBNs from all records
        val allIsbns = books.flatMap { it.allIsbns }.distinct().filter { it.isNotBlank() }
        val primaryIsbn = allIsbns.firstOrNull()
        
        
        // Collect the best value for each field from all records
        val merged = BookSearchResult(
            // Use shortest title (usually from physical book)
            title = sorted.first().title,
            // Author should be the same in all records
            author = books.first().author,
            // Take first non-blank description (URLs count as non-blank)
            description = books.firstOrNull { !it.description.isNullOrBlank() }?.description,
            // Take first non-blank publisher
            publisher = books.firstOrNull { !it.publisher.isNullOrBlank() }?.publisher,
            // Take first non-blank publish date
            publishDate = books.firstOrNull { !it.publishDate.isNullOrBlank() }?.publishDate,
            // Take first non-blank language
            language = books.firstOrNull { !it.language.isNullOrBlank() }?.language,
            // Take first non-null original language
            originalLanguage = books.firstOrNull { it.originalLanguage != null }?.originalLanguage,
            // Use best series (prefer series with number)
            series = bestSeries,
            seriesNumber = bestSeriesNumber,
            // Take first non-blank ISBN as primary
            isbn = primaryIsbn,
            // Store ALL ISBNs
            allIsbns = allIsbns,
            // Take first non-blank cover URL
            coverUrl = books.firstOrNull { !it.coverUrl.isNullOrBlank() }?.coverUrl,
            // Use source from first record
            source = books.first().source
        )
        
        
        return merged
    }

    private suspend fun parseRecord(record: Element, filterOnlineResources: Boolean = true): BookSearchResult? {
        var title: String? = null
        val authors = mutableListOf<String>()
        var publisher: String? = null
        var publishDate: String? = null
        var isbn: String? = null
        var language: String? = null
        var description: String? = null
        var materialType: String? = null
        var coverUrl: String? = null
        var series: String? = null
        var seriesNumber: String? = null
        var carrierType: String? = null
        var linkedOnlineResourceId: String? = null
        
        // Collect all series fields for later processing
        data class SeriesInfo(val name: String?, val number: String?)
        var field800: SeriesInfo? = null  // Author series (highest priority)
        var field830: SeriesInfo? = null  // Uniform title series
        var field490Ind0: SeriesInfo? = null  // Standalone series statement
        
        // Debug: Check controlfield 007 to identify online resources
        val controlfields = record.getElementsByTagNameNS("*", "controlfield")
        var isOnlineResource = false
        for (i in 0 until controlfields.length) {
            val controlfield = controlfields.item(i) as? Element ?: continue
            val tag = controlfield.getAttribute("tag")
            if (tag == "007") {
                val value = controlfield.textContent?.trim() ?: ""
                if (value.startsWith("cr")) {
                    isOnlineResource = true
                }
            }
        }

        val datafields = record.getElementsByTagNameNS("*", "datafield")

        for (i in 0 until datafields.length) {
            val datafield = datafields.item(i) as? Element ?: continue
            val tag = datafield.getAttribute("tag")

            when (tag) {
                "245" -> title = extractSubfield(datafield, "a")
                "100", "700" -> {
                    // Collect authors (filter out translators, illustrators, etc.)
                    // $4 contains the relator code: "aut" = author, "trl" = translator, "ill" = illustrator
                    val relatorCode = extractSubfield(datafield, "4")
                    val authorName = extractSubfield(datafield, "a")
                    
                    // Include if:
                    // - Field 100 (main author, always include)
                    // - Field 700 with $4="aut" (additional author)
                    // - Field 700 without $4 (assume author if role not specified)
                    val isAuthor = tag == "100" || 
                                   relatorCode == null || 
                                   relatorCode == "aut"
                    
                    if (authorName != null && isAuthor) {
                        authors.add(authorName)
                    }
                }
                "260", "264" -> {
                    if (publisher == null) {
                        publisher = extractSubfield(datafield, "b")
                    }
                    if (publishDate == null) {
                        publishDate = cleanPublishDate(extractSubfield(datafield, "c"))
                    }
                }
                "020" -> {
                    if (isbn == null) {
                        isbn = extractSubfield(datafield, "a")
                    }
                }
                "041" -> {
                    if (language == null) {
                        language = extractSubfield(datafield, "a")
                    }
                }
                "520" -> {
                    // Summary/description from MARC21 field 520
                    // This is preferred over the URL-based description
                    val summaryText = extractSubfield(datafield, "a")
                    if (!summaryText.isNullOrBlank() && summaryText.length > 20) {
                        description = summaryText
                    }
                }
                "338" -> {
                    // Carrier type (e.g., "Online-Ressource", "Band")
                    carrierType = extractSubfield(datafield, "a")
                }
                "655" -> {
                    materialType = extractSubfield(datafield, "a")
                }
                "490" -> {
                    // 490 = Series Statement
                    // Only use if ind1="0" (no corresponding 800/810/830)
                    val ind1 = datafield.getAttribute("ind1")
                    if (ind1 == "0") {
                        val seriesName = extractSubfield(datafield, "a")
                        val volumeStr = extractSubfield(datafield, "v")
                        if (seriesName != null) {
                            val number = volumeStr?.let { 
                                Regex("\\d+(?:\\.\\d+)?").find(it)?.value 
                            }
                            field490Ind0 = SeriesInfo(seriesName, number)
                        }
                    }
                    // ind1="1" means there's a corresponding 800/830 field, so ignore this 490
                }
                "800" -> {
                    // 800 = Series Added Entry - Personal Name (author series)
                    // This is the highest priority for real book series by an author
                    val seriesName = extractSubfield(datafield, "t")  // $t = title of the work/series
                    val volumeStr = extractSubfield(datafield, "v")
                    if (seriesName != null) {
                        val number = volumeStr?.let { 
                            Regex("\\d+(?:\\.\\d+)?").find(it)?.value 
                        }
                        field800 = SeriesInfo(seriesName, number)
                    }
                }
                "830" -> {
                    // 830 = Series Added Entry - Uniform Title
                    // Check $7 control subfield to filter out publisher series
                    val controlSubfield = extractSubfield(datafield, "7")
                    
                    // $7 position /1 indicates bibliographic level:
                    // "am" = language material, monograph → Real book series
                    // "as" = language material, serial → Publisher series (ignore!)
                    val isPublisherSeries = controlSubfield?.endsWith("s") == true
                    
                    if (!isPublisherSeries) {
                        val seriesName = extractSubfield(datafield, "a")
                        val volumeStr = extractSubfield(datafield, "v")
                        if (seriesName != null) {
                            val number = volumeStr?.let { 
                                Regex("\\d+(?:\\.\\d+)?").find(it)?.value 
                            }
                            field830 = SeriesInfo(seriesName, number)
                        }
                    }
                }
                "776" -> {
                    // Linking Entry (related editions like online versions)
                    // Extract DNB-ID of linked online resource
                    val relationType = extractSubfield(datafield, "n")
                    if (relationType?.contains("Online", ignoreCase = true) == true) {
                        val idField = extractSubfield(datafield, "w")
                        if (idField != null) {
                            // Format: "(DE-101)1352908603" -> extract "1352908603"
                            val idMatch = Regex("\\(DE-101\\)(\\d+)").find(idField)
                            if (idMatch != null) {
                                linkedOnlineResourceId = idMatch.groupValues[1]
                            }
                        }
                    }
                }
                "856" -> {
                    val subfield3 = extractSubfield(datafield, "3")
                    val url = extractSubfield(datafield, "u")
                    
                    
                    // Check if this is a content description (Inhaltstext)
                    val isContentDescription = subfield3?.lowercase()?.contains("inhaltstext") == true ||
                        subfield3?.lowercase()?.contains("inhaltsangabe") == true
                    
                    
                    if (isContentDescription) {
                        val hasDescription = description != null && description!!.length >= 50
                        
                        // Only use URL if we don't already have a good description from field 520
                        if (!hasDescription && url != null) {
                            // Fetch the description from the URL
                            try {
                                val fetchedDescription = fetchDescriptionFromUrl(url)
                                if (fetchedDescription != null && fetchedDescription.length > 50) {
                                    description = fetchedDescription
                                } else {
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("DnbApiClient", "Failed to fetch description from URL: ${e.message}", e)
                            }
                        } else {
                        }
                    }
                    // Cover image URL
                    else if (coverUrl == null) {
                        
                        // Only use URLs that look like images
                        if (url != null && (url.contains("cover", ignoreCase = true) ||
                            url.endsWith(".jpg", ignoreCase = true) ||
                            url.endsWith(".jpeg", ignoreCase = true) ||
                            url.endsWith(".png", ignoreCase = true))) {
                            coverUrl = url
                        } else {
                        }
                    }
                }
            }
        }
        
        // Determine final series based on priority: 800 > 830 (without "as") > 490 (ind1="0")
        val selectedSeries = when {
            field800 != null -> field800  // Highest priority: Author series
            field830 != null -> field830  // Second: Uniform title series (already filtered for "as")
            field490Ind0 != null -> field490Ind0  // Fallback: Standalone series statement
            else -> null
        }
        
        if (selectedSeries != null) {
            series = selectedSeries.name
            seriesNumber = selectedSeries.number
        }

        if (title.isNullOrBlank()) {
            return null
        }

        // Filter audiobooks (always filter these out)
        val materialTypeLower = materialType?.lowercase() ?: ""
        if (materialTypeLower.contains("hörbuch") ||
            materialTypeLower.contains("tonträger") ||
            materialTypeLower.contains("audiobook") ||
            materialTypeLower.contains("cd") ||
            materialTypeLower.contains("audio")) {
            return null
        }

        // Filter non-German books
        val languageLower = language?.lowercase()?.trim() ?: ""
        val isGerman = languageLower.isEmpty() || // Accept if language is unknown
                       languageLower == "ger" ||
                       languageLower == "de" ||
                       languageLower == "deu" ||
                       languageLower == "german" ||
                       languageLower.startsWith("ger") ||
                       languageLower.startsWith("de")
        
        if (!isGerman) {
            return null
        }

        // If no cover URL was found in MARC21 856 field, but we have an ISBN,
        // construct the DNB cover URL
        val finalCoverUrl = if (coverUrl.isNullOrBlank() && !isbn.isNullOrBlank()) {
            val cleanIsbn = isbn.trim().replace("-", "")
            "https://portal.dnb.de/opac/mvb/cover?isbn=$cleanIsbn"
        } else {
            coverUrl?.trim()
        }

        // If series is set but series number is missing, try to fetch from linked online resource
        var finalSeries = series?.trim()
        var finalSeriesNumber = seriesNumber
        if (!finalSeries.isNullOrBlank() && finalSeriesNumber == null && linkedOnlineResourceId != null) {
            val onlineSeriesInfo = fetchSeriesInfoFromOnlineResource(linkedOnlineResourceId)
            if (onlineSeriesInfo != null) {
                finalSeries = onlineSeriesInfo.first ?: finalSeries
                finalSeriesNumber = onlineSeriesInfo.second
            }
        }

        // Convert HTTP to HTTPS for deposit.dnb.de to avoid cleartext traffic issues
        // We'll fetch the description later when the book is added to the library
        val finalDescription = if (description?.startsWith("http://deposit.dnb.de") == true) {
            description.replace("http://deposit.dnb.de", "https://deposit.dnb.de")
        } else {
            description?.trim()
        }

        // Join all authors with "; " (remove duplicates)
        val authorString = if (authors.isNotEmpty()) {
            authors.distinct().joinToString("; ")
        } else {
            "Unknown Author"
        }
        
        // Normalize title and author for consistent Unicode representation
        val normalizedTitle = java.text.Normalizer.normalize(title.trim(), java.text.Normalizer.Form.NFC)
        val normalizedAuthor = java.text.Normalizer.normalize(authorString, java.text.Normalizer.Form.NFC)
        
        val result = BookSearchResult(
            title = normalizedTitle,
            author = normalizedAuthor,
            description = finalDescription,
            publisher = publisher?.trim(),
            publishDate = publishDate?.trim(),
            language = language?.trim(),
            originalLanguage = null,
            series = finalSeries,
            seriesNumber = finalSeriesNumber,
            isbn = isbn?.trim(),
            coverUrl = finalCoverUrl,
            source = BookDataSource.DNB
        )
        
        val resourceType = if (isOnlineResource) "ONLINE" else "PHYSICAL"
        
        return result
    }

    private fun extractSubfield(datafield: Element, code: String): String? {
        val subfields = datafield.getElementsByTagNameNS("*", "subfield")
        for (i in 0 until subfields.length) {
            val subfield = subfields.item(i) as? Element ?: continue
            if (subfield.getAttribute("code") == code) {
                val rawText = subfield.textContent
                val cleanedText = cleanMarcText(rawText)
                
                // Debug logging
                if (rawText != cleanedText) {
                }
                
                return cleanedText
            }
        }
        return null
    }

    private fun cleanMarcText(text: String?): String? {
        if (text == null) return null

        var cleaned = text
            // MARC21 Non-Sorting Character Markers (used to mark articles like "Die", "The", etc.)
            .replace("\u0098", "") // Start of String (SOS) - marks start of non-sorting text
            .replace("\u009C", "") // String Terminator (ST) - marks end of non-sorting text
            .replace("\u0088", "") // Character Tabulation Set
            .replace("\u0089", "") // Character Tabulation with Justification
            // MARC21 Delimiters (verschiedene Unicode-Varianten)
            .replace("‡", "")  // U+2021 Double Dagger
            .replace("†", "")  // U+2020 Dagger
            .replace("\u2021", "") // Double Dagger
            .replace("\u2020", "") // Dagger
            .replace("\u001F", "") // Unit Separator
            .replace("\u001E", "") // Record Separator
            .replace("\u001D", "") // Group Separator
            .replace("\u001C", "") // File Separator
            .replace("$", "")
            .replace("@", "")
            .replace("|", "")
            // Multiple spaces to single space
            .replace(Regex("\\s+"), " ")
            .trim()

        // Remove leading/trailing special characters
        cleaned = cleaned.trim { it in "‡†|@\$" }

        return cleaned.takeIf { it.isNotEmpty() }
    }

    private fun cleanPublishDate(date: String?): String? {
        if (date == null) return null

        var cleaned = date
            // Remove MARC21 date conventions
            .replace("[", "")      // Estimated date: [2025]
            .replace("]", "")
            .replace("c", "")      // Copyright: c2025
            .replace("©", "")      // Copyright symbol
            .replace("ca.", "")    // Circa: ca. 2025
            .replace("ca", "")
            .replace("p", "")      // Phonogram: p2025
            .replace("℗", "")      // Phonogram symbol
            .trim()
            .removeSuffix(".")     // Trailing period: 2025.
            .trim()

        // Extract first 4-digit year found (handles ranges like "2020-2025")
        val yearMatch = Regex("\\d{4}").find(cleaned)
        if (yearMatch != null) {
            return yearMatch.value
        }

        return cleaned.takeIf { it.isNotEmpty() }
    }

    private suspend fun fetchSeriesInfoFromOnlineResource(dnbId: String): Pair<String?, String?>? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                
                val response = apiService.searchByIsbn(query = "num=$dnbId", maximumRecords = 1)
                
                if (response.isSuccessful && response.body() != null) {
                    val xmlString = response.body()!!
                    
                    // Parse the XML to extract series info
                    val factory = DocumentBuilderFactory.newInstance()
                    factory.isNamespaceAware = true
                    val builder = factory.newDocumentBuilder()
                    val inputSource = InputSource(StringReader(xmlString))
                    val doc = builder.parse(inputSource)
                    
                    val datafields = doc.getElementsByTagNameNS("*", "datafield")
                    var field800Series: Pair<String?, String?>? = null
                    var field830Series: Pair<String?, String?>? = null
                    var field490Series: Pair<String?, String?>? = null
                    
                    for (i in 0 until datafields.length) {
                        val datafield = datafields.item(i) as? Element ?: continue
                        val tag = datafield.getAttribute("tag")
                        
                        when (tag) {
                            "800" -> {
                                val seriesName = extractSubfield(datafield, "t")
                                val volumeStr = extractSubfield(datafield, "v")
                                val number = volumeStr?.let { Regex("\\d+(?:\\.\\d+)?").find(it)?.value }
                                if (seriesName != null) {
                                    field800Series = Pair(seriesName, number)
                                }
                            }
                            "830" -> {
                                val controlSubfield = extractSubfield(datafield, "7")
                                val isPublisherSeries = controlSubfield?.endsWith("s") == true
                                if (!isPublisherSeries) {
                                    val seriesName = extractSubfield(datafield, "a")
                                    val volumeStr = extractSubfield(datafield, "v")
                                    val number = volumeStr?.let { Regex("\\d+(?:\\.\\d+)?").find(it)?.value }
                                    if (seriesName != null) {
                                        field830Series = Pair(seriesName, number)
                                    }
                                }
                            }
                            "490" -> {
                                val ind1 = datafield.getAttribute("ind1")
                                if (ind1 == "0") {
                                    val seriesName = extractSubfield(datafield, "a")
                                    val volumeStr = extractSubfield(datafield, "v")
                                    val number = volumeStr?.let { Regex("\\d+(?:\\.\\d+)?").find(it)?.value }
                                    if (seriesName != null) {
                                        field490Series = Pair(seriesName, number)
                                    }
                                }
                            }
                        }
                    }
                    
                    // Priority: 800 > 830 > 490 (ind1="0")
                    field800Series ?: field830Series ?: field490Series
                } else {
                    android.util.Log.w("DnbApiClient", "Failed to fetch online resource, HTTP ${response.code()}")
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("DnbApiClient", "Error fetching series info: ${e.message}", e)
                null
            }
        }
    }

    private suspend fun fetchDescriptionFromUrl(url: String): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Convert HTTP to HTTPS (Android blocks cleartext HTTP)
                val secureUrl = if (url.startsWith("http://")) {
                    url.replace("http://", "https://")
                } else {
                    url
                }
                
                val connection = java.net.URL(secureUrl).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val html = connection.inputStream.bufferedReader().use { it.readText() }
                    val description = parseDescriptionFromHtml(html)
                    description
                } else {
                    android.util.Log.w("DnbApiClient", "Failed to fetch description, HTTP $responseCode")
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("DnbApiClient", "Error fetching description: ${e.message}", e)
                null
            }
        }
    }

    private fun parseDescriptionFromHtml(html: String): String? {
        return try {
            // Extract HTML content from <div style="font-size:14px; font-family: Georgia"><p>...</p></div>
            // or from <body><div...><p>...</p></div></body>
            // We keep the HTML formatting for rich text display
            val patterns = listOf(
                Regex("<body[^>]*>(.*?)</body>", RegexOption.DOT_MATCHES_ALL),
                Regex("<div[^>]*>(.*?)</div>", RegexOption.DOT_MATCHES_ALL),
                Regex("<p>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)
            )
            
            for (pattern in patterns) {
                val match = pattern.find(html)
                if (match != null) {
                    var htmlContent = match.groupValues[1].trim()
                    
                    // Basic HTML sanitizing: remove script tags and style tags for security
                    htmlContent = htmlContent
                        .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
                        .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
                    
                    // Check if there's actual content (not just tags)
                    val textOnly = htmlContent.replace(Regex("<[^>]+>"), "").trim()
                    if (textOnly.length > 20) {
                        return htmlContent
                    }
                }
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("DnbApiClient", "Error parsing HTML: ${e.message}")
            null
        }
    }
}

