package com.cinetrack.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SharedFolderDto(
    val id: String? = null,
    @SerialName("share_code") val shareCode: String,
    @SerialName("owner_uid") val ownerUid: String,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("owner_avatar_url") val ownerAvatarUrl: String? = null,
    val title: String,
    val description: String? = null,
    val color: String? = "#FFFFFF",
    val icon: String? = "ic_cartella",
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("max_items") val maxItems: Int = 1000,
    val items: List<SharedFolderItemDto> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SharedFolderItemDto(
    val id: Long,
    @SerialName("media_type") val mediaType: String,
    val title: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("added_by") val addedBy: String? = null,
    @SerialName("added_at") val addedAt: Long = System.currentTimeMillis()
)

@Serializable
data class SharedFolderMemberDto(
    val id: String? = null,
    @SerialName("folder_id") val folderId: String,
    @SerialName("user_uid") val userUid: String,
    @SerialName("user_name") val userName: String,
    @SerialName("user_avatar_url") val userAvatarUrl: String? = null,
    val role: String = "editor",
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("shared_folders") val sharedFolder: SharedFolderDto? = null
) {
    val userId: String get() = userUid
    val displayName: String get() = userName
}

@Serializable
data class SharedFolderInviteDto(
    val code: String,
    @SerialName("folder_id") val folderId: String,
    @SerialName("created_by_uid") val createdByUid: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("max_uses") val maxUses: Int? = null,
    @SerialName("used_count") val usedCount: Int = 0,
    @SerialName("is_revoked") val isRevoked: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
) {
    val inviteCode: String get() = code

    fun getExpiresAtInstant(): java.time.Instant? {
        return try {
            java.time.Instant.parse(expiresAt)
        } catch (e: Exception) {
            try {
                java.time.OffsetDateTime.parse(expiresAt).toInstant()
            } catch (e2: Exception) {
                try {
                    val clean = expiresAt.replace(' ', 'T')
                    java.time.OffsetDateTime.parse(clean).toInstant()
                } catch (e3: Exception) {
                    null
                }
            }
        }
    }

    val isExpired: Boolean get() {
        val exp = getExpiresAtInstant() ?: return false
        return java.time.Instant.now().isAfter(exp)
    }
}

@Serializable
data class UpdateItemsRequest(
    val items: List<SharedFolderItemDto>,
    @SerialName("updated_at") val updatedAt: String
)
