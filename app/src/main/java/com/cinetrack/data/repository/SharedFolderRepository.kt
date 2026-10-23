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
import javax.inject.Inject
import javax.inject.Singleton

enum class InviteDurationOption(val minutes: Long?, val maxUses: Int?) {
    ONE_MINUTE(1L, null),
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

    private fun getCurrentUser() = auth.currentUser

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
            val formattedCode = code.trim().uppercase()
            val invites = api.getInviteByCode("eq.$formattedCode")
            val invite = invites.firstOrNull() ?: throw IllegalStateException("INVITE_NOT_FOUND")

            if (invite.isRevoked) {
                throw IllegalStateException("INVITE_REVOKED")
            }

            val expiresAtInstant = try {
                Instant.parse(invite.expiresAt)
            } catch (e: Exception) {
                Instant.MAX
            }

            if (Instant.now().isAfter(expiresAtInstant)) {
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

            // Check members count limit (10 members)
            val currentMembers = api.getFolderMembers("eq.$sharedId")
            val alreadyMember = currentMembers.any { it.userUid == user.uid }

            if (!alreadyMember) {
                if (currentMembers.size >= 10) {
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

    suspend fun pushFolderUpdate(folderEntity: FolderEntity): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sharedId = folderEntity.sharedFolderId ?: return@runCatching
            val user = getCurrentUser() ?: return@runCatching

            if (folderEntity.itemIds.size > folderEntity.maxItems) {
                throw IllegalStateException("MAX_ITEMS_REACHED")
            }

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
                        addedBy = user.displayName ?: "Membro",
                        addedAt = System.currentTimeMillis()
                    )
                } else null
            }

            api.updateSharedFolderItems(
                "eq.$sharedId",
                UpdateItemsRequest(
                    items = itemsDto,
                    updatedAt = now
                )
            )

            // If title, description or color changed, update folder metadata too
            api.updateSharedFolder(
                "eq.$sharedId",
                buildJsonObject {
                    put("title", folderEntity.name)
                    folderEntity.description?.let { put("description", it) }
                    folderEntity.color?.let { put("color", it) }
                    folderEntity.icon?.let { put("icon", it) }
                    put("updated_at", now)
                }
            )
        }
    }

    suspend fun syncUserSharedFolders(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = getCurrentUser() ?: return@runCatching
            val memberships = api.getUserSharedFolders("eq.${user.uid}")

            memberships.forEach { membership ->
                val sharedDto = membership.sharedFolder ?: return@forEach
                val sharedId = sharedDto.id ?: return@forEach
                val localId = "shared_${sharedId}"
                val compositeItemIds = sharedDto.items.map { "${it.mediaType}_${it.id}" }

                val folderEntity = FolderEntity(
                    id = localId,
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
                fetchMissingItemsMetadata(sharedDto.items)
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
            api.updateInvite(
                "eq.$code",
                buildJsonObject {
                    put("is_revoked", true)
                }
            )
            Unit
        }
    }

    suspend fun getFolderInvites(sharedFolderId: String): Result<List<SharedFolderInviteDto>> = withContext(Dispatchers.IO) {
        runCatching {
            api.getFolderInvites("eq.$sharedFolderId")
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
}
