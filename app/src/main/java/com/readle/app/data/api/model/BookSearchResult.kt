package com.readle.app.data.api.model

data class BookSearchResult(
    val title: String,
    val author: String,
    val description: String? = null,
    val publisher: String? = null,
    val publishDate: String? = null,
    val language: String? = null,
    val originalLanguage: String? = null,
    val series: String? = null,
    val seriesNumber: String? = null,
    val isbn: String? = null,
    val allIsbns: List<String> = listOfNotNull(isbn),
    val coverUrl: String? = null,
    val source: BookDataSource
)

enum class BookDataSource {
    DNB,
    GOOGLE_BOOKS,
    ISBN_DB,
    OPEN_LIBRARY
}

