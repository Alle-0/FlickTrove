package com.cinetrack.domain.usecase

import android.content.Context
import com.cinetrack.R
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.model.IrisWeeklyPicksData
import com.cinetrack.data.model.IrisWeeklyUiState
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.IrisWeeklyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetIrisWeeklyPicksUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val favoriteDao: FavoriteDao,
    private val repository: IrisWeeklyRepository
) {
    private data class CinematicCluster(
        val title: String,
        val genreIds: Set<Long>,
        val movies: List<Movie>
    )

    suspend operator fun invoke(
        forceRefresh: Boolean = false,
        clusterOffset: Int = 0
    ): IrisWeeklyUiState = withContext(Dispatchers.Default) {
        val allLocal = favoriteDao.getAll()
        val watchedIds = allLocal.filter { it.watched }.map { it.id }.toHashSet()

        // 1. Cold Start Check: raccoglie film amati (voto >= 8.0 o preferiti)
        val lovedMovies = allLocal.filter {
            (it.mediaType != "tv") && (it.favorite || (it.personalRating ?: 0.0) >= 8.0)
        }

        val seedPool = if (lovedMovies.size >= 2) {
            lovedMovies
        } else {
            // Fallback 1: Watchlist attiva
            val watchlist = allLocal.filter { it.mediaType != "tv" && !it.watched && !it.dropped }
            if (watchlist.size >= 2) watchlist else emptyList()
        }

        // Se l'utente non ha almeno 2 titoli in libreria, mostriamo la card onboarding di calibrazione
        if (seedPool.size < 2) {
            return@withContext IrisWeeklyUiState.ColdStartNeeded
        }

        // 2. Partizionamento nei macro-cluster d'atmosfera
        val clusters = buildClusters(seedPool)
        if (clusters.isEmpty()) {
            return@withContext IrisWeeklyUiState.ColdStartNeeded
        }

        val hasMultipleClusters = clusters.size > 1
        val cal = Calendar.getInstance()
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val currentYear = cal.get(Calendar.YEAR)

        val activeClusterIndex = Math.floorMod(currentWeek + clusterOffset, clusters.size)
        val activeCluster = clusters[activeClusterIndex]

        // 3. Estrazione semi candidati resiliente (fino a 18 ID)
        // Priorità 1: fino a 5 film del cluster attivo
        val clusterCandidates = activeCluster.movies
            .sortedByDescending { ((it.personalRating ?: 0.0) * 10) + (if (it.favorite) 5 else 0) }
            .take(5)

        // Priorità 2: altri film amati dell'utente da altri generi/cluster
        val otherCandidates = seedPool
            .filter { seed -> clusterCandidates.none { it.id == seed.id } }
            .sortedByDescending { ((it.personalRating ?: 0.0) * 10) + (if (it.favorite) 5 else 0) }
            .take(10)

        // Priorità 3: ruota di scorta con capolavori cult universali presenti in Vectorize (Pulp Fiction, Lord of War, Man of Steel)
        val cultFallbackIds = listOf(680L, 1830L, 49521L)

        val candidateSeedIds = (clusterCandidates.map { it.id } + otherCandidates.map { it.id } + cultFallbackIds).distinct().take(18)

        // 4. Controllo Cache Locale (TTL 7gg e stesso cluster offset)
        if (!forceRefresh) {
            val cached = repository.getCachedPicks()
            if (cached != null &&
                repository.isCacheValid(cached) &&
                cached.clusterOffset == clusterOffset
            ) {
                // Riserva: applichiamo .filter { it.id !in watchedIds }.take(10) per fare subentrare i film successivi
                val unconsumed = cached.candidateMovies.filter { it.id !in watchedIds }.take(10)
                if (unconsumed.isNotEmpty()) {
                    return@withContext IrisWeeklyUiState.Ready(
                        picks = cached.copy(
                            hasMultipleClusters = hasMultipleClusters,
                            candidateMovies = unconsumed
                        )
                    )
                }
            }
        }

        // 5. Chiamata di rete al Worker
        val lang = Locale.getDefault().language
        val result = repository.fetchRemotePicks(candidateSeedIds, lang)

        result.fold(
            onSuccess = { response ->
                val usedSeedIds = response.usedSeeds.toSet()
                val usedSeedMovies = if (usedSeedIds.isNotEmpty()) {
                    val matching = allLocal.filter { usedSeedIds.contains(it.id) }
                    matching.ifEmpty { clusterCandidates.take(3) }
                } else {
                    clusterCandidates.take(3)
                }

                val usedTitles = usedSeedMovies.map { (it.title ?: it.displayName).ifBlank { it.displayName } }
                val subtitle = if (usedTitles.isNotEmpty()) {
                    context.getString(R.string.iris_weekly_subtitle_resonance, usedTitles.joinToString(", "))
                } else {
                    context.getString(R.string.iris_weekly_subtitle_default)
                }

                // 6. Deduplicazione anti-visti e anti-cloni saga (solo per collection_id > 0)
                val seenSagas = mutableSetOf<Long>()
                val reserve20 = mutableListOf<TMDBSearchResult.MovieResult>()
                val rawCollectionIds = mutableMapOf<Long, Long?>()

                for (dto in response.results) {
                    if (watchedIds.contains(dto.id)) continue
                    if (candidateSeedIds.contains(dto.id)) continue

                    val collId = dto.collectionId
                    rawCollectionIds[dto.id] = collId

                    // Film standalone (collId == null o <= 0) passano sempre
                    if (collId != null && collId > 0) {
                        if (!seenSagas.add(collId)) continue
                    }

                    reserve20.add(dto.toMovieResult())
                    if (reserve20.size == 20) break
                }

                if (reserve20.isEmpty()) {
                    return@fold IrisWeeklyUiState.ColdStartNeeded
                }

                val picksData = IrisWeeklyPicksData(
                    weekNumber = currentWeek,
                    year = currentYear,
                    focusClusterTitle = activeCluster.title,
                    focusSubtitle = subtitle,
                    usedSeedTitles = usedTitles,
                    timestamp = System.currentTimeMillis(),
                    clusterOffset = clusterOffset,
                    hasMultipleClusters = hasMultipleClusters,
                    candidateMovies = reserve20
                )

                // Salva in cache i 20 film di riserva
                repository.savePicks(picksData, rawCollectionIds)

                val display10 = reserve20.filter { it.id !in watchedIds }.take(10)
                IrisWeeklyUiState.Ready(
                    picks = picksData.copy(candidateMovies = display10)
                )
            },
            onFailure = { err ->
                // Fallback offline trasparente: se c'è una cache locale scaduta, mostrala comunque
                val fallbackCache = repository.getCachedPicks()
                if (fallbackCache != null && fallbackCache.candidateMovies.isNotEmpty()) {
                    val filtered = fallbackCache.candidateMovies.filter { it.id !in watchedIds }.take(10)
                    IrisWeeklyUiState.Ready(
                        picks = fallbackCache.copy(candidateMovies = filtered)
                    )
                } else {
                    IrisWeeklyUiState.Error(err.message ?: context.getString(R.string.search_error_message))
                }
            }
        )
    }

    private fun buildClusters(movies: List<Movie>): List<CinematicCluster> {
        val sciFiIds = setOf(878L)
        val crimeNoirIds = setOf(80L, 53L, 9648L)
        val dramaAuteurIds = setOf(18L, 36L)
        val horrorIds = setOf(27L)
        val adventureActionIds = setOf(12L, 14L, 28L)
        val comedyIds = setOf(35L, 10749L)

        val clusters = mutableListOf<CinematicCluster>()

        fun addIfNotEmpty(titleRes: Int, genreSet: Set<Long>) {
            val matching = movies.filter { movie ->
                val genres = movie.genres?.map { it.id } ?: movie.genreIds ?: emptyList()
                genres.any { genreSet.contains(it) }
            }
            if (matching.size >= 2) {
                clusters.add(CinematicCluster(context.getString(titleRes), genreSet, matching))
            }
        }

        addIfNotEmpty(R.string.iris_cluster_scifi, sciFiIds)
        addIfNotEmpty(R.string.iris_cluster_crime_noir, crimeNoirIds)
        addIfNotEmpty(R.string.iris_cluster_drama_auteur, dramaAuteurIds)
        addIfNotEmpty(R.string.iris_cluster_horror, horrorIds)
        addIfNotEmpty(R.string.iris_cluster_adventure, adventureActionIds)
        addIfNotEmpty(R.string.iris_cluster_comedy, comedyIds)

        // Se nessun macro-cluster specifico ha >= 2 titoli, raggruppa tutti i titoli amati in un cluster universale
        if (clusters.isEmpty() && movies.isNotEmpty()) {
            clusters.add(
                CinematicCluster(
                    title = context.getString(R.string.iris_cluster_cult_favorites),
                    genreIds = emptySet(),
                    movies = movies
                )
            )
        }

        return clusters
    }
}
