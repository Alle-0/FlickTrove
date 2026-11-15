package com.cinetrack.ui.components.detail

import androidx.compose.ui.graphics.Color
import com.cinetrack.R
import java.util.Locale

internal data class HeaderRatingInfo(
    val code: String,
    val labelRes: Int,
    val descriptionRes: Int,
    val color: Color
)

internal data class ParentsGuideSeverityProfile(
    val sex: SeverityLevel,
    val violence: SeverityLevel,
    val profanity: SeverityLevel,
    val drugs: SeverityLevel,
    val frightening: SeverityLevel
)

/**
 * Single Source of Truth for age certification ratings, color palettes,
 * labels, descriptions, and Parents Guide severity mappings.
 *
 * Uses pre-allocated immutable Sets for zero-allocation O(1) lookups.
 */
internal object AgeRatingResolver {
    // Centralized Color Palette
    val ColorAllAges = Color(0xFF4CAF50)
    val ColorGuidance = Color(0xFF8BC34A)
    val ColorTeen12 = Color(0xFFFF9800)
    val ColorTeen14 = Color(0xFFFF7043)
    val ColorTeen15 = Color(0xFFFF5722)
    val ColorTeen16 = Color(0xFFE64A19)
    val ColorTvMa = Color(0xFFD32F2F)
    val ColorAdult18 = Color(0xFFB71C1C)
    val ColorRestricted17 = Color(0xFFF44336)
    val ColorSpecial = Color(0xFF9E9E9E)
    val ColorUnknown = Color.White

    // Pre-allocated immutable Sets for zero-allocation O(1) lookups
    val ALL_AGES_CERTS = setOf(
        "G", "TV-G", "TV-Y", "U", "T", "0", "FSK 0", "L", "APTA", "0+", "ALL", "SU", "GENEL", "GENEL İZLEYİCİ"
    )

    val GUIDANCE_CERTS = setOf(
        "PG", "TV-PG", "TV-Y7", "TV-Y7-FV", "6", "6+", "FSK 6", "7", "7+", "7A", "K7", "10", "10+",
        "UA", "U/A", "UA 7+", "U/A 7+", "UA7+", "U/A7+", "UA 6+", "U/A 6+", "UA6+", "U/A6+"
    )

    val TEEN_12_CERTS = setOf(
        "PG-13", "PG12", "PG-12", "11", "12", "12+", "12A", "FSK 12", "K12", "13+", "13A",
        "UA 13+", "U/A 13+", "UA13+", "U/A13+", "UA 12+", "U/A 12+", "R-13", "R13", "R12", "R-12"
    )

    val TEEN_14_CERTS = setOf(
        "TV-14", "14", "14+", "14A"
    )

    val TEEN_15_CERTS = setOf(
        "15", "15+", "R15+", "R-15", "R15", "PG15", "PG-15"
    )

    val TEEN_16_CERTS = setOf(
        "16", "16+", "NC16", "NC-16", "VM16", "FSK 16", "K16",
        "UA 16+", "U/A 16+", "UA16+", "U/A16+", "UA 15+", "U/A 15+", "R-16", "R16"
    )

    val ADULT_18_CERTS = setOf(
        "NC-17", "18", "18+", "19", "21+", "FSK 18", "K18", "M18", "R21", "18A", "R-18", "R18", "R18+",
        "UA 18+", "U/A 18+", "UA18+", "U/A18+"
    )

    val RESTRICTED_17_CERTS = setOf(
        "R", "17+", "A", "RESTRICTED"
    )

