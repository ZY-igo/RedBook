/**
 * 文件说明： HomeRepository.kt
 * 作用： 协调不同数据源，并向上层提供统一的仓储实现。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.model.DiscoverCategory
import com.zhengyang.redbook.data.model.FollowingUser
import com.zhengyang.redbook.data.model.HomeDiscoverItem

interface HomeRepository {
    suspend fun getCategories(): List<DiscoverCategory>
    suspend fun getDiscoverItems(categoryId: String): List<HomeDiscoverItem>
    suspend fun getDiscoverItemsPage(categoryId: String, offset: Int, limit: Int): List<HomeDiscoverItem>
    suspend fun getSuggestedFollowingUsers(): List<FollowingUser>
    suspend fun getFollowingFeedItems(): List<HomeDiscoverItem>
    suspend fun getFollowingFeedItemsPage(offset: Int, limit: Int): List<HomeDiscoverItem>
    suspend fun followUser(userId: String)
}
