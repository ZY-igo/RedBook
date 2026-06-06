package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.R
import com.zhengyang.redbook.data.local.ListDao
import com.zhengyang.redbook.data.local.SearchSeedDataSource
import com.zhengyang.redbook.data.model.SearchGuessEntity
import com.zhengyang.redbook.data.model.SearchHistoryEntity
import com.zhengyang.redbook.data.model.SearchResultEntity
import com.zhengyang.redbook.ui.search.ResultFilter
import com.zhengyang.redbook.ui.search.SearchGuessItem
import com.zhengyang.redbook.ui.search.SearchResultItem
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val listDao: ListDao,
    private val searchSeedDataSource: SearchSeedDataSource
) : SearchRepository {

    override suspend fun getHistory(limit: Int): List<String> {
        return listDao.getSearchHistory(limit).map { it.query }
    }

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

    override suspend fun clearHistory() {
        listDao.clearSearchHistory()
    }

    override suspend fun getGuessItems(): List<SearchGuessItem> {
        return searchSeedDataSource.load().searchGuesses
            .sortedBy { it.sortOrder }
            .map { it.toGuessItem() }
    }

    override suspend fun getSuggestions(query: String): List<SearchGuessItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val suggestions = (getHistory().map { SearchGuessItem(it, "最近搜索") } + getGuessItems())
            .distinctBy { it.title }
            .filter { it.title.contains(trimmed, ignoreCase = true) }

        return if (suggestions.isNotEmpty()) {
            suggestions.take(6)
        } else {
            listOf(
                SearchGuessItem(trimmed, "直接搜索"),
                SearchGuessItem("${trimmed}攻略", "相关笔记"),
                SearchGuessItem("${trimmed}测评", "近期热门"),
                SearchGuessItem("${trimmed}同款", "商品和搭配"),
                SearchGuessItem("${trimmed}合集", "高收藏内容"),
                SearchGuessItem("${trimmed}避雷", "经验分享")
            )
        }
    }

    override suspend fun getResults(query: String): List<SearchResultItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val results = searchSeedDataSource.load().searchResults
        val matchedKeyword = resolveKeyword(trimmed, results)
        return results
            .filter { it.keyword == matchedKeyword }
            .sortedBy { it.sortOrder }
            .map { it.toSearchResultItem() }
    }

    private fun resolveKeyword(
        query: String,
        results: List<SearchResultEntity>
    ): String {
        val normalized = query.lowercase()
        val matched = results.firstOrNull { item ->
            item.matchTokens.split(',')
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .any { token ->
                    normalized.contains(token) || token.contains(normalized)
                }
        }
        return matched?.keyword ?: DEFAULT_KEYWORD
    }

    private fun SearchGuessEntity.toGuessItem(): SearchGuessItem {
        return SearchGuessItem(
            title = title,
            meta = meta
        )
    }

    private fun SearchResultEntity.toSearchResultItem(): SearchResultItem {
        return SearchResultItem(
            filter = ResultFilter.valueOf(filter),
            title = title,
            subtitle = subtitle,
            meta = meta,
            badge = badge,
            badgeColorRes = when (badgeColorResName) {
                "xhs_search_chip_bg" -> R.color.xhs_search_chip_bg
                "xhs_search_hot_rank_bg" -> R.color.xhs_search_hot_rank_bg
                else -> R.color.xhs_search_quick_action_icon_bg
            }
        )
    }

    private companion object {
        const val DEFAULT_KEYWORD = "default"
    }
}
