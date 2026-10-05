package com.cinetrack.widget

import androidx.glance.LocalContext
import com.cinetrack.R
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Row
import androidx.glance.layout.Column
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

class FlickTroveListWidget : GlanceAppWidget() {
    
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
                ListMovieWidgetContent(localizedContext)
            }
        }
    }

    @Composable
    private fun ListMovieWidgetContent(context: Context) {
        var moviesToWatch by remember { mutableStateOf<List<Pair<Movie, Bitmap?>>>(emptyList()) }

        LaunchedEffect(Unit) {
            val db = FlickTroveDatabase.getInstance(context)
            val allMovies = db.favoriteDao().getAll()
            
            val toWatch = allMovies.filter { (it.favorite || it.reminder) && !it.watched && !it.dropped }
            val todayIso = java.time.LocalDate.now().toString()
            
            // Filter only upcoming movies
            val upcomingMovies = toWatch.filter { movie ->
                val dateStr = movie.releaseDate ?: movie.firstAirDate
                if (dateStr != null && dateStr.length >= 10 && dateStr >= todayIso) {
                    try {
                        java.time.LocalDate.parse(dateStr.take(10))
                        true
                    } catch (e: java.time.format.DateTimeParseException) {
                        false
                    }
                } else {
                    false
                }
            }
            
            // Sort by release date ascending and take top 5
            val topUpcoming = upcomingMovies.sortedBy { movie ->
                movie.releaseDate ?: movie.firstAirDate
            }.take(8) // Keep list compact to stay well within Binder transaction limits
            
            val loadedList = mutableListOf<Pair<Movie, Bitmap?>>()
            
            for (movie in topUpcoming) {
                val posterPath = movie.posterPath ?: movie.backdropPath
                var bitmap: Bitmap? = null
                if (posterPath != null) {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(buildTmdbImageUrl(posterPath, ImageType.POSTER, ImageQuality.LOW))
                            .size(104, 156)
                            .bitmapConfig(Bitmap.Config.RGB_565)
                            .allowHardware(false)
                            .build()
                        val result = context.imageLoader.execute(request)
                        bitmap = result.drawable?.toBitmap(width = 104, height = 156, config = Bitmap.Config.RGB_565)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                loadedList.add(movie to bitmap)
            }
            
            moviesToWatch = loadedList
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(androidx.glance.color.ColorProvider(day = Color(0xFFF5F5F5), night = Color(0xFF151517)))
                .cornerRadius(20.dp)
        ) {
            if (moviesToWatch.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(), 
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = LocalContext.current.getString(R.string.widget_no_releases),
                        style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.DarkGray, night = Color.LightGray), fontSize = 14.sp)
                    )
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // Header fisso in alto
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(4.dp)
                                .height(16.dp)
                                .background(Color(0xFFE50914))
                                .cornerRadius(2.dp)
                        ) {}
                        
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        
                        Text(
                            text = LocalContext.current.getString(R.string.widget_coming_soon),
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(day = Color(0xFF333333), night = Color.White),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        
                        val searchIntent = Intent(context, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            data = Uri.parse("flicktrove://search")
                        }
                        
                        Box(
                            modifier = GlanceModifier
                                .size(32.dp)
                                .background(ImageProvider(R.drawable.widget_search_bg))
                                .clickable(actionStartActivity(searchIntent)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_lente),
                                contentDescription = "Cerca",
                                modifier = GlanceModifier.size(16.dp),
                                colorFilter = androidx.glance.ColorFilter.tint(androidx.glance.color.ColorProvider(day = Color.White, night = Color.White))
                            )
                        }
                    }

                    LazyColumn(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        items(moviesToWatch) { item ->
                            val movie = item.first
                            val bitmap = item.second
                            val intent = Intent(context, MainActivity::class.java).apply {
                                action = Intent.ACTION_VIEW
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                data = Uri.parse("flicktrove://detail/${movie.mediaType}/${movie.id}")
                            }
                            
                            Column(modifier = GlanceModifier.fillMaxWidth()) {
                                Row(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .clickable(actionStartActivity(intent)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                    modifier = GlanceModifier
                                        .width(60.dp)
                                        .height(90.dp)
                                        .cornerRadius(8.dp)
                                        .background(Color(0xFF222222)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (bitmap != null) {
                                        Image(
                                            provider = ImageProvider(bitmap),
                                            contentDescription = movie.title ?: movie.name ?: "",
                                            modifier = GlanceModifier.fillMaxSize().cornerRadius(8.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = LocalContext.current.getString(R.string.widget_no_img),
                                            style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.Gray, night = Color.Gray), fontSize = 10.sp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = GlanceModifier.width(14.dp))
                                
                                Column(modifier = GlanceModifier.fillMaxWidth().padding(end = 12.dp)) {
                                    Text(
                                        text = movie.title ?: movie.name ?: "",
                                        style = TextStyle(
                                            color = androidx.glance.color.ColorProvider(day = Color.Black, night = Color.White), 
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    val dateInfo = getMovieDateInfo(movie.releaseDate ?: movie.firstAirDate, context)
                                    if (dateInfo != null) {
                                        Spacer(modifier = GlanceModifier.height(5.dp))
                                        Box(
                                            modifier = GlanceModifier
                                                .background(
                                                    when {
                                                        dateInfo.isToday -> androidx.glance.color.ColorProvider(
                                                            day = Color(0xFFFFEBEE),
                                                            night = Color(0x38E50914)
                                                        )
                                                        dateInfo.isTomorrow -> androidx.glance.color.ColorProvider(
                                                            day = Color(0xFFFFF3E0),
                                                            night = Color(0x33FF9800)
                                                        )
                                                        else -> androidx.glance.color.ColorProvider(
                                                            day = Color(0xFFEEEEF2),
                                                            night = Color(0xFF25252B)
                                                        )
                                                    }
                                                )
                                                .cornerRadius(16.dp)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Image(
                                                    provider = ImageProvider(R.drawable.ic_calendario),
                                                    contentDescription = null,
                                                    modifier = GlanceModifier.size(10.dp),
                                                    colorFilter = androidx.glance.ColorFilter.tint(
                                                        when {
                                                            dateInfo.isToday -> androidx.glance.color.ColorProvider(
                                                                day = Color(0xFFE50914),
                                                                night = Color(0xFFFF4444)
                                                            )
                                                            dateInfo.isTomorrow -> androidx.glance.color.ColorProvider(
                                                                day = Color(0xFFEF6C00),
                                                                night = Color(0xFFFFB74D)
                                                            )
                                                            else -> androidx.glance.color.ColorProvider(
                                                                day = Color(0xFF666666),
                                                                night = Color(0xFFB8B8C0)
                                                            )
                                                        }
                                                    )
                                                )
                                                Spacer(modifier = GlanceModifier.width(4.dp))
                                                Text(
                                                    text = dateInfo.label,
                                                    style = TextStyle(
                                                        color = when {
                                                            dateInfo.isToday -> androidx.glance.color.ColorProvider(
                                                                day = Color(0xFFD32F2F),
                                                                night = Color(0xFFFF5252)
                                                            )
                                                            dateInfo.isTomorrow -> androidx.glance.color.ColorProvider(
                                                                day = Color(0xFFE65100),
                                                                night = Color(0xFFFFB74D)
                                                            )
                                                            else -> androidx.glance.color.ColorProvider(
                                                                day = Color(0xFF222222),
                                                                night = Color(0xFFEEEEEE)
                                                            )
                                                        },
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Divider
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(1.dp)
                                    .background(androidx.glance.color.ColorProvider(day = Color(0xFFE0E0E0), night = Color(0xFF222225)))
                            ) {}
                        }
                    }
                }
                }
            }
        }
    }

    private data class MovieDateInfo(
        val label: String,
        val isToday: Boolean,
        val isTomorrow: Boolean
    )

    private fun getMovieDateInfo(dateStr: String?, context: Context): MovieDateInfo? {
        if (dateStr.isNullOrEmpty() || dateStr.length < 10) return null
        return try {
            val targetDate = java.time.LocalDate.parse(dateStr.take(10))
            val today = java.time.LocalDate.now()
            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(today, targetDate)
            val locale = try {
                context.resources.configuration.locales[0]
            } catch (_: Exception) {
                java.util.Locale.getDefault()
            }

            val isToday = daysBetween == 0L
            val isTomorrow = daysBetween == 1L

            val dayMonthPattern = java.time.format.DateTimeFormatter.ofPattern("d MMM", locale)
            val formattedDayMonth = targetDate.format(dayMonthPattern).uppercase()

            val label = when {
                isToday -> {
                    val todayText = context.getString(R.string.widget_date_today).uppercase()
                    "$todayText • $formattedDayMonth"
                }
                isTomorrow -> {
                    val tomorrowText = context.getString(R.string.widget_date_tomorrow).uppercase()
                    "$tomorrowText • $formattedDayMonth"
                }
                daysBetween in 2L..6L -> {
                    val weekPattern = java.time.format.DateTimeFormatter.ofPattern("EEE • d MMM", locale)
                    targetDate.format(weekPattern).uppercase()
                }
                else -> {
                    val fullPattern = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", locale)
                    targetDate.format(fullPattern).uppercase()
                }
            }
            MovieDateInfo(label = label, isToday = isToday, isTomorrow = isTomorrow)
        } catch (e: Exception) {
            null
        }
    }
}
