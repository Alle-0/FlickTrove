package com.cinetrack.domain

import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validatore avanzato di livello enterprise per la moderazione di username, profili ed email.
 *
 * Implementa un'architettura multi-livello conforme agli standard Trust & Safety (Meta / Discord / Google):
 *
 * PASS 1: Native Non-Latin Direct Detection
 * Scansiona direttamente parole vietate negli alfabeti nativi (Cirillico russo, Devanagari indiano, CJK cinese)
 * preservando fedelmente i segni vocalici (matra) e i grafemi nativi senza distorsioni di decomposizione.
 *
 * PASS 2: Latin, Transliterated & Adversarial Evasion Pipeline
 * 1. Mappatura omoglifi & leetspeak (intercetta evasioni basate su numeri, simboli e caratteri cirillici simili, es: 'fіgа')
 * 2. Decomposizione Unicode & rimozione diacritici latini (Normalizer NFD, es: 'maricón' -> 'maricon')
 * 3. Compressione delle ripetizioni trolls (es: "fuuuuuck" -> "fuck", "fiiiiga" -> "figa")
 * 4. Protezione del dominio cinematografico (Whitelist Scunthorpe per Hitchcock, Figaro, Dickens, Miranda, ecc.)
 * 5. Lexicon globale multi-lingua (Italiano, Inglese, Spagnolo, Francese, Tedesco, Portoghese, Slavo traslitterato,
 *    Hinglish indiano, Olandese, Turco, Rumeno, Scandinavo, Ungherese, Ceco, Hate Speech & tabù di sicurezza).
 * 6. Differenziazione chirurgica tra violazioni per sottostringa globale e termini ambigui vincolati a token isolati.
 */
@Singleton
class EmailValidatorUseCase @Inject constructor() {

    private val punctuationSet = setOf(
        '.', '_', '-', ' ', '/', '\\', '!', '@', '#', '$', '%', '^', '&', '*',
        '(', ')', '+', '=', '~', '`', '{', '}', '[', ']', ':', ';', '"', '\'',
        '<', '>', ',', '?'
    )

    /**
     * Vocabolario nativo in alfabeti non-latini (Russo cirillico, Hindi Devanagari, Cinese CJK).
     * Rilevato al Pass 1 prima di qualsiasi manipolazione di omoglifi o decomposizione NFD.
     */
    private val nativeNonLatinOffensiveWords = listOf(
        // === RUSSO / SLAVO (Cirillico) ===
        "пизда", "пиздец", "хуй", "хуя", "хуе", "ебать", "ебал", "блядь", "блять",
        "сука", "пидор", "пидорас", "гондон", "мудак", "заебал", "долбоеб",

        // === INDIANO (Hindi in Devanagari) ===
        "मादरचोद", "बहनचोद", "भोसड़ीके", "भोसड़ीके", "चूतिया", "गांडू", "हरामी", "रांडी", "लंड", "चूत",

        // === CINESE (CJK) ===
        "操你妈", "傻逼", "肏"
    )

    private val homoglyphs = mapOf(
        // Cirillico look-alike -> Latino
        'а' to 'a', 'в' to 'b', 'е' to 'e', 'к' to 'k', 'м' to 'm',
        'н' to 'h', 'о' to 'o', 'р' to 'p', 'с' to 'c', 'т' to 't',
        'у' to 'y', 'х' to 'x', 'і' to 'i', 'ї' to 'i', 'й' to 'i',
        // Leetspeak / Numeri & simboli
        '0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '@' to 'a',
        '5' to 's', '$' to 's', '7' to 't', '+' to 't', '8' to 'b',
        '2' to 'z', '!' to 'i'
    )

    private val whitelistedCinemaWords = setOf(
        "hitchcock", "peacock", "figaro", "dickens", "philipkdick", "mobydick",
        "dickinson", "scunthorpe", "magnifica", "magnifico", "pacific", "sassuolo",
        "classic", "cassette", "passion", "passione", "assassin", "assassino",
        "penelope", "tarantino", "arsenal", "canal", "documentary", "accumulate",
        "miranda", "blunder", "parachute"
    )

