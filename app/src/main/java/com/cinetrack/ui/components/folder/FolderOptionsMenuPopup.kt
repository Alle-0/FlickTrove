package com.cinetrack.ui.components.folder

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import com.google.firebase.auth.FirebaseAuth
import dev.chrisbanes.haze.HazeState

/**
 * Animated popup contextual menu triggered on long-pressing a FolderCard.
 * Offers Share/Members, Edit info, and Delete/Leave actions with spring animations.
 */
@Composable
fun FolderOptionsMenuPopup(
    activeMenuFolder: FolderEntity?,
    activeMenuBounds: Rect,
    activeHazeState: HazeState,
    isGuest: Boolean,
    onDismiss: () -> Unit,
    onShareFolder: (FolderEntity) -> Unit,
    onOpenMembers: (FolderEntity) -> Unit,
    onEditFolder: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onGuestAuthRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stableMenuFolder by remember { mutableStateOf<FolderEntity?>(null) }
    val showFolderMenu = activeMenuFolder != null
    if (activeMenuFolder != null) {
        stableMenuFolder = activeMenuFolder
    }

    val folderTransitionState = remember {
        MutableTransitionState(false)
    }
    folderTransitionState.targetState = showFolderMenu

    LaunchedEffect(folderTransitionState.currentState, folderTransitionState.targetState, folderTransitionState.isIdle) {
        if (!folderTransitionState.targetState && folderTransitionState.isIdle) {
            stableMenuFolder = null
        }
    }

    if (folderTransitionState.targetState || !folderTransitionState.isIdle) {
        stableMenuFolder?.let { folder ->
            Box(
                contentAlignment = AbsoluteAlignment.TopLeft,
                modifier = modifier
                    .fillMaxSize()
                    .zIndex(2000f)
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            ) {
                val density = LocalDensity.current
                val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
                val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                val rawOffsetX = with(density) {
                    if (isRtl) activeMenuBounds.right.toDp() - 200.dp else activeMenuBounds.left.toDp() + 32.dp
                }
                val offsetX = rawOffsetX.coerceIn(8.dp, (screenWidthDp - 208.dp).coerceAtLeast(8.dp))
                val offsetY = with(density) { activeMenuBounds.top.toDp() + 48.dp }

                AnimatedVisibility(
                    visibleState = folderTransitionState,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = spring(
                            dampingRatio = 0.76f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(
                        animationSpec = tween(120, easing = LinearOutSlowInEasing)
                    ),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(120, easing = FastOutLinearInEasing)
                    ) + fadeOut(
                        animationSpec = tween(110, easing = FastOutLinearInEasing)
                    ),
                    modifier = Modifier.absoluteOffset(x = offsetX, y = offsetY)
                ) {
                    Column(
                        modifier = Modifier
                            .width(200.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .hazeGlass(state = activeHazeState, shape = RoundedCornerShape(24.dp))
                    ) {
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                        val isOwner = !folder.isShared || (folder.ownerUid == currentUserId)
                        var itemDelay = 15
                        val itemStep = 35

                        if (folder.isShared || isOwner) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateEnterExit(
                                        enter = fadeIn(tween(120, delayMillis = itemDelay)) +
                                                scaleIn(
                                                    initialScale = 0.90f,
                                                    animationSpec = spring(dampingRatio = 0.70f, stiffness = 500f)
                                                ),
                                        exit = fadeOut(tween(100))
                                    )
                                    .bounceClick {
                                        val target = folder
                                        onDismiss()
                                        if (isGuest) {
                                            onGuestAuthRequired()
                                        } else if (target.isShared) {
                                            onOpenMembers(target)
                                        } else {
                                            onShareFolder(target)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.ic_people),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = if (folder.isShared) {
                                        if (isOwner) stringResource(R.string.folder_members_title) else stringResource(R.string.folder_members_only_title)
                                    } else {
                                        stringResource(R.string.folder_share_action)
                                    },
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                            itemDelay += itemStep
                        }

                        if (isOwner) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateEnterExit(
                                        enter = fadeIn(tween(120, delayMillis = itemDelay)) +
                                                scaleIn(
                                                    initialScale = 0.90f,
                                                    animationSpec = spring(dampingRatio = 0.70f, stiffness = 500f)
                                                ),
                                        exit = fadeOut(tween(100))
                                    )
                                    .bounceClick {
                                        onDismiss()
                                        onEditFolder(folder)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.ic_pencil),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    stringResource(R.string.folder_edit_info),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                            itemDelay += itemStep
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateEnterExit(
                                    enter = fadeIn(tween(120, delayMillis = itemDelay)) +
                                            scaleIn(
                                                initialScale = 0.90f,
                                                animationSpec = spring(dampingRatio = 0.70f, stiffness = 500f)
                                            ),
                                    exit = fadeOut(tween(100))
                                )
                                .bounceClick {
                                    onDismiss()
                                    onDeleteFolder(folder)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                ImageVector.vectorResource(id = R.drawable.ic_trash),
                                contentDescription = null,
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (folder.isShared && !isOwner) {
                                    stringResource(R.string.folder_leave)
                                } else {
                                    stringResource(R.string.folders_delete)
                                },
                                color = Color(0xFFFF3B30),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }
}
