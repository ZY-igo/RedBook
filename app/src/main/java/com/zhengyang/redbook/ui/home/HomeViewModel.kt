/**
 * 文件说明：HomeViewModel.kt
 * 作用：负责首页模块的状态组织、数据加载与用户操作响应。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhengyang.redbook.usecase.FollowHomeUserUseCase
import com.zhengyang.redbook.usecase.LoadHomeCategoriesUseCase
import com.zhengyang.redbook.usecase.LoadHomeDiscoverItemsUseCase
import com.zhengyang.redbook.usecase.LoadHomeFollowingSeedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首页状态管理器
 *
 * 负责把首页多个数据源的加载流程收敛到统一状态树中，
 * 并对外暴露可观察的状态流与一次性事件流。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    /** 首页分类加载用例。 */
    private val loadHomeCategories: LoadHomeCategoriesUseCase,
    /** 首页发现流加载用例。 */
    private val loadHomeDiscoverItems: LoadHomeDiscoverItemsUseCase,
    /** 首页关注流与推荐用户加载用例。 */
    private val loadHomeFollowingSeed: LoadHomeFollowingSeedUseCase,
    /** 首页关注用户操作用例。 */
    private val followHomeUser: FollowHomeUserUseCase
) : ViewModel() {

    /** 首页内部可变状态流。 */
    private val _uiState = MutableStateFlow(HomeUiState())

    /** 首页可观察状态流。 */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeUiEvent>(extraBufferCapacity = 1)

    /** 首页一次性事件流。 */
    val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()

    /** 发现流当前已加载的条目数，用于分页偏移。 */
    private val discoverOffset = AtomicInteger(0)

    /** 关注流当前已加载的条目数，用于分页偏移。 */
    private val followingOffset = AtomicInteger(0)

    /** 发现流请求版本号，用于丢弃过期响应。 */
    private val discoverRequestVersion = AtomicInteger(0)

    private val discoverCache = linkedMapOf<String, List<HomeCardItem>>()

    private var currentDiscoverCategoryId: String? = null

    /** 关注流请求版本号，用于丢弃过期响应。 */
    private val followingRequestVersion = AtomicInteger(0)

    init {
        loadInitialData()
    }

    /**
     * 刷新当前选中分类下的发现流。
     *
     * 会重置分页偏移并生成新的请求版本号，
     * 从而保证过期请求返回时不会覆盖最新状态。
     *
     * @param category 当前选中的首页分类
     */
    fun refreshDiscover(category: DiscoverCategoryItem) {
        currentDiscoverCategoryId = category.id
        discoverOffset.set(0)
        val requestVersion = discoverRequestVersion.incrementAndGet()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDiscoverRefreshing = true,
                    isDiscoverLoadingMore = false,
                    discoverHasMore = true,
                    discoverErrorMessage = null,
                    discoverItems = if (it.discoverItems.isEmpty()) HomeSkeletonFactory.discover() else it.discoverItems
                )
            }

            loadHomeDiscoverItems(category, offset = 0).fold(
                onSuccess = { items ->
                    if (requestVersion != discoverRequestVersion.get()) return@fold
                    discoverCache[category.id] = items
                    discoverOffset.set(items.size)
                    _uiState.update { state ->
                        state.copy(
                            discoverItems = items,
                            isDiscoverRefreshing = false,
                            discoverHasMore = items.isNotEmpty(),
                            discoverErrorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    if (requestVersion != discoverRequestVersion.get()) return@fold
                    val message = error.toUserMessage("Failed to load discover feed")
                    val cachedItems = discoverCache[category.id]
                        ?: _uiState.value.discoverItems.filterNot(HomeCardItem::isSkeleton)
                    _uiState.update { state ->
                        state.copy(
                            discoverItems = cachedItems,
                            isDiscoverRefreshing = false,
                            discoverHasMore = cachedItems.isNotEmpty(),
                            discoverErrorMessage = message
                        )
                    }
                    _events.tryEmit(HomeUiEvent.ShowMessage(message))
                }
            )
        }
    }

    fun selectDiscoverCategory(category: DiscoverCategoryItem) {
        currentDiscoverCategoryId = category.id
        val cachedItems = discoverCache[category.id]
        discoverOffset.set(cachedItems?.size ?: 0)
        _uiState.update { state ->
            state.copy(
                discoverItems = cachedItems ?: HomeSkeletonFactory.discover(),
                isDiscoverRefreshing = cachedItems == null,
                isDiscoverLoadingMore = false,
                discoverHasMore = true,
                discoverErrorMessage = null
            )
        }
        refreshDiscover(category)
    }

    fun peekDiscoverItems(categoryId: String): List<HomeCardItem>? = discoverCache[categoryId]

    /**
     * 加载更多发现流内容。
     *
     * 仅在未刷新、未加载更多且仍有下一页时发起请求，
     * 同时会跳过骨架态列表，避免占位数据参与分页拼接。
     *
     * @param category 当前选中的首页分类
     */
    fun loadMoreDiscover(category: DiscoverCategoryItem) {
        currentDiscoverCategoryId = category.id
        if (_uiState.value.isDiscoverRefreshing || _uiState.value.isDiscoverLoadingMore || !_uiState.value.discoverHasMore) {
            return
        }
        if (_uiState.value.discoverItems.any(HomeCardItem::isSkeleton)) return

        val requestVersion = discoverRequestVersion.get()
        val offset = discoverOffset.get()
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscoverLoadingMore = true, discoverErrorMessage = null) }
            loadHomeDiscoverItems(category, offset = offset).fold(
                onSuccess = { items ->
                    if (requestVersion != discoverRequestVersion.get()) return@fold
                    if (!discoverOffset.compareAndSet(offset, offset + items.size)) return@fold
                    _uiState.update { state ->
                        val mergedItems = state.discoverItems + items
                        discoverCache[category.id] = mergedItems
                        state.copy(
                            discoverItems = mergedItems,
                            isDiscoverLoadingMore = false,
                            discoverHasMore = items.isNotEmpty(),
                            discoverErrorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    if (requestVersion != discoverRequestVersion.get()) return@fold
                    val message = error.toUserMessage("Failed to load discover feed")
                    _uiState.update { it.copy(isDiscoverLoadingMore = false, discoverErrorMessage = message) }
                    _events.tryEmit(HomeUiEvent.ShowMessage(message))
                }
            )
        }
    }

    /**
     * 刷新关注页数据。
     *
     * 当推荐用户或已关注用户尚未初始化时会强制走完整加载，
     * 否则允许在部分场景下仅结束刷新状态，避免重复请求。
     *
     * @param force 是否强制重新请求关注页数据
     *
     */
    fun refreshFollowing(force: Boolean = false) {
        followingOffset.set(0)
        val requestVersion = followingRequestVersion.incrementAndGet()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFollowingRefreshing = true,
                    isFollowingLoadingMore = false,
                    followingHasMore = true,
                    followingErrorMessage = null,
                    followingFeedItems = if (it.followingFeedItems.isEmpty()) HomeSkeletonFactory.following() else it.followingFeedItems
                )
            }

            val shouldLoad = force || _uiState.value.suggestedUsers.isEmpty() || _uiState.value.followingFeedItems.isEmpty()
            if (!shouldLoad && _uiState.value.followingFeedItems.none(HomeCardItem::isSkeleton)) {
                _uiState.update { it.copy(isFollowingRefreshing = false) }
                return@launch
            }

            loadHomeFollowingSeed(offset = 0).fold(
                onSuccess = { seed ->
                    if (requestVersion != followingRequestVersion.get()) return@fold
                    followingOffset.set(seed.followingFeedItems.size)
                    _uiState.update { state ->
                        state.copy(
                            suggestedUsers = seed.suggestedUsers,
                            followingFeedItems = seed.followingFeedItems,
                            isFollowingRefreshing = false,
                            followingErrorMessage = null,
                            followingHasMore = seed.followingFeedItems.isNotEmpty()
                        )
                    }
                },
                onFailure = { error ->
                    if (requestVersion != followingRequestVersion.get()) return@fold
                    val message = error.toUserMessage("Failed to load following feed")
                    val cachedItems = _uiState.value.followingFeedItems.filterNot(HomeCardItem::isSkeleton)
                    _uiState.update { state ->
                        state.copy(
                            followingFeedItems = cachedItems,
                            isFollowingRefreshing = false,
                            followingHasMore = cachedItems.isNotEmpty(),
                            followingErrorMessage = message
                        )
                    }
                    _events.tryEmit(HomeUiEvent.ShowMessage(message))
                }
            )
        }
    }

    /**
     * 加载更多关注流内容。
     *
     * 会复用当前请求版本号和分页偏移，
     * 并在成功后将新内容拼接到现有关注流尾部。
     */
    fun loadMoreFollowing() {
        if (_uiState.value.isFollowingRefreshing || _uiState.value.isFollowingLoadingMore || !_uiState.value.followingHasMore) {
            return
        }
        if (_uiState.value.followingFeedItems.any(HomeCardItem::isSkeleton)) return

        val requestVersion = followingRequestVersion.get()
        val offset = followingOffset.get()
        viewModelScope.launch {
            _uiState.update { it.copy(isFollowingLoadingMore = true, followingErrorMessage = null) }
            loadHomeFollowingSeed(offset = offset, limit = FOLLOWING_PAGE_SIZE).fold(
                onSuccess = { seed ->
                    if (requestVersion != followingRequestVersion.get()) return@fold
                    val newItems = seed.followingFeedItems
                    if (!followingOffset.compareAndSet(offset, offset + newItems.size)) return@fold
                    _uiState.update { state ->
                        state.copy(
                            suggestedUsers = if (state.suggestedUsers.isEmpty()) seed.suggestedUsers else state.suggestedUsers,
                            followingFeedItems = state.followingFeedItems + newItems,
                            isFollowingLoadingMore = false,
                            followingHasMore = newItems.isNotEmpty(),
                            followingErrorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    if (requestVersion != followingRequestVersion.get()) return@fold
                    val message = error.toUserMessage("Failed to load following feed")
                    _uiState.update { it.copy(isFollowingLoadingMore = false, followingErrorMessage = message) }
                    _events.tryEmit(HomeUiEvent.ShowMessage(message))
                }
            )
        }
    }

    /**
     * 关注一个推荐用户。
     *
     * 该操作采用乐观更新：先把用户从推荐区移到已关注区，
     * 如果后端失败，再将状态回滚。
     *
     * @param userId 目标用户 ID。
     */
    fun followUser(userId: String) {
        val targetUser = _uiState.value.suggestedUsers.firstOrNull { it.id == userId } ?: return
        _uiState.update { state ->
            val user = state.suggestedUsers.firstOrNull { it.id == userId } ?: return@update state
            if (state.followingUsers.any { it.id == userId }) return@update state
            state.copy(
                suggestedUsers = state.suggestedUsers.filterNot { it.id == userId },
                followingUsers = state.followingUsers + user
            )
        }
        viewModelScope.launch {
            followHomeUser(userId).onFailure { error ->
                _uiState.update { state ->
                    if (state.suggestedUsers.any { it.id == userId } || state.followingUsers.none { it.id == userId }) {
                        state
                    } else {
                        state.copy(
                            suggestedUsers = state.suggestedUsers + targetUser,
                            followingUsers = state.followingUsers.filterNot { it.id == userId }
                        )
                    }
                }
                _events.tryEmit(HomeUiEvent.ShowMessage(error.toUserMessage("Failed to follow user")))
            }
        }
    }

    /**
     * 从推荐列表中移除一个用户建议。
     *
     * @param userId 需要忽略的用户 ID。
     */
    fun dismissSuggestion(userId: String) {
        _uiState.update { state ->
            state.copy(suggestedUsers = state.suggestedUsers.filterNot { it.id == userId })
        }
    }

    /**
     * 加载首页初始数据。
     *
     * 首先初始化分类和发现流骨架态，
     * 然后分别触发分类加载与关注页首屏数据加载。
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitialLoading = true, discoverItems = HomeSkeletonFactory.discover()) }
            loadHomeCategories().fold(
                onSuccess = { categories ->
                    val initialDiscoverCategory =
                        categories.firstOrNull(DiscoverCategoryItem::isDefaultSelected)
                            ?: categories.firstOrNull()
                    _uiState.update {
                        it.copy(
                            categories = categories,
                            isInitialLoading = false
                        )
                    }
                    initialDiscoverCategory?.let(::selectDiscoverCategory)
                },
                onFailure = { error ->
                    val message = error.toUserMessage("Failed to load home channels")
                    _uiState.update {
                        it.copy(
                            discoverItems = emptyList(),
                            isInitialLoading = false,
                            discoverHasMore = false,
                            discoverErrorMessage = message
                        )
                    }
                    _events.tryEmit(HomeUiEvent.ShowMessage(message))
                }
            )
            refreshFollowing(force = true)
        }
    }

    /**
     * 将异常转换为用户可展示文案。
     *
     * @param defaultMessage 默认错误提示。
     * @return 优先使用异常消息，否则返回默认文案。
     */
    private fun Throwable.toUserMessage(defaultMessage: String): String {
        return message?.takeIf(String::isNotBlank) ?: defaultMessage
    }

    private companion object {
        /** 关注流分页请求大小。 */
        const val FOLLOWING_PAGE_SIZE = 8
    }
}
