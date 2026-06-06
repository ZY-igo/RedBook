/**
 * 文件说明： HomeRepository.kt
 * 作用： 协调不同数据源，并向上层提供统一的仓储实现。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.model.NoteItem
import com.zhengyang.redbook.ui.home.DiscoverCategoryItem
import com.zhengyang.redbook.ui.home.FollowingUserItem
import com.zhengyang.redbook.ui.home.HomeCardItem

interface HomeRepository {
    suspend fun getListContent(): List<NoteItem>
    suspend fun getCategories(): List<DiscoverCategoryItem>
    suspend fun getDiscoverItems(category: DiscoverCategoryItem): List<HomeCardItem>
    suspend fun getSuggestedFollowingUsers(): List<FollowingUserItem>
    suspend fun getFollowingFeedItems(): List<HomeCardItem>
}
