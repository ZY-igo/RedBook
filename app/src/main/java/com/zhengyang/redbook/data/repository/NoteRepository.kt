/**
 * 文件说明：NoteRepository.kt
 * 作用：定义笔记详情场景的数据访问接口。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.remote.model.RemoteCommentDto
import com.zhengyang.redbook.data.remote.model.RemoteNoteDetailDto

/**
 * 笔记详情仓储接口
 *
 * 负责聚合笔记详情页所需的数据读取和互动操作，
 * 包括点赞、收藏、关注作者以及评论相关行为。
 */
interface NoteRepository {

    /**
     * 获取笔记详情。
     *
     * @param noteId 笔记 ID。
     * @return 服务端返回的笔记详情 DTO。
     */
    suspend fun getNoteDetail(noteId: String): RemoteNoteDetailDto

    /**
     * 获取笔记评论列表。
     *
     * @param noteId 笔记 ID。
     * @param sortMode 排序模式，如 `default`、`latest`、`most_liked`。
     * @param offset 偏移量。
     * @param limit 单次请求数量。
     * @return 当前分页范围内的评论列表。
     */
    suspend fun getComments(noteId: String, sortMode: String, offset: Int = 0, limit: Int = 20): List<RemoteCommentDto>

    /**
     * 切换笔记点赞状态。
     *
     * @param noteId 笔记 ID。
     * @param value 目标点赞状态。
     */
    suspend fun toggleLike(noteId: String, value: Boolean)

    /**
     * 切换笔记收藏状态。
     *
     * @param noteId 笔记 ID。
     * @param value 目标收藏状态。
     */
    suspend fun toggleCollect(noteId: String, value: Boolean)

    /**
     * 切换作者关注状态。
     *
     * @param userId 作者用户 ID。
     * @param value 目标关注状态。
     */
    suspend fun followAuthor(userId: String, value: Boolean)

    /**
     * 发送评论或回复。
     *
     * @param noteId 笔记 ID。
     * @param content 评论文本内容。
     * @param parentCommentId 父评论 ID；为空时表示一级评论。
     * @param replyToUserName 被回复用户名称。
     * @param imageUrl 评论附图地址。
     * @return 服务端创建成功后的评论 DTO。
     */
    suspend fun createComment(
        noteId: String,
        content: String,
        parentCommentId: String? = null,
        replyToUserName: String? = null,
        imageUrl: String? = null
    ): RemoteCommentDto

    /**
     * 切换评论点赞状态。
     *
     * @param commentId 评论 ID。
     * @param value 目标点赞状态。
     */
    suspend fun toggleCommentLike(commentId: String, value: Boolean)
}
