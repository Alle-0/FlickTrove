package com.cinetrack.widget

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
import com.cinetrack.MainActivity
import com.cinetrack.R
import com.cinetrack.data.Movie
import com.cinetrack.data.local.database.FlickTroveDatabase

class FlickTroveWidget : GlanceAppWidget() {
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent(context)
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        var moviesToWatch by remember { mutableStateOf<List<Pair<Movie, Bitmap?>>>(emptyList()) }

        LaunchedEffect(Unit) {
            val db = FlickTroveDatabase.getInstance(context)
            val allMovies = db.favoriteDao().getAll()
            
            val toWatch = allMovies.filter { !it.watched }
            val todayIso = java.time.LocalDate.now().toString()
            var upcomingMovie: Movie? = null
            var minDistanceDays = Long.MAX_VALUE
            
            for (movie in toWatch) {
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
                    } catch (e: Exception) { }
                }
            }
            
            val movieToShow = upcomingMovie ?: toWatch.maxByOrNull { it.clientUpdatedAt }
            val loadedList = mutableListOf<Pair<Movie, Bitmap?>>()
            
            if (movieToShow != null) {
                val posterPath = movieToShow.backdropPath ?: movieToShow.posterPath
                var bitmap: Bitmap? = null
                if (posterPath != null) {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data("https://image.tmdb.org/t/p/w780$posterPath")
                            .size(600)
                            .allowHardware(false)
                            .build()
                        val result = context.imageLoader.execute(request)
                        bitmap = result.drawable?.toBitmap()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                loadedList.add(movieToShow to bitmap)
            }
            moviesToWatch = loadedList
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (moviesToWatch.isNotEmpty()) {
                val m = moviesToWatch[0].first
                data = Uri.parse("flicktrove://detail/${m.mediaType}/${m.id}")
            }
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF1C1C1E))
                .cornerRadius(16.dp)
                .clickable(actionStartActivity(intent))
        ) {
            if (moviesToWatch.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Niente da vedere!",
                        style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.LightGray, night = Color.LightGray), fontSize = 14.sp)
                    )
                }
            } else {
                val movie = moviesToWatch[0].first
                val bitmap = moviesToWatch[0].second
                
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
                
                
                // Overlay text at the bottom
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ImageProvider(R.drawable.widget_bottom_gradient)) // Gradient background
                            .padding(16.dp)
                    ) {
                        Text(
                            text = movie.title ?: movie.name ?: "",
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        val date = movie.releaseDate ?: movie.firstAirDate
                        if (!date.isNullOrEmpty()) {
                            Text(
                                text = "In uscita il: $date",
                                style = TextStyle(
                                    color = androidx.glance.color.ColorProvider(day = Color(0xFFAAAAAA), night = Color(0xFFAAAAAA)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = GlanceModifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                
                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(36.dp)
                            .background(Color(0xAA000000))
                            .cornerRadius(18.dp)
                            .clickable(actionStartActivity(Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                data = Uri.parse("flicktrove://search")
                            })),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_search),
                            contentDescription = "Cerca",
                            modifier = GlanceModifier.size(18.dp),
                            colorFilter = ColorFilter.tint(androidx.glance.color.ColorProvider(day = Color.White, night = Color.White))
                        )
                    }
                }
            }
        }
    }
}