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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.cedaryellow.data.TagRepository
import com.github.cedaryellow.data.local.database.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {

    val tags: StateFlow<TagUiState> = tagRepository.tags
        .map<List<Tag>, TagUiState>(TagUiState::Success)
        .catch { emit(TagUiState.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TagUiState.Loading)

    fun getTag(tagId: Int): StateFlow<TagDetailsUiState> = tagRepository
        .getTag(tagId)
        .map<Tag, TagDetailsUiState> { TagDetailsUiState.Success(it) }
        .catch { emit(TagDetailsUiState.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TagDetailsUiState.Loading)

    fun addTag(name: String, color: String) {
        viewModelScope.launch {
            tagRepository.addTag(name, color)
        }
    }
    
    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.updateTag(tag)
        }
    }
    
    fun updateTagContent(tagId: Int, content: String) {
        viewModelScope.launch {
            tagRepository.updateTagContent(tagId, content)
        }
    }
    
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
        }
    }
    
    fun searchTags(query: String, callback: (List<Tag>) -> Unit) {
        viewModelScope.launch {
            val results = tagRepository.searchTags(query)
            callback(results)
        }
    }
    
    fun getTasksWithTag(tagId: Int): StateFlow<TaskUiState> = tagRepository
        .getTasksWithTag(tagId)
        .map<List<com.github.cedaryellow.data.local.database.Task>, TaskUiState>(TaskUiState::Success)
        .catch { emit(TaskUiState.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskUiState.Loading)
}

sealed interface TagUiState {
    object Loading : TagUiState
    data class Error(val throwable: Throwable) : TagUiState
    data class Success(val data: List<Tag>) : TagUiState
}

sealed interface TagDetailsUiState {
    object Loading : TagDetailsUiState
    data class Error(val throwable: Throwable) : TagDetailsUiState
    data class Success(val data: Tag) : TagDetailsUiState
}

sealed interface TaskUiState {
    object Loading : TaskUiState
    data class Error(val throwable: Throwable) : TaskUiState
    data class Success(val data: List<com.github.cedaryellow.data.local.database.Task>) : TaskUiState
} 