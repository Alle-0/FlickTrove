package com.cinetrack.ui.components.badge

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.cinetrack.R

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

    // Nuovi Badges (6 trofei con icone vettoriali dedicate)
    GENRE_COMEDY(R.drawable.ic_badge_jester_hat),
    GENRE_WESTERN(R.drawable.ic_badge_western_hat),
    TV_NOVEL(R.drawable.ic_badge_vhs),
    DEEP_COLLECTION(R.drawable.ic_badge_book_pile),
    TIME_MACHINE(R.drawable.ic_badge_pocket_watch),
    HARSH_CRITIC(R.drawable.ic_badge_lightning_shout),
    INSTANT_WATCH(R.drawable.ic_badge_middle_arrow),
    DAY_ONE_PREMIERE(R.drawable.ic_badge_theater_curtains),
    FULL_RATING_SPECTRUM(R.drawable.ic_badge_stairs_goal),
    CURRENT_YEAR(R.drawable.ic_badge_newspaper),
    DOUBLE_FEATURE(R.drawable.ic_badge_double_feature),
    HIDDEN_GEM(R.drawable.ic_badge_hidden_gem),
    SHORT_FILMS(R.drawable.ic_badge_short_films),
    TROPHY_HUNTER(R.drawable.ic_badge_trophy_hunter),
    CO_DIRECTOR(R.drawable.ic_badge_co_director),

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
    "secret_night_owl" -> R.string.badge_secret_night_owl_title
    "secret_genreless_rebel" -> R.string.badge_secret_genreless_rebel_title
    "secret_indecisive" -> R.string.badge_secret_indecisive_title
    "secret_completionist" -> R.string.badge_secret_completionist_title
    "secret_genesis" -> R.string.badge_secret_genesis_title
    "secret_iron_heart" -> R.string.badge_secret_iron_heart_title
    "secret_unloved_gem" -> R.string.badge_secret_unloved_gem_title
    "secret_midnight_madness" -> R.string.badge_secret_midnight_madness_title
    "secret_cinephile_streak" -> R.string.badge_secret_cinephile_streak_title
    "day_one_pioneer", "badge_day_one_pioneer" -> R.string.badge_day_one_pioneer_title
    "badge_movies" -> R.string.badge_movies_title
    "badge_tv" -> R.string.badge_tv_title
    "badge_binge_episodes" -> R.string.badge_binge_episodes_title
    "badge_watchlist_hoarder" -> R.string.badge_watchlist_hoarder_title
    "badge_veteran_club" -> R.string.badge_veteran_club_title
    "badge_time" -> R.string.badge_time_title
    "badge_movie_runtime" -> R.string.badge_movie_runtime_title
    "badge_tv_episodes" -> R.string.badge_tv_episodes_title
    "badge_tv_shows" -> R.string.badge_tv_shows_title
    "badge_sagas" -> R.string.badge_sagas_title
    "badge_rewatch" -> R.string.badge_rewatch_title
    "badge_ratings" -> R.string.badge_ratings_title
    "badge_vibes" -> R.string.badge_vibes_title
    "badge_comments" -> R.string.badge_comments_title
    "badge_folders" -> R.string.badge_folders_title
    "genre_horror" -> R.string.badge_genre_horror_title
    "genre_scifi" -> R.string.badge_genre_scifi_title
    "genre_sci_fi" -> R.string.badge_genre_sci_fi_title
    "genre_thriller" -> R.string.badge_genre_thriller_title
    "genre_anime" -> R.string.badge_genre_anime_title
    "genre_comedy" -> R.string.badge_genre_comedy_title
    "genre_animation" -> R.string.badge_genre_animation_title
    "genre_drama" -> R.string.badge_genre_drama_title
    "genre_doc" -> R.string.badge_genre_doc_title
    "genre_documentary" -> R.string.badge_genre_documentary_title
    "genre_action" -> R.string.badge_genre_action_title
    "genre_fantasy" -> R.string.badge_genre_fantasy_title
    "genre_romance" -> R.string.badge_genre_romance_title
    "genre_vintage" -> R.string.badge_genre_vintage_title
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
    "badge_executive_producer" -> R.string.badge_executive_producer_title
    "badge_onboarding_sync" -> R.string.badge_onboarding_sync_title
    "gourmet_critic" -> R.string.badge_gourmet_critic_title
    "colossal" -> R.string.badge_colossal_title
    "actor_muse" -> R.string.badge_actor_muse_title
    "director_heart" -> R.string.badge_director_heart_title
    "secret_harsh_critic" -> R.string.badge_secret_harsh_critic_title
    "genre_western" -> R.string.badge_genre_western_title
    "badge_miniseries" -> R.string.badge_miniseries_title
    "badge_folder_deep" -> R.string.badge_folder_deep_title
    "badge_time_machine" -> R.string.badge_time_machine_title
    "secret_instant_watch" -> R.string.badge_secret_instant_watch_title
    "badge_day_one_premiere", "secret_day_one_premiere" -> R.string.badge_day_one_premiere_title
    "secret_full_rating_spectrum" -> R.string.badge_secret_full_rating_spectrum_title
    "badge_current_year" -> R.string.badge_current_year_title
    "badge_double_feature" -> R.string.badge_double_feature_title
    "badge_hidden_gem" -> R.string.badge_hidden_gem_title
    "badge_short_films" -> R.string.badge_short_films_title
    "badge_trophy_hunter" -> R.string.badge_trophy_hunter_title
    "badge_co_director" -> R.string.badge_co_director_title
    else -> 0
}

