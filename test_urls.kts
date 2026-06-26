import java.net.HttpURLConnection
import java.net.URL

fun testUrl(urlString: String) {
    try {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        connection.connectTimeout = 5000
        val code = connection.responseCode
        println("$urlString -> $code")
    } catch(e: Exception) {
        println("$urlString -> ERROR: ${e.message}")
    }
}

testUrl("https://screenrant.com/feed/movie-news/")
testUrl("https://collider.com/feed/")
testUrl("https://deadline.com/v/film/feed/")
testUrl("https://variety.com/v/film/feed/")
