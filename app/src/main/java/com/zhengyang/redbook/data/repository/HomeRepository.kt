/**
 * 文件说明：HomeRepository.kt
 * 作用：定义首页场景的数据访问接口。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.model.DiscoverCategory
import com.zhengyang.redbook.data.model.FollowingUser
import com.zhengyang.redbook.data.model.HomeDiscoverItem

/**
 * 首页仓储接口
 *
 * 负责向上层提供发现页、关注流和关注操作所需的数据入口，
 * 屏蔽底层接口细节和数据源差异。
 */
interface HomeRepository {

    /**
     * 获取首页分类列表。
     *
     * @return 已按业务语义整理好的分类集合。
     */
    suspend fun getCategories(): List<DiscoverCategory>

    /**
     * 获取某个分类下的首页内容。
     *
     * @param categoryId 分类标识。
     * @return 当前分类下的内容列表。
     */
    suspend fun getDiscoverItems(categoryId: String): List<HomeDiscoverItem>

    /**
     * 分页获取某个分类下的首页内容。
     *
     * @param categoryId 分类标识。
     * @param offset 偏移量。
     * @param limit 单次请求数量。
     * @return 当前分页范围内的内容列表。
     */
    suspend fun getDiscoverItemsPage(categoryId: String, offset: Int, limit: Int): List<HomeDiscoverItem>

    /**
     * 获取关注页推荐用户列表。
     *
     * @return 推荐关注用户集合。
     */
    suspend fun getSuggestedFollowingUsers(): List<FollowingUser>

    /**
     * 获取关注流首页内容。
     *
     * @return 默认分页大小下的关注流内容列表。
     */
    suspend fun getFollowingFeedItems(): List<HomeDiscoverItem>

    /**
     * 分页获取关注流内容。
     *
     * @param offset 偏移量。
     * @param limit 单次请求数量。
     * @return 当前分页范围内的关注流内容列表。
     */
    suspend fun getFollowingFeedItemsPage(offset: Int, limit: Int): List<HomeDiscoverItem>

    /**
     * 关注指定用户。
     *
     * @param userId 目标用户 ID。
     */
    suspend fun followUser(userId: String)
}
