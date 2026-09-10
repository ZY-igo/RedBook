package com.zhengyang.redbook.data.mock

import com.zhengyang.redbook.data.remote.model.ApiResponseDto
import com.zhengyang.redbook.data.remote.model.PageResponseDto
import com.zhengyang.redbook.data.remote.model.RemoteCommentDto
import com.zhengyang.redbook.data.remote.model.RemoteCreateCommentRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteCreateNoteRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteDraftDto
import com.zhengyang.redbook.data.remote.model.RemoteFeedItemDto
import com.zhengyang.redbook.data.remote.model.RemoteFollowToggleRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteFollowingSeedDto
import com.zhengyang.redbook.data.remote.model.RemoteFollowingUserDto
import com.zhengyang.redbook.data.remote.model.RemoteHomeCategoryDto
import com.zhengyang.redbook.data.remote.model.RemoteMessageOverviewDto
import com.zhengyang.redbook.data.remote.model.RemoteMessageRowDto
import com.zhengyang.redbook.data.remote.model.RemoteMyInterestPersonDto
import com.zhengyang.redbook.data.remote.model.RemoteMyProfileDto
import com.zhengyang.redbook.data.remote.model.RemoteNoteAuthorDto
import com.zhengyang.redbook.data.remote.model.RemoteNoteDetailDto
import com.zhengyang.redbook.data.remote.model.RemotePersonSuggestionDto
import com.zhengyang.redbook.data.remote.model.RemotePushTestRequestDto
import com.zhengyang.redbook.data.remote.model.RemotePushTokenRegistrationRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteSaveDraftRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteSearchBootstrapDto
import com.zhengyang.redbook.data.remote.model.RemoteSearchGuessDto
import com.zhengyang.redbook.data.remote.model.RemoteSearchResultDto
import com.zhengyang.redbook.data.remote.RedBookApiService
import kotlinx.coroutines.delay
import okhttp3.MultipartBody

class MockRedBookApiService : RedBookApiService {

    private fun <T> success(data: T): ApiResponseDto<T> {
        return ApiResponseDto(
            success = true,
            code = "0",
            message = "success",
            data = data
        )
    }

    private val mockCategories = listOf(
        RemoteHomeCategoryDto("1", "推荐", "discovery", true, 1, true),
        RemoteHomeCategoryDto("2", "关注", "following", true, 2, false),
        RemoteHomeCategoryDto("3", "美食", "discovery", true, 3, false),
        RemoteHomeCategoryDto("4", "旅行", "discovery", true, 4, false),
        RemoteHomeCategoryDto("5", "时尚", "discovery", true, 5, false)
    )

    private val mockFeedItems = listOf(
        RemoteFeedItemDto(
            id = "1001",
            title = "周末在家做了这道菜，全家人都说太香了！",
            author = "美食达人小王",
            avatarUrl = "https://picsum.photos/seed/avatar1/100/100",
            likeCount = "2.3万",
            badge = "收藏",
            coverLabel = "家常菜",
            coverHeightDp = 200,
            mediaType = "IMAGE",
            imageUrl = "https://picsum.photos/seed/food1/800/600"
        ),
        RemoteFeedItemDto(
            id = "1002",
            title = "秋冬必学的穿搭技巧，让你回头率爆表",
            author = "时尚博主Lisa",
            avatarUrl = "https://picsum.photos/seed/avatar2/100/100",
            likeCount = "1.8万",
            badge = "热门",
            coverLabel = "穿搭",
            coverHeightDp = 220,
            mediaType = "IMAGE",
            imageUrl = "https://picsum.photos/seed/fashion1/800/600"
        ),
        RemoteFeedItemDto(
            id = "1003",
            title = "日本旅行日记｜超详细的东京大阪7日游攻略",
            author = "旅行爱好者",
            avatarUrl = "https://picsum.photos/seed/avatar3/100/100",
            likeCount = "5.6万",
            badge = "编辑推荐",
            coverLabel = "旅行",
            coverHeightDp = 180,
            mediaType = "IMAGE",
            imageUrl = "https://picsum.photos/seed/travel1/800/600"
        ),
        RemoteFeedItemDto(
            id = "1004",
            title = "新手化妆入门必看！保姆级教程",
            author = "美妆达人小雅",
            avatarUrl = "https://picsum.photos/seed/avatar4/100/100",
            likeCount = "3.2万",
            badge = null,
            coverLabel = "美妆",
            coverHeightDp = 200,
            mediaType = "IMAGE",
            imageUrl = "https://picsum.photos/seed/beauty1/800/600"
        ),
        RemoteFeedItemDto(
            id = "1005",
            title = "居家健身计划｜每天15分钟，一个月见效",
            author = "健身教练老王",
            avatarUrl = "https://picsum.photos/seed/avatar5/100/100",
            likeCount = "4.1万",
            badge = "精选",
            coverLabel = "健身",
            coverHeightDp = 190,
            mediaType = "IMAGE",
            imageUrl = "https://picsum.photos/seed/fitness1/800/600"
        )
    )

