package com.cinetrack.ui.components.badge

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import com.cinetrack.R
import com.cinetrack.ui.theme.FlickTroveTheme
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.VibrationHelper
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tassonomia cinematografica ufficiale dei Tier trofei FlickTrove.
 * Colori unificati e coerenti:
 * - Super 8: Rame / Ambra Vintage d'epoca
 * - 16mm: Smeraldo FlickTrove
 * - 35mm: Azzurro Zaffiro Brillante
 * - 70mm: Oro Regale Brillante
 * - The Final Cut: Ametista & Magenta Profondo Puro
 * - Lost Reel: Viola Nebulosa & Nero Onice
 */
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource

enum class PreviewBadgeTier(
    val formatName: String,
    val shortLabel: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color,
    @StringRes val formatNameRes: Int = 0
) {
    SUPER_8(
        formatName = "Super 8",
        shortLabel = "SUPER 8",
        subtitle = "Scoperte & Nostalgia",
        primaryColor = Color(0xFFD97706),
        secondaryColor = Color(0xFF9A3412),
        glowColor = Color(0x66D97706),
        formatNameRes = R.string.trophy_format_super_8
    ),
    MM_16(
        formatName = "16mm",
        shortLabel = "16MM",
        subtitle = "Circuito Indie",
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF059669),
        glowColor = Color(0x6610B981),
        formatNameRes = R.string.trophy_format_16mm
    ),
    MM_35(
        formatName = "35mm",
        shortLabel = "35MM",
        subtitle = "Standard Sala",
        primaryColor = Color(0xFF60A5FA),
        secondaryColor = Color(0xFF2563EB),
        glowColor = Color(0x6660A5FA),
        formatNameRes = R.string.trophy_format_35mm
    ),
    MM_70(
        formatName = "70mm",
        shortLabel = "70MM",
        subtitle = "Festival & Grand Prix",
        primaryColor = Color(0xFFFFC107),
        secondaryColor = Color(0xFFCA8A04),
        glowColor = Color(0x66FFC107),
        formatNameRes = R.string.trophy_format_70mm
    ),
    THE_FINAL_CUT(
        formatName = "The Final Cut",
        shortLabel = "FINAL CUT",
        subtitle = "L'Opera Compiuta",
        primaryColor = Color(0xFFEC4899),
        secondaryColor = Color(0xFFBE185D),
        glowColor = Color(0x77EC4899),
        formatNameRes = R.string.trophy_format_final_cut
    ),
    LOST_REEL(
        formatName = "Lost Reel",
        shortLabel = "LOST REEL",
        subtitle = "La Bobina Perduta",
        primaryColor = Color(0xFF8B5CF6),
        secondaryColor = Color(0xFF6D28D9),
        glowColor = Color(0x668B5CF6),
        formatNameRes = R.string.trophy_format_lost_reel
    );

    @Composable
    fun localizedFormatName(): String = if (formatNameRes != 0) stringResource(formatNameRes) else formatName
}

/**
 * Tipi di icone personalizzate dedicate ad ogni specifico badge.
 */
enum class CustomBadgeIconKind(@DrawableRes val iconRes: Int) {
    // 12 Badge Progressivi
    CINEMA_REEL(R.drawable.ic_badge_film_spool),
    TV_BINGE(R.drawable.ic_badge_tv),
    BINGE_NIGHT(R.drawable.ic_badge_popcorn),
    ARCHIVE_STACK(R.drawable.ic_badge_strongbox),
    CINECLUB_PASS(R.drawable.ic_badge_ticket),
    TIME_ODYSSEY(R.drawable.ic_badge_hourglass),
    SAGA_TRILOGY(R.drawable.ic_badge_crown),
    REWATCH_DEJAVU(R.drawable.ic_badge_infinity),
    STAR_RATING(R.drawable.ic_badge_star_medal),
    EMOTIONAL_VIBES(R.drawable.ic_badge_drama_masks),
    CRITIC_VOICE(R.drawable.ic_badge_acoustic_megaphone),
    AUTHOR_FOLDERS(R.drawable.ic_badge_full_folder),

    // 7 Esplorazione Generi & Epoche
    GENRE_HORROR(R.drawable.ic_badge_screaming),
    GENRE_SCIFI(R.drawable.ic_badge_spiky_field),
    GENRE_THRILLER(R.drawable.ic_badge_magnifying_glass),
    GENRE_ANIME(R.drawable.ic_badge_paper_crane),
    GENRE_DOC(R.drawable.ic_badge_video_camera),
    GENRE_VINTAGE(R.drawable.ic_badge_film_projector),
    WORLD_TOUR(R.drawable.ic_badge_world),

    // 8 Traguardi di Prestigio & Onorificenze
    PIONEER_CROWN(R.drawable.ic_badge_laurel_crown),
    SYNC_REEL(R.drawable.ic_badge_cardboard_box),
    GOURMET_CRITIC(R.drawable.ic_badge_hot_meal),
    COLOSSAL_MONUMENT(R.drawable.ic_badge_ionic_column),
    ACTOR_MUSE(R.drawable.ic_badge_star_altar),
    DIRECTOR_CHAIR(R.drawable.ic_badge_director_chair),
    PERSONAL_DIARY(R.drawable.ic_badge_notebook),
    CUSTOM_BACKDROP(R.drawable.ic_badge_light_projector),

    // 10 Lost Reel (Bobine Perdute Segrete)
    EXECUTIVE_PRODUCER(R.drawable.ic_badge_shaking_hands),
    GROUNDHOG_LOOP(R.drawable.ic_badge_alarm_clock),
    GENRELESS_REBEL(R.drawable.ic_badge_despair),
    NIGHT_OWL_MOON(R.drawable.ic_badge_owl),
    ROULETTE_FATE(R.drawable.ic_badge_rolling_dices),
    INDECISIVE_CRITIC(R.drawable.ic_badge_scales),
    COMPLETIONIST_MIND(R.drawable.ic_badge_checkered_diamond),
    GENESIS_ORIGINS(R.drawable.ic_badge_lantern_flame),
    IRON_HEART(R.drawable.ic_badge_heart_shield),
    UNLOVED_GEM(R.drawable.ic_badge_diamond_hard),

    // Alias per retrocompatibilità
    DOUBLE_TICKET(R.drawable.ic_badge_ticket),
    SECRET_KEY(R.drawable.ic_badge_strongbox)
}

/**
 * Categoria concettuale del badge.
 */
enum class BadgeTypeCategory {
    PROGRESSIVE_TIERS,   // 1 sola card dinamica che evolve nei 5 formati (Super 8 -> Final Cut)
    DAY_ONE_HONOR,       // Traguardo unico leggendario (The Final Cut fisso)
    SPECIAL_ACHIEVEMENT, // Traguardo cinematografico fisso (non progressivo)
    LOST_REEL_SECRET     // Bobina perduta segreta (Lost Reel fisso, nessun tier numerico)
}

/**
 * Livello specifico all'interno di un badge progressivo.
 */
data class ProgressiveTierDetail(
    val tier: PreviewBadgeTier,
    val targetThreshold: Int,
    val description: String,
    val milestonePhrase: String
)

/**
 * Modello descrittivo di un Badge per l'anteprima del catalogo trofei.
 */
