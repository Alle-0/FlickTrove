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
import com.cinetrack.MainActivity
import com.cinetrack.R

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
        mediaType: String
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = buildDeepLinkIntent(context, movieId, mediaType)
        val pendingIntent = PendingIntent.getActivity(
            context,
            movieId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_foreground)

        val mediaLabel = if (mediaType == "tv") "Serie TV" else "Film"
        val bodyText = "\"$movieTitle\" è ora disponibile! Aprilo per scoprire di più."

        val notification = NotificationCompat.Builder(context, RELEASE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle("Nuova uscita: $mediaLabel")
            .setContentText("\"$movieTitle\" è disponibile!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bodyText)
                    .setBigContentTitle("Nuova uscita: $mediaLabel")
                    .setSummaryText("FlickTrove · Promemoria Uscite")
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

    /**
     * Shows a notification when a specific episode airs today.
     */
    fun showEpisodeReleaseNotification(
        context: Context,
        showTitle: String,
        showId: Long,
        seasonNumber: Int,
        episodeNumber: Int
    ) {
        val episodeString = "S%02dE%02d".format(seasonNumber, episodeNumber)
        showEpisodeReleaseNotification(context, showTitle, showId, episodeString)
    }

    fun showEpisodeReleaseNotification(
        context: Context,
        showTitle: String,
        showId: Long,
        episodeString: String
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

        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_foreground)
        val bodyText = "L'episodio $episodeString di \"$showTitle\" esce oggi!"

        val notification = NotificationCompat.Builder(context, RELEASE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle("Nuova uscita: Episodio $episodeString")
            .setContentText(bodyText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bodyText)
                    .setBigContentTitle("Nuova uscita: Episodio $episodeString")
                    .setSummaryText("FlickTrove · Promemoria Uscite")
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
        newEpisodesCount: Int
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

        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_foreground)

        val episodeWord = if (newEpisodesCount == 1) "episodio" else "episodi"
        val bodyText = "$newEpisodesCount nuov${if (newEpisodesCount == 1) "o" else "i"} $episodeWord " +
            "disponibil${if (newEpisodesCount == 1) "e" else "i"} per \"$showTitle\"."

        val notification = NotificationCompat.Builder(context, EPISODES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle("Nuovi episodi disponibili")
            .setContentText(bodyText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bodyText)
                    .setBigContentTitle("Nuovi episodi disponibili")
                    .setSummaryText("FlickTrove · Aggiornamento Serie")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(ACCENT_COLOR)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .build()

        NotificationManagerCompat.from(context).notify((showId + 1_000_000L).toInt(), notification)
    }

    // ── Deep-link intent ──────────────────────────────────────────────────────

    private fun buildDeepLinkIntent(context: Context, mediaId: Long, mediaType: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data = android.net.Uri.parse("flicktrove://media/$mediaType/$mediaId")
        }
}
