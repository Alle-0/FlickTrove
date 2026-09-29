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

    private val _blockedAuthorsList = MutableStateFlow<List<BlockedAuthor>>(emptyList())
    val blockedAuthorsList: StateFlow<List<BlockedAuthor>> = _blockedAuthorsList.asStateFlow()

    private var firestoreListener: ListenerRegistration? = null
    private var currentListeningUid: String? = null

    init {
        loadFromLocalPrefs()
        listenToAuthAndFirestore()
    }

    private fun getUid(): String? = auth.currentUser?.takeIf { !it.isAnonymous }?.uid

    private fun loadFromLocalPrefs() {
        val uid = getUid() ?: return
        val idSet = prefs.getStringSet("blocked_ids_$uid", emptySet()) ?: emptySet()
        val list = mutableListOf<BlockedAuthor>()
        for (id in idSet) {
            val name = prefs.getString("blocked_name_${uid}_$id", "") ?: ""
            val time = prefs.getLong("blocked_time_${uid}_$id", System.currentTimeMillis())
            list.add(BlockedAuthor(authorId = id, authorName = name, blockedAt = time))
        }
        _blockedAuthorIds.value = idSet
        _blockedAuthorsList.value = list.sortedByDescending { it.blockedAt }
    }

    private fun listenToAuthAndFirestore() {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.takeIf { !it.isAnonymous }?.uid
            if (uid != currentListeningUid) {
                currentListeningUid = uid
                firestoreListener?.remove()
                firestoreListener = null

                if (uid != null) {
                    loadFromLocalPrefs()
                    attachFirestoreListener(uid)
                } else {
                    _blockedAuthorIds.value = emptySet()
                    _blockedAuthorsList.value = emptyList()
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

                    for (doc in snapshot.documents) {
                        val authorId = doc.getString("authorId") ?: doc.id
                        val authorName = doc.getString("authorName") ?: ""
                        val blockedAt = doc.getTimestamp("blockedAt")?.toDate()?.time
                            ?: doc.getLong("blockedAt")
                            ?: System.currentTimeMillis()

                        remoteIds.add(authorId)
                        remoteList.add(BlockedAuthor(authorId, authorName, blockedAt))

                        // Sync to local SharedPreferences
                        prefs.edit()
                            .putString("blocked_name_${uid}_$authorId", authorName)
                            .putLong("blocked_time_${uid}_$authorId", blockedAt)
                            .apply()
                    }

                    prefs.edit().putStringSet("blocked_ids_$uid", remoteIds).apply()

                    _blockedAuthorIds.value = remoteIds
                    _blockedAuthorsList.value = remoteList.sortedByDescending { it.blockedAt }
                }
            }
    }

    fun isAuthorBlocked(authorId: String): Boolean {
        if (authorId.isBlank()) return false
        return _blockedAuthorIds.value.contains(authorId)
    }

    suspend fun blockAuthor(authorId: String, authorName: String) {
        if (authorId.isBlank()) return
        val uid = getUid() ?: return

        val now = System.currentTimeMillis()
        val newSet = _blockedAuthorIds.value + authorId
        val newList = (_blockedAuthorsList.value.filterNot { it.authorId == authorId } +
            BlockedAuthor(authorId, authorName, now)).sortedByDescending { it.blockedAt }

        _blockedAuthorIds.value = newSet
        _blockedAuthorsList.value = newList

        // 1. Immediate Local Persistence
        prefs.edit()
            .putStringSet("blocked_ids_$uid", newSet)
            .putString("blocked_name_${uid}_$authorId", authorName)
            .putLong("blocked_time_${uid}_$authorId", now)
            .apply()

        // 2. Cloud Sync (Firestore)
        try {
            val docData = mapOf(
                "authorId" to authorId,
                "authorName" to authorName,
                "blockedAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("users")
                .document(uid)
                .collection("blocked_authors")
                .document(authorId)
                .set(docData)
                .await()
        } catch (e: Exception) {
            android.util.Log.e("BlockedAuthorsManager", "Failed to sync block to Firestore for $authorId", e)
        }
    }

    suspend fun unblockAuthor(authorId: String) {
        if (authorId.isBlank()) return
        val uid = getUid() ?: return

        val newSet = _blockedAuthorIds.value - authorId
        val newList = _blockedAuthorsList.value.filterNot { it.authorId == authorId }

        _blockedAuthorIds.value = newSet
        _blockedAuthorsList.value = newList

        // 1. Immediate Local Persistence
        prefs.edit()
            .putStringSet("blocked_ids_$uid", newSet)
            .remove("blocked_name_${uid}_$authorId")
            .remove("blocked_time_${uid}_$authorId")
            .apply()

        // 2. Cloud Sync (Firestore)
        try {
            firestore.collection("users")
                .document(uid)
                .collection("blocked_authors")
                .document(authorId)
                .delete()
                .await()
        } catch (e: Exception) {
            android.util.Log.e("BlockedAuthorsManager", "Failed to sync unblock to Firestore for $authorId", e)
        }
    }
}
