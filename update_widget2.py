import re

file_path = "app/src/main/java/com/cinetrack/widget/FlickTroveWidget.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Change poster priority and size
content = content.replace(
    "val posterPath = movieToShow.posterPath ?: movieToShow.backdropPath",
    "val posterPath = movieToShow.backdropPath ?: movieToShow.posterPath"
)
content = content.replace(
    ".data(\"https://image.tmdb.org/t/p/w500$posterPath\")",
    ".data(\"https://image.tmdb.org/t/p/w780$posterPath\")"
)

# 2. Add the title and date overlay
# Find the end of the else block inside the main widget box
# We will inject the overlay just before the search button overlay.
overlay_code = """                
                // Overlay text at the bottom
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(Color(0xB3000000)) // Semi-transparent black
                            .padding(12.dp)
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
                
                Box("""

content = content.replace("                Box(\n                    modifier = GlanceModifier.fillMaxSize().padding(12.dp),\n                    contentAlignment = Alignment.TopEnd\n                )", overlay_code + "\n                    modifier = GlanceModifier.fillMaxSize().padding(12.dp),\n                    contentAlignment = Alignment.TopEnd\n                )")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
