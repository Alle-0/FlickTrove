package com.cinetrack.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val description: String? = null,
    
    @SerialName("item_ids")
    @ColumnInfo(name = "item_ids") val itemIds: List<String> = emptyList(),
    
    @SerialName("created_at")
    @ColumnInfo(name = "created_at") val createdAt: String,
    
    @SerialName("updated_at")
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    
    @SerialName("sync_status")
    @ColumnInfo(name = "sync_status") val syncStatus: String = "synced",
    
    @SerialName("client_updated_at")
    @ColumnInfo(name = "client_updated_at") val clientUpdatedAt: Long = 0L
)
