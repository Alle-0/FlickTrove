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
        if (watchedMovies.size < 3) return null

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
        // 1. Bypass Voto Personale: se l'utente ha già votato il film, la sua valutazione è la verità assoluta
        val personalRating = currentMovie.personalRating
        if (personalRating != null && personalRating > 0) {
            val personalScore = (personalRating * 10f).toInt()
            return personalScore.coerceIn(10, 99)
        }

        if (profile == null) return null

        // 2. Anti-Diluizione: Max Affinity invece della media aritmetica sui generi
        var matchBonus = 0f
        val currentMovieGenreIds = currentMovie.genres?.map { it.id.toLong() }
            ?: currentMovie.genreIds?.map { it.toLong() }
            ?: emptyList()

        if (currentMovieGenreIds.isNotEmpty()) {
            val maxGenreAffinity = currentMovieGenreIds.maxOfOrNull { id ->
                profile.genreAffinities[id] ?: 0f
            } ?: 0f

            if (maxGenreAffinity > 0f) {
                matchBonus = ((maxGenreAffinity / profile.maxAffinity) * 50f).coerceIn(0f, 50f)
            }
        }

        // 3. Bilanciamento 50/50: 50 punti da TMDB (normalizzato su 8.5 max) + 50 punti dall'affinità personale
        val tmdbRating = currentMovie.voteAverage ?: 0.0
        val baseScore = ((tmdbRating / 8.5f) * 50f).toFloat().coerceAtMost(50f)
        val finalScore = (baseScore + matchBonus).toInt()
        return finalScore.coerceIn(10, 99)
    }

    operator fun invoke(currentMovie: Movie, localMovies: List<Movie>): Int? {
        val personalRating = currentMovie.personalRating
        if (personalRating != null && personalRating > 0) {
            return (personalRating * 10f).toInt().coerceIn(10, 99)
        }
        val profile = buildUserProfile(localMovies) ?: return null
        return calculateScore(currentMovie, profile)
    }
}