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
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity
data class Task(
    val name: String,
    val maxPoints: Int = 0,
    val estimatedDurationMinutes: Int = 0,
    val state: TaskState = TaskState.NOT_STARTED,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val pauseTime: Long? = null,
    val totalElapsedTime: Long = 0, // In milliseconds
    val rating: Int? = null, // 1-5 stars
    val reflection: String = "",
    val earnedPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0
}

enum class TaskState {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM task ORDER BY createdAt DESC")
    fun getTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM task WHERE uid = :taskId")
    fun getTask(taskId: Int): Flow<Task>
    
    @Insert
    suspend fun insertTask(item: Task): Long
    
    @Update
    suspend fun updateTask(item: Task)
    
    @Query("DELETE FROM task WHERE uid = :taskId")
    suspend fun deleteTask(taskId: Int)
}
