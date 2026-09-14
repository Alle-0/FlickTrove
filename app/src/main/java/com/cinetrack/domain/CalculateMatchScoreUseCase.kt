package com.cinetrack.domain

import com.cinetrack.data.model.Movie
import javax.inject.Inject

data class UserGenreProfile(
    val genreAffinities: Map<Long, Float>,
    val maxAffinity: Float
)

class CalculateMatchScoreUseCase @Inject constructor() {

    fun buildUserProfile(localMovies: List<Movie>): UserGenreProfile? {
        val watchedMovies = localMovies.filter { it.watched || it.favorite }
        if (watchedMovies.size < 5) return null

        val genreScores = mutableMapOf<Long, Float>()
        val genreCounts = mutableMapOf<Long, Int>()

        // 1. Calcoliamo i pesi con penalità per i film brutti
        watchedMovies.forEach { m ->
            val rating = m.personalRating
            val ratingWeight = when {
                rating != null && rating > 0 -> (rating.toFloat() - 6f) / 4f
                m.favorite -> 1.0f
                else -> 0.2f
            }

            m.genres?.forEach { g ->
                val id = g.id.toLong()
                genreScores[id] = (genreScores[id] ?: 0f) + ratingWeight
                genreCounts[id] = (genreCounts[id] ?: 0) + 1
            }
        }

        if (genreScores.isEmpty()) return null

        // 2. Calcoliamo l'AFFINITÀ MEDIA per genere
        val genreAffinities = genreScores.mapValues { (id, totalScore) ->
            totalScore / (genreCounts[id] ?: 1)
        }

        val maxAffinity = genreAffinities.values.maxOrNull()?.coerceAtLeast(0.1f) ?: 1f
        return UserGenreProfile(genreAffinities, maxAffinity)
    }

    fun calculateScore(currentMovie: Movie, profile: UserGenreProfile?): Int? {
        if (profile == null) return null

        var matchBonus = 0f
        val currentMovieGenreIds = currentMovie.genres?.map { it.id.toLong() }
            ?: currentMovie.genreIds?.map { it.toLong() }
            ?: emptyList()

        if (currentMovieGenreIds.isNotEmpty()) {
            val avgAffinity = currentMovieGenreIds.sumOf { id ->
                (profile.genreAffinities[id] ?: 0f).toDouble()
            } / currentMovieGenreIds.size

            matchBonus = ((avgAffinity / profile.maxAffinity) * 40f).toFloat()
        }

        val tmdbRating = currentMovie.voteAverage ?: 0.0
        val baseScore = 40f + (tmdbRating / 10f) * 20f
        val finalScore = (baseScore + matchBonus).toInt()
        return finalScore.coerceIn(10, 99)
    }

    operator fun invoke(currentMovie: Movie, localMovies: List<Movie>): Int? {
        val profile = buildUserProfile(localMovies) ?: return null
        return calculateScore(currentMovie, profile)
    }
}