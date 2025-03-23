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

import com.github.cedaryellow.data.local.database.Tag
import com.github.cedaryellow.data.local.database.TagDao
import com.github.cedaryellow.data.local.database.TaskTagDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

interface TagRepository {
    val tags: Flow<List<Tag>>
    
    fun getTag(tagId: Int): Flow<Tag>
    
    suspend fun addTag(name: String, color: String): Long
    
    suspend fun updateTag(tag: Tag)
    
    suspend fun updateTagContent(tagId: Int, content: String)
    
    suspend fun deleteTag(tag: Tag)
    
    suspend fun searchTags(query: String): List<Tag>
    
    fun getTasksWithTag(tagId: Int): Flow<List<com.github.cedaryellow.data.local.database.Task>>
}

class DefaultTagRepository @Inject constructor(
    private val tagDao: TagDao,
    private val taskTagDao: TaskTagDao
) : TagRepository {

    override val tags: Flow<List<Tag>> = tagDao.getAllTags()

    override fun getTag(tagId: Int): Flow<Tag> = tagDao.getTagById(tagId)
    
    override suspend fun addTag(name: String, color: String): Long {
        val tag = Tag(
            name = name,
            color = color
        )
        return tagDao.insertTag(tag)
    }
    
    override suspend fun updateTag(tag: Tag) {
        tagDao.updateTag(tag)
    }
    
    override suspend fun updateTagContent(tagId: Int, content: String) {
        val tag = tagDao.getTagById(tagId).first()
        tagDao.updateTag(tag.copy(content = content))
    }
    
    override suspend fun deleteTag(tag: Tag) {
        tagDao.deleteTag(tag)
    }
    
    override suspend fun searchTags(query: String): List<Tag> {
        return tagDao.searchTags("%$query%")
    }
    
    override fun getTasksWithTag(tagId: Int): Flow<List<com.github.cedaryellow.data.local.database.Task>> {
        return taskTagDao.getTasksWithTag(tagId)
    }
} 