    private val substringOffensiveWords = listOf(
        // === ITALIANO ===
        "cazzo", "cazzi", "cazzon", "cazzat", "stronz", "merda", "merde", "merdos",
        "puttan", "zoccol", "mignott", "bagascia", "baldracca", "bastard", 
        "coglion", "minchia", "minchion", "cornut", "sfigat", "stracciacazz",
        "figa", "figaccia", "chiavat", "inculat",
        "frocio", "froci", "ricchion", "culatton", "mongoloid", "spastic", 
        "sborra", "sborro", "sborrat", "pompin", "bocchin", "succhiacazz", "succhiaminchi",
        "vaffancul", "fancul", "rottincul", "leccacul", "rompicazz", "cacacazz", "scassacazz",
        "testadicazz", "faccidimerda", "pezzodimerda", "figliodiputtan", "figlidiputtan",
        "porcodio", "diocane", "diocan", "porcamadonna", "dioporco", "diomerda",
        "canedio", "diocristo", "cristodio", "madonnaputtana", "madonnacagna", "mortacci",

        // === INGLESE ===
        "fuck", "fucker", "fucking", "motherfucker", "bitch", "cunt",
        "asshole", "blowjob", "handjob", "dildo", "retard", "twat", "wanker",
        "dipshit", "shithead", "cocksucker",

        // === SPAGNOLO ===
        "hijoputa", "hizodeputa", "hijosdeputa", "maricon", "marica", "gilipollas",
        "cabron", "cabrona", "mierda", "mierdas", "culero", "pendejo", "pendeja",
        "chingar", "chingada", "chupapollas", "malparido", "cojones", "putamadre",

        // === FRANCESE ===
        "connard", "connasse", "salope", "encule", "enculer", "filsdepute",
        "niqueta", "niquetamere", "batard", "pouffiasse", "trouduc",

        // === TEDESCO ===
        "hurensohn", "arschloch", "fotze", "schlampe", "wichser", "missgeburt",
        "schweinehund", "arschgesicht", "verpiss",

        // === PORTOGHESE ===
        "filhadaputa", "filhosdaputa", "arrombado", "caralho", "buceta",
        "fodasse", "punheta", "viado", "cuzao",

        // === RUSSO / SLAVO TRASLITTERATO (Mat in caratteri latini) ===
        "cyka", "suka", "blyat", "bliat", "blyad", "pizda", "pizdec", "nahui", "nahuy",
        "dolbaeb", "mudak", "kurwa", "kurwo", "jebac", "jebany", "spierdalaj", "xyi", "xui",
        "ebat", "yebat", "pidor", "pidoras", "gandon", "zaebal",

        // === INDIANO (Hindi in caratteri latini / Hinglish) ===
        "madarchod", "madarchot", "behenchod", "bhenchod", "benchod", "bhosdike", "bhosadi",
        "chutiya", "chutiye", "gandu", "kaminey", "harami", "haramzada", "chudai", "gandfatike",

        // === CINESE TRASLITTERATO (Pinyin) ===
        "caonima", "shabi", "choushabi",

        // === OLANDESE ===
        "klootzak", "kankerlijer", "kankeraar", "teringlijer",

        // === TURCO ===
        "orospu", "siktir", "amcik", "gotveren", "yarrak", "kahpe",

        // === RUMENO ===
        "muie", "sloboz",

        // === CECO / UNGHERESE / SCANDINAVO ===
        "bazdmeg", "bazmeg", "curak", "kokot", "geci", "fitta", "javlar", "djavlar", "helvete",

        // === TABÙ GLOBALI (Hate Speech, Pedofilia, Terrorismo, Nazismo) ===
        "hitler", "nazist", "neonazi", "swastika", "nigger", "nigga", "faggot",
        "pedofil", "pedophil", "zoofil", "necrofil", "genocid", "rapist",
        "jihadist", "isis", "alqaeda"
    )

