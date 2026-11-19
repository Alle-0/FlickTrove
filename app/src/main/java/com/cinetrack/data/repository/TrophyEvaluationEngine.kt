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
import kotlinx.serialization.json.Json

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

    private val trophyMediaLookupDelegate by lazy {
        TrophyMediaLookupDelegate(context)
    }

    private val trophyLostReelDelegate by lazy {
        TrophyLostReelDelegate(context)
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
    ): List<Movie> = trophyMediaLookupDelegate.findUnlockingMoviesForBadge(
        badgeId = badgeId,
        allMovies = allMovies,
        allHistory = allHistory,
        userBadge = userBadge
    )


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
        if (badge.id.startsWith("secret_")) {
            val secretResult = trophyLostReelDelegate.evaluateSecretBadge(
                badgeId = badge.id,
                watchedMovies = watchedMovies,
                allWatched = allWatched,
                allHistory = allHistory,
                prevEntity = prevEntity
            )
            if (secretResult != null) return secretResult
        }

        return when (badge.id) {
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
                val dayToEpisodeCount = mutableMapOf<String, Int>()
                for (history in allHistory) {
                    var parsedEpisodes = 0
                    if (!history.episodeDates.isNullOrBlank()) {
                        try {
                            val datesMap = Json.decodeFromString<Map<String, String>>(history.episodeDates)
                            for ((_, dateStr) in datesMap) {
                                val dayKey = toLocalDayKey(dateStr)
                                if (!dayKey.isNullOrBlank()) {
                                    dayToEpisodeCount[dayKey] = (dayToEpisodeCount[dayKey] ?: 0) + 1
                                    parsedEpisodes++
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    if (parsedEpisodes == 0 && history.mediaType == "tv") {
                        val dayKey = toLocalDayKey(history.watchedAt)
                        if (!dayKey.isNullOrBlank()) {
                            dayToEpisodeCount[dayKey] = (dayToEpisodeCount[dayKey] ?: 0) + 1
                        }
                    }
                }
                val maxEpsInDay = dayToEpisodeCount.values.maxOrNull() ?: 0
                val progress = maxOf(maxEpsInDay, prevEntity?.progressCurrent ?: 0)
                calculateProgressiveTiers(progress, badge.progressiveSteps)
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
                val isSupporter = badgePrefs.getBoolean("is_supporter_verified", false)
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
                val isUnlocked = count >= 10 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(count, 10) else count,
                    targetThreshold = 10,
                    activeTier = PreviewBadgeTier.MM_70,
                    isUnlocked = isUnlocked
                )
            }

            // 33. Durata > 3h30m (210 min): Titanico
            "colossal" -> {
                val count = watchedMovies.count { (it.runtime ?: 0) >= 210 }
                val isUnlocked = count >= 1 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(count, 1) else count,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.MM_70,
                    isUnlocked = isUnlocked
                )
            }

            // 34. Attore Preferito: Attore Feticcio (8 check-in)
            "actor_muse" -> {
                val topActorCount = allWatched.filter { it.favoriteActorId != null }
                    .groupBy { it.favoriteActorId }
                    .values.maxOfOrNull { it.size } ?: 0
                val isUnlocked = topActorCount >= 8 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(topActorCount, 8) else topActorCount,
                    targetThreshold = 8,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = isUnlocked
                )
            }

            // 35. 8 Film Stesso Regista: Regista del Cuore (8 film)
            "director_heart" -> {
                val topDirectorCount = allWatched.filter { it.directorId != null }
                    .groupBy { it.directorId }
                    .values.maxOfOrNull { it.size } ?: 0
                val isUnlocked = topDirectorCount >= 8 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(topDirectorCount, 8) else topDirectorCount,
                    targetThreshold = 8,
                    activeTier = PreviewBadgeTier.MM_70,
                    isUnlocked = isUnlocked
                )
            }

            // 36. Note Personali: Diario di Bordo (10 note)
            "personal_diary" -> {
                val notesCount = allWatched.count { !it.personalNote.isNullOrBlank() }
                val isUnlocked = notesCount >= 10 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(notesCount, 10) else notesCount,
                    targetThreshold = 10,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = isUnlocked
                )
            }

            // 37. Backdrop & Avatar: Scenografo Personale (Target = 2: Avatar + Backdrop)
            "custom_backdrop" -> {
                val currentUid = auth.currentUser?.uid
                val userNamePrefs = context.getSharedPreferences("user_name_changes", Context.MODE_PRIVATE)
                val commsPrefs = context.getSharedPreferences("commsuni_prefs", Context.MODE_PRIVATE)

                val hasAvatar = (
                    auth.currentUser?.photoUrl != null ||
                    (!currentUid.isNullOrBlank() && !userNamePrefs.getString("user_avatar_$currentUid", null).isNullOrBlank()) ||
                    (!currentUid.isNullOrBlank() && !commsPrefs.getString("user_avatar_$currentUid", null).isNullOrBlank()) ||
                    userNamePrefs.all.keys.any { it.startsWith("user_avatar_") && !userNamePrefs.getString(it, null).isNullOrBlank() } ||
                    commsPrefs.all.keys.any { it.startsWith("user_avatar_") && !commsPrefs.getString(it, null).isNullOrBlank() }
                )

                val hasProfileBackdrop = (
                    (!currentUid.isNullOrBlank() && !userNamePrefs.getString("avatar_backdrop_$currentUid", null).isNullOrBlank()) ||
                    userNamePrefs.all.keys.any { it.startsWith("avatar_backdrop_") && !userNamePrefs.getString(it, null).isNullOrBlank() }
                )

                val avatarScore = if (hasAvatar) 1 else 0
                val backdropScore = if (hasProfileBackdrop) 1 else 0
                val totalCustomScore = avatarScore + backdropScore
                val isUnlocked = totalCustomScore >= 2 || (prevEntity?.isUnlocked == true)

                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) 2 else totalCustomScore,
                    targetThreshold = 2,
                    activeTier = PreviewBadgeTier.SUPER_8,
                    isUnlocked = isUnlocked
                )
            }

            // 41. Miniserie TV: Il Romanzo Televisivo (5 miniserie completate con 1 sola stagione)
            "badge_miniseries" -> {
                val miniseriesCount = watchedShows.count { show ->
                    show.numberOfSeasons == 1 || (show.seasons?.count { s -> s.seasonNumber > 0 } == 1)
                }
                val isUnlocked = miniseriesCount >= 5 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) 5 else miniseriesCount.coerceAtMost(5),
                    targetThreshold = 5,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = isUnlocked
                )
            }

            // 43. Profondità Cartelle: La Collezione Definitiva (almeno 1 cartella con >= 25 titoli)
            "badge_folder_deep" -> {
                val maxFolderItems = allFolders.maxOfOrNull { it.itemIds.distinct().size } ?: 0
                val isUnlocked = maxFolderItems >= 25 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) 25 else maxFolderItems.coerceAtMost(25),
                    targetThreshold = 25,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = isUnlocked
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
                val isUnlocked = distinctDecades >= 7 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) maxOf(distinctDecades, 7) else distinctDecades,
                    targetThreshold = 7,
                    activeTier = PreviewBadgeTier.MM_70,
                    isUnlocked = isUnlocked
                )
            }

            // 45. Cinema Contemporaneo: Fresco di Stampa (5 film visti dell'anno in corso)
            "badge_current_year" -> {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
                val currentYearCount = watchedMovies.count { it.releaseDate?.take(4) == currentYear }
                val isUnlocked = currentYearCount >= 5 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) 5 else currentYearCount.coerceAtMost(5),
                    targetThreshold = 5,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = isUnlocked
                )
            }

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
                val maxMoviesInSingleDay = dayToMovieIds.values.maxOfOrNull { it.size } ?: 0
                val isUnlocked = maxMoviesInSingleDay >= 2 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) 2 else maxMoviesInSingleDay.coerceAtMost(2),
                    targetThreshold = 2,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = isUnlocked
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
                val isUnlocked = hiddenGemsCount >= 1 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) 1 else hiddenGemsCount.coerceAtMost(1),
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.MM_70,
                    isUnlocked = isUnlocked
                )
            }

            // 50. Pillole di Cinema: 5 cortometraggi o film con durata <= 45 minuti
            "badge_short_films" -> {
                val shortCount = watchedMovies.count { movie ->
                    val rt = movie.runtime ?: 0
                    rt in 1..45
                }
                val isUnlocked = shortCount >= 5 || (prevEntity?.isUnlocked == true)
                TrophyEvaluationResult(
                    currentProgress = if (isUnlocked) 5 else shortCount.coerceAtMost(5),
                    targetThreshold = 5,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = isUnlocked
                )
            }

            // 52. L'Occhio Meccanico: Aggiungi alla watchlist un film scoperto con IRIS o IA
            "badge_iris_search" -> {
                val moodDiscoveredIds = getMoodDiscoveredIds()
                val isAddedToWatchlist = allMovies.any { movie ->
                    (movie.favorite || movie.reminder) && moodDiscoveredIds.contains(movie.id.toString())
                } || (prevEntity?.isUnlocked == true)

                TrophyEvaluationResult(
                    currentProgress = if (isAddedToWatchlist) 1 else 0,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = isAddedToWatchlist
                )
            }

            // 53. Meta Trofeo: Il Cacciatore di Trofei (avanza con lo sblocco degli altri trofei)
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

    fun parseToLocalDate(raw: String?): LocalDate? =
        com.cinetrack.util.DateTimeUtils.parseDateToLocalDate(raw)

    fun parseWatchedAtMillis(dateStr: String): Long {
        return com.cinetrack.util.DateTimeUtils.parseIsoDateToMillis(dateStr)
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
}