data class PreviewBadgeItem(
    val id: String,
    val title: String,
    val category: BadgeTypeCategory,
    val fixedTier: PreviewBadgeTier?,
    val initialProgressiveTier: PreviewBadgeTier = PreviewBadgeTier.SUPER_8,
    val iconKind: CustomBadgeIconKind,
    val categoryLabel: String,
    val defaultDescription: String,
    val progressiveSteps: Map<PreviewBadgeTier, ProgressiveTierDetail> = emptyMap(),
    val secretHint: String? = null,
    val progressCurrent: Int = 0,
    val progressTarget: Int = 0,
    val progressUnit: String = "",
    val rarityPercent: String = "4.2%",
    @StringRes val titleRes: Int = 0,
    @StringRes val categoryLabelRes: Int = 0,
    @StringRes val defaultDescRes: Int = 0,
    @StringRes val secretHintRes: Int? = null,
    @StringRes val progressUnitRes: Int? = null
)

fun getBadgeTitleRes(badgeId: String): Int = when (badgeId) {
    "secret_groundhog_day" -> R.string.badge_secret_groundhog_day_title
    "secret_surprise_fate" -> R.string.badge_secret_surprise_fate_title
    "secret_midnight_madness" -> R.string.badge_secret_midnight_madness_title
    "secret_cinephile_streak" -> R.string.badge_secret_cinephile_streak_title
    "day_one_pioneer" -> R.string.badge_day_one_pioneer_title
    "badge_movies" -> R.string.badge_movies_title
    "badge_movie_runtime" -> R.string.badge_movie_runtime_title
    "badge_tv_episodes" -> R.string.badge_tv_episodes_title
    "badge_tv_shows" -> R.string.badge_tv_shows_title
    "badge_rewatch" -> R.string.badge_rewatch_title
    "genre_horror" -> R.string.badge_genre_horror_title
    "genre_sci_fi" -> R.string.badge_genre_sci_fi_title
    "genre_thriller" -> R.string.badge_genre_thriller_title
    "genre_comedy" -> R.string.badge_genre_comedy_title
    "genre_animation" -> R.string.badge_genre_animation_title
    "genre_drama" -> R.string.badge_genre_drama_title
    "genre_documentary" -> R.string.badge_genre_documentary_title
    "genre_action" -> R.string.badge_genre_action_title
    "genre_fantasy" -> R.string.badge_genre_fantasy_title
    "genre_romance" -> R.string.badge_genre_romance_title
    "genre_classic_cinema" -> R.string.badge_genre_classic_cinema_title
    "world_tour" -> R.string.badge_world_tour_title
    "saga_lord_of_rings" -> R.string.badge_saga_lord_of_rings_title
    "saga_star_wars" -> R.string.badge_saga_star_wars_title
    "saga_harry_potter" -> R.string.badge_saga_harry_potter_title
    "saga_marvel_infinity" -> R.string.badge_saga_marvel_infinity_title
    "saga_christopher_nolan" -> R.string.badge_saga_christopher_nolan_title
    "saga_quentin_tarantino" -> R.string.badge_saga_quentin_tarantino_title
    "saga_studio_ghibli" -> R.string.badge_saga_studio_ghibli_title
    "saga_alien" -> R.string.badge_saga_alien_title
    "saga_godfather" -> R.string.badge_saga_godfather_title
    "saga_stanley_kubrick" -> R.string.badge_saga_stanley_kubrick_title
    "special_curator" -> R.string.badge_special_curator_title
    "special_cinematic_critic" -> R.string.badge_special_cinematic_critic_title
    "actor_director_fetish" -> R.string.badge_actor_director_fetish_title
    "personal_diary" -> R.string.badge_personal_diary_title
    "custom_backdrop" -> R.string.badge_custom_backdrop_title
    else -> 0
}

fun getBadgeCategoryRes(badgeId: String): Int = when (badgeId) {
    "secret_groundhog_day" -> R.string.badge_secret_groundhog_day_cat
    "secret_surprise_fate" -> R.string.badge_secret_surprise_fate_cat
    "secret_midnight_madness" -> R.string.badge_secret_midnight_madness_cat
    "secret_cinephile_streak" -> R.string.badge_secret_cinephile_streak_cat
    "day_one_pioneer" -> R.string.badge_day_one_pioneer_cat
    "badge_movies" -> R.string.badge_movies_cat
    "badge_movie_runtime" -> R.string.badge_movie_runtime_cat
    "badge_tv_episodes" -> R.string.badge_tv_episodes_cat
    "badge_tv_shows" -> R.string.badge_tv_shows_cat
    "badge_rewatch" -> R.string.badge_rewatch_cat
    "genre_horror" -> R.string.badge_genre_horror_cat
    "genre_sci_fi" -> R.string.badge_genre_sci_fi_cat
    "genre_thriller" -> R.string.badge_genre_thriller_cat
    "genre_comedy" -> R.string.badge_genre_comedy_cat
    "genre_animation" -> R.string.badge_genre_animation_cat
    "genre_drama" -> R.string.badge_genre_drama_cat
    "genre_documentary" -> R.string.badge_genre_documentary_cat
    "genre_action" -> R.string.badge_genre_action_cat
    "genre_fantasy" -> R.string.badge_genre_fantasy_cat
    "genre_romance" -> R.string.badge_genre_romance_cat
    "genre_classic_cinema" -> R.string.badge_genre_classic_cinema_cat
    "world_tour" -> R.string.badge_world_tour_cat
    "saga_lord_of_rings" -> R.string.badge_saga_lord_of_rings_cat
    "saga_star_wars" -> R.string.badge_saga_star_wars_cat
    "saga_harry_potter" -> R.string.badge_saga_harry_potter_cat
    "saga_marvel_infinity" -> R.string.badge_saga_marvel_infinity_cat
    "saga_christopher_nolan" -> R.string.badge_saga_christopher_nolan_cat
    "saga_quentin_tarantino" -> R.string.badge_saga_quentin_tarantino_cat
    "saga_studio_ghibli" -> R.string.badge_saga_studio_ghibli_cat
    "saga_alien" -> R.string.badge_saga_alien_cat
    "saga_godfather" -> R.string.badge_saga_godfather_cat
    "saga_stanley_kubrick" -> R.string.badge_saga_stanley_kubrick_cat
    "special_curator" -> R.string.badge_special_curator_cat
    "special_cinematic_critic" -> R.string.badge_special_cinematic_critic_cat
    "actor_director_fetish" -> R.string.badge_actor_director_fetish_cat
    "personal_diary" -> R.string.badge_personal_diary_cat
    "custom_backdrop" -> R.string.badge_custom_backdrop_cat
    else -> 0
}

