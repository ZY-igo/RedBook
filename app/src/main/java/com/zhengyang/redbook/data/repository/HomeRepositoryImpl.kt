/**
 * 文件说明：HomeRepositoryImpl.kt
 * 作用：首页数据仓库的实现类，负责协调网络请求和本地缓存。
 * 备注：实现缓存优先、多级降级的策略，提升数据加载速度和离线可用性。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.local.ListDao
import com.zhengyang.redbook.data.model.DiscoverCategory
import com.zhengyang.redbook.data.model.DiscoverCategoryEntity
import com.zhengyang.redbook.data.model.FollowingUser
import com.zhengyang.redbook.data.model.FollowingUserEntity
import com.zhengyang.redbook.data.model.HomeCardEntity
import com.zhengyang.redbook.data.model.HomeDiscoverItem
import com.zhengyang.redbook.data.model.MediaType
import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.model.RemoteFeedItemDto
import com.zhengyang.redbook.data.remote.model.RemoteFollowingSeedDto
import com.zhengyang.redbook.data.remote.model.RemoteFollowingUserDto
import com.zhengyang.redbook.data.remote.model.RemoteHomeCategoryDto
import com.zhengyang.redbook.data.remote.requireData
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 首页数据仓库实现类。
 *
 * 负责整合网络数据源（RedBookApiService）和本地缓存（ListDao），
 * 实现缓存优先、多级降级的数据加载策略。
 *
 * 数据加载策略：
 * 1. 若非强制刷新，优先查询本地缓存。
 * 2. 本地有缓存则直接返回，提升响应速度。
 * 3. 本地无缓存或强制刷新时，请求网络数据。
 * 4. 网络请求失败时，降级到本地缓存（若有）。
 * 5. 成功获取网络数据后，更新本地缓存。
 *
 * @property listDao 本地数据库访问对象，用于缓存数据。
 * @property apiService 远端 API 服务对象，用于获取网络数据。
 */
