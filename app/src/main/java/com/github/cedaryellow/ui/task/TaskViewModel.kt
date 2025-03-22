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

package com.github.cedaryellow.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.cedaryellow.data.TaskRepository
import com.github.cedaryellow.data.UserPointsRepository
import com.github.cedaryellow.data.local.database.Task
import com.github.cedaryellow.ui.task.TaskUiState.Error
import com.github.cedaryellow.ui.task.TaskUiState.Loading
import com.github.cedaryellow.ui.task.TaskUiState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userPointsRepository: UserPointsRepository
) : ViewModel() {

    val uiState: StateFlow<TaskUiState> = taskRepository
        .tasks
        .map<List<Task>, TaskUiState>(::Success)
        .catch { emit(Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)

    fun getTask(taskId: Int): StateFlow<TaskDetailsUiState> = taskRepository
        .getTask(taskId)
        .map<Task, TaskDetailsUiState> { TaskDetailsUiState.Success(it) }
        .catch { emit(TaskDetailsUiState.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskDetailsUiState.Loading)

    fun addTask(name: String, maxPoints: Int, estimatedDurationMinutes: Int) {
        viewModelScope.launch {
            taskRepository.addTask(name, maxPoints, estimatedDurationMinutes)
        }
    }
    
    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }
    
    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }
    
    fun startTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.startTask(taskId)
        }
    }
    
    fun pauseTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.pauseTask(taskId)
        }
    }
    
    fun completeTask(taskId: Int, rating: Int, reflection: String) {
        viewModelScope.launch {
            taskRepository.completeTask(taskId, rating, reflection)
            
            // Get the completed task to add its points
            val task = taskRepository.getTask(taskId).first()
            userPointsRepository.addPoints(task.earnedPoints)
        }
    }
}

sealed interface TaskUiState {
    object Loading : TaskUiState
    data class Error(val throwable: Throwable) : TaskUiState
    data class Success(val data: List<Task>) : TaskUiState
}

sealed interface TaskDetailsUiState {
    object Loading : TaskDetailsUiState
    data class Error(val throwable: Throwable) : TaskDetailsUiState
    data class Success(val data: Task) : TaskDetailsUiState
}
