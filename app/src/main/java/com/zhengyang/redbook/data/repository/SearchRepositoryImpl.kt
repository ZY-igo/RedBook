/**
 * 文件说明：SearchRepositoryImpl.kt
 * 作用：实现搜索场景仓储接口，协调本地历史记录与远端搜索数据读取。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.R
import com.zhengyang.redbook.data.local.ListDao
import com.zhengyang.redbook.data.model.SearchHistoryEntity
import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.RemoteResourceMapper
import com.zhengyang.redbook.data.remote.requireData
import com.zhengyang.redbook.data.remote.model.RemoteSearchGuessDto
import com.zhengyang.redbook.data.remote.model.RemoteSearchResultDto
import com.zhengyang.redbook.ui.search.ResultFilter
import com.zhengyang.redbook.ui.search.SearchGuessItem
import com.zhengyang.redbook.ui.search.SearchResultItem
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 搜索仓储实现类
 *
 * 负责在本地数据库中维护搜索历史，并从远端接口获取推荐词、联想词和结果列表。
 * 仓储层会统一处理关键词裁剪、空值兜底和远端 DTO 到 UI 模型的映射。
 */
class SearchRepositoryImpl @Inject constructor(
    /** 本地聚合 DAO，用于读写搜索历史。 */
    private val listDao: ListDao,

    /** 搜索远端接口服务。 */
    private val apiService: RedBookApiService,

    /** 远端资源键到本地资源 ID 的映射器。 */
    private val resourceMapper: RemoteResourceMapper
) : SearchRepository {

    /**
     * 获取最近使用的搜索历史。
     *
     * @param limit 最多返回的历史条数。
     * @return 按最近使用时间排序后的搜索词列表。
     */
    override suspend fun getHistory(limit: Int): List<String> {
        return listDao.getSearchHistory(limit).map { it.query }
    }

    /**
     * 保存搜索历史并返回最新列表。
     *
     * @param query 用户提交的原始搜索词。
     * @return 保存后的最新搜索历史列表；空查询不会写入数据库。
     */
    override suspend fun saveHistory(query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getHistory()
        listDao.insertSearchHistory(
            SearchHistoryEntity(
                query = trimmed,
                updatedAt = System.currentTimeMillis()
            )
        )
        return getHistory()
    }

    /**
     * 清空全部搜索历史。
     *
     * @return 无返回值，执行后本地搜索历史表会被清空。
     */
    override suspend fun clearHistory() {
        listDao.clearSearchHistory()
    }

    /**
     * 获取搜索页默认推荐词。
     *
     * @return 猜你想搜条目列表。
     */
    override suspend fun getGuessItems(): List<SearchGuessItem> = withContext(Dispatchers.IO) {
        apiService.getSearchBootstrap().requireData().guessItems.map { it.toUiModel() }
    }

    /**
     * 获取联想建议列表。
     *
     * @param query 当前输入内容。
     * @return 联想建议列表；空查询直接返回空集合。
     */
    override suspend fun getSuggestions(query: String): List<SearchGuessItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        apiService.getSearchSuggestions(trimmed).requireData().map { it.toUiModel() }
    }

    /**
     * 获取搜索结果列表。
     *
     * @param query 搜索关键词。
     * @return 搜索结果列表；空查询直接返回空集合。
     */
    override suspend fun getResults(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        apiService.getSearchResults(trimmed).requireData().map { it.toUiModel() }
    }

    /**
     * 将远端推荐词 DTO 转换为搜索展示模型。
     *
     * @return 搜索推荐条目模型。
     */
    private fun RemoteSearchGuessDto.toUiModel(): SearchGuessItem {
        return SearchGuessItem(
            title = title,
            meta = meta
        )
    }

    /**
     * 将远端搜索结果 DTO 转换为结果卡片模型。
     *
     * @return 搜索结果展示模型。
     */
    private fun RemoteSearchResultDto.toUiModel(): SearchResultItem {
        return SearchResultItem(
            filter = ResultFilter.valueOf(filter),
            title = title,
            subtitle = subtitle,
            meta = meta,
            badge = badge,
            badgeColorRes = resourceMapper.colorByName(
                name = badgeColorKey,
                fallback = R.color.xhs_search_quick_action_icon_bg
            )
        )
    }
}
