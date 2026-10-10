package com.cinetrack.data.repository

import com.cinetrack.data.local.dao.BadgeDao
import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.local.dao.FolderDao
import com.cinetrack.data.local.dao.WatchHistoryDao
import com.cinetrack.data.local.entities.BadgeGlobalStatsEntity
import com.cinetrack.data.local.entities.UserBadgeEntity
import com.cinetrack.data.local.entities.UserPersonFrequencyEntity
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.badge.BadgeTypeCategory
import com.cinetrack.ui.components.badge.CustomBadgeIconKind
import com.cinetrack.ui.components.badge.PreviewBadgeItem
import com.cinetrack.ui.components.badge.PreviewBadgeTier
import com.cinetrack.ui.components.badge.ProgressiveTierDetail
import com.cinetrack.ui.components.badge.OFFICIAL_TROPHY_ROOM_CATALOG
import com.cinetrack.ui.components.badge.TrophyRoomItemUi
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evento emesso quando l'utente raggiunge una nuova milestone o sblocca un tier trofeo.
 * Catturato dalla UI per mostrare il banner celebrativo stile PlayStation / Xbox.
 */
data class BadgeTierUnlockedEvent(
    val badgeId: String,
    val title: String,
    val tier: PreviewBadgeTier,
    val category: BadgeTypeCategory,
    val iconKind: CustomBadgeIconKind,
    val milestonePhrase: String,
    val isFirstUnlock: Boolean
)

