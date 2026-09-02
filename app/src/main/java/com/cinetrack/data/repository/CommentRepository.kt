package com.cinetrack.data.repository

import com.cinetrack.data.model.AppComment
import com.cinetrack.data.model.CommentReport
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storageRepository: StorageRepository
) {
    enum class DeleteCommentResult { HARD_DELETED, SOFT_DELETED, FAILED }

    private fun getMediaCommentsCollection(mediaId: String) =
        firestore.collection("media_comments").document(mediaId).collection("comments")

    private val reportsCollection = firestore.collection("comment_reports")

    suspend fun getCommentsForMedia(
        mediaId: String,
        limit: Long = 10,
        sortBy: String = "createdAt",
        direction: Query.Direction = Query.Direction.DESCENDING,
        lastVisible: DocumentSnapshot? = null
    ): Pair<List<AppComment>, DocumentSnapshot?> {
        return try {
            var query = getMediaCommentsCollection(mediaId)
                .whereEqualTo("depth", 0)
                .orderBy(sortBy, direction)
                .limit(limit)

            if (lastVisible != null) {
                query = query.startAfter(lastVisible)
            }

            val snapshot = query.get().await()

            val comments = snapshot.toObjects(AppComment::class.java).mapIndexed { index, appComment ->
                appComment.copy(id = snapshot.documents[index].id)
            }

            val newLastVisible = if (snapshot.size() > 0) snapshot.documents[snapshot.size() - 1] else null

            Pair(comments, newLastVisible)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(emptyList(), null)
        }
    }

    suspend fun getRepliesForComment(
        mediaId: String,
        commentId: String
    ): List<AppComment> {
        return try {
            val snapshot = getMediaCommentsCollection(mediaId)
                .whereEqualTo("parentId", commentId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.toObjects(AppComment::class.java).mapIndexed { index, appComment ->
                appComment.copy(id = snapshot.documents[index].id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getTopCommentsForMediaPreview(
        mediaId: String,
        limit: Long = 5
    ): List<AppComment> {
        return try {
            val snapshot = getMediaCommentsCollection(mediaId)
                .whereEqualTo("depth", 0)
                .orderBy("likesCount", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            snapshot.toObjects(AppComment::class.java).mapIndexed { index, appComment ->
                appComment.copy(id = snapshot.documents[index].id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addComment(
        mediaId: String,
        mediaType: String,
        text: String,
        isSpoiler: Boolean = false,
        parentId: String? = null,
        parentUserId: String? = null,
        depth: Int = 0,
        mediaTitle: String = "",
        mediaImage: String? = null
    ): Boolean {
        val user = auth.currentUser ?: return false
        
        // Check if user is banned
        try {
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            val isBanned = userDoc.getBoolean("bannedFromCommenting") == true
            val bannedUntil = userDoc.getTimestamp("bannedUntil")?.toDate()
            if (isBanned || (bannedUntil != null && bannedUntil.after(java.util.Date()))) {
                return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val newComment = AppComment(
            id = "", // Will be set by Firestore
            mediaId = mediaId,
            mediaType = mediaType,
            userId = user.uid,
            userDisplayName = user.displayName ?: "Anonimo",
            userAvatarUrl = user.photoUrl?.toString() ?: "",
            text = text,
            isSpoiler = isSpoiler,
            parentId = parentId,
            parentUserId = parentUserId,
            depth = depth
        )

        return try {
            val generatedCommentId = firestore.runTransaction { transaction ->
                val mediaCommentsColl = getMediaCommentsCollection(mediaId)
                val newDocRef = mediaCommentsColl.document()
                transaction.set(newDocRef, newComment)

                // If it's a reply, increment the parent's repliesCount
                if (parentId != null) {
                    val parentRef = mediaCommentsColl.document(parentId)
                    transaction.update(parentRef, "repliesCount", FieldValue.increment(1))
                }
                newDocRef.id
            }.await()

            // Resolve the target user for the notification.
            // If parentUserId isn't provided by the UI (or equals the current user),
            // fall back to the owner stored in Firestore for the parent comment.
            val resolvedParentUserId = when {
                parentUserId != null && parentUserId != user.uid -> parentUserId
                parentId != null -> {
                    try {
                        val parentSnap = getMediaCommentsCollection(mediaId).document(parentId).get().await()
                        parentSnap.getString("userId").takeUnless { it.isNullOrEmpty() || it == user.uid }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                else -> null
            }

            if (parentId != null && resolvedParentUserId != null) {
                // Inserimento della notifica Social in-app (Risposta)
                val notifRef = firestore.collection("user_social_notifications")
                    .document(resolvedParentUserId)
                    .collection("items")
                    .document()
                val socialNotif = hashMapOf(
                    "id" to notifRef.id,
                    "type" to "reply",
                    "mediaId" to mediaId,
                    "mediaType" to mediaType,
                    "mediaTitle" to mediaTitle,
                    "mediaImage" to mediaImage,
                    "commentId" to generatedCommentId,
                    "senderName" to (user.displayName ?: "Qualcuno"),
                    "senderUserId" to user.uid,
                    "createdAt" to Timestamp.now(),
                    "isRead" to false
                )
                try {
                    notifRef.set(socialNotif).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val prefsDoc = firestore.collection("users").document(resolvedParentUserId)
                            .collection("settings").document("preferences").get().await()
                        val notificationsSocial = prefsDoc.getBoolean("notificationsSocial") ?: true

                        if (notificationsSocial) {
                            com.cinetrack.util.SupabaseNotificationService.notifyUser(
                                targetUserId = resolvedParentUserId,
                                titleLocKey = "notification_reply_title",
                                bodyLocKey = "notification_reply_body",
                                bodyLocArgs = listOf(user.displayName ?: "Qualcuno"),
                                mediaId = mediaId.toLongOrNull() ?: 0L,
                                mediaType = mediaType,
                                mediaImage = mediaImage,
                                commentId = generatedCommentId
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun toggleLike(
        mediaId: String,
        commentId: String,
        mediaType: String,
        mediaTitle: String,
        mediaImage: String? = null
    ): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val docRef = getMediaCommentsCollection(mediaId).document(commentId)

        var targetOwnerId: String? = null
        var isNewLike = false
        var isUnlike = false

        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) return@runTransaction

                val currentLikes = snapshot.get("likedBy") as? List<*> ?: emptyList<String>()
                val isLiking = !currentLikes.contains(userId)

                val commentOwnerId = snapshot.getString("userId")

                if (!isLiking) {
                    transaction.update(docRef, "likesCount", FieldValue.increment(-1))
                    transaction.update(docRef, "likedBy", FieldValue.arrayRemove(userId))
                    
                    if (commentOwnerId != null && commentOwnerId != userId) {
                        targetOwnerId = commentOwnerId
                        isUnlike = true
                    }
                } else {
                    transaction.update(docRef, "likesCount", FieldValue.increment(1))
                    transaction.update(docRef, "likedBy", FieldValue.arrayUnion(userId))

                    if (commentOwnerId != null && commentOwnerId != userId) {
                        targetOwnerId = commentOwnerId
                        isNewLike = true
                    }
                }
            }.await()

            if (targetOwnerId != null) {
                val commentOwnerId = targetOwnerId!!
                val notifId = "${commentId}_${userId}_like"
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val notifRef = firestore.collection("user_social_notifications")
                            .document(commentOwnerId).collection("items").document(notifId)

                        if (isNewLike) {
                            val notif = hashMapOf(
                                "id" to notifRef.id,
                                "type" to "like",
                                "mediaId" to mediaId,
                                "mediaType" to mediaType,
                                "mediaTitle" to mediaTitle,
                                "mediaImage" to mediaImage,
                                "commentId" to commentId,
                                "senderName" to (auth.currentUser?.displayName ?: "Qualcuno"),
                                "senderUserId" to userId,
                                "createdAt" to Timestamp.now(),
                                "isRead" to false
                            )
                            notifRef.set(notif).await()

                            val prefsDoc = firestore.collection("users").document(commentOwnerId)
                                .collection("settings").document("preferences").get().await()
                            val notificationsSocial = prefsDoc.getBoolean("notificationsSocial") ?: true

                            if (notificationsSocial) {
                                com.cinetrack.util.SupabaseNotificationService.notifyUser(
                                    targetUserId = commentOwnerId,
                                    titleLocKey = "notification_like_title",
                                    bodyLocKey = "notification_like_body",
                                    bodyLocArgs = listOf(auth.currentUser?.displayName ?: "Qualcuno"),
                                    mediaId = mediaId.toLongOrNull() ?: 0L,
                                    mediaType = mediaType,
                                    mediaImage = mediaImage,
                                    commentId = commentId
                                )
                            }
                        } else if (isUnlike) {
                            notifRef.delete().await()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

enum class ReportResult {
    SUCCESS,
    COOLDOWN,
    ERROR
}

    suspend fun reportComment(mediaId: String, commentId: String, reason: String, commentText: String, commentAuthorId: String, commentAuthorName: String): ReportResult {
        val userId = auth.currentUser?.uid ?: return ReportResult.ERROR

        // 24-hour cooldown check
        val oneDayAgo = Timestamp(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
        val recentReports = reportsCollection
            .whereEqualTo("commentId", commentId)
            .whereEqualTo("reportedByUid", userId)
            .whereEqualTo("reason", reason)
            .whereGreaterThan("timestamp", oneDayAgo)
            .get()
            .await()

        if (!recentReports.isEmpty) {
            return ReportResult.COOLDOWN
        }

        val report = CommentReport(
            mediaId = mediaId,
            commentId = commentId,
            reportedByUid = userId,
            reason = reason,
            commentText = commentText,
            commentAuthorId = commentAuthorId,
            commentAuthorName = commentAuthorName,
            timestamp = Timestamp.now()
        )
        return try {
            reportsCollection.add(report).await()

            // Auto-flag spoiler logic
            if (reason == "SPOILER") {
                val spoilerReports = reportsCollection
                    .whereEqualTo("commentId", commentId)
                    .whereEqualTo("reason", "SPOILER")
                    .get()
                    .await()

                if (spoilerReports.size() >= 3) {
                    // Update comment to isSpoiler = true
                    getMediaCommentsCollection(mediaId).document(commentId)
                        .update("isSpoiler", true)
                        .await()
                }
            }

            ReportResult.SUCCESS
        } catch (e: Exception) {
            e.printStackTrace()
            ReportResult.ERROR
        }
    }

    suspend fun updateSpoilerStatus(mediaId: String, commentId: String, isSpoiler: Boolean): Boolean {
        return try {
            getMediaCommentsCollection(mediaId).document(commentId)
                .update("isSpoiler", isSpoiler)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    suspend fun deleteComment(mediaId: String, commentId: String): DeleteCommentResult {
        return try {
            val mediaCommentsColl = getMediaCommentsCollection(mediaId)
            val commentSnapshot = mediaCommentsColl.document(commentId).get().await()
            if (!commentSnapshot.exists()) return DeleteCommentResult.FAILED
            val comment = commentSnapshot.toObject(AppComment::class.java) ?: return DeleteCommentResult.FAILED
            if (comment.userId != auth.currentUser?.uid || comment.isDeleted) return DeleteCommentResult.FAILED

            val repliesSnapshot = mediaCommentsColl.whereEqualTo("parentId", commentId).get().await()

            val imageRegex = Regex("""!\[.*?\]\((.*?)\)""")
            for (match in imageRegex.findAll(comment.text)) {
                val imageUrl = match.groupValues[1]
                if (imageUrl.contains("supabase.co")) {
                    val result = storageRepository.deleteCommentImage(imageUrl)
                    if (result.isFailure) {
                        android.util.Log.e("CommentRepository", "Failed to delete image: $imageUrl", result.exceptionOrNull())
                        return DeleteCommentResult.FAILED
                    }
                }
            }

            val batch = firestore.batch()
            val result = if (repliesSnapshot.isEmpty) {
                batch.delete(mediaCommentsColl.document(commentId))
                if (comment.parentId != null) {
                    val parentRef = mediaCommentsColl.document(comment.parentId)
                    if (parentRef.get().await().exists()) {
                        batch.update(parentRef, "repliesCount", FieldValue.increment(-1))
                    }
                }
                DeleteCommentResult.HARD_DELETED
            } else {
                // Preserve the discussion tree, but sever any link to the deleted author.
                batch.update(
                    mediaCommentsColl.document(commentId),
                    mapOf(
                        "text" to "",
                        "userId" to "",
                        "userDisplayName" to "",
                        "userAvatarUrl" to "",
                        "parentUserId" to FieldValue.delete(),
                        "isSpoiler" to false,
                        "likesCount" to 0,
                        "likedBy" to emptyList<String>(),
                        "isDeleted" to true
                    )
                )
                for (doc in repliesSnapshot.documents) {
                    batch.update(doc.reference, "parentUserId", FieldValue.delete())
                }
                DeleteCommentResult.SOFT_DELETED
            }

            batch.commit().await()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            DeleteCommentResult.FAILED
        }
    }
}
