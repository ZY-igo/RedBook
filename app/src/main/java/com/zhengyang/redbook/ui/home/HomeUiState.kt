/**
 * 文件说明： HomeUiState.kt
 * 作用： 承载首页相关的界面状态与交互逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

data class HomeUiState(
    val categories: List<DiscoverCategoryItem> = emptyList(),
    val discoverItems: List<HomeCardItem> = emptyList(),
    val suggestedUsers: List<FollowingUserItem> = emptyList(),
    val followingUsers: List<FollowingUserItem> = emptyList(),
    val followingFeedItems: List<HomeCardItem> = emptyList()
)
