package com.cinetrack.domain

import com.cinetrack.data.Movie
import javax.inject.Inject

class CalculateMatchScoreUseCase @Inject constructor() {
    operator fun invoke(currentMovie: Movie, localMovies: List<Movie>): Int? {
        val watchedMovies = localMovies.filter { it.watched || it.favorite }
        if (watchedMovies.size < 5) return null
        
        val genreAffinities = mutableMapOf<Long, Float>()
        watchedMovies.forEach { m ->
            val rating = m.personalRating
            val ratingWeight = if (rating != null && rating > 0) {
                (rating.toFloat() / 10f) * 2f
            } else if (m.favorite) 1.5f else 1.0f
            
            m.genres?.forEach { g ->
                val id = g.id.toLong()
                genreAffinities[id] = (genreAffinities[id] ?: 0f) + ratingWeight
            }
        }
        
        val maxAffinity = genreAffinities.values.maxOrNull() ?: 1f
        var matchBonus = 0f
        val currentMovieGenres = currentMovie.genres ?: emptyList()
        if (currentMovieGenres.isNotEmpty()) {
            val avgAffinity = currentMovieGenres.sumOf { g ->
                (genreAffinities[g.id.toLong()] ?: 0f).toDouble()
            } / currentMovieGenres.size
            matchBonus = ((avgAffinity / maxAffinity) * 25f).toFloat() 
        }
        
        val tmdbRating = currentMovie.voteAverage ?: 0.0
        val baseScore = 60f + (tmdbRating / 10f) * 14f
        
        val finalScore = (baseScore + matchBonus).toInt()
        return finalScore.coerceIn(50, 99)
    }
}
