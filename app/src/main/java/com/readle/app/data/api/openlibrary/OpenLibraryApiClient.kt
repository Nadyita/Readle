package com.readle.app.data.api.openlibrary

import android.util.Log
import com.readle.app.data.api.model.BookDataSource
import com.readle.app.data.api.model.BookSearchResult
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenLibraryApiClient @Inject constructor() {
    
    private val apiService: OpenLibraryApiService
    
    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl("https://openlibrary.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(OpenLibraryApiService::class.java)
    }
    
    suspend fun searchByIsbn(isbn: String): Result<List<BookSearchResult>> {
        return try {
            val cleanIsbn = isbn.replace("-", "").replace(" ", "")
            val bibkey = "ISBN:$cleanIsbn"
            
            
            val response = apiService.getBookByIsbn(bibkey)
            
            if (response.isSuccessful && response.body() != null) {
                val bookMap = response.body()!!
                
                if (bookMap.isEmpty()) {
                    return Result.success(emptyList())
                }
                
                val bookData = bookMap.values.first()
                val searchResult = bookData.toBookSearchResult(cleanIsbn)
                
                
                Result.success(listOf(searchResult))
            } else {
                Log.e("OpenLibrary", "API error: ${response.code()}")
                Result.failure(Exception("Open Library API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("OpenLibrary", "Search failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun OpenLibraryBookData.toBookSearchResult(isbn: String): BookSearchResult {
        val authors = this.authors?.mapNotNull { it.name }?.joinToString("; ") ?: "Unknown Author"
        
        // Build description from available sources
        val description = buildString {
            // Use excerpts if available (often contains book description)
            val excerpt = excerpts?.firstOrNull()?.text
            if (!excerpt.isNullOrBlank()) {
                append(excerpt.trim())
            }
            
            // Add notes if available and different from excerpt
            if (!notes.isNullOrBlank() && notes != excerpt) {
                if (isNotEmpty()) append("\n\n")
                append(notes.trim())
            }
        }.takeIf { it.isNotBlank() }
        
        val fullTitle = if (!subtitle.isNullOrBlank()) {
            "$title: $subtitle"
        } else {
            title ?: "Unknown Title"
        }
        
        val publisher = publishers?.firstOrNull()?.name
        val coverUrl = cover?.large ?: cover?.medium
        
        return BookSearchResult(
            title = fullTitle,
            author = authors,
            description = description,
            publishDate = publishDate,
            publisher = publisher,
            isbn = isbn,
            coverUrl = coverUrl,
            source = BookDataSource.OPEN_LIBRARY,
            allIsbns = listOf(isbn)
        )
    }
}

