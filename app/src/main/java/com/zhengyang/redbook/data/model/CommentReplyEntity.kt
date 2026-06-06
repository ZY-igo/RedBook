/**
 * 文件说明：CommentReplyEntity.kt
 * 作用：定义评论回复数据模型，用于承载回复内容、作者信息以及交互状态。
 * 备注：该实体通常作为评论区二级回复的基础结构，被列表展示、排序和状态更新逻辑复用。
 */
package com.zhengyang.redbook.data.model

/**
 * 表示一条评论回复记录。
 *
 * 该模型完整描述了回复所属的评论、回复作者、被回复对象以及点赞状态，
 * 便于界面层直接渲染二级评论列表。
 */
data class CommentReplyEntity(
    /** 当前回复记录的唯一标识。 */
    val id: String,

    /** 当前回复所属的一级评论 ID。 */
    val commentId: String,

    /** 当前回复所属笔记的 ID，用于跨页面或仓储查询时定位数据来源。 */
    val noteId: String,

    /** 回复作者的用户 ID。 */
    val authorId: String,

    /** 回复作者在界面中展示的昵称。 */
    val authorName: String,

    /** 作者头像的占位文本，通常用于没有头像图时的首字或简称展示。 */
    val avatarText: String,

    /** 回复正文内容。 */
    val content: String,

    /** 被回复目标的回复 ID；如果为空，通常表示直接回复一级评论。 */
    val replyToId: String?,

    /** 被回复对象的展示名称，用于界面中拼接“回复某人”。 */
    val replyToName: String?,

    /** 当前回复累计获得的点赞数。 */
    val likeCount: Int,

    /** 回复创建时间戳，通常用于按时间排序或展示发布时间。 */
    val createdAt: Long,

    /** 当前用户是否已经对这条回复点过赞。 */
    val isLiked: Boolean
)
