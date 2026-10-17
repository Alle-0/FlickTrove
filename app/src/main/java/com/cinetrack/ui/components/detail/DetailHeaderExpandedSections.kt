package com.cinetrack.ui.components.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.api.MovieDetailResponse
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.ExternalRatings
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
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
        in listOf("PG-13", "PG12", "PG-12", "TV-14", "12", "12+", "12A", "FSK 12", "13+", "13A", "14", "14+", "VM14", "15") -> Color(0xFFFF9800)
        in listOf("R", "TV-MA", "16", "16+", "FSK 16", "17+", "18", "18+", "19", "21+", "VM18", "FSK 18", "NC-17", "A", "R15+", "R-15", "R15", "R18+", "R-18", "R18", "RESTRICTED") -> Color(0xFFF44336)
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
        clean in listOf("PG-13", "PG12", "PG-12", "12", "12+", "12A", "FSK 12", "13+", "13A", "14", "14+", "VM14", "15") ->
            HeaderRatingInfo(certification, R.string.rating_pg13_label, R.string.rating_pg13_desc, Color(0xFFFF9800))
        clean in listOf("TV-14") ->
            HeaderRatingInfo(certification, R.string.rating_tv14_label, R.string.rating_tv14_desc, Color(0xFFFF7043))
        clean in listOf("TV-MA") ->
            HeaderRatingInfo(certification, R.string.rating_tvma_label, R.string.rating_tvma_desc, Color(0xFFD32F2F))
        clean in listOf("NC-17", "18", "18+", "19", "21+", "VM18", "FSK 18") ->
            HeaderRatingInfo(certification, R.string.rating_nc17_label, R.string.rating_nc17_desc, Color(0xFFB71C1C))
        clean in listOf("R", "16", "16+", "FSK 16", "17+", "A", "R15+", "R-15", "R15", "R18+", "R-18", "R18", "RESTRICTED") ->
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
            .defaultMinSize(minWidth = 80.dp, minHeight = 36.dp)
            .clip(pillShape)
            .background(accentColor, pillShape)
            .padding(horizontal = 14.dp),
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
            fontSize = 7.sp,
            lineHeight = 7.sp,
            fontWeight = FontWeight.Black,
            color = pillContentColor.copy(alpha = 0.4f),
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

