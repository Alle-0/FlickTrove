package com.cinetrack.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.cinetrack.data.model.Movie
import com.cinetrack.worker.SimklInstantWriteWorker
import com.cinetrack.worker.TraktInstantWriteWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispatches instant background write tasks to Trakt and SIMKL via WorkManager
 * whenever local movie or TV show states change (watched, dropped, rating, watchlist, episodes).
 */
@Singleton
class ExternalSyncDispatcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun dispatchSaveSync(oldMovie: Movie?, movie: Movie) {
        val workRequests = mutableListOf<OneTimeWorkRequest>()

        fun enqueue(action: String, extras: Data.Builder.() -> Unit = {}) {
            val builder = Data.Builder()
                .putString(TraktInstantWriteWorker.KEY_ACTION, action)
                .putString(TraktInstantWriteWorker.KEY_MEDIA_TYPE, movie.mediaType)
                .putLong(TraktInstantWriteWorker.KEY_TMDB_ID, movie.id)
            if (movie.imdbId != null) {
                builder.putString(TraktInstantWriteWorker.KEY_IMDB_ID, movie.imdbId)
            }
            builder.apply(extras)
            workRequests += OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(builder.build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
        }

        val oldWatched = oldMovie?.watched ?: false
        val oldRating = oldMovie?.personalRating
        val oldFav = oldMovie?.favorite ?: false
        var watchlistRemoveEnqueued = false

        // watched changed (o nuovo inserimento già visto)
        if (oldWatched != movie.watched) {
            if (movie.mediaType == "tv") {
                if (movie.watched) {
                    enqueue(TraktInstantWriteWorker.ACTION_MARK_WATCHED)
                    enqueue(TraktInstantWriteWorker.ACTION_REMOVE_WATCHLIST)
                    watchlistRemoveEnqueued = true
                } else if (movie.watchedEpisodes.isNullOrEmpty()) {
                    if (movie.watchedAt == null) {
                        enqueue(TraktInstantWriteWorker.ACTION_REMOVE_WATCHED)
                    }
                }
            } else {
                val action = if (movie.watched) TraktInstantWriteWorker.ACTION_MARK_WATCHED
                else TraktInstantWriteWorker.ACTION_REMOVE_WATCHED
                enqueue(action)
                if (movie.watched) {
                    enqueue(TraktInstantWriteWorker.ACTION_REMOVE_WATCHLIST)
                    watchlistRemoveEnqueued = true
                }
            }
        }

        // rewatch for movies
        val oldWatchedAt = oldMovie?.watchedAt
        if (oldWatched == movie.watched && movie.watched && oldWatchedAt != movie.watchedAt && movie.watchedAt != null) {
            if (movie.mediaType == "movie") {
                enqueue(TraktInstantWriteWorker.ACTION_MARK_WATCHED)
            }
        }

        // dropped changed
        val oldDropped = oldMovie?.dropped ?: false
        if (oldDropped != movie.dropped) {
            val action = if (movie.dropped) TraktInstantWriteWorker.ACTION_MARK_DROPPED
            else TraktInstantWriteWorker.ACTION_REMOVE_DROPPED
            enqueue(action)
        }

        // personalRating changed
        if (oldRating != movie.personalRating) {
            val rating = movie.personalRating
            if (rating != null && rating > 0.0) {
                val traktRating = rating.toInt().coerceIn(1, 10)
                enqueue(TraktInstantWriteWorker.ACTION_ADD_RATING) {
                    putInt(TraktInstantWriteWorker.KEY_RATING, traktRating)
                }
            } else if (oldRating != null && (rating == null || rating == 0.0)) {
                enqueue(TraktInstantWriteWorker.ACTION_REMOVE_RATING)
            }
        }

        // favorite changed -> sync watchlist/favorite
        if (oldFav != movie.favorite) {
            if (movie.favorite) {
                enqueue(TraktInstantWriteWorker.ACTION_ADD_WATCHLIST)
            } else if (!watchlistRemoveEnqueued) {
                enqueue(TraktInstantWriteWorker.ACTION_REMOVE_WATCHLIST)
            }
        }

        // watchedEpisodes changed (TV only)
        if (movie.mediaType == "tv" && oldMovie?.watchedEpisodes != movie.watchedEpisodes) {
            val oldEps = oldMovie?.watchedEpisodes ?: emptyMap()
            val newEps = movie.watchedEpisodes ?: emptyMap()

            val addedEps = newEps.mapValues { (season, eps) ->
                eps - (oldEps[season]?.toSet() ?: emptySet())
            }.filter { it.value.isNotEmpty() }

            val removedEps = oldEps.mapValues { (season, eps) ->
                eps - (newEps[season]?.toSet() ?: emptySet())
            }.filter { it.value.isNotEmpty() }

            if (addedEps.isNotEmpty()) {
                val encoded = TraktInstantWriteWorker.encodeEpisodes(addedEps)
                enqueue(TraktInstantWriteWorker.ACTION_MARK_EPISODES_WATCHED) {
                    putString(TraktInstantWriteWorker.KEY_SEASON_EPISODES, encoded)
                    movie.watchedAt?.let { putString(TraktInstantWriteWorker.KEY_WATCHED_AT, it) }
                }
            }
            if (removedEps.isNotEmpty()) {
                val encoded = TraktInstantWriteWorker.encodeEpisodes(removedEps)
                enqueue(TraktInstantWriteWorker.ACTION_REMOVE_EPISODES_WATCHED) {
                    putString(TraktInstantWriteWorker.KEY_SEASON_EPISODES, encoded)
                }
            }
        }

        submitWorkRequests(workRequests)
    }

