package com.zhengyang.redbook.data.model

data class CommentReplyEntity(
    val id: String,
    val commentId: String,
    val noteId: String,
    val authorId: String,
    val authorName: String,
    val avatarText: String,
    val content: String,
    val replyToId: String?,
    val replyToName: String?,
    val likeCount: Int,
    val createdAt: Long,
    val isLiked: Boolean
)
