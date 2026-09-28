package com.cinetrack.ui.components.badge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalConfiguration
import com.cinetrack.R
import androidx.compose.ui.res.stringResource
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.dialog.DirectionChip
import com.cinetrack.ui.components.dialog.ExpandableSection
import com.cinetrack.ui.components.dialog.FilterChip
import com.cinetrack.ui.components.dialog.SortOptionItem
import com.cinetrack.ui.components.glass.GlassmorphicModal
import com.cinetrack.ui.components.navigation.GlassyBottomBar
import com.cinetrack.ui.components.navigation.GlassyTopBar
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.components.shared.MorphGlassModal
import com.cinetrack.ui.components.stats.statsCard
import com.cinetrack.ui.theme.FlickTroveTheme
import com.cinetrack.ui.theme.NeonTeal
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.verticalFadingEdges
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

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
enum class TrophySortOption(val displayName: String) {
    TIER("Prestigio tier"),
    PROGRESS("Avanzamento"),
    RARITY("Rarità"),
    RECENT("Più recenti"),
    ALPHABETICAL("Alfabetico (A-Z)")
}

/**
 * Stato filtri nativo della Trophy Room di FlickTrove.
 */
data class TrophyFilterConfig(
    val sortBy: TrophySortOption = TrophySortOption.TIER,
    val sortDirection: String = "desc",
    val categories: Set<String> = emptySet(),
    val status: String = "TUTTI",
    val tiers: Set<PreviewBadgeTier> = emptySet()
) {
    val category: String get() = categories.firstOrNull() ?: "TUTTI"
    val tier: PreviewBadgeTier? get() = tiers.firstOrNull()

    val hasActiveFilters: Boolean
        get() = sortBy != TrophySortOption.TIER || sortDirection != "desc" || categories.isNotEmpty() || status != "TUTTI" || tiers.isNotEmpty()
}

private fun getTierRank(tier: PreviewBadgeTier): Int = when (tier) {
    PreviewBadgeTier.LOST_REEL -> 7
    PreviewBadgeTier.THE_FINAL_CUT -> 6
    PreviewBadgeTier.MM_70 -> 5
    PreviewBadgeTier.MM_35 -> 4
    PreviewBadgeTier.MM_16 -> 3
    PreviewBadgeTier.SUPER_8 -> 2
}

