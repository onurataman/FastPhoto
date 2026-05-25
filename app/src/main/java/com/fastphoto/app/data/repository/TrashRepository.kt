package com.fastphoto.app.data.repository

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.fastphoto.app.data.local.dao.TrashedPhotoDao
import com.fastphoto.app.data.local.entity.TrashedPhoto
import com.fastphoto.app.data.model.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrashRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trashedPhotoDao: TrashedPhotoDao,
    private val contentResolver: ContentResolver
) {

    private val trashDir: File
        get() = File(context.getExternalFilesDir(null), "trash").also {
            if (!it.exists()) it.mkdirs()
        }

    // Android 11+ user-confirmation deletion requests are forwarded to the
    // Activity layer through this flow.
    private val _pendingDeletion = MutableSharedFlow<IntentSender>(extraBufferCapacity = 4)
    val pendingDeletion: SharedFlow<IntentSender> = _pendingDeletion.asSharedFlow()

    fun getTrashedPhotos(): Flow<List<TrashedPhoto>> {
        return trashedPhotoDao.getAllTrashedPhotos()
    }

    suspend fun moveToTrash(photo: Photo): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!trashDir.exists()) {
                trashDir.mkdirs()
            }

            val timestamp = System.currentTimeMillis()
            val fileName = "${timestamp}_${photo.displayName}"
            val trashFile = File(trashDir, fileName)

            contentResolver.openInputStream(photo.uri)?.use { inputStream ->
                FileOutputStream(trashFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val trashedPhoto = TrashedPhoto(
                originalPhotoId = photo.id,
                originalUri = photo.uri.toString(),
                displayName = photo.displayName,
                mimeType = photo.mimeType,
                size = photo.size,
                width = photo.width,
                height = photo.height,
                originalBucketId = photo.bucketId,
                originalBucketDisplayName = photo.bucketDisplayName,
                originalRelativePath = photo.relativePath,
                trashFilePath = trashFile.absolutePath,
                dateTrashed = timestamp,
                dateAdded = photo.dateAdded,
                dateTaken = photo.dateTaken
            )

            trashedPhotoDao.insertTrashedPhoto(trashedPhoto)

            deleteFromMediaStore(photo.uri)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restorePhoto(trashedPhoto: TrashedPhoto): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(trashedPhoto.trashFilePath)

            if (!trashFile.exists()) {
                return@withContext Result.failure(Exception("Trash file not found"))
            }

            val values = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, trashedPhoto.displayName)
                put(MediaStore.Images.Media.MIME_TYPE, trashedPhoto.mimeType)
                put(MediaStore.Images.Media.WIDTH, trashedPhoto.width)
                put(MediaStore.Images.Media.HEIGHT, trashedPhoto.height)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, trashedPhoto.originalRelativePath ?: "Pictures/")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val uri = contentResolver.insert(collection, values)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                trashFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }

            trashFile.delete()
            trashedPhotoDao.deleteTrashedPhoto(trashedPhoto)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePermanently(trashedPhoto: TrashedPhoto): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(trashedPhoto.trashFilePath)
            trashFile.delete()
            trashedPhotoDao.deleteTrashedPhoto(trashedPhoto)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun emptyTrash(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            trashDir.listFiles()?.forEach { it.delete() }
            trashedPhotoDao.deleteAllTrashedPhotos()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun deleteFromMediaStore(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: createDeleteRequest returns a PendingIntent whose
            // IntentSender must be launched from an Activity to show the
            // system confirmation dialog.
            try {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, listOf(uri))
                _pendingDeletion.emit(pendingIntent.intentSender)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                contentResolver.delete(uri, null, null)
            } catch (e: RecoverableSecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        _pendingDeletion.emit(e.userAction.actionIntent.intentSender)
                    } catch (ignored: Exception) {
                        ignored.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun autoCleanup(daysOld: Int = 30): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
            val deletedCount = trashedPhotoDao.deletePhotosOlderThan(cutoffTime)

            trashDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
