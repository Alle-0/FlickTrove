package com.cinetrack.ui.components.stats

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.share.saveBitmapToCacheUri
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.FlowPersona
import com.cinetrack.ui.viewmodel.MvpStat
import com.cinetrack.ui.viewmodel.VibeStat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Card principale del profilo / persona cinematografica
 */
@Composable
fun FlowPersonaCard(
    persona: FlowPersona,
    totalTitles: Int,
    topVibes: List<VibeStat>,
    topMvp: MvpStat? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cardGraphicsLayer = rememberGraphicsLayer()
    var isSharing by remember { mutableStateOf(false) }

    val totalVibesCount = remember(topVibes) { topVibes.sumOf { it.count } }
    val topVibeStat = topVibes.firstOrNull()
    val topVibeCount = topVibeStat?.count ?: 0
    val dominancePercent = if (totalVibesCount > 0) (topVibeCount * 100) / totalVibesCount else 0
    val personaColor = Color(persona.colorHex)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .drawWithContent {
                drawContent()
                if (size.width >= 1f && size.height >= 1f) {
                    val cornerPx = 24.dp.toPx()
                    cardGraphicsLayer.record(
                        size = IntSize(size.width.toInt(), size.height.toInt())
                    ) {
                        // Sfondo scuro opaco dietro al gradiente traslucido per l'immagine esportata
                        drawRoundRect(
                            color = Color(0xFF0A0D12),
                            cornerRadius = CornerRadius(cornerPx, cornerPx)
                        )
                        this@drawWithContent.drawContent()
                    }
                }
            }
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        personaColor.copy(alpha = 0.24f),
                        personaColor.copy(alpha = 0.16f)
                    )
                )
            )
            .border(
                1.dp,
                personaColor.copy(alpha = 0.35f),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Riga Avatar e Statistiche
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(personaColor.copy(alpha = 0.15f))
                        .border(1.dp, personaColor.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = persona.iconRes),
                        contentDescription = null,
                        tint = personaColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$topVibeCount film • $dominancePercent%",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nome Stile / Persona
            Text(
                text = stringResource(persona.titleRes),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Descrizione
            Text(
                text = stringResource(persona.descriptionRes),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.5.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Footer con metriche reali e pulsante Condividi su righe dedicate per evitare a capo e sovrapposizioni
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val topVibeName = topVibeStat?.let {
                    it.labelRes?.let { res -> stringResource(res) } ?: it.vibe
                } ?: "-"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = if (topMvp != null) {
                            Modifier.widthIn(max = 96.dp)
                        } else {
                            Modifier.weight(1f)
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.flow_stats_insight_vibe),
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = topVibeName,
                            color = personaColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (topMvp != null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Top MVP",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = topMvp.actorName,
                                color = Color(0xFFFFD700),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.widthIn(max = 112.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = stringResource(R.string.flow_stats_insight_titles),
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$totalTitles",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                val titleString = stringResource(persona.titleRes)
                if (!isSharing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick {
                                if (isSharing) return@bounceClick
                                isSharing = true
                                scope.launch(Dispatchers.Main) {
                                    try {
                                        withFrameMillis {}
                                        delay(80)
                                        withFrameMillis {}
                                        val bitmap = cardGraphicsLayer.toImageBitmap().asAndroidBitmap()
                                        if (bitmap.width > 0 && bitmap.height > 0) {
                                            val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
                                                bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                            } else {
                                                bitmap
                                            }
                                            val contentUri = withContext(Dispatchers.IO) {
                                                saveBitmapToCacheUri(context, softwareBitmap, titleString)
                                            }
                                            if (softwareBitmap !== bitmap) {
                                                softwareBitmap.recycle()
                                            }
                                            if (contentUri != null) {
                                                shareFlowPersonaImage(context, contentUri, titleString)
                                            } else {
                                                shareFlowPersona(context, titleString)
                                            }
                                        } else {
                                            shareFlowPersona(context, titleString)
                                        }
                                    } catch (_: Exception) {
                                        shareFlowPersona(context, titleString)
                                    } finally {
                                        isSharing = false
                                    }
                                }
                            }
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.card_share),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Condivisione immagine della Flow Persona Card tramite FileProvider, con fallback testuale
 */
private fun shareFlowPersonaImage(context: Context, contentUri: Uri, personaTitle: String) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, personaTitle)
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.flow_stats_share_text, personaTitle))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.stats_share_chooser_title))
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    } catch (_: Exception) {
        shareFlowPersona(context, personaTitle)
    }
}

/**
 * Condivisione nativa tramite Android Share Intent (fallback testuale)
 */
private fun shareFlowPersona(context: Context, personaTitle: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                context.getString(R.string.flow_stats_share_text, personaTitle)
            )
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    } catch (_: Exception) {
        // Fallback silenzioso
    }
}
