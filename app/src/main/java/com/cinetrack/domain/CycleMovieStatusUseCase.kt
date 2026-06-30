package com.cinetrack.domain

import com.cinetrack.data.Movie
import com.cinetrack.data.repository.MovieRepository
import javax.inject.Inject

class CycleMovieStatusUseCase @Inject constructor(
    private val repository: MovieRepository,
    private val updateEpisodesUseCase: UpdateEpisodesUseCase
) {
    suspend operator fun invoke(movie: Movie) {
        android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus START: id=${movie.id}, title=${movie.title ?: movie.name}, watched=${movie.watched}, fav=${movie.favorite}, rem=${movie.reminder}, released=${movie.isReleased}, mediaType=${movie.mediaType}")
        
        val updated = when {
            // Case 1: State: Watched (Check) -> Idempotent (Stay Watched)
            movie.watched -> {
                android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: Branch [Watched -> Stay Watched] (Action ignored)")
                movie
            }
            
            // Case 2: State: To See (Eye/Bell) -> Next step
            movie.favorite || movie.reminder -> {
                if (movie.isReleased) {
                    android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: Branch [To See -> Watched] (isReleased=true)")
                    // Released: Move to Watched (Check)
                    if (movie.mediaType == "tv") {
                        updateEpisodesUseCase.markAllWatched(movie).copy(
                            favorite = false,
                            reminder = false, // Explicitly clear both
                            clientUpdatedAt = System.currentTimeMillis()
                        )
                    } else {
                        movie.copy(
                            favorite = false,
                            reminder = false,
                            watched = true,
                            watchedAt = java.time.Instant.now().toString(),
                            clientUpdatedAt = System.currentTimeMillis()
                        )
                    }
                } else {
                    android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: Branch [To See -> Untracked] (isReleased=false, toggling OFF reminder)")
                    // Unreleased: Toggle OFF reminder
                    movie.copy(
                        favorite = false,
                        reminder = false,
                        watched = false,
                        clientUpdatedAt = System.currentTimeMillis()
                    )
                }
            }
            
            // Case 3: State: Untracked (+) -> Move to To See (Eye/Bell)
            else -> {
                if (movie.isReleased) {
                    android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: Branch [Untracked -> To See (Eye)] (isReleased=true)")
                    movie.copy(
                        favorite = true,
                        reminder = false,
                        watched = false,
                        clientUpdatedAt = System.currentTimeMillis()
                    )
                } else {
                    android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: Branch [Untracked -> To See (Bell)] (isReleased=false)")
                    movie.copy(
                        favorite = false,
                        reminder = true,
                        watched = false,
                        clientUpdatedAt = System.currentTimeMillis()
                    )
                }
            }
        }
        
        android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: [${updated.title}] isReleased=${updated.isReleased} (date=${updated.releaseDate})")
        android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus END: id=${updated.id}, watched=${updated.watched}, fav=${updated.favorite}, rem=${updated.reminder}")
        
        if (updated != movie) {
            android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: Saving updated movie")
            repository.saveMovie(updated)
            // Trigger background fetch for missing metadata (runtime, cast) using partial update to avoid race conditions
            repository.fetchMissingDetailsAsync(updated)
        } else {
            android.util.Log.d("CycleMovieStatusUseCase", "cycleMovieStatus: No changes to save")
        }
    }
}
