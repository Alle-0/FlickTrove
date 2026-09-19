package com.cinetrack.data.repository

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.cinetrack.data.local.dao.FolderDao
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.model.Folder
import com.cinetrack.data.remote.FirebaseRemoteDataSource
import com.cinetrack.worker.TraktInstantWriteWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(
    private val folderDao: FolderDao,
    private val firebaseRemoteDataSource: FirebaseRemoteDataSource,
    @ApplicationContext private val context: Context
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getFoldersFlow(): Flow<List<FolderEntity>> = folderDao.getAllFlow()

    fun getFolderFlow(folderId: String): Flow<FolderEntity?> = folderDao.getByIdFlow(folderId)

    suspend fun getAllFolders(): List<FolderEntity> = folderDao.getAll()

    suspend fun saveFolder(folderEntity: FolderEntity) {
        // Difference calculation (diff) between old and new state
        val oldFolder = folderDao.getByIdFlow(folderEntity.id).firstOrNull()

        val now = System.currentTimeMillis()
        val updatedEntity = folderEntity.copy(syncStatus = "synced", clientUpdatedAt = now)
        folderDao.insert(updatedEntity)

        // --- TRAKT INTEGRATION (Instant Write) ---
        if (folderEntity.id.startsWith("trakt_")) {
            val traktListId = folderEntity.id.removePrefix("trakt_").toLongOrNull()
            if (traktListId != null) {
                val workRequests = mutableListOf<androidx.work.OneTimeWorkRequest>()

                // 1. Name or Description change check
                if (oldFolder != null && (oldFolder.name != folderEntity.name || oldFolder.description != folderEntity.description)) {
                    val updateBuilder = androidx.work.Data.Builder()
                        .putString(TraktInstantWriteWorker.KEY_ACTION, "ACTION_UPDATE_LIST")
                        .putLong("LIST_ID", traktListId)
                        .putString("LIST_NAME", folderEntity.name)
                        .putString("LIST_DESC", folderEntity.description ?: "")

                    workRequests.add(
                        OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .setInputData(updateBuilder.build())
                            .build()
                    )
                }

                // 2. Added or removed items check
                val oldItems = oldFolder?.itemIds?.toSet() ?: emptySet()
                val newItems = folderEntity.itemIds.toSet()

                val added = newItems - oldItems
                val removed = oldItems - newItems

                if (added.isNotEmpty()) {
                    val addBuilder = androidx.work.Data.Builder()
                        .putString(TraktInstantWriteWorker.KEY_ACTION, "ACTION_ADD_LIST_ITEMS")
                        .putLong("LIST_ID", traktListId)
                        .putStringArray("ITEMS_ADDED", added.toTypedArray())
                    workRequests.add(
                        OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .setInputData(addBuilder.build())
                            .build()
                    )
                }

                if (removed.isNotEmpty()) {
                    val removeBuilder = androidx.work.Data.Builder()
                        .putString(TraktInstantWriteWorker.KEY_ACTION, "ACTION_REMOVE_LIST_ITEMS")
                        .putLong("LIST_ID", traktListId)
                        .putStringArray("ITEMS_REMOVED", removed.toTypedArray())
                    workRequests.add(
                        OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .setInputData(removeBuilder.build())
                            .build()
                    )
                }

                if (workRequests.isNotEmpty()) {
                    WorkManager.getInstance(context).enqueue(workRequests)
                }
            }
        } else if (oldFolder == null) {
            // Newly created local folder - push to Trakt
            val createBuilder = androidx.work.Data.Builder()
                .putString(TraktInstantWriteWorker.KEY_ACTION, "ACTION_CREATE_LIST")
                .putString("LOCAL_FOLDER_ID", folderEntity.id)
                .putString("LIST_NAME", folderEntity.name)
                .putString("LIST_DESC", folderEntity.description ?: "")

            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(createBuilder.build())
                    .build()
            )
        }
        // --- END TRAKT INTEGRATION ---

        repositoryScope.launch {
            try {
                val folder = Folder(
                    id = updatedEntity.id,
                    name = updatedEntity.name,
                    icon = updatedEntity.icon,
                    color = updatedEntity.color,
                    description = updatedEntity.description,
                    itemIds = updatedEntity.itemIds,
                    createdAt = updatedEntity.createdAt,
                    updatedAt = updatedEntity.updatedAt,
                    clientUpdatedAt = now
                )
                firebaseRemoteDataSource.setFolder(folder)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("FolderRepository", "Firebase folder sync failed for ${updatedEntity.id}", e)
                withContext(NonCancellable) {
                    folderDao.updateSyncStatus(updatedEntity.id, "pending")
                }
            }
        }
    }

    suspend fun deleteFolder(folderId: String) {
        folderDao.markDeleted(folderId)

        // --- TRAKT INTEGRATION ---
        if (folderId.startsWith("trakt_")) {
            val traktListId = folderId.removePrefix("trakt_").toLongOrNull()
            if (traktListId != null) {
                val deleteBuilder = androidx.work.Data.Builder()
                    .putString(TraktInstantWriteWorker.KEY_ACTION, "ACTION_DELETE_LIST")
                    .putLong("LIST_ID", traktListId)

                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setInputData(deleteBuilder.build())
                        .build()
                )
            }
        }
        // --- END TRAKT INTEGRATION ---

        repositoryScope.launch {
            try {
                firebaseRemoteDataSource.deleteFolder(folderId)
                folderDao.deleteById(folderId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    suspend fun pushPendingFolders() {
        val pendingFolders = folderDao.getPendingSync()
        for (folder in pendingFolders) {
            try {
                if (folder.syncStatus == "pending_delete") {
                    firebaseRemoteDataSource.deleteFolder(folder.id)
                    folderDao.deleteById(folder.id)
                } else if (folder.syncStatus == "pending") {
                    val folderDto = Folder(
                        id = folder.id,
                        name = folder.name,
                        icon = folder.icon,
                        color = folder.color,
                        description = folder.description,
                        itemIds = folder.itemIds,
                        createdAt = folder.createdAt,
                        updatedAt = folder.updatedAt,
                        clientUpdatedAt = folder.clientUpdatedAt
                    )
                    firebaseRemoteDataSource.setFolder(folderDto)
                    folderDao.updateSyncStatus(folder.id, "synced")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("FolderRepository", "Failed to push pending folder ${folder.id}", e)
            }
        }
    }

    suspend fun syncFoldersFromRemote(
        remoteFolders: List<Folder>,
        onProgress: (suspend (currentCount: Int, totalCount: Int, portion: Float) -> Unit)? = null
    ) {
        val localFoldersList = folderDao.getAll()
        val localFolders = localFoldersList.associateBy { it.id }
        val remoteFoldersMap = remoteFolders.associateBy { it.id }

        val foldersToInsert = mutableListOf<FolderEntity>()
        val foldersToDelete = mutableListOf<FolderEntity>()

        for (remoteFolder in remoteFolders) {
            val local = localFolders[remoteFolder.id]
            val remoteEntity = FolderEntity(
                id = remoteFolder.id,
                name = remoteFolder.name,
                icon = remoteFolder.icon,
                color = remoteFolder.color,
                description = remoteFolder.description,
                itemIds = remoteFolder.itemIds,
                createdAt = remoteFolder.createdAt ?: "",
                updatedAt = remoteFolder.updatedAt ?: "",
                syncStatus = "synced",
                clientUpdatedAt = remoteFolder.clientUpdatedAt
            )

            if (local == null) {
                foldersToInsert.add(remoteEntity)
            } else {
                if (remoteFolder.clientUpdatedAt >= local.clientUpdatedAt) {
                    foldersToInsert.add(remoteEntity)
                } else {
                    if (local.syncStatus == "synced") {
                        folderDao.updateSyncStatus(local.id, "pending")
                    }
                }
            }
        }

        for ((id, localFolder) in localFolders) {
            if (!remoteFoldersMap.containsKey(id)) {
                if (localFolder.syncStatus == "synced") {
                    foldersToDelete.add(localFolder)
                }
            }
        }

        if (foldersToInsert.isNotEmpty()) {
            val chunks = foldersToInsert.chunked(50)
            chunks.forEachIndexed { index, chunk ->
                folderDao.insertAll(chunk)
                val portion = (index + 1).toFloat() / chunks.size.toFloat()
                val currentCount = (index + 1) * chunk.size
                onProgress?.invoke(currentCount.coerceAtMost(foldersToInsert.size), foldersToInsert.size, portion)
            }
        }

        for (folderToDelete in foldersToDelete) {
            folderDao.deleteById(folderToDelete.id)
        }
        android.util.Log.d("FolderRepository", "Successfully synchronized folders with conflict resolution")
    }

    suspend fun clearAll() {
        folderDao.clearAll()
    }
}
