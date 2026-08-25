package com.cinetrack.domain

import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MovieRepository
import javax.inject.Inject

class CycleMovieStatusUseCase @Inject constructor(
    private val repository: MovieRepository,
    private val updateEpisodesUseCase: UpdateEpisodesUseCase
) {
    suspend operator fun invoke(movie: Movie) {
        val local = repository.getMovie(movie.id, movie.mediaType)
        val current = if (local != null) {
            movie.copy(
                watched = local.watched,
                favorite = local.favorite,
                reminder = local.reminder,
                watchedEpisodes = local.watchedEpisodes,
                numberOfEpisodes = local.numberOfEpisodes ?: movie.numberOfEpisodes,
                numberOfSeasons = local.numberOfSeasons ?: movie.numberOfSeasons,
                seasons = local.seasons ?: movie.seasons,
                runtime = local.runtime ?: movie.runtime,
                episodeRunTime = local.episodeRunTime ?: movie.episodeRunTime,
                genres = local.genres ?: movie.genres,
                topCastData = local.topCastData ?: movie.topCastData,
                directorData = local.directorData ?: movie.directorData,
                directorId = local.directorId ?: movie.directorId,
                directorName = local.directorName ?: movie.directorName,
                directorProfilePath = local.directorProfilePath ?: movie.directorProfilePath,
                personalRating = local.personalRating,
                personalNote = local.personalNote,
                watchedAt = local.watchedAt,
                progress = local.progress,
                originCountry = local.originCountry ?: movie.originCountry,
                revenue = local.revenue ?: movie.revenue,
                budget = local.budget ?: movie.budget,
                isUpcoming = local.isUpcoming ?: movie.isUpcoming,
                shortCastString = local.shortCastString ?: movie.shortCastString,
                job = local.job ?: movie.job,
                department = local.department ?: movie.department,
                character = local.character ?: movie.character,
                accentColor = local.accentColor ?: movie.accentColor,
                accentColorStatic = local.accentColorStatic ?: movie.accentColorStatic,
                emotionalVibes = local.emotionalVibes ?: movie.emotionalVibes,
                favoriteActorId = local.favoriteActorId ?: movie.favoriteActorId,
                favoriteActorName = local.favoriteActorName ?: movie.favoriteActorName,
                favoriteActorProfilePath = local.favoriteActorProfilePath ?: movie.favoriteActorProfilePath,
                favoriteActorCharacter = local.favoriteActorCharacter ?: movie.favoriteActorCharacter,
                favoriteActorTmdbPath = local.favoriteActorTmdbPath ?: movie.favoriteActorTmdbPath,
                imdbId = local.imdbId ?: movie.imdbId,
                status = local.status ?: movie.status,
                tagline = local.tagline ?: movie.tagline,
                dropped = local.dropped,
                createdAt = local.createdAt ?: movie.createdAt,
                updatedAt = local.updatedAt ?: movie.updatedAt,
                lastSyncDate = local.lastSyncDate ?: movie.lastSyncDate,
                migratedAt = local.migratedAt ?: movie.migratedAt,
                clientUpdatedAt = local.clientUpdatedAt,
                syncStatus = local.syncStatus
            )
        } else {
            movie.copy(
                watched = false,
                favorite = false,
                reminder = false
            )
        }

        android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus START: id=${current.id}, title=${current.title ?: current.name}, watched=${current.watched}, fav=${current.favorite}, rem=${current.reminder}, released=${current.isReleased}, mediaType=${current.mediaType}")
        
        val updated = when {
            // Case 1: State: Watched (Check) -> Idempotent (Stay Watched)
            current.watched -> {
                android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: Branch [Watched -> Stay Watched] (Action ignored)")
                current
            }
            
            // Case 2: State: To See (Eye/Bell) -> Next step
            current.favorite || current.reminder -> {
                if (current.isReleased) {
                    android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: Branch [To See -> Watched] (isReleased=true)")
                    // Released: Move to Watched (Check)
                    if (current.mediaType == "tv") {
                        updateEpisodesUseCase.markAllWatched(current).copy(
                            favorite = false,
                            reminder = false, // Explicitly clear both
                            dropped = false,
                            clientUpdatedAt = System.currentTimeMillis()
                        )
                    } else {
                        val watchedDate = java.time.Instant.now().toString()
                        
                        // Insert first watch history
                        repository.insertWatchHistory(
                            com.cinetrack.data.local.entities.WatchHistoryEntity(
                                movieId = current.id,
                                watchedAt = watchedDate,
                                isRewatch = false
                            )
                        )
                        
                        current.copy(
                            favorite = false,
                            reminder = false,
                            watched = true,
                            watchedAt = watchedDate,
                            dropped = false,
                            clientUpdatedAt = System.currentTimeMillis()
                        )
                    }
                } else {
                    android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: Branch [To See -> Untracked] (isReleased=false, toggling OFF reminder)")
                    // Unreleased: Toggle OFF reminder
                    current.copy(
                        favorite = false,
                        reminder = false,
                        watched = false,
                        dropped = false,
                        clientUpdatedAt = System.currentTimeMillis()
                    )
                }
            }
            
            // Case 3: State: Untracked (+) -> Move to To See (Eye/Bell)
            else -> {
                if (current.isReleased) {
                    android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: Branch [Untracked -> To See (Eye)] (isReleased=true)")
                    current.copy(
                        favorite = true,
                        reminder = false,
                        watched = false,
                        dropped = false,
                        clientUpdatedAt = System.currentTimeMillis()
                    )
                } else {
                    android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: Branch [Untracked -> To See (Bell)] (isReleased=false)")
                    current.copy(
                        favorite = false,
                        reminder = true,
                        watched = false,
                        dropped = false,
                        clientUpdatedAt = System.currentTimeMillis()
                    )
                }
            }
        }
        var finalUpdated = updated
        if (finalUpdated.mediaType == "tv" && (finalUpdated.favorite || finalUpdated.reminder)) {
            try {
                val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                val targetSeasonNum = finalUpdated.seasons?.firstOrNull { !it.airDate.isNullOrBlank() && it.airDate >= today }?.seasonNumber
                    ?: finalUpdated.nextEpisodeString?.substringAfter("S")?.substringBefore("E")?.toIntOrNull()
                
                if (targetSeasonNum != null && targetSeasonNum > 0) {
                    val detailedSeason = repository.fetchSeasonDetails(finalUpdated.id, targetSeasonNum)
                    finalUpdated = finalUpdated.copy(
                        seasons = finalUpdated.seasons?.map { if (it.seasonNumber == targetSeasonNum) detailedSeason else it }
                    )
                }
            } catch (e: Exception) {
                // Ignore failure; background workers will eventually sync this
                android.util.Log.e("CycleMovieStatusUseCase", "cycleMovieStatus: Failed to fetch season details", e)
            }
        }

        android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: [${finalUpdated.title}] isReleased=${finalUpdated.isReleased} (date=${finalUpdated.releaseDate})")
        android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus END: id=${finalUpdated.id}, watched=${finalUpdated.watched}, fav=${finalUpdated.favorite}, rem=${finalUpdated.reminder}")
        
        if (finalUpdated != current) {
            android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: Saving updated movie")
            repository.saveMovie(finalUpdated)
            // Trigger background fetch for missing metadata (runtime, cast) using partial update to avoid race conditions
            repository.fetchMissingDetailsAsync(finalUpdated)
        } else {
            android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: No changes to save")
        }
    }
}
