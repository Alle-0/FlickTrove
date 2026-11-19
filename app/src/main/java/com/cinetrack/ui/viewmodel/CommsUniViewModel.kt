package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.api.CommsUniComment
import com.cinetrack.data.model.AppComment
import com.cinetrack.data.model.filterDeletedWithoutReplies
import com.cinetrack.data.model.deduplicateComments
import com.cinetrack.data.model.mergeComments
import com.cinetrack.data.model.isSameAuthor
import com.cinetrack.data.model.isSameCommentText
import com.cinetrack.data.model.isSameMediaType
import com.cinetrack.data.api.CommsUniConversationStats
import com.cinetrack.data.api.CommsUniOrigin
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.repository.CommsUniRepository
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.TaggedMediaRef
import com.cinetrack.ui.utils.UiText
import com.cinetrack.ui.utils.extractTmdbMediaRefsFromText
import com.cinetrack.R
import com.cinetrack.util.TranslationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CommsUniViewModel @Inject constructor(
    private val commsUniRepository: CommsUniRepository,
    private val commentRepository: com.cinetrack.data.repository.CommentRepository,
    private val preferenceRepository: PreferenceRepository,
    private val auth: FirebaseAuth,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val actionFeedbackManager: ActionFeedbackManager,
    private val translationManager: TranslationManager,
    private val blockedAuthorsManager: com.cinetrack.data.repository.BlockedAuthorsManager,
    private val tvdbRepository: com.cinetrack.data.repository.TvdbRepository,
    private val storageRepository: com.cinetrack.data.repository.StorageRepository,
    private val badgeRepository: com.cinetrack.data.repository.BadgeRepository,
    private val movieRepository: com.cinetrack.data.repository.MovieRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val prefs by lazy { context.getSharedPreferences("commsuni_prefs", android.content.Context.MODE_PRIVATE) }
    private val _cachedAuthorId = MutableStateFlow<String?>(null)

    private val moderationDelegate by lazy {
        CommsUniModerationDelegate(commentRepository, commsUniRepository, blockedAuthorsManager, storageRepository, badgeRepository, actionFeedbackManager)
    }
    private val reactionsDelegate by lazy {
        CommsUniReactionsDelegate(auth, firestore, commsUniRepository, commentRepository, actionFeedbackManager)
    }
    private val translationDelegate by lazy {
        CommsUniTranslationDelegate(translationManager, preferenceRepository, actionFeedbackManager)
    }
    private val mediaTagDelegate by lazy {
        CommsUniMediaTagDelegate(movieRepository)
    }

    val currentAuthorId: String?
        get() = _cachedAuthorId.value ?: auth.currentUser?.uid?.let { prefs.getString("author_id_$it", null) }

    val isUserAnonymous: Boolean
        get() = auth.currentUser?.isAnonymous != false
        
    val currentActorId: String?
        get() = auth.currentUser?.uid?.takeIf { !auth.currentUser!!.isAnonymous }?.let {
            com.cinetrack.data.api.CommsUniInterceptor.generateActorId(it)
        }

    val currentUserId: String? get() = currentAuthorId ?: currentActorId ?: auth.currentUser?.uid

    fun isCommentLikedByMe(comment: com.cinetrack.data.model.AppComment): Boolean {
        val currentAuthUid = auth.currentUser?.uid
        val authorId = currentAuthorId
        val actorId = currentActorId
        return comment.likedBy.any { id ->
            (currentAuthUid != null && id == currentAuthUid) ||
            (authorId != null && id.equals(authorId, ignoreCase = true)) ||
            (actorId != null && id.equals(actorId, ignoreCase = true))
        }
    }

    fun isCommentMine(comment: com.cinetrack.data.model.AppComment): Boolean {
        val currentAuthUid = auth.currentUser?.uid
        val authorId = currentAuthorId
        val actorId = currentActorId
        val myCommentIds = getMyCommentIds()
        
        return (comment.id.isNotBlank() && comment.id in myCommentIds) ||
               (currentAuthUid != null && comment.userId == currentAuthUid) ||
               (authorId != null && comment.userId.equals(authorId, ignoreCase = true)) ||
               (actorId != null && comment.userId.equals(actorId, ignoreCase = true)) ||
               (currentUserId != null && comment.userId.equals(currentUserId, ignoreCase = true))
    }

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

    private fun untrackMyCommentId(id: String) {
        val uid = auth.currentUser?.uid ?: return
        val currentSet = prefs.getStringSet("my_comment_ids_$uid", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.remove(id)
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

    // Available sources list — populated after getSources() in refreshComments()
    private val _availableSources = MutableStateFlow<List<com.cinetrack.data.api.SourceCatalogRow>>(emptyList())
    val availableSources: StateFlow<List<com.cinetrack.data.api.SourceCatalogRow>> = _availableSources.asStateFlow()

    // Sort & Filters
    var currentSort = "most_recent"
        private set
    var currentSourceFilter: String? = null
        private set
    var currentLanguageFilter: String? = null
        private set

    private var currentEntityId: String = ""
    private var currentEntityType: String = ""
    private var currentTitle: String? = null

    private val _showSeasons = MutableStateFlow<List<com.cinetrack.data.model.Season>>(emptyList())
    val showSeasons: StateFlow<List<com.cinetrack.data.model.Season>> = _showSeasons.asStateFlow()

    private val _seasonEpisodes = MutableStateFlow<Map<Int, List<com.cinetrack.data.model.Episode>>>(emptyMap())
    val seasonEpisodes: StateFlow<Map<Int, List<com.cinetrack.data.model.Episode>>> = _seasonEpisodes.asStateFlow()

    private val _isLoadingSeasonEpisodes = MutableStateFlow(false)
    val isLoadingSeasonEpisodes: StateFlow<Boolean> = _isLoadingSeasonEpisodes.asStateFlow()

    private var targetTvShowId: Long? = null

    fun loadSeasonEpisodes(seasonNumber: Int) {
        val showId = targetTvShowId ?: return
        if (_seasonEpisodes.value.containsKey(seasonNumber)) return
        viewModelScope.launch {
            _isLoadingSeasonEpisodes.value = true
            try {
                val detailedSeason = movieRepository.fetchSeasonDetails(showId, seasonNumber)
                val episodes = detailedSeason.episodes ?: emptyList()
                _seasonEpisodes.value = _seasonEpisodes.value + (seasonNumber to episodes)
            } catch (e: Exception) {
                // Ignore offline
            } finally {
                _isLoadingSeasonEpisodes.value = false
            }
        }
    }

    // Translation State (Identical logic to old CommentsViewModel)
    // Uses CommentsViewModel.TranslationState for compatibility with UI components


    val translationStates: StateFlow<Map<String, CommentsViewModel.TranslationState>> get() = translationDelegate.translationStates
    val showTranslationPrompt: StateFlow<Pair<String, String>?> get() = translationDelegate.showTranslationPrompt

    // #movie / #tv hashtag search & cross-language resolution
    val mediaTagSuggestions: StateFlow<List<TaggedMediaRef>> get() = mediaTagDelegate.mediaTagSuggestions
    val isSearchingMediaTags: StateFlow<Boolean> get() = mediaTagDelegate.isSearchingMediaTags
    val taggedMediaTitles: StateFlow<Map<String, String>> get() = mediaTagDelegate.taggedMediaTitles
    val taggedMediaPosters: StateFlow<Map<String, String>> get() = mediaTagDelegate.taggedMediaPosters

    fun dismissTranslationPrompt() {
        translationDelegate.dismissTranslationPrompt()
    }

    init {
        viewModelScope.launch {
            _comments.collect { list ->
                if (list.isNotEmpty()) {
                    mediaTagDelegate.resolveMediaTagsForComments(list, viewModelScope)
                }
            }
        }
        viewModelScope.launch {
            commsUniRepository.liveCommentLikes.collect { overrides ->
                if (overrides.isNotEmpty() && _comments.value.isNotEmpty()) {
                    val uid = auth.currentUser?.uid
                    val updated = commsUniRepository.applyLikeOverrides(_comments.value, uid)
                    if (updated != _comments.value) {
                        _comments.value = updated
                    }
                }
            }
        }

        viewModelScope.launch {
            combine(
                blockedAuthorsManager.blockedAuthorIds,
                blockedAuthorsManager.blockedAuthorNames
            ) { ids, names -> Pair(ids, names) }
                .collect { (blockedIds, blockedNames) ->
                    if ((blockedIds.isNotEmpty() || blockedNames.isNotEmpty()) && _comments.value.isNotEmpty()) {
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
                            val cleanName = comment.userDisplayName.trim().lowercase()
                            val isBlocked = comment.userId in blockedIds ||
                                "name_$cleanName" in blockedIds ||
                                cleanName in blockedNames
                            !isMine && isBlocked
                        }
                    }
                }
        }
    }

    fun init(
        tvdbId: Int? = null,
        entityType: String = "movie",
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        title: String? = null,
        year: String? = null,
        rawMediaId: String = "",
        imdbId: String? = null
    ) {
        if (rawMediaId.isNotBlank()) {
            val effectiveRawMediaId = when {
                seasonNumber != null && episodeNumber != null && !rawMediaId.contains("_s") -> "${rawMediaId}_s${seasonNumber}e${episodeNumber}"
                seasonNumber != null && !rawMediaId.contains("_s") -> "${rawMediaId}_s${seasonNumber}"
                else -> rawMediaId
            }
            currentRawMediaId = effectiveRawMediaId
        }
        if (!title.isNullOrBlank()) {
            currentTitle = title
        }
        val normalizedType = when {
            seasonNumber != null && episodeNumber != null -> "episode"
            seasonNumber != null -> "season"
            entityType.lowercase() in listOf("tv", "series", "show") -> "show"
            else -> "movie"
        }
        val baseIdStr = if (rawMediaId.contains("_s")) rawMediaId.substringBefore("_s") else rawMediaId
        val showId = baseIdStr.toLongOrNull()
        if (showId != null && showId > 0 && normalizedType in listOf("show", "season", "episode")) {
            targetTvShowId = showId
            viewModelScope.launch {
                try {
                    val cachedLocal = movieRepository.getMovie(showId, "tv")
                    val localSeasons = cachedLocal?.seasons?.filter { (it.seasonNumber) > 0 }
                    if (!localSeasons.isNullOrEmpty()) {
                        _showSeasons.value = localSeasons
                        localSeasons.forEach { s ->
                            if (!s.episodes.isNullOrEmpty()) {
                                _seasonEpisodes.value = _seasonEpisodes.value + (s.seasonNumber to s.episodes)
                            }
                        }
                    }
                    movieRepository.getMovieDetailsFlow(showId, true).collect { response ->
                        val seasons = response.seasons?.filter { (it.seasonNumber) > 0 } ?: emptyList()
                        if (seasons.isNotEmpty()) {
                            _showSeasons.value = seasons
                        }
                    }
                } catch (e: Exception) {
                    // Ignore offline
                }
            }
            if (seasonNumber != null) {
                loadSeasonEpisodes(seasonNumber)
            }
        }

        if (tvdbId != null && tvdbId > 0) {
            val entityId = commsUniRepository.buildEntityId(tvdbId, seasonNumber, episodeNumber)
            if (currentEntityId == entityId && currentEntityType == normalizedType) return
            currentEntityId = entityId
            currentEntityType = normalizedType
            
            refreshStats()
            refreshComments()
        } else {
            _isLoading.value = true
            viewModelScope.launch {
                val searchType = if (normalizedType in listOf("show", "season", "episode")) "series" else "movie"
                val resolvedTvdbId = (if (!imdbId.isNullOrBlank()) {
                    tvdbRepository.resolveTvdbIdByRemoteId(imdbId, searchType)
                } else null) ?: (if (rawMediaId.isNotBlank()) {
                    tvdbRepository.resolveTvdbIdByRemoteId(rawMediaId, searchType)
                } else null) ?: if (!title.isNullOrBlank()) {
                    tvdbRepository.resolveTvdbId(title, year, searchType)
                } else null

                if (resolvedTvdbId != null && resolvedTvdbId > 0) {
                    val entityId = commsUniRepository.buildEntityId(resolvedTvdbId, seasonNumber, episodeNumber)
                    currentEntityId = entityId
                    currentEntityType = normalizedType
                    refreshStats()
                    refreshComments()
                } else {
                    _isLoading.value = false
                    refreshComments()
                }
            }
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
            val sourcesList = sourcesResult.getOrNull() ?: emptyList()
            cachedSourcesMap = sourcesList.associateBy { it.slug }
            if (sourcesList.isNotEmpty()) {
                val filteredSources = sourcesList.filter {
                    !it.slug.equals("commsuni.tv", ignoreCase = true) &&
                    !it.slug.equals("commsuni", ignoreCase = true) &&
                    !(it.displayName?.equals("commsuni.tv", ignoreCase = true) == true)
                }
                _availableSources.value = filteredSources
            }
            
            coroutineScope {
                val commsUniDeferred = async {
                    if (currentEntityId.isNotBlank()) {
                        commsUniRepository.getComments(
                            entityType = currentEntityType,
                            entityId = currentEntityId,
                            sort = currentSort,
                            limit = 50,
                            cursor = null,
                            source = currentSourceFilter,
                            language = currentLanguageFilter
                        )
                    } else null
                }

                val firestoreDeferred = async {
                    if (currentRawMediaId.isNotBlank()) {
                        try {
                            val (fComments, _) = commentRepository.getCommentsForMedia(
                                mediaId = currentRawMediaId,
                                limit = 50,
                                mediaType = currentEntityType
                            )
                            fComments
                        } catch (e: Exception) {
                            emptyList<com.cinetrack.data.model.AppComment>()
                        }
                    } else emptyList<com.cinetrack.data.model.AppComment>()
                }

                val commsUniResult = commsUniDeferred.await()
                val firestoreComments = firestoreDeferred.await().filter { isSameMediaType(it.mediaType, currentEntityType) }
                var commsUniList = emptyList<com.cinetrack.data.model.AppComment>()
                if (commsUniResult != null) {
                    commsUniResult.onSuccess { response ->
                        commsUniList = extractAllComments(response.data.comments, cachedSourcesMap)
                        nextCursor = response.data.nextCursor
                        _hasMoreComments.value = !response.data.complete
                        if (response.data.languageCounts.isNotEmpty() || response.data.sourceCounts.isNotEmpty()) {
                            val currentStats = _conversationStats.value
                            if (currentStats == null) {
                                _conversationStats.value = CommsUniConversationStats(
                                    languageCounts = response.data.languageCounts,
                                    sourceCounts = response.data.sourceCounts
                                )
                            } else {
                                _conversationStats.value = currentStats.copy(
                                    languageCounts = if (response.data.languageCounts.isNotEmpty()) response.data.languageCounts else currentStats.languageCounts,
                                    sourceCounts = if (response.data.sourceCounts.isNotEmpty()) response.data.sourceCounts else currentStats.sourceCounts
                                )
                            }
                        }
                    }.onFailure {
                        if (it.message != "not_archived") {
                            android.util.Log.e("CommsUniViewModel", "Error loading CommsUni comments", it)
                        }
                        _hasMoreComments.value = false
                    }
                } else {
                    _hasMoreComments.value = false
                }

                val matchedFirestoreIds = mutableSetOf<String>()
                val mergedCommsUni = commsUniList.map { cc ->
                    val matchingFc = firestoreComments.firstOrNull { fc ->
                        fc.id == cc.id || (
                            com.cinetrack.data.model.isSameAuthor(fc, cc) &&
                            com.cinetrack.data.model.isSameCommentText(fc.text, cc.text) &&
                            fc.parentId == cc.parentId
                        )
                    }
                    if (matchingFc != null) {
                        matchedFirestoreIds.add(matchingFc.id)
                        if (auth.currentUser?.uid != null && matchingFc.userId == auth.currentUser?.uid) {
                            trackMyCommentId(cc.id)
                        }
                        com.cinetrack.data.model.mergeComments(cc, matchingFc)
                    } else {
                        cc
                    }
                }

                val remainingFirestore = firestoreComments
                    .filterNot { it.id in matchedFirestoreIds }
                    .filter { fc ->
                        val matchesSource = currentSourceFilter.isNullOrBlank() || fc.originSlug.equals(currentSourceFilter, ignoreCase = true)
                        val matchesLanguage = currentLanguageFilter.isNullOrBlank() || fc.language.isNullOrBlank() || fc.language.equals(currentLanguageFilter, ignoreCase = true)
                        matchesSource && matchesLanguage
                    }
                    .map { fc ->
                        if (auth.currentUser?.uid != null && fc.userId == auth.currentUser?.uid) {
                            trackMyCommentId(fc.id)
                        }
                        val isCommsUni = fc.isOnCommsUni
                        val fsId = fc.firestoreId?.takeIf { it.isNotBlank() } ?: fc.id
                        if (fc.originSlug.isNullOrBlank()) {
                            fc.copy(
                                firestoreId = fsId,
                                originSlug = "flicktrove",
                                originName = "FlickTrove",
                                originColor = "#2dd4bf",
                                isOnCommsUni = isCommsUni
                            )
                        } else fc.copy(firestoreId = fsId, isOnCommsUni = isCommsUni)
                    }

                val blockedIds = blockedAuthorsManager.blockedAuthorIds.value
                val blockedNames = blockedAuthorsManager.blockedAuthorNames.value
                val combined = (mergedCommsUni + remainingFirestore)
                    .filterNot { 
                        val cleanName = it.userDisplayName.trim().lowercase()
                        it.userId in blockedIds || "name_$cleanName" in blockedIds || cleanName in blockedNames
                    }
                    .deduplicateComments()
                    .filterDeletedWithoutReplies()

                val sorted = if (currentSort == "most_liked" || currentSort == "likes") {
                    combined.sortedWith(compareByDescending<com.cinetrack.data.model.AppComment> { it.likesCount }.thenByDescending { it.createdAt?.seconds ?: 0L })
                } else {
                    combined.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }

                val currentUid = auth.currentUser?.uid
                val withOverrides = commsUniRepository.applyLikeOverrides(sorted, currentUid)
                _comments.value = withOverrides
                _isLoading.value = false
                autoTranslateCommentsIfModelsDownloaded(withOverrides)
            }
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
                val blockedIds = blockedAuthorsManager.blockedAuthorIds.value
                val blockedNames = blockedAuthorsManager.blockedAuthorNames.value
                val toAdd = newMapped
                    .filterNot { existingIds.contains(it.id) }
                    .filterNot {
                        val cleanName = it.userDisplayName.trim().lowercase()
                        it.userId in blockedIds || "name_$cleanName" in blockedIds || cleanName in blockedNames
                    }
                
                currentList.addAll(toAdd)
                val currentUid = auth.currentUser?.uid
                val deduplicated = currentList.deduplicateComments().filterDeletedWithoutReplies()
                _comments.value = commsUniRepository.applyLikeOverrides(deduplicated, currentUid)
                autoTranslateCommentsIfModelsDownloaded(toAdd)
                
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
        viewModelScope.launch {
            val result = storageRepository.uploadCommentImage(imageUri)
            result.onSuccess { url ->
                onSuccess(android.net.Uri.parse(url))
            }.onFailure { e ->
                onError(e.message ?: "Upload fallito")
            }
        }
    }

    fun deleteDraftCommentImage(imageUrl: String) {
        if (imageUrl.contains("supabase.co") || imageUrl.contains("supabase")) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val result = storageRepository.deleteCommentImage(imageUrl)
                result.onSuccess {
                    android.util.Log.d("CommsUniViewModel", "Successfully deleted draft image from Supabase: $imageUrl")
                }.onFailure { e ->
                    android.util.Log.e("CommsUniViewModel", "Failed to delete draft image from Supabase: $imageUrl", e)
                }
            }
        }
    }

    fun postComment(mediaId: String, mediaType: String, text: String, isSpoiler: Boolean, parentId: String?, parentUserId: String?, attachedMedia: List<String>) {}

    fun addComment(
        text: String,
        isSpoiler: Boolean,
        parentId: String?,
        parentUserId: String?,
        depth: Int,
        mediaTitle: String,
        mediaImage: String?,
        postToCommsUni: Boolean = true,
        attachedMedia: List<String> = emptyList()
    ) {
        if (isUserAnonymous) return  // Block anonymous writes silently
        val uid = currentUserId ?: return

        viewModelScope.launch {
            val (resolvedName, resolvedAvatar) = syncProfileIfNeeded()

            val optimisticAvatar = (resolvedAvatar ?: cachedLocalAvatar
                ?: auth.currentUser?.uid?.let { prefs.getString("user_avatar_$it", null) }
                ?: "").trim()

            // Defensive check: le risposte a commenti CommsUni devono essere inviate tassativamente su CommsUni
            val effectivePostToCommsUni = if (parentId != null) {
                val parentComment = _comments.value.find { it.id == parentId }
                if (parentComment?.isOnCommsUni == true) {
                    true
                } else if (parentComment != null && !parentComment.isOnCommsUni) {
                    false
                } else {
                    postToCommsUni
                }
            } else {
                postToCommsUni
            }

            val cleanText = text.trim()
            val commsUniAttachments = if (attachedMedia.isNotEmpty()) {
                attachedMedia.take(1).map { url ->
                    val provider = when {
                        url.contains("giphy.com", ignoreCase = true) -> "giphy"
                        url.contains("tenor.com", ignoreCase = true) -> "tenor"
                        url.contains("klipy.com", ignoreCase = true) -> "klipy"
                        else -> "flicktrove"
                    }
                    val pathWithoutQuery = url.substringBefore("?")
                    val contentType = when {
                        pathWithoutQuery.endsWith(".gif", ignoreCase = true) -> "image/gif"
                        pathWithoutQuery.endsWith(".png", ignoreCase = true) -> "image/png"
                        pathWithoutQuery.endsWith(".jpg", ignoreCase = true) ||
                            pathWithoutQuery.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                        pathWithoutQuery.endsWith(".webp", ignoreCase = true) -> "image/webp"
                        pathWithoutQuery.endsWith(".avif", ignoreCase = true) -> "image/avif"
                        provider == "giphy" || provider == "tenor" || provider == "klipy" -> "image/gif"
                        else -> "image/webp"
                    }
                    com.cinetrack.data.api.CommsUniWriteAttachment(
                        url = url,
                        contentType = contentType,
                        provider = provider
                    )
                }
            } else null

            val currentLanguage = translationManager.detectCommentLanguage(cleanText)
            val tempId = "temp_${System.currentTimeMillis()}"
            val optimistic = com.cinetrack.data.model.AppComment(
                id = tempId,
                mediaId = currentEntityId,
                mediaType = currentEntityType,
                userId = currentUserId ?: currentActorId ?: uid,
                userDisplayName = resolvedName,
                userAvatarUrl = optimisticAvatar,
                text = cleanText,
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
                nativeLikes = 0,
                attachedMedia = attachedMedia,
                language = currentLanguage,
                isOnCommsUni = effectivePostToCommsUni
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

            // Invio verso la destinazione selezionata (mutuamente esclusiva)
            val result = if (effectivePostToCommsUni && currentEntityId.isNotBlank()) {
                // Destinazione CommsUni: pubblica solo sulla rete CommsUni
                if (parentId != null) {
                    commsUniRepository.createReply(
                        commentId = parentId,
                        text = cleanText,
                        isSpoiler = isSpoiler,
                        language = currentLanguage,
                        attachments = commsUniAttachments
                    )
                } else {
                    commsUniRepository.createComment(
                        entityType = currentEntityType,
                        entityId = currentEntityId,
                        text = cleanText,
                        isSpoiler = isSpoiler,
                        language = currentLanguage,
                        title = currentTitle,
                        attachments = commsUniAttachments
                    )
                }
            } else if (!effectivePostToCommsUni && currentRawMediaId.isNotBlank()) {
                // Destinazione FlickTrove: salva solo su Firestore locale
                val generatedId = try {
                    commentRepository.addCommentAndGetId(
                        mediaId = currentRawMediaId,
                        mediaType = currentEntityType,
                        text = cleanText,
                        isSpoiler = isSpoiler,
                        parentId = parentId,
                        parentUserId = parentUserId,
                        depth = depth.coerceAtMost(1),
                        mediaTitle = "",
                        mediaImage = optimisticAvatar,
                        attachedMedia = attachedMedia,
                        language = currentLanguage
                    )
                } catch (e: Exception) {
                    null
                }

                if (generatedId != null) {
                    trackMyCommentId(generatedId)
                    Result.success(
                        CommsUniComment(
                            id = generatedId,
                            entityId = currentEntityId,
                            source = "flicktrove",
                            origin = CommsUniOrigin(kind = "native", slug = "flicktrove", displayName = "FlickTrove"),
                            text = cleanText,
                            createdAt = java.time.Instant.now().toString(),
                            userId = uid,
                            userName = resolvedName,
                            userAvatar = optimisticAvatar
                        )
                    )
                } else {
                    Result.failure(Exception("Impossibile salvare il commento su FlickTrove."))
                }
            } else {
                Result.failure(Exception("Impossibile inviare il commento."))
            }

            result.onSuccess { newComment ->
                newComment.userId?.let { recordAuthorId(it) }
                trackMyCommentId(newComment.id)
                badgeRepository.recordCommentWritten()
                // Replace temp with real comment
                val real = newComment.toAppComment(cachedSourcesMap)
                val finalReal = real.copy(
                    userAvatarUrl = if (real.userAvatarUrl.isBlank() && optimisticAvatar.isNotBlank()) optimisticAvatar else real.userAvatarUrl,
                    isOnCommsUni = effectivePostToCommsUni,
                    attachedMedia = if (real.attachedMedia.isNotEmpty()) real.attachedMedia else attachedMedia
                )
                val updated = _comments.value.toMutableList()
                val tempIndex = updated.indexOfFirst { it.id == tempId }
                if (tempIndex != -1) updated[tempIndex] = finalReal
                _comments.value = updated

                // Opzione 1: Mirroring su Firestore per tracciamento engagement admin
                if (effectivePostToCommsUni && currentRawMediaId.isNotBlank()) {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val mirrorComment = finalReal.copy(
                                mediaId = currentRawMediaId,
                                mediaType = currentEntityType,
                                originSlug = "flicktrove",
                                originName = "FlickTrove",
                                originColor = "#2dd4bf",
                                attachedMedia = if (finalReal.attachedMedia.isNotEmpty()) finalReal.attachedMedia else attachedMedia,
                                language = finalReal.language ?: currentLanguage,
                                isOnCommsUni = true
                            )
                            commentRepository.mirrorCommsUniComment(
                                comment = mirrorComment,
                                mediaTitle = if (mediaTitle.isNotBlank()) mediaTitle else (currentTitle ?: ""),
                                mediaImage = mediaImage
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("CommsUniViewModel", "Failed to mirror CommsUni comment to Firestore", e)
                        }
                    }
                }

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
        moderationDelegate.toggleSpoilerStatus(
            commentId = commentId,
            currentStatus = currentStatus,
            commentsState = _comments,
            currentRawMediaId = currentRawMediaId,
            scope = viewModelScope
        )
    }

    fun reportComment(
        commentId: String,
        category: String,
        text: String? = null,
        userId: String? = null,
        userDisplayName: String? = null,
        detail: String? = null
    ) {
        moderationDelegate.reportComment(
            commentId = commentId,
            category = category,
            text = text,
            userId = userId,
            userDisplayName = userDisplayName,
            detail = detail,
            commentsState = _comments,
            currentRawMediaId = currentRawMediaId,
            scope = viewModelScope
        )
    }

    fun blockAuthor(userId: String, authorName: String) {
        moderationDelegate.blockAuthor(
            userId = userId,
            authorName = authorName,
            commentsState = _comments,
            scope = viewModelScope
        )
    }

    fun unblockAuthor(userId: String) {
        moderationDelegate.unblockAuthor(
            userId = userId,
            scope = viewModelScope
        ) { refreshComments() }
    }

    fun emitBlockedLink(reasonResId: Int) {
        moderationDelegate.emitBlockedLink(reasonResId, viewModelScope)
    }

    fun deleteComment(commentId: String, targetComment: AppComment? = null) {
        moderationDelegate.deleteComment(
            commentId = commentId,
            targetComment = targetComment,
            commentsState = _comments,
            currentEntityId = currentEntityId,
            currentRawMediaId = currentRawMediaId,
            untrackMyCommentId = { untrackMyCommentId(it) },
            scope = viewModelScope
        )
    }

    fun toggleLikeComment(commentId: String, mediaTitle: String, mediaImage: String?) {
        reactionsDelegate.toggleLikeComment(
            commentId = commentId,
            mediaTitle = mediaTitle,
            mediaImage = mediaImage,
            commentsState = _comments,
            currentAuthorId = currentAuthorId,
            currentActorId = currentActorId,
            currentRawMediaId = currentRawMediaId,
            currentEntityType = currentEntityType,
            currentEntityId = currentEntityId,
            isCommentLikedByMe = { isCommentLikedByMe(it) },
            resolveFirebaseUidForAuthor = { resolveFirebaseUidForAuthor(it) },
            scope = viewModelScope
        )
    }

    fun translateComment(commentId: String, text: String, requireWifi: Boolean? = null) {
        translationDelegate.translateComment(
            commentId = commentId,
            text = text,
            requireWifi = requireWifi,
            scope = viewModelScope
        )
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
            target.rootCommentId ?: run {
                var curr: com.cinetrack.data.model.AppComment? = target
                while (curr != null && !curr.parentId.isNullOrBlank()) {
                    curr = currentComments.find { it.id == curr.parentId }
                }
                curr?.id ?: target.parentId
            }
        } else {
            commentId
        }
        val parentParam = if (isNested) commentId else null

        viewModelScope.launch {
            val currentList = _comments.value.toMutableList()
            val existingIds = currentList.map { it.id }.toSet()
            val newReplies = mutableListOf<com.cinetrack.data.model.AppComment>()

            // 1. Fetch Firestore replies if rawMediaId is present
            if (currentRawMediaId.isNotBlank()) {
                try {
                    val fReplies = commentRepository.getRepliesForComment(currentRawMediaId, commentId).map { reply ->
                        reply.copy(
                            parentId = commentId,
                            rootCommentId = rootId,
                            originSlug = if (reply.originSlug.isNullOrBlank()) "flicktrove" else reply.originSlug,
                            originName = if (reply.originName.isNullOrBlank()) "FlickTrove" else reply.originName,
                            originColor = if (reply.originColor.isNullOrBlank()) "#2dd4bf" else reply.originColor
                        )
                    }
                    newReplies.addAll(fReplies)
                } catch (e: Exception) {
                    android.util.Log.e("CommsUniViewModel", "Failed to load Firestore replies for $commentId", e)
                }
            }

            // 2. Fetch CommsUni replies if entityId is present
            if (currentEntityId.isNotBlank()) {
                val result = commsUniRepository.getReplies(
                    commentId = rootId,
                    sort = "most_recent",
                    limit = 50,
                    parent = parentParam,
                    source = currentSourceFilter,
                    language = currentLanguageFilter
                )
                
                result.onSuccess { response ->
                    val newMapped = extractAllComments(response.data.allItems, cachedSourcesMap)
                        .map { reply ->
                            val effectiveParentId = if (reply.parentId.isNullOrBlank()) commentId else reply.parentId
                            val effectiveRootId = if (reply.rootCommentId.isNullOrBlank()) rootId else reply.rootCommentId
                            reply.copy(parentId = effectiveParentId, rootCommentId = effectiveRootId)
                        }
                    newReplies.addAll(newMapped)
                }.onFailure { error ->
                    android.util.Log.e("CommsUniViewModel", "Failed to load CommsUni replies for $commentId (root: $rootId, parent: $parentParam)", error)
                }
            }

            if (newReplies.isNotEmpty()) {
                val toAdd = newReplies.distinctBy { it.id }.filterNot { existingIds.contains(it.id) }
                val fixedExisting = currentList.map { existing ->
                    val fixedReply = newReplies.find { it.id == existing.id }
                    if (fixedReply != null && existing.parentId.isNullOrBlank()) {
                        existing.copy(parentId = commentId, rootCommentId = rootId)
                    } else existing
                }.toMutableList()
                
                fixedExisting.addAll(toAdd)
                val currentUid = auth.currentUser?.uid
                _comments.value = commsUniRepository.applyLikeOverrides(fixedExisting.deduplicateComments(), currentUid)
                autoTranslateCommentsIfModelsDownloaded(toAdd)
            }
        }
    }

    private fun autoTranslateCommentsIfModelsDownloaded(comments: List<com.cinetrack.data.model.AppComment>) {
        // Disabled: Never auto-mangle comments into machine translations on initial load.
        // Translation is strictly on-demand per comment to prevent rate-limits and preserve authentic discussion.
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

    fun searchMediaTags(query: String) {
        mediaTagDelegate.searchMediaTags(query, viewModelScope)
    }

    fun clearMediaTagSuggestions() {
        mediaTagDelegate.clearMediaTagSuggestions()
    }

    override fun onCleared() {
        super.onCleared()
        mediaTagDelegate.cancel()
        translationDelegate.close()
    }
}
