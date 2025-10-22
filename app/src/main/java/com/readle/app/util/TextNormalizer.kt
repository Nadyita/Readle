package com.readle.app.util

object TextNormalizer {
    
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


