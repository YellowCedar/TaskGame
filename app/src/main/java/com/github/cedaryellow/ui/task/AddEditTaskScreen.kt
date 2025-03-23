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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.cedaryellow.data.local.database.Tag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel = hiltViewModel()
) {
    var taskName by remember { mutableStateOf("") }
    var maxPoints by remember { mutableStateOf("0") }
    var estimatedDuration by remember { mutableStateOf("0") }
    
    var nameError by remember { mutableStateOf(false) }
    var pointsError by remember { mutableStateOf(false) }
    var durationError by remember { mutableStateOf(false) }
    
    // For tag handling
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    var selectedTags by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Task") },
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task Name Field
            OutlinedTextField(
                value = taskName,
                onValueChange = { 
                    taskName = it
                    nameError = it.isEmpty()
                },
                label = { Text("Task Name") },
                isError = nameError,
                supportingText = { 
                    if (nameError) Text("Task name is required") 
                },
                modifier = Modifier.fillMaxWidth()
            )
            
//            // Max Points Field todo relates to TaskDetailsScreen.kt
//            OutlinedTextField(
//                value = maxPoints,
//                onValueChange = {
//                    maxPoints = it
//                    try {
//                        val points = it.toInt()
//                        pointsError = points < 0
//                    } catch (e: NumberFormatException) {
//                        pointsError = true
//                    }
//                },
//                label = { Text("Max Points") },
//                isError = pointsError,
//                supportingText = {
//                    if (pointsError) Text("Points must be a positive number")
//                },
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                modifier = Modifier.fillMaxWidth()
//            )
            
            // Estimated Duration Field
            OutlinedTextField(
                value = estimatedDuration,
                onValueChange = { 
                    estimatedDuration = it
                    try {
                        val duration = it.toInt()
                        durationError = duration <= 0
                    } catch (e: NumberFormatException) {
                        durationError = true
                    }
                },
                label = { Text("Estimated Duration (minutes)") },
                isError = durationError,
                supportingText = { 
                    if (durationError) Text("Duration must be a positive number") 
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Tag Selector
            TagSelector(
                availableTags = allTags,
                selectedTags = selectedTags,
                onTagSelected = { tag ->
                    selectedTags = selectedTags + tag
                },
                onTagDeselected = { tag ->
                    selectedTags = selectedTags.filter { it.tagId != tag.tagId }
                },
                onCreateNewTag = {
                    showCreateTagDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Save Button
            Button(
                onClick = {
                    // Validate input
                    nameError = taskName.isEmpty()
                    try {
                        val points = maxPoints.toInt()
                        pointsError = points < 0
                    } catch (e: NumberFormatException) {
                        pointsError = true
                    }
                    
                    try {
                        val duration = estimatedDuration.toInt()
                        durationError = duration <= 0
                    } catch (e: NumberFormatException) {
                        durationError = true
                    }
                    
                    // Save if valid
                    if (!nameError && !pointsError && !durationError) {
                        // Use the viewModel's coroutine scope
                        viewModel.saveTaskWithTags(
                            name = taskName,
                            maxPoints = maxPoints.toInt(),
                            estimatedDurationMinutes = estimatedDuration.toInt(),
                            selectedTags = selectedTags,
                            onComplete = { onNavigateBack() }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.End)
            ) {
                Text("Save")
            }
        }
    }
    
    // Create Tag Dialog
    if (showCreateTagDialog) {
        CreateTagDialog(
            onDismiss = { showCreateTagDialog = false },
            onConfirm = { name, color ->
                viewModel.addTag(name, color)
                showCreateTagDialog = false
            }
        )
    }
} 