fun getBadgeCategoryRes(badgeId: String): Int = when (badgeId) {
    "secret_groundhog_day" -> R.string.badge_secret_groundhog_day_cat
    "secret_surprise_fate" -> R.string.badge_secret_surprise_fate_cat
    "secret_night_owl" -> R.string.badge_secret_night_owl_cat
    "secret_genreless_rebel" -> R.string.badge_secret_genreless_rebel_cat
    "secret_indecisive" -> R.string.badge_secret_indecisive_cat
    "secret_completionist" -> R.string.badge_secret_completionist_cat
    "secret_genesis" -> R.string.badge_secret_genesis_cat
    "secret_iron_heart" -> R.string.badge_secret_iron_heart_cat
    "secret_unloved_gem" -> R.string.badge_secret_unloved_gem_cat
    "secret_midnight_madness" -> R.string.badge_secret_midnight_madness_cat
    "secret_cinephile_streak" -> R.string.badge_secret_cinephile_streak_cat
    "day_one_pioneer", "badge_day_one_pioneer" -> R.string.badge_day_one_pioneer_cat
    "badge_movies" -> R.string.badge_movies_cat
    "badge_tv" -> R.string.badge_tv_cat
    "badge_binge_episodes" -> R.string.badge_binge_episodes_cat
    "badge_watchlist_hoarder" -> R.string.badge_watchlist_hoarder_cat
    "badge_veteran_club" -> R.string.badge_veteran_club_cat
    "badge_time" -> R.string.badge_time_cat
    "badge_movie_runtime" -> R.string.badge_movie_runtime_cat
    "badge_tv_episodes" -> R.string.badge_tv_episodes_cat
    "badge_tv_shows" -> R.string.badge_tv_shows_cat
    "badge_sagas" -> R.string.badge_sagas_cat
    "badge_rewatch" -> R.string.badge_rewatch_cat
    "badge_ratings" -> R.string.badge_ratings_cat
    "badge_vibes" -> R.string.badge_vibes_cat
    "badge_comments" -> R.string.badge_comments_cat
    "badge_folders" -> R.string.badge_folders_cat
    "genre_horror" -> R.string.badge_genre_horror_cat
    "genre_scifi" -> R.string.badge_genre_scifi_cat
    "genre_sci_fi" -> R.string.badge_genre_sci_fi_cat
    "genre_thriller" -> R.string.badge_genre_thriller_cat
    "genre_anime" -> R.string.badge_genre_anime_cat
    "genre_comedy" -> R.string.badge_genre_comedy_cat
    "genre_animation" -> R.string.badge_genre_animation_cat
    "genre_drama" -> R.string.badge_genre_drama_cat
    "genre_doc" -> R.string.badge_genre_doc_cat
    "genre_documentary" -> R.string.badge_genre_documentary_cat
    "genre_action" -> R.string.badge_genre_action_cat
    "genre_fantasy" -> R.string.badge_genre_fantasy_cat
    "genre_romance" -> R.string.badge_genre_romance_cat
    "genre_vintage" -> R.string.badge_genre_vintage_cat
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
    "badge_executive_producer" -> R.string.badge_executive_producer_cat
    "badge_onboarding_sync" -> R.string.badge_onboarding_sync_cat
    "gourmet_critic" -> R.string.badge_gourmet_critic_cat
    "colossal" -> R.string.badge_colossal_cat
    "actor_muse" -> R.string.badge_actor_muse_cat
    "director_heart" -> R.string.badge_director_heart_cat
    "secret_harsh_critic" -> R.string.badge_secret_harsh_critic_cat
    "genre_western" -> R.string.badge_genre_western_cat
    "badge_miniseries" -> R.string.badge_miniseries_cat
    "badge_folder_deep" -> R.string.badge_folder_deep_cat
    "badge_time_machine" -> R.string.badge_time_machine_cat
    "secret_instant_watch" -> R.string.badge_secret_instant_watch_cat
    "badge_day_one_premiere", "secret_day_one_premiere" -> R.string.badge_day_one_premiere_cat
    "secret_full_rating_spectrum" -> R.string.badge_secret_full_rating_spectrum_cat
    "badge_current_year" -> R.string.badge_current_year_cat
    "badge_double_feature" -> R.string.badge_double_feature_cat
    "badge_hidden_gem" -> R.string.badge_hidden_gem_cat
    "badge_short_films" -> R.string.badge_short_films_cat
    "badge_trophy_hunter" -> R.string.badge_trophy_hunter_cat
    "badge_co_director" -> R.string.badge_co_director_cat
    else -> 0
}

