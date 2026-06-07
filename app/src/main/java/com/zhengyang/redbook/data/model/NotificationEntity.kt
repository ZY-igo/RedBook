/**
 * 文件说明：NotificationEntity.kt
 * 作用：定义 Notification Entity 相关本地数据实体结构。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.model

/**
 * 通知实体
 *
 * 用于表示一条与用户互动相关的通知消息，
 * 包括点赞、评论、回复、关注和系统通知等场景。
 */
data class NotificationEntity(
    /** 通知唯一标识。 */
    val id: String,

    /** 通知类型。 */
    val type: NotificationType,

    /** 触发通知的来源用户 ID。 */
    val fromUserId: String,

    /** 来源用户名称。 */
    val fromUserName: String,

    /** 来源用户头像地址。 */
    val fromUserAvatar: String,

    /** 关联笔记 ID，可为空。 */
    val noteId: String?,

    /** 关联笔记标题，可为空。 */
    val noteTitle: String?,

    /** 关联笔记封面地址，可为空。 */
    val noteCoverUrl: String?,

    /** 通知正文内容。 */
    val content: String,

    /** 当前通知是否已读。 */
    val isRead: Boolean,

    /** 通知创建时间戳，单位为毫秒。 */
    val createdAt: Long
)
