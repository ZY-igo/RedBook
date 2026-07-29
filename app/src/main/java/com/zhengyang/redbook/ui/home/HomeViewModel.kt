package com.zhengyang.redbook.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhengyang.redbook.core.common.Resource
import com.zhengyang.redbook.core.common.toUserMessage
import com.zhengyang.redbook.usecase.FollowHomeUserUseCase
import com.zhengyang.redbook.usecase.LoadHomeCategoriesUseCase
import com.zhengyang.redbook.usecase.LoadHomeFollowingSeedUseCase
import com.zhengyang.redbook.usecase.LoadHomeDiscoverItemsUseCase
import com.zhengyang.redbook.usecase.NoParams
import com.zhengyang.redbook.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
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
 * 首页 ViewModel。
 *
 * 这个类负责暴露首页需要的统一状态和一次性事件，
 * 并把“发现”和“关注”两块业务委托给各自的 coordinator 处理。
 *
 * @property loadHomeCategories 加载首页分类列表的用例。
 * @property uiState 提供给界面的只读状态流。
 * @property events 提供给界面的只读事件流。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadHomeCategories: LoadHomeCategoriesUseCase,
    loadHomeDiscoverItems: LoadHomeDiscoverItemsUseCase,
    loadHomeFollowingSeed: LoadHomeFollowingSeedUseCase,
    followHomeUser: FollowHomeUserUseCase
) : ViewModel() {

    /**
     * ViewModel 内部可变的页面状态。
     *
     * coordinator 会直接基于它更新首页 UI 快照。
     */
    private val _uiState = MutableStateFlow(HomeUiState())

    /**
     * 暴露给 UI 层的只读状态流。
     *
     * Fragment 只负责收集，不允许直接修改。
     */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * ViewModel 内部的一次性事件流。
     *
     * 这里主要用于错误提示、Toast 等不适合持久化在状态里的事件。
     */
    private val _events = MutableSharedFlow<HomeUiEvent>(extraBufferCapacity = 1)

    /**
     * 暴露给 UI 层的只读事件流。
     */
    val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()

    /**
     * “发现”区协调器。
     *
     * 负责分类切换、刷新、分页和缓存读取。
     */
    private val discoverCoordinator = HomeDiscoverCoordinator(
        uiState = _uiState,
        events = _events,
        loadHomeDiscoverItems = loadHomeDiscoverItems
    )

    /**
     * “关注”区协调器。
     *
     * 负责推荐用户、关注动作和分页加载。
     */
    private val followingCoordinator = HomeFollowingCoordinator(
        uiState = _uiState,
        events = _events,
        loadHomeFollowingSeed = loadHomeFollowingSeed,
        followHomeUser = followHomeUser
    )

    init {
        loadInitialData()
    }

    /**
     * 主动刷新指定分类下的“发现”列表。
     *
     * @param category 当前需要刷新的分类。
     */
    fun refreshDiscover(category: DiscoverCategoryItem) {
        viewModelScope.launch {
            discoverCoordinator.refresh(category)
        }
    }

    /**
     * 选择一个新的“发现”分类。
     *
     * @param category 用户当前选中的分类。
     */
    fun selectDiscoverCategory(category: DiscoverCategoryItem) {
        viewModelScope.launch {
            discoverCoordinator.selectCategory(category)
        }
    }

    /**
     * 读取某个分类已经缓存的“发现”列表。
     *
     * 该方法只读取内存，不触发新的加载请求。
     *
     * @param categoryId 分类 id。
     * @return 对应分类的缓存列表；如果没有缓存则返回 `null`。
     */
    fun peekDiscoverItems(categoryId: String): List<HomeCardItem>? {
        return discoverCoordinator.peekItems(categoryId)
    }

    /**
     * 加载“发现”列表的下一页内容。
     *
     * @param category 当前分页所属的分类。
     */
    fun loadMoreDiscover(category: DiscoverCategoryItem) {
        viewModelScope.launch {
            discoverCoordinator.loadMore(category)
        }
    }

    /**
     * 刷新“关注”页数据。
     *
     * @param force 是否强制刷新，即尽量绕过现有缓存。
     */
    fun refreshFollowing(force: Boolean = false) {
        viewModelScope.launch {
            followingCoordinator.refresh(force)
        }
    }

    /**
     * 加载更多“关注”流内容。
     */
    fun loadMoreFollowing() {
        viewModelScope.launch {
            followingCoordinator.loadMore(FOLLOWING_PAGE_SIZE)
        }
    }

    /**
     * 关注一个推荐用户。
     *
     * 这里先让 coordinator 完成一次本地预更新，
     * 再异步确认真正的关注请求。
     *
     * @param userId 目标用户 id。
     */
    fun followUser(userId: String) {
        val targetUser = followingCoordinator.beginFollowUser(userId) ?: return
        viewModelScope.launch {
            followingCoordinator.confirmFollowUser(userId, targetUser)
        }
    }

    /**
     * 从推荐列表中移除一个用户建议项。
     *
     * @param userId 需要移除的用户 id。
     */
    fun dismissSuggestion(userId: String) {
        followingCoordinator.dismissSuggestion(userId)
    }

    /**
     * 加载首页初始数据。
     *
     * 该流程会先进入首页首屏 loading，
     * 然后请求分类列表，最后无论分类是否成功都刷新“关注”区。
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInitialLoading = true,
                    discoverItems = HomeSkeletonFactory.discover()
                )
            }

            when (val result = loadHomeCategories(NoParams)) {
                is Resource.Success -> {
                    val categories = result.data
                    val initialCategory = categories.firstOrNull(DiscoverCategoryItem::isDefaultSelected)
                        ?: categories.firstOrNull()
                    AppLogger.d(
                        "HomeViewModel",
                        "Loaded categories. size=${categories.size}, initialCategory=${initialCategory?.id}, ids=${categories.joinToString { it.id }}"
                    )

                    _uiState.update {
                        it.copy(
                            categories = categories,
                            isInitialLoading = false
                        )
                    }

                    if (initialCategory != null) {
                        discoverCoordinator.selectCategory(initialCategory)
                    }
                }

                is Resource.Error -> {
                    val message = result.throwable.toUserMessage("Failed to load home channels")
                    AppLogger.e("HomeViewModel", "Failed to load categories: $message", result.throwable)
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

                is Resource.Loading -> Unit
            }

            followingCoordinator.refresh(force = true)
        }
    }

    private companion object {
        /**
         * “关注”流单次分页加载数量。
         */
        const val FOLLOWING_PAGE_SIZE = 8
    }
}
