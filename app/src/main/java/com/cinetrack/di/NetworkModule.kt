package com.cinetrack.di

import com.cinetrack.BuildConfig
import com.cinetrack.data.api.OmdbService
import com.cinetrack.data.api.TraktService
import com.cinetrack.data.api.TMDBService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        // Remove Accept-Encoding: gzip from every outgoing request.
        // TMDb's CDN sometimes sends gzip responses with an incorrect Content-Length,
        // causing okio's GzipSource to throw "gzip finished without exhausting source".
        // By not advertising gzip support, the server returns plain JSON and the issue disappears.
        val noGzipInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request().newBuilder()
                .removeHeader("Accept-Encoding")
                .addHeader("Accept-Encoding", "identity")
                .build()
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .addInterceptor(noGzipInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @Named("tmdb_retrofit")
    fun provideTMDBRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    @Named("omdb_retrofit")
    fun provideOMDBRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://www.omdbapi.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    @Named("trakt_retrofit")
    fun provideTraktRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideTMDBService(@Named("tmdb_retrofit") retrofit: Retrofit): TMDBService = retrofit.create(TMDBService::class.java)

    @Provides
    @Singleton
    fun provideOMDBService(@Named("omdb_retrofit") retrofit: Retrofit): OmdbService = retrofit.create(OmdbService::class.java)

    @Provides
    @Singleton
    fun provideTraktService(@Named("trakt_retrofit") retrofit: Retrofit): TraktService = retrofit.create(TraktService::class.java)

    @Provides
    @Named("tmdb_api_key")
    fun provideTMDBApiKey(): String = BuildConfig.TMDB_API_KEY

    @Provides
    @Named("omdb_api_key")
    fun provideOMDBApiKey(): String = BuildConfig.OMDB_API_KEY

    @Provides
    @Named("trakt_api_key")
    fun provideTraktApiKey(): String = BuildConfig.TRAKT_API_KEY
}
