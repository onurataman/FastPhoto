package com.fastphoto.app.data.model

import android.net.Uri

/**
 * Represents a photo with its metadata
 */
data class Photo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val dateTaken: Long?,
    val size: Long,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val bucketId: String,
    val bucketDisplayName: String,
    val relativePath: String?
)
