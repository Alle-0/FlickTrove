package com.cinetrack.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedElementIfAvailable(key: String, clipShape: Shape? = null): Modifier = this