@Singleton
class BadgeRepository @Inject constructor(
    private val badgeDao: BadgeDao,
    private val favoriteDao: FavoriteDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val folderDao: FolderDao,
    private val firestore: FirebaseFirestore
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _unlockedTierEvents = MutableSharedFlow<BadgeTierUnlockedEvent>(extraBufferCapacity = 8)
    val unlockedTierEvents: SharedFlow<BadgeTierUnlockedEvent> = _unlockedTierEvents.asSharedFlow()

    /**
     * Flusso reattivo principale per la Sala dei Trofei.
     * Fonde il catalogo ufficiale con:
     * 1. Lo stato di conquista personale salvato su Room (`user_badges`).
     * 2. Le percentuali di rarità globale salvate su Room (`badge_global_stats`).
     */
    fun getTrophyRoomItemsFlow(): Flow<List<TrophyRoomItemUi>> {
        return combine(
            badgeDao.getAllUserBadgesFlow(),
            badgeDao.getAllGlobalStatsFlow()
        ) { userBadges, globalStats ->
            val userBadgeMap = userBadges.associateBy { it.badgeId }
            val statsMap = globalStats.associateBy { it.badgeId }

            OFFICIAL_TROPHY_ROOM_CATALOG.map { catalogItem ->
                val baseBadge = catalogItem.badge
                val userEntity = userBadgeMap[baseBadge.id]
                val globalStat = statsMap[baseBadge.id]

                val dynamicRarity = globalStat?.let {
                    String.format(Locale.US, "%.1f%%", it.rarityPercent)
                } ?: baseBadge.rarityPercent

                if (userEntity != null) {
                    val activeTier = parseTierSafe(userEntity.currentTier, baseBadge.fixedTier ?: baseBadge.initialProgressiveTier)
                    val updatedBadge = baseBadge.copy(
                        progressCurrent = userEntity.progressCurrent,
                        progressTarget = userEntity.progressTarget,
                        rarityPercent = dynamicRarity
                    )
                    catalogItem.copy(
                        badge = updatedBadge,
                        currentTier = activeTier,
                        isUnlocked = userEntity.isUnlocked,
                        unlockedDate = userEntity.unlockedDate
                    )
                } else {
                    catalogItem.copy(
                        badge = baseBadge.copy(
                            progressCurrent = 0,
                            rarityPercent = dynamicRarity
                        ),
                        currentTier = baseBadge.fixedTier ?: baseBadge.initialProgressiveTier,
                        isUnlocked = false,
                        unlockedDate = null
                    )
                }
            }
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Valuta in background in modo asincrono tutti i badge senza bloccare il chiamante.
     */
    fun evaluateBadgesAsync() {
        repositoryScope.launch {
            try {
                evaluateAllBadges()
            } catch (e: Exception) {
                android.util.Log.e("BadgeRepository", "Error evaluating badges asynchronously", e)
            }
        }
    }

    /**
     * Valuta l'intero catalogo dei badge locali alla luce dei dati presenti in Room:
     * - film e serie viste (`favorites`)
     * - rewatch e orari (`watch_history`)
     * - cartelle create (`folders`)
     *
     * Se rileva un avanzamento di tier o un primo sblocco, aggiorna SQLite ed emette
     * un evento [BadgeTierUnlockedEvent].
     */
    suspend fun evaluateAllBadges(): List<UserBadgeEntity> {
        val allMovies = favoriteDao.getAll()
        val allHistory = watchHistoryDao.getAllWatchHistory()
        val allFolders = folderDao.getAll()
        val existingBadges = badgeDao.getAllUserBadges().associateBy { it.badgeId }

        val watchedMovies = allMovies.filter { it.watched && it.mediaType != "tv" && !it.dropped }
        val watchedShows = allMovies.filter { it.watched && it.mediaType == "tv" && !it.dropped }
        val allWatched = allMovies.filter { it.watched && !it.dropped }

        // Aggiorna frequenze attori e registi per i badge #34 e #35
        val actorFrequencies: List<UserPersonFrequencyEntity> = allWatched.filter { it.favoriteActorId != null }.groupBy { it.favoriteActorId!! }.map { (actorId, list) ->
            UserPersonFrequencyEntity(
                personId = actorId,
                personName = list.firstNotNullOfOrNull { it.favoriteActorName } ?: "Attore",
                roleType = "ACTOR",
                watchedCount = list.size,
                lastWatchedAt = System.currentTimeMillis()
            )
        }
        val directorFrequencies: List<UserPersonFrequencyEntity> = allWatched.filter { it.directorId != null }.groupBy { it.directorId!! }.map { (dirId, list) ->
            UserPersonFrequencyEntity(
                personId = dirId,
                personName = list.firstNotNullOfOrNull { it.directorName } ?: "Regista",
                roleType = "DIRECTOR",
                watchedCount = list.size,
                lastWatchedAt = System.currentTimeMillis()
            )
        }
        if (actorFrequencies.isNotEmpty() || directorFrequencies.isNotEmpty()) {
            badgeDao.upsertPersonFrequencies(actorFrequencies + directorFrequencies)
        }

        val evaluatedEntities = mutableListOf<UserBadgeEntity>()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        for (catalogItem in OFFICIAL_TROPHY_ROOM_CATALOG) {
            val badge = catalogItem.badge
            val prevEntity = existingBadges[badge.id]

            val (calculatedProgress, targetThreshold, calculatedTier, isUnlocked) = evaluateBadgeLogic(
                badge = badge,
                allMovies = allMovies,
                watchedMovies = watchedMovies,
                watchedShows = watchedShows,
                allWatched = allWatched,
                allHistory = allHistory,
                folderCount = allFolders.size,
                prevEntity = prevEntity
            )

            val newlyUnlocked = isUnlocked && (prevEntity == null || !prevEntity.isUnlocked)
            val tierUpgraded = prevEntity != null && prevEntity.isUnlocked && isHigherTier(calculatedTier, parseTierSafe(prevEntity.currentTier, PreviewBadgeTier.SUPER_8))

            val unlockedDate = when {
                prevEntity?.unlockedDate != null -> prevEntity.unlockedDate
                isUnlocked -> todayStr
                else -> null
            }

            val entity = UserBadgeEntity(
                badgeId = badge.id,
                currentTier = calculatedTier.name,
                category = badge.category.name,
                progressCurrent = calculatedProgress,
                progressTarget = targetThreshold,
                isUnlocked = isUnlocked,
                unlockedDate = unlockedDate,
                isRevealed = isUnlocked || (prevEntity?.isRevealed ?: false),
                syncStatus = if (prevEntity?.progressCurrent != calculatedProgress || newlyUnlocked || tierUpgraded) "pending" else "synced",
                lastUpdatedAt = System.currentTimeMillis()
            )
            evaluatedEntities.add(entity)

            // Emetti evento di celebrazione per nuovi sblocchi o upgrade
            if (newlyUnlocked || tierUpgraded) {
                val stepPhrase = badge.progressiveSteps[calculatedTier]?.milestonePhrase
                    ?: badge.title
                _unlockedTierEvents.tryEmit(
                    BadgeTierUnlockedEvent(
                        badgeId = badge.id,
                        title = badge.title,
                        tier = calculatedTier,
                        category = badge.category,
                        iconKind = badge.iconKind,
                        milestonePhrase = stepPhrase,
                        isFirstUnlock = newlyUnlocked
                    )
                )
            }
        }

        badgeDao.upsertBadges(evaluatedEntities)
        return evaluatedEntities
    }

    /**
     * Sincronizza le statistiche globali di rarità leggendo 1 singola volta
     * il documento aggregato `/system_metrics/badges_global` da Firestore.
     */
    suspend fun fetchGlobalRarityStats(): Boolean {
        return try {
            val doc = firestore.collection("system_metrics").document("badges_global").get().await()
            if (!doc.exists()) return false

            val totalUsers = doc.getLong("total_active_users") ?: 1000L
            @Suppress("UNCHECKED_CAST")
            val rarities = doc.get("rarities") as? Map<String, Any> ?: return false

            val statsEntities = rarities.mapNotNull { (badgeId, rawPercent) ->
                val percentFloat = when (rawPercent) {
                    is Number -> rawPercent.toFloat()
                    is String -> rawPercent.replace("%", "").toFloatOrNull()
                    else -> null
                } ?: return@mapNotNull null

                BadgeGlobalStatsEntity(
                    badgeId = badgeId,
                    rarityPercent = percentFloat.coerceIn(0.1f, 100.0f),
                    totalUsersCount = totalUsers,
                    cachedAt = System.currentTimeMillis()
                )
            }

            if (statsEntities.isNotEmpty()) {
                badgeDao.upsertGlobalStats(statsEntities)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ── VALUTAZIONE LOGICA DEI SINGOLI BADGE ──

    private data class EvaluationResult(
        val currentProgress: Int,
        val targetThreshold: Int,
        val activeTier: PreviewBadgeTier,
        val isUnlocked: Boolean
    )

    private fun evaluateBadgeLogic(
        badge: PreviewBadgeItem,
        allMovies: List<Movie>,
        watchedMovies: List<Movie>,
        watchedShows: List<Movie>,
        allWatched: List<Movie>,
        allHistory: List<com.cinetrack.data.local.entities.WatchHistoryEntity>,
        folderCount: Int,
        prevEntity: UserBadgeEntity?
    ): EvaluationResult {
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
                EvaluationResult(
                    currentProgress = if (foundLoop) 2 else 0,
                    targetThreshold = 2,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = foundLoop
                )
            }

            // 2. Roulette del Fato: Visto un film scoperto con Surprise Me
            "secret_surprise_fate" -> {
                val isUnlocked = prevEntity?.isUnlocked ?: false
                EvaluationResult(
                    currentProgress = if (isUnlocked) 1 else 0,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = isUnlocked
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
                EvaluationResult(
                    currentProgress = nightWatches,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = nightWatches >= 1
                )
            }

            // 4. Crisi d'Identità: Film completato privo di generi TMDB
            "secret_genreless_rebel" -> {
                val count = allWatched.count { it.genreIds.isNullOrEmpty() }
                EvaluationResult(
                    currentProgress = count,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = count >= 1
                )
            }

            // 5. L'Eterno Dubbioso: Voto modificato almeno 3 volte sullo stesso film
            "secret_indecisive" -> {
                val count = prevEntity?.progressCurrent ?: 0
                EvaluationResult(
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
                EvaluationResult(
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
                EvaluationResult(
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
                EvaluationResult(
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
                EvaluationResult(
                    currentProgress = gemsCount,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.LOST_REEL,
                    isUnlocked = gemsCount >= 1
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

            // 14. Longevità Cineclub: Tessera del Cineclub
            "badge_veteran_club" -> {
                val earliestTime = allMovies.mapNotNull { it.touched.takeIf { t -> t != null && t > 0L } }
                    .minOrNull() ?: System.currentTimeMillis()
                val diffMillis = (System.currentTimeMillis() - earliestTime).coerceAtLeast(0L)
                val months = ((diffMillis / (1000L * 60 * 60 * 24 * 30L)) + 1).toInt().coerceAtLeast(1)
                calculateProgressiveTiers(months, badge.progressiveSteps)
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
                val count = prevEntity?.progressCurrent ?: 0
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
                val commentsCount = prevEntity?.progressCurrent ?: 0
                calculateProgressiveTiers(commentsCount, badge.progressiveSteps)
            }

            // 21. Curatore d'Autore: Cartelle create
            "badge_folders" -> {
                calculateProgressiveTiers(folderCount, badge.progressiveSteps)
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

            // ══════════════════════════════════════════════════════════════════
            // GLI 8 TRAGUARDI SINGOLI DI PRESTIGIO & ONORIFICENZE
            // ══════════════════════════════════════════════════════════════════

            // 29. Day One Pioneer: Pioniere Prima Bobina
            "badge_day_one_pioneer" -> {
                EvaluationResult(
                    currentProgress = 1,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.THE_FINAL_CUT,
                    isUnlocked = true
                )
            }

            // 30. Sostenitore & Mecenate: Il Produttore Esecutivo
            "badge_executive_producer" -> {
                val isSupporter = prevEntity?.isUnlocked ?: false
                EvaluationResult(
                    currentProgress = if (isSupporter) 1 else 0,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.THE_FINAL_CUT,
                    isUnlocked = isSupporter
                )
            }

            // 31. Cloud Sync: Il Trasloco
            "badge_onboarding_sync" -> {
                val isSynced = prevEntity?.isUnlocked ?: false
                EvaluationResult(
                    currentProgress = if (isSynced) 1 else 0,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.MM_16,
                    isUnlocked = isSynced
                )
            }

            // 32. TMDB > 8.5: Gourmet del Cinema
            "gourmet_critic" -> {
                val count = watchedMovies.count { (it.voteAverage ?: 0.0) >= 8.5 }
                EvaluationResult(
                    currentProgress = count,
                    targetThreshold = 5,
                    activeTier = PreviewBadgeTier.MM_70,
                    isUnlocked = count >= 5
                )
            }

            // 33. Durata > 3h30m (210 min): Titanico
            "colossal" -> {
                val count = watchedMovies.count { (it.runtime ?: 0) >= 210 }
                EvaluationResult(
                    currentProgress = count,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.MM_70,
                    isUnlocked = count >= 1
                )
            }

            // 34. Attore Preferito: Attore Feticcio (5 check-in)
            "actor_muse" -> {
                val topActorCount = allWatched.filter { it.favoriteActorId != null }
                    .groupBy { it.favoriteActorId }
                    .values.maxOfOrNull { it.size } ?: 0
                EvaluationResult(
                    currentProgress = topActorCount,
                    targetThreshold = 5,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = topActorCount >= 5
                )
            }

            // 35. 5 Film Stesso Regista: Regista del Cuore
            "director_heart" -> {
                val topDirectorCount = allWatched.filter { it.directorId != null }
                    .groupBy { it.directorId }
                    .values.maxOfOrNull { it.size } ?: 0
                EvaluationResult(
                    currentProgress = topDirectorCount,
                    targetThreshold = 5,
                    activeTier = PreviewBadgeTier.MM_70,
                    isUnlocked = topDirectorCount >= 5
                )
            }

            // 36. Note Personali: Diario di Bordo (5 note)
            "personal_diary" -> {
                val notesCount = allWatched.count { !it.personalNote.isNullOrBlank() }
                EvaluationResult(
                    currentProgress = notesCount,
                    targetThreshold = 5,
                    activeTier = PreviewBadgeTier.MM_35,
                    isUnlocked = notesCount >= 5
                )
            }

            // 37. Backdrop Personalizzato: Scenografo Personale
            "custom_backdrop" -> {
                val hasCustom = allMovies.any { it.customBackdropPath != null }
                EvaluationResult(
                    currentProgress = if (hasCustom) 1 else 0,
                    targetThreshold = 1,
                    activeTier = PreviewBadgeTier.SUPER_8,
                    isUnlocked = hasCustom
                )
            }

            // Default
            else -> {
                val target = badge.progressTarget.coerceAtLeast(1)
                val curr = prevEntity?.progressCurrent ?: 0
                val unl = prevEntity?.isUnlocked ?: (curr >= target)
                val tier = prevEntity?.currentTier?.let { parseTierSafe(it, badge.fixedTier ?: PreviewBadgeTier.SUPER_8) }
                    ?: (badge.fixedTier ?: PreviewBadgeTier.SUPER_8)
                EvaluationResult(
                    currentProgress = curr,
                    targetThreshold = target,
                    activeTier = tier,
                    isUnlocked = unl
                )
            }
        }
    }

    private fun calculateProgressiveTiers(
        currentValue: Int,
        steps: Map<PreviewBadgeTier, ProgressiveTierDetail>
    ): EvaluationResult {
        if (steps.isEmpty()) {
            return EvaluationResult(currentValue, 1, PreviewBadgeTier.SUPER_8, currentValue >= 1)
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

        // Se ha raggiunto l'ultimo tier, il target è la soglia finale
        if (highestAchievedTier == PreviewBadgeTier.THE_FINAL_CUT) {
            nextTarget = steps[PreviewBadgeTier.THE_FINAL_CUT]?.targetThreshold ?: nextTarget
        }

        return EvaluationResult(
            currentProgress = currentValue,
            targetThreshold = nextTarget,
            activeTier = highestAchievedTier,
            isUnlocked = isUnlocked
        )
    }

    private fun parseTierSafe(tierStr: String, fallback: PreviewBadgeTier): PreviewBadgeTier {
        return try {
            PreviewBadgeTier.valueOf(tierStr)
        } catch (_: Exception) {
            fallback
        }
    }

    private fun isHigherTier(candidate: PreviewBadgeTier, current: PreviewBadgeTier): Boolean {
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

    private fun parseWatchedAtMillis(dateStr: String): Long {
        return try {
            java.time.Instant.parse(dateStr).toEpochMilli()
        } catch (_: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(dateStr)?.time ?: 0L
            } catch (_: Exception) {
                0L
            }
        }
    }
}
