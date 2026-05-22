package com.cinetrack.data.repository

import com.cinetrack.data.models.Feedback
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FeedbackRepository {

    override suspend fun sendFeedback(feedback: Feedback): Result<Unit> {
        return try {
            val documentRef = firestore.collection("feedbacks").document()
            val feedbackWithId = feedback.copy(id = documentRef.id)
            documentRef.set(feedbackWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FeedbackRepo", "Error sending feedback: ${e.message}", e)
            Result.failure(e)
        }
    }
}
