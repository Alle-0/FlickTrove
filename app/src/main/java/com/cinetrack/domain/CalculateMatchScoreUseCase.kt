package com.cinetrack.domain

import com.cinetrack.data.model.Movie
import javax.inject.Inject

class CalculateMatchScoreUseCase @Inject constructor() {
    operator fun invoke(currentMovie: Movie, localMovies: List<Movie>): Int? {
        val watchedMovies = localMovies.filter { it.watched || it.favorite }
        if (watchedMovies.size < 5) return null

        val genreScores = mutableMapOf<Long, Float>()
        val genreCounts = mutableMapOf<Long, Int>()

        // 1. Calcoliamo i pesi con penalità per i film brutti
        watchedMovies.forEach { m ->
            val rating = m.personalRating // <--- ASSEGNAZIONE LOCALE (IMMUTABILE)

            val ratingWeight = when {
                rating != null && rating > 0 -> {
                    // Usa la variabile locale 'rating' al posto di 'm.personalRating'
                    (rating.toFloat() - 6f) / 4f
                }
                m.favorite -> 1.0f // Un preferito vale come un bel 10/10
                else -> 0.2f // Solo visto (senza voto/non preferito) indica un leggero interesse
            }

            m.genres?.forEach { g ->
                val id = g.id.toLong()
                genreScores[id] = (genreScores[id] ?: 0f) + ratingWeight
                genreCounts[id] = (genreCounts[id] ?: 0) + 1
            }
        }

        // 2. Calcoliamo l'AFFINITÀ MEDIA per genere (risolve il problema quantità vs qualità)
        val genreAffinities = genreScores.mapValues { (id, totalScore) ->
            totalScore / genreCounts[id]!! 
        }

        val maxAffinity = genreAffinities.values.maxOrNull()?.coerceAtLeast(0.1f) ?: 1f
        var matchBonus = 0f
        
        // RECUPERIAMO GLI ID DA ENTRAMBE LE FONTI (genres oppure genreIds)
        val currentMovieGenreIds = currentMovie.genres?.map { it.id.toLong() }
            ?: currentMovie.genreIds?.map { it.toLong() } // Sostituisci "genreIds" con il nome esatto della tua variabile in Movie.kt!
            ?: emptyList()

        if (currentMovieGenreIds.isNotEmpty()) {
            val avgAffinity = currentMovieGenreIds.sumOf { id ->
                (genreAffinities[id] ?: 0f).toDouble()
            } / currentMovieGenreIds.size
            
            matchBonus = ((avgAffinity / maxAffinity) * 40f).toFloat() 
        }

        // 3. Ribilanciamo TMDb e il pavimento
        val tmdbRating = currentMovie.voteAverage ?: 0.0
        // Il base score ora va da 40 (TMDb = 0) a 60 (TMDb = 10)
        val baseScore = 40f + (tmdbRating / 10f) * 20f 

        // Punteggio finale: max 100% (60 da TMDb + 40 da affinità personale)
        val finalScore = (baseScore + matchBonus).toInt()
        
        // Coerce tra 10% e 99%. 
        // Se un utente odia i generi, un match del 30-40% rende l'idea di "Evitalo!"
        return finalScore.coerceIn(10, 99)
    }
}