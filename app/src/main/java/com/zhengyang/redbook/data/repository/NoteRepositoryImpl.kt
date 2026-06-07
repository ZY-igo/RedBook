package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.model.RemoteCommentDto
import com.zhengyang.redbook.data.remote.model.RemoteCreateCommentRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteFollowToggleRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteNoteDetailDto
import com.zhengyang.redbook.data.remote.requireData
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteRepositoryImpl @Inject constructor(
    private val apiService: RedBookApiService
) : NoteRepository {

    override suspend fun getNoteDetail(noteId: String): RemoteNoteDetailDto = withContext(Dispatchers.IO) {
        apiService.getNoteDetail(noteId).requireData()
    }

    override suspend fun getComments(noteId: String, sortMode: String, offset: Int, limit: Int): List<RemoteCommentDto> = withContext(Dispatchers.IO) {
        apiService.getNoteComments(noteId = noteId, sortMode = sortMode, offset = offset, limit = limit)
            .requireData()
            .items
    }

    override suspend fun toggleLike(noteId: String, value: Boolean) = withContext(Dispatchers.IO) {
        apiService.toggleNoteLike(noteId, RemoteFollowToggleRequestDto(value)).requireData()
        Unit
    }

    override suspend fun toggleCollect(noteId: String, value: Boolean) = withContext(Dispatchers.IO) {
        apiService.toggleNoteCollect(noteId, RemoteFollowToggleRequestDto(value)).requireData()
        Unit
    }

    override suspend fun followAuthor(userId: String, value: Boolean) = withContext(Dispatchers.IO) {
        apiService.followUser(userId, RemoteFollowToggleRequestDto(value)).requireData()
        Unit
    }

    override suspend fun createComment(
        noteId: String,
        content: String,
        parentCommentId: String?,
        replyToUserName: String?,
        imageUrl: String?
    ): RemoteCommentDto = withContext(Dispatchers.IO) {
        apiService.createComment(
            RemoteCreateCommentRequestDto(
                noteId = noteId,
                parentCommentId = parentCommentId,
                replyToUserName = replyToUserName,
                content = content,
                city = null,
                imageUrl = imageUrl
            )
        ).requireData()
    }

    override suspend fun toggleCommentLike(commentId: String, value: Boolean) = withContext(Dispatchers.IO) {
        apiService.toggleCommentLike(commentId, RemoteFollowToggleRequestDto(value)).requireData()
        Unit
    }
}
