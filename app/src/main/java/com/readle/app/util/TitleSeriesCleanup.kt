package com.readle.app.util

/**
 * Utility class for cleaning up book titles by removing redundant series names and numbers.
 * This is used during Audiobookshelf import to extract the actual book title from
 * full titles like "Die Kristallwächter-Saga #7 - Der Fluss des Vergessens".
 *
 * Extracted from AudiobookshelfApiClient for testability.
 */
object TitleSeriesCleanup {

    /**
     * Converts a series name to a regex pattern that matches both spaces and hyphens.
     * This handles cases where the series name uses spaces but the title uses hyphens
     * (e.g., series "Zimt Trilogie" should match "Zimt-Trilogie" in the title).
     * Also handles the reverse: series "Zimt-Trilogie" should match "Zimt Trilogie" in the title.
     *
     * @param seriesName The series name with spaces or hyphens
     * @return A regex pattern string where spaces and hyphens are replaced with [\s\-] pattern
     */
    private fun seriesNameToFlexiblePattern(seriesName: String): String {
        // Split by both spaces and hyphens, escape each part individually,
        // then join with [\s\-] pattern that matches both space and hyphen
        return seriesName.split(Regex("""[\s\-]+"""))
            .filter { it.isNotEmpty() }
            .joinToString("""[\s\-]+""") { part ->
                Regex.escape(part)
            }
    }

