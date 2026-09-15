package com.cinetrack.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.utils.ColorUtils
import java.util.Locale



enum class SeverityLevel(
    val filledBars: Int,
    val labelRes: Int
) {
    NONE(0, R.string.guide_severity_none),
    MILD(1, R.string.guide_severity_mild),
    MODERATE(2, R.string.guide_severity_moderate),
    SEVERE(3, R.string.guide_severity_severe)
}

enum class GuideCategoryType {
    SEX_NUDITY,
    VIOLENCE_GORE,
    PROFANITY,
    ALCOHOL_DRUGS,
    FRIGHTENING
}

data class ParentsGuideCategory(
    val titleRes: Int,
    val type: GuideCategoryType,
    val iconRes: Int,
    val level: SeverityLevel
) {
    val isNone: Boolean
        get() = level == SeverityLevel.NONE
}


data class ParentsGuideData(
    val categories: List<ParentsGuideCategory>
)

object ParentsGuideResolver {
    fun resolve(certification: String?, countryCode: String?): ParentsGuideData? {
        if (certification.isNullOrBlank()) return null
        val cleanCert = certification.trim().uppercase(Locale.ROOT)
        
        // Se il rating è per tutti (G, T, 0+, ecc.), tutte le categorie sarebbero "Nessuno".
        // In questo caso preferiamo nascondere completamente la Guida Genitori.
        if (cleanCert in listOf("G", "TV-G", "TV-Y", "U", "T", "0", "FSK 0", "L", "APTA", "0+", "ALL", "SU", "GENEL", "GENEL İZLEYİCİ")) {
            return null
        }

        val (sex, violence, profanity, drugs, frightening) = when {
            cleanCert in listOf("PG", "TV-PG", "TV-Y7", "TV-Y7-FV", "6", "6+", "FSK 6", "7", "7+", "7A", "10", "10+") -> {
                Tuple5(
                    SeverityLevel.NONE,
                    SeverityLevel.MODERATE,
                    SeverityLevel.MILD,
                    SeverityLevel.NONE,
                    SeverityLevel.MILD
                )
            }
            cleanCert in listOf("PG-13", "PG12", "PG-12", "TV-14", "12", "12+", "12A", "FSK 12", "13+", "13A", "14", "14+", "VM14", "15", "UA") -> {
                Tuple5(
                    SeverityLevel.MILD,
                    SeverityLevel.MODERATE,
                    SeverityLevel.MODERATE,
                    SeverityLevel.MILD,
                    SeverityLevel.MODERATE
                )
            }
            cleanCert in listOf("R", "TV-MA", "16", "16+", "FSK 16", "17+", "18", "18+", "19", "21+", "VM18", "FSK 18", "NC-17", "A", "R15+", "R-15", "R15", "R18+", "R-18", "R18", "RESTRICTED") -> {
                Tuple5(
                    SeverityLevel.MODERATE,
                    SeverityLevel.SEVERE,
                    SeverityLevel.SEVERE,
                    SeverityLevel.MODERATE,
                    SeverityLevel.SEVERE
                )
            }
            else -> {
                Tuple5(
                    SeverityLevel.MILD,
                    SeverityLevel.MILD,
                    SeverityLevel.MILD,
                    SeverityLevel.NONE,
                    SeverityLevel.MILD
                )
            }
        }

        val categories = listOf(
            ParentsGuideCategory(R.string.guide_cat_sex, GuideCategoryType.SEX_NUDITY, iconRes = R.drawable.ic_heart, level = sex),
            ParentsGuideCategory(R.string.guide_cat_violence, GuideCategoryType.VIOLENCE_GORE, iconRes = R.drawable.ic_parents_swords, level = violence),
            ParentsGuideCategory(R.string.guide_cat_profanity, GuideCategoryType.PROFANITY, iconRes = R.drawable.ic_comment, level = profanity),
            ParentsGuideCategory(R.string.guide_cat_alcohol, GuideCategoryType.ALCOHOL_DRUGS, iconRes = R.drawable.ic_parents_wine, level = drugs),
            ParentsGuideCategory(R.string.guide_cat_frightening, GuideCategoryType.FRIGHTENING, iconRes = R.drawable.ic_vibe_scary, level = frightening)
        )

        return ParentsGuideData(
            categories = categories
        )
    }

    private data class Tuple5(
        val sex: SeverityLevel,
        val violence: SeverityLevel,
        val profanity: SeverityLevel,
        val drugs: SeverityLevel,
        val frightening: SeverityLevel
    )
}

@Composable
fun DetailParentsGuide(
    certification: String?,
    countryCode: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null
) {
    val data = remember(certification, countryCode) {
        ParentsGuideResolver.resolve(certification, countryCode)
    } ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Section Title matching FlickTrove typography
        Text(
            text = stringResource(R.string.detail_parents_guide_title).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            ),
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            data.categories.forEach { category ->
                CategoryMeterRow(category = category, accentColor = accentColor)
            }
        }
    }
}

@Composable
private fun CategoryMeterRow(category: ParentsGuideCategory, accentColor: Color) {
    val isNone = category.isNone

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category Icon (lightened poster accent color when active, soft clear white when none)
        Icon(
            imageVector = ImageVector.vectorResource(id = category.iconRes),
            contentDescription = null,
            tint = if (isNone) Color.White.copy(alpha = 0.60f) else ColorUtils.lightenForText(accentColor, 1.35f),
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Category Title (Medium weight)
        Text(
            text = stringResource(category.titleRes),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = if (isNone) Color.White.copy(alpha = 0.70f) else Color.White,
            modifier = Modifier.weight(1f)
        )

        // 3-segment Pill Capsules (Design per Quantità: 0=None, 1=Mild, 2=Moderate, 3=Severe)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..3) {
                val isFilled = i <= category.level.filledBars
                val segmentColor = if (isFilled) {
                    ColorUtils.lightenForText(accentColor, 1.4f)
                } else {
                    Color.White.copy(alpha = 0.22f)
                }
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(segmentColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Severity Label (Bold & bright accent when active, legible grey when None)
        Text(
            text = stringResource(category.level.labelRes),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                fontWeight = if (isNone) FontWeight.Normal else FontWeight.Bold
            ),
            color = if (isNone) Color.White.copy(alpha = 0.65f) else ColorUtils.lightenForText(accentColor, 1.4f),
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 60.dp)
        )
    }
}
