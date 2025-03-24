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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.cedaryellow.data.FragmentTimeRepository
import com.github.cedaryellow.data.local.database.FragmentTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FragmentTimeViewModel @Inject constructor(
    private val fragmentTimeRepository: FragmentTimeRepository
) : ViewModel() {

    val fragmentTimes: StateFlow<FragmentTimeUiState> = fragmentTimeRepository.fragmentTimes
        .map<List<FragmentTime>, FragmentTimeUiState>(FragmentTimeUiState::Success)
        .catch { 
            emit(FragmentTimeUiState.Error(it))
            Log.e("FragmentTimeViewModel", "Error loading fragment times", it)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FragmentTimeUiState.Loading)

    fun getFragmentTime(fragmentTimeId: Int): StateFlow<FragmentTimeDetailsUiState> = fragmentTimeRepository
        .getFragmentTime(fragmentTimeId)
        .map<FragmentTime, FragmentTimeDetailsUiState> { FragmentTimeDetailsUiState.Success(it) }
        .catch { emit(FragmentTimeDetailsUiState.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FragmentTimeDetailsUiState.Loading)

    fun addFragmentTime(name: String) {
        viewModelScope.launch {
            fragmentTimeRepository.addFragmentTime(name)
        }
    }
    
    fun updateFragmentTime(fragmentTime: FragmentTime) {
        viewModelScope.launch {
            fragmentTimeRepository.updateFragmentTime(fragmentTime)
        }
    }
    
    fun deleteFragmentTime(fragmentTime: FragmentTime) {
        viewModelScope.launch {
            fragmentTimeRepository.deleteFragmentTime(fragmentTime)
        }
    }
    
    fun startFragmentTime(fragmentTimeId: Int) {
        viewModelScope.launch {
            fragmentTimeRepository.startFragmentTime(fragmentTimeId)
        }
    }
    
    fun pauseFragmentTime(fragmentTimeId: Int) {
        viewModelScope.launch {
            fragmentTimeRepository.pauseFragmentTime(fragmentTimeId)
        }
    }
    
    fun completeFragmentTime(fragmentTimeId: Int, rating: Int, reflection: String) {
        viewModelScope.launch {
            fragmentTimeRepository.completeFragmentTime(fragmentTimeId, rating, reflection)
        }
    }
}

sealed interface FragmentTimeUiState {
    object Loading : FragmentTimeUiState
    data class Error(val throwable: Throwable) : FragmentTimeUiState
    data class Success(val data: List<FragmentTime>) : FragmentTimeUiState
}

sealed interface FragmentTimeDetailsUiState {
    object Loading : FragmentTimeDetailsUiState
    data class Error(val throwable: Throwable) : FragmentTimeDetailsUiState
    data class Success(val data: FragmentTime) : FragmentTimeDetailsUiState
} 