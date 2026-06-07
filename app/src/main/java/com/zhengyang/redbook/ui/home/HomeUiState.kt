/**
 * 文件说明：HomeUiState.kt
 * 作用：定义首页模块使用的界面状态模型。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

/**
 * 首页 UI 状态
 *
 * 用于统一表达首页当前的分类、内容列表、推荐用户、
 * 加载状态、分页状态和错误提示信息。
 */
data class HomeUiState(
    /** 顶部分类列表。 */
    val categories: List<DiscoverCategoryItem> = emptyList(),

    /** 当前发现流卡片列表。 */
    val discoverItems: List<HomeCardItem> = emptyList(),

    /** 推荐关注用户列表。 */
    val suggestedUsers: List<FollowingUserItem> = emptyList(),

    /** 已关注用户列表。 */
    val followingUsers: List<FollowingUserItem> = emptyList(),

    /** 当前关注流卡片列表。 */
    val followingFeedItems: List<HomeCardItem> = emptyList(),

    /** 是否处于首页初始加载阶段。 */
    val isInitialLoading: Boolean = true,

    /** 是否正在刷新发现流。 */
    val isDiscoverRefreshing: Boolean = false,

    /** 是否正在刷新关注流。 */
    val isFollowingRefreshing: Boolean = false,

    /** 是否正在加载更多发现流内容。 */
    val isDiscoverLoadingMore: Boolean = false,

    /** 是否正在加载更多关注流内容。 */
    val isFollowingLoadingMore: Boolean = false,

    /** 发现流是否还有更多内容。 */
    val discoverHasMore: Boolean = true,

    /** 关注流是否还有更多内容。 */
    val followingHasMore: Boolean = true,

    /** 发现流错误提示，为空表示无错误。 */
    val discoverErrorMessage: String? = null,

    /** 关注流错误提示，为空表示无错误。 */
    val followingErrorMessage: String? = null
)
