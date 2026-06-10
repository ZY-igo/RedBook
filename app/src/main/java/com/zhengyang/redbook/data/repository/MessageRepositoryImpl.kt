/**
 * 文件说明：MessageRepositoryImpl.kt
 * 作用：消息模块数据仓库的实现类，负责协调网络请求和本地缓存。
 * 备注：实现缓存优先、多级降级的策略，并维护消息概览数据的内存缓存。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.local.ListDao
import com.zhengyang.redbook.data.model.MessageRowEntity
import com.zhengyang.redbook.data.model.PersonSuggestionEntity
import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.RemoteResourceMapper
import com.zhengyang.redbook.data.remote.model.RemoteMessageOverviewDto
import com.zhengyang.redbook.data.remote.model.RemoteMessageRowDto
import com.zhengyang.redbook.data.remote.model.RemotePersonSuggestionDto
import com.zhengyang.redbook.data.remote.requireData
import com.zhengyang.redbook.ui.message.MessageRowItem
import com.zhengyang.redbook.ui.message.PersonSuggestionItem
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 消息模块数据仓库实现类。
 *
 * 负责整合网络数据源（RedBookApiService）和本地缓存（ListDao），
 * 并维护消息概览数据的内存缓存以提升访问速度。
 *
 * 数据加载策略：
 * 1. 若非强制刷新，优先查询本地缓存。
 * 2. 本地有缓存则直接返回，提升响应速度。
 * 3. 本地无缓存或强制刷新时，请求网络数据。
 * 4. 网络请求失败时，降级到本地缓存（若有）。
 * 5. 成功获取网络数据后，更新本地缓存。
 *
 * 特殊设计：
 * - 消息概览数据（包含消息列表和推荐用户）通过单一 API 获取，
 *   因此使用共享的内存缓存来避免重复请求。
 *
 * @property listDao 本地数据库访问对象。
 * @property apiService 远端 API 服务对象。
 * @property resourceMapper 远端资源映射器，用于解析服务端下发的资源名称。
 */
