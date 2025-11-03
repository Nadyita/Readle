package com.readle.app.data.api.audiobookshelf

import com.google.gson.annotations.SerializedName

data class AudiobookshelfLoginRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String
)

data class AudiobookshelfLoginResponse(
    @SerializedName("user")
    val user: AudiobookshelfUser
)

data class AudiobookshelfUser(
    @SerializedName("id")
    val id: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("token")
    val token: String,
    @SerializedName("type")
    val type: String
)

data class AudiobookshelfLibrariesResponse(
    @SerializedName("libraries")
    val libraries: List<AudiobookshelfLibrary>
)

data class AudiobookshelfLibrary(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("mediaType")
    val mediaType: String
)

data class AudiobookshelfLibraryItemsResponse(
    @SerializedName("results")
    val results: List<AudiobookshelfLibraryItem>,
    @SerializedName("total")
    val total: Int
)

data class AudiobookshelfLibraryItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("mediaType")
    val mediaType: String,
    @SerializedName("media")
    val media: AudiobookshelfMedia,
    @SerializedName("userMediaProgress")
    val userMediaProgress: AudiobookshelfMediaProgress?,
    @SerializedName("addedAt")
    val addedAt: Long? = null,
    @SerializedName("mtimeMs")
    val mtimeMs: Long? = null,
    @SerializedName("birthtimeMs")
    val birthtimeMs: Long? = null,
    @SerializedName("ctimeMs")
    val ctimeMs: Long? = null
)

data class AudiobookshelfMedia(
    @SerializedName("metadata")
    val metadata: AudiobookshelfMetadata,
    @SerializedName("audioFiles")
    val audioFiles: List<Any>? = null,
    @SerializedName("tracks")
    val tracks: List<Any>? = null,
    @SerializedName("ebookFile")
    val ebookFile: AudiobookshelfEbookFile? = null,
    @SerializedName("ebookFormat")
    val ebookFormat: String? = null,
    @SerializedName("numAudioFiles")
    val numAudioFiles: Int? = null
)

data class AudiobookshelfEbookFile(
    @SerializedName("ino")
    val ino: String,
    @SerializedName("metadata")
    val metadata: AudiobookshelfFileMetadata?,
    @SerializedName("ebookFormat")
    val ebookFormat: String?
)

data class AudiobookshelfFileMetadata(
    @SerializedName("filename")
    val filename: String?,
    @SerializedName("ext")
    val ext: String?,
    @SerializedName("size")
    val size: Long?,
    @SerializedName("mtimeMs")
    val mtimeMs: Long? = null,
    @SerializedName("birthtimeMs")
    val birthtimeMs: Long? = null,
    @SerializedName("ctimeMs")
    val ctimeMs: Long? = null
)

data class AudiobookshelfMetadata(
    @SerializedName("title")
    val title: String?,
    @SerializedName("titleIgnorePrefix")
    val titleIgnorePrefix: String?,
    @SerializedName("subtitle")
    val subtitle: String?,
    @SerializedName("authors")
    val authors: List<AudiobookshelfAuthor>?,
    @SerializedName("authorName")
    val authorName: String?,
    @SerializedName("authorNameLF")
    val authorNameLF: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("isbn")
    val isbn: String?,
    @SerializedName("series")
    val series: List<AudiobookshelfSeries>?,
    @SerializedName("seriesName")
    val seriesName: String?
)

data class AudiobookshelfAuthor(
    @SerializedName("name")
    val name: String
)

data class AudiobookshelfSeries(
    @SerializedName("name")
    val name: String,
    @SerializedName("sequence")
    val sequence: String?
)

data class AudiobookshelfMediaProgress(
    @SerializedName("isFinished")
    val isFinished: Boolean
)

data class AudiobookshelfAuthorizeResponse(
    @SerializedName("user")
    val user: AudiobookshelfAuthorizeUser
)

data class AudiobookshelfAuthorizeUser(
    @SerializedName("id")
    val id: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("token")
    val token: String,
    @SerializedName("mediaProgress")
    val mediaProgress: List<AudiobookshelfMediaProgressDetail>
)

data class AudiobookshelfMediaProgressDetail(
    @SerializedName("id")
    val id: String,
    @SerializedName("libraryItemId")
    val libraryItemId: String,
    @SerializedName("isFinished")
    val isFinished: Boolean
)

data class AudiobookshelfUpdateProgressRequest(
    @SerializedName("isFinished")
    val isFinished: Boolean
)

