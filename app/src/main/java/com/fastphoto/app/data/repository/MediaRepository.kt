package com.fastphoto.app.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.fastphoto.app.data.model.Album
import com.fastphoto.app.data.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for accessing media files through MediaStore API
 */
@Singleton
class MediaRepository @Inject constructor(
    private val contentResolver: ContentResolver
) {

    /**
     * Load all photo albums from MediaStore
     */
    suspend fun loadAlbums(): Result<List<Album>> = withContext(Dispatchers.IO) {
        try {
            val albums = mutableMapOf<String, Album>()

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val bucketId = cursor.getString(bucketIdColumn) ?: continue
                    val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown"
                    val id = cursor.getLong(idColumn)

                    if (!albums.containsKey(bucketId)) {
                        val thumbnailUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        albums[bucketId] = Album(
                            id = id,
                            name = bucketName,
                            thumbnailUri = thumbnailUri,
                            photoCount = 1,
                            bucketId = bucketId
                        )
                    } else {
                        albums[bucketId] = albums[bucketId]!!.copy(
                            photoCount = albums[bucketId]!!.photoCount + 1
                        )
                    }
                }
            }

            Result.success(albums.values.sortedByDescending { it.photoCount })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load photos from a specific album
     */
    suspend fun loadPhotosFromAlbum(bucketId: String): Result<List<Photo>> = withContext(Dispatchers.IO) {
        try {
            val photos = mutableListOf<Photo>()

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH
            )

            val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
            val selectionArgs = arrayOf(bucketId)
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                photos.addAll(extractPhotosFromCursor(cursor))
            }

            Result.success(photos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load all photos from device
     */
    suspend fun loadAllPhotos(): Result<List<Photo>> = withContext(Dispatchers.IO) {
        try {
            val photos = mutableListOf<Photo>()

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                photos.addAll(extractPhotosFromCursor(cursor))
            }

            Result.success(photos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Copy a photo to a target album by inserting a new MediaStore entry and
     * streaming bytes into it. The original photo is NOT touched here — the
     * caller is responsible for hiding/trashing it. Returns the new Uri so the
     * caller can confirm placement (e.g., switch the viewer to that album).
     *
     * Insert + write goes through the app's own permissions; no system
     * confirmation dialog is required.
     */
    suspend fun copyPhotoToAlbum(photo: Photo, targetAlbumName: String): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val values = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, photo.displayName)
                put(MediaStore.Images.Media.MIME_TYPE, photo.mimeType)
                if (photo.width > 0) put(MediaStore.Images.Media.WIDTH, photo.width)
                if (photo.height > 0) put(MediaStore.Images.Media.HEIGHT, photo.height)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$targetAlbumName/")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val newUri = contentResolver.insert(collection, values)
                ?: return@withContext Result.failure(Exception("MediaStore kaydı oluşturulamadı"))

            contentResolver.openInputStream(photo.uri)?.use { input ->
                contentResolver.openOutputStream(newUri)?.use { output ->
                    input.copyTo(output)
                } ?: return@withContext Result.failure(Exception("Hedef akış açılamadı"))
            } ?: return@withContext Result.failure(Exception("Kaynak fotoğraf okunamadı"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pendingClear = android.content.ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                contentResolver.update(newUri, pendingClear, null, null)
            }

            Result.success(newUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractPhotosFromCursor(cursor: Cursor): List<Photo> {
        val photos = mutableListOf<Photo>()

        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
        val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
        val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
        val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )

            val photo = Photo(
                id = id,
                uri = uri,
                displayName = cursor.getString(displayNameColumn),
                dateAdded = cursor.getLong(dateAddedColumn),
                dateTaken = cursor.getLongOrNull(dateTakenColumn),
                size = cursor.getLong(sizeColumn),
                mimeType = cursor.getString(mimeTypeColumn),
                width = cursor.getInt(widthColumn),
                height = cursor.getInt(heightColumn),
                bucketId = cursor.getString(bucketIdColumn) ?: "",
                bucketDisplayName = cursor.getString(bucketNameColumn) ?: "Unknown",
                relativePath = cursor.getStringOrNull(relativePathColumn)
            )

            photos.add(photo)
        }

        return photos
    }

    private fun Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (isNull(columnIndex)) null else getLong(columnIndex)
    }

    private fun Cursor.getStringOrNull(columnIndex: Int): String? {
        return if (isNull(columnIndex)) null else getString(columnIndex)
    }
}
