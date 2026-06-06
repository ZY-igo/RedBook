/**
 * 文件说明： SearchSeedDataSource.kt
 * 作用： 封装本地数据访问能力，例如 Room、预置资源读取和初始化逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.local

import android.app.Application
import com.zhengyang.redbook.data.model.SearchGuessEntity
import com.zhengyang.redbook.data.model.SearchResultEntity
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchSeedDataSource @Inject constructor(
    private val application: Application
) {
    fun load(): SearchSeedPayload {
        val json = application.assets.open(SEARCH_FILE).bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        return SearchSeedPayload(
            searchGuesses = root.getJSONArray("searchGuesses").map { item ->
                SearchGuessEntity(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    meta = item.getString("meta"),
                    sortOrder = item.getInt("sortOrder")
                )
            },
            searchResults = root.getJSONArray("searchResults").map { item ->
                SearchResultEntity(
                    id = item.getString("id"),
                    keyword = item.getString("keyword"),
                    matchTokens = item.getString("matchTokens"),
                    filter = item.getString("filter"),
                    title = item.getString("title"),
                    subtitle = item.getString("subtitle"),
                    meta = item.getString("meta"),
                    badge = item.getString("badge"),
                    badgeColorResName = item.getString("badgeColorResName"),
                    sortOrder = item.getInt("sortOrder")
                )
            }
        )
    }

    private fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> {
        return List(length()) { index -> transform(getJSONObject(index)) }
    }

    companion object {
        private const val SEARCH_FILE = "search_data.json"
    }
}

data class SearchSeedPayload(
    val searchGuesses: List<SearchGuessEntity>,
    val searchResults: List<SearchResultEntity>
)
