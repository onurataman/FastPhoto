package com.fastphoto.app.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.fastphoto.app.data.local.dao.TrashedPhotoDao
import com.fastphoto.app.data.local.entity.TrashedPhoto
import com.fastphoto.app.data.model.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrashRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trashedPhotoDao: TrashedPhotoDao,
    private val contentResolver: ContentResolver,
    private val pendingIntentBus: PendingIntentBus
) {

    private val trashDir: File
        get() = File(context.getExternalFilesDir(null), "trash").also {
            if (!it.exists()) it.mkdirs()
        }

    fun getTrashedPhotos(): Flow<List<TrashedPhoto>> {
        return trashedPhotoDao.getAllTrashedPhotos()
    }

    suspend fun moveToTrash(photo: Photo): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!trashDir.exists()) trashDir.mkdirs()

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
            // Foto MediaStore'da kalır. App içinde gizlenir (ViewModel filter).
            // Sistemden silmek için TrashScreen "Toplu Sil" → bulkDeleteFromSystem.
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restorePhoto(trashedPhoto: TrashedPhoto): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Foto MediaStore'da hala var (silmedik). Sadece DB'den çıkar +
            // trash dosyasını sil → ViewModel filter'ı kalkacak, foto tekrar görünür.
            File(trashedPhoto.trashFilePath).delete()
            trashedPhotoDao.deleteTrashedPhoto(trashedPhoto)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePermanently(trashedPhoto: TrashedPhoto): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Tek fotoğraf için de yine MediaStore'dan silmek istiyoruz —
            // bulk delete ile aynı akışı kullan (tek elemanlı liste).
            bulkDeleteFromSystem(listOf(trashedPhoto))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun emptyTrash(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val all = trashedPhotoDao.getAllTrashedPhotos().first()
            bulkDeleteFromSystem(all)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bulkDeleteFromSystem(photos: List<TrashedPhoto>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (photos.isEmpty()) return@withContext Result.success(Unit)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val uris = photos.map { Uri.parse(it.originalUri) }
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                pendingIntentBus.emit(pendingIntent.intentSender)
                // Sistem dialog'u kullanıcıdan onay alacak. Onaylanırsa MediaStore
                // foto'ları siler. Local trash dosyaları + DB kayıtlarını burada
                // hemen temizliyoruz çünkü kullanıcı zaten "kalıcı sil" niyeti
                // bildirmiş; foto sistemden silinmezse bile app'ten kalıcı kaldırılır.
            } else {
                photos.forEach { photo ->
                    try { contentResolver.delete(Uri.parse(photo.originalUri), null, null) }
                    catch (e: Exception) { e.printStackTrace() }
                }
            }

            photos.forEach { photo ->
                File(photo.trashFilePath).delete()
                trashedPhotoDao.deleteTrashedPhoto(photo)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun autoCleanup(daysOld: Int = 30): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
            val deletedCount = trashedPhotoDao.deletePhotosOlderThan(cutoffTime)

            trashDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) file.delete()
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
