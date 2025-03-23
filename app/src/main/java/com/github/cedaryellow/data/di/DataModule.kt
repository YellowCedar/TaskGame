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

package com.github.cedaryellow.data.di

import com.github.cedaryellow.data.DefaultTaskRepository
import com.github.cedaryellow.data.TaskRepository
import com.github.cedaryellow.data.DefaultTagRepository
import com.github.cedaryellow.data.TagRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Singleton
    @Binds
    fun bindsTaskRepository(
        taskRepository: DefaultTaskRepository
    ): TaskRepository

}

///**
// * Fake implementation of TaskRepository for testing purposes
// */
//class FakeTaskRepository @Inject constructor() : TaskRepository {
//    private val fakeTasks = mutableListOf(
//        Task(name = "Task One", maxPoints = 10, estimatedDurationMinutes = 30),
//        Task(name = "Task Two", maxPoints = 20, estimatedDurationMinutes = 60),
//        Task(name = "Task Three", maxPoints = 30, estimatedDurationMinutes = 90)
//    ).apply {
//        // Assign IDs to simulate database behavior
//        forEachIndexed { index, task -> task.uid = index + 1 }
//    }
//
//    override val tasks: Flow<List<Task>> = flowOf(fakeTasks)
//
//    override fun getTask(taskId: Int): Flow<Task> {
//        val task = fakeTasks.find { it.uid == taskId } ?: Task(name = "Not Found")
//        return flowOf(task)
//    }
//
//    override suspend fun addTask(name: String, maxPoints: Int, estimatedDurationMinutes: Int): Long {
//        val newId = (fakeTasks.maxOfOrNull { it.uid } ?: 0) + 1
//        val newTask = Task(name = name, maxPoints = maxPoints, estimatedDurationMinutes = estimatedDurationMinutes).apply {
//            uid = newId
//        }
//        fakeTasks.add(newTask)
//        return newId.toLong()
//    }
//
//    override suspend fun updateTask(task: Task) {
//        val index = fakeTasks.indexOfFirst { it.uid == task.uid }
//        if (index != -1) {
//            fakeTasks[index] = task
//        }
//    }
//
//    override suspend fun deleteTask(taskId: Int) {
//        fakeTasks.removeIf { it.uid == taskId }
//    }
//
//    override suspend fun startTask(taskId: Int) {
//        val index = fakeTasks.indexOfFirst { it.uid == taskId }
//        if (index != -1) {
//            fakeTasks[index] = fakeTasks[index].copy(
//                state = TaskState.IN_PROGRESS,
//                startTime = System.currentTimeMillis()
//            )
//        }
//    }
//
//    override suspend fun pauseTask(taskId: Int) {
//        val index = fakeTasks.indexOfFirst { it.uid == taskId }
//        if (index != -1) {
//            val task = fakeTasks[index]
//            val currentTime = System.currentTimeMillis()
//            val elapsedSinceStart = task.startTime?.let { currentTime - it } ?: 0
//            fakeTasks[index] = task.copy(
//                state = TaskState.PAUSED,
//                pauseTime = currentTime,
//                totalElapsedTime = task.totalElapsedTime + elapsedSinceStart
//            )
//        }
//    }
//
//    override suspend fun completeTask(taskId: Int, rating: Int, reflection: String) {
//        val index = fakeTasks.indexOfFirst { it.uid == taskId }
//        if (index != -1) {
//            val task = fakeTasks[index]
//            val currentTime = System.currentTimeMillis()
//
//            // Calculate elapsed time
//            val elapsedTime = when(task.state) {
//                TaskState.IN_PROGRESS -> {
//                    val elapsedSinceStart = task.startTime?.let { currentTime - it } ?: 0
//                    task.totalElapsedTime + elapsedSinceStart
//                }
//                TaskState.PAUSED -> task.totalElapsedTime
//                else -> task.totalElapsedTime
//            }
//
//            // Calculate points based on rating
//            val earnedPoints = (task.maxPoints * rating) / 5
//
//            fakeTasks[index] = task.copy(
//                state = TaskState.COMPLETED,
//                endTime = currentTime,
//                totalElapsedTime = elapsedTime,
//                rating = rating,
//                reflection = reflection,
//                earnedPoints = earnedPoints
//            )
//        }
//    }
//}
