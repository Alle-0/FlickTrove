package com.cinetrack.ui.components

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
                .statusBarsPadding()
                .padding(start = 10.dp)
                .padding(top = 24.dp, bottom = 12.dp)
        ) {


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SectionHeader("Film")
                DrawerItem(
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    label = "Film popolari",
                    isSelected = selectedRoute == "popular_movies",
                    onClick = { onNavigate("popular_movies") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = Icons.Rounded.Movie,
                    label = "Adesso al cinema",
                    isSelected = selectedRoute == "now_playing_movies",
                    onClick = { onNavigate("now_playing_movies") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = Icons.Rounded.Event,
                    label = "Film in uscita",
                    isSelected = selectedRoute == "upcoming_movies",
                    onClick = { onNavigate("upcoming_movies") },
                    accentColor = accentColor
                )
                
                SectionHeader("Serie TV")
                DrawerItem(
                    icon = Icons.Rounded.Tv,
                    label = "Serie popolari",
                    isSelected = selectedRoute == "popular_tv",
                    onClick = { onNavigate("popular_tv") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = Icons.Rounded.Monitor,
                    label = "Serie ora in streaming",
                    isSelected = selectedRoute == "airing_today_tv",
                    onClick = { onNavigate("airing_today_tv") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = Icons.Rounded.Event,
                    label = "Serie in arrivo",
                    isSelected = selectedRoute == "on_the_air_tv",
                    onClick = { onNavigate("on_the_air_tv") },
                    accentColor = accentColor
                )

                SectionHeader("Personalizzato")
                DrawerItem(
                    icon = Icons.Rounded.AutoAwesome,
                    label = "Consigliati per te",
                    isSelected = selectedRoute == "recommendations",
                    onClick = { onNavigate("recommendations") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = Icons.Rounded.AutoAwesome,
                    label = "Sorprendimi",
                    isSelected = selectedRoute == "surprise_me",
                    onClick = { onNavigate("surprise_me") },
                    accentColor = accentColor
                )
                DrawerItem(
                    icon = Icons.Rounded.FolderSpecial,
                    label = "Le mie cartelle",
                    isSelected = selectedRoute == "my_folders",
                    onClick = { onNavigate("my_folders") },
                    accentColor = accentColor
                )

                SectionHeader("Generale")
                DrawerItem(
                    icon = Icons.Rounded.Settings,
                    label = "Impostazioni",
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
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
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
        modifier = Modifier.padding(start = 24.dp, top = 22.dp, bottom = 4.dp)
    )
}
