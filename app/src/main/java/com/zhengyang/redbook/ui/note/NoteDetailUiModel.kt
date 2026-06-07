/**
 * 文件说明：NoteDetailUiModel.kt
 * 作用：定义笔记详情页使用的界面展示模型。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.note

import com.zhengyang.redbook.data.model.MediaType

/**
 * 笔记详情页总展示模型
 *
 * 用于承载详情页头部、作者信息、媒体信息和互动状态等核心展示数据，
 * 便于页面在不同布局模式下复用统一的数据结构。
 */
data class NoteDetailUiModel(
    /** 笔记唯一标识。 */
    val id: String,
    /** 笔记标题。 */
    val title: String,
    /** 笔记正文描述。 */
    val description: String,
    /** 作者信息展示模型。 */
    val author: UserUiModel,
    /** 媒体信息展示模型。 */
    val media: MediaUiModel,
    /** 点赞数展示文案。 */
    val likeCount: String,
    /** 评论数展示文案。 */
    val commentCount: String,
    /** 收藏数展示文案。 */
    val collectCount: String,
    /** 当前用户是否已点赞。 */
    val isLiked: Boolean,
    /** 当前用户是否已收藏。 */
    val isCollected: Boolean,
    /** 当前用户是否已关注作者。 */
    val isFollowingAuthor: Boolean,
    /** 笔记发布时间文案。 */
    val createdAt: String,
    /** 笔记标签列表。 */
    val tags: List<String>
)

/**
 * 用户展示模型
 *
 * 用于承载详情页作者或相关用户的基础展示信息。
 */
data class UserUiModel(
    /** 用户唯一标识。 */
    val id: String,
    /** 用户昵称。 */
    val name: String,
    /** 头像占位文本。 */
    val avatarText: String,
    /** 头像背景色值。 */
    val avatarColorHex: String,
    /** 用户简介，可为空。 */
    val bio: String? = null,
    /** 是否显示认证状态。 */
    val isVerified: Boolean = false
)

/**
 * 媒体展示模型
 *
 * 用于描述详情页中图片或视频内容的展示资源。
 */
data class MediaUiModel(
    /** 媒体类型。 */
    val type: MediaType,
    /** 图文场景下的图片地址列表。 */
    val imageUrls: List<String> = emptyList(),
    /** 视频播放地址。 */
    val videoUrl: String? = null,
    /** 视频或图片封面地址。 */
    val coverUrl: String? = null
)
