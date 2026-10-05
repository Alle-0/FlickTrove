package com.cinetrack.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import com.cinetrack.MainActivity
import com.cinetrack.R
import com.cinetrack.data.local.database.FlickTroveDatabase
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.di.dataStore
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WatchlistWidgetEntryPoint {
    fun movieRepository(): MovieRepository
}

data class WatchlistBannerEntry(
    val id: Long,
    val title: String,
    val bannerBitmap: Bitmap?
)

class FlickTroveMoviesWatchlistWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefRepo = com.cinetrack.data.repository.PreferenceRepository(context.applicationContext.dataStore)
        val language = prefRepo.userPreferencesFlow.first().contentLanguage

        val localizedContext = if (language != "system") {
            val tag = when (language) {
                "pt" -> "pt-BR"
                "zh" -> "zh-CN"
                "in" -> "id"
                else -> language
            }
            val locale = java.util.Locale.forLanguageTag(tag.replace("_", "-"))
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } else {
            context
        }

        val entries = loadEntries(localizedContext)

        provideContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalContext provides localizedContext) {
                MoviesWatchlistWidgetContent(localizedContext, entries)
            }
        }
    }

    private suspend fun loadEntries(context: Context): List<WatchlistBannerEntry> {
        return try {
            val db = FlickTroveDatabase.getInstance(context)
            val allMovies = db.favoriteDao().getAll()

            // Filtra film non visti e non abbandonati che appartengono effettivamente alla watchlist (favorite == true), ordinati per aggiunti di recente
            val toWatch = allMovies.filter { movie ->
                (movie.mediaType == "movie" || movie.mediaType.isEmpty()) &&
                    movie.favorite &&
                    !movie.watched &&
                    !movie.dropped
            }.sortedByDescending { movie ->
                movie.clientUpdatedAt.takeIf { it > 0 }
                    ?: movie.touched.takeIf { (it ?: 0L) > 0L }
                    ?: 0L
            }

            // Mostra fino a 10 film per garantire nitidezza massima e rimanere ampiamente nei limiti IPC di RemoteViews
            val topMovies = toWatch.take(10)

            val entryPoint = try {
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WatchlistWidgetEntryPoint::class.java
                )
            } catch (_: Exception) { null }
            val movieRepository = entryPoint?.movieRepository()

            coroutineScope {
                topMovies.map { movie ->
                    async {
                        val title = movie.title ?: movie.name ?: ""
                        val backdropPath = movie.backdropPath ?: movie.posterPath

                        // 1. Scarica backdrop in risoluzione ottimizzata
                        var backdropRaw: Bitmap? = null
                        if (!backdropPath.isNullOrEmpty()) {
                            try {
                                val req = ImageRequest.Builder(context)
                                    .data(buildTmdbImageUrl(backdropPath, ImageType.BACKDROP, ImageQuality.MEDIUM))
                                    .size(720, 240)
                                    .allowHardware(false)
                                    .build()
                                val drawable = context.imageLoader.execute(req).drawable
                                backdropRaw = drawable?.toBitmap()
                            } catch (_: Exception) {}
                        }

                        // 2. Scarica logo ufficiale per bordi e font definiti
                        var logoRaw: Bitmap? = null
                        try {
                            val logoPath = movie.logoPath ?: movieRepository?.getMovieLogo(movie.id, false)
                            if (!logoPath.isNullOrEmpty()) {
                                val logoReq = ImageRequest.Builder(context)
                                    .data(buildTmdbImageUrl(logoPath, ImageType.LOGO, ImageQuality.MEDIUM))
                                    .size(480, 180)
                                    .allowHardware(false)
                                    .build()
                                val drawable = context.imageLoader.execute(logoReq).drawable
                                logoRaw = drawable?.toBitmap()
                            }
                        } catch (_: Exception) {}

                        // 3. Componi backdrop + velatura scura + logo/titolo in un unico bitmap nitido ad alta risoluzione (720x216)
                        val finalBanner = if (backdropRaw != null) {
                            createBannerBitmap(
                                backdrop = backdropRaw,
                                logo = logoRaw,
                                title = title,
                                targetWidth = 720,
                                targetHeight = 216
                            )
                        } else null

                        WatchlistBannerEntry(
                            id = movie.id,
                            title = title,
                            bannerBitmap = finalBanner
                        )
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            Log.e("MoviesWatchlistWidget", "Error loading entries in provideGlance", e)
            emptyList()
        }
    }

    /**
     * Fonde backdrop ad alta risoluzione, gradiente scuro di contrasto e logo ufficiale
     * in un unico Bitmap compatto e ultra-definito (zero sfocatura, zero memoria duplicata).
     */
    private fun createBannerBitmap(
        backdrop: Bitmap,
        logo: Bitmap?,
        title: String,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }

        // Crop proporzionale per riempire l'intero banner
        val srcW = backdrop.width
        val srcH = backdrop.height
        val srcAspect = srcW.toFloat() / srcH
        val dstAspect = targetWidth.toFloat() / targetHeight
        val srcRect = if (srcAspect > dstAspect) {
            val cropW = (srcH * dstAspect).toInt()
            val left = (srcW - cropW) / 2
            Rect(left, 0, left + cropW, srcH)
        } else {
            val cropH = (srcW / dstAspect).toInt()
            val top = (srcH - cropH) / 2
            Rect(0, top, srcW, top + cropH)
        }
        val dstRect = Rect(0, 0, targetWidth, targetHeight)
        canvas.drawBitmap(backdrop, srcRect, dstRect, paint)

        // Velatura scura calibrata per dare leggibilità al logo
        val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(105, 0, 0, 0)
            isDither = true
        }
        canvas.drawRect(dstRect, scrimPaint)

        // Disegna il logo centrato o il titolo in stile cinema proporzionato
        if (logo != null && logo.width > 0 && logo.height > 0) {
            val maxLogoW = (targetWidth * 0.48f).toInt()
            val maxLogoH = (targetHeight * 0.42f).toInt()
            val scale = minOf(maxLogoW.toFloat() / logo.width, maxLogoH.toFloat() / logo.height)
            val scaledW = (logo.width * scale).toInt()
            val scaledH = (logo.height * scale).toInt()
            val left = (targetWidth - scaledW) / 2
            val top = (targetHeight - scaledH) / 2
            val logoDst = Rect(left, top, left + scaledW, top + scaledH)
            canvas.drawBitmap(logo, null, logoDst, paint)
        } else {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                textSize = targetHeight * 0.155f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.06f
                isDither = true
            }
            val yPos = (targetHeight / 2 - (textPaint.descent() + textPaint.ascent()) / 2)
            canvas.drawText(title.uppercase(), (targetWidth / 2).toFloat(), yPos, textPaint)
        }

        return result
    }

    @Composable
    private fun MoviesWatchlistWidgetContent(
        context: Context,
        entries: List<WatchlistBannerEntry>
    ) {
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
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(searchIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = LocalContext.current.getString(R.string.widget_movies_watchlist_empty),
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
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                ) {
                    // Header fisso classico FlickTrove
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(4.dp)
                                .height(16.dp)
                                .background(Color(0xFFFFC107))
                                .cornerRadius(2.dp)
                        ) {}

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        Text(
                            text = LocalContext.current.getString(R.string.widget_movies_watchlist_title),
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
                                    androidx.glance.color.ColorProvider(
                                        day = Color.White,
                                        night = Color.White
                                    )
                                )
                            )
                        }
                    }

                    // Lista banner a scorrimento verticale con itemId stabili per abilitare il riciclo e lo scroll fluido
                    LazyColumn(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                            .padding(horizontal = 14.dp)
                    ) {
                        items(entries, itemId = { it.id }) { entry ->
                            MovieBannerCard(entry, context)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MovieBannerCard(entry: WatchlistBannerEntry, context: Context) {
        val detailIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse("flicktrove://detail/movie/${entry.id}")
        }

        // Box contenitore della riga con margine inferiore esplicito (padding bottom)
        // per garantire spazio fisico visibile tra le card in RemoteViews
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (entry.bannerBitmap != null) {
                Image(
                    provider = ImageProvider(entry.bannerBitmap),
                    contentDescription = entry.title,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .cornerRadius(14.dp)
                        .clickable(actionStartActivity(detailIntent)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .cornerRadius(14.dp)
                        .clickable(actionStartActivity(detailIntent))
                        .background(
                            androidx.glance.color.ColorProvider(
                                day = Color(0xFFE5E5EA),
                                night = Color(0xFF1E1E24)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.title,
                        maxLines = 1,
                        style = TextStyle(
                            color = androidx.glance.color.ColorProvider(
                                day = Color(0xFF151517),
                                night = Color.White
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
