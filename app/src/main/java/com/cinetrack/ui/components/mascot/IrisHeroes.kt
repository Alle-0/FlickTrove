package com.cinetrack.ui.components.mascot

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import kotlinx.coroutines.delay

/**
 * Indicatore essenziale mostrato durante la ricerca Mood in corso:
 * invece di una card pesante ("malloppone"), IRIS (o lo Sparkle AI) si posiziona
 * semplicemente in alto al centro mentre ruota / esegue il focus hunting.
 */
@Composable
fun IrisSearchingHero(
    query: String,
    modifier: Modifier = Modifier,
    isMoodAiMode: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isMoodAiMode) {
            // Sfera generativa animata per Cinema Mood AI
            AiSparkleOrb(
                size = 72.dp,
                isSearching = true,
                accentColor = MaterialTheme.colorScheme.primary
            )
        } else {
            // IRIS pura in alto al centro mentre ruota le lamelle ed esegue il focus hunting
            IrisMascot(
                mood = IrisMood.SEARCHING,
                size = 72.dp,
                accentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Testata cinematografica mostrata in cima ai risultati di ricerca Mood completati:
 * IRIS (o lo Sparkle AI) rimane in cima alla griglia e:
 * - A riposo / consultazione risultati (!isSearchFocused): guarda SEMPRE in basso (gazeBiasY = +1.0f)
 *   e perlustra orizzontalmente verso destra e sinistra (gazeBiasX) leggendo le card dei risultati
 *   con pause curiose e ammiccamenti soddisfatti (mood = DELIGHTED).
 * - Quando l'utente clicca sulla barra di ricerca o digita (isSearchFocused): alza lo sguardo in alto
 *   (gazeBiasY = -0.95f) e traccia il cursore (gazeBiasX) osservando l'utente che scrive!
 */
@Composable
fun IrisResultsHero(
    modifier: Modifier = Modifier,
    hasResults: Boolean = true,
    isLoading: Boolean = false,
    isMoodAiMode: Boolean = false,
    isSearchFocused: Boolean = false,
    cursorPosition: Int = -1,
    query: String = "",
    scrollInertia: Float = 0f,
    isFocusHunting: Boolean = false,
    inspectedCardXRatio: Float? = null,
    inspectedCardYRatio: Float? = null,
    size: Dp = 68.dp,
    onIrisLongClick: (() -> Unit)? = null
) {
    val targetSize = when {
        hasResults -> 64.dp
        isLoading -> 70.dp
        else -> 78.dp
    }
    val targetTopPadding = when {
        hasResults -> 2.dp
        isLoading -> 8.dp
        else -> 16.dp
    }
    val targetBottomPadding = when {
        hasResults -> 2.dp
        isLoading -> 6.dp
        else -> 8.dp
    }

    val animatedSize by animateDpAsState(
        targetValue = targetSize,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iris_hero_size"
    )
    val animatedTopPadding by animateDpAsState(
        targetValue = targetTopPadding,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iris_hero_top_padding"
    )
    val animatedBottomPadding by animateDpAsState(
        targetValue = targetBottomPadding,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iris_hero_bottom_padding"
    )

    // Calcolo dello sguardo orizzontale verso la barra di ricerca quando l'utente digita
    val textGazeX = remember(query, cursorPosition) {
        if (query.isEmpty()) 0f
        else {
            val effectiveCursor = if (cursorPosition in 0..query.length) cursorPosition else query.length
            val progress = (effectiveCursor / 28f).coerceIn(0f, 1f)
            -0.70f + (progress * 1.40f)
        }
    }

    // Perlustrazione dinamica e viva dei risultati sottostanti:
    // IRIS guarda in basso verso le card dei film (asse Y tra 0.60f e 0.95f),
    // perlustrando locandina, rating, titolo e affinità in modo armonico e curioso.
    // Periodicamente compie un salto visivo (saccade cinematografico) verso un'altra card,
    // mantenendo viva e scattante la mascotte invece di lasciarla statica.
    var resultsGazeX by remember { mutableFloatStateOf(0f) }
    var resultsGazeY by remember { mutableFloatStateOf(0.78f) }
    var isSaccade by remember { mutableStateOf(false) }
    var lastInspectedX by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isFocusHunting, inspectedCardXRatio) {
        if (isFocusHunting && inspectedCardXRatio != null) {
            lastInspectedX = inspectedCardXRatio
        }
    }

    val isWriting = query.isNotBlank()

    LaunchedEffect(hasResults, isFocusHunting) {
        if (hasResults && !isFocusHunting) {
            // Se eravamo in focus hunting su una card, manteniamo con calma quella posizione
            // prima di riprendere dolcemente la perlustrazione, eliminando ogni ritorno a scatto
            if (lastInspectedX != 0f) {
                resultsGazeX = lastInspectedX
                delay(700L)
            }
            // Waypoint 2D che simulano la lettura naturale di una card di film
            val scanWaypoints = listOf(
                // 1. Locandina del film (in basso a sinistra)
                Triple(-0.35f, 0.74f, 1300L..1900L),
                // 2. Rating a stella dorata sulla locandina
                Triple(-0.28f, 0.92f, 1100L..1600L),
                // 3. Titolo del film e anno (centro-alto)
                Triple(0.04f, 0.62f, 1400L..2000L),
                // 4. Tag affinità semantica e percentuale (centro-destra)
                Triple(0.34f, 0.82f, 1300L..1900L),
                // 5. Visione d'insieme al centro
                Triple(0.08f, 0.72f, 1100L..1600L),
                // 6. Locandina card adiacente / colonna successiva
                Triple(-0.22f, 0.84f, 1200L..1800L)
            )
            var waypointIdx = if (lastInspectedX != 0f) {
                scanWaypoints.indices.minByOrNull { kotlin.math.abs(scanWaypoints[it].first - lastInspectedX) } ?: 0
            } else 0
            var stepsSinceSnap = 0
            while (true) {
                // Ogni 3-5 passi compie un salto scattoso (saccade) catturata da un altro film
                val shouldSnap = stepsSinceSnap >= 3 && (stepsSinceSnap >= 5 || (1..100).random() <= 50)

                if (shouldSnap) {
                    stepsSinceSnap = 0
                    val currentX = resultsGazeX
                    // Salto scattoso verso il polo opposto
                    val snapTargetX = if (currentX >= 0f) {
                        ((-42..-26).random() / 100f)
                    } else {
                        ((26..42).random() / 100f)
                    }
                    val snapTargetY = ((65..92).random() / 100f)
                    isSaccade = true
                    resultsGazeX = snapTargetX
                    resultsGazeY = snapTargetY
                    // Riprende il ciclo dal waypoint più vicino alla nuova posizione
                    waypointIdx = if (snapTargetX < 0f) 0 else 3
                    // Sosta curiosa sul punto agganciato (1.2s - 1.8s)
                    delay((1200L..1800L).random())
                    isSaccade = false
                } else {
                    stepsSinceSnap++
                    isSaccade = false
                    val (baseX, baseY, delayRange) = scanWaypoints[waypointIdx % scanWaypoints.size]
                    waypointIdx++
                    val jitterX = ((-3..3).random() / 100f)
                    val jitterY = ((-2..2).random() / 100f)
                    resultsGazeX = (baseX + jitterX).coerceIn(-0.48f, 0.48f)
                    resultsGazeY = (baseY + jitterY).coerceIn(0.55f, 0.98f)
                    // Si sofferma con ritmo vivo e naturale (1.1s - 2.0s)
                    delay(delayRange.random())
                }
            }
        }
    }

    val effectiveGazeBiasY = when {
        isFocusHunting -> inspectedCardYRatio ?: 1.0f // Messa a fuoco selettiva diretta sulla card premuta (Feature 6)
        isLoading -> 0f
        hasResults -> resultsGazeY // Perlustra le card a schermo esplorando sia X che Y
        isWriting && isSearchFocused -> -0.90f // Guarda in alto verso la barra di ricerca SOLO mentre si scrive senza risultati a schermo
        else -> 0f
    }
    val effectiveGazeBiasX = when {
        isFocusHunting -> inspectedCardXRatio ?: lastInspectedX
        isLoading -> 0f
        hasResults -> resultsGazeX // Perlustra le card a schermo
        isWriting && isSearchFocused -> textGazeX
        else -> 0f
    }
    val effectiveMood = when {
        isLoading -> IrisMood.SEARCHING
        hasResults -> IrisMood.DELIGHTED // Soddisfatta di aver trovato i film, li osserva ed esplora
        isWriting && isSearchFocused -> IrisMood.TYPING
        else -> IrisMood.IDLE
    }

    val backgroundBrush = if (hasResults) {
        Brush.verticalGradient(
            0.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            0.60f to MaterialTheme.colorScheme.background.copy(alpha = 0.60f),
            1.0f to Color.Transparent
        )
    } else {
        SolidColor(Color.Transparent)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundBrush)
            .padding(top = animatedTopPadding, bottom = animatedBottomPadding),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isMoodAiMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                 scaleIn(initialScale = 0.94f, animationSpec = tween(240, easing = FastOutSlowInEasing)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                        scaleOut(targetScale = 0.94f, animationSpec = tween(180, easing = FastOutSlowInEasing))
                    ).using(SizeTransform(clip = false))
            },
            label = "hero_mascot_switch",
            contentAlignment = Alignment.Center
        ) { isAi ->
            if (isAi) {
                AiSparkleOrb(
                    size = animatedSize,
                    isSearching = isLoading,
                    accentColor = MaterialTheme.colorScheme.primary,
                    glowPadding = animatedSize * 0.38f
                )
            } else {
                IrisMascot(
                    mood = effectiveMood,
                    size = animatedSize,
                    gazeBiasX = effectiveGazeBiasX,
                    gazeBiasY = effectiveGazeBiasY,
                    isSaccade = isSaccade || isFocusHunting,
                    scrollInertia = scrollInertia,
                    isFocusHunting = isFocusHunting,
                    accentColor = MaterialTheme.colorScheme.primary,
                    glowPadding = animatedSize * 0.38f,
                    onLongClick = onIrisLongClick
                )
            }
        }
    }
}

/**
 * Stato vuoto cinematografico con la mascotte IRIS.
 * Quando la lista o ricerca è vuota, IRIS guarda dolcemente in basso verso il messaggio.
 */
@Composable
fun IrisEmptyState(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.search_mood_iris_no_results),
    description: String? = stringResource(R.string.search_mood_iris_no_results_desc),
    query: String = "",
    isMoodAiMode: Boolean = false,
    gazeBiasY: Float = 0.52f,
    size: Dp = 84.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isMoodAiMode) {
            AiSparkleOrb(
                size = size,
                isSearching = false,
                accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.60f)
            )
        } else {
            IrisMascot(
                mood = IrisMood.EMPTY,
                size = size,
                gazeBiasY = gazeBiasY,
                accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )

        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.50f),
                    lineHeight = 16.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 290.dp)
            )
        }
    }
}
