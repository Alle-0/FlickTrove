package com.cinetrack.domain

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailValidatorUseCase @Inject constructor() {

    private val offensiveWords = listOf(
        // Generiche / Insulti
        "stronz", "cazzo", "cazzi", "cazzon", "cazzat", "merda", "merde", "merdos",
        "puttan", "zoccol", "mignott", "bagascia", "baldracca", "bastard", 
        "coglion", "minchia", "minchion", "cornut", "sfigat", "stracciacazz",
        // Discriminazione / Omofobia
        "frocio", "froci", "ricchion", "culatton", "mongoloid", "spastic", 
        // Atti / Espliciti / Composte
        "sborra", "sborro", "sborrat", "pompin", "bocchin", "succhiacazz", "succhiaminchi",
        "vaffancul", "fancul", "rottincul", "leccacul", "rompicazz", "cacacazz", "scassacazz",
        "testadicazz", "faccidimerda", "pezzodimerda", "figliodiputtan", "figlidiputtan",
        // Bestemmie e imprecazioni
        "porcodio", "diocane", "diocan", "porcamadonna", "dioporco", "diomerda",
        "canedio", "diocristo", "cristodio", "madonnaputtana", "madonnacagna",
        "mortacci",
        // Inglese e altro
        "fuck", "shit", "bitch", "cunt", "nigger", "nigga", "negro", "faggot", "whore", "slut", 
        "asshole", "motherfucker", "dick", "pussy"
    )

    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace("4", "a")
            .replace("@", "a")
            .replace("3", "e")
            .replace("1", "i")
            .replace("!", "i")
            .replace("0", "o")
            .replace("5", "s")
            .replace("$", "s")
            .replace("7", "t")
            .replace("+", "t")
            .replace("8", "b")
            .replace("2", "z")
            .replace(Regex("[^a-z]"), "")
    }

    private val brandWords = listOf(
        "flicktrove",
        "cinetrack",
        "commsuni"
    )

    private val systemRoles = listOf(
        "admin",
        "administrator",
        "moderator",
        "support",
        "staff",
        "official",
        "system"
    )

    fun containsOffensiveWords(text: String): Boolean {
        val normalizedText = normalizeText(text)
        return offensiveWords.any { normalizedText.contains(it) }
    }

    fun containsReservedWords(text: String): Boolean {
        val normalized = normalizeText(text)
        
        // Brand names are strictly forbidden anywhere in the username
        if (brandWords.any { normalized.contains(it) }) {
            return true
        }

        // System roles: forbidden as exact match, prefix, suffix, or bounded token
        val lower = text.lowercase()
        for (role in systemRoles) {
            if (normalized == role) return true
            if (normalized.startsWith(role) || normalized.endsWith(role)) return true
            // Check with separators (e.g. user_admin_99, test.support.it)
            if (lower.contains("_${role}_") || lower.contains(".${role}.") || lower.contains("-${role}-")) return true
        }

        return false
    }
}
