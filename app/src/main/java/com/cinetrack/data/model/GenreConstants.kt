package com.cinetrack.data.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Serializable
@Stable
data class Genre(val id: Long = 0L, val name: String = "")

object GenreConstants {
    val MOVIE_GENRES = listOf(
        Genre(28, "Azione"),
        Genre(12, "Avventura"),
        Genre(16, "Animazione"),
        Genre(35, "Commedia"),
        Genre(80, "Crime"),
        Genre(99, "Documentario"),
        Genre(18, "Dramma"),
        Genre(10751, "Famiglia"),
        Genre(14, "Fantasy"),
        Genre(36, "Storia"),
        Genre(27, "Horror"),
        Genre(10402, "Musica"),
        Genre(9648, "Mistero"),
        Genre(10749, "Romance"),
        Genre(878, "Fantascienza"),
        Genre(10770, "Film TV"),
        Genre(53, "Thriller"),
        Genre(10752, "Guerra"),
        Genre(37, "Western")
    )

    val TV_GENRES = listOf(
        Genre(10759, "Action & Adventure"),
        Genre(16, "Animazione"),
        Genre(35, "Commedia"),
        Genre(80, "Crime"),
        Genre(99, "Documentario"),
        Genre(18, "Dramma"),
        Genre(10751, "Famiglia"),
        Genre(10762, "Kids"),
        Genre(9648, "Mistero"),
        Genre(10763, "News"),
        Genre(10764, "Reality"),
        Genre(10765, "Sci-Fi & Fantasy"),
        Genre(10766, "Soap"),
        Genre(10767, "Talk"),
        Genre(10768, "War & Politics"),
        Genre(37, "Western")
    )

    val ALL_GENRES = (MOVIE_GENRES + TV_GENRES).distinctBy { it.id }

    private val ENGLISH_NAMES = mapOf(
        28L to "Action",
        12L to "Adventure",
        16L to "Animation",
        35L to "Comedy",
        80L to "Crime",
        99L to "Documentary",
        18L to "Drama",
        10751L to "Family",
        14L to "Fantasy",
        36L to "History",
        27L to "Horror",
        10402L to "Music",
        9648L to "Mystery",
        10749L to "Romance",
        878L to "Science Fiction",
        10770L to "TV Movie",
        53L to "Thriller",
        10752L to "War",
        37L to "Western",
        10759L to "Action & Adventure",
        10762L to "Kids",
        10763L to "News",
        10764L to "Reality",
        10765L to "Sci-Fi & Fantasy",
        10766L to "Soap",
        10767L to "Talk",
        10768L to "War & Politics"
    )

    fun getLocalizedName(id: Long, languageCode: String, defaultName: String): String {
        if (languageCode.lowercase().startsWith("it")) return defaultName
        return ENGLISH_NAMES[id] ?: defaultName
    }
}
