package com.cinetrack.data.repository

import android.content.Context
import com.cinetrack.data.local.entities.UserBadgeEntity
import com.cinetrack.data.local.entities.WatchHistoryEntity
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.badge.PreviewBadgeTier
import com.cinetrack.util.DateTimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegate responsible for evaluating the 12 secret Lost Reel trophies.
 */
@Singleton
class TrophyLostReelDelegate @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val badgePrefs by lazy {
        context.getSharedPreferences("flicktrove_badge_prefs", Context.MODE_PRIVATE)
    }

    private fun parseWatchedAtMillis(dateStr: String): Long {
        return DateTimeUtils.parseIsoDateToMillis(dateStr)
    }

    fun evaluateSecretBadge(
        badgeId: String,
        watchedMovies: List<Movie>,
        allWatched: List<Movie>,
        allHistory: List<WatchHistoryEntity>,
        prevEntity: UserBadgeEntity?
    ): TrophyEvaluationResult? {
        return when (badgeId) {
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
                val isUnlocked = foundLoop || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) 2 else 0,
                    targetThreshold = 2,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = isUnlocked
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
                val isUnlocked = nightWatches >= 1 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(nightWatches, 1) else nightWatches,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = isUnlocked
                )
            }

            // 4. Crisi d'Identità: Film completato privo di generi TMDB
            "secret_genreless_rebel" -> {
                val count = allWatched.count { it.genreIds.isNullOrEmpty() }
                val isUnlocked = count >= 1 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(count, 1) else count,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = isUnlocked
                )
            }

            // 5. L'Eterno Dubbioso: Voto modificato almeno 3 volte sullo stesso film
            "secret_indecisive" -> {
                val count = badgePrefs.getInt("max_rating_changes_on_single_movie", prevEntity?.progressCurrent ?: 0)
                val isUnlocked = count >= 3 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(count, 3) else count,
                    targetThreshold = 3,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = isUnlocked
                )
            }

            // 6. Perfezionista Ossessivo: Voto, Nota e Vibe compilati contemporaneamente
            "secret_completionist" -> {
                val fullDetailWatches = allWatched.count { movie ->
                    movie.personalRating != null &&
                        !movie.personalNote.isNullOrBlank() &&
                        !movie.emotionalVibes.isNullOrBlank() && movie.emotionalVibes != "[]"
                }
                val isUnlocked = fullDetailWatches >= 1 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(fullDetailWatches, 1) else fullDetailWatches,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = isUnlocked
                )
            }

            // 7. Origini del Mito: Opera realizzata prima del 1940
            "secret_genesis" -> {
                val pre1940Count = watchedMovies.count { movie ->
                    val year = movie.releaseDate?.take(4)?.toIntOrNull() ?: 2026
                    year in 1880..1939
                }
                val isUnlocked = pre1940Count >= 1 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(pre1940Count, 1) else pre1940Count,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = isUnlocked
                )
            }

            // 8. Cuore d'Acciaio: 3 film horror completati senza voto negativo (< 5.0)
            "secret_iron_heart" -> {
                val horrorCount = allWatched.count { movie ->
                    val isHorror = movie.genreIds?.contains(27L) == true
                    val notDisliked = movie.personalRating == null || movie.personalRating!! >= 5.0
                    isHorror && notDisliked
                }
                val isUnlocked = horrorCount >= 3 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(horrorCount, 3) else horrorCount,
                    targetThreshold = 3,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = isUnlocked
                )
            }

            // 9. Gourmet Incompreso: Voto personale eccellente (>= 9.0) con media TMDB <= 5.8
            "secret_unloved_gem" -> {
                val gemsCount = allWatched.count { movie ->
                    val highRating = movie.personalRating != null && movie.personalRating!! >= 9.0
                    val lowAvg = (movie.voteAverage ?: 0.0) in 0.1..5.8
                    highRating && lowAvg
                }
                val isUnlocked = gemsCount >= 1 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(gemsCount, 1) else gemsCount,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = isUnlocked
                )
            }

            // 10. Il Critico Controcorrente: Voto personale <= 4.0 con media TMDB >= 8.3
            "secret_harsh_critic" -> {
                val harshCount = allWatched.count { movie ->
                    val lowPersonal = movie.personalRating != null && movie.personalRating!! <= 4.0
                    val highGlobal = (movie.voteAverage ?: 0.0) >= 8.3
                    lowPersonal && highGlobal
                }
                val isUnlocked = harshCount >= 1 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(harshCount, 1) else harshCount,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = isUnlocked
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
                val isUnlocked = count >= 10 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(count, 10) else count,
                    targetThreshold = 10,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = isUnlocked
                )
            }

            else -> null
        }
    }
}
