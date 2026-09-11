package com.cinetrack.data.model

import com.cinetrack.ui.viewmodel.FeedState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable

@Serializable
data class BecauseYouWatchedData(
    val seed: Movie,
    val recommendations: List<Movie> = emptyList()
)

@Serializable
data class CachedFeedData(
    val recommendedMovies: List<Movie> = emptyList(),
    val popularMovies: List<Movie> = emptyList(),
    val nowPlayingMovies: List<Movie> = emptyList(),
    val top10Movies: List<Movie> = emptyList(),
    val upcomingMovies: List<Movie> = emptyList(),
    val recommendedTv: List<Movie> = emptyList(),
    val popularTv: List<Movie> = emptyList(),
    val nowStreamingTv: List<Movie> = emptyList(),
    val top10Tv: List<Movie> = emptyList(),
    val upcomingTv: List<Movie> = emptyList(),
    val trendingMovies: List<Movie> = emptyList(),
    val trendingTv: List<Movie> = emptyList(),
    val magazineNews: List<NewsItem> = emptyList(),
    val continueWatchingTv: List<Movie> = emptyList(),
    val becauseYouWatchedMovie: BecauseYouWatchedData? = null,
    val becauseYouWatchedTv: BecauseYouWatchedData? = null,
    val logoPaths: Map<Long, String> = emptyMap()
) {
    fun toFeedState(): FeedState {
        fun attachLogos(list: List<Movie>): List<Movie> {
            return list.map { movie ->
                val logo = logoPaths[movie.id]
                if (logo != null) movie.apply { this.logoPath = logo } else movie
            }
        }

        return FeedState(
            isLoaded = true,
            hasError = false,
            recommendedMovies = attachLogos(recommendedMovies).toImmutableList(),
            popularMovies = popularMovies.toImmutableList(),
            nowPlayingMovies = nowPlayingMovies.toImmutableList(),
            top10Movies = top10Movies.toImmutableList(),
            upcomingMovies = upcomingMovies.toImmutableList(),
            recommendedTv = attachLogos(recommendedTv).toImmutableList(),
            popularTv = popularTv.toImmutableList(),
            nowStreamingTv = nowStreamingTv.toImmutableList(),
            top10Tv = top10Tv.toImmutableList(),
            upcomingTv = upcomingTv.toImmutableList(),
            trendingMovies = attachLogos(trendingMovies).toImmutableList(),
            trendingTv = attachLogos(trendingTv).toImmutableList(),
            magazineNews = magazineNews.toImmutableList(),
            continueWatchingTv = continueWatchingTv.toImmutableList(),
            becauseYouWatchedMovie = becauseYouWatchedMovie?.let { Pair(it.seed, it.recommendations.toImmutableList()) },
            becauseYouWatchedTv = becauseYouWatchedTv?.let { Pair(it.seed, it.recommendations.toImmutableList()) }
        )
    }

    companion object {
        fun fromFeedState(feedState: FeedState): CachedFeedData {
            val collectedLogos = mutableMapOf<Long, String>()
            (feedState.trendingMovies + feedState.trendingTv + feedState.recommendedMovies + feedState.recommendedTv).forEach { movie ->
                movie.logoPath?.let { collectedLogos[movie.id] = it }
            }

            return CachedFeedData(
                recommendedMovies = feedState.recommendedMovies,
                popularMovies = feedState.popularMovies,
                nowPlayingMovies = feedState.nowPlayingMovies,
                top10Movies = feedState.top10Movies,
                upcomingMovies = feedState.upcomingMovies,
                recommendedTv = feedState.recommendedTv,
                popularTv = feedState.popularTv,
                nowStreamingTv = feedState.nowStreamingTv,
                top10Tv = feedState.top10Tv,
                upcomingTv = feedState.upcomingTv,
                trendingMovies = feedState.trendingMovies,
                trendingTv = feedState.trendingTv,
                magazineNews = feedState.magazineNews,
                continueWatchingTv = feedState.continueWatchingTv,
                becauseYouWatchedMovie = feedState.becauseYouWatchedMovie?.let { BecauseYouWatchedData(it.first, it.second) },
                becauseYouWatchedTv = feedState.becauseYouWatchedTv?.let { BecauseYouWatchedData(it.first, it.second) },
                logoPaths = collectedLogos
            )
        }
    }
}
