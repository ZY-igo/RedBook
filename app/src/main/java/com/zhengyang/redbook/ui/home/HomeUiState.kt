package com.zhengyang.redbook.ui.home

/**
 * 首页统一 UI 状态。
 *
 * 这个状态对象同时承载分类、发现流、关注流、推荐用户以及加载错误信息。
 */
data class HomeUiState(
    val categories: List<DiscoverCategoryItem> = emptyList(),
    val discoverItems: List<HomeCardItem> = emptyList(),
    val suggestedUsers: List<FollowingUserItem> = emptyList(),
    val followingUsers: List<FollowingUserItem> = emptyList(),
    val followingFeedItems: List<HomeCardItem> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isDiscoverRefreshing: Boolean = false,
    val isFollowingRefreshing: Boolean = false,
    val isDiscoverLoadingMore: Boolean = false,
    val isFollowingLoadingMore: Boolean = false,
    val discoverHasMore: Boolean = true,
    val followingHasMore: Boolean = true,
    val discoverErrorMessage: String? = null,
    val followingErrorMessage: String? = null
)
