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

package com.github.cedaryellow.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.github.cedaryellow.data.local.database.Task
import com.github.cedaryellow.data.local.database.TaskDao
import com.github.cedaryellow.data.local.database.TaskState
import javax.inject.Inject

interface TaskRepository {
    val tasks: Flow<List<Task>>
    
    fun getTask(taskId: Int): Flow<Task>
    
    suspend fun addTask(name: String, maxPoints: Int, estimatedDurationMinutes: Int): Long
    
    suspend fun updateTask(task: Task)
    
    suspend fun deleteTask(taskId: Int)
    
    suspend fun startTask(taskId: Int)
    
    suspend fun pauseTask(taskId: Int)
    
    suspend fun completeTask(taskId: Int, rating: Int, reflection: String)
}

class DefaultTaskRepository @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override val tasks: Flow<List<Task>> = taskDao.getTasks()

    override fun getTask(taskId: Int): Flow<Task> = taskDao.getTask(taskId)

    override suspend fun addTask(
        name: String, 
        maxPoints: Int, 
        estimatedDurationMinutes: Int
    ): Long {
        val task = Task(
            name = name, 
            maxPoints = maxPoints, 
            estimatedDurationMinutes = estimatedDurationMinutes
        )
        return taskDao.insertTask(task)
    }
    
    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }
    
    override suspend fun deleteTask(taskId: Int) {
        taskDao.deleteTask(taskId)
    }
    
    override suspend fun startTask(taskId: Int) {
        val task = taskDao.getTask(taskId).first()
        val updatedTask = task.copy(
            state = TaskState.IN_PROGRESS,
            startTime = System.currentTimeMillis()
        )
        taskDao.updateTask(updatedTask)
    }
    
    override suspend fun pauseTask(taskId: Int) {
        val task = taskDao.getTask(taskId).first()
        val currentTime = System.currentTimeMillis()
        val elapsedSinceStart = task.startTime?.let { currentTime - it } ?: 0
        val updatedTask = task.copy(
            state = TaskState.PAUSED,
            pauseTime = currentTime,
            totalElapsedTime = task.totalElapsedTime + elapsedSinceStart
        )
        taskDao.updateTask(updatedTask)
    }
    
    override suspend fun completeTask(taskId: Int, rating: Int, reflection: String) {
        val task = taskDao.getTask(taskId).first()
        val currentTime = System.currentTimeMillis()
        
        // Calculate elapsed time
        val elapsedTime = when(task.state) {
            TaskState.IN_PROGRESS -> {
                val elapsedSinceStart = task.startTime?.let { currentTime - it } ?: 0
                task.totalElapsedTime + elapsedSinceStart
            }
            TaskState.PAUSED -> task.totalElapsedTime
            else -> task.totalElapsedTime
        }
        
        // Calculate points based on rating
        val earnedPoints = (task.maxPoints * rating) / 5
        
        val updatedTask = task.copy(
            state = TaskState.COMPLETED,
            endTime = currentTime,
            totalElapsedTime = elapsedTime,
            rating = rating,
            reflection = reflection,
            earnedPoints = earnedPoints
        )
        taskDao.updateTask(updatedTask)
    }
}
