package com.cinetrack.widget

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.cinetrack.data.local.database.FlickTroveDatabase
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.domain.UpdateEpisodesUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Glance ActionCallback che segna un episodio come visto direttamente dal widget.
 */
class MarkEpisodeWatchedCallback : ActionCallback {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CallbackEntryPoint {
        fun movieRepository(): MovieRepository
        fun updateEpisodesUseCase(): UpdateEpisodesUseCase
    }

    companion object {
        private const val TAG = "MarkEpisodeWatched"

        val KEY_SHOW_ID = ActionParameters.Key<Long>("show_id")
        val KEY_SHOW_ID_STR = ActionParameters.Key<String>("show_id_str")
        val KEY_MEDIA_TYPE = ActionParameters.Key<String>("media_type")
        val KEY_EPISODE_STRING = ActionParameters.Key<String>("episode_string")
        val KEY_SEASON_NUM = ActionParameters.Key<Int>("season_num")
        val KEY_EPISODE_NUM = ActionParameters.Key<Int>("episode_num")
        val KEY_IMDB_ID = ActionParameters.Key<String>("imdb_id")

        private val EPISODE_REGEX = Regex("""[Ss](\d+)[Ee](\d+)""")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            val showId = parameters[KEY_SHOW_ID]
                ?: parameters[KEY_SHOW_ID_STR]?.toLongOrNull()
            val mediaType = parameters[KEY_MEDIA_TYPE] ?: "tv"
            val episodeString = parameters[KEY_EPISODE_STRING]

            var seasonNum = parameters[KEY_SEASON_NUM]
            var episodeNum = parameters[KEY_EPISODE_NUM]

            if ((seasonNum == null || episodeNum == null) && !episodeString.isNullOrEmpty()) {
                val match = EPISODE_REGEX.find(episodeString)
                if (match != null) {
                    seasonNum = match.groupValues[1].toIntOrNull()
                    episodeNum = match.groupValues[2].toIntOrNull()
                }
            }

            Log.d(TAG, "onAction invoked: showId=$showId, mediaType=$mediaType, s=$seasonNum, e=$episodeNum, epStr=$episodeString")

            if (showId == null || seasonNum == null || episodeNum == null) {
                Log.w(TAG, "Missing parameters: showId=$showId, s=$seasonNum, e=$episodeNum")
                return
            }

            val db = FlickTroveDatabase.getInstance(context)
            val show = db.favoriteDao().getById(showId, mediaType)
            if (show == null) {
                Log.w(TAG, "Show not found in database: id=$showId, type=$mediaType")
                return
            }

            // Aggiorna la lista degli episodi visti per la stagione
            val currentWatchedMap = show.watchedEpisodes?.toMutableMap() ?: mutableMapOf()
            val currentEpList = currentWatchedMap[seasonNum.toString()]?.toMutableList() ?: mutableListOf()
            if (!currentEpList.contains(episodeNum)) {
                currentEpList.add(episodeNum)
            }
            currentWatchedMap[seasonNum.toString()] = currentEpList

            val updateEpisodesUseCase = try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    CallbackEntryPoint::class.java
                )
                entryPoint.updateEpisodesUseCase()
            } catch (_: Exception) {
                UpdateEpisodesUseCase()
            }

            var updatedShow = updateEpisodesUseCase(show, seasonNum, currentEpList).copy(dropped = false)

            // Ricalcola nextEpisodeString e nextEpisodeAirDate se necessario
            val seasons = show.seasons
            if (seasons != null) {
                data class EpInfo(val s: Int, val e: Int, val airDate: String?)
                val nextEp = seasons
                    .filter { (it.seasonNumber ?: 0) > 0 }
                    .flatMap { season ->
                        val sN = season.seasonNumber ?: return@flatMap emptyList<EpInfo>()
                        val watched = currentWatchedMap[sN.toString()] ?: emptyList()
                        (season.episodes ?: emptyList())
                            .filter { ep -> !watched.contains(ep.episodeNumber) }
                            .map { ep -> EpInfo(sN, ep.episodeNumber, ep.airDate) }
                    }
                    .sortedWith(compareBy({ it.s }, { it.e }))
                    .firstOrNull()

                if (nextEp != null) {
                    val sStr = nextEp.s.toString().padStart(2, '0')
                    val eStr = nextEp.e.toString().padStart(2, '0')
                    updatedShow = updatedShow.copy(
                        nextEpisodeString = "S${sStr}E${eStr}",
                        nextEpisodeAirDate = nextEp.airDate
                    )
                } else {
                    updatedShow = updatedShow.copy(
                        nextEpisodeString = null,
                        nextEpisodeAirDate = null
                    )
                }
            }

            // Salva tramite MovieRepository (che aggiorna Room, notifica widget ed esegue sync Trakt/Simkl)
            var savedViaRepo = false
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    CallbackEntryPoint::class.java
                )
                val repository = entryPoint.movieRepository()
                repository.saveMovie(updatedShow)
                savedViaRepo = true
                Log.d(TAG, "Show updated via MovieRepository: id=$showId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save via MovieRepository, falling back to direct DB write", e)
            }

            if (!savedViaRepo) {
                db.favoriteDao().insert(
                    updatedShow.copy(syncStatus = "synced", clientUpdatedAt = System.currentTimeMillis())
                )
                Log.d(TAG, "Show updated via direct DB insert: id=$showId")
            }

            // Mostra feedback visivo (Toast) all'utente
            val epLabel = "S${seasonNum.toString().padStart(2, '0')}E${episodeNum.toString().padStart(2, '0')}"
            val showTitle = show.name ?: show.title ?: ""
            withContext(Dispatchers.Main) {
                val msg = if (showTitle.isNotEmpty()) "$showTitle: $epLabel ✓" else "$epLabel ✓"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onAction", e)
        } finally {
            try {
                FlickTroveNowWatchingWidget().update(context, glanceId)
                FlickTroveNowWatchingWidget().updateAll(context)
                FlickTroveTvEpisodesWidget().updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widgets", e)
            }
            WidgetUpdater.update(context)
        }
    }
}
