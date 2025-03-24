/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.cedaryellow.data.local.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.cedaryellow.data.local.database.AppDatabase
import com.github.cedaryellow.data.local.database.TaskDao
import com.github.cedaryellow.data.local.database.TagDao
import com.github.cedaryellow.data.local.database.TaskTagDao
import com.github.cedaryellow.data.local.database.FragmentTimeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Migration from version 1 to 2
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create Tag table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `Tag` (
                `tagId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `color` TEXT NOT NULL DEFAULT '#FF6200EE',
                `content` TEXT NOT NULL DEFAULT '',
                `createdAt` INTEGER NOT NULL DEFAULT 0
            )
            """
        )
        
        // Create TaskTagCrossRef table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `TaskTagCrossRef` (
                `taskId` INTEGER NOT NULL,
                `tagId` INTEGER NOT NULL,
                PRIMARY KEY(`taskId`, `tagId`),
                FOREIGN KEY(`taskId`) REFERENCES `Task`(`uid`) ON DELETE CASCADE,
                FOREIGN KEY(`tagId`) REFERENCES `Tag`(`tagId`) ON DELETE CASCADE
            )
            """
        )
        
        // Create indices for TaskTagCrossRef table
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_TaskTagCrossRef_taskId` ON `TaskTagCrossRef` (`taskId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_TaskTagCrossRef_tagId` ON `TaskTagCrossRef` (`tagId`)")
    }
}

// Migration from version 2 to 3
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create the FragmentTime table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `FragmentTime` (
              `uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `name` TEXT NOT NULL,
              `state` TEXT NOT NULL,
              `startTime` INTEGER,
              `endTime` INTEGER,
              `pauseTime` INTEGER,
              `totalElapsedTime` INTEGER NOT NULL,
              `rating` INTEGER,
              `reflection` TEXT NOT NULL,
              `earnedPoints` INTEGER NOT NULL,
              `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao {
        return appDatabase.taskDao()
    }
    
    @Provides
    fun provideTagDao(appDatabase: AppDatabase): TagDao {
        return appDatabase.tagDao()
    }
    
    @Provides
    fun provideTaskTagDao(appDatabase: AppDatabase): TaskTagDao {
        return appDatabase.taskTagDao()
    }
    
    @Provides
    fun provideFragmentTimeDao(appDatabase: AppDatabase): FragmentTimeDao {
        return appDatabase.fragmentTimeDao()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "Task"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        //.fallbackToDestructiveMigration() // Only for development
        .build()
    }
}
