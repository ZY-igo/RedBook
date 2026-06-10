/**
 * 文件说明：HomeRepository.kt
 * 作用：定义首页模块的数据仓库接口，抽象数据来源。
 * 备注：采用接口+实现分离模式，便于单元测试时使用 mock 替换真实数据源。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.model.DiscoverCategory
import com.zhengyang.redbook.data.model.FollowingUser
import com.zhengyang.redbook.data.model.HomeDiscoverItem

/**
 * 首页数据仓库接口。
 *
 * 定义首页模块对外部的数据需求抽象，不直接暴露数据来源细节。
 * 调用方（UseCase/ViewModel）只需面向此接口编程，
 * 具体数据来自网络还是本地缓存由实现类内部决策。
 *
 * 设计原则：
 * - 优先返回缓存数据提升体验，网络请求失败时降级到本地。
 * - 所有方法都是 suspend 函数，必须在协程作用域内调用。
 * - forceRefresh 参数用于控制是否绕过缓存强制刷新。
 */
interface HomeRepository {

    /**
     * 获取首页分类列表。
     *
     * 通常用于首页顶部频道栏的初始化，展示所有可选内容分类。
     *
     * @param forceRefresh 是否强制从网络刷新。false 时优先返回本地缓存。
     * @return 分类列表，按服务端排序字段排列。
     */
    suspend fun getCategories(forceRefresh: Boolean = false): List<DiscoverCategory>

    /**
     * 获取指定分类下的首页内容流。
     *
     * 用于展示某个频道下的笔记列表，如"推荐"、"关注"等频道。
     * 默认分页大小由实现类常量决定。
     *
     * @param categoryId 分类 ID，标识内容所属频道。
     * @param forceRefresh 是否强制从网络刷新。
     * @return 该分类下的内容流列表。
     */
    suspend fun getDiscoverItems(
        categoryId: String,
        forceRefresh: Boolean = false
    ): List<HomeDiscoverItem>

    /**
     * 分页获取指定分类下的首页内容流。
     *
     * 支持分页加载，用于下拉刷新和上拉加载更多场景。
     *
     * @param categoryId 分类 ID。
     * @param offset 分页偏移量，表示跳过的条目数。
     * @param limit 每页请求的条目数量。
     * @param forceRefresh 是否强制从网络刷新。
     * @return 分页后的内容流列表。
     */
    suspend fun getDiscoverItemsPage(
        categoryId: String,
        offset: Int,
        limit: Int,
        forceRefresh: Boolean = false
    ): List<HomeDiscoverItem>

    /**
     * 获取推荐关注的用户列表。
     *
     * 用于首页"关注"频道顶部的推荐用户区域，
     * 帮助用户快速发现感兴趣的其他用户。
     *
     * @param forceRefresh 是否强制从网络刷新。
     * @return 推荐关注的用户列表。
     */
    suspend fun getSuggestedFollowingUsers(forceRefresh: Boolean = false): List<FollowingUser>

    /**
     * 获取关注页的信息流内容。
     *
     * 返回用户关注对象的最新动态笔记。
     *
     * @param forceRefresh 是否强制从网络刷新。
     * @return 关注用户发布的笔记列表。
     */
    suspend fun getFollowingFeedItems(forceRefresh: Boolean = false): List<HomeDiscoverItem>

    /**
     * 分页获取关注页的信息流内容。
     *
     * 支持分页加载，用于关注列表的滚动加载更多。
     *
     * @param offset 分页偏移量。
     * @param limit 每页请求的条目数量。
     * @param forceRefresh 是否强制从网络刷新。
     * @return 分页后的关注动态列表。
     */
    suspend fun getFollowingFeedItemsPage(
        offset: Int,
        limit: Int,
        forceRefresh: Boolean = false
    ): List<HomeDiscoverItem>

    /**
     * 关注指定用户。
     *
     * @param userId 要关注的用户 ID。
     */
    suspend fun followUser(userId: String)
}