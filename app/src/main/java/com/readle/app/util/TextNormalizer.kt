package com.readle.app.util

object TextNormalizer {
    
    /**
     * Normalizes a title for sorting by removing leading articles and special characters
     * based on the book's language. Used for locale-aware sorting with Collator.
     * 
     * This function:
     * 1. Removes leading special characters (¿, ¡, quotes, etc.)
     * 2. Removes leading articles based on language (Der/Die/Das for German, The for English, etc.)
     * 3. Preserves umlauts and accents for Collator to handle
     * 
     * Examples:
     * - "¿El Último Día?" (es) -> "Último Día"
     * - "Der Überfall" (de) -> "Überfall"
     * - "The Great Gatsby" (en) -> "Great Gatsby"
     * 
     * @param title The original title
     * @param language The book's language code (ISO 639-1, e.g., "de", "en", "es")
     * @return The normalized title for sorting
     */
    fun normalizeTitleForSorting(title: String, language: String?): String {
        var normalized = title.trim()
        
        // Step 1: Remove leading special characters
        val leadingCharsToRemove = charArrayOf('\u00BF', '\u00A1', '"', '\'', '\u00AB', '\u00BB', '\u201C', '\u201D', '\u2018', '\u2019', '\u201E', '\u201A', '\u2039', '\u203A')
        normalized = normalized.trimStart(*leadingCharsToRemove)
        
        // Step 2: Remove trailing special characters (matching pairs)
        val trailingCharsToRemove = charArrayOf('?', '!', '"', '\'', '\u00AB', '\u00BB', '\u201C', '\u201D', '\u2018', '\u2019', '\u201E', '\u201A', '\u2039', '\u203A')
        normalized = normalized.trimEnd(*trailingCharsToRemove)
        
        normalized = normalized.trim()
        
        // Step 3: Remove leading articles based on language
        val languageCode = language?.lowercase()?.take(2) // Take first 2 chars for language code
        
        when (languageCode) {
            "de" -> {
                // German articles
                val articles = listOf("Der ", "Die ", "Das ", "Ein ", "Eine ", "Einer ", "Eines ", "Einem ", "Einen ")
                for (article in articles) {
                    if (normalized.startsWith(article, ignoreCase = true)) {
                        normalized = normalized.substring(article.length).trim()
                        break
                    }
                }
            }
            "en" -> {
                // English articles
                val articles = listOf("The ", "A ", "An ")
                for (article in articles) {
                    if (normalized.startsWith(article, ignoreCase = true)) {
                        normalized = normalized.substring(article.length).trim()
                        break
                    }
                }
            }
            "es" -> {
                // Spanish articles
                val articles = listOf("El ", "La ", "Los ", "Las ", "Un ", "Una ", "Unos ", "Unas ")
                for (article in articles) {
                    if (normalized.startsWith(article, ignoreCase = true)) {
                        normalized = normalized.substring(article.length).trim()
                        break
                    }
                }
            }
            "fr" -> {
                // French articles
                val articles = listOf("Le ", "La ", "Les ", "L'", "Un ", "Une ", "Des ")
                for (article in articles) {
                    if (normalized.startsWith(article, ignoreCase = true)) {
                        normalized = normalized.substring(article.length).trim()
                        break
                    }
                }
            }
            "it" -> {
                // Italian articles
                val articles = listOf("Il ", "Lo ", "La ", "I ", "Gli ", "Le ", "Un ", "Una ", "Uno ")
                for (article in articles) {
                    if (normalized.startsWith(article, ignoreCase = true)) {
                        normalized = normalized.substring(article.length).trim()
                        break
                    }
                }
            }
            "pt" -> {
                // Portuguese articles
                val articles = listOf("O ", "A ", "Os ", "As ", "Um ", "Uma ", "Uns ", "Umas ")
                for (article in articles) {
                    if (normalized.startsWith(article, ignoreCase = true)) {
                        normalized = normalized.substring(article.length).trim()
                        break
                    }
                }
            }
            "nl" -> {
                // Dutch articles
                val articles = listOf("De ", "Het ", "Een ")
                for (article in articles) {
                    if (normalized.startsWith(article, ignoreCase = true)) {
                        normalized = normalized.substring(article.length).trim()
                        break
                    }
                }
            }
            else -> {
                // No language specified or unsupported language - remove common English articles as fallback
                val articles = listOf("The ", "A ", "An ")
                for (article in articles) {
                    if (normalized.startsWith(article, ignoreCase = true)) {
                        normalized = normalized.substring(article.length).trim()
                        break
                    }
                }
            }
        }
        
        return normalized
    }
    
    /**
     * Normalizes book titles by moving leading articles to the end.
     * Examples:
     * - "Die Haarteppichknüpfer" -> "Haarteppichknüpfer, Die"
     * - "Der kleine Prinz" -> "Kleine Prinz, Der"
     * - "The Great Gatsby" -> "Great Gatsby, The"
     */
    fun normalizeTitle(title: String): String {
        val trimmed = title.trim()
        
        // German articles
        val germanArticles = listOf("Der ", "Die ", "Das ")
        for (article in germanArticles) {
            if (trimmed.startsWith(article, ignoreCase = true)) {
                val articlePart = trimmed.substring(0, article.length).trim()
                val rest = trimmed.substring(article.length).trim()
                return "$rest, $articlePart"
            }
        }
        
        // English article
        if (trimmed.startsWith("The ", ignoreCase = true)) {
            val rest = trimmed.substring(4).trim()
            return "$rest, The"
        }
        
        // French articles
        val frenchArticles = listOf("Le ", "La ", "Les ", "L'")
        for (article in frenchArticles) {
            if (trimmed.startsWith(article, ignoreCase = true)) {
                val articlePart = trimmed.substring(0, article.length).trim()
                val rest = trimmed.substring(article.length).trim()
                return "$rest, $articlePart"
            }
        }
        
        return trimmed
    }
    
    /**
     * Normalizes author names to "LastName, FirstName(s)" format.
     * Supports multiple authors separated by "; ".
     * This is a best-effort implementation that handles common cases.
     * 
     * Examples:
     * - "Andreas Eschbach" -> "Eschbach, Andreas"
     * - "J.K. Rowling" -> "Rowling, J.K."
     * - "Eschbach, Andreas" -> "Eschbach, Andreas" (already normalized)
     * - "John Doe; Jane Smith" -> "Doe, John; Smith, Jane"
     */
    fun normalizeAuthor(author: String): String {
        val trimmed = author.trim()
        
        // Handle multiple authors separated by "; "
        if (trimmed.contains("; ")) {
            return trimmed.split("; ")
                .map { normalizeSingleAuthor(it.trim()) }
                .joinToString("; ")
        }
        
        return normalizeSingleAuthor(trimmed)
    }
    
    private fun normalizeSingleAuthor(author: String): String {
        val trimmed = author.trim()
        
        // Already in "LastName, FirstName" format
        if (trimmed.contains(", ")) {
            return trimmed
        }
        
        // Split by spaces
        val parts = trimmed.split(Regex("\\s+"))
        
        when {
            parts.isEmpty() -> return trimmed
            parts.size == 1 -> return trimmed  // Only one name
            parts.size == 2 -> {
                // Simple case: FirstName LastName
                return "${parts[1]}, ${parts[0]}"
            }
            else -> {
                // Multiple parts - assume last part is last name, rest is first names
                val lastName = parts.last()
                val firstNames = parts.dropLast(1).joinToString(" ")
                return "$lastName, $firstNames"
            }
        }
    }
}


