package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.model.DiscoverCategory
import com.zhengyang.redbook.data.model.FollowingUser
import com.zhengyang.redbook.data.model.HomeDiscoverItem
import com.zhengyang.redbook.data.model.MediaType
import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.requireData
import com.zhengyang.redbook.data.remote.model.RemoteFeedItemDto
import com.zhengyang.redbook.data.remote.model.RemoteFollowingUserDto
import com.zhengyang.redbook.data.remote.model.RemoteHomeCategoryDto
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeRepositoryImpl @Inject constructor(
    private val apiService: RedBookApiService
) : HomeRepository {

    override suspend fun getCategories(): List<DiscoverCategory> = withContext(Dispatchers.IO) {
        apiService.getHomeCategories().requireData()
            .sortedBy(RemoteHomeCategoryDto::sortOrder)
            .map { it.toDomain() }
    }

    override suspend fun getDiscoverItems(categoryId: String): List<HomeDiscoverItem> {
        return getDiscoverItemsPage(categoryId = categoryId, offset = 0, limit = DEFAULT_PAGE_SIZE)
    }

    override suspend fun getDiscoverItemsPage(
        categoryId: String,
        offset: Int,
        limit: Int
    ): List<HomeDiscoverItem> = withContext(Dispatchers.IO) {
        apiService.getHomeFeed(categoryId = categoryId, offset = offset, limit = limit)
            .requireData()
            .items
            .map { it.toDomain() }
    }

    override suspend fun getSuggestedFollowingUsers(): List<FollowingUser> = withContext(Dispatchers.IO) {
        apiService.getFollowingSeed(offset = 0, limit = DEFAULT_FOLLOWING_PAGE_SIZE)
            .requireData()
            .suggestedUsers
            .filterNot(RemoteFollowingUserDto::followed)
            .map { it.toDomain() }
    }

    override suspend fun getFollowingFeedItems(): List<HomeDiscoverItem> {
        return getFollowingFeedItemsPage(offset = 0, limit = DEFAULT_FOLLOWING_PAGE_SIZE)
    }

    override suspend fun getFollowingFeedItemsPage(offset: Int, limit: Int): List<HomeDiscoverItem> = withContext(Dispatchers.IO) {
        apiService.getFollowingSeed(offset = offset, limit = limit)
            .requireData()
            .followingFeedItems
            .items
            .map { it.toDomain() }
    }

    override suspend fun followUser(userId: String) = withContext(Dispatchers.IO) {
        apiService.followUser(userId).requireData()
        Unit
    }

    private fun RemoteHomeCategoryDto.toDomain(): DiscoverCategory {
        return DiscoverCategory(
            id = id,
            title = title,
            bucket = bucket,
            usesWaterfall = usesWaterfall,
            isDefaultSelected = defaultSelected
        )
    }

    private fun RemoteFollowingUserDto.toDomain(): FollowingUser {
        return FollowingUser(
            id = id,
            name = name,
            subtitle = subtitle,
            avatarColorHex = avatarColorHex,
            badge = badge
        )
    }

    private fun RemoteFeedItemDto.toDomain(): HomeDiscoverItem {
        val resolvedImageUrl = imageUrl ?: videoCoverUrl ?: coverUrl
        return HomeDiscoverItem(
            id = id,
            title = title,
            author = author,
            likeCount = likeCount.orEmpty().ifBlank { "0" },
            badge = badge.orEmpty(),
            coverLabel = coverLabel.orEmpty(),
            coverHeightDp = coverHeightDp ?: DEFAULT_COVER_HEIGHT_DP,
            mediaType = mediaType.toMediaType(),
            imageUrls = listOfNotNull(resolvedImageUrl),
            imageUrl = resolvedImageUrl,
            videoUrl = videoUrl,
            videoCoverUrl = videoCoverUrl ?: coverUrl ?: imageUrl,
            startColorHex = startColorHex ?: DEFAULT_START_COLOR_HEX,
            endColorHex = endColorHex ?: DEFAULT_END_COLOR_HEX,
            avatarColorHex = avatarColorHex ?: DEFAULT_AVATAR_COLOR_HEX
        )
    }

    private fun String.toMediaType(): MediaType {
        return when (uppercase()) {
            MediaType.VIDEO.name -> MediaType.VIDEO
            MediaType.TEXT.name -> MediaType.TEXT
            MediaType.LONG_FORM.name -> MediaType.LONG_FORM
            else -> MediaType.IMAGE
        }
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 10
        const val DEFAULT_FOLLOWING_PAGE_SIZE = 8
        const val DEFAULT_COVER_HEIGHT_DP = 220
        const val DEFAULT_START_COLOR_HEX = "#D8082B"
        const val DEFAULT_END_COLOR_HEX = "#6D000F"
        const val DEFAULT_AVATAR_COLOR_HEX = "#FF8A9F"
    }
}
