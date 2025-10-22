package com.readle.app.data.api.pocketbook

import com.google.gson.annotations.SerializedName

data class PocketbookProvidersResponse(
    @SerializedName("providers")
    val providers: List<PocketbookProvider>
)

data class PocketbookProvider(
    @SerializedName("alias")
    val alias: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("shop_id")
    val shopId: String,
    @SerializedName("icon")
    val icon: String?,
    @SerializedName("icon_eink")
    val iconEink: String?,
    @SerializedName("logged_by")
    val loggedBy: String?
)

data class PocketbookTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Int,
    @SerializedName("refresh_token")
    val refreshToken: String?
)

data class PocketbookBooksResponse(
    @SerializedName("total")
    val total: Int,
    @SerializedName("items")
    val items: List<PocketbookBook>
)

data class PocketbookBook(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("read_percent")
    val readPercent: Int,
    @SerializedName("read_status")
    val readStatus: String?,
    @SerializedName("metadata")
    val metadata: PocketbookMetadata
)

data class PocketbookMetadata(
    @SerializedName("title")
    val title: String?,
    @SerializedName("authors")
    val authors: String?,
    @SerializedName("isbn")
    val isbn: String?,
    @SerializedName("publisher")
    val publisher: String?,
    @SerializedName("year")
    val year: Int?,
    @SerializedName("series")
    val series: String?,
    @SerializedName("series_ord")
    val seriesOrd: String?,
    @SerializedName("sequence")
    val sequence: Int?  // Fallback, falls series_ord nicht vorhanden
)

