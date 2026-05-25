package com.cinetrack.ui.components.glass

import androidx.compose.runtime.compositionLocalOf
import io.github.fletchmckee.liquid.LiquidState

/**
 * Fornisce lo stato Liquid globale.
 */
val LocalLiquidState = compositionLocalOf<LiquidState?> { null }

/**
 * Determina se l'effetto Liquid è abilitato o meno dall'utente nelle impostazioni.
 */
val LocalLiquidEnabled = compositionLocalOf { true }
