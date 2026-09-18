package com.cinetrack.domain

import com.cinetrack.data.model.Movie
import javax.inject.Inject

data class UserGenreProfile(
    val genreAffinities: Map<Long, Float>,
    val maxAffinity: Float,
    val genreCounts: Map<Long, Int> = emptyMap()
) {
    fun getGenreCount(genreId: Long): Int = genreCounts[genreId] ?: 0
}

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

        // 2. Calcoliamo l'AFFINITÀ MEDIA per genere con fattore di confidenza / shrinkage
        // (Un genere con 1 solo film visto subisce una penalità di incertezza rispetto a generi consolidati con molti titoli)
        val genreAffinities = genreScores.mapValues { (id, totalScore) ->
            val count = genreCounts[id] ?: 1
            val rawAvg = totalScore / count
            // Curva di saturazione: count / (count + 3f) -> 1 film = 25% confidenza, 3 film = 50%, 10 film = 77%
            val confidence = count.toFloat() / (count + 3f)
            rawAvg * confidence
        }

        val maxAffinity = genreAffinities.values.maxOrNull()?.coerceAtLeast(0.1f) ?: 1f
        return UserGenreProfile(genreAffinities, maxAffinity, genreCounts)
    }

    fun calculateScore(currentMovie: Movie, profile: UserGenreProfile?): Int? {
        // 1. Bypass Voto Personale: se l'utente ha già votato il film, la sua valutazione è la verità assoluta (fino a 99%)
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

        // 3. Bilanciamento 50/50 con Smorzamento Bayesiano sul voto TMDB
        // (Protegge da titoli con 2-3 voti spinti artificialmente a 9.7 o 10.0)
        val rawTmdbRating = currentMovie.voteAverage ?: 0.0
        val voteCount = currentMovie.voteCount ?: 0

        val effectiveRating = if (voteCount > 0 && rawTmdbRating > 0.0) {
            val priorVotes = 50.0 // Soglia di confidenza minima
            val priorMean = 6.5   // Media globale TMDB neutra
            ((voteCount * rawTmdbRating) + (priorVotes * priorMean)) / (voteCount + priorVotes)
        } else if (rawTmdbRating > 0.0) {
            (rawTmdbRating + (6.5 * 3.0)) / 4.0
        } else {
            0.0
        }

        val baseScore = ((effectiveRating / 8.5) * 50.0).toFloat().coerceIn(0f, 50f)
        val finalScore = (baseScore + matchBonus).toInt()

        // 4. Cap Predittivo al 95%: le predizioni non votate non superano il 95% (96%-99% riservati a voti personali 10/10)
        return finalScore.coerceIn(10, 95)
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