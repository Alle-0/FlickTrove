package com.cinetrack.ui.components.badge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.repository.BadgeRepository
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.theme.FlickTroveTheme
import com.cinetrack.ui.utils.bounceClick
import kotlinx.coroutines.launch

/**
 * Contenuto del Modale di Dettaglio del Trofeo, ospitato dentro GlassmorphicModal autentico.
 * Rispetta la concentricità 32dp/16dp, icone nude e purezza cromatica del Tier.
 */
@Composable
fun TrophyDetailContent(
    item: TrophyRoomItemUi,
    onClose: () -> Unit,
    onVerifySupporter: (suspend (String) -> BadgeRepository.SupporterVerificationResult)? = null,
    onMovieClick: ((Long, String) -> Unit)? = null,
    onResolveUnlockingMovies: (suspend (String) -> List<com.cinetrack.data.model.Movie>)? = null,
    onResolveUnlockingMovie: (suspend (String) -> com.cinetrack.data.model.Movie?)? = null
) {
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    val maxHeight = (configuration.screenHeightDp * 0.85f).dp

    var unlockingMovies by remember(item.badge.id, item.isUnlocked) { mutableStateOf<List<com.cinetrack.data.model.Movie>>(emptyList()) }
    LaunchedEffect(item.badge.id, item.isUnlocked) {
        if (item.isUnlocked) {
            if (onResolveUnlockingMovies != null) {
                unlockingMovies = onResolveUnlockingMovies(item.badge.id)
            } else if (onResolveUnlockingMovie != null) {
                val single = onResolveUnlockingMovie(item.badge.id)
                unlockingMovies = if (single != null) listOf(single) else emptyList()
            }
        }
    }

    // Auto-scroll when keyboard opens so the email input and verification button are fully visible
    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    val isImeVisible = imeInsets.getBottom(density) > 0

    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            kotlinx.coroutines.delay(120)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 16.dp)
    ) {
        // Pulsante di Chiusura Concentrico a 16dp
        Box(modifier = Modifier.align(Alignment.TopEnd).zIndex(10f)) {
            ModalCloseButton(onClose = onClose)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            val availableTiers = item.badge.progressiveSteps.keys.sortedBy { it.ordinal }
            val firstTier = availableTiers.firstOrNull() ?: item.currentTier
            val targetTier = if (!item.isUnlocked) {
                firstTier
            } else {
                availableTiers.firstOrNull { it.ordinal > item.currentTier.ordinal }
            }
            val isMaxTier = item.badge.category == BadgeTypeCategory.PROGRESSIVE_TIERS &&
                item.isUnlocked && (targetTier == null || item.currentTier == availableTiers.lastOrNull())

            // Pillola Formato Tier sopra al badge circolare
            if (item.isUnlocked) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(item.currentTier.primaryColor.copy(alpha = 0.14f))
                        .border(1.dp, item.currentTier.primaryColor.copy(alpha = 0.38f), CircleShape)
                        .padding(horizontal = 11.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = item.currentTier.localizedFormatName().uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        ),
                        color = item.currentTier.primaryColor
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 11.dp, vertical = 3.5.dp)
                ) {
                    val tierLabel = targetTier?.localizedFormatName()?.uppercase() ?: stringResource(R.string.trophy_tier_1)
                    Text(
                        text = if (item.badge.category == BadgeTypeCategory.LOST_REEL_SECRET) stringResource(R.string.trophy_cat_lost_reel)
                        else stringResource(R.string.trophy_to_unlock_tier, tierLabel),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        ),
                        color = if (item.badge.category == BadgeTypeCategory.LOST_REEL_SECRET) Color(0xFF818CF8) else Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Emblema grande del trofeo con il colore del tier
            FlickTroveBadgeEmblem(
                tier = if (item.isUnlocked) item.currentTier else (targetTier ?: item.currentTier),
                iconKind = item.badge.iconKind,
                isUnlocked = item.isUnlocked,
                size = 110.dp,
                enableInteractiveParallax = true,
                enablePeriodicGleam = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Titolo del Trofeo (centrato direttamente sotto l'emblema)
            Text(
                text = item.badge.localizedTitle(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Chip meta affiancate: Prima Data di sblocco e poi Rarità
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.isUnlocked && !item.unlockedDate.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.07f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = formatLocalizedTrophyDate(item.unlockedDate),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.trophy_rarity_pct, item.badge.rarityPercent),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider sottile
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.07f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Paragrafo Descrittivo
            val desc = if (!item.isUnlocked && item.badge.secretHint != null) {
                item.badge.localizedSecretHint() ?: item.badge.localizedDescription()
            } else {
                item.badge.localizedDescription()
            }

            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                ),
                color = Color.White.copy(alpha = 0.70f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Card Cinefila: Film sbloccante / Film della sessione
            if (unlockingMovies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (unlockingMovies.size == 1) {
                                val single = unlockingMovies.first()
                                Modifier.bounceClick(
                                    scaleDown = 0.96f,
                                    enabled = onMovieClick != null,
                                    onClick = {
                                        onClose()
                                        onMovieClick?.invoke(single.id, single.mediaType.ifBlank { "movie" })
                                    }
                                )
                            } else Modifier
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, item.currentTier.primaryColor.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (unlockingMovies.size == 1) {
                        val movie = unlockingMovies.first()
                        val displayTitle = movie.title ?: movie.name ?: ""
                        val releaseYear = movie.releaseDate?.take(4) ?: movie.firstAirDate?.take(4)
                        val subtitleText = if (item.badge.id == "colossal" && (movie.runtime ?: 0) > 0) {
                            val rt = movie.runtime ?: 0
                            val h = rt / 60
                            val m = rt % 60
                            if (h > 0) "${h}h ${if (m < 10) "0$m" else "$m"}m" else "${m}m"
                        } else {
                            releaseYear
                        }
                        val fullPosterUrl = movie.posterPath?.let { path ->
                            if (path.startsWith("http")) path else "https://image.tmdb.org/t/p/w185$path"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Poster miniatura con aspect ratio cinema (2:3)
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                if (!fullPosterUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = fullPosterUrl,
                                        contentDescription = displayTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_videocamera),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp).align(Alignment.Center)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.trophy_unlocked_with).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    ),
                                    color = item.currentTier.primaryColor
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = displayTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (!subtitleText.isNullOrBlank()) {
                                    Text(
                                        text = subtitleText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color.White.copy(alpha = 0.50f)
                                    )
                                }
                            }

                            if (onMovieClick != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_right),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.40f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else {
                        // Multi-film (es. Doppio Spettacolo con i 2 film della giornata)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.trophy_unlocked_with).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = item.currentTier.primaryColor,
                                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                            )

                            unlockingMovies.forEachIndexed { index, movie ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .height(1.dp)
                                            .background(Color.White.copy(alpha = 0.08f))
                                    )
                                }

                                val displayTitle = movie.title ?: movie.name ?: ""
                                val releaseYear = movie.releaseDate?.take(4) ?: movie.firstAirDate?.take(4)
                                val fullPosterUrl = movie.posterPath?.let { path ->
                                    if (path.startsWith("http")) path else "https://image.tmdb.org/t/p/w185$path"
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .bounceClick(
                                            scaleDown = 0.97f,
                                            enabled = onMovieClick != null,
                                            onClick = {
                                                onClose()
                                                onMovieClick?.invoke(movie.id, movie.mediaType.ifBlank { "movie" })
                                            }
                                        )
                                        .padding(vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(32.dp)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                    ) {
                                        if (!fullPosterUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = fullPosterUrl,
                                                contentDescription = displayTitle,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_videocamera),
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.3f),
                                                modifier = Modifier.size(16.dp).align(Alignment.Center)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayTitle,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (!releaseYear.isNullOrBlank()) {
                                            Text(
                                                text = releaseYear,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = Color.White.copy(alpha = 0.50f)
                                            )
                                        }
                                    }

                                    if (onMovieClick != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_right),
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.40f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val hasExtraSections = item.badge.id == "badge_executive_producer" || item.badge.category == BadgeTypeCategory.PROGRESSIVE_TIERS
            if (hasExtraSections) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4b. SEZIONE SPECIALE: Il Produttore Esecutivo (Supporto & Verifica Donazione per Registrati e Guest)
            if (item.badge.id == "badge_executive_producer") {
                ExecutiveProducerDonationSection(
                    isUnlocked = item.isUnlocked,
                    onVerifySupporter = onVerifySupporter,
                    onExpandedChange = { expanded ->
                        if (expanded) {
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(160)
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        }
                    },
                    onFocusField = {
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(160)
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                )
                if (item.badge.category == BadgeTypeCategory.PROGRESSIVE_TIERS) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 5. PROGRESSIONE CHIARA: quanto ho visto e quanto serve per il prossimo traguardo
            if (item.badge.category == BadgeTypeCategory.PROGRESSIVE_TIERS) {
                val currentVal = item.badge.progressCurrent
                val unit = item.badge.localizedUnit()

                val nextTarget = if (!item.isUnlocked) {
                    item.badge.progressiveSteps[firstTier]?.targetThreshold ?: item.badge.progressTarget
                } else {
                    targetTier?.let { item.badge.progressiveSteps[it]?.targetThreshold }
                }
                val needed = if (nextTarget != null) (nextTarget - currentVal).coerceAtLeast(0) else 0
                val progressFraction = if (nextTarget != null && nextTarget > 0) {
                    (currentVal.toFloat() / nextTarget).coerceIn(0f, 1f)
                } else 1f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (isMaxTier) item.currentTier.primaryColor.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isMaxTier) stringResource(R.string.trophy_max_target_reached) else stringResource(R.string.trophy_next_goal),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = if (isMaxTier) item.currentTier.primaryColor else Color.White.copy(alpha = 0.50f)
                            )

                            if (targetTier != null) {
                                Text(
                                    text = "${(progressFraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = if (item.isUnlocked) targetTier.primaryColor else Color(0xFF10B981)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Valore numerico
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$currentVal",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            if (nextTarget != null) {
                                Text(
                                    text = " / $nextTarget $unit",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                                )
                            } else {
                                val completedSuffix = if (unit.equals("ore", ignoreCase = true) || unit.endsWith("e", ignoreCase = true)) {
                                    stringResource(R.string.trophy_completed_suffix_fem)
                                } else {
                                    stringResource(R.string.trophy_completed_suffix)
                                }
                                Text(
                                    text = if (unit.isNotBlank()) " $unit $completedSuffix" else " $completedSuffix",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Barra di avanzamento
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressFraction)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (item.isUnlocked && targetTier != null) {
                                            Brush.horizontalGradient(
                                                listOf(item.currentTier.primaryColor, targetTier.primaryColor)
                                            )
                                        } else if (item.isUnlocked) {
                                            Brush.horizontalGradient(
                                                listOf(item.currentTier.primaryColor, item.currentTier.secondaryColor)
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                listOf(Color.White.copy(alpha = 0.20f), Color(0xFF10B981))
                                            )
                                        }
                                    )
                            )
                        }

                        // Testo di supporto — senza ripetere il target già indicato sopra
                        if (!isMaxTier && targetTier != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val targetLabel = if (!item.isUnlocked) {
                                stringResource(R.string.trophy_to_unlock_trophy_tier, targetTier.localizedFormatName())
                            } else {
                                targetTier.localizedFormatName()
                            }
                            Text(
                                text = stringResource(R.string.trophy_needed_count, needed, unit, targetLabel),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                ),
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        } else if (isMaxTier) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.trophy_max_milestone_desc),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                ),
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }
                    }
                }

                // Timeline dei Formati: visibile SOLO se il trofeo è ancora in corso di scalata (!isMaxTier)
                if (!isMaxTier) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val maxStepTarget = (availableTiers.lastOrNull()?.let { item.badge.progressiveSteps[it]?.targetThreshold }) ?: item.badge.progressTarget
                    BadgeTierEvolutionTrack(
                        currentTier = item.currentTier,
                        isUnlocked = item.isUnlocked,
                        progressCurrent = item.badge.progressCurrent,
                        maxTarget = maxStepTarget,
                        stepProgressFraction = progressFraction,
                        tierSteps = if (availableTiers.isNotEmpty()) availableTiers else listOf(
                            PreviewBadgeTier.SUPER_8,
                            PreviewBadgeTier.MM_16,
                            PreviewBadgeTier.MM_35,
                            PreviewBadgeTier.MM_70,
                            PreviewBadgeTier.THE_FINAL_CUT
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            val hasTrailingBottomSection = (item.badge.category == BadgeTypeCategory.PROGRESSIVE_TIERS && !isMaxTier)
            if (hasTrailingBottomSection) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}





@Preview(
    name = "Modale Dettaglio Trofeo Ufficiale (Concentric 32dp)",
    showBackground = true,
    backgroundColor = 0xFF090A10,
    device = "spec:width=411dp,height=891dp"
)
@Composable
private fun TrophyDetailModalPreview() {
    FlickTroveTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            TrophyDetailContent(
                item = OFFICIAL_TROPHY_ROOM_CATALOG.first(),
                onClose = {}
            )
        }
    }
}
