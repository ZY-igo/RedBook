/**
 * 文件说明： SearchRepository.kt
 * 作用： 协调不同数据源，并向上层提供统一的仓储实现。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
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
