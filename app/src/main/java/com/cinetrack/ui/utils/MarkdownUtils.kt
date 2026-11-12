package com.cinetrack.ui.utils

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.cinetrack.data.repository.MovieRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private val RAW_URL_REGEX = Regex("^(?:https?://|www\\.)[^\\s\\[\\]\\(\\)<>\"']+")
private val TMDB_URL_REGEX = Regex("""^https?://(?:www\.)?themoviedb\.org/(movie|tv)/(\d+)(?:-[^/?#]*)?(?:[?#].*)?$""", RegexOption.IGNORE_CASE)
val TMDB_MEDIA_LINK_REGEX = Regex("""\[(#?[^\]]+)\]\((https?://(?:www\.)?themoviedb\.org/(movie|tv)/(\d+)(?:-[^)]*)?)\)""", RegexOption.IGNORE_CASE)
private val NUMBERED_LIST_PREFIX_REGEX = Regex("""^(\d+\.)\s+(.*)$""")

data class TaggedMediaRef(
    val id: Long,
    val mediaType: String, // "movie" or "tv"
    val title: String,
    val posterPath: String? = null,
    val releaseYear: String? = null
)

fun buildTmdbMediaTagUrl(mediaType: String, id: Long): String {
    val safeType = if (mediaType == "tv") "tv" else "movie"
    return "https://www.themoviedb.org/$safeType/$id"
}

fun parseTmdbMediaUrl(url: String): Pair<String, Long>? {
    val match = TMDB_URL_REGEX.matchEntire(url.trim()) ?: return null
    val type = match.groupValues[1].lowercase()
    val id = match.groupValues[2].toLongOrNull() ?: return null
    if (id <= 0L) return null
    return type to id
}

fun extractTmdbMediaRefsFromText(text: String): List<Pair<String, Long>> {
    if (!text.contains("themoviedb.org", ignoreCase = true)) return emptyList()
    return TMDB_MEDIA_LINK_REGEX.findAll(text).mapNotNull { match ->
        val label = match.groupValues[1]
        if (!label.startsWith("#")) return@mapNotNull null
        val type = match.groupValues[3].lowercase()
        val id = match.groupValues[4].toLongOrNull() ?: return@mapNotNull null
        if (id > 0L) type to id else null
    }.distinct().toList()
}

data class ResolvedMediaTagMetadata(
    val titles: Map<String, String> = emptyMap(),
    val posters: Map<String, String> = emptyMap()
)

suspend fun resolveTmdbMediaTagsMetadata(
    refs: List<Pair<String, Long>>,
    movieRepository: MovieRepository,
    semaphore: Semaphore = Semaphore(4)
): ResolvedMediaTagMetadata {
    if (refs.isEmpty()) return ResolvedMediaTagMetadata()
    val entries = coroutineScope {
        refs.distinct().map { (type, id) ->
            async {
                val key = "$type:$id"
                val (localizedTitle, posterPath) = semaphore.withPermit {
                    try {
                        val details = movieRepository.fetchMovieDetails(id, isTv = type == "tv")
                        val title = (if (type == "tv") details.name ?: details.title else details.title ?: details.name)
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                        val poster = details.posterPath?.trim()?.takeIf { it.isNotEmpty() }
                        title to poster
                    } catch (_: Exception) {
                        try {
                            val local = movieRepository.getMovie(id, type)
                            val title = (local?.title ?: local?.name)?.trim()?.takeIf { it.isNotEmpty() }
                            val poster = local?.posterPath?.trim()?.takeIf { it.isNotEmpty() }
                            title to poster
                        } catch (_: Exception) {
                            null to null
                        }
                    }
                }
                Triple(key, localizedTitle, posterPath)
            }
        }.awaitAll()
    }
    val titles = buildMap {
        entries.forEach { (key, title, _) -> if (title != null) put(key, title) }
    }
    val posters = buildMap {
        entries.forEach { (key, _, poster) -> if (poster != null) put(key, poster) }
    }
    return ResolvedMediaTagMetadata(titles = titles, posters = posters)
}