    fun dispatchDeleteSync(movie: Movie) {
        val workRequests = mutableListOf<OneTimeWorkRequest>()

        val baseBuilder = Data.Builder()
            .putString(TraktInstantWriteWorker.KEY_ACTION, TraktInstantWriteWorker.ACTION_REMOVE_WATCHED)
            .putString(TraktInstantWriteWorker.KEY_MEDIA_TYPE, movie.mediaType)
            .putLong(TraktInstantWriteWorker.KEY_TMDB_ID, movie.id)
        if (movie.imdbId != null) {
            baseBuilder.putString(TraktInstantWriteWorker.KEY_IMDB_ID, movie.imdbId)
        }

        workRequests += OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(baseBuilder.build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        val personalRating = movie.personalRating
        if (personalRating != null && personalRating > 0.0) {
            val ratingBuilder = Data.Builder()
                .putString(TraktInstantWriteWorker.KEY_ACTION, TraktInstantWriteWorker.ACTION_REMOVE_RATING)
                .putString(TraktInstantWriteWorker.KEY_MEDIA_TYPE, movie.mediaType)
                .putLong(TraktInstantWriteWorker.KEY_TMDB_ID, movie.id)
            workRequests += OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(ratingBuilder.build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
        }

        if (movie.favorite) {
            val watchlistBuilder = Data.Builder()
                .putString(TraktInstantWriteWorker.KEY_ACTION, TraktInstantWriteWorker.ACTION_REMOVE_WATCHLIST)
                .putString(TraktInstantWriteWorker.KEY_MEDIA_TYPE, movie.mediaType)
                .putLong(TraktInstantWriteWorker.KEY_TMDB_ID, movie.id)
            workRequests += OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(watchlistBuilder.build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
        }

        submitWorkRequests(workRequests)
    }

    fun dispatchMarkAsDeletedSync(movie: Movie) {
        val builder = Data.Builder()
            .putString(TraktInstantWriteWorker.KEY_ACTION, TraktInstantWriteWorker.ACTION_REMOVE_WATCHED)
            .putString(TraktInstantWriteWorker.KEY_MEDIA_TYPE, movie.mediaType)
            .putLong(TraktInstantWriteWorker.KEY_TMDB_ID, movie.id)
        if (movie.imdbId != null) {
            builder.putString(TraktInstantWriteWorker.KEY_IMDB_ID, movie.imdbId)
        }

        val traktRequest = OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
            .setInputData(builder.build())
            .build()
        val simklRequest = OneTimeWorkRequestBuilder<SimklInstantWriteWorker>()
            .setInputData(builder.build())
            .build()

        WorkManager.getInstance(context).enqueue(traktRequest)
        WorkManager.getInstance(context).enqueue(simklRequest)
    }

    private fun submitWorkRequests(workRequests: List<OneTimeWorkRequest>) {
        if (workRequests.isEmpty()) return

        WorkManager.getInstance(context)
            .enqueueUniqueWork("TRAKT_INSTANT_PUSH", ExistingWorkPolicy.APPEND_OR_REPLACE, workRequests)

        val simklRequests = workRequests.map { traktReq ->
            OneTimeWorkRequestBuilder<SimklInstantWriteWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(traktReq.workSpec.input)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
        }

        if (simklRequests.isNotEmpty()) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork("SIMKL_INSTANT_PUSH", ExistingWorkPolicy.APPEND_OR_REPLACE, simklRequests)
        }
    }
}
