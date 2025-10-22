package com.readle.app.data.api.dnb

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DnbApiService {

    @GET("dnb")
    suspend fun searchByIsbn(
        @Query("version") version: String = "1.1",
        @Query("operation") operation: String = "searchRetrieve",
        @Query("query") query: String,
        @Query("recordSchema") recordSchema: String = "MARC21-xml",
        @Query("maximumRecords") maximumRecords: Int = 50
    ): Response<String>

    @GET("dnb")
    suspend fun searchByTitleAuthor(
        @Query("version") version: String = "1.1",
        @Query("operation") operation: String = "searchRetrieve",
        @Query("query") query: String,
        @Query("recordSchema") recordSchema: String = "MARC21-xml",
        @Query("maximumRecords") maximumRecords: Int
    ): Response<String>
}

