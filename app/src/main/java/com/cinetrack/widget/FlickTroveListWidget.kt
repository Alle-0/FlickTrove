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
import com.cinetrack.data.Movie
import com.cinetrack.data.local.database.FlickTroveDatabase

class FlickTroveListWidget : GlanceAppWidget() {
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefRepo = com.cinetrack.data.repository.PreferenceRepository(context.dataStore)
        val language = prefRepo.userPreferencesFlow.first().contentLanguage
        
        val localizedContext = if (language != "system") {
            val locale = java.util.Locale.forLanguageTag(language.replace("_", "-"))
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
            
            val toWatch = allMovies.filter { !it.watched }
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
            }.take(15) // Can show more items in a vertical list
            
            val loadedList = mutableListOf<Pair<Movie, Bitmap?>>()
            
            for (movie in topUpcoming) {
                val posterPath = movie.posterPath ?: movie.backdropPath
                var bitmap: Bitmap? = null
                if (posterPath != null) {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(buildTmdbImageUrl(posterPath, ImageType.POSTER, ImageQuality.LOW))
                            .size(300) // Keep it small to avoid RemoteViews memory limit
                            .allowHardware(false)
                            .build()
                        val result = context.imageLoader.execute(request)
                        bitmap = result.drawable?.toBitmap()
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
                                    Spacer(modifier = GlanceModifier.height(6.dp))
                                    val dateStr = movie.releaseDate ?: movie.firstAirDate
                                    var formattedDate = dateStr ?: ""
                                    if (!dateStr.isNullOrEmpty() && dateStr.length >= 10) {
                                        try {
                                            val dateObj = java.time.LocalDate.parse(dateStr.take(10))
                                            val locale = context.resources.configuration.locales[0]
                                            val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", locale)
                                            formattedDate = dateObj.format(formatter)
                                        } catch (e: Exception) {}
                                    }
                                    
                                    if (formattedDate.isNotEmpty()) {
                                        Text(
                                            text = formattedDate.uppercase(),
                                            style = TextStyle(
                                                color = androidx.glance.color.ColorProvider(day = Color(0xFF666666), night = Color(0xFFAAAAAA)),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
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
}
