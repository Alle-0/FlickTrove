package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.AppComment
import com.cinetrack.data.repository.CommentRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.UiText
import com.cinetrack.R

import com.cinetrack.data.repository.PreferenceRepository
import kotlinx.coroutines.flow.first

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val commentRepository: CommentRepository,
    private val storageRepository: com.cinetrack.data.repository.StorageRepository,
    private val preferenceRepository: PreferenceRepository,
    private val auth: FirebaseAuth,
    private val actionFeedbackManager: ActionFeedbackManager,
    private val translationManager: com.cinetrack.util.TranslationManager
) : ViewModel() {

    val isUserAnonymous: Boolean
        get() = FirebaseAuth.getInstance().currentUser?.isAnonymous == true
        
    private val _comments = MutableStateFlow<List<AppComment>>(emptyList())
    val comments: StateFlow<List<AppComment>> = _comments.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMoreComments = MutableStateFlow(false)
    val hasMoreComments: StateFlow<Boolean> = _hasMoreComments.asStateFlow()

    private var lastVisibleComment: com.google.firebase.firestore.DocumentSnapshot? = null

    val currentUserId: String? get() = auth.currentUser?.uid

    private var currentMediaId: String = ""
    private var currentMediaType: String = ""

    // ── Translation state ─────────────────────────────────────────────────────
    /** Possible states for a single comment's translation */
    sealed class TranslationState {
        object Idle : TranslationState()
        object Downloading : TranslationState()   // model download in progress (Gotcha 2)
        object Translating : TranslationState()
        data class Translated(val text: String) : TranslationState()
        object Error : TranslationState()
        object TooShort : TranslationState()      // text too short to detect language (Gotcha 1)
    }

    private val _translationStates = MutableStateFlow<Map<String, TranslationState>>(emptyMap())
    val translationStates: StateFlow<Map<String, TranslationState>> = _translationStates.asStateFlow()

    private val _showTranslationPrompt = MutableStateFlow<Pair<String, String>?>(null)
    val showTranslationPrompt: StateFlow<Pair<String, String>?> = _showTranslationPrompt.asStateFlow()

    fun dismissTranslationPrompt() {
        _showTranslationPrompt.value = null
    }

    fun init(mediaId: String, mediaType: String) {
        if (currentMediaId == mediaId) return
        currentMediaId = mediaId
        currentMediaType = mediaType
        refreshComments()
    }

    private fun refreshComments() {
        viewModelScope.launch {
            _isLoading.value = true
            lastVisibleComment = null
            val result = commentRepository.getCommentsForMedia(currentMediaId)
            _comments.value = result.first
            lastVisibleComment = result.second
            _hasMoreComments.value = result.second != null
            _isLoading.value = false
        }
    }

    fun loadMoreComments() {
        if (_isLoadingMore.value || !_hasMoreComments.value) return
        
        viewModelScope.launch {
            _isLoadingMore.value = true
            val result = commentRepository.getCommentsForMedia(
                mediaId = currentMediaId,
                lastVisible = lastVisibleComment
            )
            
            val newComments = result.first
            if (newComments.isNotEmpty()) {
                val currentList = _comments.value.toMutableList()
                currentList.addAll(newComments)
                _comments.value = currentList
            }
            
            lastVisibleComment = result.second
            _hasMoreComments.value = result.second != null
            _isLoadingMore.value = false
        }
    }

    /**
     * Requests translation of a comment.
     * If already translated, toggles back to the original text.
     */
    fun translateComment(commentId: String, text: String, requireWifi: Boolean? = null) {
        val current = _translationStates.value[commentId]
        // Toggle: if already translated, reset to Idle
        if (current is TranslationState.Translated) {
            _translationStates.value = _translationStates.value + (commentId to TranslationState.Idle)
            return
        }
        viewModelScope.launch {
            val mediaRegex = Regex("!\\[(?:gif|foto)\\]\\((.*?)\\)")
            val cleanText = text.replace(mediaRegex, "").trim()
            if (cleanText.isBlank()) {
                return@launch
            }

            // Sync user's target language from preferences
            val prefs = preferenceRepository.userPreferencesFlow.first()
            val systemLang = java.util.Locale.getDefault().language
            translationManager.setTargetLanguage(prefs.contentLanguage, systemLang)

            val targetMlKit = translationManager.getCurrentTargetLanguage()
            val targetBcp47 = translationManager.mapMlKitToBcp47(targetMlKit)

            // Step 1: Detect source language
            val detectedLang = translationManager.identifyLanguage(cleanText)
            if (detectedLang != null && (detectedLang == targetBcp47 || detectedLang == targetMlKit)) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.comment_already_in_language))
                return@launch
            }

            val effectiveSourceLang = detectedLang ?: if (targetMlKit != com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH) {
                com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH
            } else {
                com.google.mlkit.nl.translate.TranslateLanguage.ITALIAN
            }

            // Step 2: Check / Download models
            val modelReady = translationManager.isModelDownloaded(effectiveSourceLang, targetMlKit)
            if (!modelReady) {
                if (requireWifi == null) {
                    _showTranslationPrompt.value = Pair(commentId, cleanText)
                    return@launch
                }
                _showTranslationPrompt.value = null
                _translationStates.value = _translationStates.value + (commentId to TranslationState.Downloading)
                val downloaded = translationManager.downloadModels(effectiveSourceLang, targetMlKit, requireWifi = requireWifi)
                if (!downloaded) {
                    _translationStates.value = _translationStates.value + (commentId to TranslationState.Error)
                    actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_lang_model))
                    return@launch
                }
            } else {
                _showTranslationPrompt.value = null
            }

            // Step 3: Translate
            _translationStates.value = _translationStates.value + (commentId to TranslationState.Translating)
            val translated = translationManager.translateFrom(cleanText, effectiveSourceLang, targetMlKit)
            if (translated != null && translated.trim().lowercase() != cleanText.trim().lowercase()) {
                _translationStates.value = _translationStates.value + (commentId to TranslationState.Translated(translated))
            } else {
                actionFeedbackManager.emit(UiText.StringResource(R.string.comment_already_in_language))
                _translationStates.value = _translationStates.value + (commentId to TranslationState.Idle)
            }
        }
    }

    // ── Gotcha 3: release all Translator native resources when ViewModel is destroyed ──
    override fun onCleared() {
        super.onCleared()
        translationManager.closeAll()
    }

    fun uploadCommentImage(imageUri: android.net.Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = storageRepository.uploadCommentImage(imageUri)
            result.onSuccess { url ->
                onSuccess(url)
            }.onFailure { e ->
                onError(e.message ?: "Upload fallito")
            }
        }
    }

    fun addComment(text: String, isSpoiler: Boolean = false, parentId: String? = null, parentUserId: String? = null, depth: Int = 0) {
        viewModelScope.launch {
            val success = commentRepository.addComment(
                mediaId = currentMediaId,
                mediaType = currentMediaType,
                text = text,
                isSpoiler = isSpoiler,
                parentId = parentId,
                parentUserId = parentUserId,
                depth = depth
            )
            if (success) {
                refreshComments()
            }
        }
    }

    fun toggleLikeComment(commentId: String) {
        val uId = currentUserId ?: return
        
        // Aggiornamento ottimistico della UI
        val currentComments = _comments.value.toMutableList()
        val index = currentComments.indexOfFirst { it.id == commentId }
        if (index != -1) {
            val comment = currentComments[index]
            val isLiked = comment.likedBy.contains(uId)
            
            val newLikedBy = if (isLiked) comment.likedBy - uId else comment.likedBy + uId
            val newLikesCount = if (isLiked) maxOf(0, comment.likesCount - 1) else comment.likesCount + 1
            
            currentComments[index] = comment.copy(likedBy = newLikedBy, likesCount = newLikesCount)
            _comments.value = currentComments
        }

        // Chiamata di rete in background
        viewModelScope.launch {
            val success = commentRepository.toggleLike(currentMediaId, commentId)
            if (!success) {
                // In caso di fallimento, ripristina lo stato reale dal server (aggiornando solo il commento specifico o rifacendo la query)
                refreshComments()
            }
        }
    }

    fun toggleSpoilerStatus(commentId: String, currentSpoilerStatus: Boolean) {
        val newSpoilerStatus = !currentSpoilerStatus
        
        // Optimistic UI update
        val currentComments = _comments.value.toMutableList()
        val index = currentComments.indexOfFirst { it.id == commentId }
        if (index != -1) {
            currentComments[index] = currentComments[index].copy(isSpoiler = newSpoilerStatus)
            _comments.value = currentComments
        }

        // Background network call
        viewModelScope.launch {
            val success = commentRepository.updateSpoilerStatus(currentMediaId, commentId, newSpoilerStatus)
            if (!success) {
                // Revert on failure
                refreshComments()
            }
        }
    }

    fun reportComment(commentId: String, reason: String, commentText: String, commentAuthorId: String, commentAuthorName: String) {
        viewModelScope.launch {
            val result = commentRepository.reportComment(currentMediaId, commentId, reason, commentText, commentAuthorId, commentAuthorName)
            when (result) {
                com.cinetrack.data.repository.CommentRepository.ReportResult.SUCCESS -> actionFeedbackManager.emit(UiText.StringResource(R.string.report_success))
                com.cinetrack.data.repository.CommentRepository.ReportResult.COOLDOWN -> actionFeedbackManager.emit(UiText.StringResource(R.string.report_cooldown))
                com.cinetrack.data.repository.CommentRepository.ReportResult.ERROR -> actionFeedbackManager.emit(UiText.StringResource(R.string.report_error))
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            when (commentRepository.deleteComment(currentMediaId, commentId)) {
                com.cinetrack.data.repository.CommentRepository.DeleteCommentResult.HARD_DELETED -> {
                    _comments.value = _comments.value.filterNot { it.id == commentId }
                }
                com.cinetrack.data.repository.CommentRepository.DeleteCommentResult.SOFT_DELETED -> {
                    _comments.value = _comments.value.map { comment ->
                        if (comment.id == commentId) comment.copy(
                            userId = "",
                            userDisplayName = "",
                            userAvatarUrl = "",
                            text = "",
                            parentUserId = null,
                            likesCount = 0,
                            likedBy = emptyList(),
                            isSpoiler = false,
                            isDeleted = true
                        ) else comment
                    }
                }
                com.cinetrack.data.repository.CommentRepository.DeleteCommentResult.FAILED -> refreshComments()
            }
        }
    }
}
