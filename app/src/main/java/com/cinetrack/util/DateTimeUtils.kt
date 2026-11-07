package com.cinetrack.util

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.IsoFields
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * High-performance, thread-safe date formatting and parsing utilities for FlickTrove.
 * Replaces ad-hoc SimpleDateFormat allocations across workers, repositories, and models.
 */
object DateTimeUtils {
    const val ISO_8601_MILLIS_UTC = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    const val ISO_8601_SECONDS_UTC = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    const val ISO_8601_BASIC = "yyyy-MM-dd'T'HH:mm:ss"
    const val DATE_ONLY = "yyyy-MM-dd"

    private val utcTimeZone: TimeZone = TimeZone.getTimeZone("UTC")

    private val isoMillisUtcFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat(ISO_8601_MILLIS_UTC, Locale.US).apply {
                timeZone = utcTimeZone
            }
        }
    }

    private val isoSecondsUtcFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat(ISO_8601_SECONDS_UTC, Locale.US).apply {
                timeZone = utcTimeZone
            }
        }
    }

    private val isoBasicFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat(ISO_8601_BASIC, Locale.US)
        }
    }

    private val dateOnlyFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat(DATE_ONLY, Locale.US)
        }
    }

    /**
     * Formats the current moment to ISO 8601 UTC with milliseconds (e.g. 2026-10-27T12:00:00.000Z).
     */
    fun nowIsoUtc(): String {
        return formatIsoMillisUtc(Date())
    }

    /**
     * Formats a given Date to ISO 8601 UTC with milliseconds.
     */
    fun formatIsoMillisUtc(date: Date): String {
        return isoMillisUtcFormat.get()?.format(date) ?: ""
    }

    /**
     * Formats a given timestamp (in milliseconds) to ISO 8601 UTC with milliseconds.
     */
    fun formatIsoMillisUtc(timestamp: Long): String {
        return formatIsoMillisUtc(Date(timestamp))
    }

    /**
     * Parses an ISO 8601 date string, trying java.time.Instant first,
     * then falling back to thread-safe millisecond UTC, second UTC, and basic ISO patterns.
     */
    fun parseIsoDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            Date.from(Instant.parse(dateStr))
        } catch (_: Exception) {
            try {
                isoMillisUtcFormat.get()?.parse(dateStr)
            } catch (_: Exception) {
                try {
                    isoSecondsUtcFormat.get()?.parse(dateStr)
                } catch (_: Exception) {
                    try {
                        isoBasicFormat.get()?.parse(dateStr)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }

    /**
     * Parses an ISO date string to unix epoch milliseconds, or returns 0L on failure.
     */
    fun parseIsoDateToMillis(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        return try {
            Instant.parse(dateStr).toEpochMilli()
        } catch (_: Exception) {
            parseIsoDate(dateStr)?.time ?: 0L
        }
    }

    /**
     * Parses a date string in "yyyy-MM-dd" format.
     */
    fun parseDateOnly(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            dateOnlyFormat.get()?.parse(dateStr)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Formats a Date to "yyyy-MM-dd".
     */
    fun formatDateOnly(date: Date): String {
        return dateOnlyFormat.get()?.format(date) ?: ""
    }

    /**
     * Calculates the ISO 8601 week ID in the format "${weekYear}_W$week" (e.g. "2026_W45").
     * Uses UTC and IsoFields (ISO-8601 standard: week starts on Monday, year based on week)
     * to ensure global consistency on Firestore and prevent year-boundary desynchronization
     * (e.g. Jan 1, 2027 belonging to 2026_W53).
     */
    fun getIsoWeekId(epochMillis: Long = System.currentTimeMillis()): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC)
        val weekYear = zdt.get(IsoFields.WEEK_BASED_YEAR)
        val week = zdt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return "${weekYear}_W$week"
    }

    /**
     * Returns the list of the last [weeksCount] ISO week IDs (including the current one),
     * walking backwards week by week safely across year boundaries and 52/53-week years.
     */
    fun getRecentIsoWeekIds(
        weeksCount: Int = 4,
        epochMillis: Long = System.currentTimeMillis()
    ): List<String> {
        val baseZdt = Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC)
        return (0 until weeksCount).map { offset ->
            val targetZdt = baseZdt.minusWeeks(offset.toLong())
            val weekYear = targetZdt.get(IsoFields.WEEK_BASED_YEAR)
            val week = targetZdt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            "${weekYear}_W$week"
        }
    }
}

/**
 * Looks up the watched date for an episode across legacy and modern key formats:
 * Checks "${seasonNumber}_${episodeNumber}" first, then "S${seasonNumber}E${episodeNumber}".
 */
fun Map<String, String>.getEpisodeWatchDate(seasonNumber: Int, episodeNumber: Int): String? {
    return this["${seasonNumber}_${episodeNumber}"] ?: this["S${seasonNumber}E${episodeNumber}"]
}

/**
 * Sets the watch date for an episode, populating "${seasonNumber}_${episodeNumber}"
 * and cleaning up legacy "S${seasonNumber}E${episodeNumber}" to ensure consistency.
 */
fun MutableMap<String, String>.setEpisodeWatchDate(seasonNumber: Int, episodeNumber: Int, dateIso: String) {
    this["${seasonNumber}_${episodeNumber}"] = dateIso
    this.remove("S${seasonNumber}E${episodeNumber}")
}

/**
 * Removes the episode date under both key formats.
 */
fun MutableMap<String, String>.removeEpisodeWatchDate(seasonNumber: Int, episodeNumber: Int) {
    this.remove("${seasonNumber}_${episodeNumber}")
    this.remove("S${seasonNumber}E${episodeNumber}")
}

