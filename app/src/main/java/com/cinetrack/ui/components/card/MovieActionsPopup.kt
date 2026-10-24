package com.cinetrack.ui.components.card

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
    deleteLabel: String? = null
) {
    // Use MutableTransitionState to precisely track the AnimatedVisibility lifecycle
    val transitionState = remember { 
        MutableTransitionState(false).apply { targetState = showMenu } 
    }
    
    // Sync the target state
    transitionState.targetState = showMenu

    // Only render when visible or animating
    if (transitionState.targetState || !transitionState.isIdle) {
        val density = LocalDensity.current
        val config = LocalConfiguration.current
        
        // Calculate the absolute touch position
        val touchX = cardPosition.x + pressOffset.x
        val touchY = cardPosition.y + pressOffset.y

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(9999f)
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = fadeIn(tween(200, easing = LinearOutSlowInEasing)) + 
                        expandVertically(expandFrom = Alignment.Top, animationSpec = tween(250, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(200, easing = FastOutLinearInEasing)) + 
                       shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(200, easing = FastOutLinearInEasing))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .bounceClick(scaleDown = 1f, onClick = onDismiss)
                    )

                    val menuWidth = if (onEditDate != null) 225.dp else 180.dp
                    val menuHeight = if (onEditDate != null) 310.dp else 260.dp
                    
                    val menuWidthPx = with(density) { menuWidth.toPx() }
                    val menuHeightPx = with(density) { menuHeight.toPx() }
                    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
                    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

                    val statusBarsHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val navBarsHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    
                    val topSafetyPx = with(density) { (statusBarsHeight + 180.dp).toPx() }
                    val bottomSafetyPx = with(density) { (navBarsHeight + 12.dp).toPx() }
                    val sideSafetyPx = with(density) { 16.dp.toPx() }

                    var xPos = touchX
                    var yPos = touchY

                    if (xPos + menuWidthPx > screenWidthPx - sideSafetyPx) {
                        xPos -= menuWidthPx
                    }
                    if (xPos < sideSafetyPx) xPos = sideSafetyPx

                    if (yPos + menuHeightPx > screenHeightPx - bottomSafetyPx) {
                        yPos -= menuHeightPx
                    }
                    
                    yPos = yPos.coerceIn(topSafetyPx, (screenHeightPx - bottomSafetyPx - menuHeightPx).coerceAtLeast(topSafetyPx))

                    Column(
                        modifier = Modifier
                            .offset { IntOffset(xPos.toInt(), yPos.toInt()) }
                            .width(if (onEditDate != null) 215.dp else 170.dp)
                            .hazeGlass(
                                state = hazeState,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .bounceClick(scaleDown = 1f) { }
                            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = movie.title ?: movie.name ?: "Senza titolo",
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
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        val accentColor = MaterialTheme.colorScheme.primary

                        if (onEditDate != null) {
                            MovieMenuItem(
                                text = stringResource(R.string.history_edit_date_title),
                                icon = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                                iconColor = accentColor,
                                modifier = Modifier.animateEnterExit(
                                    enter = slideInVertically(tween(300, delayMillis = 20)) { it / 2 } + fadeIn(tween(300, delayMillis = 20))
                                ),
                                onClick = { onDismiss(); onEditDate() }
                            )
                        }

                        val canRate = if (movie.mediaType == "tv") {
                            movie.watched || (movie.watchedEpisodes?.values?.any { it.isNotEmpty() } == true)
                        } else {
                            movie.watched
                        }

                        if (canRate) {
                            MovieMenuItem(
                                text = stringResource(R.string.card_quick_rate),
                                icon = ImageVector.vectorResource(id = R.drawable.ic_star),
                                iconColor = accentColor,
                                modifier = Modifier.animateEnterExit(
                                    enter = slideInVertically(tween(300, delayMillis = 40)) { it / 2 } + fadeIn(tween(300, delayMillis = 40))
                                ),
                                onClick = { onDismiss(); onQuickVote(movie) }
                            )
                        }
                        MovieMenuItem(
                            text = stringResource(R.string.card_quick_note),
                            icon = ImageVector.vectorResource(id = R.drawable.ic_pencil),
                            iconColor = accentColor,
                            modifier = Modifier.animateEnterExit(
                                enter = slideInVertically(tween(300, delayMillis = 80)) { it / 2 } + fadeIn(tween(300, delayMillis = 80))
                            ),
                            onClick = { onDismiss(); onQuickNote(movie) }
                        )
                        MovieMenuItem(
                            text = stringResource(R.string.card_folders),
                            icon = ImageVector.vectorResource(id = R.drawable.ic_cartella),
                            iconColor = accentColor,
                            modifier = Modifier.animateEnterExit(
                                enter = slideInVertically(tween(300, delayMillis = 120)) { it / 2 } + fadeIn(tween(300, delayMillis = 120))
                            ),
                            onClick = { onDismiss(); onFolders(movie) }
                        )
                        MovieMenuItem(
                            text = stringResource(R.string.card_share),
                            icon = ImageVector.vectorResource(id = R.drawable.ic_share),
                            iconModifier = Modifier.offset(x = (-1).dp),
                            iconColor = accentColor,
                            modifier = Modifier.animateEnterExit(
                                enter = slideInVertically(tween(300, delayMillis = 160)) { it / 2 } + fadeIn(tween(300, delayMillis = 160))
                            ),
                            onClick = { onDismiss(); onShare(movie) }
                        )
                        
                        if (canDelete && onDelete != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp)
                                .height(1.dp)
                                .animateEnterExit(
                                    enter = fadeIn(tween(300, delayMillis = 200))
                                )
                                .background(Color.White.copy(alpha = 0.1f)))
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            MovieMenuItem(
                                text = deleteLabel ?: stringResource(R.string.card_remove),
                                icon = ImageVector.vectorResource(id = R.drawable.ic_trash),
                                iconColor = Color(0xFFE57373),
                                isDestructive = true,
                                modifier = Modifier.animateEnterExit(
                                    enter = slideInVertically(tween(300, delayMillis = 240)) { it / 2 } + fadeIn(tween(300, delayMillis = 240))
                                ),
                                onClick = { onDelete(movie); onDismiss() }
                            )
                        }
                    }
                }
            }
        }
    }
}