class MessageRepositoryImpl @Inject constructor(
    private val listDao: ListDao,
    private val apiService: RedBookApiService,
    private val resourceMapper: RemoteResourceMapper
) : MessageRepository {

    /** 消息概览缓存的互斥锁，用于并发控制。 */
    private val overviewMutex = Mutex()

    /** 消息概览数据的内存缓存。 */
    private var cachedOverview: RemoteMessageOverviewDto? = null

    /**
     * 获取消息列表。
     *
     * 优先返回本地缓存，网络请求成功后将数据写入本地缓存。
     *
     * @param forceRefresh 是否强制从网络刷新。
     * @return 消息列表。
     */
    override suspend fun getMessageRows(forceRefresh: Boolean): List<MessageRowItem> = withContext(Dispatchers.IO) {
        // 优先检查本地缓存
        if (!forceRefresh) {
            val cached = listDao.getMessageRows()
            if (cached.isNotEmpty()) return@withContext cached.map { it.toUiModel() }
        }

        // 尝试从网络获取（使用共享的概览数据加载方法）
        val remote = runCatching { loadRemoteOverview(forceRefresh) }.getOrElse { error ->
            // 网络失败，降级到本地缓存
            val cached = listDao.getMessageRows()
            if (cached.isNotEmpty()) return@withContext cached.map { it.toUiModel() }
            throw error
        }

        // 提取消息行数据并转换为 UI 模型
        remote.messageRows.map { it.toUiModel() }
    }

    /**
     * 获取推荐用户列表。
     *
     * 优先返回本地缓存，网络请求成功后将数据写入本地缓存。
     *
     * @param forceRefresh 是否强制从网络刷新。
     * @return 推荐用户列表。
     */
    override suspend fun getPeopleSuggestions(forceRefresh: Boolean): List<PersonSuggestionItem> = withContext(Dispatchers.IO) {
        // 优先检查本地缓存
        if (!forceRefresh) {
            val cached = listDao.getPersonSuggestions()
            if (cached.isNotEmpty()) return@withContext cached.map { it.toUiModel() }
        }

        // 尝试从网络获取（使用共享的概览数据加载方法）
        val remote = runCatching { loadRemoteOverview(forceRefresh) }.getOrElse { error ->
            // 网络失败，降级到本地缓存
            val cached = listDao.getPersonSuggestions()
            if (cached.isNotEmpty()) return@withContext cached.map { it.toUiModel() }
            throw error
        }

        // 提取推荐用户数据并转换为 UI 模型
        remote.peopleSuggestions.map { it.toUiModel() }
    }

    /**
     * 加载消息概览数据的远程数据。
     *
     * 消息概览 API 同时返回消息列表和推荐用户列表，
     * 因此使用共享的内存缓存来减少重复请求。
     *
     * @param forceRefresh 是否强制从网络刷新。
     * @return 消息概览数据。
     */
    private suspend fun loadRemoteOverview(forceRefresh: Boolean): RemoteMessageOverviewDto {
        // 非强制刷新时，先检查内存缓存
        if (!forceRefresh) {
            cachedOverview?.let { return it }
        }

        // 使用互斥锁进行并发控制
        return overviewMutex.withLock {
            // 获取锁后再次检查缓存（双重检查锁定模式）
            if (!forceRefresh) {
                cachedOverview?.let { return@withLock it }
            }

            // 缓存未命中或需要刷新，请求网络数据
            val remote = apiService.getMessageOverview().requireData()

            // 更新本地数据库缓存
            // 将消息行数据批量写入数据库
            listDao.replaceMessageRows(remote.messageRows.mapIndexed { index, item -> item.toEntity(index) })
            // 将推荐用户数据批量写入数据库
            listDao.replacePersonSuggestions(remote.peopleSuggestions.mapIndexed { index, item -> item.toEntity(index) })

            // 更新内存缓存
            cachedOverview = remote
            remote
        }
    }

    // ==================== DTO -> Entity 转换方法 ====================

    /**
     * 将远程消息行 DTO 转换为数据库实体。
     *
     * @param sortOrder 排序顺序。
     * @return 消息行实体对象。
     */
    private fun RemoteMessageRowDto.toEntity(sortOrder: Int): MessageRowEntity {
        return MessageRowEntity(
            id = id,
            title = title,
            subtitle = subtitle,
            timeText = timeText,
            // 使用资源映射器解析背景资源 ID
            backgroundRes = resourceMapper.drawableByName(
                name = backgroundKey,
                fallback = resourceMapper.defaultMessageAvatarBackground()
            ),
            // 解析图标资源 ID
            iconRes = iconKey?.let {
                resourceMapper.drawableByName(it, resourceMapper.defaultMessageIcon())
            },
            avatarText = avatarText?.takeIf(String::isNotBlank),
            iconSizeDp = iconSizeDp,
            showsVerifiedBadge = showsVerifiedBadge,
            showsRedDot = showsRedDot,
            sortOrder = sortOrder
        )
    }

    /**
     * 将数据库实体转换为 UI 模型。
     *
     * @return 消息行 UI 模型。
     */
    private fun MessageRowEntity.toUiModel(): MessageRowItem {
        return MessageRowItem(
            id = id,
            title = title,
            subtitle = subtitle,
            timeText = timeText,
            backgroundRes = backgroundRes,
            iconRes = iconRes,
            avatarText = avatarText,
            iconSizeDp = iconSizeDp,
            showsVerifiedBadge = showsVerifiedBadge,
            showsRedDot = showsRedDot
        )
    }

    /**
     * 将远程消息行 DTO 直接转换为 UI 模型。
     *
     * @return 消息行 UI 模型。
     */
    private fun RemoteMessageRowDto.toUiModel(): MessageRowItem {
        return MessageRowItem(
            id = id,
            title = title,
            subtitle = subtitle,
            timeText = timeText,
            // 使用资源映射器解析背景资源 ID
            backgroundRes = resourceMapper.drawableByName(
                name = backgroundKey,
                fallback = resourceMapper.defaultMessageAvatarBackground()
            ),
            // 解析图标资源 ID
            iconRes = iconKey?.let {
                resourceMapper.drawableByName(it, resourceMapper.defaultMessageIcon())
            },
            avatarText = avatarText?.takeIf(String::isNotBlank),
            iconSizeDp = iconSizeDp,
            showsVerifiedBadge = showsVerifiedBadge,
            showsRedDot = showsRedDot
        )
    }

    /**
     * 将远程推荐用户 DTO 转换为数据库实体。
     *
     * @param sortOrder 排序顺序。
     * @return 推荐用户实体对象。
     */
    private fun RemotePersonSuggestionDto.toEntity(sortOrder: Int): PersonSuggestionEntity {
        return PersonSuggestionEntity(
            id = id,
            avatarText = avatarText,
            name = name,
            subtitle = subtitle,
            // 使用资源映射器解析头像背景资源 ID
            avatarBackgroundRes = resourceMapper.drawableByName(
                name = avatarBackgroundKey,
                fallback = resourceMapper.defaultSuggestionAvatarBackground()
            ),
            sortOrder = sortOrder
        )
    }

    /**
     * 将数据库实体转换为 UI 模型。
     *
     * @return 推荐用户 UI 模型。
     */
    private fun PersonSuggestionEntity.toUiModel(): PersonSuggestionItem {
        return PersonSuggestionItem(
            id = id,
            avatarText = avatarText,
            name = name,
            subtitle = subtitle,
            avatarBackgroundRes = avatarBackgroundRes
        )
    }

    /**
     * 将远程推荐用户 DTO 直接转换为 UI 模型。
     *
     * @return 推荐用户 UI 模型。
     */
    private fun RemotePersonSuggestionDto.toUiModel(): PersonSuggestionItem {
        return PersonSuggestionItem(
            id = id,
            avatarText = avatarText,
            name = name,
            subtitle = subtitle,
            // 使用资源映射器解析头像背景资源 ID
            avatarBackgroundRes = resourceMapper.drawableByName(
                name = avatarBackgroundKey,
                fallback = resourceMapper.defaultSuggestionAvatarBackground()
            )
        )
    }
}