private fun TrophyRoomItemUi.completionFraction(): Float {
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
 * Catalogo dimostrativo fedele al piano ufficiale BADGES_PLAN.md.
 */
val SAMPLE_TROPHY_ROOM_ITEMS = listOf(
    // ══════════════════════════════════════════════════════════════════
    // I 10 TROFEI SEGRETI (LOST REEL 🗝️) — IN CIMA ALLA SALA DEI TROFEI
    // ══════════════════════════════════════════════════════════════════

    // 1. Lost Reel: Il Produttore Esecutivo
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_executive_producer",
            title = "Il Produttore Esecutivo",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.EXECUTIVE_PRODUCER,
            categoryLabel = "LOST REEL #01",
            defaultDescription = "Dietro le quinte c'è sempre chi crede nel progetto: hai supportato direttamente lo sviluppo di FlickTrove.",
            secretHint = "Dietro le quinte c'è sempre chi crede nel progetto...",
            progressCurrent = 1,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "0.1%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = true,
        unlockedDate = "10 Ago 2026"
    ),

    // 2. Lost Reel: Il Giorno della Marmotta
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
            progressCurrent = 2,
            progressTarget = 2,
            progressUnit = "",
            rarityPercent = "0.3%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = true,
        unlockedDate = "02 Feb 2026"
    ),

    // 3. Lost Reel: Roulette del Fato
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
            progressCurrent = 1,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "0.7%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = true,
        unlockedDate = "19 Gen 2026"
    ),

    // 4. Lost Reel: Creatura della Notte
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
            progressCurrent = 1,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "1.4%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = true,
        unlockedDate = "15 Lug 2026"
    ),

    // 5. Lost Reel: Crisi d'Identità (Bloccato)
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
        isUnlocked = false
    ),

    // 6. Lost Reel: L'Eterno Dubbioso (Bloccato)
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
            progressCurrent = 1,
            progressTarget = 3,
            progressUnit = "modifiche",
            rarityPercent = "2.2%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false
    ),

    // 7. Lost Reel: Perfezionista Ossessivo (Bloccato)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_completionist",
            title = "Perfezionista Ossessivo",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.COMPLETIONIST_MIND,
            categoryLabel = "LOST REEL #07",
            defaultDescription = "Non lasciare nulla al caso: hai compilato contemporaneamente Voto, Nota Personale, Vibe e Attore preferito per un film.",
            secretHint = "Non lasciare nulla al caso: cura ogni singolo dettaglio.",
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "2.8%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false
    ),

    // 8. Lost Reel: Origini del Mito (Bloccato)
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
        isUnlocked = false
    ),

    // 9. Lost Reel: Cuore d'Acciaio (Bloccato)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_iron_heart",
            title = "Cuore d'Acciaio",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.IRON_HEART,
            categoryLabel = "LOST REEL #09",
            defaultDescription = "Nessun brivido ha potuto scalfirti: hai affrontato 3 film horror senza assegnare vibes di paura.",
            secretHint = "Nessun brivido ha potuto scalfirti...",
            progressCurrent = 1,
            progressTarget = 3,
            progressUnit = "film",
            rarityPercent = "3.5%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false
    ),

    // 10. Lost Reel: Gourmet Incompreso (Bloccato)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "secret_unloved_gem",
            title = "Gourmet Incompreso",
            category = BadgeTypeCategory.LOST_REEL_SECRET,
            fixedTier = PreviewBadgeTier.LOST_REEL,
            iconKind = CustomBadgeIconKind.UNLOVED_GEM,
            categoryLabel = "LOST REEL #10",
            defaultDescription = "La bellezza è negli occhi di chi guarda: hai assegnato il voto massimo di 10 stelle a un'opera con media TMDB inferiore a 5.0.",
            secretHint = "La bellezza è negli occhi di chi guarda...",
            progressCurrent = 0,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "4.1%"
        ),
        currentTier = PreviewBadgeTier.LOST_REEL,
        isUnlocked = false
    ),

    // ══════════════════════════════════════════════════════════════════
    // I 12 BADGE PROGRESSIVI EVOLUTIVI (1 CARD DINAMICA PER TEMA)
    // ══════════════════════════════════════════════════════════════════

    // 11. Film visti - Frequenza 24fps (The Final Cut, 100%)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_movies",
            title = "Frequenza 24fps",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.THE_FINAL_CUT,
            iconKind = CustomBadgeIconKind.CINEMA_REEL,
            categoryLabel = "FILM VISTI",
            defaultDescription = "La passione per il grande schermo attraverso i formati storici della pellicola.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 10, "I primi passi nella sala buia. Hai completato i tuoi primi 10 lungometraggi.", "10 film completati"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 25, "Il circuito del cinema d'autore. Hai raggiunto quota 25 lungometraggi.", "25 film completati"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 100, "La sala buia è la tua seconda casa. Hai completato 100 lungometraggi.", "100 film completati"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 250, "Il grande formato epico. Ben 250 lungometraggi vissuti.", "250 film completati"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 500, "L'Opera Compiuta. Oltre 500 capolavori impressi nella tua cineteca.", "500+ film • Maestria Assoluta")
            ),
            progressCurrent = 500,
            progressTarget = 500,
            progressUnit = "film",
            rarityPercent = "0.8%"
        ),
        currentTier = PreviewBadgeTier.THE_FINAL_CUT,
        isUnlocked = true,
        unlockedDate = "18 Lug 2026"
    ),

    // 12. Serie TV complete - Maratoneta Seriale (16mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_tv",
            title = "Maratoneta Seriale",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.TV_BINGE,
            categoryLabel = "SERIE TV",
            defaultDescription = "Dall'episodio pilota fino ai titoli di coda dell'ultima stagione.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 3, "Hai completato 3 serie TV.", "3 serie"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 5, "5 serie TV interamente completate. Circuito d'autore.", "5 serie"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 10, "10 serie TV viste dalla prima all'ultima puntata.", "10 serie"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 25, "25 serie TV concluse. Una dedizione monumentale.", "25 serie"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 50, "50 serie TV completate al 100%. Maratoneta definitivo.", "50 serie")
            ),
            progressCurrent = 8,
            progressTarget = 10,
            progressUnit = "serie",
            rarityPercent = "14.8%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = true,
        unlockedDate = "04 Set 2026"
    ),

    // 13. Episodi Serie in 24h - Maratona Notturna (35mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_binge_episodes",
            title = "Maratona Notturna",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_35,
            iconKind = CustomBadgeIconKind.BINGE_NIGHT,
            categoryLabel = "BINGE-WATCHING",
            defaultDescription = "Quando una puntata tira l'altra senza sosta.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 3, "3 episodi di una serie divorati in 24 ore.", "3 episodi"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 5, "5 episodi in un solo giorno. Notte insonne.", "5 episodi"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 10, "10 episodi in 24 ore. Binge-watching leggendario.", "10 episodi")
            ),
            progressCurrent = 5,
            progressTarget = 10,
            progressUnit = "episodi",
            rarityPercent = "5.2%"
        ),
        currentTier = PreviewBadgeTier.MM_35,
        isUnlocked = true,
        unlockedDate = "11 Ago 2026"
    ),

    // 14. Watchlist - L'Archivio Infinito (35mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_watchlist_hoarder",
            title = "L'Archivio Infinito",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_35,
            iconKind = CustomBadgeIconKind.ARCHIVE_STACK,
            categoryLabel = "WATCHLIST",
            defaultDescription = "Più di 100 titoli accumulati nella tua watchlist personale.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 50, "50 titoli salvati in archivio.", "50 titoli"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 100, "100 titoli pronti alla visione.", "100 titoli"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 250, "250 titoli in attesa di essere vissuti.", "250 titoli"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 500, "500+ titoli. L'archivio cinematografico supremo.", "500+ titoli")
            ),
            progressCurrent = 112,
            progressTarget = 250,
            progressUnit = "titoli",
            rarityPercent = "9.1%"
        ),
        currentTier = PreviewBadgeTier.MM_35,
        isUnlocked = true,
        unlockedDate = "15 Ago 2026"
    ),

    // 15. Fedeltà & Longevità - Tessera del Cineclub (16mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_veteran_club",
            title = "Tessera del Cineclub",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.CINECLUB_PASS,
            categoryLabel = "FEDELTÀ",
            defaultDescription = "Sei membro attivo del cineclub da oltre 6 mesi.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 1, "1 mese di cineclub.", "1 mese"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 6, "6 mesi nel cineclub.", "6 mesi"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 12, "1 anno di fedeltà cinematografica.", "1 anno"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 24, "2 anni di passione condivisa.", "2 anni"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 36, "3+ anni di cinema nel cuore.", "3+ anni")
            ),
            progressCurrent = 8,
            progressTarget = 12,
            progressUnit = "mesi",
            rarityPercent = "14.5%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = true,
        unlockedDate = "20 Lug 2026"
    ),

    // 16. Ore di Visione - Odissea del Tempo (70mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_time",
            title = "Odissea del Tempo",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_70,
            iconKind = CustomBadgeIconKind.TIME_ODYSSEY,
            categoryLabel = "TEMPO",
            defaultDescription = "Un viaggio monumentale nel tempo cinematografico.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 10, "10 ore di visione cinematografica.", "10 ore"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 50, "50 ore davanti al grande schermo.", "50 ore"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 100, "100 ore di proiezioni accumulate.", "100 ore"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 250, "250 ore trascorse in sala buia.", "250 ore"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 500, "Oltre 500 ore di puro cinema. L'Odissea è compiuta.", "500+ ore")
            ),
            progressCurrent = 268,
            progressTarget = 500,
            progressUnit = "ore",
            rarityPercent = "3.1%"
        ),
        currentTier = PreviewBadgeTier.MM_70,
        isUnlocked = true,
        unlockedDate = "12 Set 2026"
    ),

    // 17. Saghe al 100% - Signore delle Saghe (Super 8)
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
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 5, "5 saghe completate.", "5 saghe"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 10, "10 saghe al 100%. Maestro delle saghe.", "10 saghe")
            ),
            progressCurrent = 1,
            progressTarget = 3,
            progressUnit = "saghe",
            rarityPercent = "11.2%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = true,
        unlockedDate = "22 Lug 2026"
    ),

    // 18. Visioni Ripetute - Déjà-Vu (Super 8)
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
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 10, "10 visioni ripetute dei tuoi film preferiti.", "10 rewatch"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 25, "25 rewatch. La memoria cinematografica.", "25 rewatch")
            ),
            progressCurrent = 2,
            progressTarget = 5,
            progressUnit = "rewatch",
            rarityPercent = "18.3%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = true,
        unlockedDate = "01 Ago 2026"
    ),

    // 19. Voti Personali - La Giuria (Super 8)
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
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 5, "5 film valutati con voto.", "5 voti"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 25, "25 schede critiche registrate.", "25 voti"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 50, "50 film recensiti con giudizio personale.", "50 voti"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 100, "100 valutazioni ufficiali nella cineteca.", "100 voti"),
                PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 250, "Presidente di Giuria • 250 voti assegnati.", "250 voti")
            ),
            progressCurrent = 18,
            progressTarget = 25,
            progressUnit = "voti",
            rarityPercent = "16.0%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = true,
        unlockedDate = "16 Lug 2026"
    ),

    // 20. Emotional Vibes - Spettro Emozionale (16mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_vibes",
            title = "Spettro Emozionale",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.EMOTIONAL_VIBES,
            categoryLabel = "EMOZIONI",
            defaultDescription = "Dal riso al pianto: hai immortalato le tue emozioni cinematografiche con le Vibes.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 5, "5 vibes emotive assegnate.", "5 vibes"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 20, "20 vibes registrate.", "20 vibes"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 50, "50 reazioni emotive catalogate.", "50 vibes"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 100, "100 sfumature emotive vissute in sala.", "100 vibes")
            ),
            progressCurrent = 24,
            progressTarget = 50,
            progressUnit = "vibes",
            rarityPercent = "12.4%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = true,
        unlockedDate = "25 Ago 2026"
    ),

    // 21. Commenti Community - Voce della Critica (Super 8)
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
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 10, "10 riflessioni pubblicate.", "10 commenti"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 25, "25 contributi critici nella community.", "25 commenti"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 50, "50 commenti. Critico autorevole di FlickTrove.", "50 commenti")
            ),
            progressCurrent = 4,
            progressTarget = 10,
            progressUnit = "commenti",
            rarityPercent = "19.5%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = true,
        unlockedDate = "30 Lug 2026"
    ),

    // 22. Cartelle Create - Archivista d'Autore (16mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_folders",
            title = "Archivista d'Autore",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.AUTHOR_FOLDERS,
            categoryLabel = "ORGANIZZAZIONE",
            defaultDescription = "Hai curato e organizzato la tua cineteca personale raggruppando film in cartelle tematiche.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 1, "1 cartella tematica creata.", "1 cartella"),
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 3, "3 cartelle personalizzate nella cineteca.", "3 cartelle"),
                PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 5, "5 raccolte d'autore catalogate.", "5 cartelle"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 10, "10 cartelle tematiche. Archivio d'élite.", "10 cartelle")
            ),
            progressCurrent = 3,
            progressTarget = 5,
            progressUnit = "cartelle",
            rarityPercent = "13.7%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = true,
        unlockedDate = "18 Ago 2026"
    ),

    // ══════════════════════════════════════════════════════════════════
    // I 7 BADGE ESPLORAZIONE GENERI & EPOCHE (2 LIVELLI: 16MM & 70MM)
    // ══════════════════════════════════════════════════════════════════

    // 23. Horror - Notte delle Ombre (70mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "genre_horror",
            title = "Notte delle Ombre",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_70,
            iconKind = CustomBadgeIconKind.GENRE_HORROR,
            categoryLabel = "GENERE: HORROR",
            defaultDescription = "Brividi, sussurri e tensione: hai esplorato gli angoli più oscuri del cinema.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 10, "10 film horror completati.", "10 film"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 25, "25 capolavori del brivido vissuti al buio.", "25 film")
            ),
            progressCurrent = 28,
            progressTarget = 25,
            progressUnit = "film",
            rarityPercent = "6.8%"
        ),
        currentTier = PreviewBadgeTier.MM_70,
        isUnlocked = true,
        unlockedDate = "23 Set 2026"
    ),

    // 24. Sci-Fi - Oltre l'Atmosfera (16mm)
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
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 10, "10 film di fantascienza esplorati.", "10 film"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 25, "25 viaggi nel cosmo e nel futuro.", "25 film")
            ),
            progressCurrent = 14,
            progressTarget = 25,
            progressUnit = "film",
            rarityPercent = "8.4%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = true,
        unlockedDate = "17 Ago 2026"
    ),

    // 25. Noir/Thriller - Indagine a Mezzanotte (16mm)
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
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 10, "10 thriller e gialli completati.", "10 film"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 25, "25 indagini cinematografiche risolte.", "25 film")
            ),
            progressCurrent = 11,
            progressTarget = 25,
            progressUnit = "film",
            rarityPercent = "9.6%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = true,
        unlockedDate = "05 Set 2026"
    ),

    // 26. Animazione/Anime - Mondi Disegnati (Super 8, Bloccato)
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
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 10, "10 capolavori d'animazione.", "10 film"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 25, "25 mondi disegnati vissuti con stupore.", "25 film")
            ),
            progressCurrent = 7,
            progressTarget = 10,
            progressUnit = "film",
            rarityPercent = "7.2%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = false
    ),

    // 27. Documentari - Occhio del Reale (16mm)
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
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 5, "5 documentari completati.", "5 doc"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 15, "15 testimonianze del reale assimilate.", "15 doc")
            ),
            progressCurrent = 6,
            progressTarget = 15,
            progressUnit = "doc",
            rarityPercent = "10.1%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = true,
        unlockedDate = "28 Lug 2026"
    ),

    // 28. Pre-1980 - Archeologo della Bobina (16mm)
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
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 5, "5 opere storiche pre-1980 completate.", "5 film"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 15, "15 classici d'epoca che hanno fatto la storia.", "15 film")
            ),
            progressCurrent = 8,
            progressTarget = 15,
            progressUnit = "film",
            rarityPercent = "11.5%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = true,
        unlockedDate = "19 Ago 2026"
    ),

    // 29. Nazioni Diverse - Passaporto Globale (70mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "world_tour",
            title = "Passaporto Globale",
            category = BadgeTypeCategory.PROGRESSIVE_TIERS,
            fixedTier = null,
            initialProgressiveTier = PreviewBadgeTier.MM_70,
            iconKind = CustomBadgeIconKind.WORLD_TOUR,
            categoryLabel = "CINEMA MONDIALE",
            defaultDescription = "Un viaggio attorno al mondo attraverso film prodotti in nazioni e culture diverse.",
            progressiveSteps = mapOf(
                PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 5, "Opere di 5 nazioni diverse completate.", "5 nazioni"),
                PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 10, "10 nazioni esplorate. Cinefilo senza frontiere.", "10 nazioni")
            ),
            progressCurrent = 12,
            progressTarget = 10,
            progressUnit = "nazioni",
            rarityPercent = "4.5%"
        ),
        currentTier = PreviewBadgeTier.MM_70,
        isUnlocked = true,
        unlockedDate = "14 Set 2026"
    ),

    // ══════════════════════════════════════════════════════════════════
    // GLI 8 TRAGUARDI SINGOLI DI PRESTIGIO & ONORIFICENZE
    // ══════════════════════════════════════════════════════════════════

    // 30. Day One Pioneer - Pioniere Prima Bobina (The Final Cut)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_day_one_pioneer",
            title = "Pioniere Prima Bobina",
            category = BadgeTypeCategory.DAY_ONE_HONOR,
            fixedTier = PreviewBadgeTier.THE_FINAL_CUT,
            iconKind = CustomBadgeIconKind.PIONEER_CROWN,
            categoryLabel = "FONDATORI DAY-ONE",
            defaultDescription = "Hai calcato la platea di FlickTrove nei primi mesi di vita. Un posto d'onore riservato ai veri fondatori.",
            progressCurrent = 1,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "1.2%"
        ),
        currentTier = PreviewBadgeTier.THE_FINAL_CUT,
        isUnlocked = true,
        unlockedDate = "29 Ago 2026"
    ),

    // 31. Cloud Sync - Il Trasloco (16mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "badge_onboarding_sync",
            title = "Il Trasloco",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_16,
            iconKind = CustomBadgeIconKind.SYNC_REEL,
            categoryLabel = "CINETECA CLOUD",
            defaultDescription = "Hai sincronizzato con successo la tua storia cinematografica importando la cineteca da Trakt o SIMKL.",
            progressCurrent = 1,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "6.4%"
        ),
        currentTier = PreviewBadgeTier.MM_16,
        isUnlocked = true,
        unlockedDate = "14 Lug 2026"
    ),

    // 32. TMDB > 8.5 - Gourmet del Cinema (70mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "gourmet_critic",
            title = "Gourmet del Cinema",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_70,
            iconKind = CustomBadgeIconKind.GOURMET_CRITIC,
            categoryLabel = "ALTA CRITICA",
            defaultDescription = "Hai completato 5 capolavori con votazione media globale TMDB superiore a 8.5 stelle.",
            progressCurrent = 5,
            progressTarget = 5,
            progressUnit = "film",
            rarityPercent = "3.8%"
        ),
        currentTier = PreviewBadgeTier.MM_70,
        isUnlocked = true,
        unlockedDate = "21 Ago 2026"
    ),

    // 33. Durata > 3h30m - Titanico (70mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "colossal",
            title = "Titanico",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_70,
            iconKind = CustomBadgeIconKind.COLOSSAL_MONUMENT,
            categoryLabel = "SFIDA EPICA",
            defaultDescription = "Hai completato un'opera monumentale di durata superiore alle 3 ore e 30 minuti in una sola sessione.",
            progressCurrent = 1,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "3.6%"
        ),
        currentTier = PreviewBadgeTier.MM_70,
        isUnlocked = true,
        unlockedDate = "27 Lug 2026"
    ),

    // 34. Attore Preferito - Attore Feticcio (35mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "actor_muse",
            title = "Attore Feticcio",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_35,
            iconKind = CustomBadgeIconKind.ACTOR_MUSE,
            categoryLabel = "PREFERENZE",
            defaultDescription = "Hai selezionato lo stesso interprete del cuore in 5 check-in dedicati.",
            progressCurrent = 5,
            progressTarget = 5,
            progressUnit = "check-in",
            rarityPercent = "5.9%"
        ),
        currentTier = PreviewBadgeTier.MM_35,
        isUnlocked = true,
        unlockedDate = "09 Set 2026"
    ),

    // 35. 5 Film Stesso Regista - Regista del Cuore (70mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "director_heart",
            title = "Regista del Cuore",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_70,
            iconKind = CustomBadgeIconKind.DIRECTOR_CHAIR,
            categoryLabel = "AUTORIALITÀ",
            defaultDescription = "Hai esplorato la visione autoriale del tuo regista preferito completando oltre 5 sue opere.",
            progressCurrent = 5,
            progressTarget = 5,
            progressUnit = "film",
            rarityPercent = "1.8%"
        ),
        currentTier = PreviewBadgeTier.MM_70,
        isUnlocked = true,
        unlockedDate = "19 Ago 2026"
    ),

    // 36. 5 Note Personali - Diario di Bordo (35mm)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "personal_diary",
            title = "Diario di Bordo",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.MM_35,
            iconKind = CustomBadgeIconKind.PERSONAL_DIARY,
            categoryLabel = "NOTE PERSONALI",
            defaultDescription = "Hai annotato impressioni e pensieri intimi scrivendo 5 note personali sui film vissuti.",
            progressCurrent = 5,
            progressTarget = 5,
            progressUnit = "note",
            rarityPercent = "7.0%"
        ),
        currentTier = PreviewBadgeTier.MM_35,
        isUnlocked = true,
        unlockedDate = "03 Set 2026"
    ),

    // 37. Backdrop Personalizzato - Scenografo Personale (Super 8)
    TrophyRoomItemUi(
        badge = PreviewBadgeItem(
            id = "custom_backdrop",
            title = "Scenografo Personale",
            category = BadgeTypeCategory.SPECIAL_ACHIEVEMENT,
            fixedTier = PreviewBadgeTier.SUPER_8,
            iconKind = CustomBadgeIconKind.CUSTOM_BACKDROP,
            categoryLabel = "PERSONALIZZAZIONE",
            defaultDescription = "Hai personalizzato la tua identità cinefila impostando un backdrop cinematografico nel profilo.",
            progressCurrent = 1,
            progressTarget = 1,
            progressUnit = "",
            rarityPercent = "22.1%"
        ),
        currentTier = PreviewBadgeTier.SUPER_8,
        isUnlocked = true,
        unlockedDate = "15 Lug 2026"
    )
)