    private val tokenOrExactOffensiveWords = setOf(
        "fica", "fette",
        "cock", "ass", "tit", "tits", "cum",
        "dick", "pussy", "verga",
        "puta", "puto", "pute",
        "chuj", "huj",
        "pula",
        "hoer", "kut",
        "fasz",
        // Hindi token isolati (protetti da parole lunghe lecite come Miranda o Parachute)
        "randi", "lund", "chut"
    )

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

    /**
     * Normalizza il testo per il Pass 2 (pipeline latina):
     * converte omoglifi cirillici e simboli leet in caratteri latini,
     * decompone i diacritici Unicode e ripulisce caratteri non alfanumerici.
     */
    private fun normalizeLatinText(text: String): String {
        var lower = text.lowercase()
        for ((char, rep) in homoglyphs) {
            lower = lower.replace(char, rep)
        }

        val decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")

        return decomposed.replace(Regex("[^a-z0-9]"), "")
    }

    private fun matchesTokenOrExact(rawText: String, textWithoutWhitelisted: String): Boolean {
        // 1. Check su stringa normalizzata intera (es: "fica", "fica69", "69fica", "randi")
        for (bad in tokenOrExactOffensiveWords) {
            val pattern = Regex("^\\d*${Regex.escape(bad)}\\d*$")
            if (pattern.matches(textWithoutWhitelisted)) return true
        }

        // 2. Tokenizza per separatori
        val rawTokens = rawText.lowercase().split(Regex("[^a-z0-9]+"))
        for (token in rawTokens) {
            if (token.isBlank()) continue
            val normToken = normalizeLatinText(token)
            for (bad in tokenOrExactOffensiveWords) {
                val pattern = Regex("^\\d*${Regex.escape(bad)}\\d*$")
                if (pattern.matches(normToken)) return true
            }
        }

        return false
    }

    /**
     * Valuta se il testo fornito contiene parole o sequenze offensive.
     */
    fun containsOffensiveWords(text: String): Boolean {
        if (text.length < 3) return false

        // =========================================================================
        // PASS 1: NATIVE NON-LATIN SCRIPT DETECTION (Cirillico, Devanagari, CJK)
        // Rilevamento fedele senza alterare la struttura dei grafemi nativi
        // =========================================================================
        val nativeCleaned = text.lowercase().filterNot { it.isWhitespace() || it in punctuationSet }
        if (nativeNonLatinOffensiveWords.any { nativeCleaned.contains(it) }) {
            return true
        }

        // =========================================================================
        // PASS 2: LATIN, TRANSLITERATED & ADVERSARIAL HOMOGLYPH EVASION PIPELINE
        // =========================================================================
        val normalizedLatin = normalizeLatinText(text)
        if (normalizedLatin.length < 3) return false

        // Rimozione whitelist cinema per evitare il problema di Scunthorpe
        var textWithoutWhitelisted = normalizedLatin
        for (white in whitelistedCinemaWords) {
            textWithoutWhitelisted = textWithoutWhitelisted.replace(white, "")
        }

        // 1. Scansione sottostringhe inequivocabilmente vietate
        if (substringOffensiveWords.any { textWithoutWhitelisted.contains(it) }) {
            return true
        }

        // 2. Evasione ripetizioni prolungate dei troll (es. "fuuuuuck" -> "fuck", "fiiiiga" -> "figa")
        val collapsedRepetitions = textWithoutWhitelisted.replace(Regex("(.)\\1{2,}"), "$1")
        if (substringOffensiveWords.any { collapsedRepetitions.contains(it) }) {
            return true
        }

        // 3. Controllo token/parole esatte o delimitate
        if (matchesTokenOrExact(text, textWithoutWhitelisted)) {
            return true
        }

        return false
    }

    /**
     * Verifica se il testo viola i termini riservati del brand o ruoli ufficiali di sistema.
     */
    fun containsReservedWords(text: String): Boolean {
        val normalized = normalizeLatinText(text)

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
