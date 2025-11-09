package com.fastphoto.app.data.model

import android.net.Uri

/**
 * Represents a photo album with its metadata
 */
data class Album(
    val id: Long,
    val name: String,
    val thumbnailUri: Uri?,
    val photoCount: Int,
    val bucketId: String
)
