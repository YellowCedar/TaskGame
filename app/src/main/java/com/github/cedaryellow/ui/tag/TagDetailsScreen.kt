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

package com.github.cedaryellow.ui.tag

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.cedaryellow.data.local.database.Task
import com.github.cedaryellow.data.local.database.TaskState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailsScreen(
    tagId: Int,
    onNavigateBack: () -> Unit,
    onTaskClick: (Int) -> Unit,
    viewModel: TagViewModel = hiltViewModel()
) {
    val tagDetailsState by viewModel.getTag(tagId).collectAsState()
    val tasksState by viewModel.getTasksWithTag(tagId).collectAsState()
    
    var isEditing by remember { mutableStateOf(false) }
    var content by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    when (tagDetailsState) {
                        is TagDetailsUiState.Success -> {
                            val tag = (tagDetailsState as TagDetailsUiState.Success).data
                            Text("Tag: ${tag.name}")
                        }
                        else -> Text("Tag Details")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Content")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (tagDetailsState) {
                is TagDetailsUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading tag details...")
                        }
                    }
                }
                is TagDetailsUiState.Error -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Error: ${(tagDetailsState as TagDetailsUiState.Error).throwable.message}")
                        }
                    }
                }
                is TagDetailsUiState.Success -> {
                    val tag = (tagDetailsState as TagDetailsUiState.Success).data
                    
                    // Initialize content with tag's content when tag is loaded
                    if (!isEditing && content.isEmpty()) {
                        content = tag.content
                    }
                    
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(tag.color)))
                                    )
                                    Spacer(modifier = Modifier.size(12.dp))
                                    Text(
                                        text = tag.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (isEditing) {
                                    OutlinedTextField(
                                        value = content,
                                        onValueChange = { content = it },
                                        label = { Text("Tag Content") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 5
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = { 
                                                isEditing = false 
                                                content = tag.content
                                            }
                                        ) {
                                            Text("Cancel")
                                        }
                                        
                                        Spacer(modifier = Modifier.size(8.dp))
                                        
                                        Button(
                                            onClick = { 
                                                viewModel.updateTagContent(tagId, content)
                                                isEditing = false
                                            }
                                        ) {
                                            Text("Save")
                                        }
                                    }
                                } else {
                                    Text(
                                        text = tag.content.ifEmpty { "No content added yet. Click the edit button to add content." },
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Tasks with this tag",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    when (tasksState) {
                        is TaskUiState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Loading tasks...")
                                }
                            }
                        }
                        is TaskUiState.Error -> {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Error loading tasks: ${(tasksState as TaskUiState.Error).throwable.message}")
                                }
                            }
                        }
                        is TaskUiState.Success -> {
                            val tasks = (tasksState as TaskUiState.Success).data
                            
                            if (tasks.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No tasks with this tag")
                                    }
                                }
                            } else {
                                items(tasks) { task ->
                                    TaskItem(
                                        task = task,
                                        onClick = { onTaskClick(task.uid) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = when (task.state) {
                        TaskState.COMPLETED -> Color.Green
                        TaskState.IN_PROGRESS -> Color.Blue
                        TaskState.PAUSED -> Color.Yellow
                        else -> Color.Gray
                    }
                )
            }
            
            Spacer(modifier = Modifier.size(16.dp))
            
            Column {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Status: ${task.state.name.replace('_', ' ')}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (task.maxPoints > 0) {
                    Text(
                        text = "Points: ${task.earnedPoints}/${task.maxPoints}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
} 