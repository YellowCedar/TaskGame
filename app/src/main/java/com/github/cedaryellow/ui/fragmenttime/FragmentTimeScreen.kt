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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.cedaryellow.data.local.database.FragmentTime
import com.github.cedaryellow.data.local.database.TaskState
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FragmentTimeScreen(
    onAddFragmentTime: () -> Unit = {},
    onFragmentTimeClick: (Int) -> Unit = {},
    viewModel: FragmentTimeViewModel = hiltViewModel()
) {
    val fragmentTimesState by viewModel.fragmentTimes.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var newFragmentTimeName by remember { mutableStateOf("") }
    var selectedFragmentTimeId by remember { mutableStateOf<Int?>(null) }
    var rating by remember { mutableStateOf(3) }
    var reflection by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("碎片时间库") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加碎片时间")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (fragmentTimesState) {
                is FragmentTimeUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is FragmentTimeUiState.Error -> {
                    Text(
                        text = "加载出错: ${(fragmentTimesState as FragmentTimeUiState.Error).throwable.message}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is FragmentTimeUiState.Success -> {
                    val fragmentTimes = (fragmentTimesState as FragmentTimeUiState.Success).data
                    if (fragmentTimes.isEmpty()) {
                        Text(
                            text = "没有碎片时间记录，点击 + 添加一个",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(fragmentTimes) { fragmentTime ->
                                FragmentTimeItem(
                                    fragmentTime = fragmentTime,
                                    onClick = { onFragmentTimeClick(fragmentTime.uid) },
                                    onStart = { viewModel.startFragmentTime(fragmentTime.uid) },
                                    onPause = { viewModel.pauseFragmentTime(fragmentTime.uid) },
                                    onComplete = { 
                                        selectedFragmentTimeId = fragmentTime.uid
                                        rating = fragmentTime.rating ?: 3
                                        reflection = fragmentTime.reflection
                                        showCompleteDialog = true
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
        
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("新增碎片时间") },
                text = {
                    OutlinedTextField(
                        value = newFragmentTimeName,
                        onValueChange = { newFragmentTimeName = it },
                        label = { Text("名称") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFragmentTimeName.isNotBlank()) {
                                viewModel.addFragmentTime(newFragmentTimeName)
                                newFragmentTimeName = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddDialog = false }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
        
        if (showCompleteDialog && selectedFragmentTimeId != null) {
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
                            selectedFragmentTimeId?.let { id ->
                                viewModel.completeFragmentTime(id, rating, reflection)
                            }
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
    }
}

@Composable
fun FragmentTimeItem(
    fragmentTime: FragmentTime,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onComplete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = fragmentTime.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    when (fragmentTime.state) {
                        TaskState.NOT_STARTED, TaskState.PAUSED, TaskState.COMPLETED -> {
                            IconButton(onClick = onStart) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "开始")
                            }
                        }
                        TaskState.IN_PROGRESS -> {
                            IconButton(onClick = onPause) {
                                Icon(Icons.Default.Pause, contentDescription = "暂停")
                            }
                            IconButton(onClick = onComplete) {
                                Icon(Icons.Default.Check, contentDescription = "完成")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "状态: ${getStateText(fragmentTime.state)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "时长: ${formatDuration(fragmentTime.totalElapsedTime)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (fragmentTime.earnedPoints > 0) {
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = "获得: ${fragmentTime.earnedPoints}点",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
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