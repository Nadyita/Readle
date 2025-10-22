package com.readle.app.data.api.isbndb

import com.google.gson.annotations.SerializedName

data class IsbnDbResponse(
    @SerializedName("total")
    val total: Int,
    @SerializedName("books")
    val books: List<IsbnDbBook>?
)

data class IsbnDbBook(
    @SerializedName("title")
    val title: String?,
    @SerializedName("authors")
    val authors: List<String>?,
    @SerializedName("publisher")
    val publisher: String?,
    @SerializedName("date_published")
    val datePublished: String?,
    @SerializedName("synopsis")
    val synopsis: String?,
    @SerializedName("isbn")
    val isbn: String?,
    @SerializedName("isbn13")
    val isbn13: String?,
    @SerializedName("language")
    val language: String?,
    @SerializedName("image")
    val image: String?
)

