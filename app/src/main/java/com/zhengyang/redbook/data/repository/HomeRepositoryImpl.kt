/**
 * 文件说明：HomeRepositoryImpl.kt
 * 作用：实现首页场景仓储接口，负责远端首页数据的获取与映射。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
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

/**
 * 首页仓储实现类
 *
 * 负责调用首页相关远端接口，并将服务端 DTO 转换为界面和业务层可直接消费的领域模型。
 * 当前实现以远端数据源为主，并在仓储内处理分页默认值与字段兜底逻辑。
 */
class HomeRepositoryImpl @Inject constructor(
    /** 首页远端接口服务。 */
    private val apiService: RedBookApiService
) : HomeRepository {

    /**
     * 获取首页分类列表。
     *
     * @return 已按排序规则整理后的首页分类集合。
     */
    override suspend fun getCategories(): List<DiscoverCategory> = withContext(Dispatchers.IO) {
        apiService.getHomeCategories().requireData()
            .sortedBy(RemoteHomeCategoryDto::sortOrder)
            .map { it.toDomain() }
    }

    /**
     * 获取首页分类下的首屏内容。
     *
     * @param categoryId 分类标识。
     * @return 默认分页大小下的内容列表。
     */
    override suspend fun getDiscoverItems(categoryId: String): List<HomeDiscoverItem> {
        return getDiscoverItemsPage(categoryId = categoryId, offset = 0, limit = DEFAULT_PAGE_SIZE)
    }

    /**
     * 分页获取首页分类内容。
     *
     * @param categoryId 分类标识。
     * @param offset 偏移量。
     * @param limit 单次请求数量。
     * @return 当前分页范围内的内容列表。
     */
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

    /**
     * 获取关注页推荐用户列表。
     *
     * @return 过滤掉已关注用户后的推荐用户集合。
     */
    override suspend fun getSuggestedFollowingUsers(): List<FollowingUser> = withContext(Dispatchers.IO) {
        apiService.getFollowingSeed(offset = 0, limit = DEFAULT_FOLLOWING_PAGE_SIZE)
            .requireData()
            .suggestedUsers
            .filterNot(RemoteFollowingUserDto::followed)
            .map { it.toDomain() }
    }

    /**
     * 获取关注流首屏内容。
     *
     * @return 默认分页大小下的关注流内容列表。
     */
    override suspend fun getFollowingFeedItems(): List<HomeDiscoverItem> {
        return getFollowingFeedItemsPage(offset = 0, limit = DEFAULT_FOLLOWING_PAGE_SIZE)
    }

    /**
     * 分页获取关注流内容。
     *
     * @param offset 偏移量。
     * @param limit 单次请求数量。
     * @return 当前分页范围内的关注流内容列表。
     */
    override suspend fun getFollowingFeedItemsPage(offset: Int, limit: Int): List<HomeDiscoverItem> = withContext(Dispatchers.IO) {
        apiService.getFollowingSeed(offset = offset, limit = limit)
            .requireData()
            .followingFeedItems
            .items
            .map { it.toDomain() }
    }

    /**
     * 提交关注用户操作。
     *
     * @param userId 目标用户 ID。
     * @return 无返回值，请求成功即视为关注完成。
     */
    override suspend fun followUser(userId: String) = withContext(Dispatchers.IO) {
        apiService.followUser(userId).requireData()
        Unit
    }

    /**
     * 将远端首页分类 DTO 转换为领域模型。
     *
     * @return 首页分类领域模型。
     */
    private fun RemoteHomeCategoryDto.toDomain(): DiscoverCategory {
        return DiscoverCategory(
            id = id,
            title = title,
            bucket = bucket,
            usesWaterfall = usesWaterfall,
            isDefaultSelected = defaultSelected
        )
    }

    /**
     * 将远端推荐关注用户 DTO 转换为领域模型。
     *
     * @return 推荐关注用户领域模型。
     */
    private fun RemoteFollowingUserDto.toDomain(): FollowingUser {
        return FollowingUser(
            id = id,
            name = name,
            subtitle = subtitle,
            avatarColorHex = avatarColorHex,
            badge = badge
        )
    }

    /**
     * 将远端首页内容 DTO 转换为领域模型。
     *
     * 转换过程中会处理媒体封面兜底、点赞文案兜底以及默认渐变色配置，
     * 保证界面层拿到的数据结构始终可渲染。
     *
     * @return 首页内容领域模型。
     */
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

    /**
     * 将远端媒体类型字符串转换为本地枚举。
     *
     * @return 解析后的媒体类型；未知值统一按图片处理。
     */
    private fun String.toMediaType(): MediaType {
        return when (uppercase()) {
            MediaType.VIDEO.name -> MediaType.VIDEO
            MediaType.TEXT.name -> MediaType.TEXT
            MediaType.LONG_FORM.name -> MediaType.LONG_FORM
            else -> MediaType.IMAGE
        }
    }

    private companion object {
        /** 首页默认分页大小。 */
        const val DEFAULT_PAGE_SIZE = 10

        /** 关注页默认分页大小。 */
        const val DEFAULT_FOLLOWING_PAGE_SIZE = 8

        /** 缺失封面高度时使用的默认值。 */
        const val DEFAULT_COVER_HEIGHT_DP = 220

        /** 缺失渐变起始色时使用的默认值。 */
        const val DEFAULT_START_COLOR_HEX = "#D8082B"

        /** 缺失渐变结束色时使用的默认值。 */
        const val DEFAULT_END_COLOR_HEX = "#6D000F"

        /** 缺失头像背景色时使用的默认值。 */
        const val DEFAULT_AVATAR_COLOR_HEX = "#FF8A9F"
    }
}
