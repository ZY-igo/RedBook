/**
 * 文件说明：NoteRepositoryImpl.kt
 * 作用：实现笔记详情场景仓储接口，负责详情与互动类远端请求。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
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

/**
 * 笔记详情仓储实现类
 *
 * 负责调用笔记详情、评论和互动相关接口，
 * 并将上层行为转换为服务端可识别的请求参数。
 */
class NoteRepositoryImpl @Inject constructor(
    /** 笔记详情相关远端接口服务。 */
    private val apiService: RedBookApiService
) : NoteRepository {

    /**
     * 获取笔记详情。
     *
     * @param noteId 笔记 ID。
     * @return 服务端返回的笔记详情 DTO。
     */
    override suspend fun getNoteDetail(noteId: String): RemoteNoteDetailDto = withContext(Dispatchers.IO) {
        apiService.getNoteDetail(noteId).requireData()
    }

    /**
     * 获取评论列表。
     *
     * @param noteId 笔记 ID。
     * @param sortMode 评论排序模式。
     * @param offset 偏移量。
     * @param limit 单次请求数量。
     * @return 当前分页范围内的评论 DTO 列表。
     */
    override suspend fun getComments(noteId: String, sortMode: String, offset: Int, limit: Int): List<RemoteCommentDto> = withContext(Dispatchers.IO) {
        apiService.getNoteComments(noteId = noteId, sortMode = sortMode, offset = offset, limit = limit)
            .requireData()
            .items
    }

    /**
     * 切换笔记点赞状态。
     *
     * @param noteId 笔记 ID。
     * @param value 目标点赞状态。
     * @return 无返回值，请求成功即视为状态切换完成。
     */
    override suspend fun toggleLike(noteId: String, value: Boolean) = withContext(Dispatchers.IO) {
        apiService.toggleNoteLike(noteId, RemoteFollowToggleRequestDto(value)).requireData()
        Unit
    }

    /**
     * 切换笔记收藏状态。
     *
     * @param noteId 笔记 ID。
     * @param value 目标收藏状态。
     * @return 无返回值，请求成功即视为状态切换完成。
     */
    override suspend fun toggleCollect(noteId: String, value: Boolean) = withContext(Dispatchers.IO) {
        apiService.toggleNoteCollect(noteId, RemoteFollowToggleRequestDto(value)).requireData()
        Unit
    }

    /**
     * 切换作者关注状态。
     *
     * @param userId 作者用户 ID。
     * @param value 目标关注状态。
     * @return 无返回值，请求成功即视为状态切换完成。
     */
    override suspend fun followAuthor(userId: String, value: Boolean) = withContext(Dispatchers.IO) {
        apiService.followUser(userId, RemoteFollowToggleRequestDto(value)).requireData()
        Unit
    }

    /**
     * 创建评论或回复。
     *
     * @param noteId 笔记 ID。
     * @param content 评论内容。
     * @param parentCommentId 父评论 ID。
     * @param replyToUserName 被回复用户名称。
     * @param imageUrl 评论图片地址。
     * @return 服务端创建成功后的评论 DTO。
     */
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

    /**
     * 切换评论点赞状态。
     *
     * @param commentId 评论 ID。
     * @param value 目标点赞状态。
     * @return 无返回值，请求成功即视为状态切换完成。
     */
    override suspend fun toggleCommentLike(commentId: String, value: Boolean) = withContext(Dispatchers.IO) {
        apiService.toggleCommentLike(commentId, RemoteFollowToggleRequestDto(value)).requireData()
        Unit
    }
}
