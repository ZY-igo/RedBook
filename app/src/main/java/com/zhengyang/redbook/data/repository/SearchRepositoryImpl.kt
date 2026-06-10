/**
 * 文件说明：SearchRepositoryImpl.kt
 * 作用：搜索模块数据仓库的实现类，负责协调网络请求和本地缓存。
 * 备注：实现缓存优先、多级降级的策略，管理搜索历史和搜索结果缓存。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.R
import com.zhengyang.redbook.data.local.ListDao
import com.zhengyang.redbook.data.model.SearchGuessEntity
import com.zhengyang.redbook.data.model.SearchHistoryEntity
import com.zhengyang.redbook.data.model.SearchResultEntity
import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.RemoteResourceMapper
import com.zhengyang.redbook.data.remote.model.RemoteSearchGuessDto
import com.zhengyang.redbook.data.remote.model.RemoteSearchResultDto
import com.zhengyang.redbook.data.remote.requireData
import com.zhengyang.redbook.ui.search.ResultFilter
import com.zhengyang.redbook.ui.search.SearchGuessItem
import com.zhengyang.redbook.ui.search.SearchResultItem
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 搜索模块数据仓库实现类。
 *
 * 负责整合网络数据源（RedBookApiService）和本地缓存（ListDao），
 * 提供搜索相关的增删改查功能。
 *
 * 数据管理范围：
 * - 搜索历史记录：保存在本地数据库，支持增加和清空。
 * - 搜索推荐词：从网络获取并缓存到本地。
 * - 搜索结果：从网络获取，成功后缓存到本地。
 *
 * @property listDao 本地数据库访问对象。
 * @property apiService 远端 API 服务对象。
 * @property resourceMapper 远端资源映射器，用于解析服务端下发的资源名称。
 */
