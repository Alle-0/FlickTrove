package com.cinetrack.data.repository

import android.content.Context
import com.cinetrack.data.local.entities.UserBadgeEntity
import com.cinetrack.data.local.entities.WatchHistoryEntity
import com.cinetrack.data.model.Movie
import com.cinetrack.util.DateTimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegate responsible for reverse-looking up the qualifying movies and shows
 * that unlocked special trophies (e.g. Double Feature, Lost Reel easter eggs, etc.).
 */
@Singleton
class TrophyMediaLookupDelegate @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val badgePrefs by lazy {
        context.getSharedPreferences("flicktrove_badge_prefs", Context.MODE_PRIVATE)
    }

    fun toLocalDayKey(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val millis = parseWatchedAtMillis(raw)
        return if (millis > 0L) {
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            String.format(
                Locale.US,
                "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        } else {
            raw.take(10)
        }
    }

    fun parseWatchedAtMillis(dateStr: String): Long {
        return DateTimeUtils.parseIsoDateToMillis(dateStr)
    }

    private fun getMoodDiscoveredIds(): Set<String> {
        val direct = badgePrefs.getStringSet("mood_discovered_movie_ids", emptySet()) ?: emptySet()
        if (direct.isNotEmpty()) return direct

        // Retroactive fallback: se non ancora popolato, prova a leggere dalla cronologia di ricerca mood
        try {
            val moodPrefs = context.getSharedPreferences("mood_history_prefs", Context.MODE_PRIVATE)
            val raw = moodPrefs.getString("mood_history_json", null)
            if (!raw.isNullOrBlank()) {
                val idRegex = Regex("\"id\":\\s*(\\d+)")
                val found = idRegex.findAll(raw).map { it.groupValues[1] }.toSet()
                if (found.isNotEmpty()) {
                    badgePrefs.edit().putStringSet("mood_discovered_movie_ids", found).apply()
                    return found
                }
            }
        } catch (_: Exception) {}
        return direct
    }

    /**
     * Risolve in modo deterministico e retroattivo il titolo e il poster dei media che hanno soddisfatto
     * i requisiti di sblocco per i trofei speciali, sessioni multiple (es. Doppio Spettacolo) e i Lost Reel cinefili.
     */
    fun findUnlockingMoviesForBadge(
        badgeId: String,
        allMovies: List<Movie>,
        allHistory: List<WatchHistoryEntity>,
        userBadge: UserBadgeEntity?
    ): List<Movie> {
        val watchedMovies = allMovies.filter { it.watched && it.mediaType != "tv" }
        val allWatched = allMovies.filter { it.watched }

        return when (badgeId) {
            // 48. Doppio Spettacolo: 2 film diversi nello stesso giorno solare
            "badge_double_feature" -> {
                val movieIds = watchedMovies.map { it.id }.toSet()
                val dayToMovieIds = mutableMapOf<String, MutableSet<Long>>()
                for (history in allHistory) {
                    if (history.movieId in movieIds) {
                        val dayKey = toLocalDayKey(history.watchedAt)
                        if (!dayKey.isNullOrBlank()) {
                            dayToMovieIds.getOrPut(dayKey) { mutableSetOf() }.add(history.movieId)
                        }
                    }
                }
                for (movie in watchedMovies) {
                    val dayKey = toLocalDayKey(movie.watchedAt)
                    if (!dayKey.isNullOrBlank()) {
                        dayToMovieIds.getOrPut(dayKey) { mutableSetOf() }.add(movie.id)
                    }
                }

                val badgeDate = userBadge?.unlockedDate?.let { toLocalDayKey(it) } ?: userBadge?.unlockedDate?.take(10)
                val qualifyingEntry = if (!badgeDate.isNullOrBlank() && (dayToMovieIds[badgeDate]?.size ?: 0) >= 2) {
                    badgeDate to dayToMovieIds[badgeDate]!!
                } else {
                    dayToMovieIds.entries
                        .filter { it.value.size >= 2 }
                        .maxByOrNull { it.key }
                        ?.let { it.key to it.value }
                }

                val targetIds = if (qualifyingEntry != null && qualifyingEntry.second.size >= 2) {
                    qualifyingEntry.second.take(2).toList()
                } else {
                    // Fallback di sicurezza: se il trofeo è sbloccato, rintraccia la coppia di film con minima distanza oraria
                    val movieTimestamps = watchedMovies.mapNotNull { m ->
                        val bestTime = allHistory.filter { it.movieId == m.id }
                            .map { parseWatchedAtMillis(it.watchedAt) }
                            .filter { it > 0L }
                            .maxOrNull()
                            ?: m.watchedAt?.let { parseWatchedAtMillis(it) }?.takeIf { it > 0L }
                        if (bestTime != null) m to bestTime else null
                    }.sortedByDescending { it.second }

                    var bestPair: Pair<Movie, Movie>? = null
                    var minDiff = Long.MAX_VALUE
                    for (i in 0 until movieTimestamps.size - 1) {
                        val diff = Math.abs(movieTimestamps[i].second - movieTimestamps[i + 1].second)
                        if (diff < minDiff && diff <= 24 * 3600 * 1000L) {
                            minDiff = diff
                            bestPair = movieTimestamps[i].first to movieTimestamps[i + 1].first
                        }
                    }

                    if (bestPair != null) {
                        listOf(bestPair.first.id, bestPair.second.id)
                    } else {
                        watchedMovies.sortedByDescending { it.watchedAt ?: "" }.take(2).map { it.id }
                    }
                }

                targetIds.mapNotNull { id -> allMovies.firstOrNull { it.id == id } }
            }

            // Lost Reel #01: Visione notturna tra le 02:00 e le 05:00
            "secret_night_owl" -> {
                val nightHistory = allHistory.firstOrNull { h ->
                    val millis = parseWatchedAtMillis(h.watchedAt)
                    if (millis > 0L) {
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        cal.get(Calendar.HOUR_OF_DAY) in 2..4
                    } else false
                }
                val m = nightHistory?.let { h -> allMovies.firstOrNull { it.id == h.movieId } }
                if (m != null) listOf(m) else emptyList()
            }

            // Lost Reel #02: Due visioni dello stesso film in 48 ore
            "secret_groundhog_day" -> {
                val byMovie = allHistory.groupBy { it.movieId }
                var matchedMovieId: Long? = null
                for ((movieId, entries) in byMovie) {
                    if (entries.size >= 2) {
                        val sorted = entries.map { parseWatchedAtMillis(it.watchedAt) }.filter { it > 0L }.sorted()
                        for (i in 0 until sorted.size - 1) {
                            val diffHours = (sorted[i + 1] - sorted[i]) / (1000 * 60 * 60)
                            if (diffHours in 0..48) {
                                matchedMovieId = movieId
                                break
                            }
                        }
                    }
                    if (matchedMovieId != null) break
                }
                val m = matchedMovieId?.let { id -> allMovies.firstOrNull { it.id == id } }
                if (m != null) listOf(m) else emptyList()
            }

            // Lost Reel #03: Film scoperto con Surprise Me
            "secret_surprise_fate" -> {
                val surpriseDiscoveredIds = badgePrefs.getStringSet("surprise_me_discovered_ids", emptySet()) ?: emptySet()
                val m = allWatched.firstOrNull { surpriseDiscoveredIds.contains(it.id.toString()) }
                if (m != null) listOf(m) else emptyList()
            }

            // Lost Reel #04: Film privo di generi su TMDB
            "secret_genreless_rebel" -> {
                val m = allWatched.firstOrNull { it.genreIds.isNullOrEmpty() }
                if (m != null) listOf(m) else emptyList()
            }

            // Lost Reel #05: Perfezionista (Voto, Nota e Vibe compilati insieme)
            "secret_completionist" -> {
                val m = allWatched.firstOrNull { m ->
                    m.personalRating != null &&
                        !m.personalNote.isNullOrBlank() &&
                        !m.emotionalVibes.isNullOrBlank() && m.emotionalVibes != "[]"
                }
                if (m != null) listOf(m) else emptyList()
            }

            // Lost Reel #07: Origini del Mito (Opera realizzata prima del 1940)
            "secret_genesis" -> {
                val m = watchedMovies.filter { m ->
                    val year = m.releaseDate?.take(4)?.toIntOrNull() ?: 2026
                    year in 1880..1939
                }.minByOrNull { m -> m.releaseDate?.take(4)?.toIntOrNull() ?: 2026 }
                if (m != null) listOf(m) else emptyList()
            }

            // Lost Reel #09: Gourmet Incompreso (Voto personale >= 9.0 con media TMDB <= 5.8)
            "secret_unloved_gem" -> {
                val m = allWatched.firstOrNull { m ->
                    val highRating = m.personalRating != null && m.personalRating!! >= 9.0
                    val lowAvg = (m.voteAverage ?: 0.0) in 0.1..5.8
                    highRating && lowAvg
                }
                if (m != null) listOf(m) else emptyList()
            }

            // Lost Reel #10: Il Critico Controcorrente (Voto personale <= 4.0 con media TMDB >= 8.3)
            "secret_harsh_critic" -> {
                val m = allWatched.firstOrNull { m ->
                    val lowPersonal = m.personalRating != null && m.personalRating!! <= 4.0
                    val highGlobal = (m.voteAverage ?: 0.0) >= 8.3
                    lowPersonal && highGlobal
                }
                if (m != null) listOf(m) else emptyList()
            }

            // Lost Reel #11: Amore a Prima Vista (visto entro 24h dall'aggiunta ai preferiti/watchlist)
            "secret_instant_watch" -> {
                val savedId = badgePrefs.getLong("instant_watch_movie_id", 0L)
                val m = if (savedId > 0L) {
                    allWatched.firstOrNull { it.id == savedId }
                } else {
                    allWatched.firstOrNull { m ->
                        val bookmarkedAt = badgePrefs.getLong("bookmarked_at_${m.id}", 0L)
                        val watchedMillis = m.watchedAt?.let { parseWatchedAtMillis(it) } ?: 0L
                        if (bookmarkedAt > 0L && watchedMillis > 0L) {
                            val diffHours = Math.abs(watchedMillis - bookmarkedAt) / (1000 * 60 * 60)
                            diffHours in 0..24
                        } else false
                    }
                }
                if (m != null) listOf(m) else emptyList()
            }

            // Gemma Nascosta: Voti TMDB <= 500 e voto personale >= 7.5 o preferito
            "badge_hidden_gem" -> {
                val m = watchedMovies.firstOrNull { m ->
                    val tmdbVotes = m.voteCount ?: 0
                    val isNiche = tmdbVotes in 1..500
                    val isLoved = (m.personalRating ?: 0.0) >= 7.5 || m.favorite
                    isNiche && isLoved
                }
                if (m != null) listOf(m) else emptyList()
            }

            // Colossal: Film con durata >= 210 minuti
            "colossal" -> {
                val m = watchedMovies.firstOrNull { (it.runtime ?: 0) >= 210 }
                if (m != null) listOf(m) else emptyList()
            }

            // 52. L'Occhio Meccanico: Film scoperto con IRIS o IA aggiunto alla watchlist
            "badge_iris_search" -> {
                val moodDiscoveredIds = getMoodDiscoveredIds()
                val m = allMovies.firstOrNull { movie ->
                    (movie.favorite || movie.reminder) && moodDiscoveredIds.contains(movie.id.toString())
                }
                if (m != null) listOf(m) else emptyList()
            }

            else -> emptyList()
        }
    }
}
