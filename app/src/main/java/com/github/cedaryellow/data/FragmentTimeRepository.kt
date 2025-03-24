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

package com.github.cedaryellow.data

import com.github.cedaryellow.data.local.database.FragmentTime
import com.github.cedaryellow.data.local.database.FragmentTimeDao
import com.github.cedaryellow.data.local.database.TaskState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.sqrt

interface FragmentTimeRepository {
    val fragmentTimes: Flow<List<FragmentTime>>
    
    fun getFragmentTime(fragmentTimeId: Int): Flow<FragmentTime>
    
    suspend fun addFragmentTime(name: String): Long
    
    suspend fun updateFragmentTime(fragmentTime: FragmentTime)
    
    suspend fun deleteFragmentTime(fragmentTime: FragmentTime)
    
    suspend fun startFragmentTime(fragmentTimeId: Int)
    
    suspend fun pauseFragmentTime(fragmentTimeId: Int)
    
    suspend fun completeFragmentTime(fragmentTimeId: Int, rating: Int, reflection: String)
}

class DefaultFragmentTimeRepository @Inject constructor(
    private val fragmentTimeDao: FragmentTimeDao,
    private val userPointsRepository: UserPointsRepository
) : FragmentTimeRepository {

    override val fragmentTimes: Flow<List<FragmentTime>> = fragmentTimeDao.getAllFragmentTimes()

    override fun getFragmentTime(fragmentTimeId: Int): Flow<FragmentTime> = fragmentTimeDao.getFragmentTime(fragmentTimeId)
    
    override suspend fun addFragmentTime(name: String): Long {
        val fragmentTime = FragmentTime(name = name)
        return fragmentTimeDao.insertFragmentTime(fragmentTime)
    }
    
    override suspend fun updateFragmentTime(fragmentTime: FragmentTime) {
        fragmentTimeDao.updateFragmentTime(fragmentTime)
    }
    
    override suspend fun deleteFragmentTime(fragmentTime: FragmentTime) {
        fragmentTimeDao.deleteFragmentTime(fragmentTime)
    }
    
    override suspend fun startFragmentTime(fragmentTimeId: Int) {
        val fragmentTime = fragmentTimeDao.getFragmentTime(fragmentTimeId).first()
        
        // 碎片时间可以在任何状态下重新启动
        // 当重新启动时，需要记录最后完成时的累计时间，用于后续计算新增时间
        val updatedFragmentTime = if (fragmentTime.state == TaskState.COMPLETED) {
            fragmentTime.copy(
                state = TaskState.IN_PROGRESS,
                startTime = System.currentTimeMillis(),
                pauseTime = null, // 重置暂停时间
                // 记录最后一次完成时的时间，用于计算新增时间
                // 保持现有的累计时间
                // 保持已获得的积分
            )
        } else {
            fragmentTime.copy(
                state = TaskState.IN_PROGRESS,
                startTime = System.currentTimeMillis(),
                pauseTime = null // 重置暂停时间
            )
        }
        
        fragmentTimeDao.updateFragmentTime(updatedFragmentTime)
    }
    
    override suspend fun pauseFragmentTime(fragmentTimeId: Int) {
        val fragmentTime = fragmentTimeDao.getFragmentTime(fragmentTimeId).first()
        val currentTime = System.currentTimeMillis()
        val elapsedSinceStart = fragmentTime.startTime?.let { currentTime - it } ?: 0
        val updatedFragmentTime = fragmentTime.copy(
            state = TaskState.PAUSED,
            pauseTime = currentTime,
            totalElapsedTime = fragmentTime.totalElapsedTime + elapsedSinceStart
        )
        fragmentTimeDao.updateFragmentTime(updatedFragmentTime)
    }
    
    override suspend fun completeFragmentTime(fragmentTimeId: Int, rating: Int, reflection: String) {
        val fragmentTime = fragmentTimeDao.getFragmentTime(fragmentTimeId).first()
        val currentTime = System.currentTimeMillis()
        
        // 计算本次新增的时间（从上次启动开始到现在的时间）
        val newElapsedTime = when(fragmentTime.state) {
            TaskState.IN_PROGRESS -> {
                // 如果正在进行中，计算从启动到现在的时间
                val elapsedSinceStart = fragmentTime.startTime?.let { currentTime - it } ?: 0
                elapsedSinceStart
            }
            TaskState.PAUSED -> {
                // 如果已暂停，新增时间已累计到 totalElapsedTime 中
                // 我们需要计算从上次完成到现在暂停状态的总新增时间
                // 由于暂停已经更新了totalElapsedTime，所以这里使用0
                0L
            }
            else -> 0L
        }
        
        // 更新总累计时间
        val newTotalElapsedTime = fragmentTime.totalElapsedTime + newElapsedTime
        
        // 计算本次会话的总新增时间，用于计算积分
        // 对于暂停状态，我们需要计算自上次完成后的全部新增时间
        
        // 计算自上次完成后新增的总时间用于计算积分
        // 由于每次暂停都会更新 totalElapsedTime，所以累计的时间已经在 totalElapsedTime 中
        // 而对于进行中的状态，我们需要加上当前未累计的时间 newElapsedTime
        val timeForPointsCalculation = newElapsedTime
        
        // 将新增时间转换为分钟并计算积分
        val minutesSpent = (timeForPointsCalculation / (1000 * 60)).toInt()
        val newPoints = calculatePoints(minutesSpent, rating)
        
        // 累加到已有积分
        val totalEarnedPoints = fragmentTime.earnedPoints + newPoints
        
        val updatedFragmentTime = fragmentTime.copy(
            state = TaskState.COMPLETED,
            endTime = currentTime,
            totalElapsedTime = newTotalElapsedTime,
            rating = rating,
            reflection = reflection,
            earnedPoints = totalEarnedPoints
        )
        
        fragmentTimeDao.updateFragmentTime(updatedFragmentTime)
        
        // 只将本次新获得的积分添加到用户总积分
        userPointsRepository.addPoints(newPoints)
    }
    
    private fun calculatePoints(minutesSpent: Int, rating: Int): Int {
        // Base calculation: 1 point per minute
        // Rating adjustment: multiply by (rating / 5)
        return (minutesSpent * (rating / 5.0)).toInt()
    }
} 