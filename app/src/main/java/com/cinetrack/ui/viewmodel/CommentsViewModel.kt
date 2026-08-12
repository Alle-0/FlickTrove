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

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val commentRepository: CommentRepository,
    private val storageRepository: com.cinetrack.data.repository.StorageRepository,
    private val auth: FirebaseAuth,
    private val actionFeedbackManager: ActionFeedbackManager
) : ViewModel() {

    private val _comments = MutableStateFlow<List<AppComment>>(emptyList())
    val comments: StateFlow<List<AppComment>> = _comments.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    val currentUserId: String? get() = auth.currentUser?.uid

    private var currentMediaId: String = ""
    private var currentMediaType: String = ""

    fun init(mediaId: String, mediaType: String) {
        if (currentMediaId == mediaId) return
        currentMediaId = mediaId
        currentMediaType = mediaType
        refreshComments()
    }

    private fun refreshComments() {
        viewModelScope.launch {
            _isLoading.value = true
            _comments.value = commentRepository.getCommentsForMedia(currentMediaId)
            _isLoading.value = false
        }
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
                _comments.value = commentRepository.getCommentsForMedia(currentMediaId)
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
                // In caso di fallimento, ripristina lo stato reale dal server
                _comments.value = commentRepository.getCommentsForMedia(currentMediaId)
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
        // Aggiornamento ottimistico: rimuoviamo subito il commento dalla lista locale
        val currentComments = _comments.value.toMutableList()
        val index = currentComments.indexOfFirst { it.id == commentId }
        if (index != -1) {
            currentComments.removeAt(index)
            _comments.value = currentComments
        }

        // Chiamata di rete per l'eliminazione effettiva
        viewModelScope.launch {
            val success = commentRepository.deleteComment(currentMediaId, commentId)
            if (!success) {
                // Se fallisce, ripristiniamo la lista dal server
                _comments.value = commentRepository.getCommentsForMedia(currentMediaId)
            }
        }
    }
}
