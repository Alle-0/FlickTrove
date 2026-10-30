package com.cinetrack.ui.components.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.pow
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

    // ── Carica progressiva con pressione prolungata (Hold-to-Charge) ────────────
    var isHolding by remember { mutableStateOf(false) }
    val chargeProgress = remember { Animatable(0f) }

    LaunchedEffect(isHolding, isUnsealing) {
        if (isUnsealing) return@LaunchedEffect
        if (isHolding) {
            val remaining = ((1f - chargeProgress.value) * 750).toInt().coerceAtLeast(10)
            chargeProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = remaining, easing = LinearEasing)
            )
            if (chargeProgress.value >= 1f && isHolding && !isUnsealing) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isUnsealing = true
            }
        } else {
            chargeProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            )
        }
    }

    // revealProgress: 0f = sigillato, 1f = rivelato (650ms con easing deciso)
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

    // Blur radius della nebbia: da 16dp a 0dp man mano che l'onda d'urto spazza via il velo
    val blurRadius = ((1f - revealProgress) * 16f).dp.coerceAtLeast(0.1.dp)

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

    // ── Metadati della card ──────────────────────────────────────────────────
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
            // Pulsante "Risigilla" — visibile esclusivamente nelle build di debug per test interni
            if (com.cinetrack.BuildConfig.DEBUG && isRevealedToday && onResetReveal != null) {
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
            val cardScale = if (isRevealedToday) 1f else (1f - chargeProgress.value * 0.04f)

            val cardTouchModifier = if (isRevealedToday) {
                Modifier.bounceClick(scaleDown = 0.96f) {
                    val movieWithLogo = if (!localLogoPath.isNullOrEmpty() && movie.logoPath == null) {
                        movie.copy().apply { this.logoPath = localLogoPath }
                    } else movie
                    onMovieClick(movieWithLogo)
                }
            } else {
                Modifier
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                    }
                    .pointerInput(isUnsealing) {
                        if (isUnsealing) return@pointerInput
                        detectTapGestures(
                            onPress = {
                                isHolding = true
                                tryAwaitRelease()
                                isHolding = false
                            }
                        )
                    }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(255.dp)
                    .padding(horizontal = 16.dp)
                    .then(cardTouchModifier)
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        RoundedCornerShape(32.dp)
                    )
            ) {

                // ── Layer 1: Vera card sotto (sempre presente e perfettamente posizionata) ──
                if (revealProgress < 0.99f) {
                    val sealedStyle = remember(blurRadius, revealProgress) {
                        HazeBlurStyle {
                            blurRadius(blurRadius)
                            noiseFactor((0.02f * (1f - revealProgress)).coerceAtLeast(0f))
                            colorEffects(
                                listOf(
                                    HazeColorEffect.tint(
                                        TroveSeal.copy(alpha = (0.20f * (1f - revealProgress)).coerceAtLeast(0f))
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
                            detailsAlpha  = if (isRevealedToday) 1f else (revealProgress * 1.5f).coerceIn(0f, 1f),
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

                // ── Layer 2: Sigillo magico che si carica e scompare nell'esplosione ───────
                if (revealProgress < 0.99f) {
                    val overlayAlpha = if (isUnsealing) {
                        (1f - revealProgress * 3.5f).coerceIn(0f, 1f)
                    } else 1f

                    val overlayScale = if (isUnsealing) {
                        1f + revealProgress * 0.35f
                    } else {
                        1f + chargeProgress.value * 0.10f
                    }

                    TroveHoldToUnsealOverlay(
                        chargeProgress = chargeProgress.value,
                        isHolding      = isHolding,
                        alpha          = overlayAlpha,
                        scale          = overlayScale,
                        matchScore     = matchScore,
                        year           = year,
                        genres         = genres,
                        modifier       = Modifier.fillMaxSize()
                    )
                }

                // ── Layer 3: Esplosione a 36 particelle + onde d'urto luminose ─────────────
                if (revealProgress in 0.01f..0.99f) {
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
    detailsAlpha: Float = 1f,
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
                modifier            = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer { alpha = detailsAlpha },
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
// Overlay "Sigillo Mistico" con anello di carica a pressione prolungata
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TroveHoldToUnsealOverlay(
    chargeProgress: Float,
    isHolding: Boolean,
    alpha: Float,
    scale: Float,
    matchScore: Int?,
    year: String,
    genres: List<String>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier         = modifier.graphicsLayer {
            this.alpha  = alpha
            this.scaleX = scale
            this.scaleY = scale
        },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier            = Modifier.offset(y = (-8).dp)
        ) {
            // Sigillo circolare con indicatore di carica energetica
            Box(
                modifier         = Modifier.size(86.dp),
                contentAlignment = Alignment.Center
            ) {
                // 0. Bolle gravitazionali che convergono e si deformano venendo risucchiate sotto il cerchio centrale
                if (chargeProgress > 0.005f) {
                    TroveGravitationalBubbles(
                        chargeProgress = chargeProgress,
                        modifier       = Modifier
                            .size(86.dp)
                            .graphicsLayer { clip = false }
                    )
                }

                // Backing satinato scuro
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                )

                // Alone radiale di accumulo energetico che cresce con la carica
                val auraAlpha = 0.16f + chargeProgress * 0.55f
                val auraSize = (50 + chargeProgress * 36).dp
                Box(
                    modifier = Modifier
                        .size(auraSize)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    TroveCyan.copy(alpha = auraAlpha),
                                    TroveGold.copy(alpha = auraAlpha * 0.45f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                // Canvas dell'anello di carica (Track 360° + Arco attivo + Glow Tip)
                Canvas(modifier = Modifier.size(76.dp)) {
                    val strokeWidth = 3.dp.toPx()
                    val radius = (size.minDimension / 2f) - strokeWidth / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // 1. Traccia passiva
                    drawCircle(
                        color  = Color.White.copy(alpha = 0.14f),
                        radius = radius,
                        center = center,
                        style  = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                    )

                    // 2. Arco attivo continuo e uniforme: il colore dell'energia si scalda progressivamente
                    // da TroveCyan a TroveGold man mano che si carica, senza tagli netti (niente discontinuità di shader)
                    if (chargeProgress > 0.005f) {
                        val sweep = chargeProgress * 360f
                        val energyColor = lerp(
                            TroveCyan,
                            TroveGold,
                            (chargeProgress * 1.15f).coerceIn(0f, 1f)
                        )

                        drawArc(
                            color      = energyColor,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter  = false,
                            topLeft    = Offset(strokeWidth / 2f, strokeWidth / 2f),
                            size       = Size(radius * 2, radius * 2),
                            style      = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidth,
                                cap   = StrokeCap.Round
                            )
                        )

                        // 3. Testa luminosa dell'arco (glow tip che segue fluidamente la rotazione)
                        val angleRad = Math.toRadians((-90.0 + sweep)).toFloat()
                        val dotX = center.x + radius * cos(angleRad)
                        val dotY = center.y + radius * sin(angleRad)

                        drawCircle(
                            color  = energyColor.copy(alpha = 0.65f),
                            radius = 6.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                        drawCircle(
                            color  = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                }

                // Gemma centrale con finitura smeraldo
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF0F322B), Color(0xFF061411))
                            ),
                            CircleShape
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    TroveCyan.copy(alpha = 0.65f + chargeProgress * 0.35f),
                                    TroveGold.copy(alpha = 0.40f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter            = androidx.compose.ui.res.painterResource(id = R.drawable.ic_sparkle),
                        contentDescription = null,
                        tint               = if (chargeProgress > 0.85f) Color.White else TroveCyan,
                        modifier           = Modifier.size(26.dp)
                    )
                }
            }

            // ── Esca psicologica: micro-indizio metadati (Affinità • Anno • Genere) ──
            val hasTeaserData = (matchScore != null && matchScore > 0) || year.isNotEmpty() || genres.isNotEmpty()
            if (hasTeaserData) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.52f), CircleShape)
                        .border(
                            0.5.dp,
                            if (isHolding) TroveCyan.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.14f),
                            CircleShape
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = if (isHolding) TroveCyan else TroveGold.copy(alpha = 0.90f),
                            modifier = Modifier.size(11.dp)
                        )

                        if (matchScore != null && matchScore > 0) {
                            Text(
                                text = "${matchScore}% ${stringResource(R.string.match_score)}",
                                color = TroveCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                        }

                        if (year.isNotEmpty()) {
                            if (matchScore != null && matchScore > 0) {
                                Text(
                                    text = "•",
                                    color = Color.White.copy(alpha = 0.35f),
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = year,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (genres.isNotEmpty()) {
                            Text(
                                text = "•",
                                color = Color.White.copy(alpha = 0.35f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = genres.first(),
                                color = Color.White.copy(alpha = 0.70f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ── Call-To-Action: "Tieni premuto per scoprire il Trove di oggi" ──
            Text(
                text = stringResource(R.string.home_trove_pick_sealed_cta),
                color = if (isHolding) TroveCyan else Color.White.copy(alpha = 0.78f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                letterSpacing = 0.25.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bolle gravitazionali che viaggiano dall'esterno verso il centro e si deformano
// ─────────────────────────────────────────────────────────────────────────────

private data class TroveGravitationalBubble(
    val baseAngle: Float,
    val spawnDistDp: Float,
    val speed: Float,
    val phaseOffset: Float,
    val baseRadiusDp: Float,
    val color: Color
)

@Composable
private fun TroveGravitationalBubbles(
    chargeProgress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TroveBubblesInf")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TroveBubblePhase"
    )

    // Palette cromatica gioiello: Cyan, Oro, Smeraldo, Azzurro, Viola cosmico
    val bubbles = remember {
        val colors = listOf(
            TroveCyan,
            TroveGold,
            Color(0xFF38BDF8), // Azzurro elettrico
            Color(0xFF10B981), // Smeraldo
            Color(0xFFA78BFA)  // Viola stellare
        )
        val count = 10 // Pochi elementi nitidi e visibili, non ammassati
        List(count) { i ->
            val angle = (2.0 * PI * i / count).toFloat() + (((i * 17) % 5) - 2) * 0.10f
            val spawnDist = 135f + ((i * 13) % 4) * 8f // 135dp .. 159dp (lontano, nei bordi della card)
            val speed = 0.90f + ((i * 7) % 5) * 0.05f
            val offset = (i.toFloat() / count) // Distribuite uniformemente nel tempo
            val radius = 5.0f + (i % 3) * 1.5f // 5.0dp .. 8.0dp (leggere e discrete)
            val color = colors[i % colors.size]
            TroveGravitationalBubble(
                baseAngle    = angle,
                spawnDistDp  = spawnDist,
                speed        = speed,
                phaseOffset  = offset,
                baseRadiusDp = radius,
                color        = color
            )
        }
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f) // Baricentro perfetto del medaglione
        val deformThreshold = 62.dp.toPx() // Oltre 62dp (fuori dal disco da 43dp di raggio) la bolla è 100% sferica
        val minDist = 18.dp.toPx() // Scompare ben sotto il cerchio scuro (raggio 41dp)

        // All'aumentare della carica col tocco, il flusso accelera naturalmente
        val speedBoost = 1f + chargeProgress * 2.0f
        val masterAlpha = (chargeProgress / 0.08f).coerceIn(0f, 1f)

        for (bubble in bubbles) {
            val progress = ((phase * bubble.speed * speedBoost + bubble.phaseOffset) % 1f)
            val spawnDistPx = bubble.spawnDistDp.dp.toPx()

            // Traiettoria dall'esterno (spawnDist) verso il centro:
            // Procede a velocità fluida, accelerando solo nell'ultimo tratto
            val travelProgress = progress.pow(1.3f)
            val dist = spawnDistPx - (spawnDistPx - minDist) * travelProgress

            if (dist < minDist) continue

            val angle = bubble.baseAngle
            val bx = center.x + dist * cos(angle)
            val by = center.y + dist * sin(angle)

            // Deformazione solo in prossimità del centro (spaghettificazione gravitazionale sotto i 62dp):
            val stretchFactor: Float
            val compressFactor: Float
            if (dist > deformThreshold) {
                // In viaggio nello spazio aperto: la bolla è PERFETTAMENTE TONDA!
                stretchFactor = 1.0f
                compressFactor = 1.0f
            } else {
                // Sotto i 62dp (vicino all'anello): si deforma e si allunga come una goccia verso il centro
                val t = ((deformThreshold - dist) / (deformThreshold - minDist)).coerceIn(0f, 1f)
                stretchFactor = 1.0f + t * (1.8f + chargeProgress * 0.7f)
                compressFactor = (1.0f - t * 0.40f).coerceAtLeast(0.55f)
            }

            val r = bubble.baseRadiusDp.dp.toPx()
            val fadeIn = (progress / 0.12f).coerceIn(0f, 1f)
            val fadeOut = ((1f - progress) / 0.10f).coerceIn(0f, 1f)
            val alpha = (fadeIn * fadeOut * masterAlpha * (0.60f + chargeProgress * 0.40f)).coerceIn(0f, 1f)

            if (alpha <= 0.01f) continue

            if (stretchFactor == 1.0f) {
                // ── 1. Bolla sferica trasparente e nitida (in volo verso il centro) ──
                // Bagliore soffice
                drawCircle(
                    color  = bubble.color.copy(alpha = alpha * 0.22f),
                    radius = r * 1.5f,
                    center = Offset(bx, by)
                )
                // Interno traslucido tipo sapone / energia
                drawCircle(
                    brush  = Brush.radialGradient(
                        colors = listOf(
                            bubble.color.copy(alpha = alpha * 0.15f),
                            bubble.color.copy(alpha = alpha * 0.40f)
                        ),
                        center = Offset(bx, by),
                        radius = r
                    ),
                    radius = r,
                    center = Offset(bx, by)
                )
                // Bordo nitido e brillante della bolla
                drawCircle(
                    color  = bubble.color.copy(alpha = alpha * 0.90f),
                    radius = r,
                    center = Offset(bx, by),
                    style  = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.3.dp.toPx())
                )
                // Riflesso speculare bianco
                drawCircle(
                    color  = Color.White.copy(alpha = alpha * 0.95f),
                    radius = r * 0.28f,
                    center = Offset(bx - r * 0.32f, by - r * 0.32f)
                )
            } else {
                // ── 2. Bolla deformata a goccia (risucchiata sotto il medaglione) ──
                val rx = r * stretchFactor
                val ry = r * compressFactor
                val angleDeg = Math.toDegrees(angle.toDouble()).toFloat()

                rotate(degrees = angleDeg, pivot = Offset(bx, by)) {
                    // Bagliore deformato
                    drawOval(
                        color   = bubble.color.copy(alpha = alpha * 0.20f),
                        topLeft = Offset(bx - rx * 1.35f, by - ry * 1.35f),
                        size    = Size(rx * 2.7f, ry * 2.7f)
                    )
                    // Interno traslucido
                    drawOval(
                        brush   = Brush.radialGradient(
                            colors = listOf(
                                bubble.color.copy(alpha = alpha * 0.18f),
                                bubble.color.copy(alpha = alpha * 0.45f)
                            ),
                            center = Offset(bx, by),
                            radius = rx
                        ),
                        topLeft = Offset(bx - rx, by - ry),
                        size    = Size(rx * 2f, ry * 2f)
                    )
                    // Bordo deformato
                    drawOval(
                        color   = bubble.color.copy(alpha = alpha * 0.90f),
                        topLeft = Offset(bx - rx, by - ry),
                        size    = Size(rx * 2f, ry * 2f),
                        style   = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.3.dp.toPx())
                    )
                    // Riflesso speculare nella testa della goccia rivolta verso il centro
                    drawCircle(
                        color  = Color.White.copy(alpha = alpha * 0.95f),
                        radius = ry * 0.32f,
                        center = Offset(bx - rx * 0.35f, by)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Canvas burst (esplosione cinematografica con 36 particelle + onde d'urto)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TroveUnsealBurstCanvas(progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxDist = size.maxDimension * 0.65f

        // 1. Flash centrale a dissolvenza rapida
        val flashAlpha = ((1f - progress / 0.28f).coerceIn(0f, 1f)) * 0.80f
        if (flashAlpha > 0f) {
            val flashRadius = 45.dp.toPx() * (1f + progress * 2.5f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = flashAlpha),
                        TroveCyan.copy(alpha = flashAlpha * 0.5f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = flashRadius
                ),
                radius = flashRadius,
                center = center
            )
        }

        // 2. Doppia onda d'urto ad anello
        val wave1Dist = maxDist * (progress.pow(0.7f))
        val wave1Alpha = ((1f - progress) * 0.9f).coerceIn(0f, 1f)
        if (wave1Alpha > 0f) {
            drawCircle(
                color  = TroveCyan.copy(alpha = wave1Alpha),
                radius = wave1Dist,
                center = center,
                style  = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = (3.dp.toPx() * (1f - progress * 0.5f)).coerceAtLeast(1.dp.toPx())
                )
            )
        }

        val delayedProgress = (progress - 0.08f).coerceAtLeast(0f) / 0.92f
        if (delayedProgress > 0f) {
            val wave2Dist = maxDist * 0.85f * (delayedProgress.pow(0.8f))
            val wave2Alpha = ((1f - delayedProgress) * 0.6f).coerceIn(0f, 1f)
            if (wave2Alpha > 0f) {
                drawCircle(
                    color  = TroveGold.copy(alpha = wave2Alpha),
                    radius = wave2Dist,
                    center = center,
                    style  = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = (2.dp.toPx() * (1f - delayedProgress)).coerceAtLeast(0.8.dp.toPx())
                    )
                )
            }
        }

        // 3. Particelle stellari ed energetiche organiche (36 particelle con velocità e dimensioni variegate)
        val numParticles = 36
        val particleFade = ((1f - progress.pow(1.3f))).coerceIn(0f, 1f)
        for (i in 0 until numParticles) {
            val baseAngle = (2.0 * PI * i / numParticles).toFloat()
            val jitter = (((i * 17) % 11) - 5) * 0.035f
            val angle = baseAngle + jitter

            val speedFactor = 0.45f + (((i * 23) % 10) / 10f) * 0.65f
            val dist = maxDist * speedFactor * (progress.pow(0.72f))

            val x = center.x + dist * cos(angle)
            val y = center.y + dist * sin(angle)

            val pRadius = ((1.5f + (i % 4) * 0.9f).dp.toPx() * (1f - progress * 0.55f)).coerceAtLeast(0.5.dp.toPx())
            val pColor = when (i % 3) {
                0 -> TroveCyan
                1 -> TroveGold
                else -> Color.White
            }

            drawCircle(
                color  = pColor.copy(alpha = particleFade * 0.90f),
                radius = pRadius,
                center = Offset(x, y)
            )
        }
    }
}