fun serializeMediaTagsForSend(
    text: String,
    selectedTags: List<TaggedMediaRef>,
    maxLength: Int = 8000
): String {
    if (selectedTags.isEmpty() || !text.contains('#')) return text
    val sortedTags = selectedTags.distinctBy { it.title }.sortedByDescending { it.title.length }
    var result = text
    for (tag in sortedTags) {
        val cleanTitle = tag.title.trim()
        if (cleanTitle.isEmpty()) continue
        val token = "#$cleanTitle"
        val safeLabel = cleanTitle.replace("[", "(").replace("]", ")")
        val replacement = "[#$safeLabel](${buildTmdbMediaTagUrl(tag.mediaType, tag.id)})"
        val delta = replacement.length - token.length

        val sb = StringBuilder(result.length + 64)
        var searchFrom = 0
        var projectedLen = result.length
        while (searchFrom < result.length) {
            val idx = result.indexOf(token, searchFrom)
            if (idx == -1) {
                sb.append(result, searchFrom, result.length)
                break
            }
            val end = idx + token.length
            val validStart = idx == 0 || result[idx - 1].isWhitespace() || result[idx - 1] in "(\"{'"
            val validEnd = end == result.length || result[end].isWhitespace() || result[end] in ".,;:!?)\"'"
            if (validStart && validEnd && projectedLen + delta <= maxLength) {
                sb.append(result, searchFrom, idx)
                sb.append(replacement)
                projectedLen += delta
                searchFrom = end
            } else {
                sb.append(result, searchFrom, idx + 1)
                searchFrom = idx + 1
            }
        }
        result = sb.toString()
    }
    return result
}

fun stripTmdbMediaLinkForPreview(text: String): String {
    if (!text.contains("themoviedb.org", ignoreCase = true)) return text
    return TMDB_MEDIA_LINK_REGEX.replace(text) { it.groupValues[1] }
}

fun String.parseMarkdown(): AnnotatedString {
    // Gestione semplice delle liste: sostituisce "- " o "* " all'inizio della riga con un bullet "• "
    val normalizedText = this.replace(Regex("(?m)^[\\-\\*]\\s+"), "• ")
    
    return buildAnnotatedString {
        var currentIndex = 0
        // Regex per liste numerate (es. "1. "), **grassetto**, *corsivo*, _corsivo_, e citazioni (es. "> ")
        val regex = Regex("(?m)^(\\d+\\.)\\s+|\\*\\*(.*?)\\*\\*|\\*(.*?)\\*|_(.*?)_|(?m)^>\\s+(.*)")
        val matches = regex.findAll(normalizedText)

        for (match in matches) {
            // Aggiungi il testo prima del match
            append(normalizedText.substring(currentIndex, match.range.first))

            // Identifica quale gruppo ha matchato
            when {
                match.groups[1] != null -> { // Liste numerate "1. "
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(match.groupValues[1])
                    pop()
                    append(" ")
                }
                match.groups[2] != null -> { // **grassetto**
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(match.groupValues[2])
                    pop()
                }
                match.groups[3] != null -> { // *corsivo*
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(match.groupValues[3])
                    pop()
                }
                match.groups[4] != null -> { // _corsivo_
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(match.groupValues[4])
                    pop()
                }
                match.groups[5] != null -> { // blockquote "> "
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = androidx.compose.ui.graphics.Color.Gray))
                    append("» ")
                    append(match.groupValues[5])
                    pop()
                }
            }
            currentIndex = match.range.last + 1
        }

        // Aggiungi l'ultimo pezzo di testo
        if (currentIndex < normalizedText.length) {
            append(normalizedText.substring(currentIndex))
        }
    }
}

data class InlineMediaTagToken(
    val placeholderId: String,
    val mediaType: String,
    val mediaId: Long,
    val displayText: String,
    val fullUrl: String
)

data class ParsedMarkdownContent(
    val text: AnnotatedString,
    val mediaTags: List<InlineMediaTagToken>
)

fun parseSimpleMarkdown(
    text: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onLinkClick: ((String) -> Unit)? = null,
    localizedMediaTitles: Map<String, String> = emptyMap(),
    onMediaTagClick: ((mediaType: String, mediaId: Long) -> Unit)? = null
): AnnotatedString = buildParsedMarkdown(
    text = text,
    accentColor = accentColor,
    onLinkClick = onLinkClick,
    localizedMediaTitles = localizedMediaTitles,
    onMediaTagClick = onMediaTagClick,
    emitInlineMediaTags = false
).text

fun parseSimpleMarkdownWithMediaTags(
    text: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onLinkClick: ((String) -> Unit)? = null,
    localizedMediaTitles: Map<String, String> = emptyMap(),
    onMediaTagClick: ((mediaType: String, mediaId: Long) -> Unit)? = null
): ParsedMarkdownContent = buildParsedMarkdown(
    text = text,
    accentColor = accentColor,
    onLinkClick = onLinkClick,
    localizedMediaTitles = localizedMediaTitles,
    onMediaTagClick = onMediaTagClick,
    emitInlineMediaTags = true
)

