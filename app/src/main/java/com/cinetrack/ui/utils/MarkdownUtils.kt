package com.cinetrack.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

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