/**
 * Schermata Vetrina dei Trofei (Trophy Showcase) in Puro Stile FlickTrove:
 * - Sfondo: CinematicBackground()
 * - TopBar: GlassyTopBar nativa FlickTrove (pulsante filtri unico in alto a destra con bounds)
 * - Dashboard: Card sobria ed elegante in stile statsCard FlickTrove
 * - Griglia Trofei: 2 colonne con le card rifinite (senza doppioni di tab/filtri in mezzo)
 * - BottomBar: GlassyBottomBar nativa FlickTrove (i 4 tab storici: Home, Da vedere, Visti, Account)
 * - Modali: MorphGlassModal nativo per i filtri e GlassmorphicModal concentrico a 32dp per il dettaglio.
 */
@Composable
fun TrophyRoomScreenContent(
    paddingValues: PaddingValues = PaddingValues(),
    hazeState: HazeState? = null,
    isFilterModalOpenExternal: Boolean = false,
    onFilterModalOpenExternalChange: (Boolean) -> Unit = {}
) {
    val localHaze = hazeState ?: remember { HazeState() }
    val gridState = rememberLazyGridState()

    var filterConfig by remember { mutableStateOf(TrophyFilterConfig()) }
    var filterTriggerBounds by remember { mutableStateOf<Rect?>(null) }
    // Se controllato dall'esterno (via MainScreen TopBar), usa lo stato esterno
    var isFilterModalOpenInternal by remember { mutableStateOf(false) }
    val isFilterModalOpen = isFilterModalOpenExternal || isFilterModalOpenInternal
    var selectedBadgeForModal by remember { mutableStateOf<TrophyRoomItemUi?>(null) }

    // Registra la configurazione filtri nel contesto globale di MainScreen per la GlassyTopBar
    val activeTrophyFilterState = com.cinetrack.ui.LocalActiveTrophyFilterConfig.current
    DisposableEffect(filterConfig) {
        activeTrophyFilterState.value = com.cinetrack.ui.TrophyFilterModalConfig(
            config = filterConfig,
            onApply = { newConfig -> filterConfig = newConfig }
        )
        onDispose {
            activeTrophyFilterState.value = null
        }
    }

    val unlockedCount = SAMPLE_TROPHY_ROOM_ITEMS.count { it.isUnlocked }
    val totalCount = SAMPLE_TROPHY_ROOM_ITEMS.size
    val completionFraction = (unlockedCount.toFloat() / totalCount).coerceIn(0f, 1f)
    val unlockedTierCounts = remember {
        SAMPLE_TROPHY_ROOM_ITEMS.filter { it.isUnlocked }.groupBy { it.currentTier }.mapValues { it.value.size }
    }

    // 1. Filtraggio coerente guidato dal modale dei filtri
    val filteredItems = remember(filterConfig) {
        SAMPLE_TROPHY_ROOM_ITEMS.filter { item ->
            // Filtro Categoria (Multi-selezione: vuoto significa tutti)
            val matchesCategory = if (filterConfig.categories.isEmpty()) {
                true
            } else {
                filterConfig.categories.any { cat ->
                    when (cat) {
                        "PROGRESSIVI" -> item.badge.category == BadgeTypeCategory.PROGRESSIVE_TIERS && !item.badge.id.startsWith("genre_") && item.badge.id != "world_tour"
                        "GENERI & EPOCHE" -> item.badge.id.startsWith("genre_") || item.badge.id == "world_tour"
                        "ONORIFICENZE" -> item.badge.category == BadgeTypeCategory.DAY_ONE_HONOR || item.badge.category == BadgeTypeCategory.SPECIAL_ACHIEVEMENT
                        "LOST REEL" -> item.badge.category == BadgeTypeCategory.LOST_REEL_SECRET
                        "FILM" -> item.badge.id.contains("movie") || item.badge.categoryLabel.contains("FILM")
                        "SERIE TV" -> item.badge.id.contains("tv")
                        "SAGHE" -> item.badge.id.contains("saga") || item.badge.categoryLabel.contains("SAGHE")
                        else -> true
                    }
                }
            }
            // Filtro Stato
            val matchesStatus = when (filterConfig.status) {
                "SBLOCCATI" -> item.isUnlocked
                "DA CONQUISTARE" -> !item.isUnlocked
                else -> true
            }
            // Filtro Formato Tier (Multi-selezione: vuoto significa tutti)
            val matchesTier = filterConfig.tiers.isEmpty() || item.currentTier in filterConfig.tiers

            matchesCategory && matchesStatus && matchesTier
        }
    }

    // 2. Ordinamento Dinamico FlickTrove (guidato da filterConfig.sortBy e filterConfig.sortDirection):
    val sortedItems = remember(filteredItems, filterConfig.sortBy, filterConfig.sortDirection) {
        val baseComparator = compareByDescending<TrophyRoomItemUi> { it.isUnlocked }
        val specificComparator = when (filterConfig.sortBy) {
            TrophySortOption.TIER -> {
                compareByDescending<TrophyRoomItemUi> { getTierRank(it.currentTier) }
                    .thenByDescending { it.completionFraction() }
                    .thenBy { it.badge.title.lowercase() }
            }
            TrophySortOption.PROGRESS -> {
                compareByDescending<TrophyRoomItemUi> { it.completionFraction() }
                    .thenByDescending { getTierRank(it.currentTier) }
                    .thenBy { it.badge.title.lowercase() }
            }
            TrophySortOption.RARITY -> {
                compareBy<TrophyRoomItemUi> {
                    it.badge.rarityPercent.replace("%", "").trim().toFloatOrNull() ?: 100f
                }
                    .thenByDescending { getTierRank(it.currentTier) }
                    .thenBy { it.badge.title.lowercase() }
            }
            TrophySortOption.RECENT -> {
                compareByDescending<TrophyRoomItemUi> { it.unlockedDate ?: "" }
                    .thenByDescending { getTierRank(it.currentTier) }
                    .thenBy { it.badge.title.lowercase() }
            }
            TrophySortOption.ALPHABETICAL -> {
                compareBy<TrophyRoomItemUi> { it.badge.title.lowercase() }
                    .thenByDescending { getTierRank(it.currentTier) }
            }
        }
        val finalComparator = if (filterConfig.sortDirection == "asc") {
            baseComparator.then(specificComparator.reversed())
        } else {
            baseComparator.then(specificComparator)
        }
        filteredItems.sortedWith(finalComparator)
    }

    // Il top padding viene fornito da LocalAppPadding di MainScreen (via GlassyTopBar height + status bar)
    // Il bottom padding viene fornito da LocalAppPadding (80.dp per BottomBar)
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val effectiveTopInset = maxOf(statusBarTop, cutoutTop)
    val topPadding = if (paddingValues.calculateTopPadding() > 0.dp) paddingValues.calculateTopPadding() else effectiveTopInset + 10.dp + 46.dp + 22.dp
    val bottomPadding = if (paddingValues.calculateBottomPadding() > 0.dp) paddingValues.calculateBottomPadding() + 24.dp else 110.dp

    Box(modifier = Modifier.fillMaxSize()) {
        val hazeState = localHaze
        // 1. Sfondo Cinematografico FlickTrove
        CinematicBackground()

        // 2. Griglia dei Trofei con Haze
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = topPadding,
                bottom = bottomPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
        ) {
            // A. STATS CARD MINIMALISTA & CINETECA SUMMARY (Stile statsCard autentico FlickTrove)
            item(span = { GridItemSpan(maxLineSpan) }) {
                TrophyStatsSummaryCard(
                    unlockedCount = unlockedCount,
                    totalCount = totalCount,
                    completionFraction = completionFraction,
                    tierCounts = unlockedTierCounts
                )
            }

            // B. SEGNAPOSTO SPAZIALE TRA STATS E GRIGLIA (Niente filtri duplicati qui: sono nell'icona TopBar)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // C. LE CARD DEI TROFEI NELLA GRIGLIA (Ordinamento: Sbloccati in cima, bloccati in fondo, tutto per rarità)
            items(
                items = sortedItems,
                key = { it.badge.id }
            ) { item ->
                TrophyBadgeCard(
                    badge = item.badge,
                    tier = item.currentTier,
                    isUnlocked = item.isUnlocked,
                    onClick = {
                        selectedBadgeForModal = item
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // La GlassyTopBar e la GlassyBottomBar sono fornite da MainScreen (fisse e condivise).
        // Il pulsante filtro nella TopBar triggera isFilterModalOpen tramite onFilterRequest.

        var lastSelectedBadge by remember { mutableStateOf<TrophyRoomItemUi?>(null) }
        if (selectedBadgeForModal != null) {
            lastSelectedBadge = selectedBadgeForModal
        }

        // 5. MODALE DI DETTAGLIO OVERLAY (GlassmorphicModal autentico di FlickTrove)
        GlassmorphicModal(
            visible = selectedBadgeForModal != null,
            activeHazeState = hazeState,
            dimBackground = true,
            dismissOnClickOutside = true,
            onDismissRequest = { selectedBadgeForModal = null }
        ) {
            val currentModalItem = selectedBadgeForModal ?: lastSelectedBadge
            if (currentModalItem != null) {
                TrophyDetailContent(
                    item = currentModalItem,
                    onClose = { selectedBadgeForModal = null }
                )
            }
        }

        // 6. MODALE DEI FILTRI (Fallback se invocato fuori da MainScreen)
        if (activeTrophyFilterState.value == null) {
            TrophyFilterModal(
                isVisible = isFilterModalOpen,
                config = filterConfig,
                triggerBounds = filterTriggerBounds,
                hazeState = hazeState,
                onApply = { newConfig ->
                    filterConfig = newConfig
                    isFilterModalOpenInternal = false
                    onFilterModalOpenExternalChange(false)
                },
                onDismissRequest = {
                    isFilterModalOpenInternal = false
                    onFilterModalOpenExternalChange(false)
                }
            )
        }
    }
}

/**
 * Card di Riepilogo Trofei sobria ed elegante, costruita con Modifier.statsCard()
 * in perfetto stile FlickTrove (dark neutro, bordi satinati e micro-metriche).
 */
@Composable
private fun TrophyStatsSummaryCard(
    unlockedCount: Int,
    totalCount: Int,
    completionFraction: Float,
    tierCounts: Map<PreviewBadgeTier, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statsCard()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PALMARES CINEFILO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        ),
                        color = NeonTeal
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$unlockedCount",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = " / $totalCount sbloccati",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                        )
                    }
                }

                // Percentuale a pillola discreta
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(NeonTeal.copy(alpha = 0.12f))
                        .border(1.dp, NeonTeal.copy(alpha = 0.35f), CircleShape)
                        .padding(horizontal = 9.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = "${(completionFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        ),
                        color = NeonTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Micro-barra satinata
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = completionFraction)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(NeonTeal, Color(0xFF10B981))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sintesi ultra-compatta su 3 colonne x 2 righe (dimezza l'altezza verticale)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactTierBadge(PreviewBadgeTier.SUPER_8, (tierCounts[PreviewBadgeTier.SUPER_8] ?: 0).toString(), Modifier.weight(1f))
                    CompactTierBadge(PreviewBadgeTier.MM_16, (tierCounts[PreviewBadgeTier.MM_16] ?: 0).toString(), Modifier.weight(1f))
                    CompactTierBadge(PreviewBadgeTier.MM_35, (tierCounts[PreviewBadgeTier.MM_35] ?: 0).toString(), Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactTierBadge(PreviewBadgeTier.MM_70, (tierCounts[PreviewBadgeTier.MM_70] ?: 0).toString(), Modifier.weight(1f))
                    CompactTierBadge(PreviewBadgeTier.THE_FINAL_CUT, (tierCounts[PreviewBadgeTier.THE_FINAL_CUT] ?: 0).toString(), Modifier.weight(1f))
                    CompactTierBadge(PreviewBadgeTier.LOST_REEL, (tierCounts[PreviewBadgeTier.LOST_REEL] ?: 0).toString(), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CompactTierBadge(
    tier: PreviewBadgeTier,
    count: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = tier.primaryColor.copy(alpha = 0.85f),
                shape = CircleShape
            )
            .padding(start = 7.dp, top = 2.5.dp, bottom = 2.5.dp, end = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Pallino colorato del Tier
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(tier.primaryColor)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // 2. Nome del Formato / Tier in bianco
        Text(
            text = tier.shortLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.weight(1f))

        // 3. Badge numerico circolare concentrico a destra
        Box(
            modifier = Modifier
                .size(23.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                ),
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Modale di Filtro Autentico FlickTrove con MorphGlassModal (conforme alle regole 14 e 15 di GEMINI.md).
 * Si espande direttamente dal pulsante filtri in alto a destra e gestisce Categoria, Stato e Tier.
 */
@Composable
fun TrophyFilterModal(
    isVisible: Boolean,
    config: TrophyFilterConfig,
    triggerBounds: Rect?,
    hazeState: HazeState?,
    onApply: (TrophyFilterConfig) -> Unit,
    onDismissRequest: () -> Unit
) {
    var localConfig by remember(isVisible) { mutableStateOf(config) }
    var expandedSection by remember(isVisible) { mutableStateOf<String?>(null) }

    val categoryList = listOf("TUTTI", "PROGRESSIVI", "GENERI & EPOCHE", "ONORIFICENZE", "LOST REEL")
    val statusList = listOf("TUTTI", "SBLOCCATI", "DA CONQUISTARE")
    val tierList = listOf(
        null to "TUTTI",
        PreviewBadgeTier.LOST_REEL to "LOST REEL",
        PreviewBadgeTier.THE_FINAL_CUT to "FINAL CUT",
        PreviewBadgeTier.MM_70 to "70MM",
        PreviewBadgeTier.MM_35 to "35MM",
        PreviewBadgeTier.MM_16 to "16MM",
        PreviewBadgeTier.SUPER_8 to "SUPER 8"
    )

    MorphGlassModal(
        isVisible = isVisible,
        onDismissRequest = onDismissRequest,
        triggerBounds = triggerBounds,
        hazeState = hazeState,
        targetMaxWidth = 420.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // --- HEADER BAR (1:1 con HomeFilterModal) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "FILTRA TROFEI",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (localConfig.hasActiveFilters) {
                        Row(
                            modifier = Modifier
                                .bounceClick {
                                    localConfig = TrophyFilterConfig()
                                }
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_ricarica),
                                contentDescription = "Reset filtri",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.filter_reset),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    ModalCloseButton(
                        onClose = onDismissRequest
                    )
                }
            }

            // --- SCROLLABLE CONTENT (Senza spaziatura forzata tra card: spacing nativo 4dp da ExpandableSection) ---
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalFadingEdges(scrollState, 16.dp, 16.dp)
                    .verticalScroll(scrollState)
                    .padding(bottom = 12.dp)
            ) {
                // 1. ORDINA PER (Ufficiale FlickTrove con SortOptionItem e DirectionChip)
                ExpandableSection(
                    title = "ORDINA PER",
                    isExpanded = expandedSection == "sort",
                    showChevron = true,
                    isClickable = true,
                    badgeCount = if (localConfig.sortBy != TrophySortOption.TIER || localConfig.sortDirection != "desc") 1 else 0,
                    onToggle = { expandedSection = if (expandedSection == "sort") null else "sort" }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrophySortOption.values().forEach { option ->
                            SortOptionItem(
                                label = option.displayName,
                                isSelected = localConfig.sortBy == option,
                                onClick = { localConfig = localConfig.copy(sortBy = option) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DirectionChip(
                                label = stringResource(R.string.filter_dir_desc),
                                isSelected = localConfig.sortDirection == "desc",
                                icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                                iconRotation = 90f,
                                modifier = Modifier.weight(1f),
                                onClick = { localConfig = localConfig.copy(sortDirection = "desc") }
                            )
                            DirectionChip(
                                label = stringResource(R.string.filter_dir_asc),
                                isSelected = localConfig.sortDirection == "asc",
                                icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                                iconRotation = -90f,
                                modifier = Modifier.weight(1f),
                                onClick = { localConfig = localConfig.copy(sortDirection = "asc") }
                            )
                        }
                    }
                }

                // 2. CATEGORIA (Multi-selezione con FilterChip)
                ExpandableSection(
                    title = "CATEGORIA",
                    isExpanded = expandedSection == "category",
                    showChevron = true,
                    isClickable = true,
                    badgeCount = localConfig.categories.size,
                    onToggle = { expandedSection = if (expandedSection == "category") null else "category" }
                ) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryList.forEach { cat ->
                            val isAll = cat == "TUTTI"
                            val isSelected = if (isAll) localConfig.categories.isEmpty() else cat in localConfig.categories
                            val isLostReel = cat == "LOST REEL"
                            val lostReelColor = if (isLostReel) PreviewBadgeTier.LOST_REEL.primaryColor else null

                            FilterChip(
                                label = cat,
                                isSelected = isSelected,
                                onClick = {
                                    localConfig = if (isAll) {
                                        localConfig.copy(categories = emptySet())
                                    } else {
                                        val newSet = if (isSelected) localConfig.categories - cat else localConfig.categories + cat
                                        localConfig.copy(categories = newSet)
                                    }
                                },
                                accentColor = lostReelColor ?: MaterialTheme.colorScheme.primary,
                                unselectedBorderColor = lostReelColor?.copy(alpha = 0.35f) ?: Color.Transparent,
                                unselectedBgColor = if (lostReelColor != null) lostReelColor.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.45f),
                                dotColor = lostReelColor
                            )
                        }
                    }
                }

                // 3. STATO (FilterChip ufficiali dell'app)
                ExpandableSection(
                    title = "STATO",
                    isExpanded = expandedSection == "status",
                    showChevron = true,
                    isClickable = true,
                    badgeCount = if (localConfig.status != "TUTTI") 1 else 0,
                    onToggle = { expandedSection = if (expandedSection == "status") null else "status" }
                ) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusList.forEach { statusOption ->
                            FilterChip(
                                label = statusOption,
                                isSelected = localConfig.status == statusOption,
                                onClick = { localConfig = localConfig.copy(status = statusOption) }
                            )
                        }
                    }
                }

                // 4. FORMATO PELLICOLA (Multi-selezione con FilterChip)
                ExpandableSection(
                    title = "FORMATO PELLICOLA",
                    isExpanded = expandedSection == "tier",
                    showChevron = true,
                    isClickable = true,
                    badgeCount = localConfig.tiers.size,
                    onToggle = { expandedSection = if (expandedSection == "tier") null else "tier" }
                ) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tierList.forEach { (tierOption, label) ->
                            val isAll = tierOption == null
                            val isSelected = if (isAll) localConfig.tiers.isEmpty() else tierOption in localConfig.tiers
                            val tierColor = tierOption?.primaryColor

                            FilterChip(
                                label = label,
                                isSelected = isSelected,
                                onClick = {
                                    localConfig = if (isAll) {
                                        localConfig.copy(tiers = emptySet())
                                    } else {
                                        val newSet = if (isSelected) localConfig.tiers - tierOption else localConfig.tiers + tierOption
                                        localConfig.copy(tiers = newSet)
                                    }
                                },
                                accentColor = tierColor ?: MaterialTheme.colorScheme.primary,
                                unselectedBorderColor = tierColor?.copy(alpha = 0.35f) ?: Color.Transparent,
                                unselectedBgColor = if (tierColor != null) tierColor.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.45f),
                                dotColor = tierColor
                            )
                        }
                    }
                }
            }

            // --- PULSANTE APPLICA (1:1 con HomeFilterModal) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp, top = 8.dp)
                    .height(56.dp)
                    .bounceClick {
                        onApply(localConfig)
                        onDismissRequest()
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.filter_apply),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