    private val mockProfile = RemoteMyProfileDto(
        id = "current_user",
        name = "测试用户",
        avatarUrl = "https://picsum.photos/seed/me/200/200",
        avatarText = "测",
        avatarColorHex = "#FFB6C1",
        bio = "热爱技术，享受生活",
        followingCount = 256,
        fansCount = 1234,
        likesCount = 8976,
        noteCount = 45
    )

    override suspend fun getHomeCategories(): ApiResponseDto<List<RemoteHomeCategoryDto>> {
        delay(300)
        return success(mockCategories)
    }

    override suspend fun getHomeFeed(
        categoryId: String,
        offset: Int,
        limit: Int
    ): ApiResponseDto<PageResponseDto<RemoteFeedItemDto>> {
        delay(500)
        return success(
            PageResponseDto(
                items = mockFeedItems,
                offset = offset,
                limit = limit,
                total = mockFeedItems.size.toLong(),
                hasMore = true
            )
        )
    }

    override suspend fun getFollowingSeed(
        offset: Int,
        limit: Int
    ): ApiResponseDto<RemoteFollowingSeedDto> {
        delay(400)
        return success(
            RemoteFollowingSeedDto(
                suggestedUsers = listOf(
                    RemoteFollowingUserDto("u1", "摄影达人", "风光摄影师", "https://picsum.photos/seed/avatar6/100/100", "#FFB6C1", followed = false),
                    RemoteFollowingUserDto("u2", "美食家", "分享家常美食", "https://picsum.photos/seed/avatar7/100/100", "#98FB98", followed = true)
                ),
                followingFeedItems = PageResponseDto(
                    items = mockFeedItems.take(2),
                    offset = offset,
                    limit = limit,
                    total = 2,
                    hasMore = true
                )
            )
        )
    }

    override suspend fun followUser(
        userId: String,
        request: RemoteFollowToggleRequestDto
    ): ApiResponseDto<Map<String, Any?>> {
        delay(200)
        return success(mapOf("userId" to userId, "followed" to request.value))
    }

    override suspend fun getMessageOverview(): ApiResponseDto<RemoteMessageOverviewDto> {
        delay(300)
        return success(
            RemoteMessageOverviewDto(
                messageRows = listOf(
                    RemoteMessageRowDto("m1", "评论回复", "有人评论了你的笔记", "2小时前", "", null, null, 48, false, true),
                    RemoteMessageRowDto("m2", "新增关注", "你又有新粉丝啦", "昨天", "", null, null, 48, false, false),
                    RemoteMessageRowDto("m3", "点赞通知", "你的笔记被点赞了", "3天前", "", null, null, 48, false, false)
                ),
                peopleSuggestions = listOf(
                    RemotePersonSuggestionDto("p1", "摄", "摄影达人", "风光摄影师", ""),
                    RemotePersonSuggestionDto("p2", "美", "美妆达人", "分享美妆技巧", "")
                )
            )
        )
    }

