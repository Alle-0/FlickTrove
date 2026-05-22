package com.cinetrack.di

import android.content.Context
import com.cinetrack.data.local.dao.CacheDao
import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.local.dao.FolderDao
import com.cinetrack.data.local.dao.SearchHistoryDao
import com.cinetrack.data.local.database.FlickTroveDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FlickTroveDatabase {
        return FlickTroveDatabase.getInstance(context)
    }

    @Provides
    fun provideFavoriteDao(database: FlickTroveDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideFolderDao(database: FlickTroveDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideCacheDao(database: FlickTroveDatabase): CacheDao = database.cacheDao()

    @Provides
    fun provideSearchHistoryDao(database: FlickTroveDatabase): SearchHistoryDao = database.searchHistoryDao()
}
