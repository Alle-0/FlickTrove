package com.cinetrack.ui.components.folder

import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.components.shared.ColorWheel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import com.cinetrack.ui.utils.UiText
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.rememberActionFeedbackManager
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.ui.components.common.CategoryTabSelector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cinetrack.util.toComposeColor
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object FolderMembersAvatarCache {
    val cache = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun get(userId: String): String? = cache[userId]

    fun put(userId: String, url: String) {
        cache[userId] = url
    }

    fun invalidate(userId: String) {
        cache.remove(userId)
    }
}

private fun extractInitials(displayName: String): String {
    val parts = displayName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> (parts[0].take(1) + parts[1].take(1)).uppercase()
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "?"
    }
}

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
    val context = LocalContext.current
    val feedbackManager = rememberActionFeedbackManager()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Members, 1 = Invites
    var memberToRemove by remember { mutableStateOf<SharedFolderMemberDto?>(null) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    val membersListState = rememberLazyListState()
    val invitesListState = rememberLazyListState()

    val folderColor = remember(folder.color) { folder.color.toComposeColor() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM HH:mm", java.util.Locale.getDefault()) }

    val validInvites = remember(activeInvites) {
        activeInvites.filter { invite ->
            !invite.isRevoked &&
            (invite.maxUses == null || invite.usedCount < invite.maxUses) &&
            !invite.isExpired
        }
    }

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
                    text = if (isOwner) {
                        stringResource(R.string.folder_members_title)
                    } else {
                        stringResource(R.string.folder_members_only_title)
                    },
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

        if (isOwner) {
            // Tabs with classic FlickTrove physics (Owner only)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 6.dp)
                    .hazeGlass(
                        shape = CircleShape,
                        containerColor = Color.White.copy(alpha = 0.05f),
                        borderColor = Color.White.copy(alpha = 0.12f),
                        borderWidth = 1.dp,
                        useRefraction = true
                    )
            ) {
                CategoryTabSelector(
                    options = listOf(
                        stringResource(R.string.folder_members_tab, members.size),
                        stringResource(R.string.folder_invites_tab_count, validInvites.size)
                    ),
                    selectedIndex = selectedTab,
                    onOptionClick = { selectedTab = it },
                    modifier = Modifier.fillMaxWidth(),
                    fillWidth = true,
                    indicatorColor = folderColor,
                    uppercase = false
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        // Content with spring morph size animation and fading gradients
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)))
                    .togetherWith(fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)))
                    .using(
                        SizeTransform(
                            clip = true,
                            sizeAnimationSpec = { _, _ ->
                                spring(
                                    dampingRatio = 0.82f,
                                    stiffness = 380f
                                )
                            }
                        )
                    )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            label = "FolderMembersTabMorph"
        ) { tab ->
            if (tab == 0) {
                // Members List
                LazyColumn(
                    state = membersListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .verticalFadingEdges(membersListState, topEdgeHeight = 20.dp, bottomEdgeHeight = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(members, key = { it.userId }) { member ->
                        val isSelf = member.userId == currentUserId
                        val isOwnerMember = member.role.equals("owner", ignoreCase = true)
                        val currentUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
                        var firestoreAvatarUrl by remember { mutableStateOf<String?>(null) }

                        LaunchedEffect(member.userId) {
                            if (member.userAvatarUrl.isNullOrBlank()) {
                                val cached = FolderMembersAvatarCache.get(member.userId)
                                if (cached != null) {
                                    firestoreAvatarUrl = cached.takeIf { it.isNotBlank() }
                                } else {
                                    try {
                                        val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(member.userId)
                                            .get()
                                            .await()
                                        val photo = doc.getString("photoUrl")?.trim().orEmpty()
                                        FolderMembersAvatarCache.put(member.userId, photo)
                                        firestoreAvatarUrl = photo.takeIf { it.isNotBlank() }
                                    } catch (_: Exception) {
                                        FolderMembersAvatarCache.put(member.userId, "")
                                    }
                                }
                            }
                        }

                        val avatarUrl = remember(member.userAvatarUrl, isSelf, firestoreAvatarUrl) {
                            when {
                                !member.userAvatarUrl.isNullOrBlank() -> member.userAvatarUrl
                                isSelf && !currentUser?.photoUrl?.toString().isNullOrBlank() -> currentUser.photoUrl?.toString()
                                else -> firestoreAvatarUrl
                            }
                        }
                        var isAvatarError by remember(avatarUrl) { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(
                                    1.dp,
                                    com.cinetrack.ui.theme.HazeStyles.GlassBorderColor.copy(alpha = com.cinetrack.ui.theme.HazeStyles.GlassBorderAlphaTop),
                                    CircleShape
                                )
                                .padding(
                                    start = 7.dp,
                                    top = 7.dp,
                                    bottom = 7.dp,
                                    end = if (isOwner && !isOwnerMember) 11.dp else 18.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(folderColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = extractInitials(member.displayName),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = folderColor
                                )

                                if (!avatarUrl.isNullOrBlank() && !isAvatarError) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(avatarUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = member.displayName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        onError = { isAvatarError = true }
                                    )
                                }
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
                                            text = stringResource(R.string.folder_member_you),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isOwnerMember) folderColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f))
                                        .border(
                                            0.5.dp,
                                            if (isOwnerMember) folderColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.14f),
                                            CircleShape
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isOwnerMember) {
                                            stringResource(R.string.folder_member_role_owner)
                                        } else {
                                            stringResource(R.string.folder_member_role_editor)
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        ),
                                        color = if (isOwnerMember) folderColor else Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }

                            // Owner can remove other members
                            if (isOwner && !isOwnerMember) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .bounceClick(scaleDown = 0.88f) { memberToRemove = member }
                                        .clip(CircleShape)
                                        .background(Color.Red.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_trash),
                                        contentDescription = stringResource(R.string.folder_member_remove),
                                        tint = Color.Red.copy(alpha = 0.8f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Invites List
                if (validInvites.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
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
                        state = invitesListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                            .verticalFadingEdges(invitesListState, topEdgeHeight = 20.dp, bottomEdgeHeight = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(validInvites, key = { it.inviteCode }) { invite ->
                            val expiryText = remember(invite.expiresAt) {
                                val instant = invite.getExpiresAtInstant()
                                if (instant != null) {
                                    val userZone = try {
                                        java.util.TimeZone.getDefault().toZoneId()
                                    } catch (e: Exception) {
                                        ZoneId.systemDefault()
                                    }
                                    val ldt = instant.atZone(userZone).toLocalDateTime()
                                    ldt.format(dateFormatter)
                                } else {
                                    invite.expiresAt
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                                    Box(
                                        modifier = Modifier
                                            .bounceClick(scaleDown = 0.92f) { onRevokeInvite(invite) }
                                            .clip(CircleShape)
                                            .background(Color.Red.copy(alpha = 0.12f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.folder_invite_revoke),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.Red.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Bottom action bar (Nested concentric corners: Modal R=32dp - Padding=16dp -> Button R=16dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            val buttonShape = RoundedCornerShape(16.dp)
            if (isOwner) {
                val isLimitReached = validInvites.size >= 10
                val buttonTextColor = if (isLimitReached) {
                    Color.White.copy(alpha = 0.6f)
                } else if (folderColor.luminance() > 0.45f) {
                    Color.Black
                } else {
                    Color.White
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .bounceClick(scaleDown = 0.96f) {
                            if (isLimitReached) {
                                feedbackManager?.emit(UiText.StringResource(R.string.folder_invites_limit_reached))
                            } else {
                                onOpenShareModal()
                            }
                        }
                        .clip(buttonShape)
                        .background(if (isLimitReached) Color.White.copy(alpha = 0.15f) else folderColor)
                        .border(
                            1.dp,
                            if (isLimitReached) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.25f),
                            buttonShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLimitReached) stringResource(R.string.folder_invites_limit_reached_btn) else stringResource(R.string.folder_invite_create_button),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = buttonTextColor
                    )
                }
            } else {
                // Non-owner member can leave
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .bounceClick(scaleDown = 0.96f, onClick = { showLeaveConfirm = true })
                        .clip(buttonShape)
                        .background(Color.Red.copy(alpha = 0.15f))
                        .border(1.dp, Color.Red.copy(alpha = 0.3f), buttonShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.folder_leave),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Red
                    )
                }
            }
        }
    }

    // Confirm remove member
    memberToRemove?.let { target ->
        RemoveMemberDialog(
            memberName = target.displayName,
            onDismiss = { memberToRemove = null },
            onConfirm = {
                onRemoveMember(target)
                memberToRemove = null
            },
            hazeState = hazeState,
            zIndex = 110f
        )
    }

    // Confirm leave folder
    if (showLeaveConfirm) {
        DeleteFolderDialog(
            onDismiss = { showLeaveConfirm = false },
            onConfirm = {
                showLeaveConfirm = false
                onLeaveFolder()
            },
            folderName = folder.name,
            isLeave = true,
            hazeState = hazeState,
            zIndex = 110f
        )
    }
}
