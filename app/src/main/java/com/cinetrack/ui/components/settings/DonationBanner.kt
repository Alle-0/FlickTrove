package com.cinetrack.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.theme.Gold
import com.cinetrack.ui.utils.bounceClick
import dagger.hilt.android.EntryPointAccessors

/**
 * Premium patronage card with gold styling, support description,
 * and high-contrast external Ko-fi and PayPal action buttons.
 */
@Composable
fun DonationBanner(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val badgeRepo = remember {
        try {
            EntryPointAccessors.fromApplication(
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
            Gold.copy(alpha = 0.12f),
            Color.Transparent
        ),
        radius = 800f,
        center = Offset(0f, 0f)
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
                        Gold.copy(alpha = 0.45f),
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
                // Medaglione dorato mecenate
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
                            color = Gold.copy(alpha = 0.6f),
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
                        text = stringResource(id = R.string.settings_support_flicktrove),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(id = R.string.settings_support_dev_desc),
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
                // Ko-fi Button
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

                // PayPal Button
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