@Composable
internal fun HeaderRatingsSection(
    movie: Movie,
    ratings: ExternalRatings,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
        Spacer(modifier = Modifier.height(16.dp))
        
        val uriHandler = LocalUriHandler.current
        val imdbId = movie.imdbId ?: ratings.imdb

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModernRatingItem(
                badge = { Badge("IMDb", Color(0xFFF5C518), Color.Black) },
                value = ratings.imdb ?: "-",
                subValue = ratings.imdbVotes,
                onClick = {
                    imdbId?.let { uriHandler.openUri("https://www.imdb.com/title/$it") }
                }
            )
            VerticalDivider()
            
            val rtScore = ratings.rottenTomatoes?.removeSuffix("%")?.toIntOrNull() ?: 0
            val rtIcon = if (rtScore >= 60 || ratings.rottenTomatoes == null) "🍅" else "🤢"
            ModernRatingItem(
                badge = { Text(rtIcon, fontSize = 14.sp) },
                value = ratings.rottenTomatoes ?: "-",
                subValue = null,
                onClick = {
                    val title = movie.title ?: movie.name
                    title?.let { uriHandler.openUri("https://www.rottentomatoes.com/search?search=${it}") }
                }
            )
            VerticalDivider()
            
            val mcScore = ratings.metacritic?.split("/")?.firstOrNull()?.toIntOrNull() ?: 0
            val mcColor = when {
                mcScore >= 61 -> Color(0xFF66CC33)
                mcScore >= 40 -> Color(0xFFFFCC33)
                else -> Color(0xFFFF0000)
            }
            ModernRatingItem(
                badge = {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(mcColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_metacritic_logo),
                            contentDescription = "Metacritic Logo",
                            modifier = Modifier.size(14.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                },
                value = ratings.metacritic?.replace("/100", "") ?: "-",
                subValue = "Score",
                onClick = {
                    val title = movie.title ?: movie.name
                    title?.let { uriHandler.openUri("https://www.metacritic.com/search/${it}/") }
                }
            )
            VerticalDivider()
            ModernRatingItem(
                badge = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_trakt_logo),
                        contentDescription = "Trakt.tv Logo",
                        modifier = Modifier.size(20.dp),
                        contentScale = ContentScale.Fit
                    )
                },
                value = ratings.trakt?.let { "${(it * 10).toInt()}%" } ?: "-",
                subValue = ratings.traktVotes,
                onClick = {
                    if (imdbId != null) {
                        uriHandler.openUri("https://trakt.tv/search?query=$imdbId")
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        val hasBoxOffice = (movie.revenue ?: 0L) > 0
        ratings.awards?.let { awardText ->
            if (awardText != "N/A" && awardText.isNotEmpty()) {
                AwardBox(
                    awards = awardText,
                    accentColor = accentColor,
                    onClick = {
                        imdbId?.let { uriHandler.openUri("https://www.imdb.com/title/$it/awards") }
                    }
                )
                if (hasBoxOffice) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        
        movie.revenue?.let { rev ->
            if (rev > 0) {
                val formattedRevenue = java.text.NumberFormat.getCurrencyInstance(Locale.US).apply {
                    maximumFractionDigits = 0
                }.format(rev)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetaBadge(label = "BOX OFFICE", value = formattedRevenue)
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
internal fun HeaderParentsGuideSection(
    certification: String?,
    certificationCountry: String?,
    accentColor: Color,
    onCardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
        Spacer(modifier = Modifier.height(10.dp))

        val cleanCert = certification?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: "NR"
        val ratingInfo = remember(cleanCert) { resolveHeaderRatingInfo(cleanCert) }
        val certColor = remember(cleanCert) { getCertificationColor(cleanCert) }

        // Line 1: Rated Detail Card for this specific film/series
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick(scaleDown = 0.98f) { onCardClick() }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
                .border(0.5.dp, certColor.copy(alpha = 0.35f), CircleShape)
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .defaultMinSize(minWidth = 48.dp)
                    .clip(CircleShape)
                    .background(certColor.copy(alpha = 0.15f))
                    .border(0.5.dp, certColor.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                val displayCode = if (!certificationCountry.isNullOrBlank()) {
                    "$certificationCountry · $cleanCert"
                } else {
                    cleanCert
                }
                Text(
                    text = displayCode,
                    color = certColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(ratingInfo.labelRes),
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(ratingInfo.descriptionRes),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Lines below: Parents Guide category meter rows
        val parentsGuideData = remember(certification, certificationCountry) {
            ParentsGuideResolver.resolve(certification, certificationCountry)
        }

        if (parentsGuideData != null) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                parentsGuideData.categories.forEach { category ->
                    CategoryMeterRow(category = category, accentColor = accentColor)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.detail_rated_all_ages_note),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                ),
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
internal fun HeaderTechnicalSection(
    details: MovieDetailResponse?,
    movie: Movie,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
        Spacer(modifier = Modifier.height(14.dp))

        val symbols = DecimalFormatSymbols(Locale.ITALIAN).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val originalTitle = details?.originalTitle ?: details?.originalName ?: movie.displayName
            val rawStatus = details?.status ?: "—"
            val status = when (rawStatus) {
                "Returning Series" -> stringResource(R.string.status_returning_series)
                "Planned" -> stringResource(R.string.status_planned)
                "In Production" -> stringResource(R.string.status_in_production)
                "Ended" -> stringResource(R.string.status_ended)
                "Canceled", "Cancelled" -> stringResource(R.string.status_canceled)
                "Pilot" -> stringResource(R.string.status_pilot)
                "Rumored" -> stringResource(R.string.status_rumored)
                "Post Production" -> stringResource(R.string.status_post_production)
                "Released" -> stringResource(R.string.status_released)
                else -> rawStatus
            }

            // Row 1: Original Title & Status
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_original_title),
                    value = originalTitle,
                    icon = ImageVector.vectorResource(id = R.drawable.ic_documento),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1.15f)
                )
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_status),
                    value = status,
                    icon = Icons.Rounded.Info,
                    accentColor = accentColor,
                    modifier = Modifier.weight(0.85f)
                )
            }

            // Row 2: Release Date & Budget
            val releaseDate = details?.releaseDate ?: details?.firstAirDate ?: movie.releaseDate ?: movie.firstAirDate ?: "—"
            val budget = details?.budget
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_release_date),
                    value = releaseDate,
                    icon = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1.15f)
                )
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_budget),
                    value = if (budget != null && budget > 0) "${formatter.format(budget)} USD" else "—",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_dollar),
                    accentColor = accentColor,
                    modifier = Modifier.weight(0.85f)
                )
            }

            // Row 3: Language & Revenue
            val origLang = details?.originalLanguage
            val revenue = details?.revenue
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_language),
                    value = origLang?.uppercase(Locale.getDefault()) ?: "—",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_traduzione),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1.15f)
                )
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_revenue),
                    value = if (revenue != null && revenue > 0) "${formatter.format(revenue)} USD" else "—",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_world),
                    accentColor = accentColor,
                    modifier = Modifier.weight(0.85f)
                )
            }

            // Row 4: Production & Country
            val production = details?.productionCompanies?.firstOrNull()?.name ?: "—"
            val country = details?.productionCountries?.firstOrNull()?.name ?: "—"
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_production),
                    value = production,
                    icon = ImageVector.vectorResource(id = R.drawable.ic_edifici),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1.15f)
                )
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_country),
                    value = country,
                    icon = ImageVector.vectorResource(id = R.drawable.ic_partenone),
                    accentColor = accentColor,
                    modifier = Modifier.weight(0.85f)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun HeaderTechnicalItem(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(accentColor.copy(alpha = 0.15f), iconShape)
                .border(0.5.dp, ColorUtils.lightenForText(accentColor, 1.25f).copy(alpha = 0.4f), iconShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ColorUtils.lightenForText(accentColor, 1.35f),
                modifier = Modifier.size(15.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                ),
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ExtraRatingItem(label: String, value: String, subLabel: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.3f),
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(vertical = 1.dp)
        )
        subLabel?.let {
            Text(
                text = it.uppercase(),
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
fun AwardBox(awards: String, accentColor: Color, onClick: () -> Unit = {}) {
    val gold = Color(0xFFFFD700)
    var textWidth by remember { mutableStateOf(0) }
    var containerWidth by remember { mutableStateOf(0) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.97f) { onClick() }
            .background(gold.copy(alpha = 0.05f), CircleShape)
            .border(0.5.dp, gold.copy(alpha = 0.15f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { containerWidth = it.width }
                .then(
                    if (textWidth > containerWidth && containerWidth > 0) {
                        Modifier
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                val fadeWidthPx = 16.dp.toPx()
                                val fadeFraction = if (size.width > 0f) (fadeWidthPx / size.width).coerceAtMost(0.49f) else 0f
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0f to Color.Transparent,
                                        fadeFraction to Color.Black,
                                        1f - fadeFraction to Color.Black,
                                        1f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = awards,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = gold.copy(alpha = 0.8f),
                maxLines = 1,
                softWrap = false,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1200
                    )
                    .then(
                        if (textWidth > containerWidth && containerWidth > 0) {
                            Modifier.padding(horizontal = 16.dp)
                        } else Modifier
                    )
                    .onSizeChanged { textWidth = it.width }
            )
        }
    }
}

@Composable
fun ModernRatingItem(
    badge: @Composable () -> Unit,
    value: String,
    subValue: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.bounceClick(scaleDown = 0.9f) { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        badge()
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            if (subValue != null && subValue != "N/A" && subValue != "Score") {
                Text(
                    text = subValue.uppercase(),
                    fontSize = 7.sp,
                    lineHeight = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.3f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun Badge(text: String, bgColor: Color, textColor: Color, isSquare: Boolean = false) {
    Surface(
        color = bgColor,
        shape = if (isSquare) RoundedCornerShape(4.dp) else CircleShape,
        modifier = (if (isSquare) Modifier.size(20.dp) else Modifier.width(34.dp).height(14.dp))
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = if (isSquare) 12.sp else 8.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

@Composable
fun VerticalDivider() {
    Box(modifier = Modifier.width(0.5.dp).height(24.dp).background(Color.White.copy(alpha = 0.12f)))
}

@Composable
fun MetaBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
    showValueBox: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .let { if (onClick != null) it.bounceClick(scaleDown = 0.95f) { onClick() } else it }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f), CircleShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            .padding(
                start = 10.dp,
                end = if (showValueBox) 3.dp else 10.dp,
                top = 3.dp,
                bottom = 3.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.padding(end = 6.dp)
        )
        if (showValueBox) {
            Box(
                modifier = Modifier
                    .background(valueColor.copy(alpha = 0.12f), CircleShape)
                    .border(0.5.dp, valueColor.copy(alpha = 0.25f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = valueColor
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = valueColor
            )
        }
    }
}

