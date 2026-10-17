package com.cinetrack.ui.components.badge

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cinetrack.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Modello di un trofeo nella vetrina della Trophy Room.
 */
data class TrophyRoomItemUi(
    val badge: PreviewBadgeItem,
    val currentTier: PreviewBadgeTier,
    val isUnlocked: Boolean,
    val unlockedDate: String? = null
)

/**
 * Criteri di ordinamento per la Sala dei Trofei.
 */
enum class TrophySortOption(@StringRes val titleRes: Int) {
    TIER(R.string.trophy_sort_tier),
    PROGRESS(R.string.trophy_sort_progress),
    RARITY(R.string.trophy_sort_rarity),
    RECENT(R.string.trophy_sort_recent),
    ALPHABETICAL(R.string.trophy_sort_alphabetical);

    @Composable
    fun localizedDisplayName(): String = stringResource(titleRes)
}

/**
 * Categorie di filtro per la Sala dei Trofei.
 */
enum class TrophyFilterCategory(val id: String, @StringRes val titleRes: Int) {
    ALL("ALL", R.string.trophy_cat_all),
    PROGRESSIVE("PROGRESSIVE", R.string.trophy_cat_progressive),
    GENRES_ERAS("GENRES_ERAS", R.string.trophy_cat_genres_eras),
    HONORS("HONORS", R.string.trophy_cat_honors),
    LOST_REEL("LOST_REEL", R.string.trophy_cat_lost_reel)
}

/**
 * Stato di sblocco per i filtri.
 */
enum class TrophyStatusFilter(val id: String, @StringRes val titleRes: Int) {
    ALL("ALL", R.string.trophy_status_all),
    UNLOCKED("UNLOCKED", R.string.trophy_status_unlocked),
    LOCKED("LOCKED", R.string.trophy_status_locked)
}

/**
 * Stato filtri nativo della Trophy Room di FlickTrove.
 */
data class TrophyFilterConfig(
    val sortBy: TrophySortOption = TrophySortOption.TIER,
    val sortDirection: String = "desc",
    val categories: Set<TrophyFilterCategory> = emptySet(),
    val status: TrophyStatusFilter = TrophyStatusFilter.ALL,
    val tiers: Set<PreviewBadgeTier> = emptySet()
) {
    val category: TrophyFilterCategory get() = categories.firstOrNull() ?: TrophyFilterCategory.ALL
    val tier: PreviewBadgeTier? get() = tiers.firstOrNull()

    val hasActiveFilters: Boolean
        get() = sortBy != TrophySortOption.TIER || sortDirection != "desc" || categories.isNotEmpty() || status != TrophyStatusFilter.ALL || tiers.isNotEmpty()
}

fun formatLocalizedTrophyDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    return try {
        val parsed = if (dateStr.contains("-")) {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(dateStr)
        } else {
            try {
                java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ITALIAN).parse(dateStr)
            } catch (e: Exception) {
                java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US).parse(dateStr)
            }
        }
        if (parsed != null) {
            java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, java.util.Locale.getDefault()).format(parsed)
        } else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

internal fun getTierRank(tier: PreviewBadgeTier): Int = when (tier) {
    PreviewBadgeTier.LOST_REEL -> 7
    PreviewBadgeTier.THE_FINAL_CUT -> 6
    PreviewBadgeTier.MM_70 -> 5
    PreviewBadgeTier.MM_35 -> 4
    PreviewBadgeTier.MM_16 -> 3
    PreviewBadgeTier.SUPER_8 -> 2
}

internal fun TrophyRoomItemUi.completionFraction(): Float {
    if (!isUnlocked) return 0f
    val isMastered = currentTier == PreviewBadgeTier.THE_FINAL_CUT ||
        (badge.fixedTier != null && isUnlocked)
    val nextTier = when (currentTier) {
        PreviewBadgeTier.SUPER_8 -> PreviewBadgeTier.MM_16
        PreviewBadgeTier.MM_16 -> PreviewBadgeTier.MM_35
        PreviewBadgeTier.MM_35 -> PreviewBadgeTier.MM_70
        PreviewBadgeTier.MM_70 -> PreviewBadgeTier.THE_FINAL_CUT
        else -> null
    }
    val targetThreshold = if (isUnlocked && nextTier != null) {
        badge.progressiveSteps[nextTier]?.targetThreshold ?: badge.progressTarget
    } else {
        badge.progressiveSteps[currentTier]?.targetThreshold ?: badge.progressTarget
    }
    return when {
        !isUnlocked -> 0f
        isMastered -> 1f
        targetThreshold > 0 -> (badge.progressCurrent.toFloat() / targetThreshold).coerceIn(0f, 1f)
        else -> 1f
    }
}

