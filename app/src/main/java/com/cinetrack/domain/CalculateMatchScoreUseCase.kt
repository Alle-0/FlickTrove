package com.cinetrack.domain

import com.cinetrack.data.model.Movie
import javax.inject.Inject

data class UserGenreProfile(
    val genreAffinities: Map<Long, Float>,
    val maxAffinity: Float,
    val genreCounts: Map<Long, Int> = emptyMap(),
    val decadeCounts: Map<Int, Int> = emptyMap(),
    val totalWatchedCount: Int = 0
) {
    fun getGenreCount(genreId: Long): Int = genreCounts[genreId] ?: 0
    fun getDecadeCount(decade: Int): Int = decadeCounts[decade] ?: 0
    val animationRatio: Float
        get() = if (totalWatchedCount > 0) (genreCounts[16L] ?: 0).toFloat() / totalWatchedCount else 0f
}

class CalculateMatchScoreUseCase @Inject constructor() {

    fun buildUserProfile(localMovies: List<Movie>): UserGenreProfile? {
        val watchedMovies = localMovies.filter { it.watched || it.favorite }
        if (watchedMovies.size < 3) return null

        val genreScores = mutableMapOf<Long, Float>()
        val genreCounts = mutableMapOf<Long, Int>()
        val decadeCounts = mutableMapOf<Int, Int>()

        // 1. Calcoliamo i pesi con penalità per i film brutti e tracciamo le decadi
        watchedMovies.forEach { m ->
            val rating = m.personalRating
            val ratingWeight = when {
                rating != null && rating > 0 -> (rating.toFloat() - 6f) / 4f
                m.favorite -> 1.0f
                else -> 0.2f
            }

            // Fallback su genreIds: nei film importati/salvati in Room m.genres è spesso null
            val movieGenreIds = m.genres?.map { it.id.toLong() }
                ?: m.genreIds?.map { it.toLong() }
                ?: emptyList()

            movieGenreIds.distinct().forEach { id ->
                genreScores[id] = (genreScores[id] ?: 0f) + ratingWeight
                genreCounts[id] = (genreCounts[id] ?: 0) + 1
            }

            // Tracciamento decadi di visione
            extractYear(m)?.let { year ->
                val decade = (year / 10) * 10
                decadeCounts[decade] = (decadeCounts[decade] ?: 0) + 1
            }
        }

        if (genreScores.isEmpty()) return null

        // 2. Calcoliamo l'AFFINITÀ MEDIA per genere con fattore di confidenza / shrinkage
        val genreAffinities = genreScores.mapValues { (id, totalScore) ->
            val count = genreCounts[id] ?: 1
            val rawAvg = totalScore / count
            // Curva di saturazione: count / (count + 3f) -> 1 film = 25% confidenza, 3 film = 50%, 10 film = 77%
            val confidence = count.toFloat() / (count + 3f)
            rawAvg * confidence
        }

        val maxAffinity = genreAffinities.values.maxOrNull()?.coerceAtLeast(0.1f) ?: 1f
        return UserGenreProfile(
            genreAffinities = genreAffinities,
            maxAffinity = maxAffinity,
            genreCounts = genreCounts,
            decadeCounts = decadeCounts,
            totalWatchedCount = watchedMovies.size
        )
    }

    fun calculateScore(currentMovie: Movie, profile: UserGenreProfile?): Int? {
        // 1. Bypass Voto Personale: se l'utente ha già votato il film, la sua valutazione è la verità assoluta (fino a 99%)
        val personalRating = currentMovie.personalRating
        if (personalRating != null && personalRating > 0) {
            val personalScore = (personalRating * 10f).toInt()
            return personalScore.coerceIn(10, 99)
        }

        if (profile == null) return null

        // 2. Anti-Diluizione: Blend ponderato tra Max Affinity (70%) e Average Affinity (30%)
        var matchBonus = 0f
        val currentMovieGenreIds = currentMovie.genres?.map { it.id.toLong() }
            ?: currentMovie.genreIds?.map { it.toLong() }
            ?: emptyList()

        if (currentMovieGenreIds.isNotEmpty()) {
            val genreAffinities = currentMovieGenreIds.map { id ->
                profile.genreAffinities[id] ?: 0f
            }
            val maxGenreAffinity = genreAffinities.maxOrNull() ?: 0f
            val avgGenreAffinity = if (genreAffinities.isNotEmpty()) genreAffinities.average().toFloat() else 0f
            val blendedAffinity = (maxGenreAffinity * 0.7f) + (avgGenreAffinity * 0.3f)

            if (blendedAffinity > 0f) {
                matchBonus = ((blendedAffinity / profile.maxAffinity) * 50f).coerceIn(0f, 50f)
            }
        }

        // 3. Penalità di Formato per Animazione (TMDB ID 16L):
        // Se l'utente ha una libreria consolidata (>= 10 titoli) e non guarda animazione (ratio < 4%),
        // applichiamo un forte damping (-35 punti) per evitare che i punteggi alti di TMDB monopolizzino i consigli
        val isAnimation = currentMovieGenreIds.contains(16L)
        val formatPenalty = if (isAnimation && profile.totalWatchedCount >= 10 && profile.animationRatio < 0.04f) {
            35f
        } else 0f

        // 4. Modulazione Decadi / Epoca (Decade Affinity):
        // Piccolo bonus (+1.5 a +5 punti) per i titoli nelle decadi preferite dell'utente
        // e leggero damping (-2 a -4 punti) solo se molto distante da qualsiasi decade mai vista
        var eraBonus = 0f
        val candidateYear = extractYear(currentMovie)
        val totalWithDecade = profile.decadeCounts.values.sum()
        if (candidateYear != null && totalWithDecade >= 5) {
            val candidateDecade = (candidateYear / 10) * 10
            val countInDecade = profile.getDecadeCount(candidateDecade)
            val decadeRatio = countInDecade.toFloat() / totalWithDecade
            val minDecadeDiff = if (profile.decadeCounts.isNotEmpty()) {
                profile.decadeCounts.keys.minOfOrNull { kotlin.math.abs(it - candidateDecade) } ?: 0
            } else 0

            eraBonus = when {
                decadeRatio >= 0.25f -> 5f
                decadeRatio >= 0.10f -> 3f
                decadeRatio > 0f -> 1.5f
                minDecadeDiff >= 30 && profile.totalWatchedCount >= 15 -> -4f
                minDecadeDiff >= 20 && profile.totalWatchedCount >= 15 -> -2f
                else -> 0f
            }
        }

        // 5. Bilanciamento 50/50 con Smorzamento Bayesiano sul voto TMDB
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
        val finalScore = ((baseScore + matchBonus + eraBonus) - formatPenalty).toInt()

        // 6. Cap Predittivo al 95%: le predizioni non votate non superano il 95% (96%-99% riservati a voti personali 10/10)
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

    private fun extractYear(movie: Movie): Int? {
        val yearStr = movie.releaseYear
            ?: movie.releaseDate?.take(4)
            ?: movie.firstAirDate?.take(4)
        return yearStr?.toIntOrNull()?.takeIf { it in 1880..2100 }
    }
}