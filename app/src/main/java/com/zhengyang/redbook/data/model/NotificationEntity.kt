package com.zhengyang.redbook.data.model

data class NotificationEntity(
    val id: String,
    val type: NotificationType,
    val fromUserId: String,
    val fromUserName: String,
    val fromUserAvatar: String,
    val noteId: String?,
    val noteTitle: String?,
    val noteCoverUrl: String?,
    val content: String,
    val isRead: Boolean,
    val createdAt: Long
)