/**
 * Catalogo ufficiale e veritiero dei 43 trofei di FlickTrove.
 * Ogni elemento parte nello stato originario da conquistare (progressCurrent = 0, isUnlocked = false).
 * Lo stato effettivo dell'utente viene sincronizzato in tempo reale da Room (BadgeRepository).
 */
val OFFICIAL_TROPHY_ROOM_CATALOG = listOf(
    // ══════════════════════════════════════════════════════════════════
    // I TROFEI SEGRETI (LOST REEL 🗝️) — IN CIMA ALLA SALA DEI TROFEI
    // ══════════════════════════════════════════════════════════════════

    // 1. Lost Reel: Il Giorno della Marmotta
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_groundhog_day",
            title = "Il Giorno della Marmotta",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.GROUNDHOG_LOOP,
            categoryLabel = "LOST REEL #02",
            defaultDescription = "Metti la sveglia alle 6:00, Sonny e Cher stanno suonando... Hai guardato due volte lo stesso film in 48 ore.",
            secretHint = "Metti la sveglia alle 6:00, Sonny e Cher stanno suonando...",
            progressCurrent = 0,
            progressTarget = 2,
            progressUnit = "",
            rarityPercent = "0.3%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 2. Lost Reel: Roulette del Fato
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_surprise_fate",
            title = "Roulette del Fato",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.ROULETTE_FATE,
            categoryLabel = "LOST REEL #05",
            defaultDescription = "Affidati al caso per la tua prossima avventura: hai visto per intero un film scoperto tramite Surprise Me.",
            secretHint = "Affidati al caso per la tua prossima avventura cinematografica...",
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "0.7%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 3. Lost Reel: Creatura della Notte
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_night_owl",
            title = "Creatura della Notte",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.NIGHT_OWL_MOON,
            categoryLabel = "LOST REEL #04",
            defaultDescription = "Certe storie prendono vita solo quando la città dorme: hai registrato una visione tra le 02:00 e le 05:00 del mattino.",
            secretHint = "Certe storie prendono vita solo quando la città dorme...",
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "1.4%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 4. Lost Reel: Crisi d'Identità
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_genreless_rebel",
            title = "Crisi d'Identità",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.GENRELESS_REBEL,
            categoryLabel = "LOST REEL #03",
            defaultDescription = "Quando il genere sfugge a ogni classificazione: hai visto un'opera senza alcun genere catalogato su TMDB.",
            secretHint = "Quando il genere sfugge a ogni classificazione...",
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "1.9%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 5. Lost Reel: L'Eterno Dubbioso
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_indecisive",
            title = "L'Eterno Dubbioso",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.INDECISIVE_CRITIC,
            categoryLabel = "LOST REEL #06",
            defaultDescription = "Il parere di un vero critico è in costante evoluzione: hai modificato il tuo voto personale almeno 3 volte sullo stesso film.",
            secretHint = "Il parere di un vero critico è in costante evoluzione...",
            progressCurrent = 0,
            progressTarget = 3,
            progressUnit = "modifiche",
            rarityPercent = "2.2%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 6. Lost Reel: Perfezionista Ossessivo
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_completionist",
            title = "Perfezionista Ossessivo",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.COMPLETIONIST_MIND,
            categoryLabel = "LOST REEL #07",
            defaultDescription = "Non lasciare nulla al caso: hai compilato contemporaneamente Voto, Nota Personale e Vibe per un film.",
            secretHint = "Non lasciare nulla al caso: cura ogni singolo dettaglio.",
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "2.8%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 7. Lost Reel: Origini del Mito
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_genesis",
            title = "Origini del Mito",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.GENESIS_ORIGINS,
            categoryLabel = "LOST REEL #08",
            defaultDescription = "Un tuffo alle origini dell'arte muta: hai completato un'opera realizzata prima del 1940.",
            secretHint = "Un tuffo alle origini dell'arte muta...",
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "3.1%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 8. Lost Reel: Cuore d'Acciaio
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_iron_heart",
            title = "Cuore d'Acciaio",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.IRON_HEART,
            categoryLabel = "LOST REEL #09",
            defaultDescription = "Nessun brivido ha potuto scalfirti: hai affrontato 3 film horror completandoli senza stroncature.",
            secretHint = "Nessun brivido ha potuto scalfirti...",
            progressCurrent = 0,
            progressTarget = 3,
            progressUnit = "film",
            rarityPercent = "3.5%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 9. Lost Reel: Gourmet Incompreso
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_unloved_gem",
            title = "Gourmet Incompreso",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.UNLOVED_GEM,
            categoryLabel = "LOST REEL #10",
            defaultDescription = "La bellezza è negli occhi di chi guarda: hai assegnato un voto eccellente (>= 9.0) a un'opera con media TMDB inferiore a 5.8.",
            secretHint = "La bellezza è negli occhi di chi guarda...",
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "4.1%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 10. Lost Reel: Il Critico Controcorrente
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_harsh_critic",
            title = "Il Critico Controcorrente",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.HARSH_CRITIC,
            categoryLabel = "LOST REEL #11",
            defaultDescription = "Nemmeno i mostri sacri sono al sicuro dal tuo occhio critico: hai assegnato un voto impietoso (<= 4.0) a un'opera considerata un capolavoro su TMDB (>= 8.3).",
            secretHint = "Nemmeno i mostri sacri sono al sicuro dal tuo occhio critico...",
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "1.5%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 11. Lost Reel: Amore a Prima Vista
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_instant_watch",
            title = "Amore a Prima Vista",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.INSTANT_WATCH,
            categoryLabel = "LOST REEL #12",
            defaultDescription = "Nessuna esitazione, zero polvere sulla lista: hai visto un'opera entro 24 ore dall'averla aggiunta ai tuoi segnalibri.",
            secretHint = "Cogli l'attimo fuggente: non lasciare che la tua watchlist accumuli polvere...",
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "3.2%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 12. Lost Reel: La Scala del Critico
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_full_rating_spectrum",
            title = "La Scala del Critico",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.FULL_RATING_SPECTRUM,
            categoryLabel = "LOST REEL #13",
            defaultDescription = "Hai padroneggiato l'intera tavolozza del giudizio cinefilo, assegnando almeno una volta ogni voto intero da 1 a 10.",
            secretHint = "Un critico autorevole padroneggia ogni sfumatura di giudizio, dall'abisso al sublime...",
            progressCurrent = 0,
            progressTarget = 10,
            progressUnit = "voti",
            rarityPercent = "2.8%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false,
        unlockedDate = null
    ),

    // ══════════════════════════════════════════════════════════════════
    // I 12 BADGE PROGRESSIVI EVOLUTIVI (1 CARD DINAMICA PER TEMA)
    // ══════════════════════════════════════════════════════════════════

    // 10. Film visti - Frequenza 24fps
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_movies",
            title = "Frequenza 24fps",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.SUPER_8,
            iconKind = CustomBadgeIconKind.CINEMA_REEL,
            categoryLabel = "FILM VISTI",
            defaultDescription = "La passione per il grande schermo attraverso i formati storici della pellicola.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 10, "I primi passi nella sala buia. Hai completato i tuoi primi 10 lungometraggi.", "10 film completati"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 50, "Il circuito del cinema d'autore. Hai raggiunto quota 50 lungometraggi.", "50 film completati"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 200, "La sala buia è la tua seconda casa. Hai completato 200 lungometraggi.", "200 film completati"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 500, "Il grande formato epico. Ben 500 lungometraggi vissuti.", "500 film completati"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 1000, "L'Opera Compiuta. Oltre 1.000 capolavori impressi nella tua cineteca.", "1.000+ film • Maestria Assoluta")
            ),
            progressCurrent = 0,
            progressTarget = 10,
            progressUnit = "film",
            rarityPercent = "0.8%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 11. Serie TV complete - Maratoneta Seriale
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_tv",
            title = "Maratoneta Seriale",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.SUPER_8,
            iconKind = CustomBadgeIconKind.TV_BINGE,
            categoryLabel = "SERIE TV",
            defaultDescription = "Dall'episodio pilota fino ai titoli di coda dell'ultima stagione.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 3, "Hai completato 3 serie TV.", "3 serie"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 10, "10 serie TV interamente completate. Circuito d'autore.", "10 serie"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 25, "25 serie TV viste dalla prima all'ultima puntata.", "25 serie"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 50, "50 serie TV concluse. Una dedizione monumentale.", "50 serie"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 100, "100 serie TV completate al 100%. Maratoneta definitivo.", "100 serie")
            ),
            progressCurrent = 0,
            progressTarget = 3,
            progressUnit = "serie",
            rarityPercent = "14.8%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 12. Episodi Serie in 24h - Maratona Notturna
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_binge_episodes",
            title = "Maratona Notturna",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.BINGE_NIGHT,
            categoryLabel = "BINGE-WATCHING",
            defaultDescription = "Quando una puntata tira l'altra senza sosta.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 4, "4 episodi di una serie divorati in 24 ore.", "4 episodi"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 8, "8 episodi in un solo giorno. Notte insonne.", "8 episodi"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 15, "15 episodi in 24 ore. Binge-watching leggendario.", "15 episodi")
            ),
            progressCurrent = 0,
            progressTarget = 4,
            progressUnit = "episodi",
            rarityPercent = "5.2%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 13. Watchlist - L'Archivio Infinito
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_watchlist_hoarder",
            title = "L'Archivio Infinito",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.ARCHIVE_STACK,
            categoryLabel = "WATCHLIST",
            defaultDescription = "Più di 50 titoli accumulati nella tua watchlist personale.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 50, "50 titoli salvati in archivio.", "50 titoli"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 150, "150 titoli pronti alla visione.", "150 titoli"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 350, "350 titoli in attesa di essere vissuti.", "350 titoli"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 1000, "1.000+ titoli. L'archivio cinematografico supremo.", "1.000+ titoli")
            ),
            progressCurrent = 0,
            progressTarget = 50,
            progressUnit = "titoli",
            rarityPercent = "9.1%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 14. Fedeltà & Longevità - Tessera del Cineclub
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_veteran_club",
            title = "Tessera del Cineclub",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.SUPER_8,
            iconKind = CustomBadgeIconKind.CINECLUB_PASS,
            categoryLabel = "FEDELTÀ",
            defaultDescription = "Premia la tua fedeltà nel tempo: fai parte della cineteca di FlickTrove da 1 mese fino a oltre 3 anni di proiezioni.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 1, "1 mese di cineclub.", "1 mese"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 6, "6 mesi nel cineclub.", "6 mesi"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 12, "1 anno di fedeltà cinematografica.", "1 anno"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 24, "2 anni di passione condivisa.", "2 anni"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 36, "3+ anni di cinema nel cuore.", "3+ anni")
            ),
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "mesi",
            rarityPercent = "14.5%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 15. Ore di Visione - Odissea del Tempo
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_time",
            title = "Odissea del Tempo",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.SUPER_8,
            iconKind = CustomBadgeIconKind.TIME_ODYSSEY,
            categoryLabel = "TEMPO",
            defaultDescription = "Un viaggio monumentale nel tempo cinematografico.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 25, "25 ore di visione cinematografica.", "25 ore"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 100, "100 ore davanti al grande schermo.", "100 ore"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 350, "350 ore di proiezioni accumulate.", "350 ore"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 800, "800 ore trascorse in sala buia.", "800 ore"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 2000, "Oltre 2.000 ore di puro cinema. L'Odissea è compiuta.", "2.000+ ore")
            ),
            progressCurrent = 0,
            progressTarget = 25,
            progressUnit = "ore",
            rarityPercent = "3.1%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 16. Saghe al 100% - Signore delle Saghe
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_sagas",
            title = "Signore delle Saghe",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.SAGA_TRILOGY,
            categoryLabel = "SAGHE",
            defaultDescription = "Hai completato tutti i capitoli di una saga cinematografica.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 1, "1 saga completa.", "1 saga"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 3, "3 saghe complete al 100%.", "3 saghe"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 7, "7 saghe completate.", "7 saghe"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 15, "15 saghe al 100%. Maestro delle saghe.", "15 saghe")
            ),
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "saghe",
            rarityPercent = "11.2%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 17. Visioni Ripetute - Déjà-Vu
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_rewatch",
            title = "Déjà-Vu",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.SUPER_8,
            iconKind = CustomBadgeIconKind.REWATCH_DEJAVU,
            categoryLabel = "REWATCH",
            defaultDescription = "Ritornare sui propri passi: le opere del cuore che meritano di essere riviste.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 1, "Il tuo 1° rewatch registrato.", "1 rewatch"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 5, "5 rewatch completati.", "5 rewatch"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 15, "15 visioni ripetute dei tuoi film preferiti.", "15 rewatch"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 35, "35 rewatch. La memoria cinematografica.", "35 rewatch"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 50, "50 rewatch. Amore eterno per i capolavori.", "50 rewatch")
            ),
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "rewatch",
            rarityPercent = "18.3%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 18. Voti Personali - La Giuria
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_ratings",
            title = "La Giuria",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.SUPER_8,
            iconKind = CustomBadgeIconKind.STAR_RATING,
            categoryLabel = "VALUTAZIONI",
            defaultDescription = "Hai espresso il tuo giudizio critico assegnando voti alle opere viste.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 10, "10 film valutati con voto.", "10 voti"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 50, "50 schede critiche registrate.", "50 voti"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 150, "150 film recensiti con giudizio personale.", "150 voti"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 400, "400 valutazioni ufficiali nella cineteca.", "400 voti"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 1000, "Presidente di Giuria • 1.000 voti assegnati.", "1.000 voti")
            ),
            progressCurrent = 0,
            progressTarget = 10,
            progressUnit = "voti",
            rarityPercent = "16.0%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 19. Emotional Vibes - Spettro Emozionale
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_vibes",
            title = "Spettro Emozionale",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.SUPER_8,
            iconKind = CustomBadgeIconKind.EMOTIONAL_VIBES,
            categoryLabel = "EMOZIONI",
            defaultDescription = "Dal riso al pianto: hai immortalato le tue emozioni cinematografiche con le Vibes.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 5, "5 vibes emotive assegnate.", "5 vibes"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 25, "25 vibes registrate.", "25 vibes"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 75, "75 reazioni emotive catalogate.", "75 vibes"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 150, "150 sfumature emotive vissute in sala.", "150 vibes"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 300, "300 vibes. Maestro delle emozioni cinematografiche.", "300 vibes")
            ),
            progressCurrent = 0,
            progressTarget = 5,
            progressUnit = "vibes",
            rarityPercent = "12.4%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 20. Commenti Community - Voce della Critica
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_comments",
            title = "Voce della Critica",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.SUPER_8,
            iconKind = CustomBadgeIconKind.CRITIC_VOICE,
            categoryLabel = "COMMUNITY",
            defaultDescription = "Hai condiviso le tue impressioni e riflessioni con la community di cinefili.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 3, "3 commenti condivisi.", "3 commenti"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 15, "15 riflessioni pubblicate.", "15 commenti"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 40, "40 contributi critici nella community.", "40 commenti"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 80, "80 commenti. Critico autorevole di FlickTrove.", "80 commenti")
            ),
            progressCurrent = 0,
            progressTarget = 3,
            progressUnit = "commenti",
            rarityPercent = "19.5%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 21. Cartelle Create - Archivista d'Autore
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_folders",
            title = "Archivista d'Autore",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.SUPER_8,
            iconKind = CustomBadgeIconKind.AUTHOR_FOLDERS,
            categoryLabel = "ORGANIZZAZIONE",
            defaultDescription = "Hai curato e organizzato la tua cineteca personale raggruppando film in cartelle tematiche.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 1, "1 cartella tematica creata.", "1 cartella"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 3, "3 cartelle personalizzate nella cineteca.", "3 cartelle"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 7, "7 raccolte d'autore catalogate.", "7 cartelle"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 15, "15 cartelle tematiche. Archivio d'élite.", "15 cartelle")
            ),
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "cartelle",
            rarityPercent = "13.7%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 22. Prime Visioni - Prima Visione Assoluta
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_day_one_premiere",
            title = "Prima Visione Assoluta",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.SUPER_8,
            iconKind = CustomBadgeIconKind.DAY_ONE_PREMIERE,
            categoryLabel = "PRIME VISIONI",
            defaultDescription = "Vivi l'emozione del grande schermo il giorno stesso dell'uscita ufficiale in sala.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 1, "Il debutto in sala: hai vissuto il tuo primo film alla sua data esatta di debutto.", "1 film al debutto"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 5, "Fedele alla prima: 5 film visti il giorno esatto dell'uscita ufficiale.", "5 film al debutto"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 15, "In prima fila: 15 prime visioni vissute alla data di uscita.", "15 film al debutto"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 30, "Spettatore d'assalto: 30 film visti esattamente alla data di debutto.", "30 film al debutto"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 50, "L'Abbonato della Prima: 50 film visti alla data esatta di debutto • Fedeltà Leggendaria.", "50 film al debutto")
            ),
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "film",
            rarityPercent = "2.1%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = false,
        unlockedDate = null
    ),

    // ══════════════════════════════════════════════════════════════════
    // I 7 BADGE ESPLORAZIONE GENERI & EPOCHE (2 LIVELLI: 16MM & 70MM)
    // ══════════════════════════════════════════════════════════════════

    // 22. Horror - Notte delle Ombre
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "genre_horror",
            title = "Notte delle Ombre",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.GENRE_HORROR,
            categoryLabel = "GENERE: HORROR",
            defaultDescription = "Brividi, sussurri e tensione: hai esplorato gli angoli più oscuri del cinema.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 15, "15 film horror completati.", "15 film"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 50, "50 capolavori del brivido vissuti al buio.", "50 film")
            ),
            progressCurrent = 0,
            progressTarget = 15,
            progressUnit = "film",
            rarityPercent = "6.8%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 23. Sci-Fi - Oltre l'Atmosfera
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "genre_scifi",
            title = "Oltre l'Atmosfera",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.GENRE_SCIFI,
            categoryLabel = "GENERE: SCI-FI",
            defaultDescription = "Viaggi interstellari, futuri distopici e intelligenze aliene.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 15, "15 film di fantascienza esplorati.", "15 film"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 50, "50 viaggi nel cosmo e nel futuro.", "50 film")
            ),
            progressCurrent = 0,
            progressTarget = 15,
            progressUnit = "film",
            rarityPercent = "8.4%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 24. Noir/Thriller - Indagine a Mezzanotte
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "genre_thriller",
            title = "Indagine a Mezzanotte",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.GENRE_THRILLER,
            categoryLabel = "GENERE: THRILLER",
            defaultDescription = "Trame intricate, misteri insoluti e colpi di scena mozzafiato.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 15, "15 thriller e gialli completati.", "15 film"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 50, "50 indagini cinematografiche risolte.", "50 film")
            ),
            progressCurrent = 0,
            progressTarget = 15,
            progressUnit = "film",
            rarityPercent = "9.6%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 25. Animazione/Anime - Mondi Disegnati
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "genre_anime",
            title = "Mondi Disegnati",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.GENRE_ANIME,
            categoryLabel = "GENERE: ANIMAZIONE",
            defaultDescription = "L'arte dell'animazione: dai classici dell'infanzia ai capolavori d'autore giapponesi.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 15, "15 capolavori d'animazione.", "15 film"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 50, "50 mondi disegnati vissuti con stupore.", "50 film")
            ),
            progressCurrent = 0,
            progressTarget = 15,
            progressUnit = "film",
            rarityPercent = "7.2%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 26. Documentari - Occhio del Reale
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "genre_doc",
            title = "Occhio del Reale",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.GENRE_DOC,
            categoryLabel = "DOCUMENTARI",
            defaultDescription = "La cinepresa come testimone della storia, della natura e dell'animo umano.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 10, "10 documentari completati.", "10 doc"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 30, "30 testimonianze del reale assimilate.", "30 doc")
            ),
            progressCurrent = 0,
            progressTarget = 10,
            progressUnit = "doc",
            rarityPercent = "10.1%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 27. Pre-1980 - Archeologo della Bobina
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "genre_vintage",
            title = "Archeologo della Bobina",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.GENRE_VINTAGE,
            categoryLabel = "CINEMA D'EPOCA",
            defaultDescription = "Alla scoperta delle pietre miliari realizzate prima del 1980.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 10, "10 opere storiche pre-1980 completate.", "10 film"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 30, "30 classici d'epoca che hanno fatto la storia.", "30 film")
            ),
            progressCurrent = 0,
            progressTarget = 10,
            progressUnit = "film",
            rarityPercent = "11.5%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 28. Nazioni Diverse - Passaporto Globale
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "world_tour",
            title = "Passaporto Globale",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.WORLD_TOUR,
            categoryLabel = "CINEMA MONDIALE",
            defaultDescription = "Un viaggio attorno al mondo attraverso film prodotti in nazioni e culture diverse.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 5, "Opere di 5 nazioni diverse completate.", "5 nazioni"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 20, "20 nazioni esplorate. Cinefilo senza frontiere.", "20 nazioni")
            ),
            progressCurrent = 0,
            progressTarget = 5,
            progressUnit = "nazioni",
            rarityPercent = "4.5%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 29. Commedia - L'Arte della Risata
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "genre_comedy",
            title = "L'Arte della Risata",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.GENRE_COMEDY,
            categoryLabel = "GENERE: COMMEDIA",
            defaultDescription = "Dalla slapstick classica all'umorismo brillante e alla satira più dissacrante.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 20, "20 commedie viste con il sorriso.", "20 film"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 60, "60 commedie e capolavori dell'ironia cinematografica.", "60 film")
            ),
            progressCurrent = 0,
            progressTarget = 20,
            progressUnit = "film",
            rarityPercent = "15.4%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 30. Western - Mezzogiorno di Fuoco
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "genre_western",
            title = "Mezzogiorno di Fuoco",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.GENRE_WESTERN,
            categoryLabel = "GENERE: WESTERN",
            defaultDescription = "Duelli al tramonto, cavalcate polverose e saloon: hai esplorato la grande frontiera del cinema.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 8, "8 western completati nella frontiera.", "8 film"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 25, "25 leggende del selvaggio West vissute al tramonto.", "25 film")
            ),
            progressCurrent = 0,
            progressTarget = 8,
            progressUnit = "film",
            rarityPercent = "5.1%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = false,
        unlockedDate = null
    ),

    // ══════════════════════════════════════════════════════════════════
    // GLI 8 TRAGUARDI SINGOLI DI PRESTIGIO & ONORIFICENZE
    // ══════════════════════════════════════════════════════════════════

    // 29. Day One Pioneer - Pioniere Prima Bobina
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_day_one_pioneer",
            title = "Pioniere Prima Bobina",
            category = BadgeTypeCategory.DAY_ONE_HONOR,
            fixedTier = PreviewBadgeTier.THE_FINAL_CUT,
            iconKind = CustomBadgeIconKind.PIONEER_CROWN,
            categoryLabel = "FONDATORI DAY-ONE",
            defaultDescription = "Onorificenza esclusiva per i fondatori: hai iniziato a usare FlickTrove durante la fase iniziale di lancio dell'applicazione.",
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "1.2%"
        ),
        currentTier = PreviewBadgeTier.THE_FINAL_CUT,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 30. Sostenitore & Mecenate - Il Produttore Esecutivo
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_executive_producer",
            title = "Il Produttore Esecutivo",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.THE_FINAL_CUT,
            iconKind = CustomBadgeIconKind.EXECUTIVE_PRODUCER,
            categoryLabel = "MECENATI & SUPPORTO",
            defaultDescription = "Dietro le quinte c'è sempre chi crede nel progetto: hai supportato direttamente lo sviluppo di FlickTrove con una donazione.",
            secretHint = null,
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "0.1%"
        ),
        currentTier = PreviewBadgeTier.THE_FINAL_CUT,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 31. Import & Cloud Sync - Il Trasloco
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_onboarding_sync",
            title = "Il Trasloco",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.SYNC_REEL,
            categoryLabel = "IMPORT & TRASLOCO",
            defaultDescription = "Nessuna storia va perduta: hai importato o sincronizzato con successo la tua cineteca personale da Trakt, SIMKL o tramite ripristino di un archivio.",
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "6.4%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 32. TMDB > 8.5 - Gourmet del Cinema
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "gourmet_critic",
            title = "Gourmet del Cinema",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_70,
            iconKind = CustomBadgeIconKind.GOURMET_CRITIC,
            categoryLabel = "ALTA CRITICA",
            defaultDescription = "Hai completato 10 capolavori con votazione media globale TMDB superiore a 8.5 stelle.",
            progressCurrent = 0,
            progressTarget = 10,
            progressUnit = "film",
            rarityPercent = "3.8%"
        ),
        currentTier = PreviewBadgeTier.MM_70,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 33. Durata > 3h30m - Titanico
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "colossal",
            title = "Titanico",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_70,
            iconKind = CustomBadgeIconKind.COLOSSAL_MONUMENT,
            categoryLabel = "SFIDA EPICA",
            defaultDescription = "Hai completato un'opera monumentale di durata superiore alle 3 ore e 30 minuti in una sola sessione.",
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "3.6%"
        ),
        currentTier = PreviewBadgeTier.MM_70,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 34. Attore Preferito - Attore Feticcio
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "actor_muse",
            title = "Attore Feticcio",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_35,
            iconKind = CustomBadgeIconKind.ACTOR_MUSE,
            categoryLabel = "PREFERENZE",
            defaultDescription = "Hai scelto lo stesso interprete come tuo attore preferito in almeno 8 film visti diversi.",
            progressCurrent = 0,
            progressTarget = 8,
            progressUnit = "film",
            rarityPercent = "5.9%"
        ),
        currentTier = PreviewBadgeTier.MM_35,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 35. 8 Film Stesso Regista - Regista del Cuore
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "director_heart",
            title = "Regista del Cuore",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_70,
            iconKind = CustomBadgeIconKind.DIRECTOR_CHAIR,
            categoryLabel = "AUTORIALITÀ",
            defaultDescription = "Hai esplorato la visione autoriale del tuo regista preferito completando oltre 8 sue opere.",
            progressCurrent = 0,
            progressTarget = 8,
            progressUnit = "film",
            rarityPercent = "1.8%"
        ),
        currentTier = PreviewBadgeTier.MM_70,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 36. 10 Note Personali - Diario di Bordo
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "personal_diary",
            title = "Diario di Bordo",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_35,
            iconKind = CustomBadgeIconKind.PERSONAL_DIARY,
            categoryLabel = "NOTE PERSONALI",
            defaultDescription = "Hai annotato impressioni e pensieri intimi scrivendo 10 note personali sui film vissuti.",
            progressCurrent = 0,
            progressTarget = 10,
            progressUnit = "note",
            rarityPercent = "7.0%"
        ),
        currentTier = PreviewBadgeTier.MM_35,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 37. Backdrop Personalizzato - Scenografo Personale
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "custom_backdrop",
            title = "Scenografo Personale",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.SUPER_8,
            iconKind = CustomBadgeIconKind.CUSTOM_BACKDROP,
            categoryLabel = "PERSONALIZZAZIONE",
            defaultDescription = "Cura la tua identità visiva al completo (2/2): imposta sia un Avatar che un Backdrop personalizzato nel tuo profilo.",
            progressCurrent = 0,
            progressTarget = 2,
            progressUnit = "elementi",
            rarityPercent = "22.1%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 41. Miniserie TV - Il Romanzo Televisivo
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_miniseries",
            title = "Il Romanzo Televisivo",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_35,
            iconKind = CustomBadgeIconKind.TV_NOVEL,
            categoryLabel = "MINISERIE TV",
            defaultDescription = "Storie intense e autoconclusive: hai completato 5 miniserie da una sola stagione.",
            progressCurrent = 0,
            progressTarget = 5,
            progressUnit = "serie",
            rarityPercent = "11.7%"
        ),
        currentTier = PreviewBadgeTier.MM_35,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 43. Profondità Cartelle - La Collezione Definitiva
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_folder_deep",
            title = "La Collezione Definitiva",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_35,
            iconKind = CustomBadgeIconKind.DEEP_COLLECTION,
            categoryLabel = "RACCOLTE D'AUTORE",
            defaultDescription = "Un archivio ricco e meticoloso: hai creato una cartella tematica con almeno 25 titoli.",
            progressCurrent = 0,
            progressTarget = 25,
            progressUnit = "titoli",
            rarityPercent = "4.3%"
        ),
        currentTier = PreviewBadgeTier.MM_35,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 44. Attraverso i Decenni - La Macchina del Tempo
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_time_machine",
            title = "La Macchina del Tempo",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_70,
            iconKind = CustomBadgeIconKind.TIME_MACHINE,
            categoryLabel = "VIAGGIO NEL TEMPO",
            defaultDescription = "Cultura cinefila senza confini: hai visto almeno un film per ciascuno di 7 decenni diversi.",
            progressCurrent = 0,
            progressTarget = 7,
            progressUnit = "decenni",
            rarityPercent = "7.8%"
        ),
        currentTier = PreviewBadgeTier.MM_70,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 47. Cinema Contemporaneo - Fresco di Stampa
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_current_year",
            title = "Fresco di Stampa",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_35,
            iconKind = CustomBadgeIconKind.CURRENT_YEAR,
            categoryLabel = "CINEMA CONTEMPORANEO",
            defaultDescription = "Vivi il cinema del tuo tempo: hai guardato 5 film distribuiti nelle sale o in streaming nell'anno in corso.",
            progressCurrent = 0,
            progressTarget = 5,
            progressUnit = "film",
            rarityPercent = "14.2%"
        ),
        currentTier = PreviewBadgeTier.MM_35,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 48. Doppio Spettacolo
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_double_feature",
            title = "Doppio Spettacolo",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_35,
            iconKind = CustomBadgeIconKind.DOUBLE_FEATURE,
            categoryLabel = "SESSIONI DI VISIONE",
            defaultDescription = "La magia della sala non finisce mai: hai guardato 2 film diversi nello stesso giorno solare.",
            progressCurrent = 0,
            progressTarget = 2,
            progressUnit = "film",
            rarityPercent = "18.5%"
        ),
        currentTier = PreviewBadgeTier.MM_35,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 49. Gemma Nascosta
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_hidden_gem",
            title = "Gemma Nascosta",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_70,
            iconKind = CustomBadgeIconKind.HIDDEN_GEM,
            categoryLabel = "CINEMA INDIPENDENTE",
            defaultDescription = "Scopritore di tesori cinematografici: hai guardato e premiato (voto >= 7.5 o Preferito) un film di nicchia con meno di 500 voti su TMDB.",
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "gemme",
            rarityPercent = "6.1%"
        ),
        currentTier = PreviewBadgeTier.MM_70,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 50. Pillole di Cinema
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_short_films",
            title = "Pillole di Cinema",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_35,
            iconKind = CustomBadgeIconKind.SHORT_FILMS,
            categoryLabel = "CORTOMETRAGGI",
            defaultDescription = "L'arte della sintesi: hai completato la visione di 5 film o cortometraggi con durata pari o inferiore a 45 minuti.",
            progressCurrent = 0,
            progressTarget = 5,
            progressUnit = "corti",
            rarityPercent = "8.4%"
        ),
        currentTier = PreviewBadgeTier.MM_35,
        isUnlocked = false,
        unlockedDate = null
    ),

    // 51. Meta Trofeo - Il Cacciatore di Trofei
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_trophy_hunter",
            title = "Il Cacciatore di Trofei",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.SUPER_8,
            iconKind = CustomBadgeIconKind.TROPHY_HUNTER,
            categoryLabel = "TRAGUARDI CINEFILI",
            defaultDescription = "La consacrazione definitiva del collezionista: scala le vette della cineteca sbloccando i trofei dell'app.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 10, "Hai sbloccato i tuoi primi 10 trofei.", "10 trofei"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 20, "20 trofei conquistati. La tua bacheca risplende.", "20 trofei"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 35, "35 trofei nella sala d'onore. Un vero maestro del cinema.", "35 trofei"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 50, "50 trofei completati. La Sala dei Trofei è al suo apice leggendario.", "50 trofei")
            ),
            progressCurrent = 0,
            progressTarget = 10,
            progressUnit = "trofei",
            rarityPercent = "0.6%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = false,
        unlockedDate = null
    )
)