private fun buildParsedMarkdown(
    text: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onLinkClick: ((String) -> Unit)?,
    localizedMediaTitles: Map<String, String>,
    onMediaTagClick: ((mediaType: String, mediaId: Long) -> Unit)?,
    emitInlineMediaTags: Boolean
): ParsedMarkdownContent {
    val mediaTagTokens = mutableListOf<InlineMediaTagToken>()
    val annotated = buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var currentLine = line
            var isQuote = false
            var isList = false
            var numberedPrefix: String? = null
            
            if (currentLine.startsWith("> ")) {
                isQuote = true
                currentLine = currentLine.substring(2)
            } else if (currentLine.startsWith("- ") || currentLine.startsWith("* ")) {
                isList = true
                currentLine = currentLine.substring(2)
            } else {
                val numMatch = NUMBERED_LIST_PREFIX_REGEX.matchEntire(currentLine)
                if (numMatch != null) {
                    numberedPrefix = numMatch.groupValues[1]
                    currentLine = numMatch.groupValues[2]
                }
            }
            
            withStyle(
                style = SpanStyle(
                    color = if (isQuote) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f) else androidx.compose.ui.graphics.Color.Unspecified,
                    fontStyle = if (isQuote) FontStyle.Italic else null
                )
            ) {
                if (isList) {
                    append("• ")
                } else if (numberedPrefix != null) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(numberedPrefix)
                    }
                    append(" ")
                }
                
                var i = 0
                while (i < currentLine.length) {
                    // 1. Markdown Links: [label](url) & #film/serie TMDB tags
                    if (currentLine[i] == '[') {
                        val closeBracket = currentLine.indexOf(']', i + 1)
                        if (closeBracket != -1 && currentLine.startsWith("(", closeBracket + 1)) {
                            val closeParen = currentLine.indexOf(')', closeBracket + 2)
                            if (closeParen != -1) {
                                val label = currentLine.substring(i + 1, closeBracket)
                                val linkUrl = currentLine.substring(closeBracket + 2, closeParen).trim()
                                val fullUrl = if (linkUrl.startsWith("http://", ignoreCase = true) || linkUrl.startsWith("https://", ignoreCase = true)) {
                                    linkUrl
                                } else {
                                    "https://$linkUrl"
                                }
                                val tmdbTarget = parseTmdbMediaUrl(fullUrl)
                                if (tmdbTarget != null && label.startsWith("#")) {
                                    val (mType, mId) = tmdbTarget
                                    val key = "$mType:$mId"
                                    val displayLabel = localizedMediaTitles[key]?.takeIf { it.isNotBlank() }?.let { "#$it" } ?: label
                                    val tagStyle = SpanStyle(
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val tagPressedStyle = SpanStyle(
                                        color = accentColor.copy(alpha = 0.62f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    val clickHandler: (() -> Unit)? = when {
                                        onMediaTagClick != null -> ({ onMediaTagClick(mType, mId) })
                                        onLinkClick != null -> ({ onLinkClick(fullUrl) })
                                        else -> null
                                    }
                                    if (emitInlineMediaTags && clickHandler != null) {
                                        val placeholderId = "tmdb_tag_${mediaTagTokens.size}_${mType}_$mId"
                                        mediaTagTokens.add(
                                            InlineMediaTagToken(
                                                placeholderId = placeholderId,
                                                mediaType = mType,
                                                mediaId = mId,
                                                displayText = displayLabel,
                                                fullUrl = fullUrl
                                            )
                                        )
                                        appendInlineContent(id = placeholderId, alternateText = displayLabel)
                                    } else if (clickHandler != null) {
                                        pushLink(
                                            LinkAnnotation.Clickable(
                                                tag = "tmdb:$key",
                                                styles = TextLinkStyles(style = tagStyle, pressedStyle = tagPressedStyle),
                                                linkInteractionListener = { clickHandler() }
                                            )
                                        )
                                        append(displayLabel)
                                        pop()
                                    } else {
                                        withStyle(tagStyle) {
                                            append(displayLabel)
                                        }
                                    }
                                } else if (tmdbTarget != null && onMediaTagClick != null) {
                                    val (mType, mId) = tmdbTarget
                                    pushLink(
                                        LinkAnnotation.Clickable(
                                            tag = fullUrl,
                                            styles = TextLinkStyles(
                                                style = SpanStyle(
                                                    color = accentColor,
                                                    textDecoration = TextDecoration.Underline,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                pressedStyle = SpanStyle(
                                                    color = accentColor.copy(alpha = 0.62f),
                                                    textDecoration = TextDecoration.Underline,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            ),
                                            linkInteractionListener = { onMediaTagClick(mType, mId) }
                                        )
                                    )
                                    append(label)
                                    pop()
                                } else if (onLinkClick != null) {
                                    pushLink(
                                        LinkAnnotation.Clickable(
                                            tag = fullUrl,
                                            styles = TextLinkStyles(
                                                style = SpanStyle(
                                                    color = accentColor,
                                                    textDecoration = TextDecoration.Underline,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                pressedStyle = SpanStyle(
                                                    color = accentColor.copy(alpha = 0.62f),
                                                    textDecoration = TextDecoration.Underline,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            ),
                                            linkInteractionListener = { onLinkClick(fullUrl) }
                                        )
                                    )
                                    append(label)
                                    pop()
                                } else {
                                    withStyle(SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold)) {
                                        append(label)
                                    }
                                }
                                i = closeParen + 1
                                continue
                            }
                        }
                    }

                    // 2. Raw URLs (https://, http://, www.)
                    val isUrlStart = currentLine.startsWith("https://", i, ignoreCase = true) ||
                                     currentLine.startsWith("http://", i, ignoreCase = true) ||
                                     (currentLine.startsWith("www.", i, ignoreCase = true) && (i == 0 || currentLine[i - 1].isWhitespace() || currentLine[i - 1] in "([<{\"':;"))
                    if (isUrlStart) {
                        val remaining = currentLine.substring(i)
                        val match = RAW_URL_REGEX.find(remaining)
                        if (match != null && match.range.first == 0) {
                            var rawUrl = match.value
                            while (rawUrl.isNotEmpty() && rawUrl.last() in listOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '\'', '"', '>')) {
                                rawUrl = rawUrl.dropLast(1)
                            }
                            if (rawUrl.isNotEmpty()) {
                                val fullUrl = if (rawUrl.startsWith("http://", ignoreCase = true) || rawUrl.startsWith("https://", ignoreCase = true)) {
                                    rawUrl
                                } else {
                                    "https://$rawUrl"
                                }
                                if (onLinkClick != null) {
                                    pushLink(
                                        LinkAnnotation.Clickable(
                                            tag = fullUrl,
                                            styles = TextLinkStyles(
                                                style = SpanStyle(
                                                    color = accentColor,
                                                    textDecoration = TextDecoration.Underline,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                pressedStyle = SpanStyle(
                                                    color = accentColor.copy(alpha = 0.62f),
                                                    textDecoration = TextDecoration.Underline,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            ),
                                            linkInteractionListener = { onLinkClick(fullUrl) }
                                        )
                                    )
                                    append(rawUrl)
                                    pop()
                                } else {
                                    withStyle(SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium)) {
                                        append(rawUrl)
                                    }
                                }
                                i += rawUrl.length
                                continue
                            }
                        }
                    }

                    // 3. Bold: **text** or __text__
                    if (currentLine.startsWith("**", i)) {
                        val end = currentLine.indexOf("**", i + 2)
                        if (end != -1) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(currentLine.substring(i + 2, end))
                            }
                            i = end + 2
                            continue
                        }
                    }
                    if (currentLine.startsWith("__", i)) {
                        val end = currentLine.indexOf("__", i + 2)
                        if (end != -1) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(currentLine.substring(i + 2, end))
                            }
                            i = end + 2
                            continue
                        }
                    }

                    // 4. Italic: *text* or _text_
                    if (currentLine.startsWith("*", i) && !currentLine.startsWith("**", i)) {
                        val end = currentLine.indexOf("*", i + 1)
                        if (end != -1) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(currentLine.substring(i + 1, end))
                            }
                            i = end + 1
                            continue
                        }
                    }
                    if (currentLine.startsWith("_", i) && !currentLine.startsWith("__", i)) {
                        val end = currentLine.indexOf("_", i + 1)
                        if (end != -1) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(currentLine.substring(i + 1, end))
                            }
                            i = end + 1
                            continue
                        }
                    }

                    // 5. Strikethrough: ~~text~~
                    if (currentLine.startsWith("~~", i)) {
                        val end = currentLine.indexOf("~~", i + 2)
                        if (end != -1) {
                            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                append(currentLine.substring(i + 2, end))
                            }
                            i = end + 2
                            continue
                        }
                    }

                    // 6. Inline code: `text`
                    if (currentLine.startsWith("`", i)) {
                        val end = currentLine.indexOf("`", i + 1)
                        if (end != -1) {
                            withStyle(SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, background = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f))) {
                                append(currentLine.substring(i + 1, end))
                            }
                            i = end + 1
                            continue
                        }
                    }

                    append(currentLine[i])
                    i++
                }
            }
            if (index < lines.size - 1) append("\n")
        }
    }
    return ParsedMarkdownContent(annotated, mediaTagTokens)
}

