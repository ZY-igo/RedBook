package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.remote.model.RemoteDraftDto
import com.zhengyang.redbook.data.remote.model.RemoteNoteDetailDto

interface PublishRepository {
    suspend fun createTextNote(title: String, content: String, mediaType: String): RemoteNoteDetailDto
    suspend fun saveDraft(id: String? = null, title: String, content: String, mediaType: String, autoSaved: Boolean): RemoteDraftDto
    suspend fun getDrafts(): List<RemoteDraftDto>
}
