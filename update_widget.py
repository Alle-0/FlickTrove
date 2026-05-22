import re

file_path = "app/src/main/java/com/cinetrack/widget/FlickTroveWidget.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Replace the LaunchEffect block
new_launched_effect = """        LaunchedEffect(Unit) {
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
                val posterPath = movieToShow.posterPath ?: movieToShow.backdropPath
                var bitmap: Bitmap? = null
                if (posterPath != null) {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data("https://image.tmdb.org/t/p/w500$posterPath")
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
        }"""

content = re.sub(r'        LaunchedEffect\(Unit\) \{.*?(?=        Box\()', new_launched_effect + '\n\n', content, flags=re.DOTALL)


# Replace the Box block
new_box = """        val intent = Intent(context, MainActivity::class.java).apply {
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
            }
        }"""

content = re.sub(r'        Box\(\s*modifier = GlanceModifier\s*\.fillMaxSize\(\)\s*\.background\(Color\(0xFF1C1C1E\)\).*', new_box + '\n    }\n}', content, flags=re.DOTALL)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
