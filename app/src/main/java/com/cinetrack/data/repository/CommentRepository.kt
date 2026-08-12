package com.cinetrack.data.repository

import com.cinetrack.data.model.AppComment
import com.cinetrack.data.model.CommentReport
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storageRepository: StorageRepository
) {
    private fun getMediaCommentsCollection(mediaId: String) =
        firestore.collection("media_comments").document(mediaId).collection("comments")
        
    private val reportsCollection = firestore.collection("comment_reports")

    suspend fun getCommentsForMedia(mediaId: String, limit: Long = 50): List<AppComment> {
        return try {
            val snapshot = getMediaCommentsCollection(mediaId)
                .get()
                .await()
            
            snapshot.toObjects(AppComment::class.java).mapIndexed { index, appComment -> 
                appComment.copy(id = snapshot.documents[index].id)
            }
            .sortedByDescending { it.createdAt }
            .take(limit.toInt())
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addComment(
        mediaId: String,
        mediaType: String,
        text: String,
        isSpoiler: Boolean,
        parentId: String? = null,
        parentUserId: String? = null,
        depth: Int = 0
    ): Boolean {
        val user = auth.currentUser ?: return false
        val newComment = AppComment(
            mediaId = mediaId,
            mediaType = mediaType,
            userId = user.uid,
            userDisplayName = user.displayName ?: "Anonimo",
            userAvatarUrl = user.photoUrl?.toString() ?: "",
            text = text,
            createdAt = Timestamp.now(),
            isSpoiler = isSpoiler,
            parentId = parentId,
            parentUserId = parentUserId,
            depth = depth
        )

        return try {
            firestore.runTransaction { transaction ->
                val mediaCommentsColl = getMediaCommentsCollection(mediaId)
                val newDocRef = mediaCommentsColl.document()
                transaction.set(newDocRef, newComment)
                
                // If it's a reply, increment the parent's repliesCount
                if (parentId != null) {
                    val parentRef = mediaCommentsColl.document(parentId)
                    transaction.update(parentRef, "repliesCount", FieldValue.increment(1))
                }
            }.await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun toggleLike(mediaId: String, commentId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val docRef = getMediaCommentsCollection(mediaId).document(commentId)
        
        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) return@runTransaction
                
                val currentLikes = snapshot.get("likedBy") as? List<*> ?: emptyList<String>()
                if (currentLikes.contains(userId)) {
                    transaction.update(docRef, "likesCount", FieldValue.increment(-1))
                    transaction.update(docRef, "likedBy", FieldValue.arrayRemove(userId))
                } else {
                    transaction.update(docRef, "likesCount", FieldValue.increment(1))
                    transaction.update(docRef, "likedBy", FieldValue.arrayUnion(userId))
                }
            }.await()
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
    suspend fun deleteComment(mediaId: String, commentId: String): Boolean {
        return try {
            val mediaCommentsColl = getMediaCommentsCollection(mediaId)
            
            // 1. Fetch the comment to delete
            val commentSnapshot = mediaCommentsColl.document(commentId).get().await()
            if (!commentSnapshot.exists()) return false
            val comment = commentSnapshot.toObject(AppComment::class.java) ?: return false

            // 2. Fetch all replies (1 level deep)
            val repliesSnapshot = mediaCommentsColl.whereEqualTo("parentId", commentId).get().await()
            
            // 3. Delete images from Storage (for parent and replies)
            val allComments = mutableListOf(comment)
            allComments.addAll(repliesSnapshot.documents.mapNotNull { it.toObject(AppComment::class.java) })
            
            val imageRegex = Regex("""!\[.*?\]\((.*?)\)""")
            for (c in allComments) {
                val match = imageRegex.find(c.text)
                if (match != null) {
                    val imageUrl = match.groupValues[1]
                    if (imageUrl.contains("supabase.co")) {
                        val result = storageRepository.deleteCommentImage(imageUrl)
                        if (result.isFailure) {
                            android.util.Log.e("CommentRepository", "Failed to delete image: $imageUrl", result.exceptionOrNull())
                            return false // Abort if image deletion fails to avoid orphans
                        }
                    }
                }
            }

            // 4. Batch delete from Firestore
            val batch = firestore.batch()
            batch.delete(mediaCommentsColl.document(commentId))
            for (doc in repliesSnapshot.documents) {
                batch.delete(doc.reference)
            }
            
            // 5. Update parent's repliesCount if needed
            if (comment.parentId != null) {
                val parentRef = mediaCommentsColl.document(comment.parentId)
                val parentSnapshot = parentRef.get().await()
                if (parentSnapshot.exists()) {
                    batch.update(parentRef, "repliesCount", FieldValue.increment(-1))
                }
            }
            
            batch.commit().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
