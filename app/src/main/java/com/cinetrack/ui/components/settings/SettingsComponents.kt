package com.cinetrack.ui.components.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.theme.OnSurfaceMuted
import com.cinetrack.ui.utils.bounceClick
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
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initiallyExpanded) }
    val arrowRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
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
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(32.dp)
            )
    ) {
        // Section Header (Clickable for Accordion)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
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
                    .height(16.dp)
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
                        letterSpacing = 1.5.sp,
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
                    .graphicsLayer {
                        rotationZ = arrowRotation
                    }
            )
        }
        
        // Items Container - Animated Visibility
        androidx.compose.animation.AnimatedVisibility(
            visible = isExpanded,
            enter = androidx.compose.animation.expandVertically(
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            ) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically(
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            ) + androidx.compose.animation.fadeOut()
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
    trailing: @Composable (() -> Unit)? = null,
    customContent: @Composable (ColumnScope.() -> Unit)? = null,
    iconTint: Color? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val itemThemeColor = borderColor ?: tint
    val finalIconTint = iconTint ?: itemThemeColor
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .bounceClick { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick() 
            }
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
                } else if (customContent == null) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = if (isExternal) R.drawable.ic_external_link else R.drawable.ic_right),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            if (customContent != null) {
                Spacer(modifier = Modifier.height(14.dp))
                customContent()
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
        "GIPHY" -> R.drawable.ic_giphy_logo
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
                        "GIPHY" -> Color.Black
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
                                "GIPHY" -> 8.dp
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
                        "GIPHY" -> "GIPHY"
                        else -> brand.take(1)
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = if (brand == "OMDb API" || brand == "TheTVDB" || brand == "GIPHY") 9.sp else 14.sp
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
                    color = if (brand == "OMDb API") Color.Black else Color.White
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggle(!enabled) }
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
        Switch(
            checked = enabled,
            onCheckedChange = { onToggle(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = color,
                checkedTrackColor = color.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF2C2C2E),
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.scale(0.85f)
        )
    }
}

@Composable
fun SettingsActionButton(
    text: String? = null,
    icon: ImageVector? = null,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
    val haptic = LocalHapticFeedback.current
    
    // Heart pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartPulse"
    )
    
    // Glow effect background brush
    val glowBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFF5E5B).copy(alpha = 0.15f),
            Color.Transparent
        ),
        radius = 800f,
        center = androidx.compose.ui.geometry.Offset(0f, 0f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1E1E26))
            .background(glowBrush) // Apply glow on top of base dark color
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF5E5B).copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Animated Icon container
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF5E5B),
                                    Color(0xFFFF2A25)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = Color(0xFFFF8B89).copy(alpha = 0.5f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "Support",
                        tint = Color.White,
                        modifier = Modifier
                            .size(26.dp)
                            .scale(scale) // Apply pulse animation
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = R.string.settings_support_flicktrove),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = R.string.settings_support_dev_desc),
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 16.sp
                        ),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ko-fi Button (Primary)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF5E5B),
                                    Color(0xFFFF2A25)
                                )
                            )
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            uriHandler.openUri("https://ko-fi.com/alle0")
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_kofi), 
                            contentDescription = null, 
                            tint = Color.White, 
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ko-fi",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
                
                // PayPal Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0079C1),
                                    Color(0xFF00457C)
                                )
                            )
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            uriHandler.openUri("https://paypal.me/alle0")
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_paypal), 
                            contentDescription = null, 
                            tint = Color.White, 
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PayPal",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
