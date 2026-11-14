package com.cinetrack.ui.components.card

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

@Composable
fun MovieMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    iconColor: Color = Color.White.copy(alpha = 0.8f),
    isDestructive: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .bounceClick(scaleDown = 0.96f, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (isDestructive) Color(0xFFE57373).copy(alpha = 0.15f)
                    else iconColor.copy(alpha = 0.12f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) Color(0xFFE57373) else iconColor,
                modifier = iconModifier.size(16.dp)
            )
        }
        Text(
            text = text,
            color = if (isDestructive) Color(0xFFE57373) else Color.White.copy(alpha = 0.95f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MovieActionsPopup(
    movie: Movie,
    showMenu: Boolean,
    onDismiss: () -> Unit,
    pressOffset: Offset,
    cardPosition: Offset,
    hazeState: HazeState,
    onQuickVote: (Movie) -> Unit,
    onQuickNote: (Movie) -> Unit,
    onFolders: (Movie) -> Unit,
    onShare: (Movie) -> Unit,
    onDelete: ((Movie) -> Unit)? = null,
    isSaved: Boolean = false,
    canDelete: Boolean = isSaved && onDelete != null,
    onEditDate: (() -> Unit)? = null,
    deleteLabel: String? = null,
    onDismissComplete: (() -> Unit)? = null,
    cardSize: Size = Size.Zero
) {
    val stableOnEditDate = remember(movie.id) { onEditDate }
    val stableDeleteLabel = remember(movie.id) { deleteLabel }
    val stableCanDelete = remember(movie.id) { canDelete }
    val stableCardPosition = remember(movie.id) { cardPosition }
    val stableCardSize = remember(movie.id) { cardSize }
    val stablePressOffset = remember(movie.id) { pressOffset }

    val menuScale = remember { Animatable(0f) }
    val menuAlpha = remember { Animatable(0f) }
    var isRendering by remember { mutableStateOf(false) }

    LaunchedEffect(showMenu) {
        if (showMenu) {
            isRendering = true
            menuScale.snapTo(0f)
            menuAlpha.snapTo(0f)

            // Direct bouncy enlargement right from the touch point
            launch {
                menuAlpha.animateTo(1f, tween(100, easing = LinearOutSlowInEasing))
            }
            menuScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.60f, // Bouncy & snappy pop
                    stiffness = 400f
                )
            )
        } else if (isRendering) {
            // Snappy exit: shrinks slightly and fades out
            launch {
                menuAlpha.animateTo(0f, tween(90, easing = FastOutLinearInEasing))
            }
            menuScale.animateTo(
                targetValue = 0.88f,
                animationSpec = tween(90, easing = FastOutLinearInEasing)
            )
            menuScale.snapTo(0f)
            isRendering = false
            onDismissComplete?.invoke()
        }
    }

    if (isRendering || menuScale.value > 0f) {
        val density = LocalDensity.current
        val config = LocalConfiguration.current

        val cardWidthPx = if (stableCardSize.width > 0f) stableCardSize.width else with(density) { 140.dp.toPx() }
        val cardHeightPx = if (stableCardSize.height > 0f) stableCardSize.height else with(density) { 210.dp.toPx() }

        val actualPressOffset = if (stablePressOffset != Offset.Zero) stablePressOffset 
                                else Offset(cardWidthPx / 2f, cardHeightPx / 2f)
        val touchX = stableCardPosition.x + actualPressOffset.x
        val touchY = stableCardPosition.y + actualPressOffset.y

        val canRate = if (movie.mediaType == "tv") {
            movie.watched || (movie.watchedEpisodes?.values?.any { it.isNotEmpty() } == true)
        } else {
            movie.watched
        }

        val hasDelete = stableCanDelete && onDelete != null
        val hasEditDate = stableOnEditDate != null

        var visibleItemCount = 3 // Quick note, Folders, Share
        if (hasEditDate) visibleItemCount++
        if (canRate) visibleItemCount++

        val titleText = movie.title ?: movie.name ?: "Senza titolo"
        val isTwoLineTitle = titleText.length > 20
        val titleBlockHeight = if (isTwoLineTitle) 64.dp else 48.dp

        val actualMenuWidth = if (hasEditDate) 215.dp else 170.dp
        // Strictly fitted height: hugs exactly the visible items, eliminating all dead space
        val menuHeight = titleBlockHeight + (visibleItemCount * 38).dp + (if (hasDelete) 47.dp else 0.dp)

        val menuWidthPx = with(density) { actualMenuWidth.toPx() }
        val menuHeightPx = with(density) { menuHeight.toPx() }
        val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
        val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

        val statusBarsTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
        val safeDrawingTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
        val maxTopInset = maxOf(statusBarsTop, cutoutTop, safeDrawingTop)
        // Hard guarantee: popup will NEVER overlap camera punch-hole/notch or status bar
        val topSafetyDp = maxOf(maxTopInset + 12.dp, 56.dp)

        val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val safeDrawingBottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
        val maxBottomInset = maxOf(navBarsBottom, safeDrawingBottom)
        val bottomSafetyDp = maxOf(maxBottomInset + 16.dp, 48.dp)

        val topSafetyPx = with(density) { topSafetyDp.toPx() }
        val bottomSafetyPx = with(density) { bottomSafetyDp.toPx() }
        val sideSafetyPx = with(density) { 16.dp.toPx() }

        var xPos = touchX
        var yPos = touchY

        if (xPos + menuWidthPx > screenWidthPx - sideSafetyPx) {
            xPos -= menuWidthPx
        }
        if (xPos < sideSafetyPx) xPos = sideSafetyPx

        val maxAllowedY = (screenHeightPx - bottomSafetyPx - menuHeightPx).coerceAtLeast(topSafetyPx)
        val isShiftedUp = touchY + menuHeightPx > screenHeightPx - bottomSafetyPx
        if (isShiftedUp) {
            yPos -= menuHeightPx
        }

        yPos = yPos.coerceIn(topSafetyPx, maxAllowedY)

        Box(
            contentAlignment = AbsoluteAlignment.TopLeft,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(9999f)
        ) {
            // Transparent dismiss area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .bounceClick(scaleDown = 1f, onClick = onDismiss)
            )

            // Menu Capsule rooted at the fingertip with GPU spring scaling
            if (menuScale.value > 0f) {
                val pivotX = ((touchX - xPos) / menuWidthPx).coerceIn(0f, 1f)
                val pivotY = ((touchY - yPos) / menuHeightPx).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .absoluteOffset { IntOffset(xPos.toInt(), yPos.toInt()) }
                        .size(width = actualMenuWidth, height = menuHeight)
                        .graphicsLayer {
                            scaleX = menuScale.value
                            scaleY = menuScale.value
                            transformOrigin = TransformOrigin(pivotX, pivotY)
                            alpha = menuAlpha.value
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .hazeGlass(
                            state = hazeState,
                            shape = RoundedCornerShape(24.dp),
                            borderColor = Color.White.copy(alpha = 0.06f)
                        )
                        .bounceClick(scaleDown = 1f) { }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(2.5.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        val accentColor = MaterialTheme.colorScheme.primary
                        var itemDelay = 15
                        val itemStep = 20
                        val isItemReady = menuScale.value >= 0.5f

                        if (hasEditDate) {
                            val itemAlpha by animateFloatAsState(
                                targetValue = if (isItemReady) 1f else 0f,
                                animationSpec = tween(90, delayMillis = itemDelay, easing = FastOutSlowInEasing),
                                label = "editDateAlpha"
                            )
                            MovieMenuItem(
                                text = stringResource(R.string.history_edit_date_title),
                                icon = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                                iconColor = accentColor,
                                modifier = Modifier.graphicsLayer {
                                    alpha = itemAlpha
                                },
                                onClick = { onDismiss(); stableOnEditDate() }
                            )
                            itemDelay += itemStep
                        }

                        if (canRate) {
                            val itemAlpha by animateFloatAsState(
                                targetValue = if (isItemReady) 1f else 0f,
                                animationSpec = tween(90, delayMillis = itemDelay, easing = FastOutSlowInEasing),
                                label = "rateAlpha"
                            )
                            MovieMenuItem(
                                text = stringResource(R.string.card_quick_rate),
                                icon = ImageVector.vectorResource(id = R.drawable.ic_star),
                                iconColor = accentColor,
                                modifier = Modifier.graphicsLayer {
                                    alpha = itemAlpha
                                },
                                onClick = { onDismiss(); onQuickVote(movie) }
                            )
                            itemDelay += itemStep
                        }

                        val noteAlpha by animateFloatAsState(
                            targetValue = if (isItemReady) 1f else 0f,
                            animationSpec = tween(90, delayMillis = itemDelay, easing = FastOutSlowInEasing),
                            label = "noteAlpha"
                        )
                        MovieMenuItem(
                            text = stringResource(R.string.card_quick_note),
                            icon = ImageVector.vectorResource(id = R.drawable.ic_pencil),
                            iconColor = accentColor,
                            modifier = Modifier.graphicsLayer {
                                alpha = noteAlpha
                            },
                            onClick = { onDismiss(); onQuickNote(movie) }
                        )
                        itemDelay += itemStep

                        val foldersAlpha by animateFloatAsState(
                            targetValue = if (isItemReady) 1f else 0f,
                            animationSpec = tween(90, delayMillis = itemDelay, easing = FastOutSlowInEasing),
                            label = "foldersAlpha"
                        )
                        MovieMenuItem(
                            text = stringResource(R.string.card_folders),
                            icon = ImageVector.vectorResource(id = R.drawable.ic_cartella),
                            iconColor = accentColor,
                            modifier = Modifier.graphicsLayer {
                                alpha = foldersAlpha
                            },
                            onClick = { onDismiss(); onFolders(movie) }
                        )
                        itemDelay += itemStep

                        val shareAlpha by animateFloatAsState(
                            targetValue = if (isItemReady) 1f else 0f,
                            animationSpec = tween(90, delayMillis = itemDelay, easing = FastOutSlowInEasing),
                            label = "shareAlpha"
                        )
                        MovieMenuItem(
                            text = stringResource(R.string.card_share),
                            icon = ImageVector.vectorResource(id = R.drawable.ic_share),
                            iconModifier = Modifier.offset(x = (-1).dp),
                            iconColor = accentColor,
                            modifier = Modifier.graphicsLayer {
                                alpha = shareAlpha
                            },
                            onClick = { onDismiss(); onShare(movie) }
                        )
                        itemDelay += itemStep

                        if (hasDelete) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val dividerAlpha by animateFloatAsState(
                                targetValue = if (isItemReady) 1f else 0f,
                                animationSpec = tween(90, delayMillis = itemDelay, easing = FastOutSlowInEasing),
                                label = "dividerAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp)
                                    .height(1.dp)
                                    .graphicsLayer { alpha = dividerAlpha }
                                    .background(Color.White.copy(alpha = 0.1f))
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            val deleteAlpha by animateFloatAsState(
                                targetValue = if (isItemReady) 1f else 0f,
                                animationSpec = tween(90, delayMillis = itemDelay + 10, easing = FastOutSlowInEasing),
                                label = "deleteAlpha"
                            )
                            MovieMenuItem(
                                text = stableDeleteLabel ?: stringResource(R.string.card_remove),
                                icon = ImageVector.vectorResource(id = R.drawable.ic_trash),
                                iconColor = Color(0xFFE57373),
                                isDestructive = true,
                                modifier = Modifier.graphicsLayer {
                                    alpha = deleteAlpha
                                },
                                onClick = { onDelete(movie); onDismiss() }
                            )
                        }
                    }
                }
            }
        }
    }
}
