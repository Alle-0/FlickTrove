package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.api.CommsUniComment
import com.cinetrack.data.api.CommsUniConversationStats
import com.cinetrack.data.repository.CommsUniRepository
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.UiText
import com.cinetrack.R
import com.cinetrack.util.TranslationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CommsUniViewModel @Inject constructor(
    private val commsUniRepository: CommsUniRepository,
    private val preferenceRepository: PreferenceRepository,
    private val auth: FirebaseAuth,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val actionFeedbackManager: ActionFeedbackManager,
    private val translationManager: TranslationManager,
    private val blockedAuthorsManager: com.cinetrack.data.repository.BlockedAuthorsManager,
    private val tvdbRepository: com.cinetrack.data.repository.TvdbRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    init {
        viewModelScope.launch {
            blockedAuthorsManager.blockedAuthorIds.collect { blockedIds ->
                if (blockedIds.isNotEmpty() && _comments.value.isNotEmpty()) {
                    val myAuthorId = currentAuthorId
                    val myActorId = currentActorId
                    val rawUid = auth.currentUser?.uid
                    val myCommentIds = getMyCommentIds()
                    _comments.value = _comments.value.filterNot { comment ->
                        val isMine = (comment.id in myCommentIds) ||
                            (!comment.userId.isBlank() && (
                                (myAuthorId != null && comment.userId == myAuthorId) ||
                                (myActorId != null && comment.userId == myActorId) ||
                                (rawUid != null && comment.userId == rawUid) ||
                                (currentUserId != null && comment.userId == currentUserId)
                            ))
                        !isMine && comment.userId in blockedIds
                    }
                }
            }
        }
    }

    private val prefs by lazy { context.getSharedPreferences("commsuni_prefs", android.content.Context.MODE_PRIVATE) }
    private val _cachedAuthorId = MutableStateFlow<String?>(null)

    val currentAuthorId: String?
        get() = _cachedAuthorId.value ?: auth.currentUser?.uid?.let { prefs.getString("author_id_$it", null) }

    val isUserAnonymous: Boolean
        get() = auth.currentUser?.isAnonymous != false
        
    val currentActorId: String?
        get() = auth.currentUser?.uid?.takeIf { !auth.currentUser!!.isAnonymous }?.let {
            com.cinetrack.data.api.CommsUniInterceptor.generateActorId(it)
        }

    val currentUserId: String? get() = currentAuthorId ?: currentActorId ?: auth.currentUser?.uid

    private fun getMyCommentIds(): Set<String> {
        val uid = auth.currentUser?.uid ?: return emptySet()
        return prefs.getStringSet("my_comment_ids_$uid", emptySet()) ?: emptySet()
    }

    private fun trackMyCommentId(id: String) {
        val uid = auth.currentUser?.uid ?: return
        val currentSet = prefs.getStringSet("my_comment_ids_$uid", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(id)
        prefs.edit().putStringSet("my_comment_ids_$uid", currentSet).apply()
    }

    private fun recordAuthorId(authorId: String) {
        if (authorId.isNotBlank()) {
            val wasDifferent = _cachedAuthorId.value != authorId
            _cachedAuthorId.value = authorId
            val uid = auth.currentUser?.uid
            if (uid != null) {
                prefs.edit().putString("author_id_$uid", authorId).apply()
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        firestore.collection("commsuni_authors")
                            .document(authorId)
                            .set(
                                mapOf(
                                    "uid" to uid,
                                    "updatedAt" to Timestamp.now()
                                ),
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                    } catch (e: Exception) {
                        android.util.Log.e("CommsUniViewModel", "Failed to map authorId to Firestore", e)
                    }
                }
            }
            if (_comments.value.isNotEmpty()) {
                val targetId = currentUserId ?: authorId
                val myIds = getMyCommentIds()
                val avatar = cachedLocalAvatar ?: auth.currentUser?.uid?.let { prefs.getString("user_avatar_$it", null) }
                val name = cachedLocalName ?: auth.currentUser?.uid?.let { prefs.getString("user_name_$it", null) }
                _comments.value = _comments.value.map { comment ->
                    if (comment.userId == authorId || comment.id in myIds) {
                        comment.copy(
                            userId = targetId,
                            userAvatarUrl = if (!avatar.isNullOrBlank()) avatar else comment.userAvatarUrl,
                            userDisplayName = if (comment.userDisplayName.isBlank() && !name.isNullOrBlank()) name else comment.userDisplayName
                        )
                    } else {
                        comment
                    }
                }
            }
        }
    }

    private var currentRawMediaId: String = ""

    private suspend fun resolveFirebaseUidForAuthor(authorIdOrUid: String): String? {
        if (authorIdOrUid.isBlank()) return null
        val currentUid = auth.currentUser?.uid
        if (!authorIdOrUid.contains("-") && authorIdOrUid.length >= 20) {
            return if (authorIdOrUid != currentUid) authorIdOrUid else null
        }
        return try {
            val doc = firestore.collection("commsuni_authors")
                .document(authorIdOrUid)
                .get()
                .await()
            val mappedUid = doc.getString("uid")
            if (mappedUid != null && mappedUid != currentUid) mappedUid else null
        } catch (e: Exception) {
            android.util.Log.e("CommsUniViewModel", "Failed to resolve authorId to uid", e)
            null
        }
    }

    // Comments State (Mapped to AppComment for UI compatibility)
    private val _comments = MutableStateFlow<List<com.cinetrack.data.model.AppComment>>(emptyList())
    val comments: StateFlow<List<com.cinetrack.data.model.AppComment>> = _comments.asStateFlow()

    // Stats State (Header)
    private val _conversationStats = MutableStateFlow<CommsUniConversationStats?>(null)
    val conversationStats: StateFlow<CommsUniConversationStats?> = _conversationStats.asStateFlow()
    
    // Pagination State
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMoreComments = MutableStateFlow(false)
    val hasMoreComments: StateFlow<Boolean> = _hasMoreComments.asStateFlow()
    
    private var nextCursor: String? = null

    // Sort & Filters
    var currentSort = "most_liked"
        private set
    var currentSourceFilter: String? = null
        private set
    var currentLanguageFilter: String? = null
        private set

    private var currentEntityId: String = ""
    private var currentEntityType: String = ""

    // Translation State (Identical logic to old CommentsViewModel)
    // Uses CommentsViewModel.TranslationState for compatibility with UI components


    private val _translationStates = MutableStateFlow<Map<String, CommentsViewModel.TranslationState>>(emptyMap())
    val translationStates: StateFlow<Map<String, CommentsViewModel.TranslationState>> = _translationStates.asStateFlow()

    private val _showTranslationPrompt = MutableStateFlow<Pair<String, String>?>(null)
    val showTranslationPrompt: StateFlow<Pair<String, String>?> = _showTranslationPrompt.asStateFlow()

    fun dismissTranslationPrompt() {
        _showTranslationPrompt.value = null
    }

    fun init(
        tvdbId: Int? = null,
        entityType: String = "movie",
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        title: String? = null,
        year: String? = null,
        rawMediaId: String = ""
    ) {
        if (rawMediaId.isNotBlank()) {
            currentRawMediaId = rawMediaId
        }
        val normalizedType = if (entityType.lowercase() in listOf("tv", "series", "show")) "show" else "movie"
        if (tvdbId != null && tvdbId > 0) {
            val entityId = commsUniRepository.buildEntityId(tvdbId, seasonNumber, episodeNumber)
            if (currentEntityId == entityId && currentEntityType == normalizedType) return
            currentEntityId = entityId
            currentEntityType = normalizedType
            
            refreshStats()
            refreshComments()
        } else if (!title.isNullOrBlank()) {
            _isLoading.value = true
            viewModelScope.launch {
                val resolvedTvdbId = tvdbRepository.resolveTvdbId(title, year, normalizedType)
                if (resolvedTvdbId != null && resolvedTvdbId > 0) {
                    val entityId = commsUniRepository.buildEntityId(resolvedTvdbId, seasonNumber, episodeNumber)
                    currentEntityId = entityId
                    currentEntityType = normalizedType
                    refreshStats()
                    refreshComments()
                } else {
                    _isLoading.value = false
                    _comments.value = emptyList()
                    _hasMoreComments.value = false
                }
            }
        } else {
            _isLoading.value = false
            _comments.value = emptyList()
            _hasMoreComments.value = false
        }
    }

    fun setSortAndFilters(sort: String, source: String?, language: String?) {
        if (currentSort == sort && currentSourceFilter == source && currentLanguageFilter == language) return
        currentSort = sort
        currentSourceFilter = source
        currentLanguageFilter = language
        refreshComments()
    }

    private fun refreshStats() {
        viewModelScope.launch {
            val result = commsUniRepository.getConversationStats(
                entityType = currentEntityType,
                entityId = currentEntityId,
                source = currentSourceFilter,
                language = currentLanguageFilter
            )
            result.onSuccess {
                _conversationStats.value = it.data
            }
        }
    }

    private var cachedSourcesMap: Map<String, com.cinetrack.data.api.SourceCatalogRow> = emptyMap()
    private var lastSyncedName: String? = null
    private var lastSyncedAvatar: String? = null
    private var cachedLocalAvatar: String? = auth.currentUser?.uid?.let { prefs.getString("user_avatar_$it", null) }
        ?: auth.currentUser?.photoUrl?.toString()
    private var cachedLocalName: String? = auth.currentUser?.uid?.let { prefs.getString("user_name_$it", null) }
        ?: auth.currentUser?.displayName

    private suspend fun syncProfileIfNeeded(): Pair<String, String?> {
        val user = auth.currentUser ?: return Pair("User", null)
        if (user.isAnonymous) return Pair("User", null)

        var name = user.displayName?.trim()
            ?: prefs.getString("user_name_${user.uid}", null)?.trim()
        var photo = user.photoUrl?.toString()?.trim()
            ?: prefs.getString("user_avatar_${user.uid}", null)?.trim()

        if (name.isNullOrBlank() || photo.isNullOrBlank()) {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(user.uid).get().await()
                if (name.isNullOrBlank()) {
                    name = doc.getString("displayName")?.trim()
                }
                if (photo.isNullOrBlank()) {
                    photo = doc.getString("photoUrl")?.trim()
                }
            } catch (_: Exception) {}
        }

        val finalName = name?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "User"

        cachedLocalAvatar = photo ?: cachedLocalAvatar
        cachedLocalName = finalName

        val editor = prefs.edit()
        if (!photo.isNullOrBlank()) editor.putString("user_avatar_${user.uid}", photo)
        if (!finalName.isBlank()) editor.putString("user_name_${user.uid}", finalName)
        editor.apply()

        val savedAuthorId = auth.currentUser?.uid?.let { prefs.getString("author_id_$it", null) }
        val currentSavedAuthorId = _cachedAuthorId.value ?: savedAuthorId
        if (currentSavedAuthorId != null && _cachedAuthorId.value == null) {
            _cachedAuthorId.value = currentSavedAuthorId
        }

        // Retroactively update existing comments in memory if avatar or name was retrieved
        if (!cachedLocalAvatar.isNullOrBlank() && _comments.value.isNotEmpty()) {
            val myAuthorId = currentAuthorId
            val myActorId = currentActorId
            val myIds = getMyCommentIds()
            val rawUid = auth.currentUser?.uid
            _comments.value = _comments.value.map { comment ->
                val isMine = (comment.id in myIds) ||
                    (!comment.userId.isBlank() && (
                        (myAuthorId != null && comment.userId == myAuthorId) ||
                        (myActorId != null && comment.userId == myActorId) ||
                        (rawUid != null && comment.userId == rawUid) ||
                        (currentUserId != null && comment.userId == currentUserId)
                    ))
                if (isMine) {
                    comment.copy(
                        userAvatarUrl = cachedLocalAvatar!!,
                        userDisplayName = if (comment.userDisplayName.isBlank()) finalName else comment.userDisplayName
                    )
                } else {
                    comment
                }
            }
        }

        if (finalName != lastSyncedName || photo != lastSyncedAvatar || currentSavedAuthorId == null) {
            val result = commsUniRepository.updateProfile(finalName, photo)
            if (result.isSuccess) {
                lastSyncedName = finalName
                lastSyncedAvatar = photo
                result.getOrNull()?.authorId?.let { serverAuthorId ->
                    recordAuthorId(serverAuthorId)
                }
            }
        }
        return Pair(finalName, photo ?: cachedLocalAvatar)
    }

    private fun refreshComments() {
        viewModelScope.launch {
            _isLoading.value = true
            nextCursor = null
            
            // Sincronizza il profilo dell'utente (nome/avatar) con CommsUni
            syncProfileIfNeeded()
            
            val sourcesResult = commsUniRepository.getSources()
            cachedSourcesMap = sourcesResult.getOrNull()?.associateBy { it.slug } ?: emptyMap()
            
            val result = commsUniRepository.getComments(
                entityType = currentEntityType,
                entityId = currentEntityId,
                sort = currentSort,
                limit = 50,
                cursor = null,
                source = currentSourceFilter,
                language = currentLanguageFilter
            )
            
            result.onSuccess { response ->
                _comments.value = extractAllComments(response.data.comments, cachedSourcesMap)
                nextCursor = response.data.nextCursor
                _hasMoreComments.value = !response.data.complete
            }.onFailure {
                if (it.message == "not_archived") {
                    _comments.value = emptyList()
                    _hasMoreComments.value = false
                } else {
                    // Show error as a dummy comment so the developer can see it
                    _comments.value = listOf(
                        com.cinetrack.data.model.AppComment(
                            id = "error_dummy",
                            mediaId = currentEntityId,
                            mediaType = currentEntityType,
                            userId = "sys",
                            userDisplayName = "System Error",
                            userAvatarUrl = "",
                            text = "Error loading comments: ${it.message}\n\n${it.stackTraceToString()}",
                            createdAt = com.google.firebase.Timestamp.now(),
                            likesCount = 0,
                            likedBy = emptyList(),
                            parentId = null,
                            parentUserId = null,
                            repliesCount = 0,
                            depth = 0,
                            isDeleted = false,
                            isSpoiler = false,
                            originSlug = "system",
                            originName = "System",
                            archivedLikes = 0,
                            nativeLikes = 0
                        )
                    )
                    _hasMoreComments.value = false
                }
            }
            _isLoading.value = false
        }
    }

    fun loadMoreComments() {
        if (_isLoadingMore.value || !_hasMoreComments.value || nextCursor == null) return
        
        viewModelScope.launch {
            _isLoadingMore.value = true
            
            val result = commsUniRepository.getComments(
                entityType = currentEntityType,
                entityId = currentEntityId,
                sort = currentSort,
                limit = 50,
                cursor = nextCursor,
                source = currentSourceFilter,
                language = currentLanguageFilter
            )
            
            result.onSuccess { response ->
                val currentList = _comments.value.toMutableList()
                val existingIds = currentList.map { it.id }.toSet()
                
                val newMapped = extractAllComments(response.data.comments, cachedSourcesMap)
                val toAdd = newMapped.filterNot { existingIds.contains(it.id) }
                
                currentList.addAll(toAdd)
                _comments.value = currentList
                
                nextCursor = response.data.nextCursor
                _hasMoreComments.value = !response.data.complete
            }
            
            _isLoadingMore.value = false
        }
    }

    // Qui andranno aggiunti i metodi per POST like, POST comment, report, delete ecc..
    // appena creeremo gli endpoint relativi in CommsUniService.kt

    fun setSort(
        option: com.cinetrack.data.model.CommentSortOption,
        order: com.cinetrack.data.model.CommentSortOrder,
        source: String? = currentSourceFilter
    ) {
        val sortString = if (option == com.cinetrack.data.model.CommentSortOption.DATE) "most_recent" else "most_liked"
        setSortAndFilters(sortString, source, currentLanguageFilter)
    }

    fun uploadCommentImage(imageUri: android.net.Uri, onSuccess: (android.net.Uri) -> Unit, onError: (String) -> Unit) {
        onError("Not implemented for CommsUni yet")
    }

    fun postComment(mediaId: String, mediaType: String, text: String, isSpoiler: Boolean, parentId: String?, parentUserId: String?, attachedMedia: List<String>) {}

    fun addComment(text: String, isSpoiler: Boolean, parentId: String?, parentUserId: String?, depth: Int, mediaTitle: String, mediaImage: String?) {
        if (isUserAnonymous) return  // Block anonymous writes silently
        val uid = currentUserId ?: return

        viewModelScope.launch {
            val (resolvedName, resolvedAvatar) = syncProfileIfNeeded()

            val optimisticAvatar = (resolvedAvatar ?: cachedLocalAvatar
                ?: auth.currentUser?.uid?.let { prefs.getString("user_avatar_$it", null) }
                ?: "").trim()

            val tempId = "temp_${System.currentTimeMillis()}"
            val optimistic = com.cinetrack.data.model.AppComment(
                id = tempId,
                mediaId = currentEntityId,
                mediaType = currentEntityType,
                userId = currentUserId ?: currentActorId ?: uid,
                userDisplayName = resolvedName,
                userAvatarUrl = optimisticAvatar,
                text = text,
                createdAt = com.google.firebase.Timestamp.now(),
                likesCount = 0,
                likedBy = emptyList(),
                parentId = parentId,
                parentUserId = parentUserId,
                repliesCount = 0,
                depth = depth.coerceAtMost(1),
                isDeleted = false,
                isSpoiler = isSpoiler,
                originSlug = "flicktrove",
                originName = "FlickTrove",
                originColor = "#2dd4bf",
                archivedLikes = 0,
                nativeLikes = 0
            )

            val currentList = _comments.value.toMutableList()
            // Insert right after parent if it's a reply, else at top
            if (parentId != null) {
                val parentIndex = currentList.indexOfFirst { it.id == parentId }
                if (parentIndex != -1) currentList.add(parentIndex + 1, optimistic)
                else currentList.add(0, optimistic)
            } else {
                currentList.add(0, optimistic)
            }
            _comments.value = currentList

            // Real API call
            val result = if (parentId != null) {
                commsUniRepository.createReply(parentId, text, isSpoiler)
            } else {
                commsUniRepository.createComment(currentEntityType, currentEntityId, text, isSpoiler)
            }

            result.onSuccess { newComment ->
                newComment.userId?.let { recordAuthorId(it) }
                trackMyCommentId(newComment.id)
                // Replace temp with real comment
                val real = newComment.toAppComment(cachedSourcesMap)
                val finalReal = if (real.userAvatarUrl.isBlank() && optimisticAvatar.isNotBlank()) {
                    real.copy(userAvatarUrl = optimisticAvatar)
                } else {
                    real
                }
                val updated = _comments.value.toMutableList()
                val tempIndex = updated.indexOfFirst { it.id == tempId }
                if (tempIndex != -1) updated[tempIndex] = finalReal
                _comments.value = updated

                // Se è una risposta a un commento FlickTrover, invia la notifica Social e Push all'autore
                if (parentId != null) {
                    val parentComment = _comments.value.find { it.id == parentId }
                    val targetAuthorId = parentUserId ?: parentComment?.userId
                    val isParentFlickTrove = parentComment?.originSlug?.equals("flicktrove", ignoreCase = true) == true
                        || (parentComment == null && targetAuthorId != null)

                    if (isParentFlickTrove && !targetAuthorId.isNullOrBlank()) {
                        val currentAuthUid = auth.currentUser?.uid
                        if (currentAuthUid != null) {
                            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val targetUid = resolveFirebaseUidForAuthor(targetAuthorId)
                                    if (targetUid != null && targetUid != currentAuthUid) {
                                        val notifRef = firestore.collection("user_social_notifications")
                                            .document(targetUid)
                                            .collection("items")
                                            .document()
                                        val socialNotif = hashMapOf(
                                            "id" to notifRef.id,
                                            "type" to "reply",
                                            "mediaId" to currentRawMediaId,
                                            "mediaType" to currentEntityType,
                                            "mediaTitle" to mediaTitle,
                                            "mediaImage" to mediaImage,
                                            "commentId" to newComment.id,
                                            "senderName" to (auth.currentUser?.displayName ?: "Qualcuno"),
                                            "senderUserId" to currentAuthUid,
                                            "snippet" to text.take(80),
                                            "createdAt" to Timestamp.now(),
                                            "isRead" to false
                                        )
                                        notifRef.set(socialNotif).await()

                                        com.cinetrack.util.SupabaseNotificationService.notifyUser(
                                            targetUserId = targetUid,
                                            titleLocKey = "notification_reply_title",
                                            bodyLocKey = "notification_reply_body",
                                            bodyLocArgs = listOf(auth.currentUser?.displayName ?: "Qualcuno"),
                                            mediaId = currentRawMediaId.toLongOrNull() ?: 0L,
                                            mediaType = currentEntityType,
                                            mediaImage = mediaImage,
                                            commentId = newComment.id
                                        )
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("CommsUniViewModel", "Failed to send reply social notification", e)
                                }
                            }
                        }
                    }
                }
            }.onFailure { e ->
                // Remove the optimistic item
                _comments.value = _comments.value.filter { it.id != tempId }
                android.util.Log.e("CommsUniViewModel", "Failed to send comment", e)
                val msg = if (!e.message.isNullOrBlank() && !e.message!!.startsWith("HTTP error")) {
                    e.message!!
                } else {
                    "Impossibile inviare il commento."
                }
                actionFeedbackManager.emit(UiText.DynamicString(msg))
            }
        }
    }
    
    fun toggleSpoilerStatus(commentId: String, currentStatus: Boolean) {
        val targetStatus = !currentStatus
        // Optimistic update locally
        val currentList = _comments.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == commentId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(isSpoiler = targetStatus)
            _comments.value = currentList
        }

        viewModelScope.launch {
            val result = commsUniRepository.updateCommentSpoiler(commentId, targetStatus)
            if (result.isFailure) {
                // Revert on failure
                val revertList = _comments.value.toMutableList()
                val revertIndex = revertList.indexOfFirst { it.id == commentId }
                if (revertIndex != -1) {
                    revertList[revertIndex] = revertList[revertIndex].copy(isSpoiler = currentStatus)
                    _comments.value = revertList
                }
                actionFeedbackManager.emit(UiText.DynamicString("Impossibile aggiornare lo spoiler."))
            }
        }
    }

    fun reportComment(
        commentId: String,
        category: String,
        text: String? = null,
        userId: String? = null,
        userDisplayName: String? = null,
        detail: String? = null
    ) {
        viewModelScope.launch {
            val result = commsUniRepository.reportComment(commentId, reason = category, detail = detail)
            result.onSuccess { reportStatus ->
                when (reportStatus) {
                    CommsUniRepository.ReportCommentResult.SUCCESS -> {
                        actionFeedbackManager.emit(UiText.StringResource(R.string.comment_report_success))
                    }
                    CommsUniRepository.ReportCommentResult.DUPLICATE -> {
                        actionFeedbackManager.emit(UiText.StringResource(R.string.comment_report_duplicate))
                    }
                    CommsUniRepository.ReportCommentResult.ERROR -> {
                        actionFeedbackManager.emit(UiText.StringResource(R.string.comment_report_error))
                    }
                }
            }.onFailure {
                actionFeedbackManager.emit(UiText.StringResource(R.string.comment_report_error))
            }
        }
    }

    fun blockAuthor(userId: String, authorName: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            blockedAuthorsManager.blockAuthor(userId, authorName)
            _comments.value = _comments.value.filterNot { it.userId == userId }
            actionFeedbackManager.emit(
                UiText.StringResource(R.string.comment_block_user_success, authorName)
            )
        }
    }

    fun unblockAuthor(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            blockedAuthorsManager.unblockAuthor(userId)
            actionFeedbackManager.emit(
                UiText.StringResource(R.string.comment_unblock_user_success)
            )
            refreshComments()
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            // Optimistic: remove immediately
            val currentList = _comments.value.toMutableList()
            val removed = currentList.find { it.id == commentId }
            currentList.removeAll { it.id == commentId }
            _comments.value = currentList

            val result = commsUniRepository.deleteComment(commentId)
            if (result.isFailure) {
                // Revert
                removed?.let {
                    val revertList = _comments.value.toMutableList()
                    revertList.add(it)
                    _comments.value = revertList.sortedBy { c -> c.createdAt?.seconds ?: 0L }
                }
                actionFeedbackManager.emit(UiText.DynamicString("Impossibile eliminare il commento."))
            }
        }
    }


    fun toggleLikeComment(commentId: String, mediaTitle: String, mediaImage: String?) {
        val currentAuthUid = auth.currentUser?.uid ?: return
        val uid = currentUserId ?: currentAuthUid
        viewModelScope.launch {
            val currentList = _comments.value.toMutableList()
            val commentIndex = currentList.indexOfFirst { it.id == commentId }
            if (commentIndex == -1) return@launch
            
            val comment = currentList[commentIndex]
            val isLiked = comment.likedBy.contains(uid)
            
            // Optimistic update
            val newLikedBy = if (isLiked) {
                comment.likedBy.filter { it != uid }
            } else {
                comment.likedBy + uid
            }
            
            val updatedComment = comment.copy(
                likedBy = newLikedBy,
                likesCount = comment.likesCount + if (isLiked) -1 else 1,
                nativeLikes = comment.nativeLikes + if (isLiked) -1 else 1
            )
            
            currentList[commentIndex] = updatedComment
            _comments.value = currentList
            
            // Network call
            val result = if (isLiked) {
                commsUniRepository.unlikeComment(commentId)
            } else {
                commsUniRepository.likeComment(commentId)
            }
            
            if (result.isFailure) {
                // Revert
                val revertList = _comments.value.toMutableList()
                val revertIndex = revertList.indexOfFirst { it.id == commentId }
                if (revertIndex != -1) {
                    revertList[revertIndex] = comment
                    _comments.value = revertList
                }
                actionFeedbackManager.emit(UiText.DynamicString("Errore di rete con CommsUni."))
                return@launch
            }

            // Se il commento appartiene a un FlickTrover, invia/rimuovi notifica Social e Push
            if (comment.originSlug.equals("flicktrove", ignoreCase = true) && !comment.userId.isBlank()) {
                launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val targetUid = resolveFirebaseUidForAuthor(comment.userId)
                        if (targetUid != null && targetUid != currentAuthUid) {
                            val notifId = "${commentId}_${currentAuthUid}_like"
                            val notifRef = firestore.collection("user_social_notifications")
                                .document(targetUid)
                                .collection("items")
                                .document(notifId)

                            if (!isLiked) {
                                val notif = hashMapOf(
                                    "id" to notifRef.id,
                                    "type" to "like",
                                    "mediaId" to currentRawMediaId,
                                    "mediaType" to currentEntityType,
                                    "mediaTitle" to mediaTitle,
                                    "mediaImage" to mediaImage,
                                    "commentId" to commentId,
                                    "senderName" to (auth.currentUser?.displayName ?: "Qualcuno"),
                                    "senderUserId" to currentAuthUid,
                                    "snippet" to comment.text.take(80),
                                    "createdAt" to Timestamp.now(),
                                    "isRead" to false
                                )
                                notifRef.set(notif, com.google.firebase.firestore.SetOptions.merge()).await()

                                com.cinetrack.util.SupabaseNotificationService.notifyUser(
                                    targetUserId = targetUid,
                                    titleLocKey = "notification_like_title",
                                    bodyLocKey = "notification_like_body",
                                    bodyLocArgs = listOf(auth.currentUser?.displayName ?: "Qualcuno"),
                                    mediaId = currentRawMediaId.toLongOrNull() ?: 0L,
                                    mediaType = currentEntityType,
                                    mediaImage = mediaImage,
                                    commentId = commentId
                                )
                            } else {
                                notifRef.delete().await()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CommsUniViewModel", "Failed to update like social notification", e)
                    }
                }
            }
        }
    }
    
    fun translateComment(commentId: String, text: String, requireWifi: Boolean? = null) {
        val current = _translationStates.value[commentId]
        // Toggle: if already translated, reset to untranslated
        if (current is CommentsViewModel.TranslationState.Translated) {
            _translationStates.value = _translationStates.value - commentId
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

            val effectiveSourceLang = detectedLang ?: if (targetMlKit != TranslateLanguage.ENGLISH) {
                TranslateLanguage.ENGLISH
            } else {
                TranslateLanguage.ITALIAN
            }

            // Step 2: Check / Download models
            val modelReady = translationManager.isModelDownloaded(effectiveSourceLang, targetMlKit)
            if (!modelReady) {
                if (requireWifi == null) {
                    _showTranslationPrompt.value = Pair(commentId, cleanText)
                    return@launch
                }
                _showTranslationPrompt.value = null
                _translationStates.value = _translationStates.value + (commentId to CommentsViewModel.TranslationState.Downloading)
                actionFeedbackManager.emit(UiText.DynamicString("Download del pacchetto lingua in corso..."))
                val downloaded = translationManager.downloadModels(effectiveSourceLang, targetMlKit, requireWifi = requireWifi)
                if (!downloaded) {
                    _translationStates.value = _translationStates.value + (commentId to CommentsViewModel.TranslationState.Error)
                    actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_lang_model))
                    return@launch
                }
            } else {
                _showTranslationPrompt.value = null
            }

            // Step 3: Translate
            _translationStates.value = _translationStates.value + (commentId to CommentsViewModel.TranslationState.Translating)
            val translated = translationManager.translateFrom(cleanText, effectiveSourceLang, targetMlKit)
            if (translated != null && translated.trim().lowercase() != cleanText.trim().lowercase()) {
                _translationStates.value = _translationStates.value + (commentId to CommentsViewModel.TranslationState.Translated(translated))
            } else {
                actionFeedbackManager.emit(UiText.StringResource(R.string.comment_already_in_language))
                _translationStates.value = _translationStates.value - commentId
            }
        }
    }
    
    fun loadRepliesForComment(commentId: String) {
        val currentComments = _comments.value
        val target = currentComments.find { it.id == commentId }

        // CommsUni replies endpoint:
        // /v1/comments/{commentId}/replies?parent=...
        // where the path parameter must be the thread root comment ID,
        // and the 'parent' query parameter is the nested comment being expanded.
        val isNested = target != null && !target.parentId.isNullOrBlank()
        val rootId = if (isNested) {
            target?.rootCommentId ?: run {
                var curr: com.cinetrack.data.model.AppComment? = target
                while (curr != null && !curr.parentId.isNullOrBlank()) {
                    curr = currentComments.find { it.id == curr?.parentId }
                }
                curr?.id ?: target?.parentId ?: commentId
            }
        } else {
            commentId
        }
        val parentParam = if (isNested) commentId else null

        viewModelScope.launch {
            val result = commsUniRepository.getReplies(
                commentId = rootId,
                sort = "most_recent",
                limit = 50,
                parent = parentParam,
                source = currentSourceFilter,
                language = currentLanguageFilter
            )
            
            result.onSuccess { response ->
                val currentList = _comments.value.toMutableList()
                val existingIds = currentList.map { it.id }.toSet()
                
                // Map replies and ensure parentId points to commentId if not set, and rootCommentId is preserved
                val newMapped = extractAllComments(response.data.allItems, cachedSourcesMap)
                    .map { reply ->
                        val effectiveParentId = if (reply.parentId.isNullOrBlank()) commentId else reply.parentId
                        val effectiveRootId = if (reply.rootCommentId.isNullOrBlank()) rootId else reply.rootCommentId
                        reply.copy(parentId = effectiveParentId, rootCommentId = effectiveRootId)
                    }
                
                val toAdd = newMapped.filterNot { existingIds.contains(it.id) }
                
                val fixedExisting = currentList.map { existing ->
                    val fixedReply = newMapped.find { it.id == existing.id }
                    if (fixedReply != null && existing.parentId.isNullOrBlank()) {
                        existing.copy(parentId = commentId, rootCommentId = rootId)
                    } else existing
                }.toMutableList()
                
                fixedExisting.addAll(toAdd)
                _comments.value = fixedExisting
            }.onFailure { error ->
                android.util.Log.e("CommsUniViewModel", "Failed to load replies for $commentId (root: $rootId, parent: $parentParam)", error)
            }
        }
    }

    fun translateAllComments() {}

    val isUserBanned = MutableStateFlow(false)
    val banExpiration = MutableStateFlow<String?>(null)

    private fun extractAllComments(comments: List<CommsUniComment>, sourcesMap: Map<String, com.cinetrack.data.api.SourceCatalogRow>): List<com.cinetrack.data.model.AppComment> {
        val blockedIds = blockedAuthorsManager.blockedAuthorIds.value
        val myAuthorId = currentAuthorId
        val myActorId = currentActorId
        val rawUid = auth.currentUser?.uid
        val myCommentIds = getMyCommentIds()

        val result = mutableListOf<com.cinetrack.data.model.AppComment>()
        fun extract(comment: CommsUniComment) {
            val authorId = comment.userId
            val isMine = (comment.id in myCommentIds) ||
                (!authorId.isNullOrBlank() && (
                    (myAuthorId != null && authorId == myAuthorId) ||
                    (myActorId != null && authorId == myActorId) ||
                    (rawUid != null && authorId == rawUid) ||
                    (currentUserId != null && authorId == currentUserId)
                ))

            if (isMine || authorId == null || authorId !in blockedIds) {
                result.add(comment.toAppComment(sourcesMap))
                comment.replies.forEach { extract(it) }
            }
        }
        comments.forEach { extract(it) }
        return result
    }

    private fun CommsUniComment.toAppComment(sourcesMap: Map<String, com.cinetrack.data.api.SourceCatalogRow>): com.cinetrack.data.model.AppComment {
        val myAuthorId = currentAuthorId
        val myActorId = currentActorId
        val rawUid = auth.currentUser?.uid
        val myCommentIds = getMyCommentIds()

        val isMyComment = (this.id in myCommentIds) ||
            (!this.userId.isNullOrBlank() && (
                (myAuthorId != null && this.userId == myAuthorId) ||
                (myActorId != null && this.userId == myActorId) ||
                (rawUid != null && this.userId == rawUid)
            ))

        if (isMyComment && myAuthorId.isNullOrBlank() && !this.userId.isNullOrBlank()) {
            recordAuthorId(this.userId)
        }

        val localPhoto = (cachedLocalAvatar
            ?: auth.currentUser?.uid?.let { prefs.getString("user_avatar_$it", null) }
            ?: auth.currentUser?.photoUrl?.toString()
            ?: "").trim()

        val localName = if (!cachedLocalName.isNullOrBlank()) cachedLocalName else auth.currentUser?.displayName

        return com.cinetrack.data.mapper.CommsUniMapper.toAppComment(
            comment = this,
            sourcesMap = sourcesMap,
            currentEntityId = currentEntityId,
            currentEntityType = currentEntityType,
            currentUserId = currentUserId,
            myAuthorId = myAuthorId,
            myActorId = myActorId,
            rawUid = rawUid,
            myCommentIds = myCommentIds,
            cachedLocalAvatar = localPhoto,
            cachedLocalName = localName
        )
    }

    override fun onCleared() {
        super.onCleared()
        translationManager.closeAll()
    }
}