fun getBadgeDescriptionRes(badgeId: String): Int = when (badgeId) {
    "secret_groundhog_day" -> R.string.badge_secret_groundhog_day_desc
    "secret_surprise_fate" -> R.string.badge_secret_surprise_fate_desc
    "secret_midnight_madness" -> R.string.badge_secret_midnight_madness_desc
    "secret_cinephile_streak" -> R.string.badge_secret_cinephile_streak_desc
    "day_one_pioneer" -> R.string.badge_day_one_pioneer_desc
    "badge_movies" -> R.string.badge_movies_desc
    "badge_movie_runtime" -> R.string.badge_movie_runtime_desc
    "badge_tv_episodes" -> R.string.badge_tv_episodes_desc
    "badge_tv_shows" -> R.string.badge_tv_shows_desc
    "badge_rewatch" -> R.string.badge_rewatch_desc
    "genre_horror" -> R.string.badge_genre_horror_desc
    "genre_sci_fi" -> R.string.badge_genre_sci_fi_desc
    "genre_thriller" -> R.string.badge_genre_thriller_desc
    "genre_comedy" -> R.string.badge_genre_comedy_desc
    "genre_animation" -> R.string.badge_genre_animation_desc
    "genre_drama" -> R.string.badge_genre_drama_desc
    "genre_documentary" -> R.string.badge_genre_documentary_desc
    "genre_action" -> R.string.badge_genre_action_desc
    "genre_fantasy" -> R.string.badge_genre_fantasy_desc
    "genre_romance" -> R.string.badge_genre_romance_desc
    "genre_classic_cinema" -> R.string.badge_genre_classic_cinema_desc
    "world_tour" -> R.string.badge_world_tour_desc
    "saga_lord_of_rings" -> R.string.badge_saga_lord_of_rings_desc
    "saga_star_wars" -> R.string.badge_saga_star_wars_desc
    "saga_harry_potter" -> R.string.badge_saga_harry_potter_desc
    "saga_marvel_infinity" -> R.string.badge_saga_marvel_infinity_desc
    "saga_christopher_nolan" -> R.string.badge_saga_christopher_nolan_desc
    "saga_quentin_tarantino" -> R.string.badge_saga_quentin_tarantino_desc
    "saga_studio_ghibli" -> R.string.badge_saga_studio_ghibli_desc
    "saga_alien" -> R.string.badge_saga_alien_desc
    "saga_godfather" -> R.string.badge_saga_godfather_desc
    "saga_stanley_kubrick" -> R.string.badge_saga_stanley_kubrick_desc
    "special_curator" -> R.string.badge_special_curator_desc
    "special_cinematic_critic" -> R.string.badge_special_cinematic_critic_desc
    "actor_director_fetish" -> R.string.badge_actor_director_fetish_desc
    "personal_diary" -> R.string.badge_personal_diary_desc
    "custom_backdrop" -> R.string.badge_custom_backdrop_desc
    else -> 0
}

fun getBadgeSecretHintRes(badgeId: String): Int? = when (badgeId) {
    "secret_groundhog_day" -> R.string.badge_secret_groundhog_day_hint
    "secret_surprise_fate" -> R.string.badge_secret_surprise_fate_hint
    "secret_midnight_madness" -> R.string.badge_secret_midnight_madness_hint
    "secret_cinephile_streak" -> R.string.badge_secret_cinephile_streak_hint
    else -> null
}

fun getProgressUnitRes(unit: String): Int? = when (unit.lowercase().trim()) {
    "film", "movies" -> R.string.trophy_unit_movies
    "ore", "hours" -> R.string.trophy_unit_hours
    "episodi", "puntate", "episodes" -> R.string.trophy_unit_episodes
    "serie", "shows", "series" -> R.string.trophy_unit_shows
    "rewatch", "rewatches" -> R.string.trophy_unit_rewatches
    "paesi", "countries" -> R.string.trophy_unit_countries
    "cartelle", "folders" -> R.string.trophy_unit_folders
    "voti", "ratings" -> R.string.trophy_unit_ratings
    "note", "notes" -> R.string.trophy_unit_notes
    else -> null
}

@Composable
fun PreviewBadgeItem.localizedTitle(): String {
    val resId = if (titleRes != 0) titleRes else getBadgeTitleRes(id)
    return if (resId != 0) stringResource(resId) else title
}

@Composable
fun PreviewBadgeItem.localizedCategoryLabel(): String {
    val resId = if (categoryLabelRes != 0) categoryLabelRes else getBadgeCategoryRes(id)
    return if (resId != 0) stringResource(resId) else categoryLabel
}

@Composable
fun PreviewBadgeItem.localizedDescription(): String {
    val resId = if (defaultDescRes != 0) defaultDescRes else getBadgeDescriptionRes(id)
    return if (resId != 0) stringResource(resId) else defaultDescription
}

@Composable
fun PreviewBadgeItem.localizedSecretHint(): String? {
    val resId = secretHintRes ?: getBadgeSecretHintRes(id)
    return if (resId != null && resId != 0) stringResource(resId) else secretHint
}

@Composable
fun PreviewBadgeItem.localizedUnit(): String {
    val resId = progressUnitRes ?: getProgressUnitRes(progressUnit)
    return if (resId != null && resId != 0) stringResource(resId) else progressUnit
}

/**
 * Catalogo autentico con testi dinamici e soglie scalari reali da BADGES_PLAN.md.
 */
