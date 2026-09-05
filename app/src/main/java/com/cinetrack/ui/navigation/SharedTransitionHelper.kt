package com.cinetrack.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedElementIfAvailable(key: String): Modifier = this
