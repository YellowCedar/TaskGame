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

import com.github.cedaryellow.data.local.database.Task
import com.github.cedaryellow.data.local.database.TaskDao
import com.github.cedaryellow.data.local.database.TaskState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.sqrt

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
//        val latestTask = taskDao.getTask(taskId).first()
//        Log.d("DEBUG", "Latest Task State: ${latestTask.state}")
//        Log.i("updateTask", updatedTask.toString())
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
        //todo rating should be modified
//        val earnedPoints = (task.maxPoints * rating) / 5
        //val earnedPoints = 100
        val earnedPoints = calculatePoints(100, task.estimatedDurationMinutes, task.totalElapsedTime, rating)
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

    private fun calculatePoints(
        basePoints: Int,      // 基础积分 B
        idealDuration: Int,   // 理想时长 T_ideal（分钟）
        actualDuration: Long,  // 实际耗时 T（分钟）
        selfRating: Int       // 自我评价 S（1-5星）
    ): Int {
        require(selfRating in 1..5) { "自我评价必须是 1-5 星" }

        // 1. 计算自我评价系数
        val sCoeff = 0.6 + 0.08 * selfRating

        // 2. 计算时长系数
        val ratio = actualDuration.toDouble() / idealDuration
        val tCoeff = when {
            ratio < 0.5 -> 0.5
            ratio > 2.0 -> 1.5
            else -> sqrt(ratio)
        }

        // 3. 计算并四舍五入为整数
        return (basePoints * sCoeff * tCoeff).roundToInt()
    }
}
