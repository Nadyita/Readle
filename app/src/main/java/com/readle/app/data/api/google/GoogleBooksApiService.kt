package com.readle.app.data.api.google

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApiService {

    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 10,
        @Query("langRestrict") langRestrict: String? = null
    ): Response<GoogleBooksResponse>
}

