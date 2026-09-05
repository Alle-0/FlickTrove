package com.cinetrack.domain.usecase

import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.NewsRepository
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.domain.CalculateMatchScoreUseCase
import com.cinetrack.ui.viewmodel.FeedState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import javax.inject.Inject

class GetHomeFeedUseCase @Inject constructor(
    private val repository: MovieRepository,
    private val newsRepository: NewsRepository,
    private val preferenceRepository: PreferenceRepository,
    private val calculateMatchScoreUseCase: CalculateMatchScoreUseCase
) {
    suspend operator fun invoke(): FeedState {
        // Carica i film salvati dall'utente per generare le raccomandazioni
        val localMovies = repository.getLocalMoviesFlow().first()

        // Genera raccomandazioni personalizzate usando l'algoritmo
        val recMoviesDeferred = coroutineScope { async { 
            val recs = buildRecommendations(type = "movie", localMovies = localMovies)
            if (recs.isNotEmpty()) {
                val first = recs.first()
                try {
                    val detail = repository.getMovieDetail(first.id, false)
                    val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
                    val currentLang = if (rawLanguage == "system") java.util.Locale.getDefault().language else rawLanguage
                    val logos = detail.images?.logos
                    val bestLogo = logos?.firstOrNull { it.iso6391 == currentLang } ?: logos?.firstOrNull { it.iso6391 == "en" } ?: logos?.firstOrNull()
                    val newList = recs.toMutableList()
                    newList[0] = first.copy(genreIds = detail.genres?.map { it.id } ?: first.genreIds).apply { 
                        this.logoPath = bestLogo?.filePath 
                        this.matchScore = first.matchScore
                    }
                    newList.toImmutableList()
                } catch (e: Exception) { recs }
            } else recs
        } }

        val recTvDeferred = coroutineScope { async { 
            val recs = buildRecommendations(type = "tv", localMovies = localMovies)
            if (recs.isNotEmpty()) {
                val first = recs.first()
                try {
                    val detail = repository.getMovieDetail(first.id, true)
                    val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
                    val currentLang = if (rawLanguage == "system") java.util.Locale.getDefault().language else rawLanguage
                    val logos = detail.images?.logos
                    val bestLogo = logos?.firstOrNull { it.iso6391 == currentLang } ?: logos?.firstOrNull { it.iso6391 == "en" } ?: logos?.firstOrNull()
                    val newList = recs.toMutableList()
                    newList[0] = first.copy(genreIds = detail.genres?.map { it.id } ?: first.genreIds).apply { 
                        this.logoPath = bestLogo?.filePath 
                        this.matchScore = first.matchScore
                    }
                    newList.toImmutableList()
                } catch (e: Exception) { recs }
            } else recs
        } }

        val recMovies = recMoviesDeferred.await()
        val recTv = recTvDeferred.await()

        return coroutineScope {
            // Carica tutte le altre sezioni in parallelo
            val popMoviesDeferred = async { repository.getPopularMovies().take(10).map { it.copy(mediaType = "movie") }.toImmutableList() }
            val nowMoviesDeferred = async { repository.getNowPlayingMovies().take(10).map { it.copy(mediaType = "movie") }.toImmutableList() }
            val topMoviesDeferred = async { repository.getTop10FlickTrove(isTv = false).take(10).map { it.copy(mediaType = "movie") }.toImmutableList() }
            val upcMoviesDeferred = async { repository.getUpcomingMovies().take(10).map { it.copy(mediaType = "movie") }.toImmutableList() }
            val popTvDeferred = async { repository.getPopularTV().take(10).map { it.copy(mediaType = "tv") }.toImmutableList() }
            val nowTvDeferred = async { repository.getOnTheAirTV().take(10).map { it.copy(mediaType = "tv") }.toImmutableList() }
            val topTvDeferred = async { repository.getTop10FlickTrove(isTv = true).take(10).map { it.copy(mediaType = "tv") }.toImmutableList() }
            val upcTvDeferred = async { repository.getUpcomingTV().take(10).map { it.copy(mediaType = "tv") }.toImmutableList() }
            val trendingMoviesDeferred = async {
                val basicTrending = repository.getTrendingMovies().take(10)
                coroutineScope {
                    basicTrending.map { movie ->
                        async {
                            try {
                                val detail = repository.getMovieDetail(movie.id, false)
                                val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
                                val currentLang = if (rawLanguage == "system") java.util.Locale.getDefault().language else rawLanguage
                                val logos = detail.images?.logos
                                val bestLogo = logos?.firstOrNull { it.iso6391 == currentLang } ?: logos?.firstOrNull { it.iso6391 == "en" } ?: logos?.firstOrNull()
                                movie.copy(
                                    mediaType = "movie",
                                    genreIds = detail.genres?.map { it.id } ?: movie.genreIds
                                ).apply {
                                    this.logoPath = bestLogo?.filePath
                                }
                            } catch (e: Exception) {
                                movie.copy(mediaType = "movie")
                            }
                        }
                    }.map { it.await() }.toImmutableList()
                }
            }
            val trendingTvDeferred = async {
                val basicTrending = repository.getTrendingTV().take(10)
                coroutineScope {
                    basicTrending.map { tv ->
                        async {
                            try {
                                val detail = repository.getMovieDetail(tv.id, true)
                                val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
                                val currentLang = if (rawLanguage == "system") java.util.Locale.getDefault().language else rawLanguage
                                val logos = detail.images?.logos
                                val bestLogo = logos?.firstOrNull { it.iso6391 == currentLang } ?: logos?.firstOrNull { it.iso6391 == "en" } ?: logos?.firstOrNull()
                                tv.copy(
                                    mediaType = "tv",
                                    genreIds = detail.genres?.map { it.id } ?: tv.genreIds
                                ).apply {
                                    this.logoPath = bestLogo?.filePath
                                }
                            } catch (e: Exception) {
                                tv.copy(mediaType = "tv")
                            }
                        }
                    }.map { it.await() }.toImmutableList()
                }
            }
            val newsDeferred = async { newsRepository.getNews().take(5).toImmutableList() }
            
            val continueWatchingTvDeferred = async {
                localMovies.filter { movie ->
                    movie.mediaType == "tv" && 
                    !movie.watched && 
                    !movie.dropped && 
                    (movie.watchedEpisodes?.values?.sumOf { it.size } ?: 0) > 0
                }.sortedByDescending { it.clientUpdatedAt }.take(10).toImmutableList()
            }
            
            val becauseYouWatchedMovieDeferred = async { buildBecauseYouWatched("movie", localMovies) }
            val becauseYouWatchedTvDeferred = async { buildBecauseYouWatched("tv", localMovies) }

            FeedState(
                isLoaded = true,
                hasError = false,
                recommendedMovies = recMovies,
                popularMovies = popMoviesDeferred.await(),
                nowPlayingMovies = nowMoviesDeferred.await(),
                top10Movies = topMoviesDeferred.await(),
                upcomingMovies = upcMoviesDeferred.await(),
                recommendedTv = recTv,
                popularTv = popTvDeferred.await(),
                nowStreamingTv = nowTvDeferred.await(),
                top10Tv = topTvDeferred.await(),
                upcomingTv = upcTvDeferred.await(),
                trendingMovies = trendingMoviesDeferred.await(),
                trendingTv = trendingTvDeferred.await(),
                magazineNews = newsDeferred.await(),
                continueWatchingTv = continueWatchingTvDeferred.await(),
                becauseYouWatchedMovie = becauseYouWatchedMovieDeferred.await(),
                becauseYouWatchedTv = becauseYouWatchedTvDeferred.await()
            )
        }
    }

    private suspend fun buildRecommendations(type: String, localMovies: List<Movie>): ImmutableList<Movie> {
        val matching = if (type == "movie") {
            localMovies.filter { it.mediaType != "tv" }
        } else {
            localMovies.filter { it.mediaType == "tv" }
        }
        if (matching.isEmpty()) return persistentListOf()

        val goodCandidates = matching.filter { movie ->
            (movie.personalRating ?: 0.0) >= 7.0 ||
            (movie.watchedAt != null && (movie.voteAverage ?: 0.0) >= 7.0)
        }
        val pool = if (goodCandidates.size >= 3) goodCandidates else matching
        val seeds = pool
            .sortedWith(
                compareByDescending<Movie> { it.personalRating ?: 0.0 }
                    .thenByDescending { it.watchedAt ?: "" }
                    .thenByDescending { it.voteAverage ?: 0.0 }
            )
            .take(20)
            .shuffled()
            .take(3)

        val localCompositeIds = localMovies.map { "${it.mediaType}_${it.id}" }.toSet()
        val rawData = coroutineScope {
            seeds.map { seed ->
                async {
                    runCatching {
                        if (type == "movie") repository.getMovieRecommendations(seed.id)
                        else repository.getTVRecommendations(seed.id)
                    }.getOrDefault(emptyList())
                }
            }.awaitAll().flatten()
        }

        var results = rawData
            .distinctBy { it.id }
            .filter { movie ->
                val compositeId = "${type}_${movie.id}"
                !localCompositeIds.contains(compositeId)
            }
            .map { it.copy(mediaType = type) }
            .mapNotNull { movie ->
                val score = calculateMatchScoreUseCase(movie, matching)
                if (score == null || score >= 65) {
                    movie.apply { matchScore = score }
                } else null
            }
            
        if (results.isEmpty() && rawData.isNotEmpty()) {
            results = rawData
                .distinctBy { it.id }
                .filter { movie -> !localCompositeIds.contains("${type}_${movie.id}") }
                .map { it.copy(mediaType = type) }
                .map { movie -> 
                    val score = calculateMatchScoreUseCase(movie, matching)
                    movie.apply { matchScore = score }
                }
                .sortedByDescending { it.matchScore ?: 0 }
                .take(10)
        } else {
            results = results.sortedByDescending { it.matchScore ?: 0 }
        }
        
        return results.take(15).toImmutableList()
    }

    private suspend fun buildBecauseYouWatched(type: String, localMovies: List<Movie>): Pair<Movie, ImmutableList<Movie>>? {
        val matching = if (type == "movie") {
            localMovies.filter { it.mediaType != "tv" }
        } else {
            localMovies.filter { it.mediaType == "tv" }
        }
        if (matching.isEmpty()) return null

        val goodCandidates = matching.filter { movie ->
            (movie.personalRating ?: 0.0) >= 7.0 ||
            (movie.watchedAt != null && (movie.voteAverage ?: 0.0) >= 7.0)
        }
        
        val pool = if (goodCandidates.isNotEmpty()) goodCandidates else matching
        val seed = pool
            .sortedWith(
                compareByDescending<Movie> { it.personalRating ?: 0.0 }
                    .thenByDescending { it.clientUpdatedAt }
                    .thenByDescending { it.voteAverage ?: 0.0 }
            )
            .take(10)
            .randomOrNull() ?: return null

        val rawData = coroutineScope {
            runCatching {
                if (type == "movie") repository.getMovieRecommendations(seed.id)
                else repository.getTVRecommendations(seed.id)
            }.getOrDefault(emptyList())
        }

        val localCompositeIds = localMovies.map { "${it.mediaType}_${it.id}" }.toSet()
        
        var results = rawData
            .distinctBy { it.id }
            .filter { movie ->
                val compositeId = "${type}_${movie.id}"
                !localCompositeIds.contains(compositeId)
            }
            .map { it.copy(mediaType = type) }
            .mapNotNull { movie ->
                val score = calculateMatchScoreUseCase(movie, matching)
                if (score == null || score >= 65) {
                    movie.apply { matchScore = score }
                } else null
            }
            
        if (results.isEmpty() && rawData.isNotEmpty()) {
            results = rawData
                .distinctBy { it.id }
                .filter { movie -> !localCompositeIds.contains("${type}_${movie.id}") }
                .map { it.copy(mediaType = type) }
                .map { movie -> 
                    val score = calculateMatchScoreUseCase(movie, matching)
                    movie.apply { matchScore = score }
                }
                .sortedByDescending { it.matchScore ?: 0 }
                .take(10)
        } else {
            results = results.sortedByDescending { it.matchScore ?: 0 }
        }
        
        val finalList = results.take(15).toImmutableList()
        if (finalList.isEmpty()) return null
        return Pair(seed, finalList)
    }
}
