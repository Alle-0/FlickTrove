package com.cinetrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.viewmodel.AuthViewModel
import com.cinetrack.ui.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

@Composable
fun SettingsOverlayScreen(
    viewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    paddingValues: PaddingValues,
    startX: Float,
    startY: Float,
    onBack: () -> Unit,
    onClosing: () -> Unit,
    onLoggedOut: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isMeasured by remember { mutableStateOf(false) }
    var hasRevealed by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val revealAmount = remember(hasRevealed) { androidx.compose.animation.core.Animatable(if (hasRevealed) 1f else 0f) }
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(isMeasured) {
        if (isMeasured && !hasRevealed) {
            revealAmount.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 800, 
                    easing = androidx.compose.animation.core.CubicBezierEasing(0.7f, 0f, 0.2f, 1f)
                )
            )
            hasRevealed = true
        }
    }

    val triggerExit = {
        if (!isClosing) {
            isClosing = true
            onClosing()
            scope.launch {
                revealAmount.animateTo(
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 800, 
                        easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
                    )
                )
                onBack()
            }
        }
    }

    // When user taps "Replay Tutorial", close the overlay first so the tutorial
    // dialog appears on top and not hidden underneath the settings overlay (zIndex 80000).
    LaunchedEffect(Unit) {
        settingsViewModel.replayTutorialEvent.collect {
            if (!isClosing) {
                isClosing = true
                onClosing()
                scope.launch {
                    revealAmount.animateTo(
                        targetValue = 0f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 600,
                            easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
                        )
                    )
                    settingsViewModel.resetOnboarding()
                    onBack()
                }
            }
        }
    }

    BackHandler(enabled = !isClosing) {
        triggerExit()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        
        if (width > 0 && !isMeasured) {
            LaunchedEffect(Unit) { isMeasured = true }
        }
        
        val center = androidx.compose.ui.geometry.Offset(startX, startY)
        val maxRadius = remember(center, width, height) {
            val distTopLeft = kotlin.math.sqrt((center.x * center.x) + (center.y * center.y).toDouble()).toFloat()
            val distTopRight = kotlin.math.sqrt(((width - center.x) * (width - center.x)) + (center.y * center.y).toDouble()).toFloat()
            val distBottomLeft = kotlin.math.sqrt((center.x * center.x) + ((height - center.y) * (height - center.y)).toDouble()).toFloat()
            val distBottomRight = kotlin.math.sqrt(((width - center.x) * (width - center.x)) + ((height - center.y) * (height - center.y)).toDouble()).toFloat()
            kotlin.math.max(kotlin.math.max(distTopLeft, distTopRight), kotlin.math.max(distBottomLeft, distBottomRight)) * 1.1f
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val radius = revealAmount.value * maxRadius
                    clip = true
                    shape = object : androidx.compose.ui.graphics.Shape {
                        override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                addOval(androidx.compose.ui.geometry.Rect(center = center, radius = radius))
                            }
                            return androidx.compose.ui.graphics.Outline.Generic(path)
                        }
                    }
                }
        ) {
            val internalHazeState = remember { dev.chrisbanes.haze.HazeState() }
            val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
            
            val pendingReveal by settingsViewModel.pendingReveal.collectAsStateWithLifecycle()
            
            val themeGraphicsLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
            var capturedImage by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
            var isScreenshotReady by remember { mutableStateOf(false) }
            val themeAnimatedRadius = remember { androidx.compose.animation.core.Animatable(0f) }
            
            val currentBackgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background
            var oldBackgroundColor by remember { mutableStateOf(androidx.compose.ui.graphics.Color.Black) }
    
            LaunchedEffect(pendingReveal) {
                if (pendingReveal != null) {
                    val (colorName, origin) = pendingReveal!!
                    oldBackgroundColor = currentBackgroundColor
                    try {
                        capturedImage = themeGraphicsLayer.toImageBitmap()
                    } catch (e: Exception) { }
                    
                    themeAnimatedRadius.snapTo(0f)
                    isScreenshotReady = true
                    settingsViewModel.applyPendingTheme()
                    kotlinx.coroutines.delay(150)
                    
                    themeAnimatedRadius.animateTo(
                        targetValue = 10000f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 900, 
                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    )
                    
                    capturedImage = null
                    isScreenshotReady = false
                    settingsViewModel.clearPendingReveal()
                    settingsViewModel.applyPendingIcon(colorName)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isScreenshotReady && capturedImage != null) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(oldBackgroundColor)
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = capturedImage!!,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            if (!isScreenshotReady) {
                                themeGraphicsLayer.record { this@drawWithContent.drawContent() }
                            }
                            drawContent()
                        }
                        .graphicsLayer {
                            if (isScreenshotReady && pendingReveal != null) {
                                val origin = pendingReveal!!.second
                                clip = true
                                shape = object : androidx.compose.ui.graphics.Shape {
                                    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
                                        val p = androidx.compose.ui.graphics.Path().apply {
                                            addOval(androidx.compose.ui.geometry.Rect(
                                                origin.x - themeAnimatedRadius.value, 
                                                origin.y - themeAnimatedRadius.value, 
                                                origin.x + themeAnimatedRadius.value, 
                                                origin.y + themeAnimatedRadius.value
                                            ))
                                        }
                                        return androidx.compose.ui.graphics.Outline.Generic(p)
                                    }
                                }
                            }
                        }
                        .then(
                            if (isScreenshotReady) Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                            else Modifier
                        )
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        SettingsScreenContent(
                            viewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            paddingValues = paddingValues,
                            hazeState = internalHazeState,
                            scrollState = scrollState,
                            onLoggedOut = onLoggedOut,
                            onLoginClick = onLoginClick
                        )
                        
                        val isAnyDialogOpen by settingsViewModel.isAnyDialogOpen.collectAsStateWithLifecycle()
                Box(modifier = Modifier.align(Alignment.TopCenter).zIndex(10f)) {
                    com.cinetrack.ui.components.navigation.GlassyTopBar(
                        hazeState = internalHazeState,
                        title = androidx.compose.ui.res.stringResource(com.cinetrack.R.string.settings_tab_title),
                        onBackPress = triggerExit,
                        isDimmed = isAnyDialogOpen,
                        onDimmedAreaClick = { settingsViewModel.triggerCloseDialogs() }
                    )
                }
            }
        }
        }
        }
    }
}