    /**
     * Cleans up a book title by removing redundant series name and number prefix.
     *
     * @param rawTitle The original title from Audiobookshelf (before normalization)
     * @param series The series name (e.g., "Die Kristallwächter-Saga", "Foundation")
     * @param seriesNumber The series number (e.g., "1", "7.5")
     * @return The cleaned title, normalized with TextNormalizer
     *
     * Examples:
     * - cleanupTitle("Alea Aquarius #7 - Der Fluss des Vergessens", "Alea Aquarius", "7")
     *   -> "Fluss des Vergessens, Der"
     *
     * - cleanupTitle("Der Donnerstagsmordclub 5: Die letzte Teufelsnummer", "Der Donnerstagsmordclub", "5")
     *   -> "Letzte Teufelsnummer, Die"
     *
     * - cleanupTitle("Foundation 1", "Foundation", "1")
     *   -> "Foundation"
     *
     * - cleanupTitle("Die Seiten der Welt 1-3", "Die Seiten der Welt", "1")
     *   -> "Seiten der Welt 1-3, Die" (omnibus kept intact)
     *
     * - cleanupTitle("Zimt-Trilogie #1 - Foo Bar", "Zimt Trilogie", "1")
     *   -> "Foo Bar" (handles hyphen instead of space in series name)
     */
    fun cleanupTitle(rawTitle: String, series: String?, seriesNumber: String?): String {
        // If no series or series number, just normalize the title
        if (series == null || seriesNumber == null) {
            return TextNormalizer.normalizeTitle(rawTitle)
        }

        val title = TextNormalizer.normalizeTitle(rawTitle)
        var cleanedTitle = title

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
            var normalizedSeriesWithoutArticle = TextNormalizer.normalizeTitle(seriesWithoutArticle)
            val flexibleSeriesPattern = seriesNameToFlexiblePattern(normalizedSeriesWithoutArticle)
            
            // IMPORTANT: Check for omnibus/collection FIRST before trying to extract subtitle
            // This prevents "Series 1-3" from being split into "Series" + "3" as subtitle
            var match: MatchResult? = null
            var isOmnibus = false
            
            if (seriesNumber == "1") {
                // First check if it's an omnibus (e.g., "Series 1-3" or "Series 1-4")
                val omnibusPattern = Regex(
                    """^(${flexibleSeriesPattern})\s*(?:-\s*)?#?\s*0*1-\d+$""",
                    RegexOption.IGNORE_CASE
                )
                val omnibusMatch = omnibusPattern.find(workingTitle)
                if (omnibusMatch != null) {
                    // It's an omnibus - keep the full title including the range
                    cleanedTitle = TextNormalizer.normalizeTitle(workingTitle + ", $seriesArticle")
                    isOmnibus = true
                }
            }
            
            // Only proceed with normal pattern matching if it's not an omnibus
            if (!isOmnibus) {
                var seriesPattern = Regex(
                    """^${flexibleSeriesPattern}\s*(?:-\s*)?#?\s*0*${Regex.escape(seriesNumber)}(?:-\d+(?:\.\d+)?)?\s*[-:]\s*(.+)$""",
                    RegexOption.IGNORE_CASE
                )
                match = seriesPattern.find(workingTitle)

                // Also try with normalized decimal (remove trailing zeros for decimal numbers)
                // E.g., if seriesNumber="2.50", also try to match "2.5" in the title
                if (match == null && seriesNumber.contains(".")) {
                    val normalizedDecimal = seriesNumber.toDoubleOrNull()?.toString() ?: seriesNumber
                    if (normalizedDecimal != seriesNumber) {
                        seriesPattern = Regex(
                            """^${flexibleSeriesPattern}\s*(?:-\s*)?#?\s*0*${Regex.escape(normalizedDecimal)}(?:-\d+(?:\.\d+)?)?\s*[-:]\s*(.+)$""",
                            RegexOption.IGNORE_CASE
                        )
                        match = seriesPattern.find(workingTitle)
                    }
                }

                // Fallback 1: Try without series suffix (e.g., "-Saga", "-Anthologie", "-Trilogie")
                // Also matches suffixes without hyphen (e.g., "FederLeichtSaga")
                if (match == null) {
                    val suffixPattern = Regex("""-?(Saga|Dilogie|Dilog[iy]a?|Duologie|Duolog[iy]a?|Trilogie|Trilog[iy]a?|Tétralogie|Tetralogie|Tetralog[iy]a?|Anthologie|Antholog[iy]|Reihe|Serie|Series|Sammlung|Collection)$""", RegexOption.IGNORE_CASE)
                    val suffixMatch = suffixPattern.find(seriesWithoutArticle)
                    if (suffixMatch != null) {
                        seriesWithoutArticle = seriesWithoutArticle.replace(suffixPattern, "")
                        normalizedSeriesWithoutArticle = TextNormalizer.normalizeTitle(seriesWithoutArticle)
                        val flexibleSeriesPatternWithoutSuffix = seriesNameToFlexiblePattern(normalizedSeriesWithoutArticle)
                        seriesPattern = Regex(
                            """^${flexibleSeriesPatternWithoutSuffix}\s*(?:-\s*)?#?\s*0*${Regex.escape(seriesNumber)}(?:-\d+(?:\.\d+)?)?\s*[-:]\s*(.+)$""",
                            RegexOption.IGNORE_CASE
                        )
                        match = seriesPattern.find(workingTitle)
                    }
                }

                // Fallback 2: For decimal numbers (e.g., "7.5"), try with letter suffix (e.g., "7a", "07a")
                if (match == null && seriesNumber.contains(".")) {
                    val intPart = seriesNumber.substringBefore(".")
                    seriesPattern = Regex(
                        """^${flexibleSeriesPattern}\s*(?:-\s*)?#?\s*0*${Regex.escape(intPart)}[a-z]?\s*[-:]\s*(.+)$""",
                        RegexOption.IGNORE_CASE
                    )
                    match = seriesPattern.find(workingTitle)
                }

                // Fallback 3: For book #1 ONLY, if title is just "Series 1" without additional title
                if (match == null && seriesNumber == "1") {
                    // Check if it's just "Series 1" without range
                    seriesPattern = Regex(
                        """^(${flexibleSeriesPattern})\s*(?:-\s*)?#?\s*0*1$""",
                        RegexOption.IGNORE_CASE
                    )
                    val bookOneMatch = seriesPattern.find(workingTitle)
                    if (bookOneMatch != null) {
                        // Just remove the "1" and keep series name
                        cleanedTitle = TextNormalizer.normalizeTitle(bookOneMatch.groupValues[1].trim() + ", $seriesArticle")
                    }
                }
            }

            if (match != null) {
                val extractedTitle = match.groupValues[1].trim()

                // Check if extracted title starts with lowercase letter
                if (extractedTitle.isNotEmpty() && extractedTitle[0].isLowerCase()) {
                    // Special case: Only remove the number/range, keep series name
                    val numberPattern = if (seriesNumber.contains(".")) {
                        val intPart = seriesNumber.substringBefore(".")
                        Regex("""^(${flexibleSeriesPattern})\s*(?:-\s*)?#?\s*0*${Regex.escape(intPart)}[a-z]?(?:-\d+(?:\.\d+)?)?\s*[-:]\s*""", RegexOption.IGNORE_CASE)
                    } else {
                        Regex("""^(${flexibleSeriesPattern})\s*(?:-\s*)?#?\s*0*${Regex.escape(seriesNumber)}(?:-\d+(?:\.\d+)?)?\s*[-:]\s*""", RegexOption.IGNORE_CASE)
                    }
                    val titleWithSeries = workingTitle.replace(numberPattern, "$1 ")
                    cleanedTitle = TextNormalizer.normalizeTitle("$titleWithSeries, $seriesArticle")
                } else {
                    // Normal case: Title starts with uppercase, use extracted title without article
                    cleanedTitle = TextNormalizer.normalizeTitle(extractedTitle)
                }
            }
        } else {
            // Series doesn't start with article
            var workingSeries = series
            var normalizedSeries = TextNormalizer.normalizeTitle(workingSeries)
            val flexibleSeriesPattern = seriesNameToFlexiblePattern(normalizedSeries)

            // Special case: Check for omnibus/collection FIRST
            var match: MatchResult? = null
            var isOmnibus = false
            var seriesPattern: Regex

            if (seriesNumber == "1") {
                val omnibusCheckPattern = Regex(
                    """^${flexibleSeriesPattern}\s*(?:-\s*)?#?\s*0*1-\d+$""",
                    RegexOption.IGNORE_CASE
                )
                val omnibusCheck = omnibusCheckPattern.find(rawTitle)
                if (omnibusCheck != null) {
                    // It's an omnibus - keep the full title as-is
                    cleanedTitle = TextNormalizer.normalizeTitle(rawTitle)
                    isOmnibus = true
                }
            }

            // Only run normal pattern matching if omnibus wasn't detected
            if (!isOmnibus) {
                seriesPattern = Regex(
                    """^${flexibleSeriesPattern}\s*(?:-\s*)?#?\s*0*${Regex.escape(seriesNumber)}(?:-\d+(?:\.\d+)?)?\s*[-:]\s*(.+)$""",
                    RegexOption.IGNORE_CASE
                )
                match = seriesPattern.find(rawTitle)

                // Also try with normalized decimal (remove trailing zeros for decimal numbers)
                // E.g., if seriesNumber="2.50", also try to match "2.5" in the title
                if (match == null && seriesNumber.contains(".")) {
                    val normalizedDecimal = seriesNumber.toDoubleOrNull()?.toString() ?: seriesNumber
                    if (normalizedDecimal != seriesNumber) {
                        seriesPattern = Regex(
                            """^${flexibleSeriesPattern}\s*(?:-\s*)?#?\s*0*${Regex.escape(normalizedDecimal)}(?:-\d+(?:\.\d+)?)?\s*[-:]\s*(.+)$""",
                            RegexOption.IGNORE_CASE
                        )
                        match = seriesPattern.find(rawTitle)
                    }
                }
            }

            // Fallback 1: Try without series suffix
            // Also matches suffixes without hyphen (e.g., "FederLeichtSaga")
            if (!isOmnibus && match == null) {
                val suffixPattern = Regex("""-?(Saga|Dilogie|Dilog[iy]a?|Duologie|Duolog[iy]a?|Trilogie|Trilog[iy]a?|Tétralogie|Tetralogie|Tetralog[iy]a?|Anthologie|Antholog[iy]|Reihe|Serie|Series|Sammlung|Collection)$""", RegexOption.IGNORE_CASE)
                val suffixMatch = suffixPattern.find(workingSeries)
                if (suffixMatch != null) {
                    workingSeries = workingSeries.replace(suffixPattern, "")
                    normalizedSeries = TextNormalizer.normalizeTitle(workingSeries)
                    val flexibleSeriesPatternWithoutSuffix = seriesNameToFlexiblePattern(normalizedSeries)
                    seriesPattern = Regex(
                        """^${flexibleSeriesPatternWithoutSuffix}\s*(?:-\s*)?#?\s*0*${Regex.escape(seriesNumber)}(?:-\d+(?:\.\d+)?)?\s*[-:]\s*(.+)$""",
                        RegexOption.IGNORE_CASE
                    )
                    match = seriesPattern.find(rawTitle)
                }
            }

            // Fallback 2: For decimal numbers
            if (!isOmnibus && match == null && seriesNumber.contains(".")) {
                val intPart = seriesNumber.substringBefore(".")
                seriesPattern = Regex(
                    """^${flexibleSeriesPattern}\s*(?:-\s*)?#?\s*0*${Regex.escape(intPart)}[a-z]?\s*[-:]\s*(.+)$""",
                    RegexOption.IGNORE_CASE
                )
                match = seriesPattern.find(rawTitle)
            }

            // Fallback 3: For book #1 ONLY
            if (!isOmnibus && match == null && seriesNumber == "1") {
                val omnibusPattern = Regex(
                    """^(${flexibleSeriesPattern})\s*(?:-\s*)?#?\s*0*1-\d+$""",
                    RegexOption.IGNORE_CASE
                )
                val omnibusMatch = omnibusPattern.find(rawTitle)
                if (omnibusMatch != null) {
                    cleanedTitle = TextNormalizer.normalizeTitle(rawTitle)
                } else {
                    seriesPattern = Regex(
                        """^(${flexibleSeriesPattern})\s*(?:-\s*)?#?\s*0*1$""",
                        RegexOption.IGNORE_CASE
                    )
                    val bookOneMatch = seriesPattern.find(rawTitle)
                    if (bookOneMatch != null) {
                        cleanedTitle = TextNormalizer.normalizeTitle(bookOneMatch.groupValues[1].trim())
                    }
                }
            }

            if (!isOmnibus && match != null) {
                val extractedTitle = match.groupValues[1].trim()

                // Check if extracted title starts with lowercase letter
                if (extractedTitle.isNotEmpty() && extractedTitle[0].isLowerCase()) {
                    // Special case: Only remove the number/range, keep series name
                    val numberPattern = if (seriesNumber.contains(".")) {
                        val intPart = seriesNumber.substringBefore(".")
                        Regex("""^(${flexibleSeriesPattern})\s*(?:-\s*)?#?\s*0*${Regex.escape(intPart)}[a-z]?(?:-\d+(?:\.\d+)?)?\s*[-:]\s*""", RegexOption.IGNORE_CASE)
                    } else {
                        Regex("""^(${flexibleSeriesPattern})\s*(?:-\s*)?#?\s*0*${Regex.escape(seriesNumber)}(?:-\d+(?:\.\d+)?)?\s*[-:]\s*""", RegexOption.IGNORE_CASE)
                    }
                    val titleWithSeries = rawTitle.replace(numberPattern, "$1 ")
                    cleanedTitle = TextNormalizer.normalizeTitle(titleWithSeries)
                } else {
                    // Normal case: Title starts with uppercase
                    cleanedTitle = TextNormalizer.normalizeTitle(extractedTitle)
                }
            }
        }

        return cleanedTitle
    }
}

