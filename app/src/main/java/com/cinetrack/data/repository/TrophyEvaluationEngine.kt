package com.cinetrack.data.repository

import android.content.Context
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.local.entities.UserBadgeEntity
import com.cinetrack.data.local.entities.WatchHistoryEntity
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.badge.PreviewBadgeItem
import com.cinetrack.ui.components.badge.PreviewBadgeTier
import com.cinetrack.ui.components.badge.ProgressiveTierDetail
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Risultato della valutazione analitica di un singolo trofeo (progresso, soglia, tier attivo e sblocco).
 */
data class TrophyEvaluationResult(
    val currentProgress: Int,
    val targetThreshold: Int,
    val activeTier: PreviewBadgeTier,
    val isUnlocked: Boolean
)

/**
 * Motore di calcolo puro e deterministico per la Sala dei Trofei.
 * Valuta in-memory i requisiti di tutti i 52 badge ed esegue il reverse-lookup
 * dei media sbloccanti senza dipendenze dirette dal database Room o dalla rete.
 */
@Singleton
class TrophyEvaluationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val traktAuthRepository: dagger.Lazy<TraktAuthRepository>,
    private val simklAuthRepository: dagger.Lazy<SimklAuthRepository>
) {
    private val badgePrefs by lazy {
        context.getSharedPreferences("flicktrove_badge_prefs", Context.MODE_PRIVATE)
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

                // userBadge provided as parameter
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
                val m = allWatched.firstOrNull { m ->
                    val bookmarkedAt = badgePrefs.getLong("bookmarked_at_${m.id}", 0L)
                    if (bookmarkedAt > 0L) {
                        val diffHours = (System.currentTimeMillis() - bookmarkedAt) / (1000 * 60 * 60)
                        diffHours in 0..24
                    } else false
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

            else -> emptyList()
        }
    }


    fun evaluateBadgeLogic(
        badge: PreviewBadgeItem,
        allMovies: List<Movie>,
        watchedMovies: List<Movie>,
        watchedShows: List<Movie>,
        allWatched: List<Movie>,
        allHistory: List<com.cinetrack.data.local.entities.WatchHistoryEntity>,
        allFolders: List<com.cinetrack.data.local.entities.FolderEntity>,
        prevEntity: UserBadgeEntity?
    ): TrophyEvaluationResult {
        return when (badge.id) {
            // ══════════════════════════════════════════════════════════════════
            // I 9 TROFEI SEGRETI (LOST REEL 🗝️)
            // ══════════════════════════════════════════════════════════════════

            // 1. Il Giorno della Marmotta: Due visioni dello stesso film in 48 ore
            "secret_groundhog_day" -> {
                var foundLoop = false
                val byMovie = allHistory.groupBy { it.movieId }
                for ((_, entries) in byMovie) {
                    if (entries.size >= 2) {
                        val sorted = entries.map { parseWatchedAtMillis(it.watchedAt) }.filter { it > 0L }.sorted()
                        for (i in 0 until sorted.size - 1) {
                            val diffHours = (sorted[i + 1] - sorted[i]) / (1000 * 60 * 60)
                            if (diffHours in 0..48) {
                                foundLoop = true
                                break
                            }
                        }
                    }
                    if (foundLoop) break
                }
                TrophyEvaluationResult(
                    currentProgress = if (foundLoop) 2 else 0,
                    targetThreshold = 2,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = foundLoop
                )
            }

            // 2. Roulette del Fato: Visto un film scoperto con Surprise Me
            "secret_surprise_fate" -> {
                val surpriseDiscoveredIds = badgePrefs.getStringSet("surprise_me_discovered_ids", emptySet()) ?: emptySet()
                val isWatchedFromSurprise = allWatched.any { surpriseDiscoveredIds.contains(it.id.toString()) } || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isWatchedFromSurprise) 1 else 0,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = isWatchedFromSurprise
                )
            }

            // 3. Creatura della Notte: Visione tra le 02:00 e le 05:00 del mattino
            "secret_night_owl" -> {
                val nightWatches = allHistory.count { history ->
                    val millis = parseWatchedAtMillis(history.watchedAt)
                    if (millis > 0L) {
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        val hour = cal.get(Calendar.HOUR_OF_DAY)
                        hour in 2..4
                    } else false
                }
                TrophyEvaluationResult(
                    currentProgress = nightWatches,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = nightWatches >= 1
                )
            }

            // 4. Crisi d'Identità: Film completato privo di generi TMDB
            "secret_genreless_rebel" -> {
                val count = allWatched.count { it.genreIds.isNullOrEmpty() }
                TrophyEvaluationResult(
                    currentProgress = count,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = count >= 1
                )
            }

            // 5. L'Eterno Dubbioso: Voto modificato almeno 3 volte sullo stesso film
            "secret_indecisive" -> {
                val count = badgePrefs.getInt("max_rating_changes_on_single_movie", prevEntity?.progressCurrent ?: 0)
                TrophyEvaluationResult(
                    currentProgress = count,
                    targetThreshold = 3,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = count >= 3
                )
            }

            // 6. Perfezionista Ossessivo: Voto, Nota e Vibe compilati contemporaneamente
            "secret_completionist" -> {
                val fullDetailWatches = allWatched.count { movie ->
                    movie.personalRating != null &&
                        !movie.personalNote.isNullOrBlank() &&
                        !movie.emotionalVibes.isNullOrBlank() && movie.emotionalVibes != "[]"
                }
                TrophyEvaluationResult(
                    currentProgress = fullDetailWatches,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = fullDetailWatches >= 1
                )
            }

            // 7. Origini del Mito: Opera realizzata prima del 1940
            "secret_genesis" -> {
                val pre1940Count = watchedMovies.count { movie ->
                    val year = movie.releaseDate?.take(4)?.toIntOrNull() ?: 2026
                    year in 1880..1939
                }
                TrophyEvaluationResult(
                    currentProgress = pre1940Count,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = pre1940Count >= 1
                )
            }

            // 8. Cuore d'Acciaio: 3 film horror completati senza voto negativo (< 5.0)
            "secret_iron_heart" -> {
                val horrorCount = allWatched.count { movie ->
                    val isHorror = movie.genreIds?.contains(27L) == true
                    val notDisliked = movie.personalRating == null || movie.personalRating!! >= 5.0
                    isHorror && notDisliked
                }
                TrophyEvaluationResult(
                    currentProgress = horrorCount,
                    targetThreshold = 3,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = horrorCount >= 3
                )
            }

            // 9. Gourmet Incompreso: Voto personale eccellente (>= 9.0) con media TMDB <= 5.8
            "secret_unloved_gem" -> {
                val gemsCount = allWatched.count { movie ->
                    val highRating = movie.personalRating != null && movie.personalRating!! >= 9.0
                    val lowAvg = (movie.voteAverage ?: 0.0) in 0.1..5.8
                    highRating && lowAvg
                }
                TrophyEvaluationResult(
                    currentProgress = gemsCount,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = gemsCount >= 1
                )
            }

            // 10. Il Critico Controcorrente: Voto personale <= 4.0 con media TMDB >= 8.3
            "secret_harsh_critic" -> {
                val harshCount = allWatched.count { movie ->
                    val lowPersonal = movie.personalRating != null && movie.personalRating!! <= 4.0
                    val highGlobal = (movie.voteAverage ?: 0.0) >= 8.3
                    lowPersonal && highGlobal
                }
                TrophyEvaluationResult(
                    currentProgress = harshCount,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = harshCount >= 1
                )
            }

            // 11. Lost Reel: Amore a Prima Vista (visto entro 24 ore dall'aggiunta in segnalibri)
            "secret_instant_watch" -> {
                val isUnlocked = badgePrefs.getBoolean("unlocked_instant_watch", false) || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) 1 else 0,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = isUnlocked
                )
            }

            // 12. Lost Reel: La Scala del Critico (assegnato almeno una volta ogni voto intero da 1 a 10)
            "secret_full_rating_spectrum" -> {
                val usedScores = allWatched.mapNotNull {
                    it.personalRating?.let { r -> kotlin.math.round(r).toInt().takeIf { s -> s in 1..10 } }
                }.toSet()
                val count = usedScores.size
                TrophyEvaluationResult(
                    currentProgress = count,
                    targetThreshold = 10,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = count >= 10
                )
            }

            // ══════════════════════════════════════════════════════════════════
            // I 12 BADGE PROGRESSIVI EVOLUTIVI
            // ══════════════════════════════════════════════════════════════════

            // 10. Film Visti: Frequenza 24fps
            "badge_movies" -> {
                calculateProgressiveTiers(watchedMovies.size, badge.progressiveSteps)
            }

            // 11. Serie TV Complete: Maratoneta Seriale
            "badge_tv" -> {
                calculateProgressiveTiers(watchedShows.size, badge.progressiveSteps)
            }

            // 12. Episodi in 24h: Maratona Notturna
            "badge_binge_episodes" -> {
                val maxEpsInDay = allHistory.groupBy { history ->
                    val millis = parseWatchedAtMillis(history.watchedAt)
                    if (millis > 0L) {
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"
                    } else {
                        history.watchedAt.take(10)
                    }
                }.values.maxOfOrNull { it.size } ?: 0
                calculateProgressiveTiers(maxEpsInDay, badge.progressiveSteps)
            }

            // 13. Watchlist: L'Archivio Infinito
            "badge_watchlist_hoarder" -> {
                val watchlistCount = allMovies.count { !it.watched && !it.dropped }
                calculateProgressiveTiers(watchlistCount, badge.progressiveSteps)
            }

            // 14. Longevità Cineclub: Tessera del Cineclub (Esclusivo per utenti registrati con account)
            "badge_veteran_club" -> {
                val currentUser = auth.currentUser
                val isRegisteredUser = currentUser != null && !currentUser.isAnonymous
                if (!isRegisteredUser) {
                    calculateProgressiveTiers(0, badge.progressiveSteps)
                } else {
                    val registrationTime = currentUser.metadata?.creationTimestamp?.takeIf { it > 0L }
                        ?: System.currentTimeMillis()
                    val diffMillis = (System.currentTimeMillis() - registrationTime).coerceAtLeast(0L)
                    val months = ((diffMillis / (1000L * 60 * 60 * 24 * 30L)) + 1).toInt().coerceAtLeast(1)
                    calculateProgressiveTiers(months, badge.progressiveSteps)
                }
            }

            // 15. Ore di Visione: Odissea del Tempo
            "badge_time" -> {
                val totalMinutes = watchedMovies.sumOf { (it.runtime ?: 100) } +
                    watchedShows.sumOf { show ->
                        val epCount = show.watchedEpisodes?.values?.sumOf { it.size } ?: (show.numberOfEpisodes ?: 10)
                        (show.runtime ?: 45) * epCount
                    }
                val hours = (totalMinutes / 60).coerceAtLeast(0)
                calculateProgressiveTiers(hours, badge.progressiveSteps)
            }

            // 16. Saghe al 100%: Signore delle Saghe
            "badge_sagas" -> {
                val count = badgePrefs.getStringSet("completed_collection_ids", emptySet())?.size
                    ?: (prevEntity?.progressCurrent ?: 0)
                calculateProgressiveTiers(count, badge.progressiveSteps)
            }

            // 17. Déjà Vu: Rewatch cinematografici
            "badge_rewatch" -> {
                val rewatchCount = allHistory.groupBy { it.movieId }.values.sumOf {
                    if (it.size > 1) it.size - 1 else 0
                }
                calculateProgressiveTiers(rewatchCount, badge.progressiveSteps)
            }

            // 18. La Giuria: Voti Assegnati
            "badge_ratings" -> {
                val ratedCount = allWatched.count { it.personalRating != null && it.personalRating!! > 0.0 }
                calculateProgressiveTiers(ratedCount, badge.progressiveSteps)
            }

            // 19. Spettro Emozionale: Emotional Vibes
            "badge_vibes" -> {
                val vibesCount = allWatched.count { !it.emotionalVibes.isNullOrBlank() && it.emotionalVibes != "[]" }
                calculateProgressiveTiers(vibesCount, badge.progressiveSteps)
            }

            // 20. Community: Voce della Critica
            "badge_comments" -> {
                val commentsCount = badgePrefs.getInt("comments_written_count", prevEntity?.progressCurrent ?: 0)
                calculateProgressiveTiers(commentsCount, badge.progressiveSteps)
            }

            // 21. Curatore d'Autore: Cartelle create
            "badge_folders" -> {
                calculateProgressiveTiers(allFolders.size, badge.progressiveSteps)
            }

            // 22. Prime Visioni: Prima Visione Assoluta (film visti il giorno esatto di uscita nel fuso locale)
            "badge_day_one_premiere", "secret_day_one_premiere" -> {
                val dayOneCount = allWatched.count { movie ->
                    val relDateStr = (movie.releaseDate ?: movie.firstAirDate)?.take(10) ?: return@count false
                    val relDate = runCatching { LocalDate.parse(relDateStr) }.getOrNull() ?: return@count false
                    val watchedLocal = parseToLocalDate(movie.watchedAt)
                    if (watchedLocal != null && watchedLocal == relDate) return@count true
                    allHistory.any { h ->
                        h.movieId == movie.id && parseToLocalDate(h.watchedAt) == relDate
                    }
                }
                calculateProgressiveTiers(dayOneCount, badge.progressiveSteps)
            }

            // ══════════════════════════════════════════════════════════════════
            // I 7 BADGE ESPLORAZIONE GENERI & EPOCHE
            // ══════════════════════════════════════════════════════════════════

            // 22. Horror: Notte delle Ombre (Genere 27)
            "genre_horror" -> {
                val horrorCount = allWatched.count { it.genreIds?.contains(27L) == true }
                calculateProgressiveTiers(horrorCount, badge.progressiveSteps)
            }

            // 23. Sci-Fi: Oltre l'Atmosfera (Genere 878)
            "genre_scifi" -> {
                val scifiCount = allWatched.count { it.genreIds?.contains(878L) == true }
                calculateProgressiveTiers(scifiCount, badge.progressiveSteps)
            }

            // 24. Thriller/Noir: Indagine a Mezzanotte (Generi 80, 9648, 53)
            "genre_thriller" -> {
                val thrillerCount = allWatched.count { movie ->
                    movie.genreIds?.any { it in listOf(80L, 9648L, 53L) } == true
                }
                calculateProgressiveTiers(thrillerCount, badge.progressiveSteps)
            }

            // 25. Animazione: Mondi Disegnati (Genere 16)
            "genre_anime" -> {
                val animeCount = allWatched.count { it.genreIds?.contains(16L) == true }
                calculateProgressiveTiers(animeCount, badge.progressiveSteps)
            }

            // 26. Documentari: Occhio del Reale (Genere 99)
            "genre_doc" -> {
                val docCount = allWatched.count { it.genreIds?.contains(99L) == true }
                calculateProgressiveTiers(docCount, badge.progressiveSteps)
            }

            // 27. Cinema d'Epoca: Archeologo della Bobina (Pre-1980)
            "genre_vintage" -> {
                val vintageCount = watchedMovies.count { movie ->
                    val year = movie.releaseDate?.take(4)?.toIntOrNull() ?: 2026
                    year < 1980
                }
                calculateProgressiveTiers(vintageCount, badge.progressiveSteps)
            }

            // 28. Cinema Mondiale: Passaporto Globale
            "world_tour" -> {
                val distinctCountries = watchedMovies.flatMap { it.originCountry ?: emptyList() }
                    .filter { it.isNotBlank() }
                    .distinct().size
                calculateProgressiveTiers(distinctCountries, badge.progressiveSteps)
            }

            // 29. Commedia: L'Arte della Risata (Genere 35)
            "genre_comedy" -> {
                val comedyCount = allWatched.count { it.genreIds?.contains(35L) == true }
                calculateProgressiveTiers(comedyCount, badge.progressiveSteps)
            }

            // 30. Western: Mezzogiorno di Fuoco (Genere 37)
            "genre_western" -> {
                val westernCount = allWatched.count { it.genreIds?.contains(37L) == true }
                calculateProgressiveTiers(westernCount, badge.progressiveSteps)
            }

            // ══════════════════════════════════════════════════════════════════
            // GLI 8 TRAGUARDI SINGOLI DI PRESTIGIO & ONORIFICENZE
            // ══════════════════════════════════════════════════════════════════

            // 29. Day One Pioneer: Pioniere Prima Bobina
            "badge_day_one_pioneer" -> {
                // Cutoff era di lancio: 31 Dicembre 2026 23:59:59 UTC
                val launchEraCutoffMillis = 1798761600000L
                val installTime = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.getPackageInfo(
                            context.packageName,
                            android.content.pm.PackageManager.PackageInfoFlags.of(0)
                        ).firstInstallTime
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
                    }
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                val isPioneer = prevEntity?.isUnlocked == true || installTime <= launchEraCutoffMillis
                TrophyEvaluationResult(
                    currentProgress = if (isPioneer) 1 else 0,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.THE_FINAL_CUT,
                    isUnlocked = isPioneer
                )
            }

            // 30. Sostenitore & Mecenate: Il Produttore Esecutivo
            "badge_executive_producer" -> {
                val isSupporter = badgePrefs.getBoolean("is_supporter_verified", false) ||
                    (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isSupporter) 1 else 0,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.THE_FINAL_CUT,
                    isUnlocked = isSupporter
                )
            }

            // 31. Cloud Sync: Il Trasloco
            "badge_onboarding_sync" -> {
                val isSynced = (traktAuthRepository.get().isLoggedIn.value) ||
                    (simklAuthRepository.get().getAccessToken() != null) ||
                    (prevEntity?.isUnlocked == true) ||
                    badgePrefs.getBoolean("has_imported_backup", false)
                TrophyEvaluationResult(
                    currentProgress = if (isSynced) 1 else 0,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.MM_16,
                    isUnlocked = isSynced
                )
            }

            // 32. TMDB > 8.5: Gourmet del Cinema (10 capolavori)
            "gourmet_critic" -> {
                val count = watchedMovies.count { (it.voteAverage ?: 0.0) >= 8.5 }
                TrophyEvaluationResult(
                    currentProgress = count,
                    targetThreshold = 10,
                    activeTier = PreviewBadgeTier.MM_70,
                    isUnlocked = count >= 10
                )
            }

            // 33. Durata > 3h30m (210 min): Titanico
            "colossal" -> {
                val count = watchedMovies.count { (it.runtime ?: 0) >= 210 }
                TrophyEvaluationResult(
                    currentProgress = count,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.MM_70,
                    isUnlocked = count >= 1
                )
            }

            // 34. Attore Preferito: Attore Feticcio (8 check-in)
            "actor_muse" -> {
                val topActorCount = allWatched.filter { it.favoriteActorId != null }
                    .groupBy { it.favoriteActorId }
                    .values.maxOfOrNull { it.size } ?: 0
                TrophyEvaluationResult(
                    currentProgress = topActorCount,
                    targetThreshold = 8,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = topActorCount >= 8
                )
            }

            // 35. 8 Film Stesso Regista: Regista del Cuore (8 film)
            "director_heart" -> {
                val topDirectorCount = allWatched.filter { it.directorId != null }
                    .groupBy { it.directorId }
                    .values.maxOfOrNull { it.size } ?: 0
                TrophyEvaluationResult(
                    currentProgress = topDirectorCount,
                    targetThreshold = 8,
                    activeTier = PreviewBadgeTier.MM_70,
                    isUnlocked = topDirectorCount >= 8
                )
            }

            // 36. Note Personali: Diario di Bordo (10 note)
            "personal_diary" -> {
                val notesCount = allWatched.count { !it.personalNote.isNullOrBlank() }
                TrophyEvaluationResult(
                    currentProgress = notesCount,
                    targetThreshold = 10,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = notesCount >= 10
                )
            }

            // 37. Backdrop & Avatar: Scenografo Personale (Target = 2: Avatar + Backdrop)
            "custom_backdrop" -> {
                val currentUid = auth.currentUser?.uid
                val userNamePrefs = context.getSharedPreferences("user_name_changes", android.content.Context.MODE_PRIVATE)
                val commsPrefs = context.getSharedPreferences("commsuni_prefs", android.content.Context.MODE_PRIVATE)

                val hasAvatar = !currentUid.isNullOrBlank() && (
                    auth.currentUser?.photoUrl != null ||
                    !userNamePrefs.getString("user_avatar_$currentUid", null).isNullOrBlank() ||
                    !commsPrefs.getString("user_avatar_$currentUid", null).isNullOrBlank()
                )

                val hasProfileBackdrop = !currentUid.isNullOrBlank() && (
                    !userNamePrefs.getString("avatar_backdrop_$currentUid", null).isNullOrBlank()
                )

                val avatarScore = if (hasAvatar) 1 else 0
                val backdropScore = if (hasProfileBackdrop) 1 else 0
                val totalCustomScore = avatarScore + backdropScore

                TrophyEvaluationResult(
                    currentProgress = totalCustomScore,
                    targetThreshold = 2,
                    activeTier = PreviewBadgeTier.SUPER_8,
                    isUnlocked = totalCustomScore >= 2
                )
            }

            // 41. Miniserie TV: Il Romanzo Televisivo (5 miniserie completate con 1 sola stagione)
            "badge_miniseries" -> {
                val miniseriesCount = watchedShows.count { show ->
                    show.numberOfSeasons == 1 || (show.seasons?.count { s -> s.seasonNumber > 0 } == 1)
                }
                TrophyEvaluationResult(
                    currentProgress = miniseriesCount.coerceAtMost(5),
                    targetThreshold = 5,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = miniseriesCount >= 5
                )
            }

            // 43. Profondità Cartelle: La Collezione Definitiva (almeno 1 cartella con >= 25 titoli)
            "badge_folder_deep" -> {
                val maxFolderItems = allFolders.maxOfOrNull { it.itemIds.distinct().size } ?: 0
                TrophyEvaluationResult(
                    currentProgress = maxFolderItems.coerceAtMost(25),
                    targetThreshold = 25,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = maxFolderItems >= 25
                )
            }

            // Co-Regia: Cartella condivisa con almeno un altro cinefilo
            "badge_co_director" -> {
                val currentUid = auth.currentUser?.uid
                val hasJoinedSharedFolder = allFolders.any { it.isShared && !it.ownerUid.isNullOrBlank() && it.ownerUid != currentUid }
                val hasCollaborators = currentUid != null && badgePrefs.getBoolean("has_collaborator_$currentUid", false)
                val isCoDirector = hasJoinedSharedFolder || hasCollaborators || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isCoDirector) 1 else 0,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = isCoDirector
                )
            }

            // 44. Attraverso i Decenni: La Macchina del Tempo (almeno 1 film visto per 7 decenni diversi)
            "badge_time_machine" -> {
                val distinctDecades = watchedMovies.mapNotNull { movie ->
                    val year = movie.releaseDate?.take(4)?.toIntOrNull()
                    if (year != null && year in 1880..2099) (year / 10) * 10 else null
                }.distinct().size
                TrophyEvaluationResult(
                    currentProgress = distinctDecades,
                    targetThreshold = 7,
                    activeTier = PreviewBadgeTier.MM_70,
                    isUnlocked = distinctDecades >= 7
                )
            }

            // 45. Cinema Contemporaneo: Fresco di Stampa (5 film visti dell'anno in corso)
            "badge_current_year" -> {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
                val currentYearCount = watchedMovies.count { it.releaseDate?.take(4) == currentYear }
                TrophyEvaluationResult(
                    currentProgress = currentYearCount.coerceAtMost(5),
                    targetThreshold = 5,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = currentYearCount >= 5
                )
            }

            // 48. Doppio Spettacolo: 2 film diversi nello stesso giorno solare
            "badge_double_feature" -> {
                val movieIds = watchedMovies.map { it.id }.toSet()
                val dayToMovieIds = mutableMapOf<String, MutableSet<Long>>()
                for (history in allHistory) {
                    if (history.movieId in movieIds) {
                        val dayKey = history.watchedAt.take(10)
                        if (dayKey.isNotBlank()) {
                            dayToMovieIds.getOrPut(dayKey) { mutableSetOf() }.add(history.movieId)
                        }
                    }
                }
                for (movie in watchedMovies) {
                    val dayKey = movie.watchedAt?.take(10)
                    if (!dayKey.isNullOrBlank()) {
                        dayToMovieIds.getOrPut(dayKey) { mutableSetOf() }.add(movie.id)
                    }
                }
                val maxMoviesInSingleDay = dayToMovieIds.values.maxOfOrNull { it.size } ?: 0
                TrophyEvaluationResult(
                    currentProgress = maxMoviesInSingleDay.coerceAtMost(2),
                    targetThreshold = 2,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = maxMoviesInSingleDay >= 2
                )
            }

            // 49. Gemma Nascosta: Film con TMDB voteCount <= 500 apprezzato dall'utente (voto >= 7.5 o Preferito)
            "badge_hidden_gem" -> {
                val hiddenGemsCount = watchedMovies.count { movie ->
                    val tmdbVotes = movie.voteCount ?: 0
                    val isNiche = tmdbVotes in 1..500
                    val isLoved = (movie.personalRating ?: 0.0) >= 7.5 || movie.favorite
                    isNiche && isLoved
                }
                TrophyEvaluationResult(
                    currentProgress = hiddenGemsCount.coerceAtMost(1),
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.MM_70,
                    isUnlocked = hiddenGemsCount >= 1
                )
            }

            // 50. Pillole di Cinema: 5 cortometraggi o film con durata <= 45 minuti
            "badge_short_films" -> {
                val shortCount = watchedMovies.count { movie ->
                    val rt = movie.runtime ?: 0
                    rt in 1..45
                }
                TrophyEvaluationResult(
                    currentProgress = shortCount.coerceAtMost(5),
                    targetThreshold = 5,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = shortCount >= 5
                )
            }

            // 52. Meta Trofeo: Il Cacciatore di Trofei (avanza con lo sblocco degli altri trofei)
            "badge_trophy_hunter" -> {
                val curr = prevEntity?.progressCurrent ?: 0
                calculateProgressiveTiers(curr, badge.progressiveSteps)
            }

            // Default

            else -> {
                val target = badge.progressTarget.coerceAtLeast(1)
                val curr = prevEntity?.progressCurrent ?: 0
                val unl = prevEntity?.isUnlocked ?: (curr >= target)
                val tier = prevEntity?.currentTier?.let { parseTierSafe(it, badge.fixedTier ?: PreviewBadgeTier.SUPER_8) }
                    ?: (badge.fixedTier ?: PreviewBadgeTier.SUPER_8)
                TrophyEvaluationResult(
                    currentProgress = curr,
                    targetThreshold = target,
                    activeTier = tier,
                    isUnlocked = unl
                )
            }
        }
    }

    fun calculateProgressiveTiers(
        currentValue: Int,
        steps: Map<PreviewBadgeTier, ProgressiveTierDetail>
    ): TrophyEvaluationResult {
        if (steps.isEmpty()) {
            return TrophyEvaluationResult(currentValue, 1, PreviewBadgeTier.SUPER_8, currentValue >= 1)
        }

        val sortedTiers = listOf(
            PreviewBadgeTier.SUPER_8,
            PreviewBadgeTier.MM_16,
            PreviewBadgeTier.MM_35,
            PreviewBadgeTier.MM_70,
            PreviewBadgeTier.THE_FINAL_CUT
        )

        var highestAchievedTier: PreviewBadgeTier = PreviewBadgeTier.SUPER_8
        var isUnlocked = false
        var nextTarget = steps[PreviewBadgeTier.SUPER_8]?.targetThreshold ?: 1

        for (tier in sortedTiers) {
            val step = steps[tier] ?: continue
            if (currentValue >= step.targetThreshold) {
                highestAchievedTier = tier
                isUnlocked = true
            } else {
                nextTarget = step.targetThreshold
                break
            }
        }

        // Se ha raggiunto l'ultimo tier disponibile, il target è la soglia finale massima
        val maxAvailableTier = steps.keys.maxByOrNull { it.ordinal } ?: PreviewBadgeTier.THE_FINAL_CUT
        if (highestAchievedTier == maxAvailableTier) {
            nextTarget = steps[maxAvailableTier]?.targetThreshold ?: nextTarget
        }

        return TrophyEvaluationResult(
            currentProgress = currentValue,
            targetThreshold = nextTarget,
            activeTier = highestAchievedTier,
            isUnlocked = isUnlocked
        )
    }

    fun parseTierSafe(tierStr: String, fallback: PreviewBadgeTier): PreviewBadgeTier {
        return try {
            PreviewBadgeTier.valueOf(tierStr)
        } catch (_: Exception) {
            fallback
        }
    }

    fun isHigherTier(candidate: PreviewBadgeTier, current: PreviewBadgeTier): Boolean {
        val tierRank = mapOf(
            PreviewBadgeTier.SUPER_8 to 1,
            PreviewBadgeTier.MM_16 to 2,
            PreviewBadgeTier.MM_35 to 3,
            PreviewBadgeTier.MM_70 to 4,
            PreviewBadgeTier.THE_FINAL_CUT to 5,
            PreviewBadgeTier.LOST_REEL to 6
        )
        return (tierRank[candidate] ?: 0) > (tierRank[current] ?: 0)
    }

    fun parseToLocalDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        return try {
            Instant.parse(trimmed).atZone(ZoneId.systemDefault()).toLocalDate()
        } catch (_: Exception) {
            try {
                if (trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-') {
                    LocalDate.parse(trimmed.take(10))
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    fun parseWatchedAtMillis(dateStr: String): Long {
        return com.cinetrack.util.DateTimeUtils.parseIsoDateToMillis(dateStr)
    }

}