class HomeRepositoryImpl @Inject constructor(
    private val listDao: ListDao,
    private val apiService: RedBookApiService
) : HomeRepository {

    /** 关注种子数据缓存的互斥锁，用于并发控制。 */
    private val followingSeedMutex = Mutex()

    /** 关注种子数据的内存缓存，包含分页参数和结果。 */
    private var followingSeedCache: FollowingSeedCache? = null

    /**
     * 获取首页分类列表。
     *
     * 优先返回本地缓存，网络请求成功后将数据写入本地缓存。
     *
     * @param forceRefresh 是否强制从网络刷新。
     * @return 分类列表，按 sortOrder 排序。
     */
    override suspend fun getCategories(forceRefresh: Boolean): List<DiscoverCategory> = withContext(Dispatchers.IO) {
        // 非强制刷新时，先检查本地缓存
        if (!forceRefresh) {
            val cached = listDao.getDiscoverCategories()
            // 本地缓存有数据则直接返回
            if (cached.isNotEmpty()) return@withContext cached.map { it.toDomain() }
        }

        // 尝试从网络获取数据
        val remote = runCatching {
            apiService.getHomeCategories().requireData().sortedBy(RemoteHomeCategoryDto::sortOrder)
        }.getOrElse { error ->
            // 网络请求失败，降级到本地缓存
            val cached = listDao.getDiscoverCategories()
            if (cached.isNotEmpty()) return@withContext cached.map { it.toDomain() }
            // 本地也无缓存，则抛出异常
            throw error
        }

        // 网络请求成功，更新本地缓存并返回
        listDao.replaceDiscoverCategories(remote.map { it.toEntity() })
        remote.map { it.toDomain() }
    }

    /**
     * 获取指定分类下的首页内容流。
     *
     * 内部委托给分页方法实现，使用默认分页大小。
     *
     * @param categoryId 分类 ID。
     * @param forceRefresh 是否强制刷新。
     * @return 内容流列表。
     */
    override suspend fun getDiscoverItems(
        categoryId: String,
        forceRefresh: Boolean
    ): List<HomeDiscoverItem> {
        // 委托给分页方法，使用默认分页大小
        return getDiscoverItemsPage(categoryId, 0, DEFAULT_PAGE_SIZE, forceRefresh)
    }

    /**
     * 分页获取指定分类下的首页内容流。
     *
     * 实现了完整的多级缓存策略：
     * 1. 检查本地缓存
     * 2. 请求网络数据
     * 3. 网络失败降级到本地缓存
     * 4. 成功后将数据写入本地缓存
     *
     * @param categoryId 分类 ID，用于标识内容所属频道。
     * @param offset 分页偏移量。
     * @param limit 每页数量。
     * @param forceRefresh 是否强制刷新。
     * @return 分页后的内容流列表。
     */
    override suspend fun getDiscoverItemsPage(
        categoryId: String,
        offset: Int,
        limit: Int,
        forceRefresh: Boolean
    ): List<HomeDiscoverItem> = withContext(Dispatchers.IO) {
        // 优先检查本地缓存
        if (!forceRefresh) {
            val cached = listDao.getPagedHomeCardsBySection(categoryId, limit, offset)
            if (cached.isNotEmpty()) return@withContext cached.map { it.toDomain() }
        }

        // 尝试从网络获取
        val remote = runCatching {
            apiService.getHomeFeed(categoryId = categoryId, offset = offset, limit = limit)
                .requireData()
                .items
        }.getOrElse { error ->
            // 网络失败，降级到本地缓存
            val cached = listDao.getPagedHomeCardsBySection(categoryId, limit, offset)
            if (cached.isNotEmpty()) return@withContext cached.map { it.toDomain() }
            throw error
        }

        // 转换为实体并写入本地缓存
        // offset=0 时替换整页，offset>0 时追加新数据
        val entities = remote.mapIndexed { index, item ->
            item.toEntity(sectionKey = categoryId, sortOrder = offset + index)
        }
        if (offset == 0) {
            // 第一页数据，替换现有缓存
            listDao.replaceHomeCardsBySection(categoryId, entities)
        } else {
            // 非第一页，追加到现有缓存
            listDao.insertHomeCards(entities)
        }
        remote.map { it.toDomain() }
    }

    /**
     * 获取推荐关注的用户列表。
     *
     * 用于首页"关注"频道顶部的推荐用户区域。
     * 会过滤掉已经关注的用户（followed=true）。
     *
     * @param forceRefresh 是否强制刷新。
     * @return 推荐关注的用户列表。
     */
    override suspend fun getSuggestedFollowingUsers(forceRefresh: Boolean): List<FollowingUser> = withContext(Dispatchers.IO) {
        // 优先检查本地缓存
        if (!forceRefresh) {
            val cached = listDao.getFollowingUsers()
            if (cached.isNotEmpty()) return@withContext cached.map { it.toDomain() }
        }

        // 尝试从网络获取（内部会处理缓存逻辑）
        val remote = runCatching {
            loadFollowingSeedRemote(offset = 0, limit = DEFAULT_FOLLOWING_PAGE_SIZE, forceRefresh = forceRefresh)
        }.getOrElse { error ->
            // 网络失败，降级到本地缓存
            val cached = listDao.getFollowingUsers()
            if (cached.isNotEmpty()) return@withContext cached.map { it.toDomain() }
            throw error
        }

        // 过滤掉已关注的用户，只返回未关注的推荐用户
        remote.suggestedUsers.filterNot(RemoteFollowingUserDto::followed).map { it.toDomain() }
    }

    /**
     * 获取关注页的信息流。
     *
     * 返回用户关注对象的最新动态。
     * 内部委托给分页方法实现。
     *
     * @param forceRefresh 是否强制刷新。
     * @return 关注动态列表。
     */
    override suspend fun getFollowingFeedItems(forceRefresh: Boolean): List<HomeDiscoverItem> {
        return getFollowingFeedItemsPage(0, DEFAULT_FOLLOWING_PAGE_SIZE, forceRefresh)
    }

    /**
     * 分页获取关注页的信息流。
     *
     * @param offset 分页偏移量。
     * @param limit 每页数量。
     * @param forceRefresh 是否强制刷新。
     * @return 分页后的关注动态列表。
     */
    override suspend fun getFollowingFeedItemsPage(
        offset: Int,
        limit: Int,
        forceRefresh: Boolean
    ): List<HomeDiscoverItem> = withContext(Dispatchers.IO) {
        // 优先检查本地缓存
        if (!forceRefresh) {
            val cached = listDao.getPagedHomeCardsBySection(FOLLOWING_SECTION_KEY, limit, offset)
            if (cached.isNotEmpty()) return@withContext cached.map { it.toDomain() }
        }

        // 尝试从网络获取
        val remote = runCatching {
            loadFollowingSeedRemote(offset = offset, limit = limit, forceRefresh = forceRefresh)
        }.getOrElse { error ->
            // 网络失败，降级到本地缓存
            val cached = listDao.getPagedHomeCardsBySection(FOLLOWING_SECTION_KEY, limit, offset)
            if (cached.isNotEmpty()) return@withContext cached.map { it.toDomain() }
            throw error
        }

        // 提取关注 feed 项并转换为领域模型
        remote.followingFeedItems.items.map { it.toDomain() }
    }

    /**
     * 关注指定用户。
     *
     * @param userId 要关注的用户 ID。
     */
    override suspend fun followUser(userId: String) = withContext(Dispatchers.IO) {
        // 调用网络接口关注用户
        apiService.followUser(userId).requireData()
        // 无返回值
        Unit
    }

    /**
     * 加载关注种子数据的远程数据。
     *
     * 关注种子数据包含推荐用户列表和关注 feed 首屏数据，
     * 使用内存缓存减少重复请求。
     *
     * @param offset 分页偏移量。
     * @param limit 每页数量。
     * @param forceRefresh 是否强制刷新。
     * @return 关注种子数据。
     */
    private suspend fun loadFollowingSeedRemote(
        offset: Int,
        limit: Int,
        forceRefresh: Boolean
    ): RemoteFollowingSeedDto {
        // 检查内存缓存是否命中
        val cached = followingSeedCache
        if (!forceRefresh && cached != null && cached.offset == offset && cached.limit == limit) {
            // 缓存命中，直接返回
            return cached.seed
        }

        // 使用互斥锁进行并发控制，避免多次请求
        return followingSeedMutex.withLock {
            // 双重检查：获取锁后再次验证缓存
            val current = followingSeedCache
            if (!forceRefresh && current != null && current.offset == offset && current.limit == limit) {
                return@withLock current.seed
            }

            // 缓存未命中或需要刷新，请求网络数据
            val remote = apiService.getFollowingSeed(offset = offset, limit = limit).requireData()

            // 更新本地数据库缓存
            if (offset == 0) {
                // 首屏数据：替换所有推荐用户和关注 feed
                listDao.replaceFollowingUsers(
                    remote.suggestedUsers
                        .filterNot(RemoteFollowingUserDto::followed)
                        .mapIndexed { index, item -> item.toEntity(index) }
                )
                listDao.replaceHomeCardsBySection(
                    FOLLOWING_SECTION_KEY,
                    remote.followingFeedItems.items.mapIndexed { index, item ->
                        item.toEntity(sectionKey = FOLLOWING_SECTION_KEY, sortOrder = index)
                    }
                )
            } else {
                // 非首屏：追加关注 feed 数据
                listDao.insertHomeCards(
                    remote.followingFeedItems.items.mapIndexed { index, item ->
                        item.toEntity(sectionKey = FOLLOWING_SECTION_KEY, sortOrder = offset + index)
                    }
                )
            }

            // 更新内存缓存
            followingSeedCache = FollowingSeedCache(offset, limit, remote)
            remote
        }
    }

    // ==================== DTO -> Entity 转换方法 ====================

    /**
     * 将远程分类 DTO 转换为数据库实体。
     *
     * @return 分类实体对象。
     */
    private fun RemoteHomeCategoryDto.toEntity(): DiscoverCategoryEntity {
        return DiscoverCategoryEntity(
            id = id,
            title = title,
            bucket = bucket,
            usesWaterfall = usesWaterfall,
            sortOrder = sortOrder,
            isDefaultSelected = defaultSelected
        )
    }

    /**
     * 将数据库实体转换为领域模型。
     *
     * @return 分类领域对象。
     */
    private fun DiscoverCategoryEntity.toDomain(): DiscoverCategory {
        return DiscoverCategory(
            id = id,
            title = title,
            bucket = bucket,
            usesWaterfall = usesWaterfall,
            isDefaultSelected = isDefaultSelected
        )
    }

    /**
     * 将远程分类 DTO 直接转换为领域模型。
     *
     * @return 分类领域对象。
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
     * 将远程用户 DTO 转换为数据库实体。
     *
     * @param sortOrder 排序顺序。
     * @return 用户实体对象。
     */
    private fun RemoteFollowingUserDto.toEntity(sortOrder: Int): FollowingUserEntity {
        return FollowingUserEntity(
            id = id,
            name = name,
            subtitle = subtitle,
            avatarUrl = avatarUrl,
            avatarColorHex = avatarColorHex,
            badge = badge,
            sortOrder = sortOrder
        )
    }

    /**
     * 将数据库实体转换为领域模型。
     *
     * @return 用户领域对象。
     */
    private fun FollowingUserEntity.toDomain(): FollowingUser {
        return FollowingUser(
            id = id,
            name = name,
            subtitle = subtitle,
            avatarUrl = avatarUrl,
            avatarColorHex = avatarColorHex,
            badge = badge
        )
    }

    /**
     * 将远程用户 DTO 直接转换为领域模型。
     *
     * @return 用户领域对象。
     */
    private fun RemoteFollowingUserDto.toDomain(): FollowingUser {
        return FollowingUser(
            id = id,
            name = name,
            subtitle = subtitle,
            avatarUrl = avatarUrl,
            avatarColorHex = avatarColorHex,
            badge = badge
        )
    }

    /**
     * 将远程 Feed 项 DTO 转换为数据库实体。
     *
     * @param sectionKey 分区键，标识内容所属分类。
     * @param sortOrder 排序顺序。
     * @return Feed 项实体对象。
     */
    private fun RemoteFeedItemDto.toEntity(sectionKey: String, sortOrder: Int): HomeCardEntity {
        // 解析图片 URL，优先使用 imageUrl，其次 videoCoverUrl，最后 coverUrl
        val resolvedImageUrl = imageUrl ?: videoCoverUrl ?: coverUrl
        return HomeCardEntity(
            id = id,
            sectionKey = sectionKey,
            title = title,
            author = author,
            likeCount = likeCount.orEmpty().ifBlank { "0" },
            badge = badge.orEmpty(),
            coverLabel = coverLabel.orEmpty(),
            coverHeightDp = coverHeightDp ?: DEFAULT_COVER_HEIGHT_DP,
            mediaType = mediaType.toMediaType().name,
            imageUrl = resolvedImageUrl,
            videoUrl = videoUrl,
            videoCoverUrl = videoCoverUrl ?: coverUrl ?: imageUrl,
            startColorHex = startColorHex ?: DEFAULT_START_COLOR_HEX,
            endColorHex = endColorHex ?: DEFAULT_END_COLOR_HEX,
            avatarColorHex = avatarColorHex ?: DEFAULT_AVATAR_COLOR_HEX,
            sortOrder = sortOrder
        )
    }

    /**
     * 将数据库实体转换为领域模型。
     *
     * @return Feed 项领域对象。
     */
    private fun HomeCardEntity.toDomain(): HomeDiscoverItem {
        return HomeDiscoverItem(
            id = id,
            title = title,
            author = author,
            avatarUrl = null,
            likeCount = likeCount,
            badge = badge,
            coverLabel = coverLabel,
            coverHeightDp = coverHeightDp,
            mediaType = mediaType.toMediaType(),
            imageUrls = listOfNotNull(imageUrl),
            imageUrl = imageUrl,
            videoUrl = videoUrl,
            videoCoverUrl = videoCoverUrl,
            startColorHex = startColorHex,
            endColorHex = endColorHex,
            avatarColorHex = avatarColorHex
        )
    }

    /**
     * 将远程 Feed 项 DTO 直接转换为领域模型。
     *
     * @return Feed 项领域对象。
     */
    private fun RemoteFeedItemDto.toDomain(): HomeDiscoverItem {
        // 解析图片 URL，优先级：imageUrl > videoCoverUrl > coverUrl
        val resolvedImageUrl = imageUrl ?: videoCoverUrl ?: coverUrl
        return HomeDiscoverItem(
            id = id,
            title = title,
            author = author,
            avatarUrl = avatarUrl,
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
     * 将媒体类型字符串转换为枚举值。
     *
     * @return MediaType 枚举值。
     */
    private fun String.toMediaType(): MediaType {
        return when (uppercase()) {
            MediaType.VIDEO.name -> MediaType.VIDEO
            MediaType.TEXT.name -> MediaType.TEXT
            MediaType.LONG_FORM.name -> MediaType.LONG_FORM
            else -> MediaType.IMAGE
        }
    }

    /**
     * 关注种子数据缓存数据类。
     *
     * 用于内存缓存关注种子数据，包含分页参数和实际数据。
     *
     * @property offset 分页偏移量。
     * @property limit 每页数量。
     * @property seed 关注种子数据。
     */
    private data class FollowingSeedCache(
        val offset: Int,
        val limit: Int,
        val seed: RemoteFollowingSeedDto
    )

    /**
     * 常量伴生对象。
     */
    private companion object {
        /** 默认分页大小。 */
        const val DEFAULT_PAGE_SIZE = 10

        /** 关注页默认分页大小。 */
        const val DEFAULT_FOLLOWING_PAGE_SIZE = 8

        /** 关注分区键，用于标识关注 feed 数据。 */
        const val FOLLOWING_SECTION_KEY = "following"

        /** 默认封面高度（dp）。 */
        const val DEFAULT_COVER_HEIGHT_DP = 220

        /** 默认渐变起始颜色（红色系）。 */
        const val DEFAULT_START_COLOR_HEX = "#D8082B"

        /** 默认渐变结束颜色（深红色）。 */
        const val DEFAULT_END_COLOR_HEX = "#6D000F"

        /** 默认头像背景颜色（粉红色）。 */
        const val DEFAULT_AVATAR_COLOR_HEX = "#FF8A9F"
    }
}