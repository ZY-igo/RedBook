package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.ui.search.SearchGuessItem
import com.zhengyang.redbook.ui.search.SearchResultItem

interface SearchRepository {
    suspend fun getHistory(limit: Int = 6): List<String>
    suspend fun saveHistory(query: String): List<String>
    suspend fun clearHistory()
    suspend fun getGuessItems(): List<SearchGuessItem>
    suspend fun getSuggestions(query: String): List<SearchGuessItem>
    suspend fun getResults(query: String): List<SearchResultItem>
}
