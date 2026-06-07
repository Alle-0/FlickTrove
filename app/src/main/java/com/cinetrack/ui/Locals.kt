package com.cinetrack.ui

import android.content.Intent
import androidx.compose.runtime.MutableState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalAppPadding = compositionLocalOf { PaddingValues(0.dp) }
val LocalHazeState = compositionLocalOf<dev.chrisbanes.haze.HazeState?> { null }
val LocalDeepLinkIntent = staticCompositionLocalOf<MutableState<Intent?>> { error("No intent") }
