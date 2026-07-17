package com.cinetrack.domain

import com.cinetrack.data.model.Movie
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import javax.inject.Inject

class UpdateEpisodesUseCase @Inject constructor() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Calculates the updated movie object after changing watched episodes.
     * Ported from tvSyncService.ts -> calculateEpisodeUpdate
     */
    operator fun invoke(
        movie: Movie,
        seasonNum: Int,
        episodeNums: List<Int>
    ): Movie {
        val currentWatched = movie.watchedEpisodes?.toMutableMap() ?: mutableMapOf()
        currentWatched[seasonNum.toString()] = episodeNums

        val totalWatched = currentWatched.filter { it.key != "0" }.values.sumOf { it.size }
        val totalEpisodes = movie.effectiveTotalEpisodes
        val progressVal = if (totalEpisodes > 0) (totalWatched.toDouble() / totalEpisodes).coerceIn(0.0, 1.0) else 0.0

        val now = dateFormat.format(Date())

        return movie.copy(
            watchedEpisodes = currentWatched,
            progress = progressVal,
            mediaType = "tv",
            lastSyncDate = now
        ).let { nextMovie ->
            if (totalEpisodes > 0 && totalWatched >= totalEpisodes) {
                nextMovie.copy(
                    watched = true,
                    favorite = false,
                    reminder = false,
                    newEpisodesFound = 0,
                    watchedAt = movie.watchedAt ?: now
                )
            } else {
                nextMovie.copy(
                    watched = false,
                    favorite = true
                )
            }
        }
    }

    /**
     * Batch update multiple seasons/episodes at once.
     */
    fun batchUpdate(
        movie: Movie,
        updates: Map<Int, List<Int>>
    ): Movie {
        val currentWatched = movie.watchedEpisodes?.toMutableMap() ?: mutableMapOf()
        updates.forEach { (season, eps) ->
            currentWatched[season.toString()] = eps
        }

        val totalWatched = currentWatched.filter { it.key != "0" }.values.sumOf { it.size }
        val totalEpisodes = movie.effectiveTotalEpisodes
        val progressVal = if (totalEpisodes > 0) (totalWatched.toDouble() / totalEpisodes).coerceIn(0.0, 1.0) else 0.0

        val now = dateFormat.format(Date())

        return movie.copy(
            watchedEpisodes = currentWatched,
            progress = progressVal,
            mediaType = "tv",
            lastSyncDate = now
        ).let { nextMovie ->
            if (totalEpisodes > 0 && totalWatched >= totalEpisodes) {
                nextMovie.copy(
                    watched = true,
                    favorite = false,
                    reminder = false,
                    newEpisodesFound = 0,
                    watchedAt = movie.watchedAt ?: now
                )
            } else {
                nextMovie.copy(
                    watched = false,
                    favorite = true
                )
            }
        }
    }

    /**
     * Marks the entire series as watched by populating all episodes.
     */
    fun markAllWatched(movie: Movie): Movie {
        val allWatched = mutableMapOf<String, List<Int>>()
        val todayIso = try {
            LocalDate.now().toString()
        } catch (e: Exception) {
            "2026-01-01"
        }
        var nextEpSeason: Int? = null
        var nextEpNum: Int? = null
        if (!movie.nextEpisodeString.isNullOrBlank() && (movie.nextEpisodeAirDate.isNullOrBlank() || movie.nextEpisodeAirDate!! > todayIso)) {
            try {
                val match = Regex("""[Ss](\d+)[Ee](\d+)""").find(movie.nextEpisodeString!!)
                if (match != null) {
                    nextEpSeason = match.groupValues[1].toIntOrNull()
                    nextEpNum = match.groupValues[2].toIntOrNull()
                }
            } catch (e: Exception) {
                // Ignore parsing error
            }
        }

        movie.seasons?.filter { (it.seasonNumber ?: 0) > 0 }?.forEach { season ->
            val count = movie.getReleasedEpisodeCountForSeason(season, todayIso, nextEpSeason, nextEpNum)
            if (count > 0) {
                val seasonNum = season.seasonNumber ?: 0
                val eps = (1..count).toList()
                allWatched[seasonNum.toString()] = eps
            }
        }

        // If for some reason we had episodes in Season 0, preserve them but they don't count towards the mark-all
        movie.watchedEpisodes?.get("0")?.let {
            allWatched["0"] = it
        }

        val now = dateFormat.format(Date())
        return movie.copy(
            watchedEpisodes = allWatched,
            progress = 1.0,
            watched = true,
            favorite = false,
            reminder = false,
            newEpisodesFound = 0,
            watchedAt = movie.watchedAt ?: now,
            lastSyncDate = now,
            mediaType = "tv"
        )
    }
}
