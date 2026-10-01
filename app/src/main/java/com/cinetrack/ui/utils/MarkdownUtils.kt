package com.cinetrack.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

private val RAW_URL_REGEX = Regex("^(?:https?://|www\\.)[^\\s\\[\\]\\(\\)<>\"']+")

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

fun parseSimpleMarkdown(
    text: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onLinkClick: ((String) -> Unit)? = null
): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var currentLine = line
            var isQuote = false
            var isList = false
            
            if (currentLine.startsWith("> ")) {
                isQuote = true
                currentLine = currentLine.substring(2)
            } else if (currentLine.startsWith("- ")) {
                isList = true
                currentLine = currentLine.substring(2)
            }
            
            withStyle(
                style = SpanStyle(
                    color = if (isQuote) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f) else androidx.compose.ui.graphics.Color.Unspecified,
                    fontStyle = if (isQuote) FontStyle.Italic else null
                )
            ) {
                if (isList) {
                    append("• ")
                }
                
                var i = 0
                while (i < currentLine.length) {
                    // 1. Markdown Links: [label](url)
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
                                if (onLinkClick != null) {
                                    pushLink(
                                        LinkAnnotation.Clickable(
                                            tag = fullUrl,
                                            styles = TextLinkStyles(
                                                style = SpanStyle(
                                                    color = accentColor,
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

                    // 3. Bold: **text**
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

                    // 4. Italic: *text*
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

                    append(currentLine[i])
                    i++
                }
            }
            if (index < lines.size - 1) append("\n")
        }
    }
}

class MarkdownVisualTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val originalText = text.text
        val symbolColor = androidx.compose.ui.graphics.Color(0xFF666666) // Grigio scuro per i simboli markdown
        
        val annotatedString = buildAnnotatedString {
            append(originalText)
            
            // Bold
            Regex("\\*\\*(.*?)\\*\\*").findAll(originalText).forEach { match ->
                addStyle(SpanStyle(color = symbolColor), match.range.first, match.range.first + 2)
                addStyle(SpanStyle(color = symbolColor), match.range.last - 1, match.range.last + 1)
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first + 2, match.range.last - 1)
            }
            
            // Italic
            Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)").findAll(originalText).forEach { match ->
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


