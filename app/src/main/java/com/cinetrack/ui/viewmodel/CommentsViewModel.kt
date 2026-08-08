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

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val commentRepository: CommentRepository,
    private val auth: FirebaseAuth
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
        viewModelScope.launch {
            if (commentRepository.toggleLike(currentMediaId, commentId)) {
                _comments.value = commentRepository.getCommentsForMedia(currentMediaId)
            }
        }
    }

    fun reportComment(commentId: String, reason: String, commentText: String) {
        viewModelScope.launch {
            commentRepository.reportComment(currentMediaId, commentId, reason, commentText)
        }
    }
}
