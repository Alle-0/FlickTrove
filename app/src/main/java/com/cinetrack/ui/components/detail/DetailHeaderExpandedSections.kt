package com.cinetrack.ui.components.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.bounceClick
import java.util.Locale



enum class DetailHeaderExpandedSection {
    NONE,
    RATINGS,
    PARENTS_GUIDE,
    TECHNICAL
}

internal data class HeaderRatingInfo(
    val code: String,
    val labelRes: Int,
    val descriptionRes: Int,
    val color: Color
)

internal fun getCertificationColor(cleanCert: String): Color {
    return when (cleanCert) {
        in listOf("G", "TV-G", "TV-Y", "U", "T", "0", "FSK 0", "L", "APTA", "0+", "ALL", "SU", "GENEL", "GENEL İZLEYİCİ") -> Color(0xFF4CAF50)
        in listOf("PG", "TV-PG", "TV-Y7", "TV-Y7-FV", "6", "6+", "FSK 6", "7", "7+", "7A", "10", "10+", "UA") -> Color(0xFF8BC34A)
        in listOf("PG-13", "PG12", "PG-12", "12", "12+", "12A", "FSK 12", "13+", "13A", "VM12") -> Color(0xFFFF9800)
        in listOf("TV-14", "14", "14+", "VM14") -> Color(0xFFFF7043)
        in listOf("15", "15+") -> Color(0xFFFF5722)
        in listOf("16", "16+", "NC16", "NC-16", "VM16", "FSK 16") -> Color(0xFFE64A19)
        in listOf("R", "TV-MA", "17+", "18", "18+", "19", "21+", "VM18", "FSK 18", "NC-17", "M18", "R21", "A", "R15+", "R-15", "R15", "R18+", "R-18", "R18", "RESTRICTED") -> Color(0xFFF44336)
        else -> Color.White
    }
}

internal fun resolveHeaderRatingInfo(certification: String?): HeaderRatingInfo {
    if (certification.isNullOrBlank()) {
        return HeaderRatingInfo("NR", R.string.rated_guide_title, R.string.rated_guide_help, Color.White)
    }
    val clean = certification.trim().uppercase(Locale.ROOT)
    return when {
        clean in listOf("G", "TV-G", "TV-Y", "U", "T", "0", "FSK 0", "L", "APTA", "0+", "ALL", "SU", "GENEL", "GENEL İZLEYİCİ") ->
            HeaderRatingInfo(certification, R.string.rating_g_label, R.string.rating_g_desc, Color(0xFF4CAF50))
        clean in listOf("PG", "TV-PG", "TV-Y7", "TV-Y7-FV", "6", "6+", "FSK 6", "7", "7+", "7A", "10", "10+", "UA") ->
            HeaderRatingInfo(certification, R.string.rating_pg_label, R.string.rating_pg_desc, Color(0xFF8BC34A))
        clean in listOf("PG-13", "PG12", "PG-12", "12", "12+", "12A", "FSK 12", "13+", "13A") ->
            HeaderRatingInfo(certification, R.string.rating_pg13_label, R.string.rating_pg13_desc, Color(0xFFFF9800))
        clean in listOf("VM12") ->
            HeaderRatingInfo(certification, R.string.rating_vm12_label, R.string.detail_rating_it_12, Color(0xFFFF9800))
        clean in listOf("VM14") ->
            HeaderRatingInfo(certification, R.string.rating_vm14_label, R.string.detail_rating_it_14, Color(0xFFFF7043))
        clean in listOf("TV-14", "14", "14+") ->
            HeaderRatingInfo(certification, R.string.rating_tv14_label, R.string.rating_tv14_desc, Color(0xFFFF7043))
        clean in listOf("15", "15+") ->
            HeaderRatingInfo(certification, R.string.rating_15_label, R.string.rating_15_desc, Color(0xFFFF5722))
        clean in listOf("16", "16+", "NC16", "NC-16", "VM16", "FSK 16") ->
            HeaderRatingInfo(certification, R.string.rating_16_label, R.string.rating_16_desc, Color(0xFFE64A19))
        clean in listOf("TV-MA") ->
            HeaderRatingInfo(certification, R.string.rating_tvma_label, R.string.rating_tvma_desc, Color(0xFFD32F2F))
        clean in listOf("VM18") ->
            HeaderRatingInfo(certification, R.string.rating_nc17_label, R.string.detail_rating_it_18, Color(0xFFB71C1C))
        clean in listOf("NC-17", "18", "18+", "19", "21+", "FSK 18", "M18", "R21") ->
            HeaderRatingInfo(certification, R.string.rating_nc17_label, R.string.rating_nc17_desc, Color(0xFFB71C1C))
        clean in listOf("R", "17+", "A", "R15+", "R-15", "R15", "R18+", "R-18", "R18", "RESTRICTED") ->
            HeaderRatingInfo(certification, R.string.rating_r_label, R.string.rating_r_desc, Color(0xFFF44336))
        else ->
            HeaderRatingInfo(certification, R.string.rated_guide_title, R.string.rated_guide_help, Color.White)
    }
}


