package com.cinetrack.ui.components.glass

import androidx.compose.ui.res.stringResource
import com.cinetrack.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.cinetrack.util.NotificationHelper
import com.cinetrack.ui.theme.HazeStyles
import androidx.compose.ui.res.vectorResource
/**
 * A premium glassmorphic drawer content component.
 */
@Composable
fun GlassyDrawerContent(
    activeRoute: String,
    onNavigate: (String, String?) -> Unit,
    onClose: () -> Unit,
    version: String = "3.0.0"
) {
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp) // standard drawer width
            .glassmorphic(
                shape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
                blurRadius = HazeStyles.GlassBlurRadius
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.drawer_menu),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                IconButton(onClick = onClose) {
                    Icon(ImageVector.vectorResource(id = R.drawable.ic_x), contentDescription = "Close", tint = accentColor, modifier = Modifier.size(24.dp))
                }
            }

            // Scrollable Items
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 20.dp)
            ) {
                DrawerSection(title = stringResource(R.string.drawer_movies))
                DrawerItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_inflation),
                    label = stringResource(R.string.drawer_movies_popular),
                    isActive = activeRoute == "trending_movies",
                    onClick = { onNavigate("discover", "trending_movies") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = Icons.Rounded.Movie,
                    label = stringResource(R.string.drawer_movies_now_playing),
                    isActive = activeRoute == "now_playing_movies",
                    onClick = { onNavigate("discover", "now_playing_movies") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = Icons.Rounded.Event,
                    label = stringResource(R.string.drawer_movies_upcoming),
                    isActive = activeRoute == "upcoming_movies",
                    onClick = { onNavigate("discover", "upcoming_movies") },
                    accentColor = accentColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                DrawerSection(title = stringResource(R.string.drawer_tv))
                DrawerItem(
                    icon = Icons.Rounded.Tv,
                    label = stringResource(R.string.drawer_tv_popular),
                    isActive = activeRoute == "trending_tv",
                    onClick = { onNavigate("discover", "trending_tv") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = Icons.Rounded.Monitor,
                    label = stringResource(R.string.drawer_tv_airing),
                    isActive = activeRoute == "on_the_air_tv",
                    onClick = { onNavigate("discover", "on_the_air_tv") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = Icons.Rounded.Event,
                    label = stringResource(R.string.drawer_tv_upcoming),
                    isActive = activeRoute == "airing_today_tv",
                    onClick = { onNavigate("discover", "airing_today_tv") },
                    accentColor = accentColor
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                DrawerSection(title = stringResource(R.string.drawer_custom))
                DrawerItem(
                    icon = Icons.Rounded.AutoAwesome,
                    label = stringResource(R.string.drawer_custom_recommendations),
                    isActive = activeRoute == "recommendations",
                    onClick = { onNavigate("recommendations", null) },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = Icons.Rounded.AutoAwesome,
                    label = stringResource(R.string.drawer_custom_surprise),
                    isActive = activeRoute == "surprise",
                    onClick = { onNavigate("surprise", null) },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = Icons.Rounded.FolderSpecial,
                    label = stringResource(R.string.drawer_custom_folders),
                    isActive = activeRoute == "folders",
                    onClick = { onNavigate("folders", null) },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = Icons.AutoMirrored.Rounded.Article,
                    label = stringResource(R.string.drawer_custom_news),
                    isActive = activeRoute == "news",
                    onClick = { onNavigate("news", null) },
                    accentColor = accentColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                DrawerSection(title = stringResource(R.string.drawer_general))
                DrawerItem(
                    icon = Icons.Rounded.Settings,
                    label = stringResource(R.string.drawer_general_settings),
                    isActive = activeRoute == "settings",
                    onClick = { onNavigate("settings", null) },
                    accentColor = accentColor
                )



                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "FlickTrove v$version",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}

@Composable
private fun DrawerSection(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall.copy(
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    )
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.1f) else Color.Transparent)
            .clickable {
                if (!isActive) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isActive) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (isActive) accentColor else Color.White,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        )
    }
}
