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

package com.github.cedaryellow.ui.fragmenttime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.util.TimeUtils.formatDuration
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.cedaryellow.data.local.database.TaskState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FragmentTimeDetailsScreen(
    fragmentTimeId: Int,
    onNavigateBack: () -> Unit,
    viewModel: FragmentTimeViewModel = hiltViewModel()
) {
    val fragmentTimeState by viewModel.getFragmentTime(fragmentTimeId).collectAsState()
    
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var rating by remember { mutableStateOf(3) }
    var reflection by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("碎片时间详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (fragmentTimeState) {
                is FragmentTimeDetailsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is FragmentTimeDetailsUiState.Error -> {
                    Text(
                        text = "加载出错: ${(fragmentTimeState as FragmentTimeDetailsUiState.Error).throwable.message}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is FragmentTimeDetailsUiState.Success -> {
                    val fragmentTime = (fragmentTimeState as FragmentTimeDetailsUiState.Success).data
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = fragmentTime.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "状态: ${getStateText(fragmentTime.state)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "总时长: ${formatDuration(fragmentTime.totalElapsedTime)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        if (fragmentTime.startTime != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "开始时间: ${formatDate(fragmentTime.startTime)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        
                        if (fragmentTime.endTime != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "结束时间: ${formatDate(fragmentTime.endTime)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        
                        if (fragmentTime.earnedPoints > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "获得积分: ${fragmentTime.earnedPoints}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        if (fragmentTime.rating != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "评价: ",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                
                                for (i in 1..5) {
                                    Icon(
                                        imageVector = if (i <= fragmentTime.rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = null,
                                        tint = if (i <= fragmentTime.rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                        
                        if (fragmentTime.reflection.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "复盘感悟:",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = fragmentTime.reflection,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            when (fragmentTime.state) {
                                TaskState.NOT_STARTED, TaskState.PAUSED, TaskState.COMPLETED -> {
                                    Button(onClick = { viewModel.startFragmentTime(fragmentTime.uid) }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("开始")
                                    }
                                }
                                TaskState.IN_PROGRESS -> {
                                    Button(onClick = { viewModel.pauseFragmentTime(fragmentTime.uid) }) {
                                        Icon(Icons.Default.Pause, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("暂停")
                                    }
                                    
                                    Button(onClick = { 
                                        showCompleteDialog = true
                                        rating = fragmentTime.rating ?: 3
                                        reflection = fragmentTime.reflection
                                    }) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("完成")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showCompleteDialog) {
            AlertDialog(
                onDismissRequest = { showCompleteDialog = false },
                title = { Text("完成碎片时间") },
                text = {
                    Column {
                        Text("请评价你的完成情况:")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (i in 1..5) {
                                IconButton(
                                    onClick = { rating = i }
                                ) {
                                    Icon(
                                        imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = null,
                                        tint = if (i <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = reflection,
                            onValueChange = { reflection = it },
                            label = { Text("复盘感悟 (可选)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.completeFragmentTime(fragmentTimeId, rating, reflection)
                            showCompleteDialog = false
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCompleteDialog = false }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
        
        if (showDeleteConfirmDialog && fragmentTimeState is FragmentTimeDetailsUiState.Success) {
            val fragmentTime = (fragmentTimeState as FragmentTimeDetailsUiState.Success).data
            
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("确认删除") },
                text = { 
                    Text(
                        text = "确定要删除「${fragmentTime.name}」吗？此操作无法撤销。",
                        textAlign = TextAlign.Center
                    ) 
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteFragmentTime(fragmentTime)
                            showDeleteConfirmDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmDialog = false }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

@Composable
private fun getStateText(state: TaskState): String {
    return when (state) {
        TaskState.NOT_STARTED -> "未开始"
        TaskState.IN_PROGRESS -> "进行中"
        TaskState.PAUSED -> "已暂停"
        TaskState.COMPLETED -> "已完成"
    }
}

private fun formatDuration(durationMillis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
} 