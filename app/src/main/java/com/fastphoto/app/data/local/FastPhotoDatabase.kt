package com.fastphoto.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fastphoto.app.data.local.dao.TrashedPhotoDao
import com.fastphoto.app.data.local.entity.TrashedPhoto

/**
 * Room Database for FastPhoto app
 */
@Database(
    entities = [TrashedPhoto::class],
    version = 1,
    exportSchema = false
)
abstract class FastPhotoDatabase : RoomDatabase() {
    abstract fun trashedPhotoDao(): TrashedPhotoDao

    companion object {
        const val DATABASE_NAME = "fastphoto_database"
    }
}
