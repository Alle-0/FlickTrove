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
import androidx.compose.runtime.CompositionLocalProvider
import com.cinetrack.ui.LocalBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.cinetrack.R
import kotlinx.coroutines.launch
import com.cinetrack.data.model.GenreConstants
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.CinematicHaptics
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
                CinematicHaptics.unsealThud(context)
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

    // Micro-click aptici a ghiera meccanica di precisione (8 step progressivi verso il fuoco ottico)
    var lastHapticMilestone by remember { mutableStateOf(0) }
    LaunchedEffect(isHolding) {
        if (!isHolding) {
            lastHapticMilestone = 0
        }
    }
    LaunchedEffect(chargeProgress.value) {
        val totalSteps = 8
        val currentMilestone = (chargeProgress.value * totalSteps).toInt()
        if (isHolding && currentMilestone > lastHapticMilestone && currentMilestone in 1 until totalSteps) {
            lastHapticMilestone = currentMilestone
            if (currentMilestone == totalSteps / 2) {
                CinematicHaptics.mechanicalClick(context, intensity = 0.65f)
            } else {
                val stepIntensity = 0.35f + (currentMilestone.toFloat() / totalSteps) * 0.45f
                CinematicHaptics.mechanicalTick(context, intensity = stepIntensity)
            }
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
    val cardBackdrop = rememberLayerBackdrop { drawContent() }

    Box(modifier = modifier) {
        // Backdrop + gradienti come sorgente Haze e LayerBackdrop
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(cardBackdrop).haze(cardHazeState)) {
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
            // Gradiente scuro morbido dal basso verso l'alto (senza bande grigie a metà schermo)
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.30f to Color.Transparent,
                            0.60f to Color.Black.copy(alpha = 0.60f),
                            0.82f to Color.Black.copy(alpha = 0.88f),
                            1.00f to Color.Black
                        )
                    )
                )
            )
            // Gradiente scuro da sinistra a destra (sfuma dolcemente lasciando limpido il soggetto a destra)
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black.copy(alpha = 0.82f),
                            0.45f to Color.Black.copy(alpha = 0.50f),
                            0.75f to Color.Transparent,
                            1.00f to Color.Transparent
                        )
                    )
                )
            )
        }

        // Layout Opzione 3: Badge Unificato in Alto + Basso Pulito
        CompositionLocalProvider(LocalBackdrop provides cardBackdrop) {
            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .graphicsLayer { alpha = detailsAlpha },
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Badge unificato in alto a sinistra: [ 87% MATCH ] SCELTO PER TE
                val hasMatch = matchScore != null && matchScore > 0
                Box(
                    modifier = Modifier
                        .hazeGlass(
                            state          = cardHazeState,
                            shape          = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            blurRadius     = com.cinetrack.ui.theme.HazeStyles.GlassBlurRadius,
                            useRefraction  = true,
                            borderWidth    = 1.dp,
                            borderColor    = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
                        )
                    .padding(
                        start  = if (hasMatch) 4.dp else 10.dp,
                        end    = 10.dp,
                        top    = if (hasMatch) 3.5.dp else 4.5.dp,
                        bottom = if (hasMatch) 3.5.dp else 4.5.dp
                    )
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    if (hasMatch) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text          = "${matchScore}% " + stringResource(R.string.match_score).uppercase(),
                                color         = Color(0xFF07090E),
                                fontSize      = 9.5.sp,
                                fontWeight    = FontWeight.Black,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }

                    Text(
                        text          = stringResource(R.string.home_trove_pick_badge).uppercase(),
                        color         = MaterialTheme.colorScheme.primary,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 1.1.sp,
                        maxLines      = 1
                    )
                }
            }

            // Blocco inferiore: Logo o titolo + riga pura di metadati
            Column(
                modifier            = Modifier.fillMaxWidth(0.78f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Logo o titolo testuale
                if (!activeLogo.isNullOrEmpty()) {
                    val logoUrl = buildTmdbImageUrl(activeLogo, ImageType.LOGO, com.cinetrack.util.ImageQuality.MEDIUM)
                    AsyncImage(
                        model              = ImageRequest.Builder(context).data(logoUrl).crossfade(true).build(),
                        contentDescription = movie.title ?: movie.name,
                        contentScale       = ContentScale.Fit,
                        alignment          = Alignment.CenterStart,
                        modifier           = Modifier.fillMaxWidth().heightIn(min = 28.dp, max = 46.dp)
                    )
                } else if (titleText.isNotEmpty()) {
                    Text(
                        text          = titleText,
                        color         = Color.White,
                        fontFamily    = FontFamily.Serif,
                        fontSize      = 22.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        lineHeight    = 26.sp,
                        maxLines      = 2,
                        overflow      = TextOverflow.Ellipsis
                    )
                }

                // Metadati in riga pulita: Anno • Generi • ★ Voto (Zero pillole pesanti)
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (year.isNotEmpty()) {
                        Text(
                            text       = year,
                            color      = Color.White,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (year.isNotEmpty() && genres.isNotEmpty()) {
                        Text("•", color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp)
                    }

                    if (genres.isNotEmpty()) {
                        Text(
                            text       = genres.joinToString(", "),
                            color      = Color.White.copy(alpha = 0.72f),
                            fontSize   = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                    }

                    if ((year.isNotEmpty() || genres.isNotEmpty()) && rating != null && rating > 0.0) {
                        Text("•", color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp)
                    }

                    if (rating != null && rating > 0.0) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                painter            = androidx.compose.ui.res.painterResource(id = R.drawable.ic_star_piena),
                                contentDescription = null,
                                tint               = Color(0xFFFFC107),
                                modifier           = Modifier.size(11.dp)
                            )
                            Text("%.1f".format(java.util.Locale.US, rating), color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
}
