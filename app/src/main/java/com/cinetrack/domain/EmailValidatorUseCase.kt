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
        "mortacci"
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
    }

    fun containsOffensiveWords(text: String): Boolean {
        val normalizedText = normalizeText(text)
        return offensiveWords.any { normalizedText.contains(it) }
    }
}
