package com.cinetrack.data.local.converters

import android.util.Log
import androidx.room.TypeConverter
import com.cinetrack.data.model.Genre
import com.cinetrack.data.model.PersonData
import com.cinetrack.data.model.Season
import com.cinetrack.data.model.StudioData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FlickTroveConverters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value.isNullOrBlank() || value == "[]") return emptyList()
        return try {
            json.decodeFromString(value)
        } catch (t: Throwable) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        if (value.isNullOrBlank() || value == "[]") return emptyList()
        return try {
            json.decodeFromString(value)
        } catch (t: Throwable) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromLongList(value: List<Long>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        if (value.isNullOrBlank() || value == "[]") return emptyList()
        return try {
            json.decodeFromString(value)
        } catch (t: Throwable) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromSeasonList(value: List<Season>?): String? {
        if (value.isNullOrEmpty()) return null
        return try {
            // Strip large text overviews to keep SQLite rows lean and prevent OutOfMemoryError in CursorWindow
            val sanitized = value.map { season ->
                if (season.episodes.isNullOrEmpty()) {
                    if (season.overview != null) season.copy(overview = null) else season
                } else {
                    season.copy(
                        overview = null,
                        episodes = season.episodes.map { ep ->
                            if (ep.overview != null) ep.copy(overview = null) else ep
                        }
                    )
                }
            }
            json.encodeToString(sanitized)
        } catch (t: Throwable) {
            Log.e("FlickTroveConverters", "Error serializing seasons: ${t.message}")
            null
        }
    }

    @TypeConverter
    fun toSeasonList(value: String?): List<Season>? {
        if (value.isNullOrBlank() || value == "[]") return emptyList()
        return try {
            json.decodeFromString<List<Season>>(value)
        } catch (t: Throwable) {
            Log.e("FlickTroveConverters", "Error parsing seasons: ${t.message}")
            emptyList()
        }
    }

    @TypeConverter
    fun fromPersonDataList(value: List<PersonData>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toPersonDataList(value: String?): List<PersonData>? {
        if (value.isNullOrBlank() || value == "[]") return emptyList()
        return try {
            json.decodeFromString(value)
        } catch (t: Throwable) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromWatchedEpisodesMap(value: Map<String, List<Int>>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toWatchedEpisodesMap(value: String?): Map<String, List<Int>>? {
        if (value.isNullOrBlank() || value == "{}" || value == "[]") return emptyMap()
        return try {
            json.decodeFromString(value)
        } catch (t: Throwable) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromGenreList(value: List<Genre>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toGenreList(value: String?): List<Genre>? {
        if (value.isNullOrBlank() || value == "[]") return emptyList()
        return try {
            json.decodeFromString(value)
        } catch (t: Throwable) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStudioDataList(value: List<StudioData>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toStudioDataList(value: String?): List<StudioData>? {
        if (value.isNullOrBlank() || value == "[]") return emptyList()
        return try {
            json.decodeFromString(value)
        } catch (t: Throwable) {
            emptyList()
        }
    }
}
