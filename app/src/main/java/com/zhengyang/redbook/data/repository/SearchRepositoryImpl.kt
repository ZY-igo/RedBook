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

class SearchRepositoryImpl @Inject constructor(
    private val listDao: ListDao,
    private val apiService: RedBookApiService,
    private val resourceMapper: RemoteResourceMapper
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

    override suspend fun getGuessItems(): List<SearchGuessItem> = withContext(Dispatchers.IO) {
        apiService.getSearchBootstrap().requireData().guessItems.map { it.toUiModel() }
    }

    override suspend fun getSuggestions(query: String): List<SearchGuessItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        apiService.getSearchSuggestions(trimmed).requireData().map { it.toUiModel() }
    }

    override suspend fun getResults(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        apiService.getSearchResults(trimmed).requireData().map { it.toUiModel() }
    }

    private fun RemoteSearchGuessDto.toUiModel(): SearchGuessItem {
        return SearchGuessItem(
            title = title,
            meta = meta
        )
    }

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
