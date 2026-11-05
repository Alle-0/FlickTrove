package com.cinetrack.ui.components.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.utils.bounceClick

private const val PREFS_SUPPORT = "flicktrove_support"
private const val KEY_HAS_SUPPORTED = "has_supported"
private const val KEY_LAST_DISMISSED_AT = "last_dismissed_at_ms"

/** 30 giorni in millisecondi */
private const val COOLDOWN_MS = 30L * 24 * 60 * 60 * 1000

/**
 * Controlla se la card di supporto deve essere mostrata.
 * - Nascosta per sempre se l'utente ha già cliccato Supporta.
 * - Nascosta per 30 giorni dopo ogni dismiss esplicito.
 */
fun shouldShowSupportCard(context: Context): Boolean {
    // Single Source of Truth: se l'utente è già un sostenitore verificato, nascondi la card
    val badgePrefs = context.getSharedPreferences("flicktrove_badge_prefs", Context.MODE_PRIVATE)
    if (badgePrefs.getBoolean("is_supporter_verified", false)) return false

    val prefs = context.getSharedPreferences(PREFS_SUPPORT, Context.MODE_PRIVATE)
    if (prefs.getBoolean(KEY_HAS_SUPPORTED, false)) return false
    val dismissedAt = prefs.getLong(KEY_LAST_DISMISSED_AT, 0L)
    if (dismissedAt == 0L) return true
    return System.currentTimeMillis() - dismissedAt >= COOLDOWN_MS
}

fun markSupportClicked(context: Context) {
    context.getSharedPreferences(PREFS_SUPPORT, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_HAS_SUPPORTED, true)
        .apply()
}

fun markSupportDismissed(context: Context) {
    context.getSharedPreferences(PREFS_SUPPORT, Context.MODE_PRIVATE)
        .edit()
        .putLong(KEY_LAST_DISMISSED_AT, System.currentTimeMillis())
        .apply()
}

/** URL ufficiali donazioni */
private const val KOFI_URL = "https://ko-fi.com/alle0"
private const val PAYPAL_URL = "https://paypal.me/AlessandroBasile0"

@Composable
fun HomeSupportCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Stato locale volatile: si resetta a ogni sessione ma shouldShowSupportCard() fa da guard persistente
    var dismissed by remember { mutableStateOf(false) }
    val shouldShow = remember { shouldShowSupportCard(context) } && !dismissed

    AnimatedVisibility(
        visible = shouldShow,
        enter = fadeIn(tween(400)) + slideInVertically(
            animationSpec = tween(400),
            initialOffsetY = { it / 3 }
        ),
        exit = fadeOut(tween(280)) + slideOutVertically(
            animationSpec = tween(280),
            targetOffsetY = { it / 4 }
        ),
        modifier = modifier
    ) {
        val cardShape = RoundedCornerShape(32.dp)
        val buttonShape = RoundedCornerShape(16.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(cardShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E2028),
                            Color(0xFF14151B)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEAB308).copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = cardShape
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header: Pallino dorato a sinistra con icona mecenate grande
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFEAB308),
                                        Color(0xFFCA8A04)
                                    )
                                )
                            )
                            .border(2.dp, Color(0xFFFDE047).copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_badge_shaking_hands),
                            contentDescription = null,
                            tint = Color(0xFF141419),
                            modifier = Modifier.size(58.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_support_flicktrove),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.settings_support_dev_desc),
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 12.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pulsanti Ko-fi e PayPal (Vibranti, ad alta visibilità e simmetrici)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Ko-fi Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                markSupportClicked(context)
                                dismissed = true
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(KOFI_URL))
                                context.startActivity(intent)
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
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Ko-fi",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "GPay • ApplePay • Card",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.88f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 2. PayPal Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                markSupportClicked(context)
                                dismissed = true
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_URL))
                                context.startActivity(intent)
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
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "PayPal",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Account • Card",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.88f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Tasto Chiudi unificato concentrico in alto a destra (32dp, raggio 16dp a padding 16/16)
            ModalCloseButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
                size = 32.dp,
                iconSize = 16.dp,
                tint = Color.White.copy(alpha = 0.45f),
                onClose = {
                    dismissed = true
                    markSupportDismissed(context)
                }
            )
        }
    }
}
