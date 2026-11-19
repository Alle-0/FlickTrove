package com.cinetrack.ui.viewmodel

import android.util.Log
import com.cinetrack.R
import com.cinetrack.data.model.AppComment
import com.cinetrack.data.repository.BadgeRepository
import com.cinetrack.data.repository.BlockedAuthorsManager
import com.cinetrack.data.repository.CommentRepository
import com.cinetrack.data.repository.CommsUniRepository
import com.cinetrack.data.repository.StorageRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Delegate responsible for CommsUni moderation, user reporting, author blocking/unblocking,
 * spoiler toggling and comment deletion.
 */
class CommsUniModerationDelegate @Inject constructor(
    private val commentRepository: CommentRepository,
    private val commsUniRepository: CommsUniRepository,
    private val blockedAuthorsManager: BlockedAuthorsManager,
    private val storageRepository: StorageRepository,
    private val badgeRepository: BadgeRepository,
    private val actionFeedbackManager: ActionFeedbackManager
) {
    fun toggleSpoilerStatus(
        commentId: String,
        currentStatus: Boolean,
        commentsState: MutableStateFlow<List<AppComment>>,
        currentRawMediaId: String,
        scope: CoroutineScope
    ) {
        val targetStatus = !currentStatus
        // Optimistic update locally
        val currentList = commentsState.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == commentId }
        val targetComment = currentList.getOrNull(index)

        if (index != -1) {
            currentList[index] = currentList[index].copy(isSpoiler = targetStatus)
            commentsState.value = currentList
        }

        scope.launch {
            var success = false
            // Aggiorna su CommsUni
            val commsUniRes = commsUniRepository.updateCommentSpoiler(commentId, targetStatus)
            if (commsUniRes.isSuccess) {
                success = true
            }
            // Aggiorna anche su Firestore se presente
            if (currentRawMediaId.isNotBlank()) {
                val firestoreRes = try {
                    val targetFsId = targetComment?.firestoreId?.takeIf { it.isNotBlank() } ?: commentId
                    commentRepository.toggleSpoiler(currentRawMediaId, targetFsId, targetStatus)
                } catch (_: Exception) {
                    false
                }
                if (firestoreRes) {
                    success = true
                }
            }

            if (!success) {
                // Revert on failure
                val revertList = commentsState.value.toMutableList()
                val revertIndex = revertList.indexOfFirst { it.id == commentId }
                if (revertIndex != -1) {
                    revertList[revertIndex] = revertList[revertIndex].copy(isSpoiler = currentStatus)
                    commentsState.value = revertList
                }
                actionFeedbackManager.emit(UiText.DynamicString("Impossibile aggiornare lo spoiler."))
            }
        }
    }

    fun reportComment(
        commentId: String,
        category: String,
        text: String? = null,
        userId: String? = null,
        userDisplayName: String? = null,
        detail: String? = null,
        commentsState: MutableStateFlow<List<AppComment>>,
        currentRawMediaId: String,
        scope: CoroutineScope
    ) {
        scope.launch {
            val targetComment = commentsState.value.find { it.id == commentId }
            val isFlickTrove = targetComment?.originSlug?.equals("flicktrove", ignoreCase = true) == true || targetComment?.originSlug.isNullOrBlank()

            if (isFlickTrove && currentRawMediaId.isNotBlank()) {
                val fResult = try {
                    commentRepository.reportComment(
                        mediaId = currentRawMediaId,
                        commentId = commentId,
                        reason = category.uppercase(),
                        commentText = text ?: targetComment?.text ?: "",
                        commentAuthorId = userId ?: targetComment?.userId ?: "",
                        commentAuthorName = userDisplayName ?: targetComment?.userDisplayName ?: ""
                    )
                } catch (e: Exception) {
                    CommentRepository.ReportResult.ERROR
                }

                when (fResult) {
                    CommentRepository.ReportResult.SUCCESS -> {
                        actionFeedbackManager.emit(UiText.StringResource(R.string.comment_report_success))
                    }
                    CommentRepository.ReportResult.COOLDOWN -> {
                        actionFeedbackManager.emit(UiText.StringResource(R.string.comment_report_duplicate))
                    }
                    CommentRepository.ReportResult.ERROR -> {
                        actionFeedbackManager.emit(UiText.StringResource(R.string.comment_report_error))
                    }
                }
            } else {
                val result = commsUniRepository.reportComment(commentId, reason = category, detail = detail)
                result.onSuccess { reportStatus ->
                    when (reportStatus) {
                        CommsUniRepository.ReportCommentResult.SUCCESS -> {
                            actionFeedbackManager.emit(UiText.StringResource(R.string.comment_report_success))
                        }
                        CommsUniRepository.ReportCommentResult.DUPLICATE -> {
                            actionFeedbackManager.emit(UiText.StringResource(R.string.comment_report_duplicate))
                        }
                        CommsUniRepository.ReportCommentResult.ERROR -> {
                            actionFeedbackManager.emit(UiText.StringResource(R.string.comment_report_error))
                        }
                    }
                }.onFailure {
                    actionFeedbackManager.emit(UiText.StringResource(R.string.comment_report_error))
                }
            }
        }
    }

    fun blockAuthor(
        userId: String,
        authorName: String,
        commentsState: MutableStateFlow<List<AppComment>>,
        scope: CoroutineScope
    ) {
        val cleanName = authorName.trim()
        val cleanId = userId.trim()
        val effectiveId = if (cleanId.isNotBlank()) cleanId else "name_${cleanName.lowercase()}"
        if (effectiveId.isBlank()) return

        scope.launch {
            blockedAuthorsManager.blockAuthor(effectiveId, cleanName)
            commentsState.value = commentsState.value.filterNot { 
                it.userId == effectiveId || 
                (cleanId.isNotBlank() && it.userId == cleanId) || 
                (cleanName.isNotBlank() && it.userDisplayName.equals(cleanName, ignoreCase = true))
            }
            actionFeedbackManager.emit(
                UiText.StringResource(R.string.comment_block_user_success, cleanName.ifBlank { "User" })
            )
        }
    }

    fun unblockAuthor(
        userId: String,
        scope: CoroutineScope,
        onRefresh: () -> Unit
    ) {
        if (userId.isBlank()) return
        scope.launch {
            blockedAuthorsManager.unblockAuthor(userId)
            actionFeedbackManager.emit(
                UiText.StringResource(R.string.comment_unblock_user_success)
            )
            onRefresh()
        }
    }

    fun emitBlockedLink(reasonResId: Int, scope: CoroutineScope) {
        scope.launch {
            actionFeedbackManager.emit(UiText.StringResource(reasonResId))
        }
    }

    fun deleteComment(
        commentId: String,
        targetComment: AppComment? = null,
        commentsState: MutableStateFlow<List<AppComment>>,
        currentEntityId: String,
        currentRawMediaId: String,
        untrackMyCommentId: (String) -> Unit,
        scope: CoroutineScope
    ) {
        scope.launch {
            // Optimistic: remove immediately
            val currentList = commentsState.value.toMutableList()
            val removed = currentList.find { it.id == commentId } ?: targetComment
            currentList.removeAll { it.id == commentId }
            commentsState.value = currentList

            val mediaRegex = Regex("""!\[.*?\]\((.*?)\)""")
            val inlineUrls = removed?.let { mediaRegex.findAll(it.text).map { m -> m.groupValues[1] }.toList() } ?: emptyList()
            val allMediaUrls = ((removed?.attachedMedia ?: emptyList()) + inlineUrls).distinct()

            var anySuccess = false
            var firestoreDeleted = false

            // 1. Elimina su CommsUni (se connesso a un'entità di rete)
            if (currentEntityId.isNotBlank()) {
                val commsRes = commsUniRepository.deleteComment(commentId)
                if (commsRes.isSuccess) {
                    anySuccess = true
                }
            }

            // 2. Elimina anche su Firestore locale se presente
            if (currentRawMediaId.isNotBlank()) {
                val firestoreRes = try {
                    val targetFsId = (removed ?: targetComment)?.firestoreId?.takeIf { it.isNotBlank() } ?: commentId
                    val res = commentRepository.deleteComment(currentRawMediaId, targetFsId)
                    res != CommentRepository.DeleteCommentResult.FAILED
                } catch (_: Exception) {
                    false
                }
                if (firestoreRes) {
                    anySuccess = true
                    firestoreDeleted = true
                }
            }

            if (anySuccess) {
                untrackMyCommentId(commentId)
                badgeRepository.recordCommentDeleted()
                // Se non è già stato eliminato tramite CommentRepository su Firestore, elimina le immagini da Supabase Storage
                if (!firestoreDeleted) {
                    allMediaUrls.forEach { imageUrl ->
                        if (imageUrl.contains("supabase.co") || imageUrl.contains("supabase")) {
                            val delRes = storageRepository.deleteCommentImage(imageUrl)
                            if (delRes.isFailure) {
                                Log.e("CommsUniModerationDelegate", "Failed to delete Supabase comment image: $imageUrl", delRes.exceptionOrNull())
                            } else {
                                Log.d("CommsUniModerationDelegate", "Successfully deleted Supabase comment image: $imageUrl")
                            }
                        }
                    }
                }
            } else {
                // Revert in caso di fallimento su entrambi
                removed?.let {
                    val revertList = commentsState.value.toMutableList()
                    revertList.add(it)
                    commentsState.value = revertList.sortedBy { c -> c.createdAt?.seconds ?: 0L }
                }
                actionFeedbackManager.emit(UiText.DynamicString("Impossibile eliminare il commento."))
            }
        }
    }
}
