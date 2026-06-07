/**
 * 文件说明：SearchRepository.kt
 * 作用：定义搜索场景的数据访问接口。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.ui.search.SearchGuessItem
import com.zhengyang.redbook.ui.search.SearchResultItem

/**
 * 搜索仓储接口
 *
 * 负责管理搜索历史、推荐词、联想词和结果列表，
 * 为搜索页提供统一的数据读取和写入入口。
 */
interface SearchRepository {

    /**
     * 获取搜索历史。
     *
     * @param limit 最多返回的历史条数。
     * @return 按最近使用时间排序的搜索词列表。
     */
    suspend fun getHistory(limit: Int = 6): List<String>

    /**
     * 保存一条搜索历史并返回最新历史列表。
     *
     * @param query 用户提交的搜索词。
     * @return 保存后的最新搜索历史列表。
     */
    suspend fun saveHistory(query: String): List<String>

    /**
     * 清空全部搜索历史。
     */
    suspend fun clearHistory()

    /**
     * 获取搜索页初始推荐词。
     *
     * @return 猜你想搜列表。
     */
    suspend fun getGuessItems(): List<SearchGuessItem>

    /**
     * 根据用户输入获取联想词。
     *
     * @param query 当前输入内容。
     * @return 联想词列表。
     */
    suspend fun getSuggestions(query: String): List<SearchGuessItem>

    /**
     * 根据搜索词获取结果列表。
     *
     * @param query 搜索词。
     * @return 搜索结果项列表。
     */
    suspend fun getResults(query: String): List<SearchResultItem>
}