val OFFICIAL_BADGES_CATALOG = listOf(
    // 1. Progressivo: Film visti con evoluzione scalare da 10 film a 500+ film
    PreviewBadgeItem(
        id = "badge_movies",
        title = "Frequenza 24fps",
        category = BadgeTypeCategory.PROGRESSIVE_TIERS,
        fixedTier = null,
        initialProgressiveTier = PreviewBadgeTier.THE_FINAL_CUT,
        iconKind = CustomBadgeIconKind.CINEMA_REEL,
        categoryLabel = "FILM VISTI",
        defaultDescription = "La passione per il grande schermo attraverso i formati storici della pellicola.",
        progressiveSteps = mapOf(
            PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(
                tier = PreviewBadgeTier.SUPER_8,
                targetThreshold = 10,
                description = "I primi passi nella sala buia. Hai completato i tuoi primi 10 lungometraggi.",
                milestonePhrase = "10 film completati"
            ),
            PreviewBadgeTier.MM_16 to ProgressiveTierDetail(
                tier = PreviewBadgeTier.MM_16,
                targetThreshold = 25,
                description = "Il circuito del cinema d'autore. Hai raggiunto quota 25 lungometraggi completati.",
                milestonePhrase = "25 film completati"
            ),
            PreviewBadgeTier.MM_35 to ProgressiveTierDetail(
                tier = PreviewBadgeTier.MM_35,
                targetThreshold = 100,
                description = "Il ritmo costante della sala buia è diventato la tua seconda casa. Hai completato 100 lungometraggi.",
                milestonePhrase = "100 film completati"
            ),
            PreviewBadgeTier.MM_70 to ProgressiveTierDetail(
                tier = PreviewBadgeTier.MM_70,
                targetThreshold = 250,
                description = "Il grande formato epico e le proiezioni prestigiose. Ben 250 lungometraggi vissuti.",
                milestonePhrase = "250 film completati"
            ),
            PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(
                tier = PreviewBadgeTier.THE_FINAL_CUT,
                targetThreshold = 500,
                description = "L'Opera Compiuta. Oltre 500 capolavori impressi nella tua cineteca personale. Hai raggiunto l'apice leggendario della settima arte.",
                milestonePhrase = "500+ film • Maestria Assoluta"
            )
        ),
        progressCurrent = 500,
        progressTarget = 500,
        progressUnit = "film",
        rarityPercent = "0.8%"
    ),
    // 2. Traguardo Unico: Pioniere (The Final Cut fisso)
    PreviewBadgeItem(
        id = "badge_day_one_pioneer",
        title = "Pioniere Prima Bobina",
        category = BadgeTypeCategory.DAY_ONE_HONOR,
        fixedTier = PreviewBadgeTier.THE_FINAL_CUT,
        iconKind = CustomBadgeIconKind.PIONEER_CROWN,
        categoryLabel = "FONDATORI DAY-ONE",
        defaultDescription = "Hai calcato la platea di FlickTrove nei primi mesi di vita. Un posto d'onore riservato ai veri fondatori.",
        rarityPercent = "1.2%"
    ),
    // 3. Progressivo: Serie TV complete
    PreviewBadgeItem(
        id = "badge_tv",
        title = "Maratoneta Seriale",
        category = BadgeTypeCategory.PROGRESSIVE_TIERS,
        fixedTier = null,
        initialProgressiveTier = PreviewBadgeTier.MM_16,
        iconKind = CustomBadgeIconKind.TV_BINGE,
        categoryLabel = "SERIE TV COMPLETE",
        defaultDescription = "Dall'episodio pilota fino ai titoli di coda dell'ultima stagione.",
        progressiveSteps = mapOf(
            PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 3, "Hai completato 3 serie TV.", "3 serie"),
            PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 5, "5 serie TV interamente completate. Circuito d'autore conquistato.", "5 serie"),
            PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 10, "10 serie TV viste dalla prima all'ultima puntata.", "10 serie"),
            PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 25, "25 serie TV concluse. Una dedizione monumentale.", "25 serie"),
            PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 50, "50 serie TV completate al 100%. Maratoneta definitivo.", "50 serie")
        ),
        progressCurrent = 5,
        progressTarget = 5,
        progressUnit = "serie",
        rarityPercent = "14.8%"
    ),
    // 4. Progressivo: Ore di visione
    PreviewBadgeItem(
        id = "badge_time",
        title = "Odissea del Tempo",
        category = BadgeTypeCategory.PROGRESSIVE_TIERS,
        fixedTier = null,
        initialProgressiveTier = PreviewBadgeTier.MM_70,
        iconKind = CustomBadgeIconKind.TIME_ODYSSEY,
        categoryLabel = "ORE DI VISIONE",
        defaultDescription = "Un viaggio monumentale nel tempo cinematografico.",
        progressiveSteps = mapOf(
            PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 10, "10 ore di visione cinematografica.", "10 ore"),
            PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 50, "50 ore davanti al grande schermo.", "50 ore"),
            PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 100, "100 ore di proiezioni accumulate.", "100 ore"),
            PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 250, "250 ore trascorse in sala buia.", "250 ore"),
            PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 500, "Oltre 500 ore di puro cinema. L'Odissea è compiuta.", "500+ ore")
        ),
        progressCurrent = 250,
        progressTarget = 250,
        progressUnit = "ore",
        rarityPercent = "3.1%"
    ),
    // 5. Lost Reel: Giorno della Marmotta
    PreviewBadgeItem(
        id = "secret_groundhog_day",
        title = "Giorno della Marmotta",
        category = BadgeTypeCategory.LOST_REEL_SECRET,
        fixedTier = PreviewBadgeTier.LOST_REEL,
        iconKind = CustomBadgeIconKind.GROUNDHOG_LOOP,
        categoryLabel = "LOST REEL #02",
        defaultDescription = "Metti la sveglia alle 6:00, Sonny e Cher stanno suonando... Hai guardato due volte lo stesso film in 48 ore.",
        secretHint = "Metti la sveglia alle 6:00, Sonny e Cher stanno suonando...",
        rarityPercent = "2.9%"
    ),
    // 6. Lost Reel: Roulette del Fato
    PreviewBadgeItem(
        id = "secret_surprise_fate",
        title = "Roulette del Fato",
        category = BadgeTypeCategory.LOST_REEL_SECRET,
        fixedTier = PreviewBadgeTier.LOST_REEL,
        iconKind = CustomBadgeIconKind.ROULETTE_FATE,
        categoryLabel = "LOST REEL #05",
        defaultDescription = "Affidati al caso per la tua prossima avventura: hai visto un film scoperto scuotendo lo smartphone.",
        secretHint = "Affidati al caso per la tua prossima avventura cinematografica...",
        rarityPercent = "8.5%"
    )
)

// ════════════════════════════════════════════════════════════════════
// 1. EMBLEMA CINEMATOGRAFICO SU MISURA FLICKTROVE
// Purezza cromatica: nessun doppio colore ibrido. Il metallo satinato
// adotta esclusivamente la palette tonale del Tier con riflesso speculare naturale.
// ════════════════════════════════════════════════════════════════════

