package com.cinetrack.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Rich 360° representation of a user's cinematic and TV taste profile.
 * Distilled from [com.cinetrack.domain.UserGenreProfile] and synchronized lazily (at most once every 30 days,
 * only upon app launch / sync) to Firestore for future social/match capabilities.
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
)
