package com.readle.app.util

/**
 * Utility class for author name format conversions used during Audiobookshelf import.
 * Extracted from AudiobookshelfApiClient for testability.
 */
object AuthorSeparatorConverter {

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
    fun convertAuthorSeparator(authorString: String, isLastNameFirst: Boolean): String {
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
}

