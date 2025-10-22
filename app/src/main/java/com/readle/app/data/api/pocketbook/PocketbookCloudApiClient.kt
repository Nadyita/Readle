package com.readle.app.data.api.pocketbook

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PocketbookCloudApiClient @Inject constructor(
    private val settingsDataStore: com.readle.app.data.preferences.SettingsDataStore
) {
    companion object {
        private const val BASE_URL = "https://cloud.pocketbook.digital/api/v1.0"
        private const val BASE_URL_V11 = "https://cloud.pocketbook.digital/api/v1.1"
        private const val CLIENT_ID = "qNAx1RDb"
        private const val CLIENT_SECRET = "K3YYSjCgDJNoWKdGVOyO1mrROp3MMZqqRNXNXTmh"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Get a valid access token. Uses cached token if available and not expired,
     * otherwise performs login and caches the new token.
     */
    suspend fun getValidAccessToken(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if we have a cached token
            val cachedToken = settingsDataStore.pocketbookAccessToken.first()
            val tokenExpiry = settingsDataStore.pocketbookTokenExpiry.first()
            val now = System.currentTimeMillis()

            // If token exists and not expired (with 5 min buffer), use it
            if (cachedToken.isNotBlank() && tokenExpiry > now + 300_000) {
                android.util.Log.d(
                    "PocketbookCloudApi",
                    "Using cached access token (expires in ${(tokenExpiry - now) / 1000}s)"
                )
                return@withContext Result.success(cachedToken)
            }

            // Token expired or missing - perform login
            android.util.Log.d("PocketbookCloudApi", "Cached token expired or missing, logging in...")

            // Get providers
            val providersResult = getProviders(email)
            if (providersResult.isFailure) {
                return@withContext Result.failure(
                    providersResult.exceptionOrNull() ?: Exception("Failed to get providers")
                )
            }

            val provider = providersResult.getOrNull()?.firstOrNull()
                ?: return@withContext Result.failure(Exception("No provider found"))

            // Login
            val loginResult = login(provider, email, password)
            if (loginResult.isFailure) {
                return@withContext Result.failure(
                    loginResult.exceptionOrNull() ?: Exception("Login failed")
                )
            }

            val loginData = loginResult.getOrNull()
                ?: return@withContext Result.failure(Exception("No token returned"))

            // Cache token with actual expiration from API (in milliseconds, with 5 min safety buffer)
            val expiryTime = now + ((loginData.expiresIn - 300) * 1000)
            settingsDataStore.setPocketbookAccessToken(loginData.accessToken)
            settingsDataStore.setPocketbookTokenExpiry(expiryTime)

            android.util.Log.d(
                "PocketbookCloudApi",
                "Successfully logged in and cached token (expires in ${loginData.expiresIn}s)"
            )

            Result.success(loginData.accessToken)
        } catch (e: Exception) {
            android.util.Log.e("PocketbookCloudApi", "Error getting access token: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getProviders(email: String): Result<List<PocketbookProvider>> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/auth/login?username=$email&client_id=$CLIENT_ID" +
                    "&client_secret=$CLIENT_SECRET&language=en"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to get providers: ${response.code}")
                )
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))

            val providersResponse = gson.fromJson(responseBody, PocketbookProvidersResponse::class.java)
            Result.success(providersResponse.providers)
        } catch (e: Exception) {
            android.util.Log.e("PocketbookCloudApi", "Failed to get providers: ${e.message}", e)
            Result.failure(e)
        }
    }

    data class LoginResult(
        val accessToken: String,
        val expiresIn: Int
    )

    suspend fun login(
        provider: PocketbookProvider,
        email: String,
        password: String
    ): Result<LoginResult> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/auth/login/${provider.alias}"

            val formBody = FormBody.Builder()
                .add("shop_id", provider.shopId)
                .add("username", email)
                .add("password", password)
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("grant_type", "password")
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Login failed: ${response.code}")
                )
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))

            val tokenResponse = gson.fromJson(responseBody, PocketbookTokenResponse::class.java)
            Result.success(LoginResult(tokenResponse.accessToken, tokenResponse.expiresIn))
        } catch (e: Exception) {
            android.util.Log.e("PocketbookCloudApi", "Login failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getBooks(accessToken: String): Result<List<PocketbookBook>> = withContext(Dispatchers.IO) {
        try {
            // First get total count
            val countUrl = "$BASE_URL/books?limit=0"
            val countRequest = Request.Builder()
                .url(countUrl)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val countResponse = client.newCall(countRequest).execute()
            if (!countResponse.isSuccessful) {
                // If 401 Unauthorized, clear cached token so next call will re-authenticate
                if (countResponse.code == 401) {
                    android.util.Log.w("PocketbookCloudApi", "Token expired (401), clearing cache")
                    settingsDataStore.clearPocketbookToken()
                }
                return@withContext Result.failure(
                    Exception("Failed to get book count: ${countResponse.code}")
                )
            }

            val countBody = countResponse.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))

            val countData = gson.fromJson(countBody, PocketbookBooksResponse::class.java)
            val total = countData.total

            if (total == 0) {
                return@withContext Result.success(emptyList())
            }

            // Now get all books
            val booksUrl = "$BASE_URL/books?limit=$total&offset=0"
            val booksRequest = Request.Builder()
                .url(booksUrl)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val booksResponse = client.newCall(booksRequest).execute()
            if (!booksResponse.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to get books: ${booksResponse.code}")
                )
            }

            val booksBody = booksResponse.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))

            val booksData = gson.fromJson(booksBody, PocketbookBooksResponse::class.java)
            Result.success(booksData.items)
        } catch (e: Exception) {
            android.util.Log.e("PocketbookCloudApi", "Failed to get books: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun syncReadingProgress(
        email: String,
        password: String
    ): Result<List<PocketbookBook>> = withContext(Dispatchers.IO) {
        try {
            // Get valid access token (uses cache if available)
            val tokenResult = getValidAccessToken(email, password)
            if (tokenResult.isFailure) {
                return@withContext Result.failure(
                    tokenResult.exceptionOrNull() ?: Exception("Failed to get access token")
                )
            }

            val accessToken = tokenResult.getOrNull()
                ?: return@withContext Result.failure(Exception("No access token received"))

            // Try to get ALL books (not just read ones - we need to mark cloud status)
            var booksResult = getBooks(accessToken)
            
            // If we got a 401 error, token is expired - retry with fresh token
            if (booksResult.isFailure) {
                val error = booksResult.exceptionOrNull()
                val errorMessage = error?.message ?: ""
                
                if (errorMessage.contains("401")) {
                    android.util.Log.w("PocketbookCloudApi", "Token expired (401), retrying with fresh login...")
                    
                    try {
                        // Force fresh login by clearing token (already done in getBooks)
                        // and get new token
                        val retryTokenResult = getValidAccessToken(email, password)
                        if (retryTokenResult.isSuccess) {
                            val newAccessToken = retryTokenResult.getOrNull()!!
                            booksResult = getBooks(newAccessToken)
                            
                            if (booksResult.isSuccess) {
                                android.util.Log.d("PocketbookCloudApi", "Retry successful with fresh token")
                            }
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        android.util.Log.e("PocketbookCloudApi", "Timeout during retry: ${e.message}")
                        return@withContext Result.failure(
                            Exception("Pocketbook Cloud is not responding. Please try again later.")
                        )
                    }
                }
                
                // If still failing, return error with better message for timeouts
                if (booksResult.isFailure) {
                    val failureError = booksResult.exceptionOrNull()
                    val betterMessage = when {
                        failureError?.message?.contains("timeout", ignoreCase = true) == true ->
                            "Pocketbook Cloud is not responding. Please try again later."
                        else -> failureError?.message ?: "Failed to get books"
                    }
                    
                    return@withContext Result.failure(Exception(betterMessage))
                }
            }

            val books = booksResult.getOrNull() ?: emptyList()

            // Return ALL books (cloud status marking happens in UseCase)
            Result.success(books)
        } catch (e: Exception) {
            android.util.Log.e("PocketbookCloudApi", "Sync failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadBookFromFile(
        accessToken: String,
        bookTitle: String,
        bookFile: java.io.File
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d(
                "PocketbookCloudApi",
                "Preparing upload: file=${bookFile.name}, size=${bookFile.length()}, title=$bookTitle"
            )

            // Determine correct MIME type based on file extension
            val mimeType = when (bookFile.extension.lowercase()) {
                "epub" -> "application/epub+zip"
                "pdf" -> "application/pdf"
                "mobi" -> "application/x-mobipocket-ebook"
                else -> "application/octet-stream"
            }

            // Use Pocketbook's v1.1 files API: PUT to /files/{filename}
            // Encode filename for URL
            val encodedFilename = java.net.URLEncoder.encode(bookFile.name, "UTF-8")
            val url = "$BASE_URL_V11/files/$encodedFilename"

            // Create request body from file
            val requestBody = bookFile.asRequestBody(mimeType.toMediaTypeOrNull())

            android.util.Log.d(
                "PocketbookCloudApi",
                "Uploading: fileName=${bookFile.name}, mimeType=$mimeType, size=${bookFile.length()}"
            )

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "de")
                .header("Cache-Control", "no-cache")
                .put(requestBody)
                .build()

            android.util.Log.d(
                "PocketbookCloudApi",
                "Sending PUT request to $url"
            )

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error body"
                
                // Handle 409 Conflict (file already exists) as success
                if (response.code == 409) {
                    android.util.Log.d(
                        "PocketbookCloudApi",
                        "Book already exists in Pocketbook: $bookTitle - $errorBody"
                    )
                    return@withContext Result.success(Unit)
                }
                
                android.util.Log.e(
                    "PocketbookCloudApi",
                    "Failed to upload book: ${response.code} ${response.message} - $errorBody"
                )
                return@withContext Result.failure(
                    Exception("Upload failed: HTTP ${response.code} - ${response.message}")
                )
            }

            // Log the response body to see what Pocketbook returns
            val responseBody = response.body?.string() ?: ""
            android.util.Log.d(
                "PocketbookCloudApi", 
                "Upload response (${response.code}): $responseBody"
            )
            android.util.Log.d("PocketbookCloudApi", "Successfully uploaded book: $bookTitle")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PocketbookCloudApi", "Upload error: ${e.message}", e)
            Result.failure(e)
        }
    }
}

