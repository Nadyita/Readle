package com.readle.app.data.api.isbndb

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface IsbnDbApiService {

    @GET("book/{isbn}")
    suspend fun getBookByIsbn(
        @Header("Authorization") apiKey: String,
        @Path("isbn") isbn: String
    ): Response<IsbnDbBook>

    @GET("books")
    suspend fun searchBooks(
        @Header("Authorization") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
        @Query("title") title: String? = null,
        @Query("author") author: String? = null
    ): Response<IsbnDbResponse>
}