class SearchRepositoryImpl @Inject constructor(
    private val listDao: ListDao,
    private val apiService: RedBookApiService,
    private val resourceMapper: RemoteResourceMapper
) : SearchRepository {

    /**
     * 获取搜索历史记录。
     *
     * @param limit 返回的最大记录数。
     * @return 搜索历史关键词列表，按更新时间倒序排列。
     */
    override suspend fun getHistory(limit: Int): List<String> {
        // 从本地数据库查询历史记录
        return listDao.getSearchHistory(limit).map { it.query }
    }

    /**
     * 保存搜索历史记录。
     *
     * 如果关键词为空或仅包含空白字符，则不保存。
     * 保存成功后返回更新后的历史记录列表。
     *
     * @param query 搜索关键词。
     * @return 更新后的搜索历史列表。
     */
    override suspend fun saveHistory(query: String): List<String> {
        // 去除首尾空白字符
        val trimmed = query.trim()
        // 如果关键词为空，不保存直接返回现有历史
        if (trimmed.isEmpty()) return getHistory()

        // 创建历史记录实体并存入数据库
        listDao.insertSearchHistory(
            SearchHistoryEntity(
                query = trimmed,
                updatedAt = System.currentTimeMillis()
            )
        )
        // 返回更新后的历史记录
        return getHistory()
    }

    /**
     * 清空所有搜索历史记录。
     */
    override suspend fun clearHistory() {
        // 删除数据库中所有历史记录
        listDao.clearSearchHistory()
    }

    /**
     * 获取搜索推荐词列表。
     *
     * 优先从本地缓存获取，缓存为空时从网络获取并更新缓存。
     * 搜索推荐词通常展示在搜索页面顶部或搜索框下方。
     *
     * @return 搜索推荐词列表。
     */
    override suspend fun getGuessItems(): List<SearchGuessItem> = withContext(Dispatchers.IO) {
        // 优先检查本地缓存
        val cached = listDao.getSearchGuessItems()
        if (cached.isNotEmpty()) return@withContext cached.map { it.toUiModel() }

        // 缓存为空，从网络获取推荐词
        val remote = apiService.getSearchBootstrap().requireData().guessItems
        // 更新本地缓存
        listDao.replaceSearchGuessItems(remote.mapIndexed { index, item -> item.toEntity(index) })
        // 转换为 UI 模型并返回
        remote.map { it.toUiModel() }
    }

    /**
     * 获取搜索建议列表。
     *
     * 根据用户输入的查询词，获取实时的搜索建议。
     * 该接口每次都会请求网络，不使用本地缓存以保证实时性。
     *
     * @param query 用户输入的查询词。
     * @return 搜索建议列表。
     */
    override suspend fun getSuggestions(query: String): List<SearchGuessItem> = withContext(Dispatchers.IO) {
        // 去除首尾空白字符
        val trimmed = query.trim()
        // 如果关键词为空，返回空列表
        if (trimmed.isEmpty()) return@withContext emptyList()

        // 调用网络接口获取搜索建议
        apiService.getSearchSuggestions(trimmed).requireData().map { it.toUiModel() }
    }

    /**
     * 获取搜索结果。
     *
     * 优先从本地缓存获取，缓存为空时从网络获取。
     * 网络请求成功后，将结果缓存到本地数据库。
     *
     * @param query 搜索关键词。
     * @return 搜索结果列表。
     */
    override suspend fun getResults(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        // 去除首尾空白字符
        val trimmed = query.trim()
        // 如果关键词为空，返回空列表
        if (trimmed.isEmpty()) return@withContext emptyList()

        // 尝试从网络获取搜索结果
        val remote = runCatching { apiService.getSearchResults(trimmed).requireData() }.getOrElse { error ->
            // 网络失败，降级到本地缓存
            val cached = listDao.getSearchResults(trimmed)
            if (cached.isNotEmpty()) return@withContext cached.map { it.toUiModel() }
            throw error
        }

        // 成功获取网络数据，更新本地缓存
        listDao.replaceSearchResults(
            keyword = trimmed,
            items = remote.mapIndexed { index, item -> item.toEntity(trimmed, index) }
        )
        // 转换为 UI 模型并返回
        remote.map { it.toUiModel() }
    }

    // ==================== DTO -> Entity 转换方法 ====================

    /**
     * 将远程搜索推荐词 DTO 转换为数据库实体。
     *
     * @param sortOrder 排序顺序。
     * @return 搜索推荐词实体对象。
     */
    private fun RemoteSearchGuessDto.toEntity(sortOrder: Int): SearchGuessEntity {
        return SearchGuessEntity(
            id = id,
            title = title,
            meta = meta,
            sortOrder = sortOrder
        )
    }

    /**
     * 将数据库实体转换为 UI 模型。
     *
     * @return 搜索推荐词 UI 模型。
     */
    private fun SearchGuessEntity.toUiModel(): SearchGuessItem {
        return SearchGuessItem(
            title = title,
            meta = meta
        )
    }

    /**
     * 将远程搜索推荐词 DTO 直接转换为 UI 模型。
     *
     * @return 搜索推荐词 UI 模型。
     */
    private fun RemoteSearchGuessDto.toUiModel(): SearchGuessItem {
        return SearchGuessItem(
            title = title,
            meta = meta
        )
    }

    /**
     * 将远程搜索结果 DTO 转换为数据库实体。
     *
     * @param keyword 搜索关键词，用于关联搜索结果。
     * @param sortOrder 排序顺序。
     * @return 搜索结果实体对象。
     */
    private fun RemoteSearchResultDto.toEntity(keyword: String, sortOrder: Int): SearchResultEntity {
        return SearchResultEntity(
            id = id,
            keyword = keyword,
            matchTokens = keyword,
            filter = filter,
            title = title,
            subtitle = subtitle,
            meta = meta,
            badge = badge,
            badgeColorResName = badgeColorKey,
            sortOrder = sortOrder
        )
    }

    /**
     * 将数据库实体转换为 UI 模型。
     *
     * @return 搜索结果 UI 模型。
     */
    private fun SearchResultEntity.toUiModel(): SearchResultItem {
        return SearchResultItem(
            filter = ResultFilter.valueOf(filter),
            title = title,
            subtitle = subtitle,
            meta = meta,
            badge = badge,
            // 使用资源映射器解析徽章颜色资源 ID
            badgeColorRes = resourceMapper.colorByName(
                name = badgeColorResName,
                fallback = R.color.xhs_search_quick_action_icon_bg
            )
        )
    }

    /**
     * 将远程搜索结果 DTO 直接转换为 UI 模型。
     *
     * @return 搜索结果 UI 模型。
     */
    private fun RemoteSearchResultDto.toUiModel(): SearchResultItem {
        return SearchResultItem(
            filter = ResultFilter.valueOf(filter),
            title = title,
            subtitle = subtitle,
            meta = meta,
            badge = badge,
            // 使用资源映射器解析徽章颜色资源 ID
            badgeColorRes = resourceMapper.colorByName(
                name = badgeColorKey,
                fallback = R.color.xhs_search_quick_action_icon_bg
            )
        )
    }
}