package com.cinetrack.data.repository

import com.cinetrack.data.local.dao.BadgeDao
import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.local.dao.FolderDao
import com.cinetrack.data.local.dao.WatchHistoryDao
import com.cinetrack.data.local.entities.BadgeGlobalStatsEntity
import com.cinetrack.data.local.entities.UserBadgeEntity
import com.cinetrack.data.local.entities.UserPersonFrequencyEntity
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.badge.BadgeTypeCategory
import com.cinetrack.ui.components.badge.CustomBadgeIconKind
import com.cinetrack.ui.components.badge.PreviewBadgeItem
import com.cinetrack.ui.components.badge.PreviewBadgeTier
import com.cinetrack.ui.components.badge.ProgressiveTierDetail
import com.cinetrack.ui.components.badge.OFFICIAL_TROPHY_ROOM_CATALOG
import com.cinetrack.ui.components.badge.TrophyRoomItemUi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evento emesso quando l'utente sblocca un trofeo singolarmente oppure completa
 * il primo bootstrap retroattivo con un sommario celebrativo.
 */
sealed interface TrophyUnlockBannerEvent {
    data class SingleBadge(
        val badgeId: String,
        val title: String,
        val tier: PreviewBadgeTier,
        val category: BadgeTypeCategory,
        val iconKind: CustomBadgeIconKind,
        val milestonePhrase: String,
        val isFirstUnlock: Boolean
    ) : TrophyUnlockBannerEvent

    data class InitialBootstrapSummary(
        val unlockedCount: Int
    ) : TrophyUnlockBannerEvent
}

/**
 * Modello notifiche trofei per la schermata Updates (Notifiche / Aggiornamenti).
 */
sealed interface TrophyUpdateNotification {
    val key: String

    data class BootstrapSummary(
        val unlockedCount: Int
    ) : TrophyUpdateNotification {
        override val key: String get() = "trophy_bootstrap_summary"
    }

    data class SingleTrophy(
        val badgeId: String,
        val title: String,
        val tier: PreviewBadgeTier,
        val category: BadgeTypeCategory,
        val iconKind: CustomBadgeIconKind,
        val subtitle: String,
        val isFirstUnlock: Boolean
    ) : TrophyUpdateNotification {
        override val key: String get() = "trophy_single_$badgeId"
    }
}

typealias BadgeTierUnlockedEvent = TrophyUnlockBannerEvent.SingleBadge

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface BadgeRepositoryEntryPoint {
    fun badgeRepository(): BadgeRepository
}

