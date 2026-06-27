package com.cinetrack.di

import com.cinetrack.data.api.OmdbService
import com.cinetrack.data.api.TraktService
import com.cinetrack.data.api.TMDBService
import com.cinetrack.data.api.NewsService
import com.cinetrack.utils.Keys
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.first
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
    fun provideOkHttpClient(preferenceRepository: com.cinetrack.data.repository.PreferenceRepository): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.cinetrack.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val tmdbAuthInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            if (original.url.host.contains("themoviedb.org")) {
                val rawLanguage = kotlinx.coroutines.runBlocking { 
                    preferenceRepository.userPreferencesFlow.first().contentLanguage 
                }
                val resolvedLanguage = if (rawLanguage == "system") {
                    java.util.Locale.getDefault().language
                } else {
                    rawLanguage
                }
                val urlBuilder = original.url.newBuilder()
                    .addQueryParameter("api_key", Keys.getTmdbKey())
                    .setQueryParameter("language", resolvedLanguage)

                if (original.url.queryParameter("include_image_language") != null) {
                    val imageLanguage = "$resolvedLanguage,en,null"
                    urlBuilder.setQueryParameter("include_image_language", imageLanguage)
                }
                
                val url = urlBuilder.build()
                val request = original.newBuilder().url(url).build()
                chain.proceed(request)
            } else {
                chain.proceed(original)
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(tmdbAuthInterceptor)
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
    @Named("news_retrofit")
    fun provideNewsRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://localhost/") // overridden by @Url
            .client(okHttpClient)
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
    @Singleton
    fun provideNewsService(@Named("news_retrofit") retrofit: Retrofit): NewsService = retrofit.create(NewsService::class.java)

    @Provides
    @Named("tmdb_api_key")
    fun provideTMDBApiKey(): String = Keys.getTmdbKey()

    @Provides
    @Named("omdb_api_key")
    fun provideOMDBApiKey(): String = Keys.getOmdbKey()

    @Provides
    @Named("trakt_api_key")
    fun provideTraktApiKey(): String = Keys.getTraktKey()
}
