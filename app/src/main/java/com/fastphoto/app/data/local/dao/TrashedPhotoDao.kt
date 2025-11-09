package com.fastphoto.app.data.local.dao

import androidx.room.*
import com.fastphoto.app.data.local.entity.TrashedPhoto
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TrashedPhoto entity
 */
@Dao
interface TrashedPhotoDao {

    @Query("SELECT * FROM trashed_photos ORDER BY dateTrashed DESC")
    fun getAllTrashedPhotos(): Flow<List<TrashedPhoto>>

    @Query("SELECT * FROM trashed_photos WHERE id = :id")
    suspend fun getTrashedPhotoById(id: Long): TrashedPhoto?

    @Query("SELECT * FROM trashed_photos WHERE originalPhotoId = :originalPhotoId")
    suspend fun getTrashedPhotoByOriginalId(originalPhotoId: Long): TrashedPhoto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashedPhoto(photo: TrashedPhoto): Long

    @Delete
    suspend fun deleteTrashedPhoto(photo: TrashedPhoto)

    @Query("DELETE FROM trashed_photos WHERE id = :id")
    suspend fun deleteTrashedPhotoById(id: Long)

    @Query("DELETE FROM trashed_photos")
    suspend fun deleteAllTrashedPhotos()

    @Query("SELECT COUNT(*) FROM trashed_photos")
    suspend fun getTrashedPhotoCount(): Int

    // Delete photos older than specified timestamp (for auto-cleanup)
    @Query("DELETE FROM trashed_photos WHERE dateTrashed < :timestamp")
    suspend fun deletePhotosOlderThan(timestamp: Long): Int
}