@Singleton
class BadgeRepository @Inject constructor(
    private val badgeDao: BadgeDao,
    private val favoriteDao: FavoriteDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val folderDao: FolderDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val evaluationEngine: TrophyEvaluationEngine,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val badgePrefs by lazy {
        context.getSharedPreferences("flicktrove_badge_prefs", android.content.Context.MODE_PRIVATE)
    }

    private var supporterListenerRegistration: ListenerRegistration? = null
    private var creditSyncJob: Job? = null
    private var lastSyncedCreditState: Boolean? = null

    init {
        listenToSupporterStatus()
    }

    private fun listenToSupporterStatus() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            supporterListenerRegistration?.remove()
            supporterListenerRegistration = null

            if (user != null) {
                // Real-time listener on user doc
                supporterListenerRegistration = firestore.collection("users").document(user.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error == null && snapshot != null) {
                            val isUserDocSupporter = snapshot.exists() && (
                                snapshot.getBoolean("isSupporter") == true ||
                                snapshot.getBoolean("isExecutiveProducer") == true
                            )

                            if (isUserDocSupporter) {
                                if (!badgePrefs.getBoolean("is_supporter_verified", false)) {
                                    badgePrefs.edit()
                                        .putBoolean("is_supporter_verified", true)
                                        .putString("supporter_verified_uid", user.uid)
                                        .apply()
                                    evaluateBadgesAsync()
                                }
                            } else {
                                // Controlla se l'utente ha una donazione verificata manualmente con questo UID
                                val verifiedUid = badgePrefs.getString("supporter_verified_uid", null)
                                val hasManualClaim = verifiedUid == user.uid && badgePrefs.getBoolean("is_supporter_verified", false)

                                if (!hasManualClaim) {
                                    // Verifica nella collection /supporters/{email}
                                    val email = user.email
                                    if (!email.isNullOrBlank()) {
                                        firestore.collection("supporters").document(email.trim().lowercase())
                                            .get()
                                            .addOnSuccessListener { doc ->
                                                val isSupporterDoc = doc.exists() && (
                                                    doc.getBoolean("isSupporter") == true ||
                                                    doc.getBoolean("isExecutiveProducer") == true
                                                )
                                                if (isSupporterDoc) {
                                                    val claimed = doc.getBoolean("claimed") == true
                                                    val claimedByUid = doc.getString("claimedByUid")
                                                    if (!claimed || claimedByUid == user.uid) {
                                                        if (!claimed) {
                                                            doc.reference.update(
                                                                mapOf(
                                                                    "claimed" to true,
                                                                    "claimedByUid" to user.uid,
                                                                    "claimedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                                                )
                                                            )
                                                        }
                                                        if (!badgePrefs.getBoolean("is_supporter_verified", false)) {
                                                            badgePrefs.edit()
                                                                .putBoolean("is_supporter_verified", true)
                                                                .putString("supporter_verified_uid", user.uid)
                                                                .apply()
                                                            evaluateBadgesAsync()
                                                        }
                                                    }
                                                } else {
                                                    // Non è supporter né sul doc utente né nella collection supporters
                                                    if (badgePrefs.getBoolean("is_supporter_verified", false)) {
                                                        badgePrefs.edit()
                                                            .putBoolean("is_supporter_verified", false)
                                                            .remove("supporter_verified_uid")
                                                            .remove("supporter_verified_email")
                                                            .remove("supporter_credit_name")
                                                            .apply()
                                                        evaluateBadgesAsync()
                                                    }
                                                }
                                            }
                                    } else {
                                        // Utente anonimo o senza email e non supporter
                                        if (badgePrefs.getBoolean("is_supporter_verified", false)) {
                                            badgePrefs.edit()
                                                .putBoolean("is_supporter_verified", false)
                                                .remove("supporter_verified_uid")
                                                .remove("supporter_verified_email")
                                                .remove("supporter_credit_name")
                                                .apply()
                                            evaluateBadgesAsync()
                                        }
                                    }
                                }
                            }
                        }
                    }
            } else {
                // Logout: pulisci lo stato supporter volatile
                if (badgePrefs.getBoolean("is_supporter_verified", false)) {
                    badgePrefs.edit()
                        .putBoolean("is_supporter_verified", false)
                        .remove("supporter_verified_uid")
                        .remove("supporter_verified_email")
                        .remove("supporter_credit_name")
                        .apply()
                    evaluateBadgesAsync()
                }
            }
        }
    }

    private var isInitialEvaluationDone: Boolean
        get() = badgePrefs.getBoolean("is_initial_badge_evaluation_done", false)
        set(value) = badgePrefs.edit().putBoolean("is_initial_badge_evaluation_done", value).apply()

    private val _unlockedTierEvents = MutableSharedFlow<TrophyUnlockBannerEvent>(extraBufferCapacity = 16)
    val unlockedTierEvents: SharedFlow<TrophyUnlockBannerEvent> = _unlockedTierEvents.asSharedFlow()

    private val _trophyNotificationsTrigger = MutableStateFlow(0)

    private fun recordPendingTrophyNotification(badgeId: String) {
        val currentSet = badgePrefs.getStringSet("pending_trophy_badge_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (currentSet.add(badgeId)) {
            badgePrefs.edit().putStringSet("pending_trophy_badge_ids", currentSet).apply()
            _trophyNotificationsTrigger.value++
        }
    }

    fun dismissTrophyBootstrapNotification() {
        badgePrefs.edit()
            .putBoolean("is_trophy_bootstrap_acknowledged", true)
            .putStringSet("pending_trophy_badge_ids", emptySet())
            .apply()
        _trophyNotificationsTrigger.value++
    }

    fun dismissTrophyNotification(badgeId: String) {
        val currentSet = badgePrefs.getStringSet("pending_trophy_badge_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.remove(badgeId)
        badgePrefs.edit().putStringSet("pending_trophy_badge_ids", currentSet).apply()
        _trophyNotificationsTrigger.value++
    }

    fun dismissAllTrophyNotifications() {
        badgePrefs.edit()
            .putBoolean("is_trophy_bootstrap_acknowledged", true)
            .putStringSet("pending_trophy_badge_ids", emptySet())
            .apply()
        _trophyNotificationsTrigger.value++
    }

    suspend fun clearAllData() {
        badgeDao.clearUserBadges()
        badgeDao.clearPersonFrequencies()
        badgePrefs.edit()
            .remove("is_initial_badge_evaluation_done")
            .remove("is_trophy_bootstrap_acknowledged")
            .remove("pending_trophy_badge_ids")
            .remove("is_supporter_verified")
            .remove("supporter_verified_uid")
            .remove("supporter_verified_email")
            .remove("supporter_credit_name")
            .apply()
        _trophyNotificationsTrigger.value++
    }

    /**
     * Flusso reattivo delle notifiche trofeo per la schermata Updates (Opzione B):
     * 1. Bootstrap storico: 1 singola card riassuntiva ("X trofei sbloccati") se non ancora convalidata.
     * 2. Sblocchi in tempo reale successivi: card singola per ogni nuovo trofeo conquistato (post-bootstrap).
     */
    fun getTrophyNotificationsFlow(): Flow<List<TrophyUpdateNotification>> {
        return combine(
            badgeDao.getAllUserBadgesFlow(),
            _trophyNotificationsTrigger
        ) { userBadges, _ ->
            val result = mutableListOf<TrophyUpdateNotification>()
            val isBootstrapAcknowledged = badgePrefs.getBoolean("is_trophy_bootstrap_acknowledged", false)
            val unlockedBadges = userBadges.filter { it.isUnlocked }

            // 1. Bootstrap Summary: se non ancora confermato ed esistono badge conquistati, mostra SOLO l'aggregato
            if (!isBootstrapAcknowledged && unlockedBadges.isNotEmpty()) {
                result.add(TrophyUpdateNotification.BootstrapSummary(unlockedCount = unlockedBadges.size))
            } else {
                // 2. Notifiche singole per sblocchi successivi in tempo reale (solo dopo che il bootstrap è stato confermato)
                val pendingIds = badgePrefs.getStringSet("pending_trophy_badge_ids", emptySet()) ?: emptySet()
                if (pendingIds.isNotEmpty()) {
                    val userBadgeMap = userBadges.associateBy { it.badgeId }
                    for (id in pendingIds) {
                        val catalogItem = OFFICIAL_TROPHY_ROOM_CATALOG.firstOrNull { it.badge.id == id } ?: continue
                        val badge = catalogItem.badge
                        val userEntity = userBadgeMap[id]
                        if (userEntity?.isUnlocked != true) continue
                        val tier = evaluationEngine.parseTierSafe(userEntity.currentTier, badge.fixedTier ?: badge.initialProgressiveTier)
                        val stepPhrase = badge.progressiveSteps[tier]?.milestonePhrase ?: badge.defaultDescription

                        result.add(
                            TrophyUpdateNotification.SingleTrophy(
                                badgeId = badge.id,
                                title = badge.title,
                                tier = tier,
                                category = badge.category,
                                iconKind = badge.iconKind,
                                subtitle = stepPhrase,
                                isFirstUnlock = userEntity.currentTier == badge.initialProgressiveTier.name
                            )
                        )
                    }
                }
            }

            result
        }.flowOn(Dispatchers.Default)
    }

    fun triggerTestUnlockBanner(
        badgeId: String = "badge_movies",
        tier: PreviewBadgeTier = PreviewBadgeTier.MM_70
    ) {
        val catalogItem = OFFICIAL_TROPHY_ROOM_CATALOG.firstOrNull { it.badge.id == badgeId }
        val badge = catalogItem?.badge
        val stepPhrase = badge?.progressiveSteps?.get(tier)?.milestonePhrase
            ?: "Hai sbloccato il formato ${tier.formatName}!"
        val event = TrophyUnlockBannerEvent.SingleBadge(
            badgeId = badgeId,
            title = badge?.title ?: "Frequenza 24fps",
            tier = tier,
            category = badge?.category ?: BadgeTypeCategory.PROGRESSIVE_TIERS,
            iconKind = badge?.iconKind ?: CustomBadgeIconKind.CINEMA_REEL,
            milestonePhrase = stepPhrase,
            isFirstUnlock = false
        )
        _unlockedTierEvents.tryEmit(event)
        recordPendingTrophyNotification(badgeId)
    }

    fun recordSurpriseMeDiscovered(movieId: Long) {
        val currentSet = badgePrefs.getStringSet("surprise_me_discovered_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (currentSet.add(movieId.toString())) {
            badgePrefs.edit().putStringSet("surprise_me_discovered_ids", currentSet).apply()
            evaluateBadgesAsync()
        }
    }

    fun recordRatingChanged(movieId: Long) {
        val key = "rating_change_count_$movieId"
        val newCount = badgePrefs.getInt(key, 0) + 1
        val currentMax = badgePrefs.getInt("max_rating_changes_on_single_movie", 0)
        val editor = badgePrefs.edit().putInt(key, newCount)
        if (newCount > currentMax) {
            editor.putInt("max_rating_changes_on_single_movie", newCount)
        }
        editor.apply()
        evaluateBadgesAsync()
    }

    fun recordCompletedCollection(collectionId: String) {
        if (collectionId.isBlank()) return
        val currentSet = badgePrefs.getStringSet("completed_collection_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (currentSet.add(collectionId)) {
            badgePrefs.edit().putStringSet("completed_collection_ids", currentSet).apply()
            evaluateBadgesAsync()
        }
    }

    fun removeCompletedCollection(collectionId: String) {
        if (collectionId.isBlank()) return
        val currentSet = badgePrefs.getStringSet("completed_collection_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (currentSet.remove(collectionId)) {
            badgePrefs.edit().putStringSet("completed_collection_ids", currentSet).apply()
            evaluateBadgesAsync()
        }
    }

    private val commentCountMutex = Mutex()

    fun recordCommentWritten() {
        updateCommentWrittenCount(delta = 1)
    }

    fun recordCommentDeleted() {
        updateCommentWrittenCount(delta = -1)
    }

    private fun updateCommentWrittenCount(delta: Int) {
        repositoryScope.launch {
            commentCountMutex.withLock {
                val baseCount = if (badgePrefs.contains("comments_written_count")) {
                    badgePrefs.getInt("comments_written_count", 0)
                } else {
                    badgeDao.getBadgeById("badge_comments")?.progressCurrent ?: 0
                }
                val newCount = (baseCount + delta).coerceAtLeast(0)
                badgePrefs.edit().putInt("comments_written_count", newCount).apply()
            }
            try {
                evaluateAllBadges()
                syncPendingBadgesToFirestore()
                fetchGlobalRarityStats()
            } catch (e: Exception) {
                android.util.Log.e("BadgeRepository", "Error evaluating badges asynchronously", e)
            }
        }
    }

    fun recordBackupRestored() {
        badgePrefs.edit().putBoolean("has_imported_backup", true).apply()
        evaluateBadgesAsync()
    }

    fun recordSupporterVerified() {
        badgePrefs.edit().putBoolean("is_supporter_verified", true).apply()
        evaluateBadgesAsync()
    }

    enum class SupporterVerificationResult {
        SUCCESS,
        NOT_FOUND,
        ALREADY_CLAIMED,
        INVALID_EMAIL,
        ERROR
    }

    /**
     * Verifica on-demand lo stato sostenitore tramite email (utile per Guest o donazioni
     * effettuate con indirizzi PayPal/Ko-fi differenti rispetto al login).
     *
     * Anti-Abuse (Soluzione A - Riscatto Univoco):
     * La donazione viene legata in modo univoco all'UID dell'account richiedente (registrato o guest).
     * Se la donazione risulta già riscattata da un altro UID, il tentativo viene bloccato (ALREADY_CLAIMED),
     * a meno che l'utente non sia autenticato via Google con quello stesso identico indirizzo email.
     */
    suspend fun verifySupporterByEmail(email: String): SupporterVerificationResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return@withContext SupporterVerificationResult.INVALID_EMAIL
        }

        try {
            var user = auth.currentUser
            if (user == null) {
                try {
                    user = auth.signInAnonymously().await().user
                } catch (e: Exception) {
                    android.util.Log.w("BadgeRepository", "Anonymous sign-in for supporter verification: ${e.message}")
                }
            }

            val supporterRef = firestore.collection("supporters").document(cleanEmail)
            val currentUid = user?.uid
            val isVerifiedEmailOwner = user != null && !user.isAnonymous &&
                user.email?.trim()?.equals(cleanEmail, ignoreCase = true) == true

            val snapshot = try {
                supporterRef.get().await()
            } catch (e: Exception) {
                android.util.Log.e("BadgeRepository", "Failed to fetch supporter doc: ${e.message}")
                null
            }

            var isSupporter = false
            var supporterDisplayName: String? = null
            if (snapshot != null && snapshot.exists()) {
                isSupporter = snapshot.getBoolean("isSupporter") == true ||
                    snapshot.getBoolean("isExecutiveProducer") == true

                supporterDisplayName = snapshot.getString("displayName")?.takeIf { !it.isNullOrBlank() }
                    ?: snapshot.getString("creditName")?.takeIf { !it.isNullOrBlank() }

                val claimed = snapshot.getBoolean("claimed") == true
                val claimedByUid = snapshot.getString("claimedByUid")

                // Se già riscattato da un altro account e l'utente non è il legittimo proprietario verificato dell'email
                if (claimed && !claimedByUid.isNullOrBlank() && claimedByUid != currentUid && !isVerifiedEmailOwner) {
                    return@withContext SupporterVerificationResult.ALREADY_CLAIMED
                }

                // Prova ad aggiornare claimedByUid se consentito (silenzioso se non admin)
                try {
                    val updates = mutableMapOf<String, Any>(
                        "claimed" to true,
                        "claimedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    if (!currentUid.isNullOrBlank()) {
                        updates["claimedByUid"] = currentUid
                    }
                    supporterRef.update(updates).await()
                } catch (_: Exception) {
                    // Non fallisce la verifica se le security rules impediscono la scrittura a non-admin
                }
            } else if (user != null && !user.isAnonymous) {
                // Fallback: controlla anche il profilo utente corrente se registrato
                try {
                    val userDoc = firestore.collection("users").document(user.uid).get().await()
                    if (userDoc.exists() && (userDoc.getBoolean("isSupporter") == true || userDoc.getBoolean("isExecutiveProducer") == true)) {
                        isSupporter = true
                        supporterDisplayName = userDoc.getString("displayName") ?: userDoc.getString("creditName")
                    }
                } catch (_: Exception) {}
            }

            if (isSupporter) {
                badgePrefs.edit()
                    .putBoolean("is_supporter_verified", true)
                    .putString("supporter_verified_email", cleanEmail)
                    .putString("supporter_verified_uid", currentUid)
                    .apply()

                // Se l'utente ha un nome utente o il mecenate ha un displayName, inseriscilo in automatico nei titoli di coda
                val rawSupporterName = when {
                    !auth.currentUser?.displayName.isNullOrBlank() && auth.currentUser?.displayName?.contains("@") == false -> auth.currentUser?.displayName?.trim()
                    !supporterDisplayName.isNullOrBlank() && !supporterDisplayName.contains("@") -> supporterDisplayName.trim()
                    else -> null
                }

                if (!rawSupporterName.isNullOrBlank()) {
                    val formattedName = rawSupporterName.split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { part ->
                            part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        }

                    badgePrefs.edit()
                        .putString("supporter_credit_name", formattedName)
                        .putBoolean("supporter_show_in_credits", true)
                        .apply()

                    val currentCached = badgePrefs.getStringSet("cached_credits_supporters", emptySet())?.toMutableSet() ?: mutableSetOf()
                    currentCached.add(formattedName)
                    badgePrefs.edit().putStringSet("cached_credits_supporters", currentCached).apply()

                    try {
                        firestore.collection("system_metrics").document("credits_producers")
                            .update("producers", com.google.firebase.firestore.FieldValue.arrayUnion(formattedName))
                            .await()
                    } catch (_: Exception) {
                        try {
                            firestore.collection("system_metrics").document("credits_producers")
                                .set(
                                    mapOf("producers" to com.google.firebase.firestore.FieldValue.arrayUnion(formattedName)),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                                .await()
                        } catch (_: Exception) {}
                    }
                }

                if (user != null && !user.isAnonymous) {
                    try {
                        val userUpdates = mutableMapOf<String, Any>("isSupporter" to true)
                        if (!rawSupporterName.isNullOrBlank()) {
                            userUpdates["creditName"] = rawSupporterName
                            userUpdates["showInCredits"] = true
                        }
                        firestore.collection("users").document(user.uid)
                            .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                            .await()
                    } catch (_: Exception) {}
                }
                evaluateBadgesAsync()
                SupporterVerificationResult.SUCCESS
            } else {
                SupporterVerificationResult.NOT_FOUND
            }
        } catch (e: Exception) {
            android.util.Log.e("BadgeRepository", "Error verifying supporter email: $cleanEmail", e)
            SupporterVerificationResult.ERROR
        }
    }

    fun isSupporterVerified(): Boolean {
        return badgePrefs.getBoolean("is_supporter_verified", false)
    }

    fun getSupporterCreditSettings(): Pair<String, Boolean> {
        val name = badgePrefs.getString("supporter_credit_name", "") ?: ""
        val show = badgePrefs.getBoolean("supporter_show_in_credits", true)
        return Pair(name, show)
    }

    suspend fun updateSupporterCreditSettings(creditName: String, showInCredits: Boolean): Boolean = withContext(Dispatchers.IO) {
        val cleanName = creditName.trim().take(30)
        badgePrefs.edit()
            .putString("supporter_credit_name", cleanName)
            .putBoolean("supporter_show_in_credits", showInCredits)
            .apply()

        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            try {
                firestore.collection("users").document(user.uid)
                    .update(
                        mapOf(
                            "creditName" to cleanName,
                            "showInCredits" to showInCredits
                        )
                    ).await()
            } catch (e: Exception) {
                android.util.Log.w("BadgeRepository", "Failed to update credit name in users collection: ${e.message}")
            }
        }
        true
    }

    fun isSupporterCreditEnabled(): Boolean {
        return badgePrefs.getBoolean("supporter_show_in_credits", true)
    }

    fun setSupporterCreditEnabled(enabled: Boolean) {
        badgePrefs.edit().putBoolean("supporter_show_in_credits", enabled).apply()
        val name = badgePrefs.getString("supporter_credit_name", null)?.takeIf { it.isNotBlank() }
        if (!name.isNullOrBlank()) {
            val cached = badgePrefs.getStringSet("cached_credits_supporters", emptySet())?.toMutableSet() ?: mutableSetOf()
            if (enabled) {
                cached.add(name)
            } else {
                cached.remove(name)
            }
            badgePrefs.edit().putStringSet("cached_credits_supporters", cached).apply()
        }

        // Zero-Waste Firebase Spark: Debounce di 1200ms per proteggere la quota da click a raffica.
        // Se l'utente clicca lo switch 100 volte, le prime 99 chiamate vengono annullate e parte solo l'ultima stabilizzata.
        creditSyncJob?.cancel()
        creditSyncJob = repositoryScope.launch(Dispatchers.IO) {
            delay(1200L)
            if (name.isNullOrBlank()) return@launch
            if (lastSyncedCreditState == enabled) return@launch

            try {
                if (enabled) {
                    try {
                        firestore.collection("system_metrics").document("credits_producers")
                            .update("producers", com.google.firebase.firestore.FieldValue.arrayUnion(name))
                            .await()
                    } catch (_: Exception) {
                        try {
                            firestore.collection("system_metrics").document("credits_producers")
                                .set(
                                    mapOf("producers" to com.google.firebase.firestore.FieldValue.arrayUnion(name)),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                                .await()
                        } catch (_: Exception) {}
                    }
                } else {
                    firestore.collection("system_metrics").document("credits_producers")
                        .update("producers", com.google.firebase.firestore.FieldValue.arrayRemove(name))
                        .await()
                }
                lastSyncedCreditState = enabled
            } catch (e: Exception) {
                android.util.Log.d("BadgeRepository", "Debounced credits sync error: ${e.message}")
            }

            val user = auth.currentUser
            if (user != null && !user.isAnonymous) {
                try {
                    firestore.collection("users").document(user.uid)
                        .update("showInCredits", enabled)
                        .await()
                } catch (_: Exception) {}
            }
        }
    }

    fun getCachedPublicCreditsSupporters(): List<String> {
        val cached = badgePrefs.getStringSet("cached_credits_supporters", emptySet())?.toMutableSet() ?: mutableSetOf()
        val myName = badgePrefs.getString("supporter_credit_name", null)?.takeIf { it.isNotBlank() }
        if (isSupporterVerified() && isSupporterCreditEnabled()) {
            if (!myName.isNullOrBlank()) {
                cached.add(myName)
            }
        } else if (!isSupporterCreditEnabled() && !myName.isNullOrBlank()) {
            cached.remove(myName)
        }
        return cached.toList()
    }

    suspend fun fetchPublicCreditsSupporters(forceRefresh: Boolean = false): List<String> = withContext(Dispatchers.IO) {
        val lastFetch = badgePrefs.getLong("credits_last_fetch_time", 0L)
        val now = System.currentTimeMillis()
        if (!forceRefresh && now - lastFetch < 5 * 60 * 1000L) {
            val cached = badgePrefs.getStringSet("cached_credits_supporters", null)
            if (!cached.isNullOrEmpty()) return@withContext cached.toList()
        }

        try {
            // Risolvi automaticamente il nome del sostenitore se verificato
            var mySupporterName = badgePrefs.getString("supporter_credit_name", null)?.takeIf { it.isNotBlank() }
            if (isSupporterVerified()) {
                if (mySupporterName.isNullOrBlank()) {
                    val authName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() && !it.contains("@") }
                    if (!authName.isNullOrBlank()) {
                        mySupporterName = authName
                    } else {
                        val verifiedEmail = badgePrefs.getString("supporter_verified_email", null) ?: auth.currentUser?.email
                        if (!verifiedEmail.isNullOrBlank()) {
                            try {
                                val suppDoc = firestore.collection("supporters").document(verifiedEmail.trim().lowercase()).get().await()
                                val sName = suppDoc.getString("displayName")?.takeIf { it.isNotBlank() && !it.contains("@") }
                                    ?: suppDoc.getString("creditName")?.takeIf { it.isNotBlank() && !it.contains("@") }
                                if (!sName.isNullOrBlank()) {
                                    mySupporterName = sName
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    if (!mySupporterName.isNullOrBlank()) {
                        val formatted = mySupporterName.split(" ")
                            .filter { it.isNotBlank() }
                            .joinToString(" ") { part -> part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
                        mySupporterName = formatted
                        badgePrefs.edit().putString("supporter_credit_name", formatted).apply()
                    }
                }
            }

            val isCreditActive = isSupporterCreditEnabled()

            // 1. Single Source of Truth: system_metrics/credits_producers
            // Se esiste e contiene la lista "producers", usa prioritariamente questa lista curata (1 lettura Spark)
            try {
                val metricDoc = firestore.collection("system_metrics").document("credits_producers").get().await()
                if (metricDoc.exists()) {
                    val list = metricDoc.get("producers") as? List<*>
                    if (list != null) {
                        val names = list.mapNotNull { it?.toString()?.trim() }
                            .filter { it.isNotBlank() }
                            .toMutableList()

                        if (!mySupporterName.isNullOrBlank()) {
                            if (isCreditActive && isSupporterVerified()) {
                                if (!names.contains(mySupporterName)) {
                                    names.add(mySupporterName)
                                    try {
                                        firestore.collection("system_metrics").document("credits_producers")
                                            .update("producers", com.google.firebase.firestore.FieldValue.arrayUnion(mySupporterName))
                                    } catch (_: Exception) {}
                                }
                            } else {
                                names.remove(mySupporterName)
                            }
                        }

                        if (names.isNotEmpty()) {
                            val distinctNames = names.distinct()
                            badgePrefs.edit()
                                .putStringSet("cached_credits_supporters", distinctNames.toSet())
                                .putLong("credits_last_fetch_time", now)
                                .apply()
                            return@withContext distinctNames
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d("BadgeRepository", "credits_producers doc read failed: ${e.message}")
            }

            // 2. Fallback dinamico: se il documento centralizzato non esiste, interroga /supporters
            val collectedNames = mutableListOf<String>()
            if (isCreditActive && isSupporterVerified() && !mySupporterName.isNullOrBlank()) {
                collectedNames.add(mySupporterName)
            }
            try {
                if (auth.currentUser == null) {
                    try { auth.signInAnonymously().await() } catch (_: Exception) {}
                }
                val supportersSnap = firestore.collection("supporters").limit(100).get().await()
                supportersSnap.documents.forEach { doc ->
                    val isSupp = doc.getBoolean("isSupporter") == true || doc.getBoolean("isExecutiveProducer") == true
                    val email = doc.getString("email") ?: doc.id
                    // Esclude developer e non-supporter
                    if (isSupp && !email.contains("alessandrobasile909", ignoreCase = true)) {
                        val dName = doc.getString("displayName")?.trim()
                        val cName = doc.getString("creditName")?.trim()
                        val rawName = when {
                            !cName.isNullOrBlank() -> cName
                            !dName.isNullOrBlank() -> dName
                            else -> null
                        }
                        if (!rawName.isNullOrBlank() && !rawName.contains("@")) {
                            val formatted = rawName.split(" ")
                                .joinToString(" ") { part -> part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
                            collectedNames.add(formatted)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d("BadgeRepository", "Supporters collection read failed: ${e.message}")
            }

            // 3. Fallback secondario: utenti registrati con isSupporter == true
            try {
                val usersSnap = firestore.collection("users")
                    .whereEqualTo("isSupporter", true)
                    .limit(50)
                    .get()
                    .await()
                usersSnap.documents.forEach { doc ->
                    val dName = doc.getString("creditName") ?: doc.getString("displayName")
                    if (!dName.isNullOrBlank() && !dName.contains("@")) {
                        val formatted = dName.trim().split(" ")
                            .joinToString(" ") { part -> part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
                        collectedNames.add(formatted)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d("BadgeRepository", "Users collection read failed: ${e.message}")
            }

            val finalNames = collectedNames.filter { it.isNotBlank() }.distinct()

            if (finalNames.isNotEmpty()) {
                badgePrefs.edit()
                    .putStringSet("cached_credits_supporters", finalNames.toSet())
                    .putLong("credits_last_fetch_time", now)
                    .apply()
                return@withContext finalNames
            }

            badgePrefs.getStringSet("cached_credits_supporters", emptySet())?.toList().orEmpty()
        } catch (e: Exception) {
            android.util.Log.w("BadgeRepository", "Failed to fetch public credits: ${e.message}")
            badgePrefs.getStringSet("cached_credits_supporters", emptySet())?.toList().orEmpty()
        }
    }

    suspend fun onUserNameChanged(newDisplayName: String) = withContext(Dispatchers.IO) {
        if (!isSupporterVerified()) return@withContext
        val cleanName = newDisplayName.trim()
        if (cleanName.isBlank() || cleanName.contains("@")) return@withContext

        val formatted = cleanName.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

        val oldName = badgePrefs.getString("supporter_credit_name", null)
        badgePrefs.edit().putString("supporter_credit_name", formatted).apply()

        // Aggiorna cache locale
        val cached = badgePrefs.getStringSet("cached_credits_supporters", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (!oldName.isNullOrBlank()) cached.remove(oldName)
        cached.add(formatted)
        badgePrefs.edit().putStringSet("cached_credits_supporters", cached).apply()

        // Sincronizza su Firestore
        try {
            if (!oldName.isNullOrBlank()) {
                firestore.collection("system_metrics").document("credits_producers")
                    .update("producers", com.google.firebase.firestore.FieldValue.arrayRemove(oldName))
                    .await()
            }
            firestore.collection("system_metrics").document("credits_producers")
                .update("producers", com.google.firebase.firestore.FieldValue.arrayUnion(formatted))
                .await()
        } catch (_: Exception) {}
    }

    fun recordMovieBookmarked(movieId: Long) {
        badgePrefs.edit().putLong("bookmarked_at_$movieId", System.currentTimeMillis()).apply()
    }

    fun recordMovieWatched(movieId: Long) {
        val bookmarkedAt = badgePrefs.getLong("bookmarked_at_$movieId", 0L)
        if (bookmarkedAt > 0L) {
            val diffHours = (System.currentTimeMillis() - bookmarkedAt) / (1000 * 60 * 60)
            if (diffHours in 0..24) {
                badgePrefs.edit().putBoolean("unlocked_instant_watch", true).putLong("instant_watch_movie_id", movieId).apply()
            }
        }
        evaluateBadgesAsync()
    }

        /**
     * Risolve in modo deterministico e retroattivo il titolo e il poster dei media che hanno soddisfatto
     * i requisiti di sblocco per i trofei speciali, sessioni multiple (es. Doppio Spettacolo) e i Lost Reel cinefili.
     */
    suspend fun findUnlockingMoviesForBadge(badgeId: String): List<Movie> = withContext(Dispatchers.Default) {
        val allMovies = favoriteDao.getAll()
        val allHistory = watchHistoryDao.getAllWatchHistory()
        val userBadge = badgeDao.getBadgeById(badgeId)
        evaluationEngine.findUnlockingMoviesForBadge(
            badgeId = badgeId,
            allMovies = allMovies,
            allHistory = allHistory,
            userBadge = userBadge
        )
    }

    suspend fun findUnlockingMovieForBadge(badgeId: String): Movie? =
        findUnlockingMoviesForBadge(badgeId).firstOrNull()

    /**
     * Flusso reattivo principale per la Sala dei Trofei.
     * Fonde il catalogo ufficiale con:
     * 1. Lo stato di conquista personale salvato su Room (`user_badges`).
     * 2. Le percentuali di rarità globale salvate su Room (`badge_global_stats`).
     */
    fun getTrophyRoomItemsFlow(): Flow<List<TrophyRoomItemUi>> {
        return combine(
            badgeDao.getAllUserBadgesFlow(),
            badgeDao.getAllGlobalStatsFlow()
        ) { userBadges, globalStats ->
            val userBadgeMap = userBadges.associateBy { it.badgeId }
            val statsMap = globalStats.associateBy { it.badgeId }

            OFFICIAL_TROPHY_ROOM_CATALOG.map { catalogItem ->
                val baseBadge = catalogItem.badge
                val userEntity = userBadgeMap[baseBadge.id]
                val globalStat = statsMap[baseBadge.id]

                val dynamicRarity = globalStat?.let {
                    String.format(Locale.US, "%.1f%%", it.rarityPercent)
                } ?: baseBadge.rarityPercent

                if (userEntity != null) {
                    val activeTier = evaluationEngine.parseTierSafe(userEntity.currentTier, baseBadge.fixedTier ?: baseBadge.initialProgressiveTier)
                    val updatedBadge = baseBadge.copy(
                        progressCurrent = userEntity.progressCurrent,
                        progressTarget = userEntity.progressTarget,
                        rarityPercent = dynamicRarity
                    )
                    catalogItem.copy(
                        badge = updatedBadge,
                        currentTier = activeTier,
                        isUnlocked = userEntity.isUnlocked,
                        unlockedDate = userEntity.unlockedDate
                    )
                } else {
                    catalogItem.copy(
                        badge = baseBadge.copy(
                            progressCurrent = 0,
                            rarityPercent = dynamicRarity
                        ),
                        currentTier = baseBadge.fixedTier ?: baseBadge.initialProgressiveTier,
                        isUnlocked = false,
                        unlockedDate = null
                    )
                }
            }
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Valuta in background in modo asincrono tutti i badge senza bloccare il chiamante.
     */
    fun evaluateBadgesAsync() {
        repositoryScope.launch {
            try {
                evaluateAllBadges()
                syncPendingBadgesToFirestore()
                fetchGlobalRarityStats()
            } catch (e: Exception) {
                android.util.Log.e("BadgeRepository", "Error evaluating badges asynchronously", e)
            }
        }
    }

    private data class PendingUnlockData(
        val badge: PreviewBadgeItem,
        val tier: PreviewBadgeTier,
        val isFirstUnlock: Boolean
    )

    /**
     * Valuta l'intero catalogo dei badge locali alla luce dei dati presenti in Room:
     * - film e serie viste (`favorites`)
     * - rewatch e orari (`watch_history`)
     * - cartelle create (`folders`)
     *
     * Se rileva un avanzamento di tier o un primo sblocco, aggiorna SQLite ed emette
     * un evento [BadgeTierUnlockedEvent].
     */
    suspend fun evaluateAllBadges(): List<UserBadgeEntity> {
        val allMovies = favoriteDao.getAll()
        val allHistory = watchHistoryDao.getAllWatchHistory()
        val allFolders = folderDao.getAll()
        val existingBadges = badgeDao.getAllUserBadges().associateBy { it.badgeId }
        val prevUnlockedCount = existingBadges.values.count { it.isUnlocked }

        val isBootstrap = !isInitialEvaluationDone || existingBadges.isEmpty() || prevUnlockedCount == 0
        var bootstrapNewlyUnlockedCount = 0
        val pendingLiveEvents = mutableListOf<PendingUnlockData>()

        val watchedMovies = allMovies.filter { it.watched && it.mediaType != "tv" && !it.dropped }
        val watchedShows = allMovies.filter { it.watched && it.mediaType == "tv" && !it.dropped }
        val allWatched = allMovies.filter { it.watched && !it.dropped }

        // Aggiorna frequenze attori e registi per i badge #34 e #35
        val actorFrequencies: List<UserPersonFrequencyEntity> = allWatched.filter { it.favoriteActorId != null }.groupBy { it.favoriteActorId!! }.map { (actorId, list) ->
            UserPersonFrequencyEntity(
                personId = actorId,
                personName = list.firstNotNullOfOrNull { it.favoriteActorName } ?: "Attore",
                roleType = "ACTOR",
                watchedCount = list.size,
                lastWatchedAt = System.currentTimeMillis()
            )
        }
        val directorFrequencies: List<UserPersonFrequencyEntity> = allWatched.filter { it.directorId != null }.groupBy { it.directorId!! }.map { (dirId, list) ->
            UserPersonFrequencyEntity(
                personId = dirId,
                personName = list.firstNotNullOfOrNull { it.directorName } ?: "Regista",
                roleType = "DIRECTOR",
                watchedCount = list.size,
                lastWatchedAt = System.currentTimeMillis()
            )
        }
        if (actorFrequencies.isNotEmpty() || directorFrequencies.isNotEmpty()) {
            badgeDao.upsertPersonFrequencies(actorFrequencies + directorFrequencies)
        }

        val evaluatedEntities = mutableListOf<UserBadgeEntity>()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        for (catalogItem in OFFICIAL_TROPHY_ROOM_CATALOG.filter { it.badge.id != "badge_trophy_hunter" }) {
            val badge = catalogItem.badge
            val prevEntity = existingBadges[badge.id]

            val (calculatedProgress, targetThreshold, calculatedTier, isUnlocked) = evaluationEngine.evaluateBadgeLogic(
                badge = badge,
                allMovies = allMovies,
                watchedMovies = watchedMovies,
                watchedShows = watchedShows,
                allWatched = allWatched,
                allHistory = allHistory,
                allFolders = allFolders,
                prevEntity = prevEntity
            )

            val newlyUnlocked = isUnlocked && (prevEntity == null || !prevEntity.isUnlocked)
            val tierUpgraded = prevEntity != null && prevEntity.isUnlocked && evaluationEngine.isHigherTier(calculatedTier, evaluationEngine.parseTierSafe(prevEntity.currentTier, PreviewBadgeTier.SUPER_8))

            val unlockedDate = when {
                !isUnlocked -> null
                prevEntity?.unlockedDate != null -> prevEntity.unlockedDate
                else -> todayStr
            }

            val entity = UserBadgeEntity(
                badgeId = badge.id,
                currentTier = calculatedTier.name,
                category = badge.category.name,
                progressCurrent = calculatedProgress,
                progressTarget = targetThreshold,
                isUnlocked = isUnlocked,
                unlockedDate = unlockedDate,
                isRevealed = isUnlocked || (prevEntity?.isRevealed ?: false),
                syncStatus = if (prevEntity?.progressCurrent != calculatedProgress || newlyUnlocked || tierUpgraded) "pending" else "synced",
                lastUpdatedAt = System.currentTimeMillis()
            )
            evaluatedEntities.add(entity)

            if (newlyUnlocked) {
                bootstrapNewlyUnlockedCount++
            }

            if (newlyUnlocked || tierUpgraded) {
                pendingLiveEvents.add(
                    PendingUnlockData(badge, calculatedTier, newlyUnlocked)
                )
            }
        }

        // Valutazione dinamica del meta-trofeo "Il Cacciatore di Trofei": avanza con lo sblocco di tutti gli altri trofei
        val trophyHunterCatalogItem = OFFICIAL_TROPHY_ROOM_CATALOG.firstOrNull { it.badge.id == "badge_trophy_hunter" }
        if (trophyHunterCatalogItem != null) {
            val badge = trophyHunterCatalogItem.badge
            val prevEntity = existingBadges[badge.id]
            val otherUnlockedCount = evaluatedEntities.count { it.isUnlocked }

            val (calculatedProgress, targetThreshold, calculatedTier, isUnlocked) = evaluationEngine.calculateProgressiveTiers(
                otherUnlockedCount,
                badge.progressiveSteps
            )

            val newlyUnlocked = isUnlocked && (prevEntity == null || !prevEntity.isUnlocked)
            val tierUpgraded = prevEntity != null && prevEntity.isUnlocked && evaluationEngine.isHigherTier(calculatedTier, evaluationEngine.parseTierSafe(prevEntity.currentTier, PreviewBadgeTier.SUPER_8))

            val unlockedDate = when {
                !isUnlocked -> null
                prevEntity?.unlockedDate != null -> prevEntity.unlockedDate
                else -> todayStr
            }

            val entity = UserBadgeEntity(
                badgeId = badge.id,
                currentTier = calculatedTier.name,
                category = badge.category.name,
                progressCurrent = calculatedProgress,
                progressTarget = targetThreshold,
                isUnlocked = isUnlocked,
                unlockedDate = unlockedDate,
                isRevealed = isUnlocked || (prevEntity?.isRevealed ?: false),
                syncStatus = if (prevEntity?.progressCurrent != calculatedProgress || newlyUnlocked || tierUpgraded) "pending" else "synced",
                lastUpdatedAt = System.currentTimeMillis()
            )
            evaluatedEntities.add(entity)

            if (newlyUnlocked) {
                bootstrapNewlyUnlockedCount++
            }

            if (newlyUnlocked || tierUpgraded) {
                pendingLiveEvents.add(
                    PendingUnlockData(badge, calculatedTier, newlyUnlocked)
                )
            }
        }

        badgeDao.upsertBadges(evaluatedEntities)

        val totalUnlockedCount = evaluatedEntities.count { it.isUnlocked }
        val isBulkBootstrap = isBootstrap || pendingLiveEvents.size >= 3

        if (isBulkBootstrap) {
            isInitialEvaluationDone = true
            // In caso di bootstrap o import massivo, pulisci notifiche singole sporche e mantieni solo l'aggregato
            badgePrefs.edit().putStringSet("pending_trophy_badge_ids", emptySet()).apply()

            if (totalUnlockedCount > 0 && !badgePrefs.getBoolean("is_trophy_bootstrap_acknowledged", false)) {
                _unlockedTierEvents.tryEmit(
                    TrophyUnlockBannerEvent.InitialBootstrapSummary(
                        unlockedCount = totalUnlockedCount
                    )
                )
            }
            _trophyNotificationsTrigger.value++
        } else {
            // Sblocchi live reali (1 o 2 trofei in tempo reale)
            for (item in pendingLiveEvents) {
                val stepPhrase = item.badge.progressiveSteps[item.tier]?.milestonePhrase
                    ?: item.badge.title
                _unlockedTierEvents.tryEmit(
                    TrophyUnlockBannerEvent.SingleBadge(
                        badgeId = item.badge.id,
                        title = item.badge.title,
                        tier = item.tier,
                        category = item.badge.category,
                        iconKind = item.badge.iconKind,
                        milestonePhrase = stepPhrase,
                        isFirstUnlock = item.isFirstUnlock
                    )
                )
                recordPendingTrophyNotification(item.badge.id)
            }
        }

        return evaluatedEntities
    }

    /**
     * Sincronizza i nuovi badge sbloccati in cloud:
     * 1. Salva la copia sul profilo personale dell'utente in `/users/{uid}/unlocked_badges/{badgeId}`
     * 2. Incrementa in modo atomico il contatore della community in `/system_metrics/badges_global`
     * 3. Aggiorna lo stato Room locale a 'synced'
     */
    suspend fun syncPendingBadgesToFirestore(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.isAnonymous) return false // Solo gli utenti registrati contribuiscono alle statistiche globali!
        val pendingBadges = badgeDao.getPendingSyncBadges().filter { it.isUnlocked }
        if (pendingBadges.isEmpty()) return true

        return try {
            val docRef = firestore.collection("system_metrics").document("badges_global")
            val userBadgesCollection = firestore.collection("users").document(user.uid).collection("unlocked_badges")

            // Verifica l'esistenza remota leggendo puntualmente SOLO i documenti dei badge pendenti (1-2 letture anziché 52 full-fetch)
            val existingBadgeIds = mutableSetOf<String>()
            for (b in pendingBadges) {
                try {
                    val snap = userBadgesCollection.document(b.badgeId).get().await()
                    if (snap.exists()) {
                        existingBadgeIds.add(b.badgeId)
                    }
                } catch (_: Exception) {}
            }

            val actuallyNewBadges = pendingBadges.filter { it.badgeId !in existingBadgeIds }

            if (actuallyNewBadges.isNotEmpty()) {
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    if (!snapshot.exists()) return@runTransaction

                    val totalUsers = snapshot.getLong("total_active_users") ?: 154L
                    @Suppress("UNCHECKED_CAST")
                    val countsMap = (snapshot.get("unlock_counts") as? Map<String, Any>)?.toMutableMap() ?: mutableMapOf()
                    @Suppress("UNCHECKED_CAST")
                    val raritiesMap = (snapshot.get("rarities") as? Map<String, Any>)?.toMutableMap() ?: mutableMapOf()

                    for (b in actuallyNewBadges) {
                        val currentCount = (countsMap[b.badgeId] as? Number)?.toLong() ?: 0L
                        val newCount = currentCount + 1L
                        countsMap[b.badgeId] = newCount
                        val newPct = ((newCount.toDouble() / totalUsers) * 100.0).coerceIn(0.1, 100.0)
                        raritiesMap[b.badgeId] = String.format(Locale.US, "%.1f", newPct).toDoubleOrNull() ?: newPct
                    }

                    transaction.update(docRef, mapOf(
                        "unlock_counts" to countsMap,
                        "rarities" to raritiesMap
                    ))
                }.await()
            }

            val batch = firestore.batch()
            for (b in pendingBadges) {
                val badgeDoc = userBadgesCollection.document(b.badgeId)
                batch.set(badgeDoc, mapOf(
                    "badgeId" to b.badgeId,
                    "tier" to b.currentTier,
                    "unlockedDate" to (b.unlockedDate ?: ""),
                    "syncedAt" to System.currentTimeMillis()
                ))
            }
            batch.commit().await()

            badgeDao.markBadgesAsSynced(pendingBadges.map { it.badgeId })
            android.util.Log.d("BadgeRepository", "syncPendingBadgesToFirestore: synced ${pendingBadges.size} badges (new increments: ${actuallyNewBadges.size})")

            if (actuallyNewBadges.isNotEmpty()) {
                fetchGlobalRarityStats(forceRefresh = true)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("BadgeRepository", "syncPendingBadgesToFirestore failed", e)
            false
        }
    }

    /**
     * Sincronizza le statistiche globali di rarità leggendo 1 singola volta
     * il documento aggregato `/system_metrics/badges_global` da Firestore.
     */
    suspend fun fetchGlobalRarityStats(forceRefresh: Boolean = false): Boolean {
        if (!forceRefresh) {
            val lastCached = badgeDao.getLatestGlobalStatsTimestamp() ?: 0L
            val ttlMillis = 24 * 60 * 60 * 1000L // 24 ore di cache locale
            val cachedCount = badgeDao.getGlobalStatsCount()
            if (System.currentTimeMillis() - lastCached < ttlMillis && cachedCount >= OFFICIAL_TROPHY_ROOM_CATALOG.size) {
                return true // Dati già freschi in Room e catalogo completo: zero chiamate di rete!
            }
        }
        return try {
            val doc = firestore.collection("system_metrics").document("badges_global").get().await()
            if (!doc.exists()) {
                android.util.Log.w("BadgeRepository", "badges_global document does not exist in Firestore")
                return false
            }

            val totalUsers = (doc.getLong("total_active_users") ?: 154L).coerceAtLeast(1L)
            @Suppress("UNCHECKED_CAST")
            val rarities = doc.get("rarities") as? Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val counts = doc.get("unlock_counts") as? Map<String, Any>

            if (rarities == null && counts == null) return false

            val allBadgeIds = (counts?.keys ?: emptySet()) + (rarities?.keys ?: emptySet())
            val statsEntities = allBadgeIds.mapNotNull { badgeId ->
                val countNumber = counts?.get(badgeId) as? Number
                val percentFloat = if (countNumber != null) {
                    ((countNumber.toFloat() / totalUsers) * 100f).coerceIn(0.1f, 100.0f)
                } else {
                    val rawPercent = rarities?.get(badgeId)
                    when (rawPercent) {
                        is Number -> rawPercent.toFloat()
                        is String -> rawPercent.replace("%", "").toFloatOrNull()
                        else -> null
                    }
                } ?: return@mapNotNull null

                BadgeGlobalStatsEntity(
                    badgeId = badgeId,
                    rarityPercent = percentFloat.coerceIn(0.1f, 100.0f),
                    totalUsersCount = totalUsers,
                    cachedAt = System.currentTimeMillis()
                )
            }

            if (statsEntities.isNotEmpty()) {
                badgeDao.upsertGlobalStats(statsEntities)
                android.util.Log.d("BadgeRepository", "fetchGlobalRarityStats: successfully saved ${statsEntities.size} global rarity stats (totalUsers=$totalUsers)")
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("BadgeRepository", "fetchGlobalRarityStats failed", e)
            false
        }
    }
}
