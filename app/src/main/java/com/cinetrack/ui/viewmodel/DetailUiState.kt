package com.cinetrack.ui.viewmodel

import com.cinetrack.data.Movie
import com.cinetrack.data.api.MovieDetailResponse
import com.cinetrack.data.api.CrewMember
import com.cinetrack.data.api.CastMember
import com.cinetrack.data.api.Provider
import com.cinetrack.data.models.Season

import com.cinetrack.data.local.entities.FolderEntity

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Error(val message: String) : DetailUiState()
    data class Success(
        val movieEntry: Movie,
        val details: MovieDetailResponse,
        val isFavorite: Boolean,
        val isWatched: Boolean,
        val watchState: WatchState,
        val watchedProgress: Float,
        val directors: List<CrewMember>,
        val cast: List<CastMember>,
        val streamingProviders: List<Provider>,
        val buyRentProviders: List<Provider>,
        val trailers: List<String>,
        val recommendations: List<Movie>,
        val collectionMovies: List<Movie> = emptyList(),
        val externalRatings: ExternalRatings = ExternalRatings(),
        val loadingSeason: Boolean = false,
        val seasonDetails: Map<Int, Season> = emptyMap(),
        val folders: List<FolderEntity> = emptyList(),
        val watchProviderLink: String? = null
    ) : DetailUiState()
}

enum class WatchState {
    NONE, BOOKMARKED, WATCHED
}

data class ExternalRatings(
    val imdb: String? = null,
    val imdbVotes: String? = null,
    val rottenTomatoes: String? = null,
    val metacritic: String? = null,
    val trakt: Double? = null,
    val traktVotes: String? = null,
    val awards: String? = null,
    val certification: String? = null
)

sealed class DetailEvent {
    object ToggleFavorite : DetailEvent()
    object ToggleWatched : DetailEvent()
    data class SetWatchState(val state: WatchState) : DetailEvent()
    object Refresh : DetailEvent()
    data class Rate(val rating: Double?) : DetailEvent()
    data class UpdateNote(val note: String) : DetailEvent()
    data class LoadSeasonDetails(val seasonNumber: Int) : DetailEvent()
    data class ToggleEpisode(val seasonNumber: Int, val episodeNumber: Int) : DetailEvent()
    data class ToggleSeason(val seasonNumber: Int, val episodes: List<Int>) : DetailEvent()
    data class RateMovie(val movie: Movie, val rating: Double?) : DetailEvent()
    data class UpdateMovieNote(val movie: Movie, val note: String) : DetailEvent()
    data class DeleteMovieItem(val movie: Movie) : DetailEvent()
    data class ToggleMovieFolderMembership(val movie: Movie, val folder: FolderEntity) : DetailEvent()
    data class CycleStatus(val movie: Movie) : DetailEvent()
    object DeleteMovie : DetailEvent()
    data class SyncWatchedEpisodes(val episodes: Map<String, List<Int>>) : DetailEvent()
    data class ToggleFolderMembership(val folder: FolderEntity) : DetailEvent()
    data class CreateFolder(val name: String, val color: String) : DetailEvent()
}


