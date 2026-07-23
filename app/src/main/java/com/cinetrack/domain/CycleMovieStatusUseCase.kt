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
                personalRating = local.personalRating,
                personalNote = local.personalNote,
                watchedAt = local.watchedAt
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
                        current.copy(
                            favorite = false,
                            reminder = false,
                            watched = true,
                            watchedAt = java.time.Instant.now().toString(),
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
        
        android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: [${updated.title}] isReleased=${updated.isReleased} (date=${updated.releaseDate})")
        android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus END: id=${updated.id}, watched=${updated.watched}, fav=${updated.favorite}, rem=${updated.reminder}")
        
        if (updated != current) {
            android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: Saving updated movie")
            repository.saveMovie(updated)
            // Trigger background fetch for missing metadata (runtime, cast) using partial update to avoid race conditions
            repository.fetchMissingDetailsAsync(updated)
        } else {
            android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: No changes to save")
        }
    }
}
