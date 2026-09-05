package com.cinetrack.ui.components.navigation
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.cinetrack.R
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.layoutToggleIcon
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.util.NetworkMonitor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

@Composable
fun GlassyTopBar(
    hazeState: HazeState,
    title: String,
    onMenuClick: (() -> Unit)? = null,
    onBackPress: (() -> Unit)? = null,
    onFilterClick: ((Offset) -> Unit)? = null,
    onUpdatesClick: ((Offset) -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null,
    hasActiveFilters: Boolean = false,
    isSyncing: Boolean = false,
    isDimmed: Boolean = false,
    onDimmedAreaClick: (() -> Unit)? = null,
    notificationCount: Int = 0,
    onDeleteClick: (() -> Unit)? = null, // Deprecated, use onFolderOptionsClick
    onFolderOptionsClick: ((Offset) -> Unit)? = null,
    indicatorColor: Color? = null,
    onLayoutToggleClick: (() -> Unit)? = null,
    layoutColumns: Int? = null,
    hasAppUpdateBadge: Boolean = false,
    onEditBackdropClick: (() -> Unit)? = null,
    onSettingsClick: ((Offset) -> Unit)? = null,
    isStatsRewatchesEnabled: Boolean? = null,
    onStatsRewatchToggle: ((Boolean) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }
    val connectionState by networkMonitor.connectionState.collectAsStateWithLifecycle(initialValue = com.cinetrack.util.ConnectionState.ONLINE)

    val animatedDimAlpha by animateFloatAsState(
        targetValue = if (isDimmed) 0.7f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "TopBarDimAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "SyncPulse")
    val syncAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SyncAlpha"
    )

    // Shared enter/exit specs for all top bar icons.
    // expandFrom = Alignment.End keeps the RIGHT edge of each icon fixed,
    // so the bell never moves when icons appear/disappear to its left.
    val iconEnterSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
    val iconEnter: EnterTransition = expandHorizontally(
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        expandFrom = Alignment.End
    ) + fadeIn(iconEnterSpec) + scaleIn(iconEnterSpec, initialScale = 0.7f)
    val iconExit: ExitTransition = shrinkHorizontally(
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        shrinkTowards = Alignment.End
    ) + fadeOut(iconEnterSpec) + scaleOut(iconEnterSpec, targetScale = 0.7f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .displayCutoutPadding()
            .padding(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 0.dp)
    ) {
        // BACKGROUND BLUR LAYER
        Box(
            modifier = Modifier
                .matchParentSize()
                .hazeGlass(state = hazeState, shape = RoundedCornerShape(50))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isStatsRewatchesEnabled != null) 100.dp else 56.dp), // Leaves space for the text pill
                contentAlignment = Alignment.Center
            ) {
                // Text is swapped while invisible, so the layout never changes size.
                var displayedTitle by remember { mutableStateOf(title) }
                val titleAlpha = remember { Animatable(1f) }
                val titleBlur = remember { Animatable(0f) }

                LaunchedEffect(title) {
                    if (displayedTitle != title) {
                        // Animate out: Blur inizia subito, il fade ritarda un attimo così si vede il testo sfocato
                        launch { titleBlur.animateTo(12f, tween(150, easing = FastOutSlowInEasing)) }
                        titleAlpha.animateTo(0f, tween(100, delayMillis = 50, easing = LinearEasing))
                        
                        // Swap while invisible
                        displayedTitle = title
                        
                        // Animate in: Appare sfocato e poi si mette a fuoco
                        launch { titleAlpha.animateTo(1f, tween(150, easing = FastOutSlowInEasing)) }
                        titleBlur.animateTo(0f, tween(200, delayMillis = 50, easing = FastOutSlowInEasing))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = titleAlpha.value
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && titleBlur.value > 0f) {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                titleBlur.value, titleBlur.value,
                                android.graphics.Shader.TileMode.DECAL
                            ).asComposeRenderEffect()
                        }
                    }
                ) {
                    if (indicatorColor != null) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(indicatorColor)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                        )
                        Spacer(Modifier.width(8.dp))
                    }

                    if (displayedTitle == "FlickTrove") {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.cinetrack.R.drawable.ic_launcher_foreground_vector),
                            contentDescription = "Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }

                    Text(
                        text = displayedTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                // Menu/Back Button
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val isNavVisible = onBackPress != null || onMenuClick != null
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isNavVisible,
                        enter = fadeIn(iconEnterSpec) + scaleIn(iconEnterSpec, initialScale = 0.7f),
                        exit = fadeOut(iconEnterSpec) + scaleOut(iconEnterSpec, targetScale = 0.7f)
                    ) {
                        androidx.compose.animation.AnimatedContent(
                            targetState = onBackPress != null,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) togetherWith fadeOut(animationSpec = tween(90)) + scaleOut(targetScale = 0.92f, animationSpec = tween(90))
                            },
                            label = "NavIconAnim"
                        ) { isBack ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .bounceClick(
                                        enabled = !isDimmed,
                                        onClick = {
                                            if (isBack) onBackPress?.invoke() else onMenuClick?.invoke()
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isBack) ImageVector.vectorResource(id = R.drawable.ic_left) else ImageVector.vectorResource(id = R.drawable.ic_menu),
                                    contentDescription = "Navigation",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (!isBack && hasAppUpdateBadge) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .border(1.dp, Color.Black, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.wrapContentWidth()
                ) {
                val isOffline = connectionState == com.cinetrack.util.ConnectionState.OFFLINE
                val isPoor = connectionState == com.cinetrack.util.ConnectionState.POOR
                AnimatedVisibility(
                    visible = isOffline,
                    enter = iconEnter,
                    exit = iconExit
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = "Offline",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                AnimatedVisibility(
                    visible = isPoor,
                    enter = iconEnter,
                    exit = iconExit
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Wifi,
                            contentDescription = "Poor Connection",
                            tint = Color(0xFFFFA500),
                            modifier = Modifier.size(20.dp).alpha(0.8f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                AnimatedVisibility(
                    visible = isSyncing,
                    enter = iconEnter,
                    exit = iconExit
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = "Syncing",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .alpha(syncAlpha)
                            .padding(end = 8.dp)
                    )
                }

                AnimatedVisibility(
                    visible = onLayoutToggleClick != null && layoutColumns != null,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(spring(stiffness = Spring.StiffnessMediumLow), initialScale = 0.7f) + slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { -it },
                    exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) + scaleOut(spring(stiffness = Spring.StiffnessMediumLow), targetScale = 0.7f) + slideOutVertically(spring(stiffness = Spring.StiffnessMediumLow)) { -it }
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .bounceClick(
                                enabled = !isDimmed,
                                onClick = { onLayoutToggleClick?.invoke() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = layoutToggleIcon(layoutColumns ?: 2),
                            contentDescription = "Cambia Colonne",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                val filterButtonCenter = remember { arrayOf(Offset.Zero) }
                AnimatedVisibility(
                    visible = onFilterClick != null,
                    enter = iconEnter,
                    exit = iconExit
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .then(
                                if (hasActiveFilters) Modifier.border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(50))
                                else Modifier
                            )
                            .onGloballyPositioned { coords ->
                                val position = coords.positionInWindow()
                                filterButtonCenter[0] = Offset(
                                    x = position.x + coords.size.width / 2f,
                                    y = position.y + coords.size.height / 2f
                                )
                            }
                            .bounceClick(
                                enabled = !isDimmed,
                                onClick = { onFilterClick?.invoke(filterButtonCenter[0]) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_filtri),
                            contentDescription = "Filtri",
                            tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = onRefreshClick != null,
                    enter = iconEnter,
                    exit = iconExit
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick(
                                enabled = !isDimmed,
                                onClick = { onRefreshClick?.invoke() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_ricarica),
                            contentDescription = "Ricarica",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                val settingsButtonCenter = remember { arrayOf(Offset.Zero) }
                AnimatedVisibility(
                    visible = onSettingsClick != null,
                    enter = iconEnter,
                    exit = iconExit
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .onGloballyPositioned { coords ->
                                val position = coords.positionInWindow()
                                settingsButtonCenter[0] = Offset(
                                    x = position.x + coords.size.width / 2f,
                                    y = position.y + coords.size.height / 2f
                                )
                            }
                            .bounceClick(
                                enabled = !isDimmed,
                                onClick = { onSettingsClick?.invoke(settingsButtonCenter[0]) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_settings),
                            contentDescription = "Impostazioni",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                val isOptimisticRewatches = remember(isStatsRewatchesEnabled) { mutableStateOf(isStatsRewatchesEnabled ?: false) }
                AnimatedVisibility(
                    visible = isStatsRewatchesEnabled != null && onStatsRewatchToggle != null,
                    enter = iconEnter,
                    exit = iconExit
                ) {
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .wrapContentWidth()
                            .bounceClick(
                                enabled = !isDimmed,
                                onClick = {
                                    isOptimisticRewatches.value = !isOptimisticRewatches.value
                                    onStatsRewatchToggle?.invoke(isOptimisticRewatches.value)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 6.dp)
                                .background(
                                    color = if (isOptimisticRewatches.value) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(id = R.string.stats_rewatches),
                                color = if (isOptimisticRewatches.value) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = onEditBackdropClick != null,
                    enter = iconEnter,
                    exit = iconExit
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick(
                                enabled = !isDimmed,
                                onClick = { onEditBackdropClick?.invoke() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_pencil),
                            contentDescription = "Modifica Sfondo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                val updatesButtonCenter = remember { arrayOf(Offset.Zero) }
                AnimatedVisibility(
                    visible = onUpdatesClick != null,
                    enter = iconEnter,
                    exit = iconExit
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .onGloballyPositioned { coords ->
                                val position = coords.positionInWindow()
                                updatesButtonCenter[0] = Offset(
                                    x = position.x + coords.size.width / 2f,
                                    y = position.y + coords.size.height / 2f
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .bounceClick(
                                    enabled = !isDimmed,
                                    onClick = { onUpdatesClick?.invoke(updatesButtonCenter[0]) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_bell_piena),
                                contentDescription = "Aggiornamenti",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (notificationCount > 0) {
                            val isPill = notificationCount > 9
                            val badgeText = if (notificationCount > 99) "99+" else notificationCount.toString()
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    // Keep offset inside the enlarged 36dp box — no overflow, no clipping
                                    .offset(x = (-1).dp, y = 1.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                                    .then(if (isPill) Modifier.sizeIn(minWidth = 16.dp).height(14.dp) else Modifier.size(14.dp))
                                    .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(50))
                                    .padding(horizontal = if (isPill) 3.dp else 0.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = badgeText,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 8.sp
                                )
                            }
                        }
                    }
                }
                
                val optionsOffset = remember { arrayOf(Offset.Zero) }
                AnimatedVisibility(
                    visible = onFolderOptionsClick != null,
                    enter = iconEnter,
                    exit = iconExit
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .onGloballyPositioned { coords ->
                                val position = coords.positionInWindow()
                                optionsOffset[0] = Offset(position.x, position.y + coords.size.height)
                            }
                            .bounceClick(
                                enabled = !isDimmed,
                                onClick = { onFolderOptionsClick?.invoke(optionsOffset[0]) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Opzioni",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (onUpdatesClick == null && onFilterClick == null && onDeleteClick == null && !isSyncing && onLayoutToggleClick == null && onFolderOptionsClick == null && onStatsRewatchToggle == null && onSettingsClick == null) {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
            }

        }

        // DIM OVERLAY
        if (animatedDimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = animatedDimAlpha), RoundedCornerShape(50))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            onDimmedAreaClick?.invoke()
                        }
                    }
            )
        }
    }
}