    override suspend fun getMyProfile(): ApiResponseDto<RemoteMyProfileDto> {
        delay(200)
        return success(mockProfile)
    }

    override suspend fun uploadMyAvatar(file: MultipartBody.Part): ApiResponseDto<RemoteMyProfileDto> {
        delay(1000)
        return success(mockProfile.copy(avatarUrl = "https://picsum.photos/seed/newavatar/200/200"))
    }

    override suspend fun getMyInterests(): ApiResponseDto<List<RemoteMyInterestPersonDto>> {
        delay(300)
        return success(
            listOf(
                RemoteMyInterestPersonDto("ip1", "摄", "摄影达人", "1.2万", "", false),
                RemoteMyInterestPersonDto("ip2", "美", "美妆达人", "8千", "", true)
            )
        )
    }

    override suspend fun getNoteDetail(noteId: String): ApiResponseDto<RemoteNoteDetailDto> {
        delay(300)
        return success(
            RemoteNoteDetailDto(
                id = noteId,
                title = "日本旅行日记｜超详细的东京大阪7日游攻略",
                description = "这次日本之旅真的太棒了！分享一些实用的旅行tips给大家...",
                author = RemoteNoteAuthorDto("u1", "旅行爱好者", "https://picsum.photos/seed/avatar2/100/100", "旅", "#FFB6C1"),
                mediaType = "IMAGE",
                imageUrls = listOf(
                    "https://picsum.photos/seed/japan1/800/600",
                    "https://picsum.photos/seed/japan2/800/600",
                    "https://picsum.photos/seed/japan3/800/600"
                ),
                videoUrl = null,
                coverUrl = "https://picsum.photos/seed/japan1/800/600",
                likeCount = 56000,
                commentCount = 890,
                collectCount = 12000,
                liked = false,
                collected = false,
                followingAuthor = false,
                createdAt = "2024-01-15 10:30",
                tags = listOf("日本", "旅行", "东京", "大阪")
            )
        )
    }

    override suspend fun getNoteComments(
        noteId: String,
        sortMode: String,
        offset: Int,
        limit: Int
    ): ApiResponseDto<PageResponseDto<RemoteCommentDto>> {
        delay(300)
        return success(
            PageResponseDto(
                items = emptyList(),
                offset = offset,
                limit = limit,
                total = 0,
                hasMore = false
            )
        )
    }

    override suspend fun toggleNoteLike(
        noteId: String,
        request: RemoteFollowToggleRequestDto
    ): ApiResponseDto<Map<String, Any?>> {
        delay(200)
        return success(mapOf("noteId" to noteId, "liked" to request.value))
    }

    override suspend fun toggleNoteCollect(
        noteId: String,
        request: RemoteFollowToggleRequestDto
    ): ApiResponseDto<Map<String, Any?>> {
        delay(200)
        return success(mapOf("noteId" to noteId, "collected" to request.value))
    }

    override suspend fun createComment(request: RemoteCreateCommentRequestDto): ApiResponseDto<RemoteCommentDto> {
        delay(200)
        return success(
            RemoteCommentDto(
                id = "new_comment_${System.currentTimeMillis()}",
                noteId = request.noteId ?: "",
                authorId = "current_user",
                author = "测试用户",
                content = request.content,
                city = "",
                createdAt = "刚刚",
                likeCount = 0,
                liked = false,
                authorFlag = false,
                avatarUrl = "https://picsum.photos/seed/me/100/100",
                avatarText = "测",
                avatarColorHex = "#FFB6C1",
                imageUrl = null,
                replies = emptyList()
            )
        )
    }

    override suspend fun toggleCommentLike(
        commentId: String,
        request: RemoteFollowToggleRequestDto
    ): ApiResponseDto<Map<String, Any?>> {
        delay(200)
        return success(mapOf("commentId" to commentId, "liked" to request.value))
    }

