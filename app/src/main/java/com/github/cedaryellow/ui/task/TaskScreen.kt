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

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.cedaryellow.data.local.database.Task
import com.github.cedaryellow.data.local.database.TaskState
import com.github.cedaryellow.data.local.database.Tag
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.PaddingValues
import com.github.cedaryellow.ui.task.TagList
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    onAddTask: () -> Unit,
    onTaskClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val showAllTasks by viewModel.showAllTasks.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Filled.Add, contentDescription = "添加任务")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding)
        ) {
            // 日期选择栏
            DateSelectionBar(
                selectedDate = selectedDate,
                showAllTasks = showAllTasks,
                onShowAllTasksChanged = { viewModel.setShowAllTasks(it) },
                onSelectDate = { showDatePicker = true },
                onPreviousDay = { viewModel.setYesterday() },
                onNextDay = { viewModel.setTomorrow() },
                onToday = { viewModel.setToday() },
                modifier = Modifier.fillMaxWidth()
            )
            
            // 任务列表
            when (uiState) {
                is TaskUiState.Loading -> {
                    LoadingScreen()
                }
                is TaskUiState.Error -> {
                    ErrorScreen()
                }
                is TaskUiState.Success -> {
                    val tasks = (uiState as TaskUiState.Success).data
                    TaskListScreen(
                        tasks = tasks,
                        onTaskClick = onTaskClick,
                        viewModel = viewModel
                    )
                }
            }
        }
        
        // 日期选择器对话框
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
            
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                viewModel.setSelectedDate(it)
                                Log.i("date", it.toString());
                                showDatePicker = false
                            } ?: run {
                                showDatePicker = false
                            }
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDatePicker = false }
                    ) {
                        Text("取消")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState
                )
            }
        }
    }
}

@Composable
fun DateSelectionBar(
    selectedDate: Long,
    showAllTasks: Boolean,
    onShowAllTasksChanged: (Boolean) -> Unit,
    onSelectDate: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // 日期选择行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousDay) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "前一天")
                }
                
                Row(
                    modifier = Modifier
                        .clickable(onClick = onSelectDate)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "选择日期",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatDate(selectedDate),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "点击选择日期",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                IconButton(onClick = onNextDay) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "后一天")
                }
            }
            
            // 今天按钮和显示全部任务选项
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToday) {
                    Text("今天")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showAllTasks,
                        onCheckedChange = onShowAllTasksChanged
                    )
                    Text(
                        text = "显示所有任务",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// 格式化日期，显示如 "2023年4月15日 星期六"
@Composable
fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy年MM月dd日 E")
    val dateStr = formatter.format(Date(timestamp))
    Log.i("inner date", "$timestamp $dateStr");
    // 检查是否是今天
    val today = Calendar.getInstance()
    val selectedDate = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    val isToday = today.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                 today.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
    
    return if (isToday) {
        "$dateStr (今天)"
    } else {
        dateStr
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("加载任务时出错，请稍后重试。")
    }
}

@Composable
fun TaskListScreen(
    tasks: List<Task>,
    onTaskClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel,
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "当前没有任务。\n点击右下角按钮添加新任务！",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            items(tasks) { task ->
                TaskListItem(
                    task = task,
                    onClick = { onTaskClick(task.uid) },
                    onStartClick = { viewModel.startTask(task.uid) },
                    onPauseClick = { viewModel.pauseTask(task.uid) },
                    viewModel = viewModel,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskListItem(
    task: Task,
    onClick: () -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val tags by viewModel.getTagsForTask(task.uid).collectAsStateWithLifecycle()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Task control buttons
                when (task.state) {
                    TaskState.NOT_STARTED -> {
                        IconButton(onClick = onStartClick) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start Task"
                            )
                        }
                    }
                    TaskState.IN_PROGRESS -> {
                        IconButton(onClick = onPauseClick) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause Task"
                            )
                        }
                    }
                    else -> {
                        // No action buttons for paused or completed tasks
                    }
                }
            }
            
            // Display task status and duration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${task.estimatedDurationMinutes} min",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Status chip
                val statusColor = when(task.state) {
                    TaskState.NOT_STARTED -> MaterialTheme.colorScheme.outline
                    TaskState.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                    TaskState.PAUSED -> MaterialTheme.colorScheme.tertiary
                    TaskState.COMPLETED -> MaterialTheme.colorScheme.secondary
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when(task.state) {
                            TaskState.NOT_STARTED -> "Not Started"
                            TaskState.IN_PROGRESS -> "In Progress"
                            TaskState.PAUSED -> "Paused"
                            TaskState.COMPLETED -> "Completed"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
            }
            
            // Show tags if available
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                TagList(
                    tags = tags,
                    onTagClick = { /* Can't navigate from here, just show */ },
                    maxDisplayedTags = 3,
                    showAll = false
                )
            }
        }
    }
}

@Composable
fun TaskStatusIndicator(task: Task) {
    val (color, text) = when (task.state) {
        TaskState.NOT_STARTED -> Pair(MaterialTheme.colorScheme.outline, "Not Started")
        TaskState.IN_PROGRESS -> Pair(MaterialTheme.colorScheme.primary, "In Progress")
        TaskState.PAUSED -> Pair(MaterialTheme.colorScheme.tertiary, "Paused")
        TaskState.COMPLETED -> Pair(MaterialTheme.colorScheme.secondary, "Completed")
    }
    
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodySmall
    )
}
