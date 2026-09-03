package com.cinetrack.ui.viewmodel

import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.NewsItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class FeedState(
    val isLoaded: Boolean = false,
    val hasError: Boolean = false,
    val recommendedMovies: ImmutableList<Movie> = persistentListOf(),
    val popularMovies: ImmutableList<Movie> = persistentListOf(),
    val nowPlayingMovies: ImmutableList<Movie> = persistentListOf(),
    val top10Movies: ImmutableList<Movie> = persistentListOf(),
    val upcomingMovies: ImmutableList<Movie> = persistentListOf(),
    val recommendedTv: ImmutableList<Movie> = persistentListOf(),
    val popularTv: ImmutableList<Movie> = persistentListOf(),
    val nowStreamingTv: ImmutableList<Movie> = persistentListOf(),
    val top10Tv: ImmutableList<Movie> = persistentListOf(),
    val upcomingTv: ImmutableList<Movie> = persistentListOf(),
    val trendingMovies: ImmutableList<Movie> = persistentListOf(),
    val trendingTv: ImmutableList<Movie> = persistentListOf(),
    val magazineNews: ImmutableList<NewsItem> = persistentListOf(),
    
    // Personalized Sections
    val continueWatchingTv: ImmutableList<Movie> = persistentListOf(),
    val becauseYouWatchedMovie: Pair<Movie, ImmutableList<Movie>>? = null,
    val becauseYouWatchedTv: Pair<Movie, ImmutableList<Movie>>? = null
)
