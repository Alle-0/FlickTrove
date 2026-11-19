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

    // Perlustrazione orizzontale dei risultati sottostanti:
    // IRIS guarda SEMPRE in basso verso i risultati (Y = 1.0f fisso e costante),
    // e perlustra in modo continuo e armonico (-0.30f ➔ -0.08f ➔ +0.18f ➔ +0.38f ➔ +0.10f ➔ -0.18f).
    // Occasionalmente (ogni 4-6 passi), compie un "salto scattoso" (saccade) verso l'estremo opposto,
    // catturata da una locandina o rating a sorpresa, per poi riprendere il ciclo armonico!
    var resultsGazeX by remember { mutableFloatStateOf(0f) }
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
                delay(900L)
            }
            val scanWaypoints = listOf(
                -0.30f, // Esamina la locandina a sinistra
                -0.08f, // Passa al titolo
                 0.18f, // Scivola verso il centro-destra
                 0.38f, // Esamina l'affinità e il rating a destra
                 0.10f, // Ritorno calmo al centro
                -0.18f  // Torna dolcemente verso sinistra
            )
            var waypointIdx = if (lastInspectedX != 0f) {
                scanWaypoints.indices.minByOrNull { kotlin.math.abs(scanWaypoints[it] - lastInspectedX) } ?: 0
            } else 0
            var stepsSinceSnap = 0
            while (true) {
                // Ogni 4-6 passi c'è l'occasione di un salto scattoso verso il polo opposto
                val shouldSnap = stepsSinceSnap >= 4 && (stepsSinceSnap >= 6 || (1..100).random() <= 45)

                if (shouldSnap) {
                    stepsSinceSnap = 0
                    val currentX = resultsGazeX
                    // Salto scattoso verso l'estremo opposto (saccade cinematografico)
                    val snapTarget = if (currentX >= 0f) {
                        ((-42..-32).random() / 100f)
                    } else {
                        ((32..42).random() / 100f)
                    }
                    isSaccade = true
                    resultsGazeX = snapTarget
                    // Riprende il ciclo armonico dal waypoint più vicino alla nuova posizione
                    waypointIdx = if (snapTarget < 0f) 0 else 3
                    // Sosta attenta e curiosa sul punto agganciato (1.8s - 2.5s)
                    delay((1800L..2500L).random())
                    isSaccade = false
                } else {
                    stepsSinceSnap++
                    isSaccade = false
                    val base = scanWaypoints[waypointIdx % scanWaypoints.size]
                    waypointIdx++
                    val jitter = ((-3..3).random() / 100f)
                    resultsGazeX = (base + jitter).coerceIn(-0.45f, 0.45f)
                    // Si sofferma con calma per 3.0 - 4.5 secondi su ciascuna sezione
                    delay((3000L..4500L).random())
                }
            }
        }
    }

    val effectiveGazeBiasY = when {
        isFocusHunting -> inspectedCardYRatio ?: 1.0f // Messa a fuoco selettiva diretta sulla card premuta (Feature 6)
        isLoading -> 0f
        hasResults -> 1.0f // Quando ci sono risultati a schermo, IRIS guarda SEMPRE in basso verso i film
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