@Composable
fun RatingPill(
    voteAverage: Double,
    expansion: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val pillRadius = 48.dp
    val pillShape = RoundedCornerShape(pillRadius)
    val pillContentColor = ColorUtils.contentColorForAccent(accentColor)
    
    Box(
        modifier = modifier
            .bounceClick(scaleDown = 0.92f, onClick = onClick)
            .defaultMinSize(minWidth = 72.dp, minHeight = 36.dp)
            .clip(pillShape)
            .background(accentColor, pillShape)
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        // Main Rating
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                // Precise centering logic
                translationY = (1f - expansion) * -2.dp.toPx()
                compositingStrategy = CompositingStrategy.Offscreen
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_tmdb_logo),
                    contentDescription = "TMDB Logo",
                    modifier = Modifier.height(11.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(pillContentColor.copy(alpha = 0.9f))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                    contentDescription = null,
                    tint = pillContentColor.copy(alpha = 0.9f),
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = String.format(Locale.US, "%.1f", voteAverage),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = pillContentColor.copy(alpha = 0.9f)
                )
            }
        }

        // Help Text - Moved further down
        Text(
            text = if (expansion > 0.5f) stringResource(R.string.detail_tap_for_less) else stringResource(R.string.detail_tap_for_more),
            fontSize = 6.8.sp,
            lineHeight = 7.sp,
            fontWeight = FontWeight.Black,
            color = pillContentColor.copy(alpha = 0.4f),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .graphicsLayer {
                    // Precise offset
                    val startOffset = 11.dp.toPx()
                    translationY = startOffset
                }
        )
    }
}

@Composable
internal fun HeaderRatedPill(
    certification: String?,
    certificationCountry: String?,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val pillShape = CircleShape
    val cleanCert = certification?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: "NR"
    val certColor = remember(cleanCert) { getCertificationColor(cleanCert) }
    val selectedContentColor = if (certColor == Color.White) Color.Black else Color.Black

    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) selectedContentColor else certColor,
        animationSpec = tween(250),
        label = "RatedContentColor"
    )

    val revealProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(
            durationMillis = 320,
            easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
        ),
        label = "RatedReveal"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) Color.Transparent else certColor.copy(alpha = 0.55f),
        animationSpec = tween(200),
        label = "RatedBorderColor"
    )

    Box(
        modifier = modifier
            .bounceClick(scaleDown = 0.92f, onClick = onClick)
            .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
            .clip(pillShape)
            .background(Color.White.copy(alpha = 0.08f), pillShape)
            .drawBehind {
                if (revealProgress > 0f) {
                    val maxRadius = kotlin.math.hypot(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = certColor,
                        radius = maxRadius * revealProgress,
                        center = center
                    )
                }
            }
            .border(
                width = 0.5.dp,
                color = animatedBorderColor,
                shape = pillShape
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cleanCert,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            color = animatedContentColor
        )
    }
}

@Composable
internal fun HeaderTechnicalPill(
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val pillShape = CircleShape
    val selectedContentColor = ColorUtils.contentColorForAccent(accentColor)

    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) selectedContentColor else Color.White.copy(alpha = 0.85f),
        animationSpec = tween(250),
        label = "TechContentColor"
    )

    val revealProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(
            durationMillis = 320,
            easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
        ),
        label = "TechReveal"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.15f),
        animationSpec = tween(200),
        label = "TechBorderColor"
    )

    Box(
        modifier = modifier
            .bounceClick(scaleDown = 0.92f, onClick = onClick)
            .size(36.dp)
            .clip(pillShape)
            .background(Color.White.copy(alpha = 0.08f), pillShape)
            .drawBehind {
                if (revealProgress > 0f) {
                    val maxRadius = kotlin.math.hypot(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = accentColor,
                        radius = maxRadius * revealProgress,
                        center = center
                    )
                }
            }
            .border(
                width = 0.5.dp,
                color = animatedBorderColor,
                shape = pillShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = if (isSelected) R.drawable.ic_open_ciak else R.drawable.ic_ciak),
            contentDescription = stringResource(R.string.detail_btn_tech_specs),
            tint = animatedContentColor,
            modifier = Modifier.size(15.dp)
        )
    }
}



