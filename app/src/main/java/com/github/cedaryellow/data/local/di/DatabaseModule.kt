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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Migration from version 1 to 2
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns to Task table
//        database.execSQL("ALTER TABLE Task ADD COLUMN maxPoints INTEGER NOT NULL DEFAULT 0")
//        database.execSQL("ALTER TABLE Task ADD COLUMN estimatedDurationMinutes INTEGER NOT NULL DEFAULT 0")
//        database.execSQL("ALTER TABLE Task ADD COLUMN state TEXT NOT NULL DEFAULT 'NOT_STARTED'")
//        database.execSQL("ALTER TABLE Task ADD COLUMN startTime INTEGER DEFAULT NULL")
//        database.execSQL("ALTER TABLE Task ADD COLUMN endTime INTEGER DEFAULT NULL")
//        database.execSQL("ALTER TABLE Task ADD COLUMN pauseTime INTEGER DEFAULT NULL")
//        database.execSQL("ALTER TABLE Task ADD COLUMN totalElapsedTime INTEGER NOT NULL DEFAULT 0")
//        database.execSQL("ALTER TABLE Task ADD COLUMN rating INTEGER DEFAULT NULL")
//        database.execSQL("ALTER TABLE Task ADD COLUMN reflection TEXT NOT NULL DEFAULT ''")
//        database.execSQL("ALTER TABLE Task ADD COLUMN earnedPoints INTEGER NOT NULL DEFAULT 0")
//        database.execSQL("ALTER TABLE Task ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
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
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "Task"
        )
        //.addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigration() // Only for development
        .build()
    }
}
