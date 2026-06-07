package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.model.RemoteCreateNoteRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteDraftDto
import com.zhengyang.redbook.data.remote.model.RemoteNoteDetailDto
import com.zhengyang.redbook.data.remote.model.RemoteSaveDraftRequestDto
import com.zhengyang.redbook.data.remote.requireData
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PublishRepositoryImpl @Inject constructor(
    private val apiService: RedBookApiService
) : PublishRepository {

    override suspend fun createTextNote(title: String, content: String, mediaType: String): RemoteNoteDetailDto = withContext(Dispatchers.IO) {
        apiService.createNote(
            RemoteCreateNoteRequestDto(
                title = title,
                description = content,
                mediaType = mediaType
            )
        ).requireData()
    }

    override suspend fun saveDraft(id: String?, title: String, content: String, mediaType: String, autoSaved: Boolean): RemoteDraftDto = withContext(Dispatchers.IO) {
        apiService.saveDraft(
            RemoteSaveDraftRequestDto(
                id = id,
                title = title,
                description = content,
                mediaType = mediaType,
                autoSaved = autoSaved
            )
        ).requireData()
    }

    override suspend fun getDrafts(): List<RemoteDraftDto> = withContext(Dispatchers.IO) {
        apiService.getDrafts().requireData()
    }
}
