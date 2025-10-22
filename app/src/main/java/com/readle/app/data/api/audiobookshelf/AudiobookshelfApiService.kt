package com.readle.app.data.api.audiobookshelf

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface AudiobookshelfApiService {

    @POST("login")
    suspend fun login(
        @Body request: AudiobookshelfLoginRequest
    ): Response<AudiobookshelfLoginResponse>

    @GET("api/libraries")
    suspend fun getLibraries(
        @Header("Authorization") auth: String
    ): Response<AudiobookshelfLibrariesResponse>

    @GET("api/libraries/{id}/items")
    suspend fun getLibraryItems(
        @Path("id") libraryId: String,
        @Header("Authorization") auth: String,
        @retrofit2.http.Query("minified") minified: Int = 0
    ): Response<AudiobookshelfLibraryItemsResponse>

    @GET("api/items/{id}")
    suspend fun getLibraryItem(
        @Path("id") libraryItemId: String,
        @Header("Authorization") auth: String
    ): Response<AudiobookshelfLibraryItem>
    
    @POST("api/authorize")
    suspend fun authorize(
        @Header("Authorization") auth: String
    ): Response<AudiobookshelfAuthorizeResponse>
    
    @PATCH("api/me/progress/{id}")
    suspend fun updateProgress(
        @Path("id") libraryItemId: String,
        @Header("Authorization") auth: String,
        @Body request: AudiobookshelfUpdateProgressRequest
    ): Response<Unit>

    @retrofit2.http.Streaming
    @GET("api/items/{id}/file/{ino}")
    suspend fun downloadEbookFile(
        @Path("id") libraryItemId: String,
        @Path("ino") fileIno: String,
        @Header("Authorization") auth: String
    ): Response<okhttp3.ResponseBody>
}

