package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.remote.model.RemoteCommentDto
import com.zhengyang.redbook.data.remote.model.RemoteNoteDetailDto

interface NoteRepository {
    suspend fun getNoteDetail(noteId: String): RemoteNoteDetailDto
    suspend fun getComments(noteId: String, sortMode: String, offset: Int = 0, limit: Int = 20): List<RemoteCommentDto>
    suspend fun toggleLike(noteId: String, value: Boolean)
    suspend fun toggleCollect(noteId: String, value: Boolean)
    suspend fun followAuthor(userId: String, value: Boolean)
    suspend fun createComment(
        noteId: String,
        content: String,
        parentCommentId: String? = null,
        replyToUserName: String? = null,
        imageUrl: String? = null
    ): RemoteCommentDto

    suspend fun toggleCommentLike(commentId: String, value: Boolean)
}