fun getBadgeDescriptionRes(badgeId: String): Int = when (badgeId) {
    "secret_groundhog_day" -> R.string.badge_secret_groundhog_day_desc
    "secret_surprise_fate" -> R.string.badge_secret_surprise_fate_desc
    "secret_night_owl" -> R.string.badge_secret_night_owl_desc
    "secret_genreless_rebel" -> R.string.badge_secret_genreless_rebel_desc
    "secret_indecisive" -> R.string.badge_secret_indecisive_desc
    "secret_completionist" -> R.string.badge_secret_completionist_desc
    "secret_genesis" -> R.string.badge_secret_genesis_desc
    "secret_iron_heart" -> R.string.badge_secret_iron_heart_desc
    "secret_unloved_gem" -> R.string.badge_secret_unloved_gem_desc
    "secret_midnight_madness" -> R.string.badge_secret_midnight_madness_desc
    "secret_cinephile_streak" -> R.string.badge_secret_cinephile_streak_desc
    "day_one_pioneer", "badge_day_one_pioneer" -> R.string.badge_day_one_pioneer_desc
    "badge_movies" -> R.string.badge_movies_desc
    "badge_tv" -> R.string.badge_tv_desc
    "badge_binge_episodes" -> R.string.badge_binge_episodes_desc
    "badge_watchlist_hoarder" -> R.string.badge_watchlist_hoarder_desc
    "badge_veteran_club" -> R.string.badge_veteran_club_desc
    "badge_time" -> R.string.badge_time_desc
    "badge_movie_runtime" -> R.string.badge_movie_runtime_desc
    "badge_tv_episodes" -> R.string.badge_tv_episodes_desc
    "badge_tv_shows" -> R.string.badge_tv_shows_desc
    "badge_sagas" -> R.string.badge_sagas_desc
    "badge_rewatch" -> R.string.badge_rewatch_desc
    "badge_ratings" -> R.string.badge_ratings_desc
    "badge_vibes" -> R.string.badge_vibes_desc
    "badge_comments" -> R.string.badge_comments_desc
    "badge_folders" -> R.string.badge_folders_desc
    "genre_horror" -> R.string.badge_genre_horror_desc
    "genre_scifi" -> R.string.badge_genre_scifi_desc
    "genre_sci_fi" -> R.string.badge_genre_sci_fi_desc
    "genre_thriller" -> R.string.badge_genre_thriller_desc
    "genre_anime" -> R.string.badge_genre_anime_desc
    "genre_comedy" -> R.string.badge_genre_comedy_desc
    "genre_animation" -> R.string.badge_genre_animation_desc
    "genre_drama" -> R.string.badge_genre_drama_desc
    "genre_doc" -> R.string.badge_genre_doc_desc
    "genre_documentary" -> R.string.badge_genre_documentary_desc
    "genre_action" -> R.string.badge_genre_action_desc
    "genre_fantasy" -> R.string.badge_genre_fantasy_desc
    "genre_romance" -> R.string.badge_genre_romance_desc
    "genre_vintage" -> R.string.badge_genre_vintage_desc
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
    "badge_executive_producer" -> R.string.badge_executive_producer_desc
    "badge_onboarding_sync" -> R.string.badge_onboarding_sync_desc
    "gourmet_critic" -> R.string.badge_gourmet_critic_desc
    "colossal" -> R.string.badge_colossal_desc
    "actor_muse" -> R.string.badge_actor_muse_desc
    "director_heart" -> R.string.badge_director_heart_desc
    "secret_harsh_critic" -> R.string.badge_secret_harsh_critic_desc
    "genre_western" -> R.string.badge_genre_western_desc
    "badge_miniseries" -> R.string.badge_miniseries_desc
    "badge_folder_deep" -> R.string.badge_folder_deep_desc
    "badge_time_machine" -> R.string.badge_time_machine_desc
    "secret_instant_watch" -> R.string.badge_secret_instant_watch_desc
    "badge_day_one_premiere", "secret_day_one_premiere" -> R.string.badge_day_one_premiere_desc
    "secret_full_rating_spectrum" -> R.string.badge_secret_full_rating_spectrum_desc
    "badge_current_year" -> R.string.badge_current_year_desc
    "badge_double_feature" -> R.string.badge_double_feature_desc
    "badge_hidden_gem" -> R.string.badge_hidden_gem_desc
    "badge_short_films" -> R.string.badge_short_films_desc
    "badge_trophy_hunter" -> R.string.badge_trophy_hunter_desc
    "badge_co_director" -> R.string.badge_co_director_desc
    else -> 0
}

