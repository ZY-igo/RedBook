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
