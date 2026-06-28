package com.cinetrack.ui.components

import androidx.compose.ui.res.stringResource
import com.cinetrack.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.google.firebase.auth.FirebaseAuth

import com.cinetrack.ui.components.glass.glassmorphic
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState
import androidx.compose.ui.res.vectorResource
@Composable
fun GlassyDrawer(
    hazeState: HazeState,
    selectedRoute: String? = null,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(290.dp)
            .offset(x = (-10).dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF121214).copy(alpha = 0.95f),
                            Color(0xFF1E1E22).copy(alpha = 0.92f)
                        )
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                .padding(start = 10.dp)
        ) {


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.drawer_menu),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 20.dp)
                    .fadingEdge(scrollState)
                    .verticalScroll(scrollState)
            ) {
                SectionHeader(stringResource(R.string.drawer_movies))
                DrawerItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_inflation),
                    label = stringResource(R.string.drawer_movies_popular),
                    isSelected = selectedRoute == "popular_movies",
                    onClick = { onNavigate("popular_movies") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_ciack),
                    label = stringResource(R.string.drawer_movies_now_playing),
                    isSelected = selectedRoute == "now_playing_movies",
                    onClick = { onNavigate("now_playing_movies") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                    label = stringResource(R.string.drawer_movies_upcoming),
                    isSelected = selectedRoute == "upcoming_movies",
                    onClick = { onNavigate("upcoming_movies") },
                    accentColor = accentColor
                )
                
                SectionHeader(stringResource(R.string.drawer_tv))
                DrawerItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_tv),
                    label = stringResource(R.string.drawer_tv_popular),
                    isSelected = selectedRoute == "popular_tv",
                    onClick = { onNavigate("popular_tv") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_tv),
                    label = stringResource(R.string.drawer_tv_airing),
                    isSelected = selectedRoute == "airing_today_tv",
                    onClick = { onNavigate("airing_today_tv") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                    label = stringResource(R.string.drawer_tv_upcoming),
                    isSelected = selectedRoute == "on_the_air_tv",
                    onClick = { onNavigate("on_the_air_tv") },
                    accentColor = accentColor
                )

                SectionHeader(stringResource(R.string.drawer_custom))
                DrawerItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_cartella_piena),
                    label = stringResource(R.string.drawer_custom_folders),
                    isSelected = selectedRoute == "my_folders",
                    onClick = { onNavigate("my_folders") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
                    label = stringResource(R.string.drawer_custom_recommendations),
                    isSelected = selectedRoute == "recommendations",
                    onClick = { onNavigate("recommendations") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_bacchetta),
                    label = stringResource(R.string.drawer_custom_surprise),
                    isSelected = selectedRoute == "surprise_me",
                    onClick = { onNavigate("surprise_me") },
                    accentColor = accentColor
                )

                SectionHeader(stringResource(R.string.drawer_discover))
                DrawerItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_documento),
                    label = stringResource(R.string.drawer_custom_news),
                    isSelected = selectedRoute == "news",
                    onClick = { onNavigate("news") },
                    accentColor = accentColor
                )

                SectionHeader(stringResource(R.string.drawer_general))
                DrawerItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_settings),
                    label = stringResource(R.string.drawer_general_settings),
                    isSelected = selectedRoute == "settings",
                    onClick = { onNavigate("settings") },
                    accentColor = accentColor
                )
                

            }
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    accentColor: Color,
    isSelected: Boolean = false
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 1.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical neon bar for selected item
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = accentColor, spotColor = accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            // Spacer to keep icons aligned even when not selected
            Spacer(modifier = Modifier.width(11.dp)) 
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            color = Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 11.sp
        ),
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp)
    )
}

fun Modifier.fadingEdge(scrollState: androidx.compose.foundation.ScrollState): Modifier = this
    .graphicsLayer { alpha = 0.99f }
    .drawWithContent {
        drawContent()

        val topFadingEdgeStrength = (scrollState.value / 100f).coerceIn(0f, 1f)
        val bottomFadingEdgeStrength = if (scrollState.maxValue > 0) {
            ((scrollState.maxValue - scrollState.value) / 100f).coerceIn(0f, 1f)
        } else 0f
        
        val edgeHeight = 32.dp.toPx()

        if (topFadingEdgeStrength > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black,
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = edgeHeight
                ),
                size = androidx.compose.ui.geometry.Size(size.width, edgeHeight),
                alpha = topFadingEdgeStrength,
                blendMode = BlendMode.DstOut
            )
        }
        if (bottomFadingEdgeStrength > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black
                    ),
                    startY = size.height - edgeHeight,
                    endY = size.height
                ),
                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - edgeHeight),
                size = androidx.compose.ui.geometry.Size(size.width, edgeHeight),
                alpha = bottomFadingEdgeStrength,
                blendMode = BlendMode.DstOut
            )
        }
    }
