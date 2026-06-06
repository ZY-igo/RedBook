package com.zhengyang.redbook.data.model

data class UserNoteEntity(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val mediaType: String,
    val likeCount: Int,
    val commentCount: Int,
    val shareCount: Int,
    val createdAt: Long,
    val isDraft: Boolean
)
