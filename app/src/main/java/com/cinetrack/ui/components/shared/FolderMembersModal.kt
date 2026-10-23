package com.cinetrack.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.model.SharedFolderInviteDto
import com.cinetrack.data.model.SharedFolderMemberDto
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.toComposeColor
import dev.chrisbanes.haze.HazeState
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun FolderMembersModal(
    folder: FolderEntity,
    members: List<SharedFolderMemberDto>,
    activeInvites: List<SharedFolderInviteDto>,
    currentUserId: String,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onRemoveMember: (SharedFolderMemberDto) -> Unit,
    onLeaveFolder: () -> Unit,
    onRevokeInvite: (SharedFolderInviteDto) -> Unit,
    onOpenShareModal: () -> Unit,
    hazeState: HazeState? = null
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Members, 1 = Invites
    var memberToRemove by remember { mutableStateOf<SharedFolderMemberDto?>(null) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    val folderColor = remember(folder.color) { folder.color.toComposeColor() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM HH:mm", java.util.Locale.getDefault()) }

    FlickTroveModal(
        isVisible = true,
        onDismissRequest = onDismiss,
        hazeState = hazeState,
        maxWidth = 440.dp,
        maxWidthFraction = 0.92f,
        contentPadding = PaddingValues(0.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 16.dp, top = 22.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.folder_members_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = folderColor
                )
            }
            ModalCloseButton(onClose = onDismiss)
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selectedTab == 0) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.folder_members_tab, members.size),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.5f)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selectedTab == 1) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.folder_invites_tab),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 360.dp)
                .padding(horizontal = 22.dp)
        ) {
            if (selectedTab == 0) {
                // Members List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members, key = { it.userId }) { member ->
                        val isSelf = member.userId == currentUserId
                        val isOwnerMember = member.role.equals("owner", ignoreCase = true)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(folderColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = folderColor
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = member.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White
                                    )
                                    if (isSelf) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "(Tu)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (isOwnerMember) {
                                        stringResource(R.string.folder_member_role_owner)
                                    } else {
                                        stringResource(R.string.folder_member_role_editor)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOwnerMember) folderColor else Color.White.copy(alpha = 0.5f)
                                )
                            }

                            // Owner can remove other members
                            if (isOwner && !isOwnerMember) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { memberToRemove = member }
                                        .padding(6.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_trash),
                                        contentDescription = stringResource(R.string.folder_member_remove),
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Invites List
                if (activeInvites.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.folder_invites_none),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeInvites, key = { it.inviteCode }) { invite ->
                            val expiryText = remember(invite.expiresAt) {
                                try {
                                    val instant = Instant.parse(invite.expiresAt)
                                    val ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                                    ldt.format(dateFormatter)
                                } catch (e: Exception) {
                                    invite.expiresAt
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = invite.inviteCode,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.folder_invite_expires_in, expiryText),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }

                                if (isOwner) {
                                    TextButton(
                                        onClick = { onRevokeInvite(invite) }
                                    ) {
                                        Text(
                                            text = stringResource(R.string.folder_invite_revoke),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.Red.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Bottom action bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            if (isOwner) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick(scaleDown = 0.98f, onClick = onOpenShareModal)
                        .clip(RoundedCornerShape(14.dp))
                        .background(folderColor)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.folder_invite_create_button),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            } else {
                // Non-owner member can leave
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick(scaleDown = 0.98f, onClick = { showLeaveConfirm = true })
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Red.copy(alpha = 0.15f))
                        .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.folder_leave),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Red
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    // Confirm remove member
    memberToRemove?.let { target ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = {
                Text(
                    text = stringResource(R.string.folder_member_remove),
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.folder_member_remove_confirm, target.displayName),
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveMember(target)
                        memberToRemove = null
                    }
                ) {
                    Text(stringResource(R.string.folder_member_remove), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text(stringResource(R.string.settings_cancel), color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF1E1E24)
        )
    }

    // Confirm leave folder
    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.folder_leave),
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.folder_leave_confirm),
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveConfirm = false
                        onLeaveFolder()
                    }
                ) {
                    Text(stringResource(R.string.folder_leave), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text(stringResource(R.string.settings_cancel), color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF1E1E24)
        )
    }
}
