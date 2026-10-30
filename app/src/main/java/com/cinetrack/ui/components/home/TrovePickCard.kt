package com.cinetrack.ui.components.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.data.model.GenreConstants
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.haze
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Colori tematici dello Scrigno
// ─────────────────────────────────────────────────────────────────────────────

private val TroveCyan = Color(0xFF2CE5B5)
private val TroveGold = Color(0xFFE2AC28)
private val TroveSeal = Color(0xFF0A1412)

// ─────────────────────────────────────────────────────────────────────────────
// Entry-point pubblico
// ─────────────────────────────────────────────────────────────────────────────

/**
 * "The Trove's Pick" — card con tre stati:
 *
 *  • **Locked**    → scrigno sigillato (nebbia Haze + overlay con indizi anonimi)
 *  • **Unsealing** → animazione di apertura 650 ms con burst di particelle
 *  • **Revealed**  → card completa nitida con apertura al dettaglio su bounceClick
 *
 * Il sigillo persiste per tutta la giornata (DataStore via ViewModel); a mezzanotte
 * si ripristina automaticamente con un nuovo pick.
 * Il pull-to-refresh NON tocca mai lo stato del sigillo.
 *
 * @param movie           Film/serie selezionato dal motore di raccomandazione.
 * @param isRevealedToday `true` se lo scrigno è già aperto oggi.
 * @param onReveal        Invocato al completamento dell'animazione — persiste lo stato.
 * @param onResetReveal   Invocato per ripristinare lo stato sigillato.
 * @param onMovieClick    Apre il dettaglio del film.
 * @param modifier        Modifier esterno.
 * @param onResolveLogo   Resolver lazy per il logo TMDB.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun TrovePickCard(
    movie: Movie,
    isRevealedToday: Boolean,
    onReveal: (movieId: Long) -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    onResetReveal: (() -> Unit)? = null,
    onResolveLogo: (suspend (Movie) -> String?)? = null
) {
    val context       = LocalContext.current
    val imageQuality  = LocalImageQuality.current
    val haptic        = LocalHapticFeedback.current
    val cardHazeState = remember { HazeState() }

    // ── Logo resolution ───────────────────────────────────────────────────────
    var localLogoPath by remember(movie.id, movie.logoPath) { mutableStateOf(movie.logoPath) }
    LaunchedEffect(movie.id, movie.logoPath) {
        if (localLogoPath.isNullOrEmpty() && onResolveLogo != null) {
            val fetched = onResolveLogo.invoke(movie)
            if (!fetched.isNullOrEmpty()) localLogoPath = fetched
        } else {
            localLogoPath = movie.logoPath
        }
    }

    // ── URL immagini ──────────────────────────────────────────────────────────
    val backdropUrl = buildTmdbImageUrl(movie.backdropPath ?: movie.posterPath, ImageType.BACKDROP, imageQuality)
    val posterUrl   = buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, imageQuality)

    // ── Stato apertura ────────────────────────────────────────────────────────
    var isUnsealing by rememberSaveable { mutableStateOf(false) }

    // Se isRevealedToday torna false (es. nuovo giorno), resettiamo isUnsealing
    LaunchedEffect(isRevealedToday) {
        if (!isRevealedToday) isUnsealing = false
    }

    // revealProgress: 0f = sigillato, 1f = rivelato
    val revealProgress by animateFloatAsState(
        targetValue = if (isRevealedToday || isUnsealing) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        finishedListener = { value ->
            if (value >= 1f && isUnsealing && !isRevealedToday) {
                onReveal(movie.id)
            }
        },
        label = "TroveReveal"
    )

    // Blur radius della nebbia teaser: 14dp → 0dp (sfoca testo e dettagli preservando le cromie del poster)
    val blurRadius = ((1f - revealProgress) * 14f).dp.coerceAtLeast(0.1.dp)

    // ── Entry animation ────────────────────────────────────────────────────────
    val hasAnimated = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasAnimated.value = true }

    val cardAlpha by animateFloatAsState(
        targetValue   = if (hasAnimated.value) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label         = "TroveEntryAlpha"
    )
    val cardTranslateY by animateFloatAsState(
        targetValue   = if (hasAnimated.value) 0f else 60f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label         = "TroveEntryY"
    )

    // ── Metadati per l'overlay "sigillato" ────────────────────────────────────
    val currentLang = context.resources.configuration.locales[0].language
    val year = (movie.releaseDate ?: movie.firstAirDate)?.take(4) ?: ""
    val genres = movie.genreIds?.mapNotNull { id ->
        val list        = if (movie.mediaType == "tv") GenreConstants.TV_GENRES else GenreConstants.MOVIE_GENRES
        val defaultName = list.find { it.id == id }?.name ?: ""
        GenreConstants.getLocalizedName(id, currentLang, defaultName).takeIf { it.isNotBlank() }
    }?.take(2) ?: emptyList()

    val matchScore = movie.matchScore
    val rating     = movie.voteAverage
    val titleText  = (movie.title ?: movie.name ?: "").uppercase()
    val activeLogo = localLogoPath ?: movie.logoPath

    // ─────────────────────────────────────────────────────────────────────────
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha        = cardAlpha
                translationY = cardTranslateY
            }
    ) {
        // Intestazione sezione
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter            = androidx.compose.ui.res.painterResource(id = R.drawable.ic_sparkle),
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = stringResource(R.string.home_section_trove_pick),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            // Pulsante "Risigilla" — visibile solo quando lo scrigno è già stato aperto oggi
            if (isRevealedToday && onResetReveal != null) {
                Spacer(Modifier.width(10.dp))
                androidx.compose.material3.IconButton(
                    onClick  = onResetReveal,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.LockReset,
                        contentDescription = stringResource(R.string.home_trove_pick_reset),
                        tint               = Color.White.copy(alpha = 0.55f),
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ── Card principale ──────────────────────────────────────────────────
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .graphicsLayer { clip = false },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(255.dp)
                    .padding(horizontal = 16.dp)
                    .bounceClick(
                        scaleDown = 0.96f,
                        onLongClick = {
                            if (!isRevealedToday && !isUnsealing) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isUnsealing = true
                            }
                        }
                    ) {
                        if (isRevealedToday) {
                            val movieWithLogo = if (!localLogoPath.isNullOrEmpty() && movie.logoPath == null) {
                                movie.copy().apply { this.logoPath = localLogoPath }
                            } else movie
                            onMovieClick(movieWithLogo)
                        } else {
                            // Feedback tattile al tap rapido per ricordare la pressione prolungata
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        RoundedCornerShape(32.dp)
                    )
            ) {

                // ── Layer 1: Card reale con blur progressivo ─────────────────
                if (revealProgress < 0.99f) {
                    val sealedStyle = remember(blurRadius, revealProgress) {
                        HazeBlurStyle {
                            blurRadius(blurRadius)
                            noiseFactor((0.02f * (1f - revealProgress)).coerceAtLeast(0f))
                            colorEffects(
                                listOf(
                                    HazeColorEffect.tint(
                                        TroveSeal.copy(alpha = (0.22f * (1f - revealProgress)).coerceAtLeast(0f))
                                    )
                                )
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeBlur(
                                input = HazeInput.Content,
                                style = sealedStyle,
                                performanceMode = HazePerformanceMode.Default
                            )
                    ) {
                        ActualTroveCardContent(
                            movie         = movie,
                            posterUrl     = posterUrl,
                            backdropUrl   = backdropUrl,
                            activeLogo    = activeLogo,
                            titleText     = titleText,
                            year          = year,
                            genres        = genres,
                            matchScore    = matchScore,
                            rating        = rating,
                            cardHazeState = cardHazeState,
                            modifier      = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    ActualTroveCardContent(
                        movie         = movie,
                        posterUrl     = posterUrl,
                        backdropUrl   = backdropUrl,
                        activeLogo    = activeLogo,
                        titleText     = titleText,
                        year          = year,
                        genres        = genres,
                        matchScore    = matchScore,
                        rating        = rating,
                        cardHazeState = cardHazeState,
                        modifier      = Modifier.fillMaxSize()
                    )
                }

                // ── Layer 2: Overlay scrigno sigillato ───────────────────────
                if (revealProgress < 0.99f) {
                    TroveSealedOverlay(
                        affinityPct = matchScore,
                        year        = year,
                        genres      = genres,
                        alpha       = (1f - revealProgress).coerceIn(0f, 1f)
                    )
                }

                // ── Layer 3: Burst di particelle durante la transizione ───────
                if (revealProgress in 0.01f..0.98f) {
                    TroveUnsealBurstCanvas(progress = revealProgress)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Contenuto reale della card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActualTroveCardContent(
    movie: Movie,
    posterUrl: String?,
    backdropUrl: String?,
    activeLogo: String?,
    titleText: String,
    year: String,
    genres: List<String>,
    matchScore: Int?,
    rating: Double?,
    cardHazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier) {
        // Backdrop + gradienti come sorgente Haze
        Box(modifier = Modifier.fillMaxSize().haze(cardHazeState)) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backdropUrl)
                    .crossfade(true)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black.copy(alpha = 0.80f),
                            0.40f to Color.Black.copy(alpha = 0.42f),
                            0.75f to Color.Transparent,
                            1.00f to Color.Transparent
                        )
                    )
                )
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black.copy(alpha = 0.58f),
                            0.22f to Color.Black.copy(alpha = 0.22f),
                            0.42f to Color.Transparent,
                            0.55f to Color.Black.copy(alpha = 0.60f),
                            0.75f to Color.Black.copy(alpha = 0.92f),
                            1.00f to Color.Black
                        )
                    )
                )
            )
        }

        // Poster + colonna dettagli
        Row(
            modifier              = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Poster a sinistra
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(posterUrl)
                    .allowHardware(false)
                    .crossfade(true)
                    .build(),
                contentDescription = movie.title ?: movie.name,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .width(136.dp)
                    .fillMaxHeight()
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
            )

            // Colonna dettagli
            Column(
                modifier            = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Badge "SCELTO PER TE"
                Box(
                    modifier = Modifier
                        .hazeGlass(
                            state          = cardHazeState,
                            shape          = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            blurRadius     = 16.dp,
                            borderWidth    = 0.5.dp,
                            borderColor    = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        )
                        .padding(horizontal = 9.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text          = stringResource(R.string.home_trove_pick_badge).uppercase(),
                        color         = MaterialTheme.colorScheme.primary,
                        fontSize      = 10.5.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        maxLines      = 1
                    )
                }

                // Blocco inferiore: logo/titolo, anno•generi, score+rating
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Logo o titolo testuale
                    if (!activeLogo.isNullOrEmpty()) {
                        val logoUrl = buildTmdbImageUrl(activeLogo, ImageType.LOGO, com.cinetrack.util.ImageQuality.MEDIUM)
                        AsyncImage(
                            model              = ImageRequest.Builder(context).data(logoUrl).crossfade(true).build(),
                            contentDescription = movie.title ?: movie.name,
                            contentScale       = ContentScale.Fit,
                            alignment          = Alignment.CenterStart,
                            modifier           = Modifier.fillMaxWidth(0.92f).heightIn(min = 28.dp, max = 46.dp)
                        )
                    } else if (titleText.isNotEmpty()) {
                        Text(
                            text          = titleText,
                            color         = Color.White,
                            fontFamily    = FontFamily.Serif,
                            fontSize      = 21.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            lineHeight    = 25.sp,
                            maxLines      = 2,
                            overflow      = TextOverflow.Ellipsis
                        )
                    }

                    // Anno • Generi
                    val metaText = remember(year, genres) {
                        buildAnnotatedString {
                            if (year.isNotEmpty()) withStyle(SpanStyle(color = Color.White.copy(alpha = 0.95f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp)) { append(year) }
                            if (year.isNotEmpty() && genres.isNotEmpty()) withStyle(SpanStyle(color = Color.White.copy(alpha = 0.40f), fontSize = 12.5.sp)) { append("  •  ") }
                            if (genres.isNotEmpty()) withStyle(SpanStyle(color = Color.White.copy(alpha = 0.65f), fontWeight = FontWeight.Medium, fontSize = 12.sp)) { append(genres.joinToString(", ")) }
                        }
                    }
                    if (metaText.isNotEmpty()) {
                        Text(text = metaText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Spacer(Modifier.height(2.dp))

                    // Match score + rating
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (matchScore != null && matchScore > 0) {
                            Box(
                                modifier = Modifier
                                    .hazeGlass(
                                        state          = cardHazeState,
                                        shape          = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        blurRadius     = 16.dp,
                                        borderWidth    = 0.5.dp,
                                        borderColor    = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("${matchScore}%", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.match_score).uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
                                }
                            }
                        }
                        if (rating != null && rating > 0.0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(
                                    painter            = androidx.compose.ui.res.painterResource(id = R.drawable.ic_star_piena),
                                    contentDescription = null,
                                    tint               = Color(0xFFFFC107),
                                    modifier           = Modifier.size(11.dp)
                                )
                                Text("%.1f".format(java.util.Locale.US, rating), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Overlay "Scrigno Sigillato"
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TroveSealedOverlay(
    affinityPct: Int?,
    year: String,
    genres: List<String>,
    alpha: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TroveSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label         = "TroveSweepAngle"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 0.96f,
        targetValue   = 1.04f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label         = "TrovePulse"
    )

    Box(
        modifier         = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
        contentAlignment = Alignment.Center
    ) {
        // Vignettatura radiale: centro trasparente per far risplendere le tonalità del poster,
        // con bordi satinati scuri per massimo contrasto
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.50f to Color.Black.copy(alpha = 0.30f),
                            1.00f to Color.Black.copy(alpha = 0.72f)
                        ),
                        radius = 700f
                    )
                )
        )

        // Struttura a 3 livelli: Badge in cima, Sigillo + CTA al centro, Pillole indizio in basso
        Column(
            modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP: Badge di stato dello scrigno
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .border(0.5.dp, TroveCyan.copy(alpha = 0.40f), CircleShape)
                    .padding(horizontal = 11.dp, vertical = 4.5.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        painter            = androidx.compose.ui.res.painterResource(id = R.drawable.ic_sparkle),
                        contentDescription = null,
                        tint               = TroveCyan,
                        modifier           = Modifier.size(11.dp)
                    )
                    Text(
                        text          = stringResource(R.string.home_trove_pick_sealed_badge).uppercase(),
                        color         = TroveCyan,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                }
            }

            // 2. CENTER: Sigillo mistico + CTA
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier         = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Alone morbido radiale pulsante
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .graphicsLayer {
                                scaleX = pulseScale * 1.05f
                                scaleY = pulseScale * 1.05f
                            }
                            .background(
                                Brush.radialGradient(
                                    listOf(TroveCyan.copy(alpha = 0.24f), Color.Transparent)
                                ),
                                CircleShape
                            )
                    )

                    // Anello orbitale sottile a 360° continuo rotante (nessuna discontinuità visiva)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer { rotationZ = sweepAngle }
                            .border(
                                width = 1.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        TroveCyan.copy(alpha = 0.85f),
                                        TroveGold.copy(alpha = 0.65f),
                                        TroveCyan.copy(alpha = 0.15f),
                                        TroveCyan.copy(alpha = 0.85f)
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // Gemma centrale con finitura glass scura
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF0D2E28), Color(0xFF05100E))
                                ),
                                CircleShape
                            )
                            .border(1.dp, TroveCyan.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter            = androidx.compose.ui.res.painterResource(id = R.drawable.ic_sparkle),
                            contentDescription = null,
                            tint               = TroveCyan,
                            modifier           = Modifier.size(26.dp)
                        )
                    }
                }

                Text(
                    text          = stringResource(R.string.home_trove_pick_sealed_cta),
                    color         = Color.White,
                    fontSize      = 13.5.sp,
                    fontWeight    = FontWeight.SemiBold,
                    textAlign     = TextAlign.Center,
                    letterSpacing = 0.1.sp
                )
            }

            // 3. BOTTOM: Pillole indizio localizzate
            TroveHintPills(affinityPct = affinityPct, year = year, genres = genres)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pillole indizio
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TroveHintPills(
    affinityPct: Int?,
    year: String,
    genres: List<String>
) {
    val matchScoreLabel = stringResource(R.string.match_score)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        if (affinityPct != null && affinityPct > 0) {
            HintPill(
                label = stringResource(R.string.home_trove_pick_affinity, affinityPct, matchScoreLabel),
                tint  = TroveCyan
            )
        }
        if (year.isNotEmpty()) {
            HintPill(year, Color.White.copy(alpha = 0.85f))
        }
        if (genres.isNotEmpty()) {
            HintPill(genres.joinToString(" • "), Color.White.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun HintPill(label: String, tint: Color) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), CircleShape)
            .border(0.5.dp, tint.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 11.dp, vertical = 4.5.dp)
    ) {
        Text(
            text       = label,
            color      = tint,
            fontSize   = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Canvas burst (particelle radiali durante l'apertura)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TroveUnsealBurstCanvas(progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val numParticles = 18
        val center       = Offset(size.width / 2f, size.height / 2f)
        val maxRadius    = size.minDimension * 0.48f
        val fadeIn       = (progress / 0.35f).coerceIn(0f, 1f)
        val fadeOut      = ((1f - progress) / 0.50f).coerceIn(0f, 1f)
        val particleAlpha = fadeIn * fadeOut

        for (i in 0 until numParticles) {
            val angle  = (2.0 * PI * i / numParticles).toFloat()
            val dist   = maxRadius * progress
            val x      = center.x + dist * cos(angle)
            val y      = center.y + dist * sin(angle)
            val color  = if (i % 3 == 0) TroveGold else TroveCyan
            val radius = (3.dp.toPx() * (1f - progress * 0.5f)).coerceAtLeast(0.5.dp.toPx())
            drawCircle(
                color  = color.copy(alpha = particleAlpha * 0.85f),
                radius = radius,
                center = Offset(x, y)
            )
        }

        // Onda d'urto luminosa
        val ringAlpha  = (fadeIn * fadeOut * 0.55f).coerceIn(0f, 1f)
        val ringRadius = maxRadius * progress
        drawCircle(
            color  = TroveCyan.copy(alpha = ringAlpha),
            radius = ringRadius,
            center = center,
            style  = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}
