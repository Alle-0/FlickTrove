package com.cinetrack.ui.components.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.cinetrack.ui.components.common.FlickTroveSwitch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.theme.OnSurfaceMuted
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.mirrorInRtl
import com.cinetrack.ui.utils.rtlSafeLetterSpacing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
fun Color.toArgb(): Int {
    return (alpha * 255.0f + 0.5f).toInt() shl 24 or
            (red * 255.0f + 0.5f).toInt() shl 16 or
            (green * 255.0f + 0.5f).toInt() shl 8 or
            (blue * 255.0f + 0.5f).toInt()
}

@Composable
fun SettingsSection(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    footerText: String? = null,
    initiallyExpanded: Boolean = false,
    borderBrush: Brush? = null,
    backgroundBrush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initiallyExpanded) }
    val arrowRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 360,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "arrowRotation"
    )
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .then(
                if (backgroundBrush != null) Modifier.background(backgroundBrush) else Modifier
            )
            .then(
                if (borderBrush != null) {
                    Modifier.border(
                        width = 1.dp,
                        brush = borderBrush,
                        shape = RoundedCornerShape(32.dp)
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(32.dp)
                    )
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick(scaleDown = 0.98f) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isExpanded = !isExpanded
                }
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thick elegant vertical bar with accent color
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = rtlSafeLetterSpacing(1.5.sp),
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Icon(
                painter = painterResource(id = R.drawable.ic_right),
                contentDescription = "Expand",
                tint = OnSurfaceMuted,
                modifier = Modifier
                    .size(24.dp)
                    .mirrorInRtl()
                    .graphicsLayer {
                        rotationZ = arrowRotation
                    }
            )
        }
        
        // Items Container - Animated Visibility
        androidx.compose.animation.AnimatedVisibility(
            visible = isExpanded,
            enter = androidx.compose.animation.expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 360,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            ) + androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 240)
            ),
            exit = androidx.compose.animation.shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 280,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            ) + androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 180)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content()
                
                if (footerText != null) {
                    Text(
                        text = footerText,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .padding(horizontal = 12.dp),
                        textAlign = TextAlign.Start,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    description: String? = null,
    tint: Color = Color.White,
    borderColor: Color? = null,
    isExternal: Boolean = false,
    alpha: Float = 1f,
    trailing: @Composable (() -> Unit)? = null,
    customContent: @Composable (ColumnScope.() -> Unit)? = null,
    iconTint: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val itemThemeColor = borderColor ?: tint
    val finalIconTint = iconTint ?: itemThemeColor
    val isClickable = onClick != null
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .then(
                if (isClickable) {
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .bounceClick { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick() 
                        }
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(itemThemeColor.copy(alpha = 0.08f))
                        .border(
                            width = 1.dp,
                            color = itemThemeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = finalIconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = if (borderColor != null) borderColor else Color.White
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceMuted,
                            lineHeight = 14.sp,
                            fontSize = 12.sp
                        )
                    }
                }
                
                if (trailing != null) {
                    trailing()
                } else if (isClickable) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = if (isExternal) R.drawable.ic_external_link else R.drawable.ic_right),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = if (isExternal) Modifier.size(18.dp) else Modifier.size(18.dp).mirrorInRtl()
                    )
                }
            }
            
            if (customContent != null) {
                Spacer(modifier = Modifier.height(14.dp))
                customContent()
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
fun AttributionRow(
    brand: String,
    text: String
) {
    val logoRes = when (brand) {
        "TMDB" -> R.drawable.ic_tmdb_logo
        "Trakt.tv" -> R.drawable.ic_trakt_logo
        "TheTVDB" -> R.drawable.ic_tvdb_logo
        "KLIPY" -> R.drawable.ic_klipy_logo
        "SIMKL" -> R.drawable.ic_simkl_logo
        "CommsUni", "CommsUni (commsuni.tv)" -> R.drawable.ic_commsuni_logo
        "Rotten Tomatoes" -> R.drawable.ic_rottentomatoes_logo
        "Metacritic" -> R.drawable.ic_metacritic_logo
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo Representation with local vector or fallback text
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (brand) {
                        "TMDB" -> Color(0xFF032541)
                        "OMDb API" -> Color(0xFFF5C518)
                        "TheTVDB" -> Color(0xFF31B057)
                        "Trakt.tv" -> Color.White.copy(alpha = 0.05f)
                        "KLIPY" -> Color(0xFF141418)
                        "SIMKL" -> Color(0xFF1B1B1B)
                        "CommsUni", "CommsUni (commsuni.tv)" -> Color(0xFF2B2D30)
                        "Rotten Tomatoes" -> Color(0xFFFA320A)
                        "Metacritic" -> Color(0xFFFFCC33)
                        else -> Color.White.copy(alpha = 0.05f)
                    }
                )
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (logoRes != null) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = "$brand Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            when (brand) {
                                "TMDB" -> 6.dp
                                "KLIPY" -> 4.dp
                                else -> 0.dp
                            }
                        ),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Stylized Fallback for OMDb, TheTVDB, or missing logos
                Text(
                    text = when (brand) {
                        "OMDb API" -> "OMDb"
                        "TheTVDB" -> "TVDB"
                        "KLIPY" -> "KLIPY"
                        "SIMKL" -> "SIMKL"
                        "Rotten Tomatoes" -> "RT"
                        "Metacritic" -> "MC"
                        else -> brand.take(1)
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = if (brand == "OMDb API" || brand == "TheTVDB" || brand == "KLIPY" || brand == "SIMKL") 9.sp else 14.sp
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
                    color = if (brand == "OMDb API" || brand == "Metacritic") Color.Black else Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        val annotatedString = buildAnnotatedString {
            val parts = text.split(brand)
            parts.forEachIndexed { index, part ->
                append(part)
                if (index < parts.size - 1) {
                    withStyle(
                        style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color.White)
                    ) {
                        append(brand)
                    }
                }
            }
        }
        
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = OnSurfaceMuted,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun BadgeLegendItem(
    text: String, 
    color: Color, 
    desc: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f) { onToggle(!enabled) }
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 55.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                .border(1.dp, if (enabled) color.copy(alpha = 0.6f) else Color.DarkGray, RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (enabled) color else Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp,
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.8f else 0.4f),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        FlickTroveSwitch(
            checked = enabled,
            onCheckedChange = { onToggle(it) },
            accentColor = color,
            modifier = Modifier.scale(0.85f)
        )
    }
}

