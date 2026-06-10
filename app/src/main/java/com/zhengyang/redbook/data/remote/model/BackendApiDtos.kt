/**
 * 文件说明：BackendApiDtos.kt
 * 作用：定义 remote 层请求体与响应体使用的数据传输模型。
 * 备注：DTO 只描述远端协议字段，不直接承载页面交互状态或复杂业务逻辑。
 */
package com.zhengyang.redbook.data.remote.model

/**
 * 通用接口响应壳。
 *
 * 多数接口都会返回统一结构：
 * 1. `success` 表示业务是否成功。
 * 2. `code` 表示后端业务码。
 * 3. `message` 提供错误说明或调试信息。
 * 4. `data` 承载真实业务数据。
 */
data class ApiResponseDto<T>(
    val success: Boolean,
    val code: String,
    val message: String,
    val data: T?
)

/** 通用分页响应模型，适用于 feed、评论等列表接口。 */
data class PageResponseDto<T>(
    val items: List<T>,
    val offset: Int,
    val limit: Int,
    val total: Long,
    val hasMore: Boolean
)

/** 首页频道分类 DTO，用于驱动首页 tab 和频道排序。 */
data class RemoteHomeCategoryDto(
    val id: String,
    val title: String,
    val bucket: String,
    val usesWaterfall: Boolean,
    val sortOrder: Int,
    val defaultSelected: Boolean
)

/**
 * 首页 feed 卡片 DTO。
 *
 * 该模型兼容图文和视频两种内容形态，因此同时保留图片、视频和视觉样式相关字段。
 */
data class RemoteFeedItemDto(
    val id: String,
    val sectionKey: String? = null,
    val title: String,
    val authorId: String? = null,
    val author: String,
    val avatarUrl: String? = null,
    val likeCount: String? = null,
    val badge: String? = null,
    val coverLabel: String? = null,
    val coverHeightDp: Int? = null,
    val mediaType: String,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val videoCoverUrl: String? = null,
    val startColorHex: String? = null,
    val endColorHex: String? = null,
    val avatarColorHex: String? = null,
    val description: String? = null,
    val coverUrl: String? = null
)

/** 关注页推荐用户 DTO。 */
data class RemoteFollowingUserDto(
    val id: String,
    val name: String,
    val subtitle: String,
    val avatarUrl: String? = null,
    val avatarColorHex: String,
    val badge: String? = null,
    val followed: Boolean = false
)

/** 关注页首屏种子数据，同时包含推荐用户和关注 feed。 */
data class RemoteFollowingSeedDto(
    val suggestedUsers: List<RemoteFollowingUserDto>,
    val followingFeedItems: PageResponseDto<RemoteFeedItemDto>
)

/** 消息页列表行 DTO。 */
data class RemoteMessageRowDto(
    val id: String,
    val title: String,
    val subtitle: String,
    val timeText: String,
    val backgroundKey: String,
    val iconKey: String? = null,
    val avatarText: String? = null,
    val iconSizeDp: Int,
    val showsVerifiedBadge: Boolean,
    val showsRedDot: Boolean
)

/** 消息页推荐人物 DTO。 */
data class RemotePersonSuggestionDto(
    val id: String,
    val avatarText: String,
    val name: String,
    val subtitle: String,
    val avatarBackgroundKey: String
)

/** 消息页概览 DTO，把消息列表和推荐人物合并返回。 */
data class RemoteMessageOverviewDto(
    val messageRows: List<RemoteMessageRowDto>,
    val peopleSuggestions: List<RemotePersonSuggestionDto>
)

/** 当前登录用户资料 DTO。 */
data class RemoteMyProfileDto(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val avatarText: String,
    val avatarColorHex: String,
    val bio: String? = null,
    val followingCount: Int,
    val fansCount: Int,
    val likesCount: Int,
    val noteCount: Int
)

/** “我感兴趣的人”推荐卡片 DTO。 */
data class RemoteMyInterestPersonDto(
    val id: String,
    val avatarText: String,
    val name: String,
    val fansText: String,
    val avatarBackgroundKey: String,
    val followed: Boolean = false
)

/** 搜索页启动数据 DTO，包含历史记录和猜词列表。 */
data class RemoteSearchBootstrapDto(
    val historyItems: List<String>,
    val guessItems: List<RemoteSearchGuessDto>
)

