package com.cinetrack.ui.components.badge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cinetrack.R
import com.cinetrack.ui.theme.Gold
import com.cinetrack.ui.utils.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dialog cinematografico "Titoli di Coda" (End Credits).
 * Celebra i Produttori Esecutivi (donatori e mecenati), la regia e i partner dati
 * con la tipica estetica dei titoli di coda del cinema d'autore.
 */
@Composable
fun CinemaCreditsModal(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    onFetchSupporters: (suspend () -> List<String>)? = null,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var supporters by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        val list = try {
            withContext(Dispatchers.IO) {
                onFetchSupporters?.invoke().orEmpty()
            }
        } catch (_: Exception) {
            emptyList()
        }
        supporters = list
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .padding(horizontal = 20.dp, vertical = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            // Contenitore Titoli di Coda: Stile Pellicola 35mm
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF141419),
                                Color(0xFF0C0C0F)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Gold.copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar con chiusura
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_badge_shaking_hands),
                                contentDescription = null,
                                tint = Gold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.credits_screen_title).uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    fontSize = 13.sp
                                ),
                                color = Gold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .bounceClick { onDismissRequest() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Chiudi",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contenuto a scorrimento dei crediti
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        // Titolo Principale
                        item {
                            Text(
                                text = "FLICKTROVE",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 4.sp,
                                    fontFamily = FontFamily.Serif
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.credits_screen_subtitle),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    letterSpacing = 1.sp,
                                    fontSize = 12.sp
                                ),
                                color = Color.White.copy(alpha = 0.55f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }

                        // PRODUTTORI ESECUTIVI (I Mecenati del Cinema)
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.credits_producers_role).uppercase(),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.5.sp,
                                        fontSize = 13.sp
                                    ),
                                    color = Gold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.credits_producers_desc),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.60f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Gold,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                } else if (supporters.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.credits_producers_empty),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            fontSize = 12.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.40f),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }

                        // Lista dei donatori
                        if (!isLoading && supporters.isNotEmpty()) {
                            items(supporters) { name ->
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(28.dp))
                            }
                        }

                        // Partner & Attribuzioni Tecniche
                        item {
                            CreditsRoleSection(
                                role = stringResource(R.string.credits_tech_role),
                                names = listOf("The Movie Database (TMDB)", "Trakt.tv", "SIMKL", "OMDb API", "TheTVDB", "CommsUni")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.credits_tech_desc),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    lineHeight = 15.sp
                                ),
                                color = Color.White.copy(alpha = 0.40f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // Special Thanks
                        item {
                            CreditsRoleSection(
                                role = stringResource(R.string.credits_special_thanks),
                                names = emptyList()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.credits_special_thanks_desc),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                ),
                                color = Color.White.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditsRoleSection(
    role: String,
    names: List<String>,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = role.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 11.sp
            ),
            color = if (highlight) Gold else Color.White.copy(alpha = 0.50f),
            textAlign = TextAlign.Center
        )
        if (names.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            names.forEach { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (highlight) 16.sp else 13.5.sp,
                        letterSpacing = 0.8.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

