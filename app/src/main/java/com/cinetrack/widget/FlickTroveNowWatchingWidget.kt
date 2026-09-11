package com.cinetrack.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.flow.first
import com.cinetrack.R
import com.cinetrack.MainActivity
import com.cinetrack.data.local.database.FlickTroveDatabase
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.di.dataStore
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl

// ─────────────────────────────────────────────────────────────────────────────
// Una riga per ogni (serie × stagione) con episodio da vedere.
// Mostra il PRIMO episodio non visto di quella stagione + pulsante ✓.
// ─────────────────────────────────────────────────────────────────────────────
private data class EpisodeEntry(
    val showId: Long,
    val showName: String,
    val imdbId: String?,
    val mediaType: String,
    val bitmap: Bitmap?,
    val seasonNum: Int,
    val episodeNum: Int,
    val label: String   // es. "S01E03"
) {
    val itemId: Long get() = showId * 100_000L + seasonNum * 1_000L + episodeNum
}

/**
 * Widget "Now Watching" — lista episodi in visione.
 *
 * Mostra 1 episodio per stagione di ogni serie che stai guardando.
 * Cliccando ✓ l'episodio viene segnato come visto e compare immediatamente quello successivo.
 */
class FlickTroveNowWatchingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefRepo = PreferenceRepository(context.applicationContext.dataStore)
        val language = prefRepo.userPreferencesFlow.first().contentLanguage

        val localizedContext = if (language != "system") {
            val locale = java.util.Locale.forLanguageTag(language.replace("_", "-"))
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } else {
            context
        }

        val entries = loadEntries(localizedContext)

        provideContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalContext provides localizedContext) {
                NowWatchingWidgetContent(localizedContext, entries)
            }
        }
    }

    private suspend fun loadEntries(context: Context): List<EpisodeEntry> {
        return try {
            val db = FlickTroveDatabase.getInstance(context)
            val allShows = db.favoriteDao().getAll()
            val todayIso = java.time.LocalDate.now().toString()

            val availableShows = allShows.filter { show ->
                show.mediaType == "tv" &&
                    !show.watched &&
                    !show.dropped &&
                    ((show.watchedEpisodes?.values?.sumOf { it.size } ?: 0) > 0 || show.favorite)
            }.sortedByDescending { it.clientUpdatedAt }

            // Build candidate entries first without loading posters (1 per serie)
            val rawEntries = buildEntries(availableShows, emptyMap(), todayIso)
            // Cap to max 8 items so Glance RemoteViews stay well below 1MB Binder transaction limit
            val cappedEntries = rawEntries.take(8)

            val neededShows = availableShows.filter { show ->
                cappedEntries.any { it.showId == show.id }
            }

            val posterCache = mutableMapOf<Long, Bitmap?>()
            for (show in neededShows) {
                val path = show.posterPath ?: show.backdropPath ?: continue
                try {
                    val req = ImageRequest.Builder(context)
                        .data(buildTmdbImageUrl(path, ImageType.POSTER, ImageQuality.LOW))
                        .size(104, 156)
                        .bitmapConfig(Bitmap.Config.RGB_565)
                        .allowHardware(false)
                        .build()
                    val drawable = context.imageLoader.execute(req).drawable
                    posterCache[show.id] = drawable?.toBitmap(width = 104, height = 156, config = Bitmap.Config.RGB_565)
                } catch (_: Exception) {}
            }

            cappedEntries.map { entry ->
                entry.copy(bitmap = posterCache[entry.showId])
            }
        } catch (e: Exception) {
            Log.e("NowWatchingWidget", "Error loading entries in provideGlance", e)
            emptyList()
        }
    }

    // ── UI ───────────────────────────────────────────────────────────────────

    @Composable
    private fun NowWatchingWidgetContent(context: Context, entries: List<EpisodeEntry>) {
        val searchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse("flicktrove://search")
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(
                    androidx.glance.color.ColorProvider(
                        day = Color(0xFFF5F5F5),
                        night = Color(0xFF151517)
                    )
                )
                .cornerRadius(20.dp)
        ) {
            if (entries.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(searchIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = LocalContext.current.getString(R.string.widget_now_watching_empty),
                        style = TextStyle(
                            color = androidx.glance.color.ColorProvider(
                                day = Color.DarkGray,
                                night = Color.LightGray
                            ),
                            fontSize = 13.sp
                        )
                    )
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize()) {

                    // Header
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(4.dp).height(16.dp)
                                .background(Color(0xFF1DB954))
                                .cornerRadius(2.dp)
                        ) {}
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = LocalContext.current.getString(R.string.widget_now_watching_title),
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(
                                    day = Color(0xFF333333),
                                    night = Color.White
                                ),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Box(
                            modifier = GlanceModifier
                                .size(32.dp)
                                .background(ImageProvider(R.drawable.widget_search_bg))
                                .clickable(actionStartActivity(searchIntent)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_lente),
                                contentDescription = "Search",
                                modifier = GlanceModifier.size(16.dp),
                                colorFilter = androidx.glance.ColorFilter.tint(
                                    androidx.glance.color.ColorProvider(Color.White, Color.White)
                                )
                            )
                        }
                    }

                    // Lista episodi (1 per stagione)
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                        items(entries, itemId = { it.itemId }) { entry ->
                            EpisodeRow(entry)
                        }
                    }
                }
            }
        }
    }

    // ── Riga episodio ─────────────────────────────────────────────────────────

    @Composable
    private fun EpisodeRow(entry: EpisodeEntry) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Poster
                Box(
                    modifier = GlanceModifier
                        .width(52.dp).height(78.dp)
                        .cornerRadius(8.dp)
                        .background(Color(0xFF222222)),
                    contentAlignment = Alignment.Center
                ) {
                    if (entry.bitmap != null) {
                        Image(
                            provider = ImageProvider(entry.bitmap),
                            contentDescription = entry.showName,
                            modifier = GlanceModifier.fillMaxSize().cornerRadius(8.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = LocalContext.current.getString(R.string.widget_no_img),
                            style = TextStyle(color = androidx.glance.color.ColorProvider(Color.Gray, Color.Gray), fontSize = 9.sp)
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                // Titolo + episodio
                Column(modifier = GlanceModifier.defaultWeight().padding(end = 8.dp)) {
                    Text(
                        text = entry.showName,
                        style = TextStyle(
                            color = androidx.glance.color.ColorProvider(Color.Black, Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = entry.label,
                        style = TextStyle(
                            color = androidx.glance.color.ColorProvider(Color(0xFF1DB954), Color(0xFF1DB954)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Pulsante ✓
                Box(
                    modifier = GlanceModifier
                        .size(36.dp)
                        .cornerRadius(18.dp)
                        .background(
                            androidx.glance.color.ColorProvider(Color(0xFF1DB954), Color(0xFF1DB954))
                        )
                        .clickable(
                            actionRunCallback<MarkEpisodeWatchedCallback>(
                                actionParametersOf(
                                    MarkEpisodeWatchedCallback.KEY_SHOW_ID to entry.showId,
                                    MarkEpisodeWatchedCallback.KEY_SHOW_ID_STR to entry.showId.toString(),
                                    MarkEpisodeWatchedCallback.KEY_MEDIA_TYPE to entry.mediaType,
                                    MarkEpisodeWatchedCallback.KEY_EPISODE_STRING to entry.label,
                                    MarkEpisodeWatchedCallback.KEY_SEASON_NUM to entry.seasonNum,
                                    MarkEpisodeWatchedCallback.KEY_EPISODE_NUM to entry.episodeNum,
                                    MarkEpisodeWatchedCallback.KEY_IMDB_ID to (entry.imdbId ?: "")
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_tick),
                        contentDescription = LocalContext.current.getString(R.string.widget_mark_watched),
                        modifier = GlanceModifier.size(18.dp),
                        colorFilter = androidx.glance.ColorFilter.tint(
                            androidx.glance.color.ColorProvider(Color.White, Color.White)
                        )
                    )
                }
            }

            // Divider
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth().padding(horizontal = 16.dp).height(1.dp)
                    .background(
                        androidx.glance.color.ColorProvider(Color(0xFFE0E0E0), Color(0xFF222225))
                    )
            ) {}
        }
    }

    // ── Costruzione lista ─────────────────────────────────────────────────────

    /**
     * Costruisce esattamente 1 riga per ogni serie TV in visione,
     * mostrando il PRIMO episodio non visto (prossimo da guardare).
     * Quando l'episodio viene segnato come visto con ✓, compare quello successivo.
     */
    private fun buildEntries(
        shows: List<Movie>,
        posterCache: Map<Long, Bitmap?>,
        todayIso: String
    ): List<EpisodeEntry> {
        val result = mutableListOf<EpisodeEntry>()

        for (show in shows) {
            val name = show.name ?: show.title ?: continue
            val bitmap = posterCache[show.id]
            val unwatched = collectFirstUnwatchedEpisode(show, todayIso)

            if (unwatched != null) {
                val (s, e) = unwatched
                val sStr = s.toString().padStart(2, '0')
                val eStr = e.toString().padStart(2, '0')
                result.add(
                    EpisodeEntry(
                        showId = show.id,
                        showName = name,
                        imdbId = show.imdbId,
                        mediaType = show.mediaType ?: "tv",
                        bitmap = bitmap,
                        seasonNum = s,
                        episodeNum = e,
                        label = "S${sStr}E${eStr}"
                    )
                )
            } else {
                // Fallback se seasons non popolato ma c'è nextEpisodeString
                val next = show.nextEpisodeString
                if (!next.isNullOrEmpty()) {
                    val match = Regex("""[Ss](\d+)[Ee](\d+)""").find(next)
                    val s = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    val e = match?.groupValues?.get(2)?.toIntOrNull() ?: 1
                    result.add(
                        EpisodeEntry(
                            showId = show.id,
                            showName = name,
                            imdbId = show.imdbId,
                            mediaType = show.mediaType ?: "tv",
                            bitmap = bitmap,
                            seasonNum = s,
                            episodeNum = e,
                            label = next.uppercase()
                        )
                    )
                }
            }
        }

        return result
    }

    /**
     * Restituisce il PRIMO episodio in assoluto non ancora visto per la serie
     * (ordinato per stagione e numero episodio rilasciato fino ad oggi).
     * Esclude speciali (stagione 0) ed episodi futuri non ancora trasmessi.
     */
    private fun collectFirstUnwatchedEpisode(show: Movie, todayIso: String): Pair<Int, Int>? {
        val seasons = show.seasons ?: return null

        var nextAiringSeason: Int? = null
        var nextAiringEpNum: Int? = null
        if (!show.nextEpisodeString.isNullOrBlank() && (show.nextEpisodeAirDate.isNullOrBlank() || show.nextEpisodeAirDate!! > todayIso)) {
            try {
                val match = Regex("""[Ss](\d+)[Ee](\d+)""").find(show.nextEpisodeString!!)
                if (match != null) {
                    nextAiringSeason = match.groupValues[1].toIntOrNull()
                    nextAiringEpNum = match.groupValues[2].toIntOrNull()
                }
            } catch (_: Exception) {}
        }

        val validSeasons = seasons.filter { (it.seasonNumber ?: 0) > 0 }.sortedBy { it.seasonNumber }

        for (season in validSeasons) {
            val sNum = season.seasonNumber ?: continue
            val watchedEps = show.watchedEpisodes?.get(sNum.toString()) ?: emptyList()

            // 1. Se la stagione ha la lista degli episodi dettagliata
            val epsList = season.episodes
            if (!epsList.isNullOrEmpty()) {
                val firstUnwatched = epsList
                    .filter { ep ->
                        val d = ep.airDate
                        (d.isNullOrEmpty() || d.take(10) <= todayIso) && !watchedEps.contains(ep.episodeNumber)
                    }
                    .minByOrNull { it.episodeNumber }

                if (firstUnwatched != null) {
                    return sNum to firstUnwatched.episodeNumber
                }
            } else {
                // 2. Altrimenti usa il conteggio degli episodi rilasciati
                val airedCount = show.getReleasedEpisodeCountForSeason(season, todayIso, nextAiringSeason, nextAiringEpNum)
                if (airedCount > 0) {
                    for (ep in 1..airedCount) {
                        if (!watchedEps.contains(ep)) {
                            return sNum to ep
                        }
                    }
                }
            }
        }

        return null
    }
}
