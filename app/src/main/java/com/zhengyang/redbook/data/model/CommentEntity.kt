/**
 * 文件说明：CommentEntity.kt
 * 作用：定义 Comment Entity 相关本地数据实体结构。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.model

data class CommentEntity(
    val id: String,
    val noteId: String,
    val authorId: String,
    val authorName: String,
    val avatarText: String,
    val content: String,
    val likeCount: Int,
    val replyCount: Int,
    val createdAt: Long,
    val isAuthor: Boolean,
    val isLiked: Boolean
)
