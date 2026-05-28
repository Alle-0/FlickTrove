package com.cinetrack.data

import androidx.compose.runtime.Stable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import com.cinetrack.data.models.PersonData
import com.cinetrack.data.models.Season
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Serializable
@Entity(
    tableName = "favorites",
    primaryKeys = ["id", "media_type"],
    indices = [Index(value = ["sync_status"])]
)
@Stable
data class Movie(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: Long = 0L,
    @SerialName("media_type")
    @get:PropertyName("media_type")
    @set:PropertyName("media_type")
    @ColumnInfo(name = "media_type") var mediaType: String = "", // 'movie' or 'tv'
    
    @SerialName("imdb_id")
    @get:PropertyName("imdb_id")
    @set:PropertyName("imdb_id")
    @ColumnInfo(name = "imdb_id") var imdbId: String? = null,
    
    // Core Display Info
    @get:PropertyName("title")
    @set:PropertyName("title")
    var title: String? = null,
    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String? = null, // TV shows use name instead of title
    
    @SerialName("poster_path")
    @get:PropertyName("poster_path")
    @set:PropertyName("poster_path")
    @ColumnInfo(name = "poster_path") var posterPath: String? = null,
    
    @SerialName("backdrop_path")
    @get:PropertyName("backdrop_path")
    @set:PropertyName("backdrop_path")
    @ColumnInfo(name = "backdrop_path") var backdropPath: String? = null,
    
    @SerialName("vote_average")
    @get:Exclude
    @set:Exclude
    @ColumnInfo(name = "vote_average") var voteAverage: Double? = null,
    
    @SerialName("vote_count")
    @get:PropertyName("vote_count")
    @set:PropertyName("vote_count")
    @ColumnInfo(name = "vote_count") var voteCount: Int? = 0,
    
    @get:PropertyName("overview")
    @set:PropertyName("overview")
    var overview: String? = null,
    
    @SerialName("release_date")
    @get:PropertyName("release_date")
    @set:PropertyName("release_date")
    @ColumnInfo(name = "release_date") var releaseDate: String? = null,
    
    @SerialName("first_air_date")
    @get:PropertyName("first_air_date")
    @set:PropertyName("first_air_date")
    @ColumnInfo(name = "first_air_date") var firstAirDate: String? = null,
    
    @SerialName("next_episode_air_date")
    @get:PropertyName("next_episode_air_date")
    @set:PropertyName("next_episode_air_date")
    @ColumnInfo(name = "next_episode_air_date") var nextEpisodeAirDate: String? = null,
    
    @SerialName("next_episode_string")
    @get:PropertyName("next_episode_string")
    @set:PropertyName("next_episode_string")
    @ColumnInfo(name = "next_episode_string") var nextEpisodeString: String? = null,
    
    @SerialName("last_air_date")
    @get:PropertyName("last_air_date")
    @set:PropertyName("last_air_date")
    @ColumnInfo(name = "last_air_date") var lastAirDate: String? = null,
    
    @SerialName("release_year")
    @get:PropertyName("release_year")
    @set:PropertyName("release_year")
    @ColumnInfo(name = "release_year") var releaseYear: String? = null,
    
    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String? = null,
    @get:PropertyName("tagline")
    @set:PropertyName("tagline")
    var tagline: String? = null,
    @get:PropertyName("runtime")
    @set:PropertyName("runtime")
    var runtime: Int? = 0,
    
    @SerialName("episode_run_time")
    @get:PropertyName("episode_run_time")
    @set:PropertyName("episode_run_time")
    @ColumnInfo(name = "episode_run_time") var episodeRunTime: List<Int>? = null,
    
    // User State
    @get:PropertyName("favorite")
    @set:PropertyName("favorite")
    var favorite: Boolean = false,
    @get:PropertyName("watched")
    @set:PropertyName("watched")
    var watched: Boolean = false,
    @get:PropertyName("reminder")
    @set:PropertyName("reminder")
    var reminder: Boolean = false,
    
    @SerialName("personal_rating")
    @get:Exclude
    @set:Exclude
    @ColumnInfo(name = "personal_rating") var personalRating: Double? = null,
    
    @SerialName("personal_note")
    @get:PropertyName("personal_note")
    @set:PropertyName("personal_note")
    @ColumnInfo(name = "personal_note") var personalNote: String? = null,
    
    @SerialName("watched_at")
    @get:Exclude
    @set:Exclude
    @ColumnInfo(name = "watched_at") var watchedAt: String? = null,

    @SerialName("updated_at")
    @get:Exclude
    @set:Exclude
    @ColumnInfo(name = "updated_at") var updatedAt: String? = null,
    
    // Series Specific
    @get:PropertyName("seasons")
    @set:PropertyName("seasons")
    var seasons: List<Season>? = null,
    
    @SerialName("number_of_seasons")
    @get:PropertyName("number_of_seasons")
    @set:PropertyName("number_of_seasons")
    @ColumnInfo(name = "number_of_seasons") var numberOfSeasons: Int? = 0,
    
    @SerialName("number_of_episodes")
    @get:PropertyName("number_of_episodes")
    @set:PropertyName("number_of_episodes")
    @ColumnInfo(name = "number_of_episodes") var numberOfEpisodes: Int? = 0,
    
    @SerialName("watched_episodes")
    @get:PropertyName("watched_episodes")
    @set:PropertyName("watched_episodes")
    @ColumnInfo(name = "watched_episodes") var watchedEpisodes: Map<String, List<Int>>? = null, // "seasonNum": [epNums]
    
    @SerialName("new_episodes_found")
    @get:PropertyName("new_episodes_found")
    @set:PropertyName("new_episodes_found")
    @ColumnInfo(name = "new_episodes_found") var newEpisodesFound: Int? = 0,
    
    @get:Exclude
    @set:Exclude
    var progress: Double? = 0.0, // 0-1
    
    // Metadata & Classification
    @get:Exclude
    @set:Exclude
    @ColumnInfo(name = "genres") var genres: List<Genre>? = null,
    
    @SerialName("genre_ids")
    @get:PropertyName("genre_ids")
    @set:PropertyName("genre_ids")
    @ColumnInfo(name = "genre_ids") var genreIds: List<Long>? = null,
    
    @SerialName("streaming_provider_ids")
    @get:PropertyName("streaming_provider_ids")
    @set:PropertyName("streaming_provider_ids")
    @ColumnInfo(name = "streaming_provider_ids") var streamingProviderIds: List<Long>? = null,
    
    @SerialName("genre_names_string")
    @get:PropertyName("genre_names_string")
    @set:PropertyName("genre_names_string")
    @ColumnInfo(name = "genre_names_string") var genreNamesString: String? = null,
    
    @SerialName("revenue")
    @get:PropertyName("revenue")
    @set:PropertyName("revenue")
    @ColumnInfo(name = "revenue") var revenue: Long? = 0L,

    @SerialName("budget")
    @get:PropertyName("budget")
    @set:PropertyName("budget")
    @ColumnInfo(name = "budget") var budget: Long? = 0L,

    @SerialName("is_upcoming")
    @get:PropertyName("is_upcoming")
    @set:PropertyName("is_upcoming")
    @ColumnInfo(name = "is_upcoming") var isUpcoming: Boolean? = false,
    
    // Crew & Cast
    @SerialName("director_name")
    @get:PropertyName("director_name")
    @set:PropertyName("director_name")
    @ColumnInfo(name = "director_name") var directorName: String? = null,
    
    @SerialName("director_profile_path")
    @get:PropertyName("director_profile_path")
    @set:PropertyName("director_profile_path")
    @ColumnInfo(name = "director_profile_path") var directorProfilePath: String? = null,
    
    @SerialName("director_id")
    @get:PropertyName("director_id")
    @set:PropertyName("director_id")
    @ColumnInfo(name = "director_id") var directorId: Long? = null,
    
    @SerialName("director_data")
    @get:Exclude
    @set:Exclude
    @ColumnInfo(name = "director_data") var directorData: List<PersonData>? = null,
    
    @SerialName("short_cast_string")
    @get:PropertyName("short_cast_string")
    @set:PropertyName("short_cast_string")
    @ColumnInfo(name = "short_cast_string") var shortCastString: String? = null,
    
    @SerialName("top_cast_data")
    @get:Exclude
    @set:Exclude
    @ColumnInfo(name = "top_cast_data") var topCastData: List<PersonData>? = null,

    @SerialName("job")
    @get:PropertyName("job")
    @set:PropertyName("job")
    @ColumnInfo(name = "job") var job: String? = null,

    @SerialName("character")
    @get:PropertyName("character")
    @set:PropertyName("character")
    @ColumnInfo(name = "character") var character: String? = null,
    
    // UI Aesthetics
    @SerialName("accent_color")
    @get:PropertyName("accent_color")
    @set:PropertyName("accent_color")
    @ColumnInfo(name = "accent_color") var accentColor: String? = null,
    
    @SerialName("accent_color_static")
    @get:PropertyName("accent_color_static")
    @set:PropertyName("accent_color_static")
    @ColumnInfo(name = "accent_color_static") var accentColorStatic: String? = null,
    
    // System Fields
    @SerialName("last_sync_date")
    @get:Exclude
    @set:Exclude
    @ColumnInfo(name = "last_sync_date") var lastSyncDate: String? = null,
    
    @SerialName("created_at")
    @get:Exclude
    @set:Exclude
    @ColumnInfo(name = "created_at") var createdAt: String? = null,
    
    @SerialName("migrated_at")
    @get:Exclude
    @set:Exclude
    @ColumnInfo(name = "migrated_at") var migratedAt: String? = null,

    
    @SerialName("_touched")
    @get:PropertyName("_touched")
    @set:PropertyName("_touched")
    @ColumnInfo(name = "_touched") var touched: Long? = 0L,
    
    @SerialName("stats_enriched")
    @get:PropertyName("stats_enriched")
    @set:PropertyName("stats_enriched")
    @ColumnInfo(name = "stats_enriched") var statsEnriched: Boolean? = false,
    
    // Room/Sync Specific (mapped from db.ts columns)
    @SerialName("sync_status")
    @get:PropertyName("sync_status")
    @set:PropertyName("sync_status")
    @ColumnInfo(name = "sync_status") var syncStatus: String = "synced",
    
    @SerialName("client_updated_at")
    @get:PropertyName("client_updated_at")
    @set:PropertyName("client_updated_at")
    @ColumnInfo(name = "client_updated_at") var clientUpdatedAt: Long = 0L
) {
    // --- Computed Properties ---

    @get:Exclude
    val compositeId: String
        get() = "${mediaType}_$id"

    @get:Exclude
    val displayName: String
        get() = (if (!title.isNullOrEmpty()) title else name) ?: ""

    @get:Exclude
    val isReleased: Boolean
        get() {
            // 1. Robust date comparison (Priority)
            val todayIso = try {
                LocalDate.now().toString()
            } catch (e: Exception) {
                "2024-01-01" // Fallback if system date fails
            }
            val currentYear = todayIso.take(4).toIntOrNull() ?: 2024

            // Check date if available
            val dateStr = releaseDate ?: firstAirDate
            if (!dateStr.isNullOrBlank()) {
                try {
                    if (dateStr.length >= 10) {
                        // Compare full ISO strings: "2026-11-25" <= "2026-05-12" is false
                        // If date is today or before, it's released!
                        if (dateStr.take(10) <= todayIso) return true
                        // Otherwise it's in the future, so definitely not released
                        return false
                    } else if (dateStr.length >= 4) {
                        val year = dateStr.take(4).toIntOrNull()
                        if (year != null) {
                            if (year < currentYear) return true
                            if (year > currentYear) return false
                        }
                    }
                } catch (e: Exception) {
                    // Ignore date format errors and fall through
                }
            }

            // Check year-only fields as fallback
            releaseYear?.toIntOrNull()?.let { year ->
                if (year > currentYear) return false
                if (year < currentYear) return true
            }

            // 2. Check status if available (TMDB status)
            val lowerStatus = status?.lowercase()
            if (lowerStatus != null) {
                if (lowerStatus == "released") return true
                if (lowerStatus == "planned" || lowerStatus == "in production" ||
                    lowerStatus == "post production" || lowerStatus == "rumored") {
                    return false
                }
            }

            // 3. Fallback to isUpcoming flag
            if (isUpcoming == true) return false

            // 4. If no date at all and no status confirming release → treat as unreleased.
            //    Films announced on TMDB without a date (e.g. "Zootopia 3") fall here.
            if (dateStr.isNullOrBlank() && releaseYear.isNullOrBlank()) return false

            // Default to true only when we have at least some date evidence
            return true
        }

    // --- Firestore Resilient Deserialization Proxies ---

    @get:PropertyName("vote_average")
    @set:PropertyName("vote_average")
    var voteAverageProxy: Any?
        get() = voteAverage
        set(value) {
            voteAverage = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }
        }

    @get:PropertyName("personal_rating")
    @set:PropertyName("personal_rating")
    var personalRatingProxy: Any?
        get() = personalRating
        set(value) {
            personalRating = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }
        }

    @get:PropertyName("progress")
    @set:PropertyName("progress")
    var progressProxy: Any?
        get() = progress
        set(value) {
            progress = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }
        }

    @get:PropertyName("watched_at")
    @set:PropertyName("watched_at")
    var watchedAtProxy: Any?
        get() = watchedAt
        set(value) { watchedAt = parseDate(value) }

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAtProxy: Any?
        get() = updatedAt
        set(value) { updatedAt = parseDate(value) }

    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAtProxy: Any?
        get() = createdAt
        set(value) { createdAt = parseDate(value) }

    @get:PropertyName("last_sync_date")
    @set:PropertyName("last_sync_date")
    var lastSyncDateProxy: Any?
        get() = lastSyncDate
        set(value) { lastSyncDate = parseDate(value) }

    @get:PropertyName("migrated_at")
    @set:PropertyName("migrated_at")
    var migratedAtProxy: Any?
        get() = migratedAt
        set(value) { migratedAt = parseDate(value) }

    @get:PropertyName("genres")
    @set:PropertyName("genres")
    var genresProxy: List<Any?>?
        get() = genres
        set(value) {
            genres = value?.mapNotNull { parseGenre(it) }
        }

    @get:PropertyName("director_data")
    @set:PropertyName("director_data")
    var directorDataProxy: List<Any?>?
        get() = directorData
        set(value) {
            directorData = value?.mapNotNull { parsePerson(it) }
        }

    @get:PropertyName("top_cast_data")
    @set:PropertyName("top_cast_data")
    var topCastDataProxy: List<Any?>?
        get() = topCastData
        set(value) {
            topCastData = value?.mapNotNull { parsePerson(it) }
        }

    private fun parseDate(value: Any?): String? {
        return when (value) {
            is String -> value
            is com.google.firebase.Timestamp -> {
                utcFormatter.get()?.format(value.toDate())
            }
            is Map<*, *> -> {
                // Sometimes Firestore deserializes Timestamps as Maps {seconds=..., nanoseconds=...}
                val seconds = (value["seconds"] as? Number)?.toLong()
                if (seconds != null) {
                    utcFormatter.get()?.format(Date(seconds * 1000))
                } else null
            }
            else -> null
        }
    }

    private fun parseGenre(item: Any?): Genre? {
        return when (item) {
            is String -> {
                val found = GenreConstants.ALL_GENRES.find { it.name.equals(item, ignoreCase = true) }
                found ?: Genre(id = 0L, name = item)
            }
            is Map<*, *> -> {
                val id = (item["id"] as? Number)?.toLong() ?: 0L
                val name = item["name"] as? String ?: ""
                if (name.isNotEmpty()) Genre(id, name) else null
            }
            is Genre -> item
            else -> null
        }
    }

    private fun parsePerson(item: Any?): PersonData? {
        return when (item) {
            is String -> PersonData(id = 0L, name = item)
            is Map<*, *> -> {
                val id = (item["id"] as? Number)?.toLong() ?: 0L
                val name = item["name"] as? String ?: ""
                val profilePath = item["profile_path"] as? String
                if (name.isNotEmpty()) PersonData(id, name, profilePath) else null
            }
            is PersonData -> item
            else -> null
        }
    }

    companion object {
        private val utcFormatter = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            }
        }
    }
}
