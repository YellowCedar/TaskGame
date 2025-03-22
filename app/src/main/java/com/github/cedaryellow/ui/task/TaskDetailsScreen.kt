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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.cedaryellow.data.local.database.Task
import com.github.cedaryellow.data.local.database.TaskState
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsScreen(
    taskId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.getTask(taskId).collectAsStateWithLifecycle()
    var showCompletionDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is TaskDetailsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is TaskDetailsUiState.Error -> {
                    Text(
                        text = "Error loading task",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is TaskDetailsUiState.Success -> {
                    val task = (uiState as TaskDetailsUiState.Success).data
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Task Header
                        Text(
                            text = task.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Task Info Card
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                InfoRow("Status", getStatusText(task.state))
                                InfoRow("Points", "${task.maxPoints}")
                                InfoRow("Estimated Duration", "${task.estimatedDurationMinutes} minutes")
                                
                                if (task.state == TaskState.IN_PROGRESS || task.state == TaskState.PAUSED) {
                                    val elapsedTime = formatElapsedTime(task)
                                    InfoRow("Time Elapsed", elapsedTime)
                                }
                                
                                if (task.state == TaskState.COMPLETED) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    InfoRow("Earned Points", "${task.earnedPoints}")
                                    InfoRow("Rating", "${task.rating} / 5")
                                    Text(
                                        text = "Reflection",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    Text(
                                        text = task.reflection.ifEmpty { "No reflection provided" },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Action Buttons
                        when (task.state) {
                            TaskState.NOT_STARTED -> {
                                Button(
                                    onClick = { viewModel.startTask(taskId) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Text("Start Task", modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                            TaskState.IN_PROGRESS -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.pauseTask(taskId) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = null)
                                        Text("Pause", modifier = Modifier.padding(start = 8.dp))
                                    }
                                    
                                    Button(
                                        onClick = { showCompletionDialog = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Text("Complete", modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                            TaskState.PAUSED -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.startTask(taskId) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Text("Resume", modifier = Modifier.padding(start = 8.dp))
                                    }
                                    
                                    Button(
                                        onClick = { showCompletionDialog = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Text("Complete", modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                            TaskState.COMPLETED -> {
                                // No action buttons for completed tasks
                            }
                        }
                    }
                    
                    if (showCompletionDialog) {
                        TaskCompletionDialog(
                            onDismiss = { showCompletionDialog = false },
                            onComplete = { rating, reflection ->
                                viewModel.completeTask(taskId, rating, reflection)
                                showCompletionDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun TaskCompletionDialog(
    onDismiss: () -> Unit,
    onComplete: (rating: Int, reflection: String) -> Unit
) {
    var rating by remember { mutableStateOf(3) }
    var reflection by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Task") },
        text = {
            Column {
                Text("How would you rate your performance?")
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) { index ->
                        IconButton(
                            onClick = { rating = index + 1 }
                        ) {
                            Icon(
                                imageVector = if (index < rating) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = reflection,
                    onValueChange = { reflection = it },
                    label = { Text("Reflection (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onComplete(rating, reflection) }
            ) {
                Text("Complete")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun getStatusText(state: TaskState): String {
    return when (state) {
        TaskState.NOT_STARTED -> "Not Started"
        TaskState.IN_PROGRESS -> "In Progress"
        TaskState.PAUSED -> "Paused"
        TaskState.COMPLETED -> "Completed"
    }
}

private fun formatElapsedTime(task: Task): String {
    var totalElapsedMillis = task.totalElapsedTime
    
    // If task is currently running, calculate the additional elapsed time
    if (task.state == TaskState.IN_PROGRESS && task.startTime != null) {
        val currentTimeMillis = System.currentTimeMillis()
        totalElapsedMillis += (currentTimeMillis - task.startTime)
    }
    
    val hours = TimeUnit.MILLISECONDS.toHours(totalElapsedMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(totalElapsedMillis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(totalElapsedMillis) % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
} 