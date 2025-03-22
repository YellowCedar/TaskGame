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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userPointsRepository: UserPointsRepository
) : ViewModel() {

    // 选中的日期，默认为今天
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate
    
    // 是否显示所有任务，默认只显示选中日期的任务
    private val _showAllTasks = MutableStateFlow(false)
    val showAllTasks: StateFlow<Boolean> = _showAllTasks

    // 根据选中的日期和显示模式来动态更新UI状态
    val uiState: StateFlow<TaskUiState> = combine(
        _selectedDate,
        _showAllTasks
    ) { date, showAll ->
        Pair(date, showAll)
    }.flatMapLatest { (date, showAll) ->
        if (showAll) {
            taskRepository.tasks
        } else {
            taskRepository.getTasksByDate(date)
        }
    }.map<List<Task>, TaskUiState>(::Success)
     .catch { emit(Error(it)) }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)

    // 设置是否显示所有任务
    fun setShowAllTasks(showAll: Boolean) {
        _showAllTasks.value = showAll
    }
    
    // 设置选中日期
    fun setSelectedDate(timestamp: Long) {
        _selectedDate.value = timestamp
    }
    
    // 将日期设置为今天
    fun setToday() {
        _selectedDate.value = System.currentTimeMillis()
    }
    
    // 将日期设置为昨天
    fun setYesterday() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = _selectedDate.value
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        _selectedDate.value = calendar.timeInMillis
    }
    
    // 将日期设置为明天
    fun setTomorrow() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = _selectedDate.value
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        _selectedDate.value = calendar.timeInMillis
    }

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
