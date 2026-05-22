package com.cinetrack.data.repository

import com.cinetrack.data.models.Feedback

interface FeedbackRepository {
    suspend fun sendFeedback(feedback: Feedback): Result<Unit>
}
