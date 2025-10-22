package com.readle.app.data.api.google

import com.google.gson.annotations.SerializedName

data class GoogleBooksResponse(
    @SerializedName("totalItems")
    val totalItems: Int,
    @SerializedName("items")
    val items: List<GoogleBookItem>?
)

data class GoogleBookItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("volumeInfo")
    val volumeInfo: GoogleVolumeInfo
)

data class GoogleVolumeInfo(
    @SerializedName("title")
    val title: String?,
    @SerializedName("authors")
    val authors: List<String>?,
    @SerializedName("publisher")
    val publisher: String?,
    @SerializedName("publishedDate")
    val publishedDate: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("industryIdentifiers")
    val industryIdentifiers: List<GoogleIndustryIdentifier>?,
    @SerializedName("pageCount")
    val pageCount: Int?,
    @SerializedName("categories")
    val categories: List<String>?,
    @SerializedName("imageLinks")
    val imageLinks: GoogleImageLinks?,
    @SerializedName("language")
    val language: String?,
    @SerializedName("seriesInfo")
    val seriesInfo: GoogleSeriesInfo?
)

data class GoogleIndustryIdentifier(
    @SerializedName("type")
    val type: String,
    @SerializedName("identifier")
    val identifier: String
)

data class GoogleImageLinks(
    @SerializedName("smallThumbnail")
    val smallThumbnail: String?,
    @SerializedName("thumbnail")
    val thumbnail: String?,
    @SerializedName("small")
    val small: String?,
    @SerializedName("medium")
    val medium: String?,
    @SerializedName("large")
    val large: String?
)

data class GoogleSeriesInfo(
    @SerializedName("kind")
    val kind: String?,
    @SerializedName("volumeSeries")
    val volumeSeries: List<GoogleVolumeSeries>?
)

data class GoogleVolumeSeries(
    @SerializedName("seriesId")
    val seriesId: String?,
    @SerializedName("seriesBookType")
    val seriesBookType: String?,
    @SerializedName("orderNumber")
    val orderNumber: Int?,
    @SerializedName("issue")
    val issue: List<GoogleSeriesIssue>?
)

data class GoogleSeriesIssue(
    @SerializedName("issueDisplayNumber")
    val issueDisplayNumber: String?
)

