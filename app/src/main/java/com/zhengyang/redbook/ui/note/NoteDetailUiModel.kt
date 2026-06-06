package com.zhengyang.redbook.ui.note

import com.zhengyang.redbook.data.model.MediaType

data class NoteDetailUiModel(
    val id: String,
    val title: String,
    val description: String,
    val author: UserUiModel,
    val media: MediaUiModel,
    val likeCount: String,
    val commentCount: String,
    val collectCount: String,
    val isLiked: Boolean,
    val isCollected: Boolean,
    val isFollowingAuthor: Boolean,
    val createdAt: String,
    val tags: List<String>
)

data class UserUiModel(
    val id: String,
    val name: String,
    val avatarText: String,
    val avatarColorHex: String,
    val bio: String? = null,
    val isVerified: Boolean = false
)

data class MediaUiModel(
    val type: MediaType,
    val imageUrls: List<String> = emptyList(),
    val videoUrl: String? = null,
    val coverUrl: String? = null
)
