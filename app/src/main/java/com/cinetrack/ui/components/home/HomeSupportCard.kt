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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
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
        val cardShape = RoundedCornerShape(24.dp)
        val coralAccent = Color(0xFFFF4B55)
        val paypalBlue = Color(0xFF0070BA)

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
                            coralAccent.copy(alpha = 0.35f),
                            coralAccent.copy(alpha = 0.08f)
                        )
                    ),
                    shape = cardShape
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                // Header: Icona cerchio cuore a sinistra + Titolo & Descrizione
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icona cerchio rosso/corallo con cuore bianco pieno
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(coralAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
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

                // Pulsanti Ko-fi e PayPal affiancati
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Ko-fi Button (Corallo/Rosso)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .bounceClick {
                                markSupportClicked(context)
                                dismissed = true
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(KOFI_URL))
                                context.startActivity(intent)
                            }
                            .clip(CircleShape)
                            .background(coralAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_kofi),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Ko-fi",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. PayPal Button (Blu PayPal)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .bounceClick {
                                markSupportClicked(context)
                                dismissed = true
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_URL))
                                context.startActivity(intent)
                            }
                            .clip(CircleShape)
                            .background(paypalBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_paypal),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "PayPal",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Tasto Chiudi discreto in alto a destra (non comprime il testo)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .size(28.dp)
                    .bounceClick {
                        dismissed = true
                        markSupportDismissed(context)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                    contentDescription = "Chiudi",
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
