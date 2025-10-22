package com.readle.app.data.api.openlibrary

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenLibraryApiService {
    
    @GET("api/books")
    suspend fun getBookByIsbn(
        @Query("bibkeys") bibkeys: String,
        @Query("format") format: String = "json",
        @Query("jscmd") jscmd: String = "data"
    ): Response<Map<String, OpenLibraryBookData>>
}

