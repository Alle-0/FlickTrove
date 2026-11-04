package com.cinetrack.ui.components.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.input.pointer.pointerInput
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

@Composable
fun MainFolderOptionsMenu(
    visible: Boolean = true,
    offset: Offset,
    hazeState: HazeState,
    isShared: Boolean = false,
    isOwner: Boolean = true,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onReorder: () -> Unit,
    onShareOrMembers: () -> Unit = {},
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(2000f)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    ) {
        val density = LocalDensity.current
        val offsetX = with(density) { offset.x.toDp() }
        val offsetY = with(density) { offset.y.toDp() }

        AnimatedVisibility(
            visible = visible,
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
            modifier = Modifier.absoluteOffset(x = offsetX - 200.dp + 32.dp, y = offsetY + 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .then(if (visible) Modifier.hazeGlass(state = hazeState, shape = RoundedCornerShape(24.dp)) else Modifier)
            ) {
                var itemDelay = 15
                val itemStep = 35

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
                            onShareOrMembers()
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
                        text = if (isShared) {
                            if (isOwner) stringResource(R.string.folder_members_title) else stringResource(R.string.folder_members_only_title)
                        } else {
                            stringResource(R.string.folder_share_action)
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
                itemDelay += itemStep

                if (!isShared || isOwner) {
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
                                onEdit()
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
                            onReorder()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_lista),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.main_folder_reorder),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
                itemDelay += itemStep

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
                            onDelete()
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
                        text = if (isShared && !isOwner) stringResource(R.string.folder_leave) else stringResource(R.string.main_folder_delete),
                        color = Color(0xFFFF3B30),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}
