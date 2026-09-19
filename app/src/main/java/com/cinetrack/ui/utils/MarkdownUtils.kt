package com.cinetrack.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

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

fun parseSimpleMarkdown(text: String, accentColor: androidx.compose.ui.graphics.Color): AnnotatedString {
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
                    if (currentLine.startsWith("~~", i)) {
                        val end = currentLine.indexOf("~~", i + 2)
                        if (end != -1) {
                            withStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) {
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


