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
    private val auth: FirebaseAuth
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

    suspend fun reportComment(mediaId: String, commentId: String, reason: String, commentText: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val report = CommentReport(
            mediaId = mediaId,
            commentId = commentId,
            reportedByUid = userId,
            reason = reason,
            commentText = commentText,
            timestamp = Timestamp.now()
        )
        return try {
            reportsCollection.add(report).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    suspend fun deleteComment(mediaId: String, commentId: String): Boolean {
        return try {
            getMediaCommentsCollection(mediaId).document(commentId).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
