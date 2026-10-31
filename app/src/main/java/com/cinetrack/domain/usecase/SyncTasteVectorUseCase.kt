package com.cinetrack.domain.usecase

import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.TasteVector
import com.cinetrack.data.remote.FirebaseRemoteDataSource
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.domain.CalculateMatchScoreUseCase
import com.cinetrack.domain.UserGenreProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lazy Sync UseCase for [TasteVector].
 *
 * Rules & Invariants:
 * 1. Zero-Waste Spark Quota: Updates at most ONCE every 30 days, only upon active app open / sync.
 * 2. Multi-Media Fidelity: Distills separate movieGenres and tvGenres matrices from [UserGenreProfile]
 *    to preserve distinct cinematic vs TV habits.
 * 3. 360° Matchmaking Attributes:
 *    - All relevant movie and TV genres + disliked/repulsive genres
 *    - Top Decades distribution (e.g. 1980s, 1990s, 2020s)
 *    - Top 5 beloved Directors & Top 5 Actors
 *    - Cultural / Origin Country affinities
 *    - Movie vs TV ratio and Animation ratio
 *    - Arthouse / Indie Seeker score and Average user rating
 * 4. Critical Mass Threshold: Requires >= 5 completed/rated items to prevent premature profile skew.
 * 5. Execution off the UI thread: Computes profiles on [Dispatchers.Default] and performs cloud write on [Dispatchers.IO].
 */
@Singleton
class SyncTasteVectorUseCase @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val preferenceRepository: PreferenceRepository,
    private val matchScoreUseCase: CalculateMatchScoreUseCase,
    private val firebaseRemoteDataSource: FirebaseRemoteDataSource
) {
    companion object {
        private const val SYNC_INTERVAL_DAYS = 30L
        private const val MIN_LIBRARY_SIZE = 5
        private const val MIN_GENRE_FREQUENCY = 0.03f
        private const val MIN_COUNTRY_FREQUENCY = 0.05f
        private const val TOP_GENRES_LIMIT = 8
        private const val TOP_COUNTRIES_LIMIT = 4
        private const val TOP_DIRECTORS_LIMIT = 5
        private const val TOP_ACTORS_LIMIT = 5
        private const val TOP_DECADES_LIMIT = 4
    }

    suspend operator fun invoke(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastSync = preferenceRepository.getLastTasteVectorSyncTimestamp()
        val thirtyDaysMs = TimeUnit.DAYS.toMillis(SYNC_INTERVAL_DAYS)

        if (!force && (now - lastSync < thirtyDaysMs)) {
            return@withContext false
        }

        val allMovies = favoriteDao.getAll()
        val completedCount = allMovies.count { it.watched || (it.personalRating ?: 0.0) > 0.0 }
        if (completedCount < MIN_LIBRARY_SIZE) {
            return@withContext false
        }

        val tasteVector = withContext(Dispatchers.Default) {
            buildTasteVector(allMovies, completedCount, now)
        } ?: return@withContext false

        val saved = firebaseRemoteDataSource.saveTasteVector(tasteVector)
        if (saved) {
            preferenceRepository.updateLastTasteVectorSyncTimestamp(now)
            android.util.Log.d("SyncTasteVectorUseCase", "360° TasteVector successfully updated to Firestore for social match.")
        }
        saved
    }

    private fun buildTasteVector(
        allMovies: List<Movie>,
        totalWatched: Int,
        timestamp: Long
    ): TasteVector? {
        val globalProfile = matchScoreUseCase.buildUserProfile(allMovies) ?: return null

        val movieItems = allMovies.filter { it.mediaType != "tv" }
        val tvItems = allMovies.filter { it.mediaType == "tv" }

        val movieProfile = if (movieItems.size >= 3) {
            matchScoreUseCase.buildUserProfile(movieItems) ?: globalProfile
        } else {
            globalProfile
        }

        val tvProfile = if (tvItems.size >= 3) {
            matchScoreUseCase.buildUserProfile(tvItems) ?: globalProfile
        } else {
            globalProfile
        }

        val movieGenres = distillGenres(movieProfile.genreFrequencies)
        val tvGenres = distillGenres(tvProfile.genreFrequencies)
        val topCountries = distillCountries(globalProfile.countryFrequencies)

        val dislikedGenres = globalProfile.negativeGenreScores
            .filterValues { it >= 1.5f }
            .keys
            .map { it.toString() }
            .take(5)

        val totalDecades = globalProfile.decadeCounts.values.sum().coerceAtLeast(1)
        val topDecades = globalProfile.decadeCounts
            .toList()
            .sortedByDescending { it.second }
            .take(TOP_DECADES_LIMIT)
            .associate { (decade, count) ->
                val freq = count.toFloat() / totalDecades
                decade.toString() to ((freq * 100).toInt() / 100f)
            }

        val topDirectors = globalProfile.directorAffinities
            .toList()
            .sortedByDescending { it.second }
            .take(TOP_DIRECTORS_LIMIT)
            .map { it.first }

        val topActors = globalProfile.actorAffinities
            .toList()
            .sortedByDescending { it.second }
            .take(TOP_ACTORS_LIMIT)
            .map { it.first }

        val completedMoviesCount = movieItems.count { it.watched || (it.personalRating ?: 0.0) > 0.0 }
        val movieRatio = if (totalWatched > 0) {
            ((completedMoviesCount.toFloat() / totalWatched) * 100).toInt() / 100f
        } else 0.5f

        val animationRatio = ((globalProfile.animationRatio * 100).toInt() / 100f)

        return TasteVector(
            movieGenres = movieGenres,
            tvGenres = tvGenres,
            dislikedGenres = dislikedGenres,
            topDecades = topDecades,
            topCountries = topCountries,
            topDirectors = topDirectors,
            topActors = topActors,
            movieRatio = movieRatio,
            avgRating = ((globalProfile.averageUserRating * 10).toInt() / 10f),
            isIndieSeeker = globalProfile.isIndieSeeker,
            animationRatio = animationRatio,
            totalWatched = totalWatched,
            updatedAt = timestamp
        )
    }

    private fun distillGenres(frequencies: Map<Long, Float>): Map<String, Float> {
        return frequencies
            .filterValues { it >= MIN_GENRE_FREQUENCY }
            .toList()
            .sortedByDescending { it.second }
            .take(TOP_GENRES_LIMIT)
            .associate { (genreId, freq) -> genreId.toString() to ((freq * 100).toInt() / 100f) }
    }

    private fun distillCountries(frequencies: Map<String, Float>): Map<String, Float> {
        return frequencies
            .filterValues { it >= MIN_COUNTRY_FREQUENCY }
            .toList()
            .sortedByDescending { it.second }
            .take(TOP_COUNTRIES_LIMIT)
            .associate { (countryCode, freq) -> countryCode to ((freq * 100).toInt() / 100f) }
    }
}
