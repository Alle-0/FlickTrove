package com.cinetrack.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.api.CastMember
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl

/**
 * Chip representing an emotional vibe with icon, label and global community percentage.
 */
@Composable
fun VibeChip(
    vibe: EmotionalVibe,
    isSelected: Boolean,
    isDisabled: Boolean = false,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .alpha(if (isDisabled) 0.3f else 1f)
            .bounceClick(scaleDown = if (isDisabled) 1f else 0.90f) {
                if (!isDisabled) onClick()
            }
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.18f)
                else Color.White.copy(alpha = 0.06f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            Icon(
                painter = painterResource(id = vibe.iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = stringResource(vibe.labelRes),
                fontSize = 9.sp,
                lineHeight = 10.sp,
                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.7f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Always render the text to maintain consistent chip heights, 
            // but make it transparent if the percentage is 0
            Text(
                text = "${vibe.percentage}%",
                fontSize = 8.sp,
                lineHeight = 9.sp,
                color = if (vibe.percentage > 0) {
                    if (isSelected) accentColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f)
                } else {
                    Color.Transparent
                },
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Chip representing a cast member for the MVP (Most Valuable Performer) selection.
 */
@Composable
fun CastMvpChip(
    actor: CastMember,
    isMvp: Boolean,
    accentColor: Color,
    characterImageUrl: String? = null,
    mvpPercentage: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mvpGold = Color(0xFFFFC800)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(64.dp)
            .bounceClick(scaleDown = 0.92f) { onClick() }
    ) {
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(64.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = if (isMvp) 2.dp else 1.dp,
                        color = if (isMvp) mvpGold else Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                val initials = remember(actor.name) {
                    actor.name.split(" ")
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .uppercase()
                }
                if (initials.isNotEmpty()) {
                    Text(
                        text = initials,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_persona),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (!characterImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = characterImageUrl,
                        contentDescription = actor.character,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                    )
                } else if (!actor.profilePath.isNullOrBlank()) {
                    AsyncImage(
                        model = buildTmdbImageUrl(actor.profilePath, ImageType.PROFILE, ImageQuality.LOW),
                        contentDescription = actor.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                    )
                }
            }
            if (isMvp) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(mvpGold)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                        contentDescription = null,
                        tint = Color(0xFF1A1A1A),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = actor.character ?: actor.name,
            fontSize = 9.sp,
            color = if (isMvp) mvpGold else Color.White.copy(alpha = 0.75f),
            fontWeight = if (isMvp) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2,
            lineHeight = 10.sp,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (mvpPercentage != null && mvpPercentage > 0) {
            Text(
                text = "$mvpPercentage%",
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            )
        }
        Text(
            text = actor.name,
            fontSize = 8.sp,
            color = Color.Gray,
            maxLines = 2,
            lineHeight = 9.sp,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
