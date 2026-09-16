package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import com.cinetrack.data.model.SocialNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.cinetrack.ui.components.updates.generateReminderItems
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.UiText

data class UpdatesUiState(
    val movies: ImmutableList<Movie> = persistentListOf(),
    val notificationCount: Int = 0,
    val socialNotifications: ImmutableList<SocialNotification> = persistentListOf(),
    val socialUnreadCount: Int = 0,
    val totalUnreadCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val actionFeedbackManager: ActionFeedbackManager
) : ViewModel() {

    fun emitMessage(message: UiText) {
        actionFeedbackManager.emit(message)
    }

    private val _socialNotifications = MutableStateFlow<ImmutableList<SocialNotification>>(persistentListOf())
    private val _socialUnreadCount = MutableStateFlow(0)

    init {
        observeSocialNotifications()
        syncUpcomingSeasonsForReminders()
    }

    private fun syncUpcomingSeasonsForReminders() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Wait for initial screen setup and animations to settle
                kotlinx.coroutines.delay(4000)
                val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                val movies = repository.getLocalMoviesFlow().first()
                val tvShows = movies.filter {
                    it.mediaType == "tv" && !it.dropped && (it.reminder || it.favorite) &&
                    it.status != "Ended" && it.status != "Canceled"
                }

                for (show in tvShows) {
                    val nextAirDate = show.nextEpisodeAirDate
                    val seasons = show.seasons ?: continue

                    val targetSeasons = seasons.filter { s ->
                        s.seasonNumber > 0 && s.episodes.isNullOrEmpty() && (
                            (!s.airDate.isNullOrBlank() && s.airDate >= today) ||
                            (!nextAirDate.isNullOrBlank() && nextAirDate >= today)
                        )
                    }

                    if (targetSeasons.isEmpty()) continue

                    var current = repository.getMovie(show.id, "tv") ?: show
                    var hasUpdates = false

                    for (targetSeason in targetSeasons) {
                        try {
                            val detailedSeason = repository.fetchSeasonDetails(show.id, targetSeason.seasonNumber)
                            val sanitizedSeason = detailedSeason.copy(
                                overview = null,
                                episodes = detailedSeason.episodes?.map { ep -> ep.copy(overview = null) }
                            )
                            val updatedSeasons = current.seasons?.map {
                                if (it.seasonNumber == targetSeason.seasonNumber) sanitizedSeason else it
                            } ?: current.seasons
                            current = current.copy(seasons = updatedSeasons)
                            hasUpdates = true
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                        }
                    }

                    if (hasUpdates) {
                        repository.saveMovie(current, syncToTrakt = false)
                        // Polite delay between shows to allow GC and avoid Room invalidation storms
                        kotlinx.coroutines.delay(600)
                    }
                }
            } catch (e: Exception) {
                // Ignore background prefetch errors
            }
        }
    }

    private fun observeSocialNotifications() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("user_social_notifications").document(userId)
            .collection("items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val notifs = snapshot.documents.mapNotNull { it.toObject(SocialNotification::class.java) }
                    _socialNotifications.value = notifs.toImmutableList()
                    _socialUnreadCount.value = notifs.count { !it.isRead }
                }
            }
    }

    val uiState: StateFlow<UpdatesUiState> = combine(
        repository.getLocalMoviesFlow(),
        _socialNotifications,
        _socialUnreadCount
    ) { movies, socialNotifs, socialUnread ->
            val today = java.time.LocalDate.now().toString()
            val updateList = movies.filter { 
                (!it.dropped && (it.newEpisodesFound ?: 0) > 0) || it.reminder == true || it.migratedAt == today || (it.mediaType == "tv" && !it.dropped)
            }
                .sortedByDescending { it.clientUpdatedAt }
            val unreadNotifCount = movies.count {
                !it.dropped && ((it.newEpisodesFound ?: 0) > 0 || (it.migratedAt == today && (it.newEpisodesFound ?: 0) == 0))
            }
            val futureRemindersCount = movies.flatMap { it.generateReminderItems(today) }.size

            UpdatesUiState(
                movies = updateList.toImmutableList(),
                notificationCount = unreadNotifCount,
                socialNotifications = socialNotifs,
                socialUnreadCount = socialUnread,
                totalUnreadCount = unreadNotifCount + socialUnread,
                isLoading = false
            )
        }
        .flowOn(Dispatchers.Default)
        .catch { e ->
            emit(UpdatesUiState(isLoading = false))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UpdatesUiState()
        )

    fun clearUpdate(movieId: Long, mediaType: String) {
        viewModelScope.launch {
            val movie = repository.getMovie(movieId, mediaType)
            movie?.let {
                repository.saveMovie(it.copy(newEpisodesFound = 0))
            }
        }
    }

    fun clearMigrated(movieId: Long, mediaType: String) {
        viewModelScope.launch {
            val movie = repository.getMovie(movieId, mediaType)
            movie?.let {
                repository.saveMovie(it.copy(migratedAt = null))
            }
        }
    }

    fun clearAllNewEpisodes() {
        viewModelScope.launch {
            val moviesWithUpdates = repository.getLocalMovies().filter { (it.newEpisodesFound ?: 0) > 0 }
            if (moviesWithUpdates.isNotEmpty()) {
                val updatedMovies = moviesWithUpdates.map { it.copy(newEpisodesFound = 0) }
                repository.saveMoviesBulk(updatedMovies)
            }
        }
    }

    fun clearAllMigrated() {
        viewModelScope.launch {
            val today = java.time.LocalDate.now().toString()
            val moviesWithMigrated = repository.getLocalMovies().filter { it.migratedAt == today }
            moviesWithMigrated.forEach { movie ->
                repository.saveMovie(movie.copy(migratedAt = null))
            }
        }
    }

    fun markSocialNotificationAsRead(id: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("user_social_notifications").document(userId)
            .collection("items").document(id)
            .update("isRead", true)
    }

    fun markAllSocialNotificationsAsRead() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val unreadDocs = firestore.collection("user_social_notifications").document(userId)
                    .collection("items").whereEqualTo("isRead", false).get().await()
                
                if (unreadDocs.isEmpty) return@launch
                
                val batch = firestore.batch()
                for (doc in unreadDocs.documents) {
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteSocialNotification(id: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("user_social_notifications").document(userId)
            .collection("items").document(id)
            .delete()
    }
}
