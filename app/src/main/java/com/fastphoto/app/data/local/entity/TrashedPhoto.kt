package com.fastphoto.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing metadata of photos moved to trash
 */
@Entity(tableName = "trashed_photos")
data class TrashedPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Original photo information
    val originalPhotoId: Long,
    val originalUri: String,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val width: Int,
    val height: Int,

    // Original location info
    val originalBucketId: String,
    val originalBucketDisplayName: String,
    val originalRelativePath: String?,

    // Trash location
    val trashFilePath: String,

    // Timestamps
    val dateTrashed: Long,
    val dateAdded: Long,
    val dateTaken: Long?
)
