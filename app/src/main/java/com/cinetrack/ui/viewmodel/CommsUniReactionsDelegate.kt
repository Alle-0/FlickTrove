package com.cinetrack.ui.viewmodel

import android.util.Log
import com.cinetrack.data.model.AppComment
import com.cinetrack.data.repository.CommentRepository
import com.cinetrack.data.repository.CommsUniRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.UiText
import com.cinetrack.util.SupabaseNotificationService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Delegate responsible for CommsUni comment reactions (likes/unlikes), optimistic local UI state,
 * Firestore/CommsUni two-way syncing and social notifications.
 */
class CommsUniReactionsDelegate @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val commsUniRepository: CommsUniRepository,
    private val commentRepository: CommentRepository,
    private val actionFeedbackManager: ActionFeedbackManager
) {
    fun toggleLikeComment(
        commentId: String,
        mediaTitle: String,
        mediaImage: String?,
        commentsState: MutableStateFlow<List<AppComment>>,
        currentAuthorId: String?,
        currentActorId: String?,
        currentRawMediaId: String,
        currentEntityType: String,
        currentEntityId: String,
        isCommentLikedByMe: (AppComment) -> Boolean,
        resolveFirebaseUidForAuthor: suspend (String) -> String?,
        scope: CoroutineScope
    ) {
        val currentAuthUid = auth.currentUser?.uid ?: return
        val authorId = currentAuthorId
        val actorId = currentActorId

        scope.launch {
            val currentList = commentsState.value.toMutableList()
            val commentIndex = currentList.indexOfFirst { it.id == commentId }
            if (commentIndex == -1) return@launch

            val comment = currentList[commentIndex]
            val isLiked = isCommentLikedByMe(comment)
            val myIds = listOfNotNull(currentAuthUid, authorId, actorId)

            // Optimistic update
            val newLikedBy = if (isLiked) {
                comment.likedBy.filterNot { it in myIds }
            } else {
                (comment.likedBy + currentAuthUid).distinct()
            }

            val updatedComment = comment.copy(
                likedBy = newLikedBy,
                likesCount = maxOf(0, comment.likesCount + if (isLiked) -1 else 1),
                nativeLikes = maxOf(0, comment.nativeLikes + if (isLiked) -1 else 1)
            )

            currentList[commentIndex] = updatedComment
            commentsState.value = currentList

            commsUniRepository.recordLikeOverride(
                commentId = comment.id,
                isLiked = !isLiked,
                likesCount = updatedComment.likesCount,
                alternativeId = comment.firestoreId
            )

            // Network call
            val isFlickTrove = comment.originSlug.equals("flicktrove", ignoreCase = true) || comment.originSlug.isBlank()
            if (isFlickTrove && currentRawMediaId.isNotBlank()) {
                try {
                    val targetFsId = comment.firestoreId?.takeIf { it.isNotBlank() } ?: commentId
                    commentRepository.toggleLike(currentRawMediaId, targetFsId, currentEntityType, mediaTitle, mediaImage, commentSnippet = comment.text)
                } catch (_: Exception) {}
            }
            val result = if (currentEntityId.isNotBlank()) {
                if (isLiked) {
                    commsUniRepository.unlikeComment(commentId)
                } else {
                    commsUniRepository.likeComment(commentId)
                }
            } else {
                Result.success(Unit)
            }

            if (result.isFailure && !isFlickTrove) {
                // Revert
                val revertList = commentsState.value.toMutableList()
                val revertIndex = revertList.indexOfFirst { it.id == commentId }
                if (revertIndex != -1) {
                    revertList[revertIndex] = comment
                    commentsState.value = revertList
                    commsUniRepository.recordLikeOverride(
                        commentId = comment.id,
                        isLiked = isLiked,
                        likesCount = comment.likesCount,
                        alternativeId = comment.firestoreId
                    )
                }
                actionFeedbackManager.emit(UiText.DynamicString("Errore di rete con CommsUni."))
                return@launch
            }

            // Se il commento appartiene a un FlickTrover, invia/rimuovi notifica Social e Push
            if (comment.originSlug.equals("flicktrove", ignoreCase = true) && !comment.userId.isBlank()) {
                launch(Dispatchers.IO) {
                    try {
                        val targetUid = resolveFirebaseUidForAuthor(comment.userId)
                        if (targetUid != null && targetUid != currentAuthUid) {
                            val notifId = "${commentId}_${currentAuthUid}_like"
                            val notifRef = firestore.collection("user_social_notifications")
                                .document(targetUid)
                                .collection("items")
                                .document(notifId)

                            if (!isLiked) {
                                val notif = hashMapOf(
                                    "id" to notifRef.id,
                                    "type" to "like",
                                    "mediaId" to currentRawMediaId,
                                    "mediaType" to currentEntityType,
                                    "mediaTitle" to mediaTitle,
                                    "mediaImage" to mediaImage,
                                    "commentId" to commentId,
                                    "senderName" to (auth.currentUser?.displayName ?: "Qualcuno"),
                                    "senderUserId" to currentAuthUid,
                                    "snippet" to comment.text.take(80),
                                    "createdAt" to Timestamp.now(),
                                    "isRead" to false
                                )
                                notifRef.set(notif, SetOptions.merge()).await()

                                SupabaseNotificationService.notifyUser(
                                    targetUserId = targetUid,
                                    titleLocKey = "notification_like_title",
                                    bodyLocKey = "notification_like_body",
                                    bodyLocArgs = listOf(auth.currentUser?.displayName ?: "Qualcuno"),
                                    mediaId = currentRawMediaId.toLongOrNull() ?: 0L,
                                    mediaType = currentEntityType,
                                    mediaImage = mediaImage,
                                    commentId = commentId
                                )
                            } else {
                                notifRef.delete().await()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CommsUniReactionsDelegate", "Failed to update like social notification", e)
                    }
                }
            }
        }
    }
}
