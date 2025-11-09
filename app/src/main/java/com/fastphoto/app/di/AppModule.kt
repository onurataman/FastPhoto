package com.fastphoto.app.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.fastphoto.app.data.local.FastPhotoDatabase
import com.fastphoto.app.data.local.dao.TrashedPhotoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for app-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(
        @ApplicationContext context: Context
    ): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun provideFastPhotoDatabase(
        @ApplicationContext context: Context
    ): FastPhotoDatabase {
        return Room.databaseBuilder(
            context,
            FastPhotoDatabase::class.java,
            FastPhotoDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideTrashedPhotoDao(
        database: FastPhotoDatabase
    ): TrashedPhotoDao {
        return database.trashedPhotoDao()
    }
}
