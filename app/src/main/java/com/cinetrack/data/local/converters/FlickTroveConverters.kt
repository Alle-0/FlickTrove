package com.cinetrack.data.local.converters

import androidx.room.TypeConverter
import com.cinetrack.data.model.Genre
import com.cinetrack.data.model.PersonData
import com.cinetrack.data.model.Season
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
    fun toStringList(value: String?): List<String>? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromLongList(value: List<Long>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toLongList(value: String?): List<Long>? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromSeasonList(value: List<Season>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toSeasonList(value: String?): List<Season>? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromPersonDataList(value: List<PersonData>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toPersonDataList(value: String?): List<PersonData>? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromWatchedEpisodesMap(value: Map<String, List<Int>>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toWatchedEpisodesMap(value: String?): Map<String, List<Int>>? = value?.let { json.decodeFromString(it) }
    @TypeConverter
    fun fromGenreList(value: List<Genre>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toGenreList(value: String?): List<Genre>? = value?.let { json.decodeFromString(it) }
}
