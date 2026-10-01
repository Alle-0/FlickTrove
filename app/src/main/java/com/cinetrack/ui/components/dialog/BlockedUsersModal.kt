package com.cinetrack.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.repository.BlockedAuthor
import com.cinetrack.ui.components.glass.GlassmorphicModal
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.util.VibrationHelper
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BlockedUsersModal(
    isVisible: Boolean,
    onClose: () -> Unit,
    blockedUsers: List<BlockedAuthor>,
    onUnblock: (String) -> Unit,
    hazeState: HazeState,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    GlassmorphicModal(
        visible = isVisible,
        activeHazeState = hazeState,
        dimBackground = true,
        dismissOnClickOutside = true,
        onDismissRequest = onClose
    ) {
        val configuration = LocalConfiguration.current
        val context = LocalContext.current
        val maxDialogHeight = (configuration.screenHeightDp.dp * 0.72f).coerceIn(400.dp, 580.dp)
        val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxDialogHeight)
                .padding(bottom = 16.dp)
        ) {
            // Header Bar Concentrica (Raggio 32.dp / clearance 16dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5252).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_block),
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.settings_blocked_users_title),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (blockedUsers.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                val countStr = "${blockedUsers.size}"
                                Box(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .defaultMinSize(minWidth = 20.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.18f))
                                        .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape)
                                        .padding(horizontal = if (countStr.length > 1) 5.dp else 0.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = countStr,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            lineHeight = 11.sp
                                        ),
                                        color = accentColor
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.settings_blocked_users_subtitle),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                ModalCloseButton(
                    onClick = onClose
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = Color.White.copy(alpha = 0.08f)
            )

            if (blockedUsers.isEmpty()) {
                // Empty State Elegante
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(vertical = 48.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_block),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.settings_blocked_users_empty),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = blockedUsers, key = { it.authorId }) { item ->
                        val initial = item.authorName.trim().firstOrNull()?.uppercase() ?: "?"
                        val blockedDateStr = remember(item.blockedAt) {
                            dateFormatter.format(Date(item.blockedAt))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                .padding(all = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar cerchio
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Dettagli Autore
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = item.authorName.ifBlank { "User ${item.authorId.take(6)}" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = blockedDateStr,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.45f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Bottone a pillola "Sblocca"
                            // Concentricità: outer pill ~58dp (r=29dp), button 36dp (r=18dp)
                            // Clearance richiesta = 29-18 = 11dp → outer padding 8dp + trailing 3dp = 11dp ✓
                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .bounceClick {
                                        VibrationHelper.vibrateTick(context)
                                        onUnblock(item.authorId)
                                    }
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.14f))
                                    .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.comment_unblock_user_btn),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = accentColor,
                                    maxLines = 1
                                )
                            }

                            // Trailing spacer per concentricità: 3dp extra (8dp outer + 3dp = 11dp clearance)
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                    }
                }
            }
        }
    }
}