@Composable
fun FlickTroveBadgeEmblem(
    tier: PreviewBadgeTier,
    iconKind: CustomBadgeIconKind,
    isUnlocked: Boolean = true,
    progressFraction: Float = 1f,
    showProgressRing: Boolean = false,
    size: Dp = 92.dp,
    enableInteractiveParallax: Boolean = size >= 90.dp,
    enablePeriodicGleam: Boolean = true,
    modifier: Modifier = Modifier
) {
    val tierColor by animateColorAsState(
        targetValue = if (isUnlocked) tier.primaryColor else Color(0xFF64748B),
        animationSpec = tween(300),
        label = "EmblemColor"
    )

    val glowColor by animateColorAsState(
        targetValue = if (isUnlocked) tier.glowColor else Color.Transparent,
        animationSpec = tween(300),
        label = "EmblemGlow"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "EmblemProgress"
    )

    // ── 1. EFFETTO LUCCICHIO PERIODICO (Gleam calmo e raro ogni ~9 secondi) ──
    val shimmerDelayMs = remember(iconKind, tier) {
        ((iconKind.hashCode() and 0x7FFFFFFF) % 4000)
    }
    val infiniteTransition = rememberInfiniteTransition(label = "EmblemInfiniteTransition")
    val gleamProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 9000
                0.0f at 0 using LinearOutSlowInEasing
                1.0f at 1300 using FastOutSlowInEasing
                1.0f at 9000
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(shimmerDelayMs)
        ),
        label = "GleamProgress"
    )

    // ── 2. EFFETTO PARALLASSE TATTILE 3D COL DITO ──
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }

    val touchModifier = if (enableInteractiveParallax) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                VibrationHelper.vibrateTick(context)
                val halfW = this.size.width / 2f
                val halfH = this.size.height / 2f
                val startNormX = if (halfW > 0f) ((down.position.x - halfW) / halfW).coerceIn(-1.2f, 1.2f) else 0f
                val startNormY = if (halfH > 0f) ((down.position.y - halfH) / halfH).coerceIn(-1.2f, 1.2f) else 0f
                coroutineScope.launch {
                    tiltX.animateTo(startNormX, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
                    tiltY.animateTo(startNormY, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
                }

                var pointerId = down.id
                while (true) {
                    val event = awaitPointerEvent()
                    val currentPointer = event.changes.firstOrNull { it.id == pointerId } ?: break
                    if (!currentPointer.pressed) break
                    currentPointer.consume()
                    val normX = if (halfW > 0f) ((currentPointer.position.x - halfW) / halfW).coerceIn(-1.2f, 1.2f) else 0f
                    val normY = if (halfH > 0f) ((currentPointer.position.y - halfH) / halfH).coerceIn(-1.2f, 1.2f) else 0f
                    coroutineScope.launch {
                        tiltX.snapTo(normX)
                        tiltY.snapTo(normY)
                    }
                }

                // Rilascio del tocco: ritorno elastico armonico alla posizione neutrale
                coroutineScope.launch {
                    launch {
                        tiltX.animateTo(
                            0f,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    launch {
                        tiltY.animateTo(
                            0f,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(touchModifier)
            .graphicsLayer {
                rotationY = tiltX.value * 14f
                rotationX = -tiltY.value * 14f
                cameraDistance = 16f * density
            },
        contentAlignment = Alignment.Center
    ) {
        // A. BACKGROUND CANVAS (Bagliore Neon, Lente Dark, Tacche ottiche, Anelli e Traccia Progresso)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = (this.size.minDimension / 2f)

            // 1. Bagliore Neon Ambilight volumetrico coerente
            if (isUnlocked) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor,
                            glowColor.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius * 1.18f
                    ),
                    radius = radius * 1.15f,
                    center = center
                )
            }

            // Dimensioni proporzionate in base alla presenza del progress ring esterno
            val lensRadius = if (showProgressRing) radius * 0.92f else radius * 0.88f
            val tickInnerR = if (showProgressRing) radius * 0.72f else radius * 0.82f
            val tickOuterR = if (showProgressRing) radius * 0.78f else radius * 0.88f
            val innerRingR = if (showProgressRing) radius * 0.60f else radius * 0.68f

            // 0. Ombra di contatto e sopraelevazione sotto la ghiera esterna (Drop Shadow verso il fondo)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.80f),
                        Color.Black.copy(alpha = 0.40f),
                        Color.Black.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(center.x, center.y + radius * 0.045f),
                    radius = lensRadius * 1.10f
                ),
                radius = lensRadius * 1.10f,
                center = Offset(center.x, center.y + radius * 0.045f)
            )

            // 2. Base satinata della lente (dark cinema profondo e uniforme su tutta la superficie)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isUnlocked) {
                        listOf(
                            Color(0xFF0F1118),
                            Color(0xFF0A0C12),
                            Color(0xFF06070B)
                        )
                    } else {
                        listOf(
                            Color(0xFF13151D),
                            Color(0xFF0B0C12)
                        )
                    },
                    center = center,
                    radius = lensRadius
                ),
                radius = lensRadius,
                center = center
            )

            // 3. Tacche ottiche graduate a telemetro cinematografico
            val tickCount = 24
            val tickColor = if (isUnlocked) tierColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)

            for (i in 0 until tickCount) {
                val angle = Math.toRadians((i * (360.0 / tickCount)))
                val startX = center.x + (tickInnerR * cos(angle)).toFloat()
                val startY = center.y + (tickInnerR * sin(angle)).toFloat()
                val endX = center.x + (tickOuterR * cos(angle)).toFloat()
                val endY = center.y + (tickOuterR * sin(angle)).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = radius * 0.018f
                )
            }

            // 4. Anello concentrico di rifinitura della lente
            drawCircle(
                color = if (isUnlocked) tierColor.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.08f),
                radius = innerRingR,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = radius * 0.02f
                )
            )

            // 5. PROGRESS PILL CIRCOLARE INTORNO AL BADGE (O anello esterno standard)
            if (showProgressRing) {
                val ringRadius = radius * 0.92f
                val ringStroke = (radius * 0.065f).coerceAtLeast(2.5.dp.toPx())

                // Traccia satinata di sfondo scuro
                drawCircle(
                    color = Color.White.copy(alpha = 0.09f),
                    radius = ringRadius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = ringStroke
                    )
                )

                // Pillola di progresso curva con estremità arrotondate (StrokeCap.Round)
                if (animatedProgress >= 0.999f) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                tierColor,
                                tier.secondaryColor,
                                Color.White.copy(alpha = 0.90f),
                                tierColor,
                                tier.secondaryColor,
                                tierColor
                            ),
                            center = center
                        ),
                        radius = ringRadius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = ringStroke
                        )
                    )
                } else if (animatedProgress > 0f) {
                    val sweep = 360f * animatedProgress
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                tierColor,
                                tier.secondaryColor,
                                tierColor
                            ),
                            center = center
                        ),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = ringStroke,
                            cap = StrokeCap.Round
                        )
                    )
                }
            } else {
                // Anello concentrico standard a 360° per le viste senza track circolare
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = if (isUnlocked) {
                            listOf(
                                tierColor,
                                tier.secondaryColor,
                                Color.White.copy(alpha = 0.85f),
                                tierColor,
                                tier.secondaryColor,
                                tierColor
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.05f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        },
                        center = center
                    ),
                    radius = radius * 0.88f,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = radius * 0.045f
                    )
                )
            }
        }

        // B. AREA CENTRALE DELLA LENTE CON ICONA IN SECONDO PIANO (EFFETTO PARALLASSE IN PROFONDITÀ)
        val innerCircleSize = if (showProgressRing) size * 0.60f else size * 0.68f
        val standardIconSize = if (showProgressRing) size * 0.46f else size * 0.52f

        // Solo queste 3 icone specifiche (Scenografo Personale, Notte delle Ombre, Produttore Esecutivo)
        // vengono ingrandite ulteriormente a 1.85x del diametro della lente per riempire il cerchio ed essere croppate
        val isSpecialLargeIcon = isUnlocked && (
            iconKind.iconRes == R.drawable.ic_badge_light_projector ||
            iconKind.iconRes == R.drawable.ic_badge_screaming ||
            iconKind.iconRes == R.drawable.ic_badge_shaking_hands
        )

        val currentIconSize = if (isSpecialLargeIcon) {
            innerCircleSize * 1.15f
        } else {
            standardIconSize
        }

        // Sposta solo queste icone un pochetto più in alto per bilanciare la base
        val isShiftedUpIcon = isUnlocked && (
            iconKind.iconRes == R.drawable.ic_badge_hot_meal ||
            iconKind.iconRes == R.drawable.ic_badge_star_altar ||
            iconKind.iconRes == R.drawable.ic_badge_tv ||
            iconKind.iconRes == R.drawable.ic_badge_magnifying_glass ||
            iconKind.iconRes == R.drawable.ic_badge_film_projector ||
            iconKind.iconRes == R.drawable.ic_badge_scales ||
            iconKind.iconRes == R.drawable.ic_badge_acoustic_megaphone ||
            iconKind.iconRes == R.drawable.ic_badge_lantern_flame
        )

        // Abbassa la medaglia con stella (La Giuria) che ha la barra orizzontale larga in cima
        val isShiftedDownIcon = isUnlocked && (
            iconKind.iconRes == R.drawable.ic_badge_star_medal
        )

        val iconOffsetY = when {
            isShiftedUpIcon -> -size * 0.032f
            isShiftedDownIcon -> size * 0.028f
            else -> 0.dp
        }

        // Parallasse dell'icona in secondo piano (l'icona si muove in senso opposto al tilt per simulare profondità sotto il cristallo)
        val maxParallaxDistance = innerCircleSize * 0.14f
        val parallaxX = maxParallaxDistance * (-tiltX.value)
        val parallaxY = maxParallaxDistance * (-tiltY.value)

        Box(
            modifier = Modifier
                .size(innerCircleSize)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // ICONA RECESSA NEL SECONDO PIANO (Tagliata con precisione alla circonferenza interna della lente)
            if (isUnlocked) {
                Icon(
                    painter = painterResource(id = iconKind.iconRes),
                    contentDescription = null,
                    tint = tierColor,
                    modifier = Modifier
                        .offset(x = parallaxX, y = iconOffsetY + parallaxY)
                        .wrapContentSize(align = Alignment.Center, unbounded = true)
                        .requiredSize(currentIconSize)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = "Bloccato",
                    tint = if (tier == PreviewBadgeTier.LOST_REEL) Color(0xFF818CF8).copy(alpha = 0.75f) else Color(0xFF64748B),
                    modifier = Modifier
                        .offset(x = parallaxX * 0.7f, y = parallaxY * 0.7f)
                        .size(standardIconSize * 0.82f)
                )
            }
        }

        // C. FOREGROUND GLASS CANVAS: OMBRE DI RILIEVO E LUCCICHIO SUL VETRO
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = (this.size.minDimension / 2f)
            val lensRadius = if (showProgressRing) radius * 0.92f else radius * 0.88f
            val innerRingR = if (showProgressRing) radius * 0.60f else radius * 0.68f

            // 1. Ombra di incasso (Bezel Inner Shadow): la ghiera esterna con le tacche, essendo sopraelevata,
            // proietta un'ombra interna morbida e profonda lungo il perimetro dell'oblò,
            // facendo risaltare la parte esterna come fisicamente più alta e l'icona incassata in profondità.
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color.Transparent,
                    0.45f to Color.Transparent,
                    0.68f to Color.Black.copy(alpha = 0.30f),
                    0.86f to Color.Black.copy(alpha = 0.68f),
                    0.96f to Color.Black.copy(alpha = 0.88f),
                    1.0f to Color.Black.copy(alpha = 0.96f),
                    center = center,
                    radius = innerRingR
                ),
                radius = innerRingR,
                center = center
            )

            val glassPath = Path().apply {
                addOval(
                    Rect(
                        center.x - lensRadius,
                        center.y - lensRadius,
                        center.x + lensRadius,
                        center.y + lensRadius
                    )
                )
            }

            clipPath(glassPath) {
                // Luccichio Periodico a Spazzamento Diagonale sul Vetro (ogni ~9s)
                if (enablePeriodicGleam && gleamProgress in 0.001f..0.999f) {
                    val sweepMin = -lensRadius * 1.6f
                    val sweepMax = lensRadius * 1.6f
                    val currentSweep = sweepMin + (sweepMax - sweepMin) * gleamProgress

                    // Vettore diagonale a 45° (da in alto a sinistra a in basso a destra)
                    val dirX = 0.7071f
                    val dirY = 0.7071f
                    val beamHalfWidth = lensRadius * 0.35f

                    val startSweep = Offset(
                        center.x + dirX * (currentSweep - beamHalfWidth),
                        center.y + dirY * (currentSweep - beamHalfWidth)
                    )
                    val endSweep = Offset(
                        center.x + dirX * (currentSweep + beamHalfWidth),
                        center.y + dirY * (currentSweep + beamHalfWidth)
                    )

                    // Riflesso satinato e semitrasparente: lascia visibile l'icona sul fondo senza coprirla con bianco opaco
                    val beamColors = if (isUnlocked) {
                        listOf(
                            Color.Transparent,
                            tierColor.copy(alpha = 0.04f),
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.24f),
                            Color.White.copy(alpha = 0.12f),
                            tierColor.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    } else {
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.03f),
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    }

                    drawRect(
                        brush = Brush.linearGradient(
                            colors = beamColors,
                            start = startSweep,
                            end = endSweep
                        ),
                        size = this.size
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// 2. TIMELINE EVOLUTIVA DEI 5 TIERS (SPUNTA TICK SU TUTTI I NODI CONQUISTATI)
// ════════════════════════════════════════════════════════════════════

@Composable
fun BadgeTierEvolutionTrack(
    currentTier: PreviewBadgeTier,
    isUnlocked: Boolean = true,
    progressCurrent: Int = 0,
    maxTarget: Int = 0,
    stepProgressFraction: Float = 0f,
    tierSteps: List<PreviewBadgeTier> = listOf(
        PreviewBadgeTier.SUPER_8,
        PreviewBadgeTier.MM_16,
        PreviewBadgeTier.MM_35,
        PreviewBadgeTier.MM_70,
        PreviewBadgeTier.THE_FINAL_CUT
    ),
    modifier: Modifier = Modifier
) {
    val currentOrdinal = tierSteps.indexOf(currentTier).takeIf { it >= 0 } ?: 0
    val isCompletedAll = isUnlocked && currentOrdinal == tierSteps.lastIndex

    val animatedStepProgress by animateFloatAsState(
        targetValue = stepProgressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "StepLineProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(vertical = 14.dp, horizontal = 16.dp)
    ) {
        // Intestazione con percentuale sul massimo o badge COMPLETATO
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.trophy_track_title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = Color.White.copy(alpha = 0.50f)
            )

            if (isCompletedAll) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(currentTier.primaryColor.copy(alpha = 0.18f))
                        .border(1.dp, currentTier.primaryColor, CircleShape)
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.trophy_completed_badge),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        ),
                        color = currentTier.primaryColor
                    )
                }
            } else {
                val percent = if (maxTarget > 0) {
                    ((progressCurrent.toFloat() / maxTarget) * 100).toInt().coerceIn(0, 100)
                } else {
                    if (isUnlocked) (((currentOrdinal + 1) / tierSteps.size.toFloat()) * 100).toInt().coerceIn(0, 100) else 0
                }
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = if (isUnlocked) currentTier.primaryColor else Color.White.copy(alpha = 0.50f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // STRUTTURA ATOMICA CON ALLINEAMENTO PERFETTO NODI-TESTI
        val stepCount = tierSteps.size
        Box(modifier = Modifier.fillMaxWidth()) {
            if (stepCount > 1) {
                // Linee di connessione disegnate sui centri matematici delle colonne
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    val colW = size.width / stepCount.toFloat()
                    val strokeH = 2.5.dp.toPx()
                    val y = size.height / 2f

                    for (i in 0 until (stepCount - 1)) {
                        val x1 = colW * (i + 0.5f)
                        val x2 = colW * (i + 1.5f)

                        // 1. Traccia scura satinata di sfondo
                        drawLine(
                            color = Color.White.copy(alpha = 0.10f),
                            start = Offset(x1, y),
                            end = Offset(x2, y),
                            strokeWidth = strokeH,
                            cap = StrokeCap.Round
                        )

                        // 2. Tratto colorato in proporzione all'avanzamento
                        val segmentFraction = when {
                            !isUnlocked -> 0f
                            isCompletedAll || i < currentOrdinal -> 1f
                            i == currentOrdinal -> animatedStepProgress
                            else -> 0f
                        }

                        if (segmentFraction > 0f) {
                            val activeX2 = x1 + (x2 - x1) * segmentFraction
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    listOf(tierSteps[i].primaryColor, tierSteps[i + 1].primaryColor),
                                    startX = x1,
                                    endX = x2
                                ),
                                start = Offset(x1, y),
                                end = Offset(activeX2, y),
                                strokeWidth = strokeH,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // Colonne centrate: ciascuna contiene il nodo e la sua etichetta sullo stesso asse verticale X
            Row(modifier = Modifier.fillMaxWidth()) {
                tierSteps.forEachIndexed { index, stepTier ->
                    val isNodeAchieved = isUnlocked && index <= currentOrdinal
                    val isCurrent = isUnlocked && index == currentOrdinal

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Nodo da 24.dp
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 24.dp else 16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isNodeAchieved -> stepTier.primaryColor
                                            else -> Color.White.copy(alpha = 0.08f)
                                        }
                                    )
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = when {
                                            isCurrent -> Color.White
                                            isNodeAchieved -> stepTier.primaryColor
                                            else -> Color.White.copy(alpha = 0.15f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isNodeAchieved) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick),
                                        contentDescription = null,
                                        tint = if (stepTier == PreviewBadgeTier.THE_FINAL_CUT) Color.White else Color.Black,
                                        modifier = Modifier.size(if (isCurrent) 11.dp else 9.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Etichetta del formato rigorosamente centrata sotto il proprio cerchio
                        Text(
                            text = stepTier.shortLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.8.sp,
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            ),
                            color = when {
                                isCurrent -> stepTier.primaryColor
                                isNodeAchieved -> Color.White.copy(alpha = 0.90f)
                                else -> Color.White.copy(alpha = 0.40f)
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// 3. CARD DA VETRINA PER LA GRIGLIA TROFEI (TROPHY SHOWCASE)
// ════════════════════════════════════════════════════════════════════

@Composable
fun TrophyBadgeCard(
    badge: PreviewBadgeItem,
    tier: PreviewBadgeTier,
    isUnlocked: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = if (isUnlocked) {
        tier.primaryColor.copy(alpha = 0.32f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }

    val isProgressive = badge.category == BadgeTypeCategory.PROGRESSIVE_TIERS
    val isMastered = isProgressive && tier == PreviewBadgeTier.THE_FINAL_CUT && isUnlocked

    // Il target visualizzato e la barra di avanzamento puntano SEMPRE al prossimo formato da conquistare
    val nextTier = when (tier) {
        PreviewBadgeTier.SUPER_8 -> PreviewBadgeTier.MM_16
        PreviewBadgeTier.MM_16 -> PreviewBadgeTier.MM_35
        PreviewBadgeTier.MM_35 -> PreviewBadgeTier.MM_70
        PreviewBadgeTier.MM_70 -> PreviewBadgeTier.THE_FINAL_CUT
        else -> null
    }

    val targetThreshold = if (isUnlocked && nextTier != null) {
        badge.progressiveSteps[nextTier]?.targetThreshold ?: badge.progressTarget
    } else {
        badge.progressiveSteps[tier]?.targetThreshold ?: badge.progressTarget
    }

    val progressFraction = when {
        !isUnlocked -> 0f
        isMastered -> 1f
        isProgressive && targetThreshold > 0 -> (badge.progressCurrent.toFloat() / targetThreshold).coerceIn(0f, 1f)
        else -> 1f
    }

    Box(
        modifier = modifier
            .widthIn(min = 104.dp)
            .height(148.dp)
            .bounceClick { onClick() }
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 6.dp, vertical = 10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // Emblema Cinematico FlickTrove con Progress Pill circolare integrata
            FlickTroveBadgeEmblem(
                tier = tier,
                iconKind = badge.iconKind,
                isUnlocked = isUnlocked,
                progressFraction = progressFraction,
                showProgressRing = true,
                size = 66.dp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Slot fisso per il contatore x/n SOPRA AL TITOLO (15.dp):
            // garantisce perfetta simmetria e la stessa identica altezza su tutte le card della griglia.
            val unitStr = badge.localizedUnit()
            val progressCountText = if (isProgressive && !isMastered) {
                if (unitStr.isNotBlank()) {
                    "${badge.progressCurrent} / $targetThreshold $unitStr".trim()
                } else {
                    "${badge.progressCurrent} / $targetThreshold"
                }
            } else {
                null
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(15.dp),
                contentAlignment = Alignment.Center
            ) {
                if (progressCountText != null) {
                    Text(
                        text = progressCountText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = if (isUnlocked) FontWeight.SemiBold else FontWeight.Medium,
                            letterSpacing = 0.2.sp
                        ),
                        color = if (isUnlocked) tier.primaryColor else Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Titolo Trofeo con altezza fissa (30.dp) per allineare a registro tutte le card (1 o 2 righe)
            // Padding orizzontale di 8.dp per garantire margine laterale elegante e mandare a capo prima i titoli lunghi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = badge.localizedTitle(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        lineHeight = 13.5.sp
                    ),
                    color = if (isUnlocked) Color.White else Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// 4. VISTA DETTAGLIO HERO & CONDIVISIONE STORY (9:16)
// ════════════════════════════════════════════════════════════════════

@Composable
fun BadgeHeroDetailShowcase(
    badge: PreviewBadgeItem,
    tier: PreviewBadgeTier,
    isUnlocked: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isProgressive = badge.category == BadgeTypeCategory.PROGRESSIVE_TIERS
    val isMastered = isProgressive && tier == PreviewBadgeTier.THE_FINAL_CUT && isUnlocked

    val activeDescription = if (!isUnlocked && badge.secretHint != null) {
        badge.localizedSecretHint() ?: badge.localizedDescription()
    } else {
        badge.localizedDescription()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF151822),
                        Color(0xFF0D0F15)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        tier.primaryColor.copy(alpha = 0.45f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(22.dp)
    ) {
        // Tag di rarità globale
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(tier.primaryColor.copy(alpha = 0.10f))
                .border(
                    width = 1.dp,
                    color = tier.primaryColor.copy(alpha = 0.35f),
                    shape = CircleShape
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.trophy_rarity_pct, badge.rarityPercent).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp
                ),
                color = tier.primaryColor
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Emblema Grande Cine-Lens: PUREZZA CROMATICA SENZA DOPPI COLORI IBRIDI
        FlickTroveBadgeEmblem(
            tier = tier,
            iconKind = badge.iconKind,
            isUnlocked = isUnlocked,
            size = 130.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Formato e nome tier
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(tier.primaryColor)
            )
            Text(
                text = tier.localizedFormatName().uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp
                ),
                color = tier.primaryColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Titolo Grande Trofeo
        Text(
            text = badge.localizedTitle(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.3.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Descrizione d'onore cinefila dinamica
        Text(
            text = activeDescription,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 19.sp
            ),
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Timeline o Sigillo
        when (badge.category) {
            BadgeTypeCategory.PROGRESSIVE_TIERS -> {
                val nextTier = when (tier) {
                    PreviewBadgeTier.SUPER_8 -> PreviewBadgeTier.MM_16
                    PreviewBadgeTier.MM_16 -> PreviewBadgeTier.MM_35
                    PreviewBadgeTier.MM_35 -> PreviewBadgeTier.MM_70
                    PreviewBadgeTier.MM_70 -> PreviewBadgeTier.THE_FINAL_CUT
                    else -> null
                }
                val nextTarget = nextTier?.let { badge.progressiveSteps[it]?.targetThreshold }
                val stepFraction = if (nextTarget != null && nextTarget > 0) {
                    (badge.progressCurrent.toFloat() / nextTarget).coerceIn(0f, 1f)
                } else if (isUnlocked && tier == PreviewBadgeTier.THE_FINAL_CUT) 1f else 0f

                val maxStepTarget = badge.progressiveSteps[PreviewBadgeTier.THE_FINAL_CUT]?.targetThreshold ?: badge.progressTarget
                BadgeTierEvolutionTrack(
                    currentTier = tier,
                    isUnlocked = isUnlocked,
                    progressCurrent = badge.progressCurrent,
                    maxTarget = maxStepTarget,
                    stepProgressFraction = stepFraction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            BadgeTypeCategory.DAY_ONE_HONOR -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(tier.primaryColor.copy(alpha = 0.10f))
                        .border(1.dp, tier.primaryColor.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ONORIFICENZA STORICA DEI FONDATORI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.4.sp
                            ),
                            color = tier.primaryColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Traguardo unico non replicabile • Registrato prima del 29 Agosto 2026",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            BadgeTypeCategory.SPECIAL_ACHIEVEMENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(tier.primaryColor.copy(alpha = 0.10f))
                        .border(1.dp, tier.primaryColor.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TRAGUARDO CINEMATOGRAFICO D'AUTORE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.4.sp
                            ),
                            color = tier.primaryColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Formato d'eccellenza ${tier.formatName} • Riconoscimento speciale non progressivo",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            BadgeTypeCategory.LOST_REEL_SECRET -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.30f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BOBINA PERDUTA • TRAGUARDO SEGRETO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.4.sp
                            ),
                            color = Color(0xFFA78BFA)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isUnlocked) "Easter Egg scoperto! Condividilo con la community" else "Indizio misterioso celato tra le pieghe della cineteca...",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// 5. HUB COMPLETO DI ANTEPRIMA & SELEZIONE BADGE
// ════════════════════════════════════════════════════════════════════

@Composable
fun BadgePreviewShowcase() {
    var selectedBadge by remember { mutableStateOf(OFFICIAL_BADGES_CATALOG[0]) }
    var selectedProgressiveTier by remember { mutableStateOf(PreviewBadgeTier.THE_FINAL_CUT) }
    var isUnlocked by remember { mutableStateOf(true) }

    val activeTier = selectedBadge.fixedTier ?: selectedProgressiveTier

    Surface(
        color = Color(0xFF090B10),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            // Header
            Text(
                text = "FLICKTROVE • TROPHY SHOWCASE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = Color(0xFF2DD4BF)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Anteprima Badge & Trofei",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black
                ),
                color = Color.White
            )
            Text(
                text = "Catalogo ufficiale con traguardi progressivi e trofei unici / Lost Reel.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // SELETTORE DEL TIPO DI BADGE
            Text(
                text = "SCEGLI UN BADGE DAL CATALOGO:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OFFICIAL_BADGES_CATALOG.forEach { badgeItem ->
                    val isSelected = badgeItem.id == selectedBadge.id
                    Box(
                        modifier = Modifier
                            .bounceClick {
                                selectedBadge = badgeItem
                                if (badgeItem.fixedTier == null) {
                                    selectedProgressiveTier = badgeItem.initialProgressiveTier
                                }
                            }
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF2DD4BF).copy(alpha = 0.18f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF2DD4BF) else Color.White.copy(alpha = 0.10f),
                                shape = CircleShape
                            )
                            .padding(horizontal = 13.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = badgeItem.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.5.sp
                            ),
                            color = if (isSelected) Color(0xFF2DD4BF) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SELETTORE DEL FORMATO TIER PER I PROGRESSIVI
            if (selectedBadge.category == BadgeTypeCategory.PROGRESSIVE_TIERS) {
                Text(
                    text = "CAMBIA FORMATO DEL BADGE PROGRESSIVO:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val progressiveTiers = listOf(
                    PreviewBadgeTier.SUPER_8,
                    PreviewBadgeTier.MM_16,
                    PreviewBadgeTier.MM_35,
                    PreviewBadgeTier.MM_70,
                    PreviewBadgeTier.THE_FINAL_CUT
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    progressiveTiers.forEach { tier ->
                        val isCurrent = tier == selectedProgressiveTier
                        Box(
                            modifier = Modifier
                                .bounceClick { selectedProgressiveTier = tier }
                                .clip(CircleShape)
                                .background(
                                    if (isCurrent) tier.primaryColor.copy(alpha = 0.22f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                    color = if (isCurrent) tier.primaryColor else Color.White.copy(alpha = 0.12f),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 13.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tier.shortLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = if (isCurrent) tier.primaryColor else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (selectedBadge.category == BadgeTypeCategory.LOST_REEL_SECRET)
                            "🗝️ Badge Segreto: Formato fisso Lost Reel (Nessuna progressione a livelli)"
                        else
                            "✨ Traguardo Storico: Formato d'onore The Final Cut (Non ha livelli intermedi)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF94A3B8)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // TOGGLE SBLOCCATO / BLOCCATO
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .bounceClick { isUnlocked = true }
                        .clip(CircleShape)
                        .background(if (isUnlocked) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Transparent)
                        .border(1.dp, if (isUnlocked) Color(0xFF10B981) else Color.White.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "SBLOCCATO ✨",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isUnlocked) Color(0xFF10B981) else Color(0xFF94A3B8)
                    )
                }

                Box(
                    modifier = Modifier
                        .bounceClick { isUnlocked = false }
                        .clip(CircleShape)
                        .background(if (!isUnlocked) Color(0xFF64748B).copy(alpha = 0.2f) else Color.Transparent)
                        .border(1.dp, if (!isUnlocked) Color(0xFF64748B) else Color.White.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "BLOCCATO 🔒",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (!isUnlocked) Color(0xFFE2E8F0) else Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 1. VISTA DETTAGLIO HERO & STORY 9:16
            Text(
                text = "1. VISTA DETTAGLIO & CONDIVISIONE STORY (9:16)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = Color(0xFFE2E8F0)
            )
            Spacer(modifier = Modifier.height(10.dp))

            BadgeHeroDetailShowcase(
                badge = selectedBadge,
                tier = activeTier,
                isUnlocked = isUnlocked
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 2. CARD GRIGLIA TROFEI (SALA DEI TROFEI)
            Text(
                text = "2. CARD GRIGLIA TROFEI (VARI TIPI A CONFRONTO)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = Color(0xFFE2E8F0)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OFFICIAL_BADGES_CATALOG.forEach { sampleItem ->
                    val itemTier = sampleItem.fixedTier ?: sampleItem.initialProgressiveTier
                    TrophyBadgeCard(
                        badge = sampleItem,
                        tier = itemTier,
                        isUnlocked = sampleItem.id != "secret_groundhog_day" || isUnlocked,
                        onClick = {
                            selectedBadge = sampleItem
                            if (sampleItem.fixedTier == null) {
                                selectedProgressiveTier = sampleItem.initialProgressiveTier
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. PALETTE CROMATICA DEI 6 FORMATI
            Text(
                text = "3. PALETTE CROMATICA DEI 6 FORMATI (COLORI PURI COERENTI)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = Color(0xFFE2E8F0)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PreviewBadgeTier.values().forEach { tierItem ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(98.dp)
                            .bounceClick {
                                if (selectedBadge.category == BadgeTypeCategory.PROGRESSIVE_TIERS && tierItem != PreviewBadgeTier.LOST_REEL) {
                                    selectedProgressiveTier = tierItem
                                }
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF13151F))
                            .border(1.dp, tierItem.primaryColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                            .padding(10.dp)
                    ) {
                        FlickTroveBadgeEmblem(
                            tier = tierItem,
                            iconKind = selectedBadge.iconKind,
                            isUnlocked = true,
                            size = 54.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tierItem.shortLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = tierItem.primaryColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// -------------------------------------------------------------
// PREVIEW ANDROID STUDIO
// -------------------------------------------------------------

@Preview(
    name = "Showcase Completo Trofei FlickTrove",
    showBackground = true,
    backgroundColor = 0xFF090B10,
    device = "spec:width=411dp,height=891dp"
)
@Composable
private fun BadgePreviewShowcaseFullPreview() {
    FlickTroveTheme {
        BadgePreviewShowcase()
    }
}

@Preview(
    name = "Card Frequenza 24fps (The Final Cut - 100% Completato)",
    showBackground = true,
    backgroundColor = 0xFF090B10
)
@Composable
private fun BadgeCardMasteredPreview() {
    FlickTroveTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TrophyBadgeCard(
                badge = OFFICIAL_BADGES_CATALOG[0],
                tier = PreviewBadgeTier.THE_FINAL_CUT,
                isUnlocked = true
            )
        }
    }
}

@Preview(
    name = "Timeline Formati 16mm (Tick sul nodo attivo)",
    showBackground = true,
    backgroundColor = 0xFF090B10
)
@Composable
private fun BadgeTimeline16mmPreview() {
    FlickTroveTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            BadgeTierEvolutionTrack(
                currentTier = PreviewBadgeTier.MM_16,
                isUnlocked = true,
                progressCurrent = 25,
                maxTarget = 250,
                stepProgressFraction = 0.5f
            )
        }
    }
}
