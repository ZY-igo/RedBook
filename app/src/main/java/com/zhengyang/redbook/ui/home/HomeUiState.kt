package com.zhengyang.redbook.ui.home

data class HomeUiState(
    val categories: List<DiscoverCategoryItem> = emptyList(),
    val discoverItems: List<HomeCardItem> = emptyList(),
    val suggestedUsers: List<FollowingUserItem> = emptyList(),
    val followingUsers: List<FollowingUserItem> = emptyList(),
    val followingFeedItems: List<HomeCardItem> = emptyList()
)
