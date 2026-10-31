package com.cinetrack.ui.components.home

import android.graphics.BlurMaskFilter
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
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
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
import kotlinx.coroutines.launch
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

    // ── Coroutine scope ────────────────────────────────────────────────────────
    val scope = rememberCoroutineScope()

    // ── Carica progressiva con pressione prolungata (Hold-to-Charge) ────────────
    var isHolding by remember { mutableStateOf(false) }
    val chargeProgress = remember { Animatable(0f) }

    // Coordinate dinamiche per agganciare l'origine dell'esplosione al centro esatto del sigillo
    var cardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var medallionCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var burstCenterOffset by remember { mutableStateOf(Offset.Zero) }

    // ── Stato apertura ────────────────────────────────────────────────────────
    var isUnsealing by rememberSaveable(movie.id) { mutableStateOf(false) }

    // Se isRevealedToday torna false (es. risigilla o nuovo giorno), resettiamo isUnsealing e la carica
    LaunchedEffect(isRevealedToday) {
        if (!isRevealedToday) {
            isUnsealing = false
            chargeProgress.snapTo(0f)
        }
    }

    LaunchedEffect(isHolding, isUnsealing) {
        if (isUnsealing) return@LaunchedEffect
        if (isHolding) {
            val remaining = ((1f - chargeProgress.value) * 1100).toInt().coerceAtLeast(10)
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
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        }
    }

    // revealProgress: 0f = sigillato, 1f = rivelato (650ms con easing deciso)
    val revealProgress by animateFloatAsState(
        targetValue = if (isRevealedToday || isUnsealing) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        finishedListener = { value ->
            if (value >= 1f && isUnsealing && !isRevealedToday) {
                isUnsealing = false
                onReveal(movie.id)
            }
        },
        label = "TroveReveal"
    )

    // Micro-click aptici alle tappe della messa a fuoco (33% e 66%) per simulare gli scatti meccanici della ghiera
    var lastHapticMilestone by remember { mutableStateOf(0) }
    LaunchedEffect(isHolding) {
        if (!isHolding) {
            lastHapticMilestone = 0
        }
    }
    LaunchedEffect(chargeProgress.value) {
        val currentMilestone = (chargeProgress.value * 3).toInt()
        if (isHolding && currentMilestone > lastHapticMilestone && currentMilestone in 1..2) {
            lastHapticMilestone = currentMilestone
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Blur radius: durante la carica prolungata l'obiettivo mette progressivamente a fuoco la scena (da 16dp a ~4.5dp).
    // All'esplosione dell'onda d'urto (revealProgress) il blur si azzera nitidamente a 0dp.
    val focusBlurFactor = (1f - (chargeProgress.value * 0.72f)).coerceIn(0.28f, 1f)
    val unsealBlurFactor = (1f - revealProgress)
    val blurRadius = (16f * focusBlurFactor * unsealBlurFactor).dp.coerceAtLeast(0.1.dp)

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
                    onClick  = {
                        isUnsealing = false
                        scope.launch { chargeProgress.snapTo(0f) }
                        onResetReveal()
                    },
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
                    .onGloballyPositioned {
                        cardCoordinates = it
                        val medallion = medallionCoordinates
                        if (it.isAttached && medallion != null && medallion.isAttached) {
                            val centerInMedallion = Offset(
                                medallion.size.width / 2f,
                                medallion.size.height / 2f
                            )
                            burstCenterOffset = it.localPositionOf(medallion, centerInMedallion)
                        }
                    }
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
                                        Color(0xFF0A0A0A).copy(alpha = (0.22f * (1f - revealProgress)).coerceAtLeast(0f))
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
                        onMedallionPositioned = { coords ->
                            medallionCoordinates = coords
                            val card = cardCoordinates
                            if (card != null && card.isAttached && coords.isAttached) {
                                val centerInMedallion = Offset(
                                    coords.size.width / 2f,
                                    coords.size.height / 2f
                                )
                                burstCenterOffset = card.localPositionOf(coords, centerInMedallion)
                            }
                        },
                        modifier       = Modifier.fillMaxSize()
                    )
                }

                // ── Layer 3: Esplosione a 36 particelle + onde d'urto luminose ─────────────
                if (revealProgress in 0.01f..0.99f) {
                    TroveUnsealBurstCanvas(
                        progress = revealProgress,
                        burstCenter = burstCenterOffset
                    )
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
// Overlay "Obiettivo Cinema" con messa a fuoco a pressione prolungata (Cinema Lens)
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
    onMedallionPositioned: (LayoutCoordinates) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Respiro ottico a riposo (3.2 secondi) che dà vita all'obiettivo prima del tocco
    val infiniteTransition = rememberInfiniteTransition(label = "TroveLensIdle")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TroveLensIdlePulse"
    )

    Box(
        modifier         = modifier.graphicsLayer {
            this.alpha  = alpha
            this.scaleX = scale
            this.scaleY = scale
        },
        contentAlignment = Alignment.Center
    ) {
        // Sottile vignettatura radiale di sala al centro per far risaltare l'ottica e il teaser dal blur
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black.copy(alpha = 0.50f),
                            0.55f to Color.Black.copy(alpha = 0.35f),
                            1.00f to Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier            = Modifier.offset(y = (-8).dp)
        ) {
            // Obiettivo Cinema con ghiera di messa a fuoco
            Box(
                modifier         = Modifier
                    .size(76.dp)
                    .onGloballyPositioned { onMedallionPositioned(it) },
                contentAlignment = Alignment.Center
            ) {
                // 0. Bolle gravitazionali che convergono venendo risucchiate nel fuoco dell'obiettivo
                if (chargeProgress > 0.005f) {
                    TroveGravitationalBubbles(
                        chargeProgress = chargeProgress,
                        modifier       = Modifier
                            .size(76.dp)
                            .graphicsLayer { clip = false }
                    )
                }

                // 1. Alone luminoso dell'ottica: delicato respiro in idle, fascio focalizzato durante la carica
                val baseAuraAlpha = if (chargeProgress > 0.01f) {
                    (chargeProgress * 0.38f).coerceIn(0f, 0.38f)
                } else {
                    0.08f + idlePulse * 0.12f
                }
                val auraSize = if (chargeProgress > 0.01f) {
                    (52 + chargeProgress * 28).dp
                } else {
                    (56 + idlePulse * 10).dp
                }
                Box(
                    modifier = Modifier
                        .size(auraSize)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    primaryColor.copy(alpha = baseAuraAlpha),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                // 2. Corpo lente in vetro ottico multistrato con finitura antiriflesso cinema
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to Color(0xFF1C2028).copy(alpha = 0.94f),
                                    0.55f to Color(0xFF0F1116).copy(alpha = 0.92f),
                                    0.90f to Color(0xFF060709).copy(alpha = 0.96f),
                                    1.00f to primaryColor.copy(alpha = 0.18f)
                                )
                            ),
                            CircleShape
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isHolding) 0.50f else 0.28f),
                                    primaryColor.copy(alpha = if (isHolding) 0.60f else 0.20f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // 3. Ghiera di messa a fuoco con tacche di calibrazione e anello di carica progressivo
                Canvas(modifier = Modifier.size(68.dp)) {
                    val strokeWidth = 2.5.dp.toPx()
                    val radius = (size.minDimension / 2f) - strokeWidth / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Tacche millimetriche della ghiera di messa a fuoco (12 tacche a 30°)
                    // Ruotano leggermente mentre si mette a fuoco
                    val ringRotation = chargeProgress * 45f
                    val tickRadiusInner = radius - 4.dp.toPx()
                    val tickRadiusOuter = radius - 1.5.dp.toPx()
                    val tickColor = Color.White.copy(alpha = if (isHolding) 0.35f else 0.18f)

                    for (i in 0 until 12) {
                        val angleDeg = (i * 30f) + ringRotation
                        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                        val cosA = cos(angleRad)
                        val sinA = sin(angleRad)
                        val isMajor = (i % 3 == 0)
                        val rIn = if (isMajor) tickRadiusInner - 1.5.dp.toPx() else tickRadiusInner

                        drawLine(
                            color       = if (isMajor) primaryColor.copy(alpha = 0.50f) else tickColor,
                            start       = Offset(center.x + rIn * cosA, center.y + rIn * sinA),
                            end         = Offset(center.x + tickRadiusOuter * cosA, center.y + tickRadiusOuter * sinA),
                            strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx(),
                            cap         = StrokeCap.Round
                        )
                    }

                    // Anello interno di guida del diaframma
                    drawCircle(
                        color  = Color.White.copy(alpha = 0.06f),
                        radius = radius * 0.62f,
                        center = center,
                        style  = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )

                    // Arco di messa a fuoco attivo (Focus Arc)
                    if (chargeProgress > 0.005f) {
                        val sweep = chargeProgress * 360f

                        drawArc(
                            color      = primaryColor,
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

                        // Testa luminosa dell'arco che segue la rotazione della messa a fuoco
                        val angleRad = Math.toRadians((-90.0 + sweep)).toFloat()
                        val dotX = center.x + radius * cos(angleRad)
                        val dotY = center.y + radius * sin(angleRad)

                        drawCircle(
                            color  = primaryColor.copy(alpha = 0.60f),
                            radius = 5.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                        drawCircle(
                            color  = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                }

                // 4. Icona scintillio al centro dell'ottica: brilla ed entra in pieno fuoco (> 0.85f)
                val iconScale = 1f + chargeProgress * 0.12f
                Icon(
                    painter            = androidx.compose.ui.res.painterResource(id = R.drawable.ic_sparkle),
                    contentDescription = null,
                    tint               = if (chargeProgress > 0.85f) Color.White else primaryColor,
                    modifier           = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }

            // ── Esca psicologica: micro-indizio metadati (Affinità • Anno • Genere) ──
            val hasTeaserData = (matchScore != null && matchScore > 0) || year.isNotEmpty() || genres.isNotEmpty()
            if (hasTeaserData) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF161A22).copy(alpha = 0.90f),
                                    Color(0xFF0C0E13).copy(alpha = 0.86f)
                                )
                            ),
                            CircleShape
                        )
                        .border(
                            0.5.dp,
                            if (isHolding) primaryColor.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.16f),
                            CircleShape
                        )
                        .padding(horizontal = 12.dp, vertical = 4.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (matchScore != null && matchScore > 0) {
                            Text(
                                text = "${matchScore}% ${stringResource(R.string.match_score)}",
                                color = primaryColor,
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
                color = if (isHolding) primaryColor else Color.White.copy(alpha = 0.82f),
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

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    // Palette cromatica armonica basata sul tema dell'app (Primary, Secondary, Tertiary, White)
    val bubbles = remember(primary, secondary, tertiary) {
        val colors = listOf(
            primary,
            secondary,
            primary.copy(alpha = 0.85f),
            tertiary,
            Color.White.copy(alpha = 0.90f)
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
        val deformThreshold = 52.dp.toPx() // Sotto i 52dp si deforma venendo risucchiata verso il centro
        val diskRadiusPx = 34.dp.toPx() // Raggio del disco centrale da 68dp
        val borderFadeDistPx = 6.dp.toPx() // Distanza di transizione sfocatura / scomparsa bordo
        val minDist = 14.dp.toPx() // Scompare sotto il disco scuro

        // Dimensioni e posizione della pillola teaser dei metadati
        val pillCenterY = center.y + 58.dp.toPx()
        val pillHalfWidth = 92.dp.toPx()
        val pillCornerRadius = 14.dp.toPx()

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

            // Deformazione solo in prossimità del centro (spaghettificazione gravitazionale sotto i 52dp):
            val stretchFactor: Float
            val compressFactor: Float
            if (dist > deformThreshold) {
                stretchFactor = 1.0f
                compressFactor = 1.0f
            } else {
                val t = ((deformThreshold - dist) / (deformThreshold - minDist)).coerceIn(0f, 1f)
                stretchFactor = 1.0f + t * (1.8f + chargeProgress * 0.7f)
                compressFactor = (1.0f - t * 0.40f).coerceAtLeast(0.55f)
            }

            // Calcolo sfocatura:
            // 1. Sotto il disco centrale
            val diskDist = kotlin.math.hypot(bx - center.x, by - center.y)
            val diskT = ((diskRadiusPx + borderFadeDistPx - diskDist) / borderFadeDistPx).coerceIn(0f, 1f)

            // 2. Sotto la pillola teaser dei metadati (capsula)
            val pillDx = (kotlin.math.abs(bx - center.x) - (pillHalfWidth - pillCornerRadius)).coerceAtLeast(0f)
            val pillDy = kotlin.math.abs(by - pillCenterY)
            val pillDist = kotlin.math.hypot(pillDx, pillDy)
            val pillT = ((pillCornerRadius + borderFadeDistPx - pillDist) / borderFadeDistPx).coerceIn(0f, 1f)

            // Fattore unificato di sfocatura (attivo sia sotto il disco che sotto la pillola teaser)
            val blurT = maxOf(diskT, pillT)
            val strokeAlpha = (1f - blurT).coerceIn(0f, 1f)

            val r = bubble.baseRadiusDp.dp.toPx()
            val fadeIn = (progress / 0.12f).coerceIn(0f, 1f)
            val fadeOut = ((1f - progress) / 0.10f).coerceIn(0f, 1f)
            val alpha = (fadeIn * fadeOut * masterAlpha * (0.60f + chargeProgress * 0.40f)).coerceIn(0f, 1f)

            if (alpha <= 0.01f) continue

            val rx = r * stretchFactor
            val ry = r * compressFactor
            val angleDeg = if (stretchFactor != 1.0f) Math.toDegrees(angle.toDouble()).toFloat() else 0f

            rotate(degrees = angleDeg, pivot = Offset(bx, by)) {
                // Se la bolla passa sotto il disco o sotto la pillola teaser, applica sfocatura gaussiana diffusa
                if (blurT > 0.04f) {
                    drawIntoCanvas { canvas ->
                        val blurPx = (6.dp.toPx() + blurT * 12.dp.toPx())
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = bubble.color.toArgb()
                            this.alpha = (alpha * (0.35f + blurT * 0.35f) * 255).toInt().coerceIn(0, 255)
                            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                        }
                        canvas.nativeCanvas.drawOval(
                            bx - rx * (1.1f + blurT * 0.4f),
                            by - ry * (1.1f + blurT * 0.4f),
                            bx + rx * (1.1f + blurT * 0.4f),
                            by + ry * (1.1f + blurT * 0.4f),
                            paint
                        )
                    }
                }

                // Bagliore soffice
                val glowExpand = 1.35f + blurT * 0.8f
                drawOval(
                    color   = bubble.color.copy(alpha = alpha * (0.20f + blurT * 0.15f)),
                    topLeft = Offset(bx - rx * glowExpand, by - ry * glowExpand),
                    size    = Size(rx * glowExpand * 2f, ry * glowExpand * 2f)
                )

                // Interno traslucido sfumato
                val bodyExpand = 1f + blurT * 0.4f
                drawOval(
                    brush   = Brush.radialGradient(
                        colors = listOf(
                            bubble.color.copy(alpha = alpha * (0.18f + blurT * 0.15f)),
                            bubble.color.copy(alpha = alpha * 0.45f * (1f - blurT * 0.6f)),
                            Color.Transparent
                        ),
                        center = Offset(bx, by),
                        radius = rx * bodyExpand
                    ),
                    topLeft = Offset(bx - rx * bodyExpand, by - ry * bodyExpand),
                    size    = Size(rx * bodyExpand * 2f, ry * bodyExpand * 2f)
                )

                // Bordo netto della bolla: sfuma fino a sparire COMPLETAMENTE sotto il disco o la pillola
                if (strokeAlpha > 0.01f) {
                    drawOval(
                        color   = bubble.color.copy(alpha = alpha * 0.90f * strokeAlpha),
                        topLeft = Offset(bx - rx, by - ry),
                        size    = Size(rx * 2f, ry * 2f),
                        style   = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.3.dp.toPx() * strokeAlpha)
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
private fun TroveUnsealBurstCanvas(
    progress: Float,
    burstCenter: Offset = Offset.Zero
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = if (burstCenter != Offset.Zero) {
            burstCenter
        } else {
            Offset(size.width / 2f, size.height * 0.30f)
        }
        val maxDist = size.maxDimension * 0.65f

        // 1. Flash centrale a dissolvenza rapida
        val flashAlpha = ((1f - progress / 0.28f).coerceIn(0f, 1f)) * 0.80f
        if (flashAlpha > 0f) {
            val flashRadius = 45.dp.toPx() * (1f + progress * 2.5f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = flashAlpha),
                        primary.copy(alpha = flashAlpha * 0.5f),
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
                color  = primary.copy(alpha = wave1Alpha),
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
                    color  = secondary.copy(alpha = wave2Alpha),
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
                0 -> primary
                1 -> secondary
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