    override suspend fun getSearchBootstrap(): ApiResponseDto<RemoteSearchBootstrapDto> {
        delay(200)
        return success(
            RemoteSearchBootstrapDto(
                historyItems = listOf("日本旅行", "护肤", "健身计划"),
                guessItems = listOf(
                    RemoteSearchGuessDto("g1", "秋冬穿搭", ""),
                    RemoteSearchGuessDto("g2", "减脂餐", ""),
                    RemoteSearchGuessDto("g3", "新手化妆", "")
                )
            )
        )
    }

    override suspend fun getSearchSuggestions(query: String): ApiResponseDto<List<RemoteSearchGuessDto>> {
        delay(200)
        val guesses = listOf(
            RemoteSearchGuessDto("g1", "秋冬穿搭", ""),
            RemoteSearchGuessDto("g2", "减脂餐", ""),
            RemoteSearchGuessDto("g3", "新手化妆", ""),
            RemoteSearchGuessDto("g4", "露营装备", ""),
            RemoteSearchGuessDto("g5", "咖啡探店", "")
        )
        return success(guesses.filter { it.title.contains(query) })
    }

    override suspend fun getSearchResults(query: String): ApiResponseDto<List<RemoteSearchResultDto>> {
        delay(400)
        return success(
            listOf(
                RemoteSearchResultDto("s1", query, "", "日本东京5日游全攻略", "旅行爱好者", "5.6万", "", ""),
                RemoteSearchResultDto("s2", query, "", "日本京都赏枫指南", "旅游博主", "3.2万", "", ""),
                RemoteSearchResultDto("s3", query, "", "日本美食地图｜东京必吃清单", "美食家", "8.9万", "", "")
            )
        )
    }

    override suspend fun createNote(request: RemoteCreateNoteRequestDto): ApiResponseDto<RemoteNoteDetailDto> {
        delay(500)
        return success(
            RemoteNoteDetailDto(
                id = "new_note_${System.currentTimeMillis()}",
                title = request.title,
                description = request.description ?: "",
                author = RemoteNoteAuthorDto("current_user", "测试用户", "https://picsum.photos/seed/me/100/100", "测", "#FFB6C1"),
                mediaType = request.mediaType,
                imageUrls = request.mediaUrls,
                videoUrl = null,
                coverUrl = null,
                likeCount = 0,
                commentCount = 0,
                collectCount = 0,
                liked = false,
                collected = false,
                followingAuthor = false,
                createdAt = "刚刚",
                tags = request.tags
            )
        )
    }

    override suspend fun getDrafts(): ApiResponseDto<List<RemoteDraftDto>> {
        delay(200)
        return success(emptyList())
    }

    override suspend fun saveDraft(request: RemoteSaveDraftRequestDto): ApiResponseDto<RemoteDraftDto> {
        delay(200)
        return success(
            RemoteDraftDto(
                id = request.id ?: "draft_${System.currentTimeMillis()}",
                userId = "current_user",
                title = request.title,
                description = request.description,
                mediaUrls = request.mediaUrls,
                mediaType = request.mediaType,
                location = request.location,
                tags = request.tags,
                savedAt = "刚刚",
                autoSaved = request.autoSaved
            )
        )
    }

    override suspend fun registerPushToken(request: RemotePushTokenRegistrationRequestDto): ApiResponseDto<Map<String, Any?>> {
        delay(100)
        return success(mapOf("token" to request.token, "registered" to true))
    }

    override suspend fun unregisterPushToken(request: RemotePushTokenRegistrationRequestDto): ApiResponseDto<Map<String, Any?>> {
        delay(100)
        return success(mapOf("token" to request.token, "unregistered" to true))
    }

    override suspend fun sendPushTest(request: RemotePushTestRequestDto): ApiResponseDto<Map<String, Any?>> {
        delay(200)
        return success(mapOf("sent" to true, "title" to request.title))
    }
}
