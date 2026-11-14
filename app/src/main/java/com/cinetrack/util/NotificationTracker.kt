package com.cinetrack.util

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object NotificationTracker {

    private const val PREFS_NAME = "flicktrove_notification_tracker_prefs"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun wasEpisodeNotified(context: Context, showId: Long, episodeString: String): Boolean {
        val key = "ep_${showId}_$episodeString"
        return getPrefs(context).contains(key)
    }

    fun markEpisodeNotified(
        context: Context,
        showId: Long,
        episodeString: String,
        date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    ) {
        val key = "ep_${showId}_$episodeString"
        getPrefs(context).edit {
            putString(key, date)
        }
    }

    fun wasReleaseNotified(context: Context, mediaType: String, id: Long, date: String): Boolean {
        val key = "rel_${mediaType}_${id}_$date"
        return getPrefs(context).contains(key)
    }

    fun markReleaseNotified(context: Context, mediaType: String, id: Long, date: String) {
        val key = "rel_${mediaType}_${id}_$date"
        getPrefs(context).edit {
            putString(key, date)
        }
    }

    /**
     * Purges entries older than 30 days to keep SharedPreferences minimal and performant.
     */
    fun cleanupStaleEntries(context: Context) {
        try {
            val prefs = getPrefs(context)
            val allEntries = prefs.all
            if (allEntries.size < 50) return

            val cutoffDate = LocalDate.now().minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val keysToRemove = mutableListOf<String>()

            for ((key, value) in allEntries) {
                if (value is String && value < cutoffDate) {
                    keysToRemove.add(key)
                }
            }

            if (keysToRemove.isNotEmpty()) {
                prefs.edit {
                    keysToRemove.forEach { remove(it) }
                }
            }
        } catch (e: Exception) {
            // Defensive: ignore preferences cleanup error
        }
    }
}
