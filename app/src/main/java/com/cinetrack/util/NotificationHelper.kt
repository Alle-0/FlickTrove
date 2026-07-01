package com.cinetrack.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.cinetrack.MainActivity
import com.cinetrack.R
import kotlinx.coroutines.launch

object NotificationHelper {

    // ── Release channel (film/serie in uscita oggi) ──────────────────────────
    private const val RELEASE_CHANNEL_ID   = "flicktrove_releases"
    private const val RELEASE_CHANNEL_NAME = "Nuove Uscite"
    private const val RELEASE_CHANNEL_DESC =
        "Avvisi quando un film o una serie che segui è disponibile"

    // ── New-episodes channel (episodi aggiornati) ─────────────────────────────
    private const val EPISODES_CHANNEL_ID   = "flicktrove_episodes"
    private const val EPISODES_CHANNEL_NAME = "Nuovi Episodi"
    private const val EPISODES_CHANNEL_DESC =
        "Avvisi quando vengono trovati nuovi episodi per le serie che guardi"

    // Accent color (teal) – matches NeonTeal in the app theme
    private const val ACCENT_COLOR = 0xFF00BFA5.toInt()

    // ── Channel creation (call once in Application.onCreate) ─────────────────

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

            // Release channel – high importance so it shows as a heads-up
            val releaseChannel = NotificationChannel(
                RELEASE_CHANNEL_ID,
                RELEASE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = RELEASE_CHANNEL_DESC
                enableLights(true)
                lightColor = ACCENT_COLOR
                enableVibration(true)
            }

            // Episodes channel – default importance (silent update)
            val episodesChannel = NotificationChannel(
                EPISODES_CHANNEL_ID,
                EPISODES_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = EPISODES_CHANNEL_DESC
            }

            manager.createNotificationChannel(releaseChannel)
            manager.createNotificationChannel(episodesChannel)
        }
    }

    // ── Permission helper ─────────────────────────────────────────────────────

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Below Android 13 no runtime permission is needed
            true
        }
    }

    // ── Release notification ──────────────────────────────────────────────────

    /**
     * Shows a rich "Nuova Release!" notification for a movie or TV show
     * whose reminder date has arrived.
     *
     * Silently skips if the POST_NOTIFICATIONS permission is not granted.
     */
    fun showReleaseNotification(
        context: Context,
        movieTitle: String,
        movieId: Long,
        mediaType: String,
        posterPath: String? = null
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = buildDeepLinkIntent(context, movieId, mediaType)
        val pendingIntent = PendingIntent.getActivity(
            context,
            movieId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mediaLabel = if (mediaType == "tv") context.getString(R.string.notif_media_tv) else context.getString(R.string.notif_media_movie)
        val bodyText = context.getString(R.string.notif_new_release_body, movieTitle)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val largeIcon = loadPosterBitmap(context, posterPath)
                ?: getBitmapFromVectorDrawable(context, R.drawable.ic_launcher_foreground_vector)

            val notification = NotificationCompat.Builder(context, RELEASE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(context.getString(R.string.notif_new_release_title, mediaLabel))
                .setContentText(context.getString(R.string.notif_new_release_body, movieTitle))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(bodyText)
                        .setBigContentTitle(context.getString(R.string.notif_new_release_title, mediaLabel))
                        .setSummaryText(context.getString(R.string.notif_reminder_summary))
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(ACCENT_COLOR)
                .setColorized(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build()

            NotificationManagerCompat.from(context).notify(movieId.toInt(), notification)
        }
    }

    /**
     * Shows a notification when a specific episode airs today.
     */
    fun showEpisodeReleaseNotification(
        context: Context,
        showTitle: String,
        showId: Long,
        seasonNumber: Int,
        episodeNumber: Int,
        posterPath: String? = null
    ) {
        val episodeString = "S%02dE%02d".format(seasonNumber, episodeNumber)
        showEpisodeReleaseNotification(context, showTitle, showId, episodeString, posterPath)
    }

    fun showEpisodeReleaseNotification(
        context: Context,
        showTitle: String,
        showId: Long,
        episodeString: String,
        posterPath: String? = null
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = buildDeepLinkIntent(context, showId, "tv")
        val pendingIntent = PendingIntent.getActivity(
            context,
            // Offset for episode release
            (showId + 2_000_000L).toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bodyText = context.getString(R.string.notif_new_episode_body, episodeString, showTitle)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val largeIcon = loadPosterBitmap(context, posterPath)
                ?: getBitmapFromVectorDrawable(context, R.drawable.ic_launcher_foreground_vector)

            val notification = NotificationCompat.Builder(context, RELEASE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(context.getString(R.string.notif_new_episode_title, episodeString))
                .setContentText(bodyText)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(bodyText)
                        .setBigContentTitle(context.getString(R.string.notif_new_episode_title, episodeString))
                        .setSummaryText(context.getString(R.string.notif_reminder_summary))
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(ACCENT_COLOR)
                .setColorized(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build()

            NotificationManagerCompat.from(context).notify((showId + 2_000_000L).toInt(), notification)
        }
    }

    // ── New-episodes notification ─────────────────────────────────────────────

    /**
     * Shows a notification when new episodes are found for a tracked TV show.
     *
     * Silently skips if the POST_NOTIFICATIONS permission is not granted.
     */
    fun showNewEpisodesNotification(
        context: Context,
        showTitle: String,
        showId: Long,
        newEpisodesCount: Int,
        posterPath: String? = null
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = buildDeepLinkIntent(context, showId, "tv")
        val pendingIntent = PendingIntent.getActivity(
            context,
            // Use a different request code range (offset by Int.MAX_VALUE/2) to avoid
            // collisions with release notifications
            (showId + 1_000_000L).toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bodyText = if (newEpisodesCount == 1) {
            context.getString(R.string.notif_new_episodes_body_single, showTitle)
        } else {
            context.getString(R.string.notif_new_episodes_body_plural, newEpisodesCount, showTitle)
        }

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val largeIcon = loadPosterBitmap(context, posterPath)
                ?: getBitmapFromVectorDrawable(context, R.drawable.ic_launcher_foreground_vector)

            val notification = NotificationCompat.Builder(context, EPISODES_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(context.getString(R.string.notif_new_episodes_title))
                .setContentText(bodyText)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(bodyText)
                        .setBigContentTitle(context.getString(R.string.notif_new_episodes_title))
                        .setSummaryText(context.getString(R.string.notif_update_summary))
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setColor(ACCENT_COLOR)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .build()

            NotificationManagerCompat.from(context).notify((showId + 1_000_000L).toInt(), notification)
        }
    }

    private suspend fun loadPosterBitmap(context: Context, posterPath: String?): android.graphics.Bitmap? {
        if (posterPath.isNullOrBlank()) return null
        return try {
            val url = buildTmdbImageUrl(posterPath, ImageType.POSTER, ImageQuality.MEDIUM) ?: return null
            val request = coil.request.ImageRequest.Builder(context)
                .data(url)
                .size(300)
                .allowHardware(false)
                .build()
            val result = coil.Coil.imageLoader(context).execute(request)
            result.drawable?.toBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ── Deep-link intent ──────────────────────────────────────────────────────

    private fun buildDeepLinkIntent(context: Context, mediaId: Long, mediaType: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data = android.net.Uri.parse("flicktrove://media/$mediaType/$mediaId")
        }

    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): android.graphics.Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        val bitmap = android.graphics.Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
