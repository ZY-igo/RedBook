/**
 * 文件说明： NoteDetailUiModel.kt
 * 作用： 承载笔记详情展示、播放控制和相关界面行为。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
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
