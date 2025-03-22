package com.github.cedaryellow.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.cedaryellow.data.UserPointsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 表示兑换结果
sealed class RedeemResult {
    object None : RedeemResult()
    object Success : RedeemResult()
    object InsufficientPoints : RedeemResult()
}

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val userPointsRepository: UserPointsRepository
) : ViewModel() {

    val totalPoints = userPointsRepository.totalPoints
    
    // 追踪兑换结果状态
    private val _redeemResult = MutableStateFlow<RedeemResult>(RedeemResult.None)
    val redeemResult: StateFlow<RedeemResult> = _redeemResult.asStateFlow()
    
    // 兑换放松时间
    fun redeemRelaxationTime() {
        viewModelScope.launch {
            val result = userPointsRepository.usePoints(10000)
            _redeemResult.value = if (result) {
                RedeemResult.Success
            } else {
                RedeemResult.InsufficientPoints
            }
        }
    }
    
    // 重置兑换结果状态
    fun resetRedeemResult() {
        _redeemResult.value = RedeemResult.None
    }
} 