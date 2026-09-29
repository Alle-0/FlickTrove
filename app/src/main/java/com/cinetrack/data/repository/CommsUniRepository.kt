package com.cinetrack.data.repository

import com.cinetrack.data.api.CommsUniService
import com.cinetrack.data.api.CommsUniCommentsResponse
import com.cinetrack.data.api.CommsUniConversationStatsResponse
import com.cinetrack.data.model.AppComment
import com.cinetrack.data.mapper.CommsUniMapper
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommsUniRepository @Inject constructor(
    private val commsUniService: CommsUniService
) {

    /**
     * Recupera le statistiche della conversazione (es. numero di commenti)
     * entityType: "show", "movie", "season", "episode"
     * entityId: es. "tvdb-289590", "tvdb-289590-s1", "tvdb-289590-s1e1"
     */
    suspend fun getConversationStats(
        entityType: String,
        entityId: String,
        source: String? = null,
        language: String? = null
    ): Result<CommsUniConversationStatsResponse> {
        return try {
            val response = commsUniService.getConversationStats(entityType, entityId, source, language)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty body"))
            } else {
                if (response.code() == 404) {
                    Result.failure(Exception("not_archived"))
                } else {
                    Result.failure(Exception("HTTP error ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera la pagina di commenti per una data entità
     */
    suspend fun getComments(
        entityType: String,
        entityId: String,
        sort: String? = "most_liked",
        limit: Int? = 50,
        cursor: String? = null,
        include: String? = "media_urls,language_counts,source_counts",
        source: String? = null,
        language: String? = null
    ): Result<CommsUniCommentsResponse> {
        return try {
            val response = commsUniService.getComments(
                entityType = entityType,
                entityId = entityId,
                sort = sort,
                limit = limit,
                cursor = cursor,
                include = include,
                source = source,
                language = language
            )
            
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty body"))
            } else {
                if (response.code() == 404) {
                    Result.failure(Exception("not_archived"))
                } else {
                    Result.failure(Exception("HTTP error ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Utility per generare l'entityId corretto per CommsUni dato il tvdbId base.
     */
    fun buildEntityId(tvdbId: Int, seasonNumber: Int? = null, episodeNumber: Int? = null): String {
        return when {
            seasonNumber != null && episodeNumber != null -> "tvdb-$tvdbId-s${seasonNumber}e${episodeNumber}"
            seasonNumber != null -> "tvdb-$tvdbId-s$seasonNumber"
            else -> "tvdb-$tvdbId"
        }
    }

    suspend fun getReplies(
        commentId: String,
        sort: String? = "most_recent",
        limit: Int? = 50,
        cursor: String? = null,
        parent: String? = null,
        source: String? = null,
        language: String? = null
    ): Result<CommsUniCommentsResponse> {
        return try {
            val response = commsUniService.getReplies(
                commentId = commentId,
                sort = sort,
                limit = limit,
                cursor = cursor,
                parent = parent,
                source = source,
                language = language
            )
            
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty body"))
            } else {
                if (response.code() == 404) {
                    Result.failure(Exception("not_archived"))
                } else {
                    Result.failure(Exception("HTTP error ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private var cachedSources: List<com.cinetrack.data.api.SourceCatalogRow>? = null

    suspend fun getSources(): Result<List<com.cinetrack.data.api.SourceCatalogRow>> {
        if (cachedSources != null) {
            return Result.success(cachedSources!!)
        }
        return try {
            val response = commsUniService.getSources()
            if (response.isSuccessful) {
                val sources = response.body()?.data ?: emptyList()
                cachedSources = sources
                Result.success(sources)
            } else {
                Result.failure(Exception("HTTP error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val json = org.json.JSONObject(errorBody)
            if (json.has("error")) {
                val errorObj = json.getJSONObject("error")
                val msg = errorObj.optString("message")
                if (msg.isNotBlank()) msg else errorObj.optString("code").ifBlank { null }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createComment(entityType: String, entityId: String, text: String, isSpoiler: Boolean, language: String? = null): Result<com.cinetrack.data.api.CommsUniComment> {
        return try {
            val response = commsUniService.createComment(entityType, entityId, com.cinetrack.data.api.CommsUniWriteCommentRequest(text = text, language = language, isSpoiler = isSpoiler))
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it.data.comment) } ?: Result.failure(Exception("Empty body"))
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("CommsUni", "createComment error: HTTP ${response.code()} - $errorBody")
                val serverMsg = parseErrorMessage(errorBody) ?: "HTTP error ${response.code()}"
                Result.failure(Exception(serverMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("CommsUni", "createComment exception", e)
            Result.failure(e)
        }
    }

    suspend fun createReply(commentId: String, text: String, isSpoiler: Boolean, language: String? = null): Result<com.cinetrack.data.api.CommsUniComment> {
        return try {
            val response = commsUniService.createReply(commentId, com.cinetrack.data.api.CommsUniWriteCommentRequest(text = text, language = language, isSpoiler = isSpoiler))
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it.data.comment) } ?: Result.failure(Exception("Empty body"))
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("CommsUni", "createReply error: HTTP ${response.code()} - $errorBody")
                val serverMsg = parseErrorMessage(errorBody) ?: "HTTP error ${response.code()}"
                Result.failure(Exception(serverMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("CommsUni", "createReply exception", e)
            Result.failure(e)
        }
    }

    suspend fun likeComment(commentId: String): Result<Unit> {
        return try {
            val response = commsUniService.likeComment(commentId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("CommsUni", "likeComment error: HTTP ${response.code()} - $errorBody")
                Result.failure(Exception(parseErrorMessage(errorBody) ?: "HTTP error ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("CommsUni", "likeComment exception", e)
            Result.failure(e)
        }
    }

    suspend fun unlikeComment(commentId: String): Result<Unit> {
        return try {
            val response = commsUniService.unlikeComment(commentId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("CommsUni", "unlikeComment error: HTTP ${response.code()} - $errorBody")
                Result.failure(Exception(parseErrorMessage(errorBody) ?: "HTTP error ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("CommsUni", "unlikeComment exception", e)
            Result.failure(e)
        }
    }

    enum class ReportCommentResult {
        SUCCESS,
        DUPLICATE,
        ERROR
    }

    suspend fun reportComment(
        commentId: String,
        reason: com.cinetrack.data.api.CommsUniReportReason,
        detail: String? = null
    ): Result<ReportCommentResult> {
        return reportComment(commentId, reason.value, detail)
    }

    suspend fun reportComment(
        commentId: String,
        reason: String,
        detail: String? = null
    ): Result<ReportCommentResult> {
        val safeReason = com.cinetrack.data.api.CommsUniReportReason.fromValue(reason).value
        return try {
            val response = commsUniService.reportComment(
                commentId,
                com.cinetrack.data.api.CommsUniReportRequest(reason = safeReason, detail = detail)
            )
            if (response.isSuccessful) {
                Result.success(ReportCommentResult.SUCCESS)
            } else {
                val errorBody = response.errorBody()?.string()
                val isDuplicate = response.code() == 409 ||
                    response.headers()["Report-Duplicate"]?.equals("true", ignoreCase = true) == true ||
                    errorBody?.contains("duplicate", ignoreCase = true) == true ||
                    errorBody?.contains("already_reported", ignoreCase = true) == true

                if (isDuplicate) {
                    android.util.Log.i("CommsUni", "reportComment: duplicate report detected for comment $commentId")
                    Result.success(ReportCommentResult.DUPLICATE)
                } else {
                    android.util.Log.e("CommsUni", "reportComment error: HTTP ${response.code()} - $errorBody")
                    Result.failure(Exception(parseErrorMessage(errorBody) ?: "HTTP error ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CommsUni", "reportComment exception", e)
            Result.failure(e)
        }
    }

    suspend fun deleteComment(commentId: String): Result<Unit> {
        return try {
            val response = commsUniService.deleteComment(commentId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("CommsUni", "deleteComment error: HTTP ${response.code()} - $errorBody")
                Result.failure(Exception(parseErrorMessage(errorBody) ?: "HTTP error ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("CommsUni", "deleteComment exception", e)
            Result.failure(e)
        }
    }

    suspend fun updateCommentSpoiler(commentId: String, isSpoiler: Boolean): Result<com.cinetrack.data.api.CommsUniComment> {
        return try {
            val response = commsUniService.updateCommentSpoiler(
                commentId = commentId,
                request = com.cinetrack.data.api.CommsUniUpdateSpoilerRequest(isSpoiler = isSpoiler)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data.comment)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("CommsUni", "updateCommentSpoiler error: HTTP ${response.code()} - $errorBody")
                Result.failure(Exception(parseErrorMessage(errorBody) ?: "HTTP error ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("CommsUni", "updateCommentSpoiler exception", e)
            Result.failure(e)
        }
    }

    /**
     * Aggiorna l'identità/overlay del profilo utente (Nome e Avatar) su CommsUni
     */
    suspend fun updateProfile(displayName: String, avatarUrl: String? = null): Result<com.cinetrack.data.api.CommsUniProfileData> {
        val safeName = displayName.trim().take(64)
        if (safeName.isEmpty()) return Result.failure(IllegalArgumentException("displayName cannot be empty"))

        val safeAvatar = avatarUrl?.trim()?.takeIf { it.isNotBlank() && it.startsWith("http") }

        return try {
            val response = commsUniService.updateProfile(
                com.cinetrack.data.api.CommsUniUpdateProfileRequest(
                    displayName = safeName,
                    avatarUrl = safeAvatar
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("CommsUni", "updateProfile error: HTTP ${response.code()} - $errorBody")
                // Se fallisce per via dell'host avatar non consentito (400 invalid_avatar_url / invalid_parameter), riprova senza avatarUrl
                if (response.code() in 400..422 && safeAvatar != null) {
                    android.util.Log.w("CommsUni", "Retrying updateProfile without avatarUrl due to HTTP ${response.code()} ($errorBody)")
                    val retryResponse = commsUniService.updateProfile(
                        com.cinetrack.data.api.CommsUniUpdateProfileRequest(
                            displayName = safeName,
                            avatarUrl = null
                        )
                    )
                    if (retryResponse.isSuccessful && retryResponse.body() != null) {
                        return Result.success(retryResponse.body()!!.data)
                    }
                }
                Result.failure(Exception(parseErrorMessage(errorBody) ?: "HTTP error ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("CommsUni", "updateProfile exception", e)
            Result.failure(e)
        }
    }

    /**
     * Recupera i top commenti per l'anteprima nella schermata di dettaglio
     */
    suspend fun getTopCommentsPreview(
        tvdbId: Int,
        entityType: String,
        limit: Int = 5,
        currentUserId: String? = null
    ): List<AppComment> {
        val entityId = buildEntityId(tvdbId)
        val normalizedType = if (entityType.lowercase() in listOf("tv", "series", "show")) "show" else "movie"
        val result = getComments(
            entityType = normalizedType,
            entityId = entityId,
            sort = "most_liked",
            limit = limit
        )
        return if (result.isSuccess) {
            val response = result.getOrNull()
            val comments = response?.data?.allItems ?: emptyList()
            val sources = getSources().getOrNull()?.associateBy { it.slug } ?: emptyMap()
            comments.map { 
                CommsUniMapper.toAppComment(
                    comment = it,
                    sourcesMap = sources,
                    currentEntityId = entityId,
                    currentEntityType = normalizedType,
                    currentUserId = currentUserId
                )
            }
        } else {
            emptyList()
        }
    }
}
