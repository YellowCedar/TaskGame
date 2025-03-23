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
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Delete
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity
data class Task(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,
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
)

@Entity
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val tagId: Int = 0,
    val name: String,
    val color: String = "#FF6200EE", // Default color
    val content: String = "", // Content associated with this tag
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    primaryKeys = ["taskId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["uid"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["tagId"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("taskId"),
        Index("tagId")
    ]
)
data class TaskTagCrossRef(
    val taskId: Int,
    val tagId: Int
)

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
    
    @Query("SELECT * FROM task WHERE createdAt BETWEEN :startOfDay AND :endOfDay ORDER BY createdAt DESC")
    fun getTasksByDate(startOfDay: Long, endOfDay: Long): Flow<List<Task>>
    
    @Insert
    suspend fun insertTask(item: Task): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTask(item: Task)
    
    @Query("DELETE FROM task WHERE uid = :taskId")
    suspend fun deleteTask(taskId: Int)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tag ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tag WHERE tagId = :tagId")
    fun getTagById(tagId: Int): Flow<Tag>

    @Insert
    suspend fun insertTag(tag: Tag): Long

    @Update
    suspend fun updateTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("SELECT * FROM tag WHERE name LIKE :query ORDER BY name ASC")
    suspend fun searchTags(query: String): List<Tag>
}

@Dao
interface TaskTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crossRef: TaskTagCrossRef)

    @Delete
    suspend fun delete(crossRef: TaskTagCrossRef)
    
    @Query("DELETE FROM tasktagcrossref WHERE taskId = :taskId")
    suspend fun deleteAllTagsForTask(taskId: Int)

    @Transaction
    @Query("SELECT * FROM tag INNER JOIN tasktagcrossref ON tag.tagId = tasktagcrossref.tagId WHERE tasktagcrossref.taskId = :taskId")
    fun getTagsForTask(taskId: Int): Flow<List<Tag>>

    @Transaction
    @Query("SELECT * FROM task INNER JOIN tasktagcrossref ON task.uid = tasktagcrossref.taskId WHERE tasktagcrossref.tagId = :tagId")
    fun getTasksWithTag(tagId: Int): Flow<List<Task>>
}
