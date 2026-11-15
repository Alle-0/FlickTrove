package com.cinetrack.data.repository

import android.content.Context
import com.cinetrack.data.repository.BadgeRepository.SupporterVerificationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SupporterRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val badgePrefs by lazy {
        context.getSharedPreferences("flicktrove_badge_prefs", Context.MODE_PRIVATE)
    }

    private var supporterListenerRegistration: ListenerRegistration? = null
    private var creditSyncJob: Job? = null
    private var lastSyncedCreditState: Boolean? = null

    /**
     * Ascolta in tempo reale lo stato sostenitore dell'utente corrente e notifica
     * tramite callback quando lo stato cambia (es. per triggerare la valutazione dei trofei).
     */
    fun listenToSupporterStatus(onStatusChanged: () -> Unit) {
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
                                    onStatusChanged()
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
                                                            onStatusChanged()
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
                                                        onStatusChanged()
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
                                            onStatusChanged()
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
                    onStatusChanged()
                }
            }
        }
    }

    fun recordSupporterVerified() {
        badgePrefs.edit().putBoolean("is_supporter_verified", true).apply()
    }

    /**
     * Verifica on-demand lo stato sostenitore tramite email (utile per Guest o donazioni
     * effettuate con indirizzi PayPal/Ko-fi differenti rispetto al login).
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
                    android.util.Log.w("SupporterRepository", "Anonymous sign-in for supporter verification: ${e.message}")
                }
            }

            val supporterRef = firestore.collection("supporters").document(cleanEmail)
            val currentUid = user?.uid
            val isVerifiedEmailOwner = user != null && !user.isAnonymous &&
                user.email?.trim()?.equals(cleanEmail, ignoreCase = true) == true

            val snapshot = try {
                supporterRef.get().await()
            } catch (e: Exception) {
                android.util.Log.e("SupporterRepository", "Failed to fetch supporter doc: ${e.message}")
                null
            }

            var isSupporter = false
            var supporterDisplayName: String? = null
            if (snapshot != null && snapshot.exists()) {
                isSupporter = snapshot.getBoolean("isSupporter") == true ||
                    snapshot.getBoolean("isExecutiveProducer") == true

                supporterDisplayName = snapshot.getString("displayName")?.takeIf { it.isNotBlank() && !it.contains("@") }
                    ?: snapshot.getString("creditName")?.takeIf { it.isNotBlank() && !it.contains("@") }
            }

            if (!isSupporter) {
                try {
                    val query = firestore.collection("supporters")
                        .whereEqualTo("email", cleanEmail)
                        .limit(1)
                        .get()
                        .await()

                    if (!query.isEmpty) {
                        val doc = query.documents.first()
                        isSupporter = doc.getBoolean("isSupporter") == true ||
                            doc.getBoolean("isExecutiveProducer") == true
                        if (supporterDisplayName.isNullOrBlank()) {
                            supporterDisplayName = doc.getString("displayName")?.takeIf { it.isNotBlank() && !it.contains("@") }
                                ?: doc.getString("creditName")?.takeIf { it.isNotBlank() && !it.contains("@") }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SupporterRepository", "Fallback query by field failed: ${e.message}")
                }
            }

            if (isSupporter) {
                val targetDoc = snapshot ?: firestore.collection("supporters").document(cleanEmail).get().await()
                val isAlreadyClaimed = targetDoc.getBoolean("claimed") == true
                val claimedByUid = targetDoc.getString("claimedByUid")

                if (isAlreadyClaimed && !isVerifiedEmailOwner && currentUid != null && claimedByUid != currentUid) {
                    return@withContext SupporterVerificationResult.ALREADY_CLAIMED
                }

                if (!isAlreadyClaimed && currentUid != null) {
                    try {
                        targetDoc.reference.update(
                            mapOf(
                                "claimed" to true,
                                "claimedByUid" to currentUid,
                                "claimedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                            )
                        ).await()
                    } catch (e: Exception) {
                        android.util.Log.w("SupporterRepository", "Could not update claimed status: ${e.message}")
                    }
                }

                val editor = badgePrefs.edit()
                    .putBoolean("is_supporter_verified", true)
                    .putString("supporter_verified_email", cleanEmail)

                if (currentUid != null) {
                    editor.putString("supporter_verified_uid", currentUid)
                }

                if (!supporterDisplayName.isNullOrBlank()) {
                    val formatted = supporterDisplayName.split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { part -> part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
                    editor.putString("supporter_credit_name", formatted)
                }

                editor.apply()

                if (user != null && !user.isAnonymous) {
                    try {
                        firestore.collection("users").document(user.uid)
                            .set(mapOf("isSupporter" to true), com.google.firebase.firestore.SetOptions.merge())
                            .await()
                    } catch (_: Exception) {}
                }

                SupporterVerificationResult.SUCCESS
            } else {
                SupporterVerificationResult.NOT_FOUND
            }
        } catch (e: Exception) {
            android.util.Log.e("SupporterRepository", "Error verifying supporter email: $cleanEmail", e)
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
                android.util.Log.w("SupporterRepository", "Failed to update credit name in users collection: ${e.message}")
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

        creditSyncJob?.cancel()
        creditSyncJob = repositoryScope.launch(Dispatchers.IO) {
            delay(1200L)
            if (name.isNullOrBlank()) return@launch
            if (lastSyncedCreditState == enabled) return@launch

            try {
                if (enabled) {
                    firestore.collection("system_metrics").document("credits_producers")
                        .update("producers", com.google.firebase.firestore.FieldValue.arrayUnion(name))
                        .await()
                    lastSyncedCreditState = true
                } else {
                    firestore.collection("system_metrics").document("credits_producers")
                        .update("producers", com.google.firebase.firestore.FieldValue.arrayRemove(name))
                        .await()
                    lastSyncedCreditState = false
                }
            } catch (e: Exception) {
                android.util.Log.w("SupporterRepository", "Failed to sync credit name with system_metrics: ${e.message}")
                try {
                    val user = auth.currentUser
                    if (user != null && !user.isAnonymous) {
                        firestore.collection("users").document(user.uid)
                            .update("showInCredits", enabled)
                            .await()
                    }
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
                android.util.Log.d("SupporterRepository", "credits_producers doc read failed: ${e.message}")
            }

            // 2. Fallback dinamico: se il documento centralizzato non esiste, interroga /supporters
            val collectedNames = mutableListOf<String>()
            if (isCreditActive && isSupporterVerified() && !mySupporterName.isNullOrBlank()) {
                collectedNames.add(mySupporterName)
            }

            try {
                val supportersSnap = firestore.collection("supporters").limit(100).get().await()
                supportersSnap.documents.forEach { doc ->
                    val isSupp = doc.getBoolean("isSupporter") == true || doc.getBoolean("isExecutiveProducer") == true
                    val email = doc.getString("email") ?: doc.id
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
                android.util.Log.d("SupporterRepository", "Supporters collection read failed: ${e.message}")
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
                android.util.Log.d("SupporterRepository", "Users collection read failed: ${e.message}")
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
            android.util.Log.w("SupporterRepository", "Failed to fetch public credits: ${e.message}")
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

        val cached = badgePrefs.getStringSet("cached_credits_supporters", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (!oldName.isNullOrBlank()) cached.remove(oldName)
        cached.add(formatted)
        badgePrefs.edit().putStringSet("cached_credits_supporters", cached).apply()

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
}
