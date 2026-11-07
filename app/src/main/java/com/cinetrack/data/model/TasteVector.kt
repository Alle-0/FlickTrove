package com.cinetrack.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Rich 360° representation of a user's cinematic and TV taste profile.
 * Distilled from [com.cinetrack.domain.UserGenreProfile] and synchronized lazily (at most once every 30 days,
 * only upon app launch / sync) to Firestore for social / Cine-Match capabilities.
 *
 * Payload: ~1.2 KB (well below Firestore 1 MB document limits, exactly 1 monthly write).
 */
@Keep
@Serializable
data class TasteVector(
    @get:PropertyName("movie_genres")
    @set:PropertyName("movie_genres")
    @SerialName("movie_genres")
    var movieGenres: Map<String, Float> = emptyMap(),

    @get:PropertyName("tv_genres")
    @set:PropertyName("tv_genres")
    @SerialName("tv_genres")
    var tvGenres: Map<String, Float> = emptyMap(),

    @get:PropertyName("top_genre_pairs")
    @set:PropertyName("top_genre_pairs")
    @SerialName("top_genre_pairs")
    var topGenrePairs: List<String> = emptyList(),

    @get:PropertyName("disliked_genres")
    @set:PropertyName("disliked_genres")
    @SerialName("disliked_genres")
    var dislikedGenres: List<String> = emptyList(),

    @get:PropertyName("top_decades")
    @set:PropertyName("top_decades")
    @SerialName("top_decades")
    var topDecades: Map<String, Float> = emptyMap(),

    @get:PropertyName("top_countries")
    @set:PropertyName("top_countries")
    @SerialName("top_countries")
    var topCountries: Map<String, Float> = emptyMap(),

    @get:PropertyName("top_directors")
    @set:PropertyName("top_directors")
    @SerialName("top_directors")
    var topDirectors: List<Long> = emptyList(),

    @get:PropertyName("top_actors")
    @set:PropertyName("top_actors")
    @SerialName("top_actors")
    var topActors: List<Long> = emptyList(),

    @get:PropertyName("movie_ratio")
    @set:PropertyName("movie_ratio")
    @SerialName("movie_ratio")
    var movieRatio: Float = 0.5f,

    @get:PropertyName("avg_rating")
    @set:PropertyName("avg_rating")
    @SerialName("avg_rating")
    var avgRating: Float = 0f,

    @get:PropertyName("is_indie_seeker")
    @set:PropertyName("is_indie_seeker")
    @SerialName("is_indie_seeker")
    var isIndieSeeker: Boolean = false,

    @get:PropertyName("animation_ratio")
    @set:PropertyName("animation_ratio")
    @SerialName("animation_ratio")
    var animationRatio: Float = 0f,

    @get:PropertyName("total_watched")
    @set:PropertyName("total_watched")
    @SerialName("total_watched")
    var totalWatched: Int = 0,

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    @SerialName("updated_at")
    var updatedAt: Long = 0L
) {
    /**
     * Calculates the Cine-Match compatibility score (10% to 99%) between this profile and [other].
     *
     * Weighted Dimensions:
     * 1. Multi-Media Cosine Similarity (Movie & TV normalized genres): up to 45 pts
     * 2. Repulsion / Detested Genre Collision: penalty up to -15 pts
     * 3. Synergy Combinatorial Pairs Overlap: up to +10 pts
     * 4. Era / Decades Alignment: up to +10 pts
     * 5. Auteur & Cast Overlap (Directors + Actors): up to +15 pts
     * 6. Persona Nuance (Indie seeker, Animation, Critical Rating): up to +15 pts
     */
    fun calculateCompatibility(other: TasteVector): Int {
        if (totalWatched < 3 || other.totalWatched < 3) return 50

        // 1. Cosine similarity on movie and TV genres (up to 45 pts)
        val movieCos = computeCosineSimilarity(movieGenres, other.movieGenres)
        val tvCos = computeCosineSimilarity(tvGenres, other.tvGenres)
        val avgMovieRatio = ((movieRatio + other.movieRatio) / 2f).coerceIn(0f, 1f)
        val blendedGenreCos = (movieCos * avgMovieRatio) + (tvCos * (1f - avgMovieRatio))
        val genrePoints = blendedGenreCos * 45f

        // 2. Repulsion Collision Penalty (up to -15 pts)
        var collisionPenalty = 0f
        dislikedGenres.forEach { d ->
            if ((other.movieGenres[d] ?: 0f) >= 0.45f || (other.tvGenres[d] ?: 0f) >= 0.45f) {
                collisionPenalty += 7.5f
            }
        }
        other.dislikedGenres.forEach { d ->
            if ((movieGenres[d] ?: 0f) >= 0.45f || (tvGenres[d] ?: 0f) >= 0.45f) {
                collisionPenalty += 7.5f
            }
        }
        collisionPenalty = collisionPenalty.coerceAtMost(15f)

        // 3. Synergy Genre Pairs Overlap (up to +10 pts)
        val sharedPairs = topGenrePairs.intersect(other.topGenrePairs.toSet()).size
        val pairPoints = when {
            sharedPairs >= 2 -> 10f
            sharedPairs == 1 -> 5f
            else -> 0f
        }

        // 4. Era / Decades Overlap (up to +10 pts)
        val decadeCos = computeCosineSimilarity(topDecades, other.topDecades)
        val decadePoints = decadeCos * 10f

        // 5. Auteur & Cast Overlap (up to +15 pts)
        val sharedDirectors = topDirectors.intersect(other.topDirectors.toSet()).size
        val sharedActors = topActors.intersect(other.topActors.toSet()).size
        val auteurPoints = ((sharedDirectors * 4.0f) + (sharedActors * 2.5f)).coerceAtMost(15f)

        // 6. Persona & Taste Nuance (up to +15 pts)
        val indiePoints = when {
            isIndieSeeker && other.isIndieSeeker -> 5f
            !isIndieSeeker && !other.isIndieSeeker -> 3f
            else -> 0f
        }

        val animDiff = abs(animationRatio - other.animationRatio)
        val animPoints = when {
            animDiff <= 0.10f -> 5f
            animDiff <= 0.25f -> 2.5f
            else -> 0f
        }

        val ratingDiff = abs(avgRating - other.avgRating)
        val ratingPoints = when {
            ratingDiff <= 0.7f -> 5f
            ratingDiff <= 1.4f -> 2.5f
            else -> 0f
        }

        val totalRaw = genrePoints - collisionPenalty + pairPoints + decadePoints + auteurPoints + indiePoints + animPoints + ratingPoints
        return totalRaw.roundToInt().coerceIn(10, 99)
    }

    private fun computeCosineSimilarity(v1: Map<String, Float>, v2: Map<String, Float>): Float {
        if (v1.isEmpty() || v2.isEmpty()) return 0.5f
        val allKeys = v1.keys + v2.keys
        var dot = 0f
        var norm1 = 0f
        var norm2 = 0f
        allKeys.forEach { k ->
            val val1 = v1[k] ?: 0f
            val val2 = v2[k] ?: 0f
            dot += val1 * val2
            norm1 += val1 * val1
            norm2 += val2 * val2
        }
        val denom = sqrt(norm1.toDouble()) * sqrt(norm2.toDouble())
        return if (denom > 0.0) (dot / denom).toFloat().coerceIn(0f, 1f) else 0f
    }
}
