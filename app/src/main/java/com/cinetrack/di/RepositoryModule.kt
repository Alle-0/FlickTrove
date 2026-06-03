package com.cinetrack.di

import com.cinetrack.data.repository.FeedbackRepository
import com.cinetrack.data.repository.FeedbackRepositoryImpl
import com.cinetrack.data.repository.SettingsRepository
import com.cinetrack.data.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindFeedbackRepository(
        feedbackRepositoryImpl: FeedbackRepositoryImpl
    ): FeedbackRepository

    @Binds
    @Singleton
    abstract fun bindWidgetNotifier(
        glanceWidgetNotifier: com.cinetrack.widget.GlanceWidgetNotifier
    ): com.cinetrack.domain.WidgetNotifier
}
