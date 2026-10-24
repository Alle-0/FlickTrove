package com.cinetrack.data.repository

import android.content.Context
import com.cinetrack.data.api.SupabaseSharedFolderApi
import com.cinetrack.data.api.TMDBService
import com.cinetrack.data.local.dao.FolderDao
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.mapper.MovieMapper
import com.cinetrack.data.model.*
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Duration
import java.time.Instant
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

enum class InviteDurationOption(val minutes: Long?, val maxUses: Int?) {
    TWENTY_FOUR_HOURS(24 * 60L, null),
    FORTY_EIGHT_HOURS(48 * 60L, null),
    SEVEN_DAYS(7 * 24 * 60L, null),
    SINGLE_USE(7 * 24 * 60L, 1)
}

@Singleton
class SharedFolderRepository @Inject constructor(
    private val api: SupabaseSharedFolderApi,
    private val folderDao: FolderDao,
    private val movieRepository: MovieRepository,
    private val tmdbService: TMDBService,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {

    private fun generateShareCode(): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val randomPart = (1..5).map { chars.random() }.joinToString("")
        return "FT-$randomPart"
    }

    private fun getCurrentUser() = auth.currentUser?.takeIf { !it.isAnonymous }

    suspend fun createSharedFolder(folderEntity: FolderEntity): Result<FolderEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val user = getCurrentUser() ?: throw IllegalStateException("USER_NOT_LOGGED_IN")
            val code = generateShareCode()
            val now = Instant.now().toString()

            val itemsDto = folderEntity.itemIds.mapNotNull { compositeId ->
                val parts = compositeId.split("_", limit = 2)
                if (parts.size == 2) {
                    val type = parts[0]
                    val id = parts[1].toLongOrNull() ?: return@mapNotNull null
                    val movie = movieRepository.getMovie(id, type)
                    SharedFolderItemDto(
                        id = id,
                        mediaType = type,
                        title = movie?.title ?: movie?.name,
                        posterPath = movie?.posterPath,
                        addedBy = user.displayName ?: "Autore",
                        addedAt = System.currentTimeMillis()
                    )
                } else null
            }

            val folderDto = SharedFolderDto(
                shareCode = code,
                ownerUid = user.uid,
                ownerName = user.displayName ?: "Utente FlickTrove",
                ownerAvatarUrl = user.photoUrl?.toString(),
                title = folderEntity.name,
                description = folderEntity.description,
                color = folderEntity.color ?: "#FFFFFF",
                icon = folderEntity.icon ?: "ic_cartella",
                isPublic = false,
                maxItems = folderEntity.maxItems,
                items = itemsDto,
                createdAt = now,
                updatedAt = now
            )

            val createdList = api.createSharedFolder(folderDto)
            val created = createdList.firstOrNull() ?: throw IllegalStateException("FAILED_TO_CREATE_SHARED_FOLDER")
            val sharedId = created.id ?: throw IllegalStateException("MISSING_SHARED_FOLDER_ID")

            // Add owner to members table
            val ownerMember = SharedFolderMemberDto(
                folderId = sharedId,
                userUid = user.uid,
                userName = user.displayName ?: "Utente FlickTrove",
                userAvatarUrl = user.photoUrl?.toString(),
                role = "owner",
                joinedAt = now
            )
            api.addMember(ownerMember)

            // Update local Room database
            val updatedFolder = folderEntity.copy(
                isShared = true,
                sharedFolderId = sharedId,
                ownerUid = user.uid,
                ownerName = user.displayName ?: "Utente FlickTrove",
                shareCode = code,
                maxItems = created.maxItems,
                syncStatus = "synced",
                clientUpdatedAt = System.currentTimeMillis()
            )
            folderDao.insert(updatedFolder)
            updatedFolder
        }
    }

    suspend fun createInvite(
        sharedFolderId: String,
        durationOption: InviteDurationOption
    ): Result<SharedFolderInviteDto> = withContext(Dispatchers.IO) {
        runCatching {
            val user = getCurrentUser() ?: throw IllegalStateException("USER_NOT_LOGGED_IN")
            
            val currentActiveInvites = getFolderInvites(sharedFolderId).getOrDefault(emptyList())
            if (currentActiveInvites.size >= 10) {
                throw IllegalStateException("MAX_INVITES_REACHED")
            }

            val code = generateShareCode()
            val now = Instant.now()
            val expiresAt = if (durationOption.minutes != null) {
                now.plus(Duration.ofMinutes(durationOption.minutes)).toString()
            } else {
                now.plus(Duration.ofDays(7)).toString()
            }

            val inviteDto = SharedFolderInviteDto(
                code = code,
                folderId = sharedFolderId,
                createdByUid = user.uid,
                expiresAt = expiresAt,
                maxUses = durationOption.maxUses,
                usedCount = 0,
                isRevoked = false,
                createdAt = now.toString()
            )

            val created = api.createInvite(inviteDto)
            created.firstOrNull() ?: throw IllegalStateException("FAILED_TO_CREATE_INVITE")
        }
    }

    suspend fun shareFolderAndCreateInvite(
        folderEntity: FolderEntity,
        durationOption: InviteDurationOption
    ): Result<Pair<FolderEntity, SharedFolderInviteDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val sharedFolder = if (folderEntity.isShared && folderEntity.sharedFolderId != null) {
                folderEntity
            } else {
                createSharedFolder(folderEntity).getOrThrow()
            }
            val sharedId = sharedFolder.sharedFolderId ?: throw IllegalStateException("MISSING_SHARED_FOLDER_ID")
            val invite = createInvite(sharedId, durationOption).getOrThrow()
            Pair(sharedFolder, invite)
        }
    }

    suspend fun getInvite(code: String): Result<Pair<SharedFolderInviteDto, SharedFolderDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = code.trim().uppercase()
            val formattedCode = if (raw.startsWith("FT-")) raw else "FT-$raw"
            val invites = api.getInviteByCode("eq.$formattedCode")
            val invite = invites.firstOrNull() ?: throw IllegalStateException("INVITE_NOT_FOUND")

            if (invite.isRevoked) {
                throw IllegalStateException("INVITE_REVOKED")
            }

            if (invite.isExpired) {
                throw IllegalStateException("INVITE_EXPIRED")
            }

            if (invite.maxUses != null && invite.usedCount >= invite.maxUses) {
                throw IllegalStateException("INVITE_MAX_USES_REACHED")
            }

            val folders = api.getSharedFolderById("eq.${invite.folderId}")
            val folder = folders.firstOrNull() ?: throw IllegalStateException("FOLDER_NOT_FOUND")

            Pair(invite, folder)
        }
    }

    suspend fun acceptInvite(code: String): Result<FolderEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val user = getCurrentUser() ?: throw IllegalStateException("USER_NOT_LOGGED_IN")
            val (invite, sharedDto) = getInvite(code).getOrThrow()
            val sharedId = sharedDto.id ?: throw IllegalStateException("MISSING_SHARED_FOLDER_ID")

            // Check members count limit (25 members)
            val currentMembers = api.getFolderMembers("eq.$sharedId")
            val alreadyMember = currentMembers.any { it.userUid == user.uid }

            if (!alreadyMember) {
                if (currentMembers.size >= MAX_FOLDER_MEMBERS) {
                    throw IllegalStateException("MEMBER_LIMIT_REACHED")
                }
                val memberDto = SharedFolderMemberDto(
                    folderId = sharedId,
                    userUid = user.uid,
                    userName = user.displayName ?: "Collaboratore",
                    userAvatarUrl = user.photoUrl?.toString(),
                    role = "editor",
                    joinedAt = Instant.now().toString()
                )
                api.addMember(memberDto)

                // Increment invite used_count
                val newCount = invite.usedCount + 1
                api.updateInvite(
                    "eq.${invite.code}",
                    buildJsonObject {
                        put("used_count", newCount)
                    }
                )
            }

            // Check if user already has this folder locally (e.g. owner or already imported)
            val existingLocal = folderDao.getAll().find { it.sharedFolderId == sharedId }
            if (existingLocal != null) {
                return@runCatching existingLocal
            }

            // Map and store in local Room database
            val localFolderId = "shared_${sharedId}"
            val compositeItemIds = sharedDto.items.map { "${it.mediaType}_${it.id}" }

            val folderEntity = FolderEntity(
                id = localFolderId,
                name = sharedDto.title,
                icon = sharedDto.icon ?: "ic_cartella",
                color = sharedDto.color ?: "#FFFFFF",
                description = sharedDto.description,
                itemIds = compositeItemIds,
                createdAt = sharedDto.createdAt ?: Instant.now().toString(),
                updatedAt = sharedDto.updatedAt ?: Instant.now().toString(),
                syncStatus = "synced",
                clientUpdatedAt = System.currentTimeMillis(),
                isShared = true,
                sharedFolderId = sharedId,
                ownerUid = sharedDto.ownerUid,
                ownerName = sharedDto.ownerName,
                shareCode = sharedDto.shareCode,
                maxItems = sharedDto.maxItems
            )
            folderDao.insert(folderEntity)

            // Asynchronously pre-fetch any missing movie/TV details into Room cache
            fetchMissingItemsMetadata(sharedDto.items)

            folderEntity
        }
    }

    private suspend fun fetchMissingItemsMetadata(items: List<SharedFolderItemDto>) {
        withContext(Dispatchers.IO) {
            val missingMovies = mutableListOf<com.cinetrack.data.model.Movie>()
            items.forEach { item ->
                try {
                    val local = movieRepository.getMovie(item.id, item.mediaType)
                    if (local == null) {
                        val response = if (item.mediaType == "tv") {
                            tmdbService.getTVBasicDetails(item.id)
                        } else {
                            tmdbService.getMovieBasicDetails(item.id)
                        }
                        val movie = MovieMapper.mapResponseToMovie(response, item.mediaType)
                        missingMovies.add(movie)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SharedFolderRepository", "Error fetching item metadata for ${item.id}", e)
                }
            }
            if (missingMovies.isNotEmpty()) {
                movieRepository.saveMoviesBulk(missingMovies)
            }
        }
    }

    suspend fun syncSingleSharedFolder(sharedFolderId: String): Result<FolderEntity?> = withContext(Dispatchers.IO) {
        runCatching {
            val remoteFolders = api.getSharedFolderById("eq.$sharedFolderId")
            val remoteDto = remoteFolders.firstOrNull() ?: return@runCatching null
            val localFolders = folderDao.getAll()
            val existing = localFolders.find { it.sharedFolderId == sharedFolderId }
            val localId = existing?.id ?: "shared_$sharedFolderId"
            val compositeItemIds = remoteDto.items.map { "${it.mediaType}_${it.id}" }

            val updatedEntity = FolderEntity(
                id = localId,
                name = remoteDto.title,
                icon = remoteDto.icon ?: "ic_cartella",
                color = remoteDto.color ?: "#FFFFFF",
                description = remoteDto.description,
                itemIds = compositeItemIds,
                createdAt = existing?.createdAt ?: remoteDto.createdAt ?: Instant.now().toString(),
                updatedAt = remoteDto.updatedAt ?: Instant.now().toString(),
                syncStatus = "synced",
                clientUpdatedAt = System.currentTimeMillis(),
                isShared = true,
                sharedFolderId = sharedFolderId,
                ownerUid = remoteDto.ownerUid,
                ownerName = remoteDto.ownerName,
                shareCode = remoteDto.shareCode,
                maxItems = remoteDto.maxItems
            )
            folderDao.insert(updatedEntity)
            fetchMissingItemsMetadata(remoteDto.items)
            updatedEntity
        }
    }

    suspend fun pushFolderUpdate(folderEntity: FolderEntity): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sharedId = folderEntity.sharedFolderId ?: return@runCatching
            val user = getCurrentUser() ?: return@runCatching

            if (folderEntity.itemIds.size > folderEntity.maxItems) {
                throw IllegalStateException("MAX_ITEMS_REACHED")
            }

            val now = Instant.now().toString()

            // Fetch current remote items to preserve existing addedBy, addedAt, posterPath and titles
            val remoteFolder = api.getSharedFolderById("eq.$sharedId").firstOrNull()
            val remoteItemsMap = remoteFolder?.items?.associateBy { "${it.mediaType}_${it.id}" } ?: emptyMap()

            val itemsDto = folderEntity.itemIds.mapNotNull { compositeId ->
                val parts = compositeId.split("_", limit = 2)
                if (parts.size == 2) {
                    val type = parts[0]
                    val id = parts[1].toLongOrNull() ?: return@mapNotNull null
                    val existing = remoteItemsMap[compositeId]
                    if (existing != null) {
                        existing
                    } else {
                        val movie = movieRepository.getMovie(id, type)
                        SharedFolderItemDto(
                            id = id,
                            mediaType = type,
                            title = movie?.title ?: movie?.name,
                            posterPath = movie?.posterPath,
                            addedBy = user.displayName ?: "Membro",
                            addedAt = System.currentTimeMillis()
                        )
                    }
                } else null
            }

            val respItems = api.updateSharedFolderItems(
                "eq.$sharedId",
                UpdateItemsRequest(
                    items = itemsDto,
                    updatedAt = now
                )
            )
            if (!respItems.isSuccessful) {
                throw HttpException(respItems)
            }

            // Only update folder metadata (title, description, color, icon) if user is owner
            if (folderEntity.ownerUid == user.uid) {
                val respMeta = api.updateSharedFolder(
                    "eq.$sharedId",
                    buildJsonObject {
                        put("title", folderEntity.name)
                        folderEntity.description?.let { put("description", it) }
                        folderEntity.color?.let { put("color", it) }
                        folderEntity.icon?.let { put("icon", it) }
                        put("updated_at", now)
                    }
                )
                if (!respMeta.isSuccessful) {
                    throw HttpException(respMeta)
                }
            }
        }
    }

    suspend fun syncUserSharedFolders(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = getCurrentUser() ?: return@runCatching
            val memberships = api.getUserSharedFolders("eq.${user.uid}")
            val localFolders = folderDao.getAll()

            // 1. Auto-cleanup duplicates in local Room database:
            // Group by sharedFolderId and remove any redundant clone with id == "shared_$sharedId"
            // if an original folder already exists with a different ID.
            localFolders.filter { it.isShared && it.sharedFolderId != null }
                .groupBy { it.sharedFolderId!! }
                .forEach { (sharedId, foldersWithSameSharedId) ->
                    if (foldersWithSameSharedId.size > 1) {
                        // Keep the original folder (prefer the one with oldest createdAt or id != "shared_$sharedId")
                        val original = foldersWithSameSharedId.minByOrNull { it.createdAt }
                            ?: foldersWithSameSharedId.first()
                        val duplicates = foldersWithSameSharedId.filter { it.id != original.id }
                        duplicates.forEach { dup ->
                            folderDao.deleteById(dup.id)
                        }
                    }
                }

            // 2. Remove local shared folders where user is collaborator if owner deleted the folder or removed the member
            val activeRemoteSharedIds = memberships.mapNotNull { it.sharedFolder?.id }.toSet()
            localFolders.filter { it.isShared && it.ownerUid != user.uid && it.sharedFolderId != null }
                .filter { it.sharedFolderId !in activeRemoteSharedIds }
                .forEach { deadFolder ->
                    folderDao.deleteById(deadFolder.id)
                }

            val refreshedLocalFolders = folderDao.getAll()

            memberships.forEach { membership ->
                val sharedDto = membership.sharedFolder ?: return@forEach
                val sharedId = sharedDto.id ?: return@forEach
                
                // Match existing local folder by sharedFolderId to avoid ID collision
                val existing = refreshedLocalFolders.find { it.sharedFolderId == sharedId }
                val localId = existing?.id ?: "shared_${sharedId}"
                val compositeItemIds = sharedDto.items.map { "${it.mediaType}_${it.id}" }

                val folderEntity = FolderEntity(
                    id = localId,
                    name = sharedDto.title,
                    icon = sharedDto.icon ?: "ic_cartella",
                    color = sharedDto.color ?: "#FFFFFF",
                    description = sharedDto.description,
                    itemIds = compositeItemIds,
                    createdAt = existing?.createdAt ?: sharedDto.createdAt ?: Instant.now().toString(),
                    updatedAt = sharedDto.updatedAt ?: Instant.now().toString(),
                    syncStatus = "synced",
                    clientUpdatedAt = System.currentTimeMillis(),
                    isShared = true,
                    sharedFolderId = sharedId,
                    ownerUid = sharedDto.ownerUid,
                    ownerName = sharedDto.ownerName,
                    shareCode = sharedDto.shareCode,
                    maxItems = sharedDto.maxItems
                )
                folderDao.insert(folderEntity)
                fetchMissingItemsMetadata(sharedDto.items)
            }

            // 3. Auto-revert owner folders with 0 active invites and 0 other members
            val finalFolders = folderDao.getAll()
            finalFolders.filter { it.isShared && it.ownerUid == user.uid && it.sharedFolderId != null }
                .forEach { ownerFolder ->
                    checkAndAutoRevertIfEmpty(ownerFolder)
                }
        }
    }

    suspend fun getFolderMembers(sharedFolderId: String): Result<List<SharedFolderMemberDto>> = withContext(Dispatchers.IO) {
        runCatching {
            api.getFolderMembers("eq.$sharedFolderId")
        }
    }

    suspend fun removeMember(sharedFolderId: String, userUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            api.removeMember("eq.$sharedFolderId", "eq.$userUid")
            Unit
        }
    }

    suspend fun revokeInvite(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.revokeInvite("eq.$code")
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
            Unit
        }
    }

    suspend fun getFolderInvites(sharedFolderId: String): Result<List<SharedFolderInviteDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val invites = api.getFolderInvites("eq.$sharedFolderId")
            invites.filter { invite ->
                !invite.isRevoked &&
                (invite.maxUses == null || invite.usedCount < invite.maxUses) &&
                !invite.isExpired
            }
        }
    }

    suspend fun revertSharedFolderToPrivate(folder: FolderEntity): Result<FolderEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val sharedId = folder.sharedFolderId
            if (sharedId != null) {
                try {
                    api.deleteSharedFolder("eq.$sharedId")
                } catch (e: Exception) {
                    // Ignore network/server errors during remote deletion
                }
            }
            val privateFolder = folder.copy(
                isShared = false,
                sharedFolderId = null,
                shareCode = null,
                ownerUid = null,
                ownerName = null,
                syncStatus = "local",
                clientUpdatedAt = System.currentTimeMillis()
            )
            folderDao.insert(privateFolder)
            privateFolder
        }
    }

    suspend fun checkAndAutoRevertIfEmpty(folder: FolderEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val sharedId = folder.sharedFolderId ?: return@runCatching false
            val user = getCurrentUser() ?: return@runCatching false
            if (folder.ownerUid != user.uid) return@runCatching false

            val members = api.getFolderMembers("eq.$sharedId")
            val otherMembers = members.filter { it.userUid != user.uid }
            if (otherMembers.isNotEmpty()) return@runCatching false

            val allInvites = api.getFolderInvites("eq.$sharedId")
            val activeInvites = allInvites.filter { !it.isRevoked && (it.maxUses == null || it.usedCount < it.maxUses) && !it.isExpired }
            if (activeInvites.isNotEmpty()) return@runCatching false

            revertSharedFolderToPrivate(folder).getOrThrow()
            true
        }
    }

    suspend fun handleDeleteOrLeave(folder: FolderEntity): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sharedId = folder.sharedFolderId ?: return@runCatching
            val user = getCurrentUser() ?: return@runCatching

            if (folder.ownerUid == user.uid) {
                // Owner deletes the shared folder entirely from Supabase
                api.deleteSharedFolder("eq.$sharedId")
            } else {
                // Member leaves the folder
                api.removeMember("eq.$sharedId", "eq.${user.uid}")
            }
            Unit
        }
    }

    suspend fun syncProfileToSharedFolders(displayName: String, avatarUrl: String?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = getCurrentUser() ?: return@runCatching
            val now = Instant.now().toString()

            // 1. Update member records across all joined shared folders
            val memberUpdates = buildJsonObject {
                put("user_name", displayName)
                if (avatarUrl != null) {
                    put("user_avatar_url", avatarUrl)
                }
            }
            api.updateMemberProfile("eq.${user.uid}", memberUpdates)

            // 2. Update owner records across all owned shared folders
            val ownerUpdates = buildJsonObject {
                put("owner_name", displayName)
                if (avatarUrl != null) {
                    put("owner_avatar_url", avatarUrl)
                }
                put("updated_at", now)
            }
            api.updateSharedFoldersByOwner("eq.${user.uid}", ownerUpdates)

            // 3. Update local Room folder entities where user is owner
            val localFolders = folderDao.getAll()
            localFolders.filter { it.isShared && it.ownerUid == user.uid }.forEach { folder ->
                folderDao.insert(folder.copy(ownerName = displayName))
            }
            Unit
        }
    }

    companion object {
        const val MAX_FOLDER_MEMBERS = 25
    }
}
