package com.cinetrack.ui.components.folder

import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.components.shared.ColorWheel

import androidx.compose.runtime.*
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.model.SharedFolderInviteDto
import com.cinetrack.data.model.SharedFolderMemberDto
import com.cinetrack.ui.ModalHostState
import com.cinetrack.ui.viewmodel.FoldersViewModel
import com.google.firebase.auth.FirebaseAuth
import dev.chrisbanes.haze.HazeState

/**
 * Host component managing the registration and lifecycle of dialogs and modals in FoldersScreen:
 * - Create Folder Dialog
 * - Join Folder Dialog
 * - Delete / Leave Folder Dialog
 * - Edit Folder Dialog
 * - Share Folder Modal
 * - Folder Members Modal
 */
@Composable
fun FolderModalsHost(
    isCreateDialogOpen: Boolean,
    isJoinDialogOpen: Boolean,
    joinDialogInitialCode: String,
    folderToDelete: FolderEntity?,
    folderToEdit: FolderEntity?,
    folderEditMode: FolderEditMode,
    folderToShare: FolderEntity?,
    folderForMembers: FolderEntity?,
    dialogHazeState: HazeState?,
    modalHost: ModalHostState,
    viewModel: FoldersViewModel,
    onCloseCreateDialog: () -> Unit,
    onCloseJoinDialog: () -> Unit,
    onClearFolderToDelete: () -> Unit,
    onClearFolderToEdit: () -> Unit,
    onClearFolderToShare: () -> Unit,
    onClearFolderForMembers: () -> Unit,
    onOpenShareModal: (FolderEntity) -> Unit
) {
    // 1. Create Folder Dialog
    LaunchedEffect(isCreateDialogOpen) {
        if (isCreateDialogOpen) {
            modalHost.show {
                FolderCreateDialog(
                    onDismiss = {
                        onCloseCreateDialog()
                        modalHost.dismiss()
                    },
                    onCreate = { name, icon, color, description ->
                        viewModel.createFolder(name, icon, color, description ?: "")
                        onCloseCreateDialog()
                        modalHost.dismiss()
                    },
                    hazeState = dialogHazeState
                )
            }
        }
    }

    // 2. Join Folder Dialog
    LaunchedEffect(isJoinDialogOpen) {
        if (isJoinDialogOpen) {
            modalHost.show {
                FolderJoinDialog(
                    initialCode = joinDialogInitialCode,
                    onDismiss = {
                        onCloseJoinDialog()
                        modalHost.dismiss()
                    },
                    onCheckCode = { code, callback ->
                        viewModel.checkInvite(code, callback)
                    },
                    onAcceptInvite = { code, callback ->
                        viewModel.acceptInvite(code, callback)
                    },
                    hazeState = dialogHazeState
                )
            }
        }
    }

    // 3. Delete / Leave Folder Dialog
    LaunchedEffect(folderToDelete) {
        val folder = folderToDelete
        if (folder != null) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val isLeave = folder.isShared && (folder.ownerUid != currentUserId)
            modalHost.show {
                DeleteFolderDialog(
                    onConfirm = {
                        viewModel.deleteFolder(folder.id)
                        onClearFolderToDelete()
                        modalHost.dismiss()
                    },
                    onDismiss = {
                        onClearFolderToDelete()
                        modalHost.dismiss()
                    },
                    folderName = folder.name,
                    isLeave = isLeave,
                    hazeState = dialogHazeState
                )
            }
        }
    }

    // 4. Edit Folder Dialog
    LaunchedEffect(folderToEdit) {
        val folder = folderToEdit
        if (folder != null) {
            modalHost.show {
                FolderEditDialog(
                    initialName = folder.name,
                    initialColor = folder.color ?: "#FFFFFF",
                    initialDescription = folder.description,
                    editMode = folderEditMode,
                    onDismiss = {
                        onClearFolderToEdit()
                        modalHost.dismiss()
                    },
                    onSave = { newName, newColor, newDesc ->
                        viewModel.updateFolder(folder.copy(name = newName, color = newColor, description = newDesc))
                        onClearFolderToEdit()
                        modalHost.dismiss()
                    },
                    hazeState = dialogHazeState
                )
            }
        }
    }

    // 5. Share Folder Modal
    LaunchedEffect(folderToShare) {
        val folder = folderToShare
        if (folder != null) {
            modalHost.show {
                FolderShareModal(
                    folder = folder,
                    onDismiss = {
                        onClearFolderToShare()
                        modalHost.dismiss()
                    },
                    onCreateInvite = { durationOption, callback ->
                        viewModel.createInviteForFolder(folder, durationOption, callback)
                    },
                    hazeState = dialogHazeState
                )
            }
        }
    }

    // 6. Folder Members Modal
    LaunchedEffect(folderForMembers) {
        val folder = folderForMembers
        if (folder != null) {
            modalHost.show {
                FolderMembersModalHostContent(
                    folder = folder,
                    viewModel = viewModel,
                    dialogHazeState = dialogHazeState,
                    onDismiss = {
                        onClearFolderForMembers()
                        modalHost.dismiss()
                    },
                    onOpenShareModal = { target ->
                        onClearFolderForMembers()
                        modalHost.dismiss()
                        onOpenShareModal(target)
                    }
                )
            }
        }
    }
}

@Composable
private fun FolderMembersModalHostContent(
    folder: FolderEntity,
    viewModel: FoldersViewModel,
    dialogHazeState: HazeState?,
    onDismiss: () -> Unit,
    onOpenShareModal: (FolderEntity) -> Unit
) {
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val isOwner = folder.ownerUid == currentUserId
    var members by remember(folder.sharedFolderId) {
        mutableStateOf<List<SharedFolderMemberDto>>(emptyList())
    }
    var activeInvites by remember(folder.sharedFolderId) {
        mutableStateOf<List<SharedFolderInviteDto>>(emptyList())
    }

    LaunchedEffect(folder.sharedFolderId) {
        folder.sharedFolderId?.let { sharedId ->
            viewModel.getFolderMembers(sharedId) { res ->
                members = res.getOrDefault(emptyList())
            }
            viewModel.getFolderInvites(sharedId) { res ->
                activeInvites = res.getOrDefault(emptyList())
                if (isOwner) {
                    viewModel.checkAutoRevert(folder) { didRevert ->
                        if (didRevert) {
                            onDismiss()
                        }
                    }
                }
            }
        }
    }

    FolderMembersModal(
        folder = folder,
        members = members,
        activeInvites = activeInvites,
        currentUserId = currentUserId,
        isOwner = isOwner,
        onDismiss = onDismiss,
        onRemoveMember = { member ->
            viewModel.removeFolderMember(folder, member.userId) { res ->
                if (res.isSuccess) {
                    val didRevert = res.getOrDefault(false)
                    if (didRevert) {
                        onDismiss()
                    } else {
                        members = members.filter { it.userId != member.userId }
                    }
                }
            }
        },
        onLeaveFolder = {
            viewModel.deleteFolder(folder.id)
            onDismiss()
        },
        onRevokeInvite = { invite ->
            viewModel.revokeFolderInvite(folder, invite.inviteCode) { res ->
                if (res.isSuccess) {
                    val didRevert = res.getOrDefault(false)
                    if (didRevert) {
                        onDismiss()
                    } else {
                        activeInvites = activeInvites.filter { it.inviteCode != invite.inviteCode }
                    }
                }
            }
        },
        onOpenShareModal = {
            onOpenShareModal(folder)
        },
        hazeState = dialogHazeState
    )
}

