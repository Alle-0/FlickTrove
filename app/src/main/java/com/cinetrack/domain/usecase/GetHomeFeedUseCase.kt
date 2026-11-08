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
    suspend operator fun invoke(): FeedState = coroutineScope {
        // Carica i film salvati dall'utente per generare le raccomandazioni
        val localMovies = repository.getLocalMoviesFlow().first()

        // Lancia tutte le sezioni in parallelo all'unisono
        val recMoviesDeferred = async { 
            val (recs, seedIds) = buildRecommendations(type = "movie", localMovies = localMovies)
            if (recs.isNotEmpty()) {
                val candidateList = recs.toMutableList()
                val validRecs = mutableListOf<Movie>()
                var resolvedFirst: Movie? = null

                val movieProfile = calculateMatchScoreUseCase.buildUserProfile(localMovies.filter { it.mediaType != "tv" })
                val isShortEnthusiast = (movieProfile?.shortRatio ?: 0f) >= 0.10f

                for (candidate in candidateList) {
                    if (resolvedFirst == null) {
                        try {
                            val detail = repository.getMovieDetail(candidate.id, false)
                            // I cortometraggi (< 40 min) vengono scartati da Trove's Pick a meno che
                            // l'utente non sia un appassionato di corti (shortRatio >= 10%) E il titolo
                            // non abbia una solida reputazione internazionale (voteCount >= 500)
                            val isShort = detail.runtime != null && detail.runtime in 1..39
                            if (isShort && (!isShortEnthusiast || (detail.voteCount ?: 0) < 500)) {
                                continue
                            }
                            val rawLanguage = preferenceRepository.getContentLanguage()
                            val currentLang = if (rawLanguage == "system") java.util.Locale.getDefault().language else rawLanguage
                            val logos = detail.images?.logos
                            val bestLogo = logos?.firstOrNull { it.iso6391 == currentLang } ?: logos?.firstOrNull { it.iso6391 == "en" } ?: logos?.firstOrNull()
                            resolvedFirst = candidate.copy(
                                genreIds = detail.genres?.map { it.id } ?: candidate.genreIds,
                                runtime = detail.runtime ?: candidate.runtime
                            ).apply { 
                                this.logoPath = bestLogo?.filePath 
                                this.matchScore = candidate.matchScore
                            }
                            validRecs.add(resolvedFirst)
                        } catch (e: Exception) { 
                            validRecs.add(candidate)
                            resolvedFirst = candidate
                        }
                    } else {
                        validRecs.add(candidate)
                    }
                }
                Pair(validRecs.toImmutableList(), seedIds)
            } else Pair(recs, seedIds)
        }

        val recTvDeferred = async { 
            val (recs, seedIds) = buildRecommendations(type = "tv", localMovies = localMovies)
            if (recs.isNotEmpty()) {
                val first = recs.first()
                try {
                    val detail = repository.getMovieDetail(first.id, true)
                    val rawLanguage = preferenceRepository.getContentLanguage()
                    val currentLang = if (rawLanguage == "system") java.util.Locale.getDefault().language else rawLanguage
                    val logos = detail.images?.logos
                    val bestLogo = logos?.firstOrNull { it.iso6391 == currentLang } ?: logos?.firstOrNull { it.iso6391 == "en" } ?: logos?.firstOrNull()
                    val newList = recs.toMutableList()
                    newList[0] = first.copy(genreIds = detail.genres?.map { it.id } ?: first.genreIds).apply { 
                        this.logoPath = bestLogo?.filePath 
                        this.matchScore = first.matchScore
                    }
                    Pair(newList.toImmutableList(), seedIds)
                } catch (e: Exception) { Pair(recs, seedIds) }
            } else Pair(recs, seedIds)
        }

        val popMoviesDeferred = async { repository.getPopularMovies().take(10).map { it.copy(mediaType = "movie") }.toImmutableList() }
        val nowMoviesDeferred = async { repository.getNowPlayingMovies().take(10).map { it.copy(mediaType = "movie") }.toImmutableList() }
        val topBothDeferred = async { repository.getTop10FlickTroveBoth() }
        val topMoviesDeferred = async { 
            val (rawTopMovies, _) = topBothDeferred.await()
            val result = rawTopMovies.toMutableList()
            if (result.size < 10) {
                try {
                    val trendingFallback = repository.getTrendingMovies()
                    for (movie in trendingFallback) {
                        if (result.none { it.id == movie.id }) {
                            result.add(movie)
                            if (result.size >= 10) break
                        }
                    }
                } catch (e: Exception) {
                    // ignore fallback error
                }
            }
            result.take(10).map { it.copy(mediaType = "movie") }.toImmutableList() 
        }
        val upcMoviesDeferred = async { repository.getUpcomingMovies().take(10).map { it.copy(mediaType = "movie") }.toImmutableList() }
        val popTvDeferred = async { repository.getPopularTV().take(10).map { it.copy(mediaType = "tv") }.toImmutableList() }
        val nowTvDeferred = async { repository.getOnTheAirTV().take(10).map { it.copy(mediaType = "tv") }.toImmutableList() }
        val topTvDeferred = async { 
            val (_, rawTopTv) = topBothDeferred.await()
            val result = rawTopTv.toMutableList()
            if (result.size < 10) {
                try {
                    val trendingFallback = repository.getTrendingTV()
                    for (tv in trendingFallback) {
                        if (result.none { it.id == tv.id }) {
                            result.add(tv)
                            if (result.size >= 10) break
                        }
                    }
                } catch (e: Exception) {
                    // ignore fallback error
                }
            }
            result.take(10).map { it.copy(mediaType = "tv") }.toImmutableList() 
        }
        val upcTvDeferred = async { repository.getUpcomingTV().take(10).map { it.copy(mediaType = "tv") }.toImmutableList() }
        val trendingMoviesDeferred = async {
            val basic = repository.getTrendingMovies().take(10)
            coroutineScope {
                basic.map { movie ->
                    async {
                        val logo = repository.getMovieLogo(movie.id, isTv = false)
                        movie.copy(mediaType = "movie").apply {
                            this.logoPath = logo
                        }
                    }
                }.awaitAll().toImmutableList()
            }
        }
        val trendingTvDeferred = async {
            val basic = repository.getTrendingTV().take(10)
            coroutineScope {
                basic.map { tv ->
                    async {
                        val logo = repository.getMovieLogo(tv.id, isTv = true)
                        tv.copy(mediaType = "tv").apply {
                            this.logoPath = logo
                        }
                    }
                }.awaitAll().toImmutableList()
            }
        }
        val newsDeferred = async { newsRepository.getNews().take(5).toImmutableList() }
        
        val continueWatchingTvDeferred = async {
            localMovies.filter { movie ->
                movie.mediaType == "tv" &&
                movie.syncStatus != "pending_delete" &&
                !movie.watched &&
                !movie.dropped &&
                (movie.watchedEpisodes?.values?.sumOf { it.size } ?: 0) > 0 &&
                run {
                    val next = movie.calculateNextEpisode()
                    next != null && !next.isUpToDateWithAirDate
                }
            }.sortedByDescending { it.clientUpdatedAt }.take(10).toImmutableList()
        }
        
        // Collect seed IDs used by the main recommendation blocks to prevent
        // the same film from appearing in both "Consigliati" and "Perché hai visto"
        val usedMovieSeedIds = recMoviesDeferred.await().second
        val usedTvSeedIds = recTvDeferred.await().second
        val becauseYouWatchedMovieDeferred = async { buildBecauseYouWatched("movie", localMovies, usedMovieSeedIds) }
        val becauseYouWatchedTvDeferred = async { buildBecauseYouWatched("tv", localMovies, usedTvSeedIds) }

        FeedState(
            isLoaded = true,
            hasError = false,
            recommendedMovies = recMoviesDeferred.await().first,
            popularMovies = popMoviesDeferred.await(),
            nowPlayingMovies = nowMoviesDeferred.await(),
            top10Movies = topMoviesDeferred.await(),
            upcomingMovies = upcMoviesDeferred.await(),
            recommendedTv = recTvDeferred.await().first,
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

    // Returns (recommendations, usedSeedIds) so that buildBecauseYouWatched can avoid the same seeds
    private suspend fun buildRecommendations(type: String, localMovies: List<Movie>): Pair<ImmutableList<Movie>, Set<Long>> {
        val matching = if (type == "movie") {
            localMovies.filter { it.mediaType != "tv" }
        } else {
            localMovies.filter { it.mediaType == "tv" }
        }
        if (matching.isEmpty()) return Pair(persistentListOf(), emptySet())

        val goodCandidates = matching.filter { movie ->
            (movie.personalRating ?: 0.0) >= 7.0 ||
            (movie.watchedAt != null && (movie.voteAverage ?: 0.0) >= 7.0)
        }
        val pool = if (goodCandidates.size >= 3) goodCandidates else matching
        val topPool = pool
            .sortedWith(
                compareByDescending<Movie> { it.personalRating ?: 0.0 }
                    .thenByDescending { it.watchedAt ?: "" }
                    .thenByDescending { it.voteAverage ?: 0.0 }
            )
            .take(20)

        // Anchor-based genre diversification:
        // 1. Best-rated film is the anchor seed
        // 2. Pick a second seed with minimal genre overlap with the anchor
        // 3. Third seed is random from the remainder
        val anchor = topPool.firstOrNull()
        val second = if (anchor != null) {
            topPool.drop(1).minByOrNull { candidate ->
                (candidate.genreIds ?: emptyList()).intersect((anchor.genreIds ?: emptyList()).toSet()).size
            }
        } else null
        val seeds = listOfNotNull(anchor, second)
        val usedSeedIds = seeds.map { it.id }.toSet()

        val userProfile = calculateMatchScoreUseCase.buildUserProfile(matching)

        // Estrazione vettori chiave dal profilo per query mirate TMDB Discover
        val topGenrePairKey = userProfile?.genrePairAffinities?.maxByOrNull { it.value }?.key
        val genrePairStr = if (topGenrePairKey != null) {
            val genreA = (topGenrePairKey shr 32)
            val genreB = (topGenrePairKey and 0xFFFFFFFFL)
            "$genreA,$genreB"
        } else null

        val topForeignCountry = userProfile?.countryFrequencies?.entries
            ?.filter { it.key != "US" && it.value >= 0.18f }
            ?.maxByOrNull { it.value }?.key

        val excludedGenresList = mutableListOf<Long>()
        calculateMatchScoreUseCase.nicheFormatGenres.forEach { nicheId ->
            if ((userProfile?.genreFrequencies?.get(nicheId) ?: 0f) == 0f) {
                excludedGenresList.add(nicheId)
            }
        }
        userProfile?.negativeGenreScores?.forEach { (negId, negScore) ->
            if (negScore >= 0.5f && (userProfile.genreFrequencies[negId] ?: 0f) == 0f) {
                excludedGenresList.add(negId)
            }
        }
        val excludedGenresStr = if (excludedGenresList.isNotEmpty()) {
            excludedGenresList.distinct().joinToString(",")
        } else null

        val topDirectorId = if (type == "movie") {
            userProfile?.directorAffinities?.maxByOrNull { it.value }?.key
        } else null

        val topSingleGenre = userProfile?.genreAffinities?.maxByOrNull { it.value }?.key
        val isShortEnthusiast = (userProfile?.shortRatio ?: 0f) >= 0.10f

        val localCompositeIds = localMovies.map { it.compositeId }.toSet()

        val rawData = coroutineScope {
            // Stream 1 & 2: Seed Recommendations tradizionali (Anchor + Diversified)
            val seedDeferred = seeds.map { seed ->
                async {
                    runCatching {
                        if (type == "movie") repository.getMovieRecommendations(seed.id)
                        else repository.getTVRecommendations(seed.id)
                    }.getOrDefault(emptyList())
                }
            }

            // Stream 3: Discover Micro-Cluster & Culture (Synergy + Country)
            val discoverSynergyDeferred = async {
                runCatching {
                    val params = mutableMapOf<String, String>(
                        "vote_count.gte" to "120",
                        "vote_average.gte" to "6.8",
                        "sort_by" to "popularity.desc"
                    )
                    if (type == "movie" && !isShortEnthusiast) {
                        params["with_runtime.gte"] = "40"
                    }
                    genrePairStr?.let { params["with_genres"] = it }
                    excludedGenresStr?.let { params["without_genres"] = it }
                    topForeignCountry?.let { params["with_origin_country"] = it }

                    if (type == "movie") repository.discoverMoviesWithParams(options = params)
                    else repository.discoverTVWithParams(options = params)
                }.getOrDefault(emptyList())
            }

            // Stream 4: Auteur Boost o Indie Gem Discover
            val discoverAuteurOrIndieDeferred = async {
                runCatching {
                    if (type == "movie" && topDirectorId != null) {
                        val params = mutableMapOf<String, String>(
                            "with_crew" to topDirectorId.toString(),
                            "vote_count.gte" to "50",
                            "sort_by" to "vote_average.desc"
                        )
                        if (!isShortEnthusiast) {
                            params["with_runtime.gte"] = "40"
                        }
                        excludedGenresStr?.let { params["without_genres"] = it }
                        repository.discoverMoviesWithParams(options = params).onEach { movie ->
                            movie.directorId = topDirectorId
                        }
                    } else if (topSingleGenre != null) {
                        val params = mutableMapOf<String, String>(
                            "with_genres" to topSingleGenre.toString(),
                            "vote_count.gte" to if (isShortEnthusiast) "250" else "80",
                            "vote_count.lte" to "3500",
                            "vote_average.gte" to "7.3",
                            "sort_by" to "vote_average.desc"
                        )
                        if (type == "movie" && !isShortEnthusiast) {
                            params["with_runtime.gte"] = "40"
                        }
                        excludedGenresStr?.let { params["without_genres"] = it }
                        if (type == "movie") repository.discoverMoviesWithParams(options = params)
                        else repository.discoverTVWithParams(options = params)
                    } else {
                        emptyList()
                    }
                }.getOrDefault(emptyList())
            }

            val seedResults = seedDeferred.awaitAll().flatten()
            val synergyResults = discoverSynergyDeferred.await()
            val auteurOrIndieResults = discoverAuteurOrIndieDeferred.await()

            seedResults + synergyResults + auteurOrIndieResults
        }

        // Quality gate: filter out low-quality or posterless results
        val qualityData = rawData.filter { movie ->
            (movie.voteCount ?: 0) >= 50 &&
            (movie.voteAverage ?: 0.0) >= 5.5 &&
            movie.posterPath != null
        }

        var results = qualityData
            .distinctBy { it.id }
            .filter { movie -> !localCompositeIds.contains("${type}_${movie.id}") }
            .map { it.copy(mediaType = type) }
            .mapNotNull { movie ->
                val score = calculateMatchScoreUseCase.calculateScore(movie, userProfile)
                if (score == null || score >= 70) {
                    movie.apply { matchScore = score }
                } else null
            }

        val sortedResults = if (results.isEmpty() && qualityData.isNotEmpty()) {
            // Graceful degradation: try 55% threshold before full fallback
            val degraded = qualityData
                .distinctBy { it.id }
                .filter { movie -> !localCompositeIds.contains("${type}_${movie.id}") }
                .map { it.copy(mediaType = type) }
                .map { movie ->
                    val score = calculateMatchScoreUseCase.calculateScore(movie, userProfile)
                    movie.apply { matchScore = score }
                }
                .filter { it.matchScore == null || (it.matchScore ?: 0) >= 55 }
                .sortedByDescending { it.matchScore ?: 0 }
                .take(10)

            if (degraded.isEmpty()) {
                qualityData
                    .distinctBy { it.id }
                    .filter { movie -> !localCompositeIds.contains("${type}_${movie.id}") }
                    .map { it.copy(mediaType = type) }
                    .map { movie ->
                        val score = calculateMatchScoreUseCase.calculateScore(movie, userProfile)
                        movie.apply { matchScore = score }
                    }
                    .sortedByDescending { it.matchScore ?: 0 }
                    .take(10)
            } else {
                degraded
            }
        } else {
            results.sortedWith(
                compareByDescending<Movie> { it.matchScore ?: 0 }
                    .thenBy { it.id }
            )
        }

        val finalOrdered = if (sortedResults.isNotEmpty()) {
            val maxScore = sortedResults.first().matchScore ?: 0
            // Rotazione giornaliera deterministica del Trove's Pick tra i titoli top-tier
            val topCandidates = sortedResults
                .filter { (it.matchScore ?: 0) >= (maxScore - 6).coerceAtLeast(78) }
                .take(5)
                .ifEmpty { listOf(sortedResults.first()) }

            val epochDay = java.time.LocalDate.now().toEpochDay()
            val pickIndex = (Math.floorMod(epochDay, topCandidates.size.toLong())).toInt()
            val pick = topCandidates.getOrNull(pickIndex) ?: sortedResults.first()

            // Il Trove's Pick viene posizionato al primo posto e rimosso dalla coda per evitare duplicati nella riga consigliati
            listOf(pick) + sortedResults.filter { it.id != pick.id }
        } else {
            sortedResults
        }

        return Pair(finalOrdered.take(15).toImmutableList(), usedSeedIds)
    }

    private suspend fun buildBecauseYouWatched(type: String, localMovies: List<Movie>, excludeIds: Set<Long> = emptySet()): Pair<Movie, ImmutableList<Movie>>? {
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

        // Exclude seeds already used by buildRecommendations to avoid cross-section duplicates
        val pool = (if (goodCandidates.isNotEmpty()) goodCandidates else matching)
            .filter { it.id !in excludeIds }
            .ifEmpty { (if (goodCandidates.isNotEmpty()) goodCandidates else matching) } // fallback if all excluded

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

        // Quality gate: filter out low-quality or posterless results
        val qualityData = rawData.filter { movie ->
            (movie.voteCount ?: 0) >= 50 &&
            (movie.voteAverage ?: 0.0) >= 5.5 &&
            movie.posterPath != null
        }

        val localCompositeIds = localMovies.map { it.compositeId }.toSet()
        val userProfile = calculateMatchScoreUseCase.buildUserProfile(matching)

        var results = qualityData
            .distinctBy { it.id }
            .filter { movie -> !localCompositeIds.contains("${type}_${movie.id}") }
            .map { it.copy(mediaType = type) }
            .mapNotNull { movie ->
                val score = calculateMatchScoreUseCase.calculateScore(movie, userProfile)
                if (score == null || score >= 65) {
                    movie.apply { matchScore = score }
                } else null
            }

        if (results.isEmpty() && qualityData.isNotEmpty()) {
            // Graceful degradation: 50% threshold before full fallback
            results = qualityData
                .distinctBy { it.id }
                .filter { movie -> !localCompositeIds.contains("${type}_${movie.id}") }
                .map { it.copy(mediaType = type) }
                .map { movie ->
                    val score = calculateMatchScoreUseCase.calculateScore(movie, userProfile)
                    movie.apply { matchScore = score }
                }
                .filter { it.matchScore == null || (it.matchScore ?: 0) >= 50 }
                .sortedByDescending { it.matchScore ?: 0 }
                .take(10)
        }

        if (results.isEmpty() && qualityData.isNotEmpty()) {
            results = qualityData
                .distinctBy { it.id }
                .filter { movie -> !localCompositeIds.contains("${type}_${movie.id}") }
                .map { it.copy(mediaType = type) }
                .map { movie ->
                    val score = calculateMatchScoreUseCase.calculateScore(movie, userProfile)
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
