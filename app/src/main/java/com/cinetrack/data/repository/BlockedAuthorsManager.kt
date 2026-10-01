package com.cinetrack.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class BlockedAuthor(
    val authorId: String = "",
    val authorName: String = "",
    val blockedAt: Long = System.currentTimeMillis()
)

@Singleton
class BlockedAuthorsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("blocked_authors_prefs", Context.MODE_PRIVATE)

    private val _blockedAuthorIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedAuthorIds: StateFlow<Set<String>> = _blockedAuthorIds.asStateFlow()

    private val _blockedAuthorNames = MutableStateFlow<Set<String>>(emptySet())
    val blockedAuthorNames: StateFlow<Set<String>> = _blockedAuthorNames.asStateFlow()

    private val _blockedAuthorsList = MutableStateFlow<List<BlockedAuthor>>(emptyList())
    val blockedAuthorsList: StateFlow<List<BlockedAuthor>> = _blockedAuthorsList.asStateFlow()

    private var firestoreListener: ListenerRegistration? = null
    private var currentListeningUid: String? = null

    init {
        loadFromLocalPrefs()
        listenToAuthAndFirestore()
    }

    private fun getActiveKey(): String = auth.currentUser?.takeIf { !it.isAnonymous }?.uid ?: "local_guest"

    private fun getCloudUid(): String? = auth.currentUser?.takeIf { !it.isAnonymous }?.uid

    private fun loadFromLocalPrefs() {
        val activeKey = getActiveKey()
        val idSet = prefs.getStringSet("blocked_ids_$activeKey", emptySet())?.toMutableSet() ?: mutableSetOf()

        // Se l'utente è loggato nel cloud, facciamo il merge con gli eventuali blocchi registrati da guest locale
        if (activeKey != "local_guest") {
            val guestIds = prefs.getStringSet("blocked_ids_local_guest", emptySet()) ?: emptySet()
            if (guestIds.isNotEmpty()) {
                idSet.addAll(guestIds)
                for (id in guestIds) {
                    val name = prefs.getString("blocked_name_local_guest_$id", "") ?: ""
                    val time = prefs.getLong("blocked_time_local_guest_$id", System.currentTimeMillis())
                    prefs.edit()
                        .putString("blocked_name_${activeKey}_$id", name)
                        .putLong("blocked_time_${activeKey}_$id", time)
                        .apply()
                }
                prefs.edit().putStringSet("blocked_ids_$activeKey", idSet).apply()
            }
        }

        val list = mutableListOf<BlockedAuthor>()
        val names = mutableSetOf<String>()
        for (id in idSet) {
            val name = prefs.getString("blocked_name_${activeKey}_$id", "") ?: ""
            val time = prefs.getLong("blocked_time_${activeKey}_$id", System.currentTimeMillis())
            list.add(BlockedAuthor(authorId = id, authorName = name, blockedAt = time))
            if (name.isNotBlank()) {
                names.add(name.trim().lowercase())
            }
        }
        _blockedAuthorIds.value = idSet
        _blockedAuthorsList.value = list.sortedByDescending { it.blockedAt }
        _blockedAuthorNames.value = names
    }

    private fun listenToAuthAndFirestore() {
        auth.addAuthStateListener { firebaseAuth ->
            val cloudUid = firebaseAuth.currentUser?.takeIf { !it.isAnonymous }?.uid
            if (cloudUid != currentListeningUid) {
                currentListeningUid = cloudUid
                firestoreListener?.remove()
                firestoreListener = null

                loadFromLocalPrefs()

                if (cloudUid != null) {
                    attachFirestoreListener(cloudUid)
                }
            }
        }
    }

    private fun attachFirestoreListener(uid: String) {
        firestoreListener = firestore.collection("users")
            .document(uid)
            .collection("blocked_authors")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch {
                    val remoteList = mutableListOf<BlockedAuthor>()
                    val remoteIds = mutableSetOf<String>()
                    val remoteNames = mutableSetOf<String>()

                    for (doc in snapshot.documents) {
                        val authorId = doc.getString("authorId") ?: doc.id
                        val authorName = doc.getString("authorName") ?: ""
                        val blockedAt = doc.getTimestamp("blockedAt")?.toDate()?.time
                            ?: doc.getLong("blockedAt")
                            ?: System.currentTimeMillis()

                        remoteIds.add(authorId)
                        remoteList.add(BlockedAuthor(authorId, authorName, blockedAt))
                        if (authorName.isNotBlank()) {
                            remoteNames.add(authorName.trim().lowercase())
                        }

                        // Sync to local SharedPreferences
                        prefs.edit()
                            .putString("blocked_name_${uid}_$authorId", authorName)
                            .putLong("blocked_time_${uid}_$authorId", blockedAt)
                            .apply()
                    }

                    prefs.edit().putStringSet("blocked_ids_$uid", remoteIds).apply()

                    _blockedAuthorIds.value = remoteIds
                    _blockedAuthorsList.value = remoteList.sortedByDescending { it.blockedAt }
                    _blockedAuthorNames.value = remoteNames
                }
            }
    }

    fun isAuthorBlocked(authorId: String?, authorName: String? = null): Boolean {
        val cleanId = authorId?.trim().orEmpty()
        val cleanName = authorName?.trim().orEmpty().lowercase()

        if (cleanId.isNotBlank()) {
            if (_blockedAuthorIds.value.contains(cleanId)) return true
            if (_blockedAuthorIds.value.contains("name_$cleanId")) return true
        }
        if (cleanName.isNotBlank()) {
            if (_blockedAuthorNames.value.contains(cleanName)) return true
            if (_blockedAuthorIds.value.contains("name_$cleanName")) return true
            if (_blockedAuthorIds.value.contains(cleanName)) return true
        }
        return false
    }

    suspend fun blockAuthor(authorId: String, authorName: String) {
        val cleanId = authorId.trim()
        val cleanName = authorName.trim()
        val effectiveId = if (cleanId.isNotBlank()) cleanId else "name_${cleanName.lowercase()}"
        if (effectiveId.isBlank()) return

        val activeKey = getActiveKey()
        val now = System.currentTimeMillis()

        val newSet = _blockedAuthorIds.value + effectiveId
        val newList = (_blockedAuthorsList.value.filterNot { it.authorId == effectiveId } +
            BlockedAuthor(effectiveId, cleanName, now)).sortedByDescending { it.blockedAt }
        val newNames = newList.map { it.authorName.trim().lowercase() }.filter { it.isNotBlank() }.toSet()

        _blockedAuthorIds.value = newSet
        _blockedAuthorsList.value = newList
        _blockedAuthorNames.value = newNames

        // 1. Immediate Local Persistence (funziona sia per guest che per utenti autenticati)
        prefs.edit()
            .putStringSet("blocked_ids_$activeKey", newSet)
            .putString("blocked_name_${activeKey}_$effectiveId", cleanName)
            .putLong("blocked_time_${activeKey}_$effectiveId", now)
            .apply()

        // 2. Cloud Sync (Firestore) se autenticati con vero account
        val cloudUid = getCloudUid()
        if (cloudUid != null) {
            try {
                val docData = mapOf(
                    "authorId" to effectiveId,
                    "authorName" to cleanName,
                    "blockedAt" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("users")
                    .document(cloudUid)
                    .collection("blocked_authors")
                    .document(effectiveId)
                    .set(docData)
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("BlockedAuthorsManager", "Failed to sync block to Firestore for $effectiveId", e)
            }
        }
    }

    suspend fun unblockAuthor(authorId: String) {
        if (authorId.isBlank()) return
        val activeKey = getActiveKey()

        val target = _blockedAuthorsList.value.find {
            it.authorId == authorId ||
            it.authorName.equals(authorId, ignoreCase = true) ||
            it.authorId == "name_${authorId.trim().lowercase()}"
        }
        val idToRemove = target?.authorId ?: authorId

        val newSet = _blockedAuthorIds.value - idToRemove
        val newList = _blockedAuthorsList.value.filterNot { it.authorId == idToRemove }
        val newNames = newList.map { it.authorName.trim().lowercase() }.filter { it.isNotBlank() }.toSet()

        _blockedAuthorIds.value = newSet
        _blockedAuthorsList.value = newList
        _blockedAuthorNames.value = newNames

        // 1. Immediate Local Persistence
        prefs.edit()
            .putStringSet("blocked_ids_$activeKey", newSet)
            .remove("blocked_name_${activeKey}_$idToRemove")
            .remove("blocked_time_${activeKey}_$idToRemove")
            .apply()

        if (activeKey != "local_guest") {
            val guestIds = prefs.getStringSet("blocked_ids_local_guest", emptySet())?.toMutableSet()
            if (guestIds != null && guestIds.remove(idToRemove)) {
                prefs.edit()
                    .putStringSet("blocked_ids_local_guest", guestIds)
                    .remove("blocked_name_local_guest_$idToRemove")
                    .remove("blocked_time_local_guest_$idToRemove")
                    .apply()
            }
        }

        // 2. Cloud Sync (Firestore)
        val cloudUid = getCloudUid()
        if (cloudUid != null) {
            try {
                firestore.collection("users")
                    .document(cloudUid)
                    .collection("blocked_authors")
                    .document(idToRemove)
                    .delete()
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("BlockedAuthorsManager", "Failed to sync unblock to Firestore for $idToRemove", e)
            }
        }
    }
}
