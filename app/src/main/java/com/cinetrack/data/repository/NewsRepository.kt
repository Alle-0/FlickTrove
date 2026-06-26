package com.cinetrack.data.repository

import android.util.Xml
import com.cinetrack.data.api.NewsService
import com.cinetrack.data.model.NewsItem
import com.cinetrack.data.repository.PreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Locale

@Singleton
class NewsRepository @Inject constructor(
    private val newsService: NewsService,
    private val preferenceRepository: PreferenceRepository
) {
    suspend fun getNews(): List<NewsItem> = withContext(Dispatchers.IO) {
        val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
        val resolvedLanguage = if (rawLanguage == "system") {
            Locale.getDefault().language
        } else {
            rawLanguage
        }

        val feedUrl = if (resolvedLanguage == "it") {
            "https://cinema.everyeye.it/feed/"
        } else {
            "https://collider.com/feed/"
        }

        val response = newsService.getNewsFeed(feedUrl)
        val inputStream = response.byteStream()
        
        parseRss(inputStream)
    }

    private fun parseRss(inputStream: InputStream): List<NewsItem> {
        val newsItems = mutableListOf<NewsItem>()
        inputStream.use { stream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(stream, null)
            
            var eventType = parser.eventType
            var currentTitle = ""
            var currentLink = ""
            var currentImageUrl: String? = null
            var currentPubDate = ""
            var insideItem = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("item", ignoreCase = true)) {
                            insideItem = true
                            currentTitle = ""
                            currentLink = ""
                            currentImageUrl = null
                            currentPubDate = ""
                        } else if (insideItem) {
                            when {
                                name.equals("title", ignoreCase = true) -> currentTitle = parser.nextText()
                                name.equals("link", ignoreCase = true) -> currentLink = parser.nextText()
                                name.equals("pubDate", ignoreCase = true) -> currentPubDate = parser.nextText()
                                name.equals("enclosure", ignoreCase = true) -> {
                                    val url = parser.getAttributeValue(null, "url")
                                    if (url != null && currentImageUrl == null) {
                                        currentImageUrl = url
                                    }
                                }
                                name.equals("content", ignoreCase = true) || name.equals("media:content", ignoreCase = true) -> {
                                    val url = parser.getAttributeValue(null, "url")
                                    if (url != null && currentImageUrl == null) {
                                        currentImageUrl = url
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.equals("item", ignoreCase = true)) {
                            insideItem = false
                            newsItems.add(NewsItem(currentTitle, currentLink, currentImageUrl, currentPubDate))
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        return newsItems
    }
}
