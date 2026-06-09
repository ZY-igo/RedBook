/**
 * 文件说明：BackendApiDtos.kt
 * 作用：定义远端接口返回与请求使用的数据传输模型。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.remote.model

data class ApiResponseDto<T>(
    val success: Boolean,
    val code: String,
    val message: String,
    val data: T?
)

data class PageResponseDto<T>(
    val items: List<T>,
    val offset: Int,
    val limit: Int,
    val total: Long,
    val hasMore: Boolean
)

data class RemoteHomeCategoryDto(
    val id: String,
    val title: String,
    val bucket: String,
    val usesWaterfall: Boolean,
    val sortOrder: Int,
    val defaultSelected: Boolean
)

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

data class RemoteFollowingUserDto(
    val id: String,
    val name: String,
    val subtitle: String,
    val avatarUrl: String? = null,
    val avatarColorHex: String,
    val badge: String? = null,
    val followed: Boolean = false
)

data class RemoteFollowingSeedDto(
    val suggestedUsers: List<RemoteFollowingUserDto>,
    val followingFeedItems: PageResponseDto<RemoteFeedItemDto>
)

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

data class RemotePersonSuggestionDto(
    val id: String,
    val avatarText: String,
    val name: String,
    val subtitle: String,
    val avatarBackgroundKey: String
)

data class RemoteMessageOverviewDto(
    val messageRows: List<RemoteMessageRowDto>,
    val peopleSuggestions: List<RemotePersonSuggestionDto>
)

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

data class RemoteMyInterestPersonDto(
    val id: String,
    val avatarText: String,
    val name: String,
    val fansText: String,
    val avatarBackgroundKey: String,
    val followed: Boolean = false
)

data class RemoteSearchBootstrapDto(
    val historyItems: List<String>,
    val guessItems: List<RemoteSearchGuessDto>
)

data class RemoteSearchGuessDto(
    val id: String,
    val title: String,
    val meta: String
)

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

data class RemoteFollowToggleRequestDto(
    val value: Boolean
)

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

data class RemoteCommentPageDto(
    val items: List<RemoteCommentDto>,
    val offset: Int,
    val limit: Int,
    val total: Long,
    val hasMore: Boolean
)

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

data class RemoteCreateCommentRequestDto(
    val noteId: String,
    val parentCommentId: String? = null,
    val replyToUserName: String? = null,
    val content: String,
    val city: String? = null,
    val imageUrl: String? = null
)

data class RemoteCreateNoteRequestDto(
    val title: String,
    val description: String? = null,
    val mediaType: String,
    val mediaUrls: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)

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
