package com.readle.app.data.api.openlibrary

import com.google.gson.annotations.SerializedName

data class OpenLibraryBookData(
    @SerializedName("title")
    val title: String?,
    
    @SerializedName("subtitle")
    val subtitle: String?,
    
    @SerializedName("authors")
    val authors: List<OpenLibraryAuthor>?,
    
    @SerializedName("publishers")
    val publishers: List<OpenLibraryPublisher>?,
    
    @SerializedName("publish_date")
    val publishDate: String?,
    
    @SerializedName("number_of_pages")
    val numberOfPages: Int?,
    
    @SerializedName("cover")
    val cover: OpenLibraryCover?,
    
    @SerializedName("notes")
    val notes: String?,
    
    @SerializedName("excerpts")
    val excerpts: List<OpenLibraryExcerpt>?
)

data class OpenLibraryAuthor(
    @SerializedName("name")
    val name: String?
)

data class OpenLibraryPublisher(
    @SerializedName("name")
    val name: String?
)

data class OpenLibraryCover(
    @SerializedName("medium")
    val medium: String?,
    
    @SerializedName("large")
    val large: String?
)

data class OpenLibraryExcerpt(
    @SerializedName("text")
    val text: String?
)