    /**
     * Resolves the full descriptive rating info (title, description, color) for the media header.
     */
    fun resolveHeaderRatingInfo(certification: String?): HeaderRatingInfo {
        if (certification.isNullOrBlank()) {
            return HeaderRatingInfo("NR", R.string.rated_guide_title, R.string.rated_guide_help, ColorUnknown)
        }
        val clean = certification.trim().uppercase(Locale.ROOT)
        val normalized = clean.replace(" ", "").replace("/", "").replace("-", "")
        return when {
            clean in ALL_AGES_CERTS || normalized in ALL_AGES_CERTS ->
                HeaderRatingInfo(certification, R.string.rating_g_label, R.string.rating_g_desc, ColorAllAges)
            clean in GUIDANCE_CERTS || normalized in GUIDANCE_CERTS ->
                HeaderRatingInfo(certification, R.string.rating_pg_label, R.string.rating_pg_desc, ColorGuidance)
            clean in TEEN_12_CERTS || normalized in TEEN_12_CERTS ->
                HeaderRatingInfo(certification, R.string.rating_pg13_label, R.string.rating_pg13_desc, ColorTeen12)
            clean == "VM12" || normalized == "VM12" ->
                HeaderRatingInfo(certification, R.string.rating_vm12_label, R.string.detail_rating_it_12, ColorTeen12)
            clean == "VM14" || normalized == "VM14" ->
                HeaderRatingInfo(certification, R.string.rating_vm14_label, R.string.detail_rating_it_14, ColorTeen14)
            clean in TEEN_14_CERTS || normalized in TEEN_14_CERTS ->
                HeaderRatingInfo(certification, R.string.rating_tv14_label, R.string.rating_tv14_desc, ColorTeen14)
            clean in TEEN_15_CERTS || normalized in TEEN_15_CERTS ->
                HeaderRatingInfo(certification, R.string.rating_15_label, R.string.rating_15_desc, ColorTeen15)
            clean in TEEN_16_CERTS || normalized in TEEN_16_CERTS ->
                HeaderRatingInfo(certification, R.string.rating_16_label, R.string.rating_16_desc, ColorTeen16)
            clean == "TV-MA" || normalized == "TVMA" ->
                HeaderRatingInfo(certification, R.string.rating_tvma_label, R.string.rating_tvma_desc, ColorTvMa)
            clean == "VM18" || normalized == "VM18" ->
                HeaderRatingInfo(certification, R.string.rating_nc17_label, R.string.detail_rating_it_18, ColorAdult18)
            clean in ADULT_18_CERTS || normalized in ADULT_18_CERTS ->
                HeaderRatingInfo(certification, R.string.rating_nc17_label, R.string.rating_nc17_desc, ColorAdult18)
            clean in RESTRICTED_17_CERTS || normalized in RESTRICTED_17_CERTS ->
                HeaderRatingInfo(certification, R.string.rating_r_label, R.string.rating_r_desc, ColorRestricted17)
            clean == "S" || normalized == "S" ->
                HeaderRatingInfo(certification, R.string.rated_guide_title, R.string.rated_guide_help, ColorSpecial)
            else ->
                HeaderRatingInfo(certification, R.string.rated_guide_title, R.string.rated_guide_help, ColorUnknown)
        }
    }

    /**
     * Resolves the certification badge color strictly from the Single Source of Truth.
     */
    fun getCertificationColor(certification: String?): Color {
        return resolveHeaderRatingInfo(certification).color
    }

    /**
     * Resolves the Parents Guide severity profile based on the age rating tier.
     * Returns null if the rating is for all ages (Guide hidden).
     */
    fun resolveParentsGuideSeverity(cleanCert: String): ParentsGuideSeverityProfile? {
        val clean = cleanCert.trim().uppercase(Locale.ROOT)
        val normalized = clean.replace(" ", "").replace("/", "").replace("-", "")
        if (clean in ALL_AGES_CERTS || normalized in ALL_AGES_CERTS) {
            return null
        }
        return when {
            clean in GUIDANCE_CERTS || normalized in GUIDANCE_CERTS -> {
                ParentsGuideSeverityProfile(
                    sex = SeverityLevel.NONE,
                    violence = SeverityLevel.MODERATE,
                    profanity = SeverityLevel.MILD,
                    drugs = SeverityLevel.NONE,
                    frightening = SeverityLevel.MILD
                )
            }
            clean in TEEN_12_CERTS || normalized in TEEN_12_CERTS ||
            clean in TEEN_14_CERTS || normalized in TEEN_14_CERTS ||
            clean in TEEN_15_CERTS || normalized in TEEN_15_CERTS ||
            clean in setOf("VM12", "VM14", "UA") || normalized in setOf("VM12", "VM14", "UA") -> {
                ParentsGuideSeverityProfile(
                    sex = SeverityLevel.MILD,
                    violence = SeverityLevel.MODERATE,
                    profanity = SeverityLevel.MODERATE,
                    drugs = SeverityLevel.MILD,
                    frightening = SeverityLevel.MODERATE
                )
            }
            clean in RESTRICTED_17_CERTS || normalized in RESTRICTED_17_CERTS ||
            clean in ADULT_18_CERTS || normalized in ADULT_18_CERTS ||
            clean in TEEN_16_CERTS || normalized in TEEN_16_CERTS ||
            clean in setOf("TV-MA", "VM18") || normalized in setOf("TVMA", "VM18") -> {
                ParentsGuideSeverityProfile(
                    sex = SeverityLevel.MODERATE,
                    violence = SeverityLevel.SEVERE,
                    profanity = SeverityLevel.SEVERE,
                    drugs = SeverityLevel.MODERATE,
                    frightening = SeverityLevel.SEVERE
                )
            }
            else -> {
                ParentsGuideSeverityProfile(
                    sex = SeverityLevel.MILD,
                    violence = SeverityLevel.MILD,
                    profanity = SeverityLevel.MILD,
                    drugs = SeverityLevel.NONE,
                    frightening = SeverityLevel.MILD
                )
            }
        }
    }
}

// Package-level delegating helpers for clean backward compatibility
internal fun resolveHeaderRatingInfo(certification: String?): HeaderRatingInfo =
    AgeRatingResolver.resolveHeaderRatingInfo(certification)

internal fun getCertificationColor(certification: String?): Color =
    AgeRatingResolver.getCertificationColor(certification)