class MarkdownVisualTransformation(
    private val highlightedMediaTags: Set<String> = emptySet(),
    private val accentColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF2DD4BF)
) : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val originalText = text.text
        val symbolColor = androidx.compose.ui.graphics.Color(0xFF666666) // Grigio scuro per i simboli markdown
        
        val annotatedString = buildAnnotatedString {
            append(originalText)
            
            // Bold (** and __)
            Regex("\\*\\*(.*?)\\*\\*").findAll(originalText).forEach { match ->
                addStyle(SpanStyle(color = symbolColor), match.range.first, match.range.first + 2)
                addStyle(SpanStyle(color = symbolColor), match.range.last - 1, match.range.last + 1)
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first + 2, match.range.last - 1)
            }
            Regex("__(.*?)__").findAll(originalText).forEach { match ->
                addStyle(SpanStyle(color = symbolColor), match.range.first, match.range.first + 2)
                addStyle(SpanStyle(color = symbolColor), match.range.last - 1, match.range.last + 1)
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first + 2, match.range.last - 1)
            }
            
            // Italic (* and _)
            Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)").findAll(originalText).forEach { match ->
                addStyle(SpanStyle(color = symbolColor), match.range.first, match.range.first + 1)
                addStyle(SpanStyle(color = symbolColor), match.range.last, match.range.last + 1)
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first + 1, match.range.last)
            }
            Regex("(?<!_)_(?!_)(.*?)(?<!_)_(?!_)").findAll(originalText).forEach { match ->
                addStyle(SpanStyle(color = symbolColor), match.range.first, match.range.first + 1)
                addStyle(SpanStyle(color = symbolColor), match.range.last, match.range.last + 1)
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first + 1, match.range.last)
            }
            
            // Strikethrough
            Regex("~~(.*?)~~").findAll(originalText).forEach { match ->
                addStyle(SpanStyle(color = symbolColor), match.range.first, match.range.first + 2)
                addStyle(SpanStyle(color = symbolColor), match.range.last - 1, match.range.last + 1)
                addStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough), match.range.first + 2, match.range.last - 1)
            }
            
            // Quotes & Lists (line by line)
            val lines = originalText.split("\n")
            var currentIndex = 0
            lines.forEach { line ->
                if (line.startsWith("> ")) {
                    addStyle(SpanStyle(color = symbolColor), currentIndex, currentIndex + 2)
                    addStyle(SpanStyle(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f), fontStyle = FontStyle.Italic), currentIndex + 2, currentIndex + line.length)
                } else if (line.startsWith("- ")) {
                    addStyle(SpanStyle(color = symbolColor), currentIndex, currentIndex + 2)
                } else if (line.matches(Regex("^\\d+\\.\\s.*"))) {
                    val dotIndex = line.indexOf(". ") + 2
                    addStyle(SpanStyle(color = symbolColor), currentIndex, currentIndex + dotIndex)
                }
                currentIndex += line.length + 1 // +1 for the newline
            }

            // Highlight confirmed #Movie / #TV tags (never plain hashtags like #masterpiece)
            if (highlightedMediaTags.isNotEmpty() && originalText.contains('#')) {
                val coveredRanges = mutableListOf<IntRange>()
                val sortedTags = highlightedMediaTags.sortedByDescending { it.length }
                for (tag in sortedTags) {
                    if (tag.length < 2) continue
                    var searchFrom = 0
                    while (searchFrom < originalText.length) {
                        val idx = originalText.indexOf(tag, searchFrom)
                        if (idx == -1) break
                        val end = idx + tag.length
                        val validStart = idx == 0 || originalText[idx - 1].isWhitespace() || originalText[idx - 1] in "(\"{'"
                        val validEnd = end == originalText.length || originalText[end].isWhitespace() || originalText[end] in ".,;:!?)\"'"
                        val overlaps = coveredRanges.any { idx < it.last && end > it.first }
                        if (validStart && validEnd && !overlaps) {
                            addStyle(
                                SpanStyle(color = accentColor, fontWeight = FontWeight.Bold),
                                idx,
                                end
                            )
                            coveredRanges.add(idx until end)
                            searchFrom = end
                        } else {
                            searchFrom = idx + 1
                        }
                    }
                }
            }
        }
        return androidx.compose.ui.text.input.TransformedText(annotatedString, androidx.compose.ui.text.input.OffsetMapping.Identity)
    }
}

fun parseMarkdownSpans(text: String, accentColor: androidx.compose.ui.graphics.Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = accentColor, fontWeight = FontWeight.SemiBold)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}


