/**
 * 文件说明：MessageRepositoryImpl.kt
 * 作用：实现消息场景仓储接口，负责消息页远端数据获取与短时缓存。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.R
import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.RemoteResourceMapper
import com.zhengyang.redbook.data.remote.requireData
import com.zhengyang.redbook.data.remote.model.RemoteMessageOverviewDto
import com.zhengyang.redbook.data.remote.model.RemoteMessageRowDto
import com.zhengyang.redbook.data.remote.model.RemotePersonSuggestionDto
import com.zhengyang.redbook.ui.message.MessageRowItem
import com.zhengyang.redbook.ui.message.PersonSuggestionItem
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 消息页仓储实现类
 *
 * 负责加载消息总览接口，并将其中的入口列表与推荐用户列表映射为页面模型。
 * 为减少短时间内重复请求，仓储内部维护了一个基于时间窗口的内存缓存。
 */
class MessageRepositoryImpl @Inject constructor(
    /** 消息相关远端接口服务。 */
    private val apiService: RedBookApiService,

    /** 远端资源键到本地资源 ID 的映射器。 */
    private val resourceMapper: RemoteResourceMapper
) : MessageRepository {

    /** 保护总览缓存并发读写的互斥锁。 */
    private val overviewMutex = Mutex()

    /** 最近一次加载成功的消息总览缓存。 */
    private var cachedOverview: RemoteMessageOverviewDto? = null

    /** 最近一次缓存写入时间戳。 */
    private var cachedAt: Long = 0L

    /**
     * 获取消息页入口列表。
     *
     * @return 消息页入口展示模型列表。
     */
    override suspend fun getMessageRows(): List<MessageRowItem> = withContext(Dispatchers.IO) {
        loadOverview().messageRows.map { it.toUiModel() }
    }

    /**
     * 获取可能认识的人推荐列表。
     *
     * @return 推荐用户展示模型列表。
     */
    override suspend fun getPeopleSuggestions(): List<PersonSuggestionItem> = withContext(Dispatchers.IO) {
        loadOverview().peopleSuggestions.map { it.toUiModel() }
    }

    /**
     * 加载消息总览数据。
     *
     * 优先命中短时缓存，缓存失效后再发起远端请求，并通过互斥锁避免并发重复加载。
     *
     * @return 消息总览远端 DTO。
     */
    private suspend fun loadOverview(): RemoteMessageOverviewDto {
        val now = System.currentTimeMillis()
        cachedOverview?.takeIf { now - cachedAt < CACHE_TTL_MS }?.let { return it }
        return overviewMutex.withLock {
            val current = System.currentTimeMillis()
            cachedOverview?.takeIf { current - cachedAt < CACHE_TTL_MS }?.let { return@withLock it }
            apiService.getMessageOverview().requireData().also {
                cachedOverview = it
                cachedAt = current
            }
        }
    }

    /**
     * 将远端消息入口 DTO 转换为页面模型。
     *
     * @return 消息入口展示模型。
     */
    private fun RemoteMessageRowDto.toUiModel(): MessageRowItem {
        return MessageRowItem(
            id = id,
            title = title,
            subtitle = subtitle,
            timeText = timeText,
            backgroundRes = resourceMapper.drawableByName(
                name = backgroundKey,
                fallback = resourceMapper.defaultMessageAvatarBackground()
            ),
            iconRes = iconKey?.let {
                resourceMapper.drawableByName(it, resourceMapper.defaultMessageIcon())
            },
            avatarText = avatarText?.takeIf { it.isNotBlank() },
            iconSizeDp = iconSizeDp,
            showsVerifiedBadge = showsVerifiedBadge,
            showsRedDot = showsRedDot
        )
    }

    /**
     * 将远端推荐用户 DTO 转换为页面模型。
     *
     * @return 推荐用户展示模型。
     */
    private fun RemotePersonSuggestionDto.toUiModel(): PersonSuggestionItem {
        return PersonSuggestionItem(
            id = id,
            avatarText = avatarText,
            name = name,
            subtitle = subtitle,
            avatarBackgroundRes = resourceMapper.drawableByName(
                name = avatarBackgroundKey,
                fallback = resourceMapper.defaultSuggestionAvatarBackground()
            )
        )
    }

    private companion object {
        /** 消息总览缓存有效期，单位为毫秒。 */
        const val CACHE_TTL_MS = 1_000L
    }
}
