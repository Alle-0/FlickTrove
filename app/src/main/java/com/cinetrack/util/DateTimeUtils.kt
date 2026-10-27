package com.cinetrack.util

import java.text.SimpleDateFormat
import java.time.Instant
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
}
