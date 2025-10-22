package com.readle.app.di

import android.content.Context
import androidx.room.Room
import com.readle.app.data.database.BookDao
import com.readle.app.data.database.ReadleDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideReadleDatabase(@ApplicationContext context: Context): ReadleDatabase {
        return Room.databaseBuilder(
            context,
            ReadleDatabase::class.java,
            ReadleDatabase.DATABASE_NAME
        )
            .addMigrations(
                ReadleDatabase.MIGRATION_1_2,
                ReadleDatabase.MIGRATION_2_3,
                ReadleDatabase.MIGRATION_3_4,
                ReadleDatabase.MIGRATION_4_5,
                ReadleDatabase.MIGRATION_5_6,
                ReadleDatabase.MIGRATION_6_7,
                ReadleDatabase.MIGRATION_7_8,
                ReadleDatabase.MIGRATION_8_9,
                ReadleDatabase.MIGRATION_9_10
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideBookDao(database: ReadleDatabase): BookDao {
        return database.bookDao()
    }
}

