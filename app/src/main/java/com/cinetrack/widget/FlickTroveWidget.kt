package com.cinetrack.widget

import androidx.glance.LocalContext
import com.cinetrack.R
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.ColorFilter
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.flow.first
import com.cinetrack.di.dataStore
import com.cinetrack.MainActivity
import com.cinetrack.data.model.Movie
import com.cinetrack.data.local.database.FlickTroveDatabase
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun movieRepository(): MovieRepository
}

private data class WidgetMovieData(
    val movie: Movie,
    val backdropBitmap: Bitmap?,
    val logoBitmap: Bitmap?
)

class FlickTroveWidget : GlanceAppWidget() {
    
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

        provideContent {
            androidx.compose.runtime.CompositionLocalProvider(androidx.glance.LocalContext provides localizedContext) {
                SingleMovieWidgetContent(localizedContext)
            }
        }
    }

    @Composable
    private fun SingleMovieWidgetContent(context: Context) {
        var movieData by remember { mutableStateOf<WidgetMovieData?>(null) }

        LaunchedEffect(Unit) {
            val db = FlickTroveDatabase.getInstance(context)
            val allMovies = db.favoriteDao().getAll()
            
            val todayIso = java.time.LocalDate.now().toString()
            var upcomingMovie: Movie? = null
            var minDistanceDays = Long.MAX_VALUE
            
            // Cerca il prossimo film in arrivo salvato specificamente con reminder (Remind me)
            val remindedMovies = allMovies.filter { it.reminder && !it.watched }
            for (movie in remindedMovies) {
                val dateStr = movie.releaseDate ?: movie.firstAirDate
                if (dateStr != null && dateStr.length >= 10 && dateStr >= todayIso) {
                    try {
                        val releaseDate = java.time.LocalDate.parse(dateStr.take(10))
                        val todayDate = java.time.LocalDate.now()
                        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(todayDate, releaseDate)
                        if (daysBetween in 0 until minDistanceDays) {
                            minDistanceDays = daysBetween
                            upcomingMovie = movie
                        }
                    } catch (e: java.time.format.DateTimeParseException) {
                        // Ignora formati non validi
                    }
                }
            }
            
            val movieToShow = upcomingMovie
            
            if (movieToShow != null) {
                val posterPath = movieToShow.backdropPath ?: movieToShow.posterPath
                var backdropBitmap: Bitmap? = null
                if (posterPath != null) {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(buildTmdbImageUrl(posterPath, ImageType.BACKDROP, ImageQuality.HIGH))
                            .size(600)
                            .allowHardware(false)
                            .build()
                        val result = context.imageLoader.execute(request)
                        backdropBitmap = result.drawable?.toBitmap()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                var logoBitmap: Bitmap? = null
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        WidgetEntryPoint::class.java
                    )
                    val movieRepository = entryPoint.movieRepository()
                    val isTv = movieToShow.mediaType == "tv"
                    val logoPath = movieToShow.logoPath ?: movieRepository.getMovieLogo(movieToShow.id, isTv)
                    if (!logoPath.isNullOrEmpty()) {
                        val logoRequest = ImageRequest.Builder(context)
                            .data(buildTmdbImageUrl(logoPath, ImageType.LOGO, ImageQuality.HIGH))
                            .size(360)
                            .allowHardware(false)
                            .build()
                        val logoResult = context.imageLoader.execute(logoRequest)
                        logoBitmap = logoResult.drawable?.toBitmap()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                movieData = WidgetMovieData(movieToShow, backdropBitmap, logoBitmap)
            } else {
                movieData = null
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val m = movieData?.movie
            if (m != null) {
                data = Uri.parse("flicktrove://detail/${m.mediaType}/${m.id}")
            }
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF1C1C1E))
                .cornerRadius(24.dp)
                .clickable(actionStartActivity(intent))
        ) {
            val currentData = movieData
            if (currentData == null) {
                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier.fillMaxWidth()
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_calendario),
                            contentDescription = null,
                            modifier = GlanceModifier.size(32.dp),
                            colorFilter = ColorFilter.tint(
                                androidx.glance.color.ColorProvider(day = Color(0xFFAAAAAA), night = Color(0xFF777777))
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text(
                            text = LocalContext.current.getString(R.string.widget_no_releases),
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(day = Color.LightGray, night = Color.LightGray),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            } else {
                val movie = currentData.movie
                val bitmap = currentData.backdropBitmap
                val logoBitmap = currentData.logoBitmap
                
                if (bitmap != null) {
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = movie.title ?: movie.name ?: "",
                        modifier = GlanceModifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = movie.title ?: movie.name ?: "",
                            style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.Gray, night = Color.Gray), fontSize = 14.sp)
                        )
                    }
                }
                
                // Overlay content at the bottom with deep smooth gradient
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ImageProvider(R.drawable.widget_bottom_gradient))
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        if (logoBitmap != null) {
                            val logoWidthDp = if (logoBitmap.height > 0) {
                                val ratio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
                                (34f * ratio).coerceIn(24f, 180f).dp
                            } else {
                                160.dp
                            }
                            Image(
                                provider = ImageProvider(logoBitmap),
                                contentDescription = movie.title ?: movie.name ?: "",
                                modifier = GlanceModifier
                                    .height(34.dp)
                                    .width(logoWidthDp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = movie.title ?: movie.name ?: "",
                                style = TextStyle(
                                    color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        
                        val date = movie.releaseDate ?: movie.firstAirDate
                        if (!date.isNullOrEmpty()) {
                            val formattedDate = if (date.length >= 10) {
                                try {
                                    val dateObj = java.time.LocalDate.parse(date.take(10))
                                    val locale = context.resources.configuration.locales[0]
                                    val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", locale)
                                    dateObj.format(formatter)
                                } catch (e: Exception) { date }
                            } else date

                            Spacer(modifier = GlanceModifier.height(6.dp))

                            // Badge a pillola moderno con la data di uscita
                            Row(
                                modifier = GlanceModifier
                                    .background(Color(0x33FFFFFF))
                                    .cornerRadius(10.dp)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formattedDate,
                                    style = TextStyle(
                                        color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Top-end Search Action with subtle translucent container
                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(36.dp)
                            .background(Color(0x66000000))
                            .cornerRadius(18.dp)
                            .clickable(actionStartActivity(Intent(context, MainActivity::class.java).apply {
                                action = Intent.ACTION_VIEW
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                data = Uri.parse("flicktrove://search")
                            })),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_lente),
                            contentDescription = "Cerca",
                            modifier = GlanceModifier.size(16.dp),
                            colorFilter = ColorFilter.tint(androidx.glance.color.ColorProvider(day = Color.White, night = Color.White))
                        )
                    }
                }
            }
        }
    }
}