@Composable
fun SettingsActionButton(
    text: String? = null,
    icon: ImageVector? = null,
    tint: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .bounceClick(enabled = enabled) { onClick() }
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = if (enabled) 0.1f else 0.05f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            if (text != null) Spacer(modifier = Modifier.width(6.dp))
        }
        if (text != null) {
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}

@Composable
fun DonationBanner(
    modifier: Modifier = Modifier
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = LocalHapticFeedback.current

    val badgeRepo = remember {
        try {
            dagger.hilt.android.EntryPointAccessors.fromApplication(
                context.applicationContext,
                com.cinetrack.data.repository.BadgeRepositoryEntryPoint::class.java
            ).badgeRepository()
        } catch (_: Exception) {
            null
        }
    }

    // Glow effect background brush (Warm Gold / Cinema Slate)
    val glowBrush = Brush.radialGradient(
        colors = listOf(
            com.cinetrack.ui.theme.Gold.copy(alpha = 0.12f),
            Color.Transparent
        ),
        radius = 800f,
        center = androidx.compose.ui.geometry.Offset(0f, 0f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF1E1E26))
            .background(glowBrush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        com.cinetrack.ui.theme.Gold.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.06f)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Medaglione dorato imponente mecenate
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFEAB308),
                                    Color(0xFFCA8A04)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = com.cinetrack.ui.theme.Gold.copy(alpha = 0.6f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_badge_shaking_hands),
                        contentDescription = "Patron",
                        tint = Color(0xFF141419),
                        modifier = Modifier.size(58.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = R.string.settings_support_flicktrove),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = R.string.settings_support_dev_desc),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        ),
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val buttonShape = RoundedCornerShape(16.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ko-fi Button (Vibrante, ad alta visibilità)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .bounceClick {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            uriHandler.openUri("https://ko-fi.com/alle0")
                        }
                        .clip(buttonShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF5E5B),
                                    Color(0xFFE53935)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_kofi),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = "Ko-fi",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    lineHeight = 15.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "GPay • ApplePay • Card",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 10.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // PayPal Button (Vibrante, simmetrico a Ko-fi)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .bounceClick {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            uriHandler.openUri("https://paypal.me/AlessandroBasile0")
                        }
                        .clip(buttonShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0079C1),
                                    Color(0xFF00457C)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_paypal),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = "PayPal",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    lineHeight = 15.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "Account • Card",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 10.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun SettingsDialogConfirmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .height(48.dp)
            .bounceClick(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .clip(shape)
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun SettingsDialogCancelButton(
    text: String = stringResource(R.string.settings_cancel),
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .height(48.dp)
            .bounceClick(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

