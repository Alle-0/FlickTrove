package com.cinetrack.domain

import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailValidatorUseCase @Inject constructor() {

    /**
     * Termini cinematografici, opere o nomi celebri che contengono sottostringhe
     * ambigue (es. Hitchcock contiene "cock", Figaro contiene "figa", Dickens/Dickinson contiene "dick",
     * MobyDick contiene "dick", Magnifica contiene "fica", Sassuolo/Classics contiene "ass").
     * Vengono protetti dai falsi positivi (Scunthorpe problem).
     */
    private val whitelistedCinemaWords = listOf(
        "hitchcock",
        "peacock",
        "figaro",
        "dickens",
        "philipkdick",
        "mobydick",
        "dickinson",
        "scunthorpe",
        "magnifica",
        "magnifico",
        "pacific",
        "sassuolo",
        "classic",
        "cassette",
        "passion",
        "passione",
        "assassin",
        "assassino"
    )

    /**
     * Termini inequivocabilmente offensivi, osceni, d'odio o illegali.
     * La loro presenza (anche come sottostringa o in leetspeak es. "f1g4", "c4zzo")
     * determina il blocco immediato, purché non facciano parte di un termine in whitelist.
     */
    private val substringOffensiveWords = listOf(
        // === ITALIANO ===
        // Anatomia / Volgarità esplicita
        "cazzo", "cazzi", "cazzon", "cazzat", "stronz", "merda", "merde", "merdos",
        "puttan", "zoccol", "mignott", "bagascia", "baldracca", "bastard", 
        "coglion", "minchia", "minchion", "cornut", "sfigat", "stracciacazz",
        "figa", "figaccia", "chiavat", "inculat",
        // Discriminazione / Omofobia / Abilitismo
        "frocio", "froci", "ricchion", "culatton", "mongoloid", "spastic", 
        // Atti / Espliciti / Composte
        "sborra", "sborro", "sborrat", "pompin", "bocchin", "succhiacazz", "succhiaminchi",
        "vaffancul", "fancul", "rottincul", "leccacul", "rompicazz", "cacacazz", "scassacazz",
        "testadicazz", "faccidimerda", "pezzodimerda", "figliodiputtan", "figlidiputtan",
        // Bestemmie e imprecazioni
        "porcodio", "diocane", "diocan", "porcamadonna", "dioporco", "diomerda",
        "canedio", "diocristo", "cristodio", "madonnaputtana", "madonnacagna", "mortacci",

        // === INGLESE ===
        "fuck", "fucker", "fucking", "motherfucker", "shit", "bitch", "cunt",
        "nigger", "nigga", "faggot", "whore", "slut", "asshole", "blowjob",
        "handjob", "dildo", "retard", "twat", "wanker", "dipshit",

        // === SPAGNOLO ===
        "hijoputa", "hizodeputa", "hijosdeputa", "maricon", "marica", "gilipollas",
        "cabron", "cabrona", "mierda", "mierdas", "culero", "pendejo", "pendeja",
        "chingar", "chingada", "chupapollas", "malparido",

        // === FRANCESE ===
        "connard", "connasse", "salope", "encule", "enculer", "filsdepute",
        "niqueta", "niquetamere", "batard",

        // === TEDESCO ===
        "hurensohn", "arschloch", "fotze", "schlampe", "wichser", "missgeburt", "schweinehund",

        // === PORTOGHESE ===
        "filhadaputa", "filhosdaputa", "arrombado", "caralho", "buceta", "fodasse",
        "punheta", "viado",

        // === TABÙ UNIVERSALI (Hate Speech, Crimini, Pedofilia, Nazismo) ===
        "hitler", "nazist", "neonazi", "swastika", "pedofil", "pedophil",
        "zoofil", "zoophil", "necrofil", "necrophil", "genocid", "rapist"
    )

    /**
     * Termini brevi o ambigui da bloccare SOLO se usati come parola intera, token isolato
     * o accompagnati da numeri/simboli (es. "fica", "la_fica", "fica69", "cock", "bad_ass"),
     * evitando di respingere termini leciti composti (es. "magnifica", "pacific", "classic").
     */
    private val tokenOrExactOffensiveWords = listOf(
        "fica", "fette",
        "cock", "ass", "tit", "tits", "cum",
        "puta", "puto", "pute",
        "dick", "pussy", "verga"
    )

    private fun normalizeText(text: String): String {
        // Decomponi i caratteri accentati (es: é -> e, ó -> o, à -> a)
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")

        return decomposed.lowercase()
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

    private fun matchesTokenOrExact(rawText: String, textWithoutWhitelisted: String): Boolean {
        // 1. Verifica match esatto o parola + numeri sul testo normalizzato (es. "fica", "fica99", "69fica")
        for (bad in tokenOrExactOffensiveWords) {
            val pattern = Regex("^\\d*${Regex.escape(bad)}\\d*$")
            if (pattern.matches(textWithoutWhitelisted)) return true
        }

        // 2. Tokenizza il testo grezzo separando per caratteri non alfanumerici (spazi, trattini, underscore, punti)
        val rawTokens = rawText.lowercase().split(Regex("[^a-z0-9]+"))
        for (token in rawTokens) {
            if (token.isBlank()) continue
            val normToken = normalizeText(token)
            for (bad in tokenOrExactOffensiveWords) {
                val pattern = Regex("^\\d*${Regex.escape(bad)}\\d*$")
                if (pattern.matches(normToken)) return true
            }
        }

        return false
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
        
        // Rimuovi parole whitelisted per neutralizzare i falsi positivi di Scunthorpe
        val textWithoutWhitelisted = whitelistedCinemaWords.fold(normalizedText) { acc, word ->
            acc.replace(word, "")
        }

        // 1. Check su sottostringa per parole inequivocabilmente volgari o vietate
        if (substringOffensiveWords.any { textWithoutWhitelisted.contains(it) }) {
            return true
        }

        // 2. Check su token esatto per parole corte/ambigue
        if (matchesTokenOrExact(text, textWithoutWhitelisted)) {
            return true
        }

        return false
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