/**
 * Contenuto del Modale di Dettaglio del Trofeo, ospitato dentro GlassmorphicModal autentico.
 * Rispetta la concentricità 32dp/16dp, icone nude e purezza cromatica del Tier.
 */
@Composable
fun TrophyDetailContent(
    item: TrophyRoomItemUi,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.85f).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .padding(horizontal = 22.dp)
            .padding(top = 16.dp, bottom = 22.dp)
    ) {
        // Pulsante di Chiusura Concentrico a 16dp
        Box(modifier = Modifier.align(Alignment.TopEnd).zIndex(10f)) {
            ModalCloseButton(onClose = onClose)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            val availableTiers = item.badge.progressiveSteps.keys.sortedBy { it.ordinal }
            val firstTier = availableTiers.firstOrNull() ?: item.currentTier
            val targetTier = if (!item.isUnlocked) {
                firstTier
            } else {
                availableTiers.firstOrNull { it.ordinal > item.currentTier.ordinal }
            }

            // Pillola Formato Tier sopra al badge circolare
            if (item.isUnlocked) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(item.currentTier.primaryColor.copy(alpha = 0.14f))
                        .border(1.dp, item.currentTier.primaryColor.copy(alpha = 0.38f), CircleShape)
                        .padding(horizontal = 11.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = item.currentTier.formatName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        ),
                        color = item.currentTier.primaryColor
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 11.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = if (item.badge.category == BadgeTypeCategory.LOST_REEL_SECRET) "LOST REEL"
                        else "DA SBLOCCARE • ${targetTier?.formatName?.uppercase() ?: "1° LIVELLO"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        ),
                        color = if (item.badge.category == BadgeTypeCategory.LOST_REEL_SECRET) Color(0xFF818CF8) else Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Emblema grande del trofeo con il colore del tier
            FlickTroveBadgeEmblem(
                tier = if (item.isUnlocked) item.currentTier else (targetTier ?: item.currentTier),
                iconKind = item.badge.iconKind,
                isUnlocked = item.isUnlocked,
                size = 110.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Titolo del Trofeo (centrato direttamente sotto l'emblema)
            Text(
                text = item.badge.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Chip meta affiancate: Prima Data di sblocco e poi Rarità
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.isUnlocked && item.unlockedDate != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.07f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.unlockedDate,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Sbloccato dal ${item.badge.rarityPercent} dei cinefili",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider sottile
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.07f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Paragrafo Descrittivo
            val desc = if (!item.isUnlocked && item.badge.secretHint != null) {
                item.badge.secretHint
            } else {
                item.badge.progressiveSteps[item.currentTier]?.description ?: item.badge.defaultDescription
            }

            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                ),
                color = Color.White.copy(alpha = 0.70f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. PROGRESSIONE CHIARA: quanto ho visto e quanto serve per il prossimo traguardo
            if (item.badge.category == BadgeTypeCategory.PROGRESSIVE_TIERS) {
                val currentVal = item.badge.progressCurrent
                val unit = item.badge.progressUnit
                val isMaxTier = item.isUnlocked && (targetTier == null || item.currentTier == availableTiers.lastOrNull())

                val nextTarget = if (!item.isUnlocked) {
                    item.badge.progressiveSteps[firstTier]?.targetThreshold ?: item.badge.progressTarget
                } else {
                    targetTier?.let { item.badge.progressiveSteps[it]?.targetThreshold }
                }
                val needed = if (nextTarget != null) (nextTarget - currentVal).coerceAtLeast(0) else 0
                val progressFraction = if (nextTarget != null && nextTarget > 0) {
                    (currentVal.toFloat() / nextTarget).coerceIn(0f, 1f)
                } else 1f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isMaxTier) "TRAGUARDO MASSIMO" else "PROSSIMO OBIETTIVO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.White.copy(alpha = 0.50f)
                            )

                            if (targetTier != null) {
                                Text(
                                    text = "${(progressFraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = if (item.isUnlocked) targetTier.primaryColor else Color(0xFF10B981)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Valore numerico
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$currentVal",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            if (nextTarget != null) {
                                Text(
                                    text = " / $nextTarget $unit",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                                )
                            } else {
                                val completedSuffix = if (unit.equals("ore", ignoreCase = true) || unit.endsWith("e", ignoreCase = true)) "completate" else "completati"
                                Text(
                                    text = if (unit.isNotBlank()) " $unit $completedSuffix" else " completati",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Barra di avanzamento
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressFraction)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (item.isUnlocked && targetTier != null) {
                                            Brush.horizontalGradient(
                                                listOf(item.currentTier.primaryColor, targetTier.primaryColor)
                                            )
                                        } else if (item.isUnlocked) {
                                            Brush.horizontalGradient(
                                                listOf(item.currentTier.primaryColor, item.currentTier.secondaryColor)
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                listOf(Color.White.copy(alpha = 0.20f), Color(0xFF10B981))
                                            )
                                        }
                                    )
                            )
                        }

                        // Testo di supporto — senza ripetere il target già indicato sopra
                        if (!isMaxTier && targetTier != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val targetLabel = if (!item.isUnlocked) "il trofeo (${targetTier.formatName})" else targetTier.formatName
                            Text(
                                text = "Ti mancano $needed $unit per sbloccare $targetLabel.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                ),
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        } else if (isMaxTier) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Hai raggiunto il traguardo massimo di questo trofeo.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                ),
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }
                    }
                }

                // Timeline dei Formati: visibile SOLO se il trofeo è ancora in corso di scalata (!isMaxTier)
                if (!isMaxTier) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val maxStepTarget = (availableTiers.lastOrNull()?.let { item.badge.progressiveSteps[it]?.targetThreshold }) ?: item.badge.progressTarget
                    BadgeTierEvolutionTrack(
                        currentTier = item.currentTier,
                        isUnlocked = item.isUnlocked,
                        progressCurrent = item.badge.progressCurrent,
                        maxTarget = maxStepTarget,
                        stepProgressFraction = progressFraction,
                        tierSteps = if (availableTiers.isNotEmpty()) availableTiers else listOf(
                            PreviewBadgeTier.SUPER_8,
                            PreviewBadgeTier.MM_16,
                            PreviewBadgeTier.MM_35,
                            PreviewBadgeTier.MM_70,
                            PreviewBadgeTier.THE_FINAL_CUT
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ANTEPRIME COMPOSE ANDROID STUDIO (SPLIT VIEW)
// -------------------------------------------------------------

@Preview(
    name = "Trophy Room Ufficiale FlickTrove (TopBar + BottomBar)",
    showBackground = true,
    backgroundColor = 0xFF090A10,
    device = "spec:width=411dp,height=891dp"
)
@Composable
private fun TrophyRoomScreenPreviewFull() {
    FlickTroveTheme {
        TrophyRoomScreenContent()
    }
}

@Preview(
    name = "Modale Filtro Trofei Ufficiale",
    showBackground = true,
    backgroundColor = 0xFF090A10,
    device = "spec:width=411dp,height=891dp"
)
@Composable
private fun TrophyFilterModalPreview() {
    FlickTroveTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            TrophyFilterModal(
                isVisible = true,
                config = TrophyFilterConfig(),
                triggerBounds = null,
                hazeState = null,
                onApply = {},
                onDismissRequest = {}
            )
        }
    }
}

@Preview(
    name = "Modale Dettaglio Trofeo Ufficiale (Con Data affiancata)",
    showBackground = true,
    backgroundColor = 0xFF090A10,
    device = "spec:width=411dp,height=891dp"
)
@Composable
private fun TrophyDetailModalPreview() {
    FlickTroveTheme {
        val hazeState = remember { HazeState() }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            GlassmorphicModal(
                visible = true,
                activeHazeState = hazeState,
                dimBackground = true,
                dismissOnClickOutside = true,
                onDismissRequest = {}
            ) { _ ->
                TrophyDetailContent(
                    item = SAMPLE_TROPHY_ROOM_ITEMS[0],
                    onClose = {}
                )
            }
        }
    }
}

