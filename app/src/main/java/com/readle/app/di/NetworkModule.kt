package com.readle.app.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.readle.app.data.api.dnb.DnbApiService
import com.readle.app.data.api.google.GoogleBooksApiService
import com.readle.app.data.api.isbndb.IsbnDbApiService
import com.readle.app.data.api.pocketbook.PocketbookCloudApiClient
import com.readle.app.data.preferences.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DnbRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoogleBooksRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsbnDbRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @DnbRetrofit
    fun provideDnbRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://services.dnb.de/sru/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @GoogleBooksRetrofit
    fun provideGoogleBooksRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/books/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @IsbnDbRetrofit
    fun provideIsbnDbRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api2.isbndb.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideDnbApiService(@DnbRetrofit retrofit: Retrofit): DnbApiService {
        return retrofit.create(DnbApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGoogleBooksApiService(@GoogleBooksRetrofit retrofit: Retrofit): GoogleBooksApiService {
        return retrofit.create(GoogleBooksApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideIsbnDbApiService(@IsbnDbRetrofit retrofit: Retrofit): IsbnDbApiService {
        return retrofit.create(IsbnDbApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePocketbookCloudApiClient(
        settingsDataStore: SettingsDataStore
    ): PocketbookCloudApiClient {
        return PocketbookCloudApiClient(settingsDataStore)
    }
}

