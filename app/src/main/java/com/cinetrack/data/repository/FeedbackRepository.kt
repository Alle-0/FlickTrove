package com.cinetrack.data.repository

import com.cinetrack.data.model.Feedback

interface FeedbackRepository {
    suspend fun sendFeedback(feedback: Feedback): Result<Unit>
}