/** 搜索联想词 DTO。 */
data class RemoteSearchGuessDto(
    val id: String,
    val title: String,
    val meta: String
)

/** 搜索结果项 DTO。 */
data class RemoteSearchResultDto(
    val id: String,
    val keyword: String,
    val filter: String,
    val title: String,
    val subtitle: String,
    val meta: String,
    val badge: String,
    val badgeColorKey: String
)

/** 通用布尔切换请求体，适用于关注、点赞、收藏等开关式接口。 */
data class RemoteFollowToggleRequestDto(
    val value: Boolean
)

/**
 * 笔记详情 DTO。
 *
 * 该模型是详情页远端数据的核心载体，包含媒体内容、作者信息、互动状态和统计字段。
 */
data class RemoteNoteDetailDto(
    val id: String,
    val title: String,
    val description: String,
    val author: RemoteNoteAuthorDto,
    val mediaType: String,
    val imageUrls: List<String>,
    val videoUrl: String? = null,
    val coverUrl: String? = null,
    val likeCount: Int,
    val commentCount: Int,
    val collectCount: Int,
    val liked: Boolean,
    val collected: Boolean,
    val followingAuthor: Boolean,
    val createdAt: String,
    val tags: List<String>
)

/** 笔记作者 DTO。 */
data class RemoteNoteAuthorDto(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val avatarText: String,
    val avatarColorHex: String,
    val bio: String? = null,
    val location: String? = null,
    val birthday: String? = null,
    val gender: String? = null,
    val job: String? = null,
    val school: String? = null,
    val followingCount: Int = 0,
    val fansCount: Int = 0,
    val likesCount: Int = 0,
    val noteCount: Int = 0,
    val verified: Boolean = false,
    val following: Boolean = false
)

/** 评论分页 DTO。当前项目更多直接复用 `PageResponseDto<RemoteCommentDto>`，这里保留作兼容结构。 */
data class RemoteCommentPageDto(
    val items: List<RemoteCommentDto>,
    val offset: Int,
    val limit: Int,
    val total: Long,
    val hasMore: Boolean
)

/**
 * 评论 DTO。
 *
 * 一个评论既包含自身信息，也带有首层回复列表，
 * 便于详情页一次性渲染评论树的第一层结构。
 */
data class RemoteCommentDto(
    val id: String,
    val noteId: String,
    val authorId: String,
    val author: String,
    val content: String,
    val city: String,
    val createdAt: String,
    val likeCount: Int,
    val liked: Boolean,
    val authorFlag: Boolean,
    val avatarUrl: String? = null,
    val avatarText: String,
    val avatarColorHex: String,
    val imageUrl: String? = null,
    val replies: List<RemoteReplyDto>
)

/** 评论回复 DTO。 */
data class RemoteReplyDto(
    val id: String,
    val authorId: String,
    val author: String,
    val content: String,
    val city: String,
    val createdAt: String,
    val likeCount: Int,
    val liked: Boolean,
    val authorFlag: Boolean,
    val avatarUrl: String? = null,
    val avatarText: String,
    val avatarColorHex: String,
    val imageUrl: String? = null,
    val replyToName: String? = null
)

/**
 * 创建评论请求 DTO。
 *
 * `parentCommentId` 和 `replyToUserName` 共同表达“正在回复谁”的上下文；
 * 当两者都为空时，表示发布一级评论。
 */
data class RemoteCreateCommentRequestDto(
    val noteId: String,
    val parentCommentId: String? = null,
    val replyToUserName: String? = null,
    val content: String,
    val city: String? = null,
    val imageUrl: String? = null
)

/** 创建笔记请求 DTO。 */
data class RemoteCreateNoteRequestDto(
    val title: String,
    val description: String? = null,
    val mediaType: String,
    val mediaUrls: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)

/** 草稿 DTO，用于草稿箱展示和发布页内容恢复。 */
data class RemoteDraftDto(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val mediaUrls: List<String>,
    val mediaType: String,
    val location: String? = null,
    val tags: List<String>,
    val savedAt: String,
    val autoSaved: Boolean
)

/** 保存草稿请求 DTO。 */
data class RemoteSaveDraftRequestDto(
    val id: String? = null,
    val title: String,
    val description: String,
    val mediaType: String,
    val mediaUrls: List<String> = emptyList(),
    val location: String? = null,
    val tags: List<String> = emptyList(),
    val autoSaved: Boolean
)
