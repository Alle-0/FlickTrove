package com.cinetrack.ui.components.card

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay

/**
 * Stato delle proprietà animate per l'ingresso scaglionato (staggered) delle card in FlickTrove.
 */
data class CardEntranceState(
    val alpha: Float,
    val scale: Float,
    val translateY: Float
)

/**
 * Calcola l'animazione d'ingresso a cascata (staggered entrance) standard di FlickTrove.
 *
 * @param itemId Identificativo univoco dell'elemento (es. "123_movie").
 * @param staggerIndex Indice di apparizione (0..11 per animazione ritardata, < 0 per scroll items).
 * @param hasAnimatedSet Set globale persistente degli ID già animati durante la sessione.
 * @param isExploding Indica se la card sta scomparendo per effetto particellare (alpha -> 0).
 * @param labelPrefix Prefisso per i label delle animazioni.
 */
@Composable
fun rememberCardEntranceAnimation(
    itemId: String,
    staggerIndex: Int,
    hasAnimatedSet: MutableSet<String>?,
    isExploding: Boolean = false,
    labelPrefix: String = "card"
): CardEntranceState {
    val hasAnimated = rememberSaveable(itemId) {
        mutableStateOf(hasAnimatedSet?.contains(itemId) == true)
    }

    LaunchedEffect(itemId) {
        if (hasAnimated.value) return@LaunchedEffect
        if (staggerIndex in 0..11) delay(staggerIndex * 40L)
        hasAnimated.value = true
        hasAnimatedSet?.add(itemId)
    }

    val isScrollItem = staggerIndex < 0 || staggerIndex >= 12
    val isVisible = hasAnimated.value || staggerIndex < 0

    val cardAlpha by animateFloatAsState(
        targetValue = if (isExploding) 0f else (if (isVisible) 1f else 0f),
        animationSpec = if (isExploding) snap() else tween(
            durationMillis = if (isScrollItem) 180 else 250,
            easing = LinearOutSlowInEasing
        ),
        label = "${labelPrefix}Alpha"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = if (isScrollItem) snap() else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "${labelPrefix}Scale"
    )

    val cardTranslateY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 40f,
        animationSpec = if (isScrollItem) tween(200, easing = FastOutSlowInEasing) else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "${labelPrefix}TranslateY"
    )

    return CardEntranceState(
        alpha = cardAlpha,
        scale = cardScale,
        translateY = cardTranslateY
    )
}