fun getBadgeSecretHintRes(badgeId: String): Int? = when (badgeId) {
    "secret_groundhog_day" -> R.string.badge_secret_groundhog_day_hint
    "secret_surprise_fate" -> R.string.badge_secret_surprise_fate_hint
    "secret_night_owl" -> R.string.badge_secret_night_owl_hint
    "secret_genreless_rebel" -> R.string.badge_secret_genreless_rebel_hint
    "secret_indecisive" -> R.string.badge_secret_indecisive_hint
    "secret_completionist" -> R.string.badge_secret_completionist_hint
    "secret_genesis" -> R.string.badge_secret_genesis_hint
    "secret_iron_heart" -> R.string.badge_secret_iron_heart_hint
    "secret_unloved_gem" -> R.string.badge_secret_unloved_gem_hint
    "secret_midnight_madness" -> R.string.badge_secret_midnight_madness_hint
    "secret_cinephile_streak" -> R.string.badge_secret_cinephile_streak_hint
    "secret_harsh_critic" -> R.string.badge_secret_harsh_critic_hint
    "secret_instant_watch" -> R.string.badge_secret_instant_watch_hint
    "secret_full_rating_spectrum" -> R.string.badge_secret_full_rating_spectrum_hint
    else -> null
}

fun getProgressUnitRes(unit: String): Int? = when (unit.lowercase().trim()) {
    "film", "movies" -> R.string.trophy_unit_movies
    "ore", "hours" -> R.string.trophy_unit_hours
    "episodi", "puntate", "episodes" -> R.string.trophy_unit_episodes
    "serie", "shows", "series" -> R.string.trophy_unit_shows
    "rewatch", "rewatches" -> R.string.trophy_unit_rewatches
    "paesi", "countries", "nazioni", "nations" -> R.string.trophy_unit_countries
    "cartelle", "folders" -> R.string.trophy_unit_folders
    "voti", "ratings" -> R.string.trophy_unit_ratings
    "note", "notes" -> R.string.trophy_unit_notes
    "decenni", "decades" -> R.string.trophy_unit_decades
    "titoli", "titles" -> R.string.trophy_unit_movies
    "gemme", "gems" -> R.string.trophy_unit_gems
    "corti", "shorts" -> R.string.trophy_unit_shorts
    "commenti", "comments" -> R.string.trophy_unit_comments
    "saghe", "sagas" -> R.string.trophy_unit_sagas
    "mesi", "months" -> R.string.trophy_unit_months
    "modifiche", "edits" -> R.string.trophy_unit_edits
    "vibes", "vibe" -> R.string.trophy_unit_vibes
    "elementi", "elements", "items" -> R.string.trophy_unit_items
    "check-in", "checkins", "check-ins" -> R.string.trophy_unit_checkins
    "doc", "documentari", "documentaries" -> R.string.trophy_unit_movies
    "trofei", "trofeo", "trophies", "trophy" -> R.string.trophy_unit_trophies
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
                targetThreshold = 50,
                description = "Il circuito del cinema d'autore. Hai raggiunto quota 50 lungometraggi completati.",
                milestonePhrase = "50 film completati"
            ),
            PreviewBadgeTier.MM_35 to ProgressiveTierDetail(
                tier = PreviewBadgeTier.MM_35,
                targetThreshold = 200,
                description = "Il ritmo costante della sala buia è diventato la tua seconda casa. Hai completato 200 lungometraggi.",
                milestonePhrase = "200 film completati"
            ),
            PreviewBadgeTier.MM_70 to ProgressiveTierDetail(
                tier = PreviewBadgeTier.MM_70,
                targetThreshold = 500,
                description = "Il grande formato epico e le proiezioni prestigiose. Ben 500 lungometraggi vissuti.",
                milestonePhrase = "500 film completati"
            ),
            PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(
                tier = PreviewBadgeTier.THE_FINAL_CUT,
                targetThreshold = 1000,
                description = "L'Opera Compiuta. Oltre 1.000 capolavori impressi nella tua cineteca personale. Hai raggiunto l'apice leggendario della settima arte.",
                milestonePhrase = "1.000+ film • Maestria Assoluta"
            )
        ),
        progressCurrent = 1000,
        progressTarget = 1000,
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
            PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 10, "10 serie TV interamente completate. Circuito d'autore conquistato.", "10 serie"),
            PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 25, "25 serie TV viste dalla prima all'ultima puntata.", "25 serie"),
            PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 50, "50 serie TV concluse. Una dedizione monumentale.", "50 serie"),
            PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 100, "100 serie TV completate al 100%. Maratoneta definitivo.", "100 serie")
        ),
        progressCurrent = 10,
        progressTarget = 10,
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
            PreviewBadgeTier.SUPER_8 to ProgressiveTierDetail(PreviewBadgeTier.SUPER_8, 25, "25 ore di visione cinematografica.", "25 ore"),
            PreviewBadgeTier.MM_16 to ProgressiveTierDetail(PreviewBadgeTier.MM_16, 100, "100 ore davanti al grande schermo.", "100 ore"),
            PreviewBadgeTier.MM_35 to ProgressiveTierDetail(PreviewBadgeTier.MM_35, 350, "350 ore di proiezioni accumulate.", "350 ore"),
            PreviewBadgeTier.MM_70 to ProgressiveTierDetail(PreviewBadgeTier.MM_70, 800, "800 ore trascorse in sala buia.", "800 ore"),
            PreviewBadgeTier.THE_FINAL_CUT to ProgressiveTierDetail(PreviewBadgeTier.THE_FINAL_CUT, 2000, "Oltre 2.000 ore di puro cinema. L'Odissea è compiuta.", "2.000+ ore")
        ),
        progressCurrent = 350,
        progressTarget = 350,
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
