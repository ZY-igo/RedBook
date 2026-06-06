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
