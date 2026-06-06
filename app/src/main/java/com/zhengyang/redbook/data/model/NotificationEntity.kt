/**
 * 文件说明： NotificationEntity.kt
 * 作用： 定义数据层使用的模型结构，用于持久化、映射转换或仓储内部的数据传递。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
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
