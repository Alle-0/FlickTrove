package com.cinetrack.ui.viewmodel

import com.cinetrack.R
import com.cinetrack.data.model.AppComment
import com.cinetrack.data.model.deduplicateComments
import com.cinetrack.data.model.isSameAuthor
import com.cinetrack.data.model.isSameCommentText
import com.cinetrack.data.model.mergeComments
import com.cinetrack.data.repository.BadgeRepository
import com.cinetrack.data.repository.BlockedAuthorsManager
import com.cinetrack.data.repository.CommentRepository
import com.cinetrack.data.repository.CommsUniRepository
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.data.repository.TvdbRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.UiText
import com.cinetrack.util.TranslationManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Delegate responsible for managing comments preview, live interaction (add, like, report)
 * and smart Google Translate / GTX translations in the MovieDetail screen.
 */
class MovieDetailCommentsDelegate @Inject constructor(
    private val commentRepository: CommentRepository,
    private val commsUniRepository: CommsUniRepository,
    private val blockedAuthorsManager: BlockedAuthorsManager,
    private val tvdbRepository: TvdbRepository,
    private val preferenceRepository: PreferenceRepository,
    private val translationManager: TranslationManager,
    private val badgeRepository: BadgeRepository,
    private val actionFeedbackManager: ActionFeedbackManager
) {
    data class TranslationState(
        val isTranslating: Boolean = false,
        val translatedText: String? = null,
        val error: String? = null
    )

    private val _appComments = MutableStateFlow<List<AppComment>>(emptyList())
    val appComments: StateFlow<List<AppComment>> = _appComments.asStateFlow()

    private val _translationStates = MutableStateFlow<Map<Long, TranslationState>>(emptyMap())
    val translationStates: StateFlow<Map<Long, TranslationState>> = _translationStates.asStateFlow()

    private val _showTranslationPrompt = MutableStateFlow<Pair<Long, String>?>(null)
    val showTranslationPrompt: StateFlow<Pair<Long, String>?> = _showTranslationPrompt.asStateFlow()

    fun reset() {
        _appComments.value = emptyList()
        _translationStates.value = emptyMap()
        _showTranslationPrompt.value = null
    }

    fun setComments(comments: List<AppComment>) {
        _appComments.value = comments
    }

    suspend fun fetchPreviewComments(
        movieId: Long,
        mediaType: String,
        tvdbId: Int?,
        title: String?,
        year: String?,
        onTvdbIdResolved: ((Int) -> Unit)? = null
    ): List<AppComment> {
        return try {
            coroutineScope {
                val firestoreDeferred = async {
                    if (movieId != 0L) {
                        try {
                            commentRepository.getTopCommentsForMediaPreview(movieId.toString(), 5)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } else emptyList()
                }

                val commsUniDeferred = async {
                    try {
                        val resolvedTvdbId = if (tvdbId != null && tvdbId > 0) {
                            tvdbId
                        } else {
                            val searchType = if (mediaType == "tv") "series" else "movie"
                            tvdbRepository.resolveTvdbIdByRemoteId(movieId.toString(), searchType)
                                ?: if (!title.isNullOrBlank()) tvdbRepository.resolveTvdbId(title, year, searchType) else null
                        }

                        if (resolvedTvdbId != null && resolvedTvdbId > 0) {
                            onTvdbIdResolved?.invoke(resolvedTvdbId)
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            commsUniRepository.getTopCommentsPreview(
                                tvdbId = resolvedTvdbId,
                                entityType = if (mediaType == "tv") "show" else "movie",
                                limit = 5,
                                currentUserId = uid
                            )
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                val firestoreComments = firestoreDeferred.await()
                val commsUniComments = commsUniDeferred.await()

                val matchedFirestoreIds = mutableSetOf<String>()
                val mergedCommsUni = commsUniComments.map { cc ->
                    val matchingFc = firestoreComments.firstOrNull { fc ->
                        fc.id == cc.id || (
                            isSameAuthor(fc, cc) &&
                            isSameCommentText(fc.text, cc.text) &&
                            fc.parentId == cc.parentId
                        )
                    }
                    if (matchingFc != null) {
                        matchedFirestoreIds.add(matchingFc.id)
                        mergeComments(cc, matchingFc)
                    } else {
                        cc
                    }
                }

                val remainingFirestore = firestoreComments
                    .filterNot { it.id in matchedFirestoreIds }
                    .map { fc ->
                        if (fc.originSlug.isBlank()) {
                            fc.copy(originSlug = "flicktrove", originName = "FlickTrove", originColor = "#2dd4bf")
                        } else fc
                    }

                val combined = (mergedCommsUni + remainingFirestore)
                    .filterNot { blockedAuthorsManager.isAuthorBlocked(it.userId, it.userDisplayName) }
                    .deduplicateComments()
                    .filterNot { it.isEffectivelyDeleted }
                combined.sortedWith(
                    compareByDescending<AppComment> {
                        it.originSlug.equals("flicktrove", ignoreCase = true) || it.originSlug.isBlank()
                    }.thenByDescending {
                        it.likesCount
                    }
                ).take(5)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun refreshComments(
        movieId: Long,
        mediaType: String,
        tvdbId: Int?,
        title: String?,
        year: String?,
        scope: CoroutineScope,
        onTvdbIdResolved: ((Int) -> Unit)? = null
    ) {
        scope.launch {
            val comments = fetchPreviewComments(movieId, mediaType, tvdbId, title, year, onTvdbIdResolved)
            if (comments.isNotEmpty() || _appComments.value.isEmpty()) {
                _appComments.value = comments
            }
        }
    }

    fun addComment(
        movieId: Long,
        mediaType: String,
        mediaTitle: String,
        mediaImage: String?,
        text: String,
        isSpoiler: Boolean = false,
        parentId: String? = null,
        parentUserId: String? = null,
        depth: Int = 0,
        scope: CoroutineScope
    ) {
        scope.launch {
            val success = commentRepository.addComment(
                mediaId = movieId.toString(),
                mediaType = mediaType,
                text = text,
                isSpoiler = isSpoiler,
                parentId = parentId,
                parentUserId = parentUserId,
                depth = depth,
                mediaTitle = mediaTitle,
                mediaImage = mediaImage
            )
            if (success) {
                badgeRepository.recordCommentWritten()
                _appComments.value = commentRepository.getTopCommentsForMediaPreview(movieId.toString())
            }
        }
    }

    fun toggleCommentLike(
        movieId: Long,
        mediaType: String,
        mediaTitle: String,
        mediaImage: String?,
        commentId: String,
        scope: CoroutineScope,
        onNetworkFallbackRefresh: (() -> Unit)? = null
    ) {
        val uId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val currentComments = _appComments.value.toMutableList()
        val index = currentComments.indexOfFirst { it.id == commentId }
        if (index != -1) {
            val comment = currentComments[index]
            val isLiked = comment.likedBy.contains(uId)

            val newLikedBy = if (isLiked) comment.likedBy - uId else comment.likedBy + uId
            val newLikesCount = if (isLiked) maxOf(0, comment.likesCount - 1) else comment.likesCount + 1

            currentComments[index] = comment.copy(likedBy = newLikedBy, likesCount = newLikesCount)
            _appComments.value = currentComments

            scope.launch {
                val isFlickTrove = comment.originSlug.equals("flicktrove", ignoreCase = true) || comment.originSlug.isBlank()
                if (isFlickTrove) {
                    commentRepository.toggleLike(movieId.toString(), commentId, mediaType, mediaTitle, mediaImage)
                }
                val result = if (isLiked) {
                    commsUniRepository.unlikeComment(commentId)
                } else {
                    commsUniRepository.likeComment(commentId)
                }
                if (result.isFailure && !isFlickTrove) {
                    onNetworkFallbackRefresh?.invoke()
                }
            }
        }
    }

    fun reportComment(
        movieId: Long,
        commentId: String,
        reason: String,
        commentText: String,
        commentAuthorId: String,
        commentAuthorName: String,
        scope: CoroutineScope
    ) {
        scope.launch {
            val result = commentRepository.reportComment(movieId.toString(), commentId, reason, commentText, commentAuthorId, commentAuthorName)
            when (result) {
                CommentRepository.ReportResult.SUCCESS -> actionFeedbackManager.emit(UiText.StringResource(R.string.report_success))
                CommentRepository.ReportResult.COOLDOWN -> actionFeedbackManager.emit(UiText.StringResource(R.string.report_cooldown))
                CommentRepository.ReportResult.ERROR -> actionFeedbackManager.emit(UiText.StringResource(R.string.report_error))
            }
        }
    }

    fun requestTranslation(commentId: Long, text: String, scope: CoroutineScope) {
        scope.launch {
            val prefs = preferenceRepository.userPreferencesFlow.first()
            val systemLang = java.util.Locale.getDefault().language
            translationManager.setTargetLanguage(prefs.contentLanguage, systemLang)
            translateComment(commentId, text, requireWifi = false, scope = scope)
        }
    }

    fun dismissTranslationPrompt() {
        _showTranslationPrompt.value = null
    }

    fun translateComment(commentId: Long, text: String, requireWifi: Boolean, scope: CoroutineScope) {
        scope.launch {
            _showTranslationPrompt.value = null
            _translationStates.update { it + (commentId to TranslationState(isTranslating = true)) }
            val prefs = preferenceRepository.userPreferencesFlow.first()
            val systemLang = java.util.Locale.getDefault().language
            translationManager.setTargetLanguage(prefs.contentLanguage, systemLang)
            when (val result = translationManager.translateCommentSmart(text)) {
                is TranslationManager.TranslationResult.Success -> {
                    _translationStates.update { it + (commentId to TranslationState(isTranslating = false, translatedText = result.text)) }
                }
                is TranslationManager.TranslationResult.AlreadyInLanguage -> {
                    _translationStates.update { it - commentId }
                    actionFeedbackManager.emit(UiText.StringResource(R.string.comment_already_in_language))
                }
                is TranslationManager.TranslationResult.Error -> {
                    _translationStates.update { it + (commentId to TranslationState(isTranslating = false, error = "Errore durante la traduzione")) }
                    actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_translating))
                }
            }
        }
    }
}

