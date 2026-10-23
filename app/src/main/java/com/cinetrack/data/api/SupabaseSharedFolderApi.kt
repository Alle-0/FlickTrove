package com.cinetrack.data.api

import com.cinetrack.data.model.SharedFolderDto
import com.cinetrack.data.model.SharedFolderInviteDto
import com.cinetrack.data.model.SharedFolderMemberDto
import com.cinetrack.data.model.UpdateItemsRequest
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.*

interface SupabaseSharedFolderApi {

    // 1. Folders
    @GET("rest/v1/shared_folders")
    suspend fun getSharedFolderByCode(
        @Query("share_code") shareCodeQuery: String, // e.g. "eq.FT-7K9P2"
        @Query("select") select: String = "*"
    ): List<SharedFolderDto>

    @GET("rest/v1/shared_folders")
    suspend fun getSharedFolderById(
        @Query("id") idQuery: String, // e.g. "eq.uuid"
        @Query("select") select: String = "*"
    ): List<SharedFolderDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/shared_folders")
    suspend fun createSharedFolder(
        @Body folder: SharedFolderDto
    ): List<SharedFolderDto>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/shared_folders")
    suspend fun updateSharedFolder(
        @Query("id") idQuery: String,
        @Body updates: JsonObject
    ): Response<Unit>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/shared_folders")
    suspend fun updateSharedFolderItems(
        @Query("id") idQuery: String,
        @Body request: UpdateItemsRequest
    ): Response<Unit>

    @DELETE("rest/v1/shared_folders")
    suspend fun deleteSharedFolder(
        @Query("id") idQuery: String
    ): Response<Unit>

    // 2. Members
    @GET("rest/v1/shared_folder_members")
    suspend fun getFolderMembers(
        @Query("folder_id") folderIdQuery: String,
        @Query("select") select: String = "*"
    ): List<SharedFolderMemberDto>

    @GET("rest/v1/shared_folder_members")
    suspend fun getUserSharedFolders(
        @Query("user_uid") userUidQuery: String,
        @Query("select") select: String = "*,shared_folders(*)"
    ): List<SharedFolderMemberDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/shared_folder_members")
    suspend fun addMember(
        @Body member: SharedFolderMemberDto
    ): List<SharedFolderMemberDto>

    @DELETE("rest/v1/shared_folder_members")
    suspend fun removeMember(
        @Query("folder_id") folderIdQuery: String,
        @Query("user_uid") userUidQuery: String
    ): Response<Unit>

    // 3. Invites
    @GET("rest/v1/shared_folder_invites")
    suspend fun getInviteByCode(
        @Query("code") codeQuery: String,
        @Query("select") select: String = "*"
    ): List<SharedFolderInviteDto>

    @GET("rest/v1/shared_folder_invites")
    suspend fun getFolderInvites(
        @Query("folder_id") folderIdQuery: String,
        @Query("select") select: String = "*"
    ): List<SharedFolderInviteDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/shared_folder_invites")
    suspend fun createInvite(
        @Body invite: SharedFolderInviteDto
    ): List<SharedFolderInviteDto>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/shared_folder_invites")
    suspend fun updateInvite(
        @Query("code") codeQuery: String,
        @Body updates: JsonObject
    ): Response<Unit>

    @DELETE("rest/v1/shared_folder_invites")
    suspend fun revokeInvite(
        @Query("code") codeQuery: String
    ): Response<Unit>
}
