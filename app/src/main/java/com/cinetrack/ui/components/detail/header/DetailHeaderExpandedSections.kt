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
            .defaultMinSize(minHeight = 36.dp)
            .clip(pillShape)
            .background(accentColor, pillShape)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
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



