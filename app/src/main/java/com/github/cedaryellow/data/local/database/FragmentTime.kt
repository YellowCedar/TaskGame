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

package com.github.cedaryellow.data.local.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Entity
data class FragmentTime(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,
    val name: String,
    val state: TaskState = TaskState.NOT_STARTED,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val pauseTime: Long? = null,
    val totalElapsedTime: Long = 0, // In milliseconds
    val rating: Int? = null, // 1-5 stars
    val reflection: String = "",
    val earnedPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface FragmentTimeDao {
    @Query("SELECT * FROM fragmenttime ORDER BY createdAt DESC")
    fun getAllFragmentTimes(): Flow<List<FragmentTime>>
    
    @Query("SELECT * FROM fragmenttime WHERE uid = :fragmentTimeId")
    fun getFragmentTime(fragmentTimeId: Int): Flow<FragmentTime>
    
    @Insert
    suspend fun insertFragmentTime(item: FragmentTime): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateFragmentTime(item: FragmentTime)
    
    @Delete
    suspend fun deleteFragmentTime(item: FragmentTime)
} 