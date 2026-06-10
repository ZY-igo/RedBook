package com.zhengyang.redbook.ui.home

import com.zhengyang.redbook.core.common.Resource
import com.zhengyang.redbook.core.common.toUserMessage
import com.zhengyang.redbook.usecase.FollowHomeUserUseCase
import com.zhengyang.redbook.usecase.LoadHomeFollowingSeedParams
import com.zhengyang.redbook.usecase.LoadHomeFollowingSeedUseCase
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * 首页“关注”区协调器。
 *
 * 负责“关注”流的首屏刷新、分页、推荐用户处理以及关注动作回滚逻辑。
 */
class HomeFollowingCoordinator(
    private val uiState: MutableStateFlow<HomeUiState>,
    private val events: MutableSharedFlow<HomeUiEvent>,
    private val loadHomeFollowingSeed: LoadHomeFollowingSeedUseCase,
    private val followHomeUser: FollowHomeUserUseCase
) {
    /**
     * 当前“关注”流的分页偏移。
     */
    private val followingOffset = AtomicInteger(0)

    /**
     * 当前“关注”请求版本号。
     *
     * 用于忽略晚到的旧响应。
     */
    private val followingRequestVersion = AtomicInteger(0)

    /**
     * 刷新“关注”页数据。
     *
     * @param force 是否强制刷新底层数据源。
     */
    suspend fun refresh(force: Boolean = false) {
        followingOffset.set(0)
        val requestVersion = followingRequestVersion.incrementAndGet()
        uiState.update {
            it.copy(
                isFollowingRefreshing = true,
                isFollowingLoadingMore = false,
                followingHasMore = true,
                followingErrorMessage = null,
                followingFeedItems = if (it.followingFeedItems.isEmpty()) {
                    HomeSkeletonFactory.following()
                } else {
                    it.followingFeedItems
                }
            )
        }

        val shouldLoad = force ||
            uiState.value.suggestedUsers.isEmpty() ||
            uiState.value.followingFeedItems.isEmpty()
        if (!shouldLoad && uiState.value.followingFeedItems.none(HomeCardItem::isSkeleton)) {
            uiState.update { it.copy(isFollowingRefreshing = false) }
            return
        }

        when (
            val result = loadHomeFollowingSeed(
                LoadHomeFollowingSeedParams(forceRefresh = force)
            )
        ) {
            is Resource.Success -> {
                if (requestVersion != followingRequestVersion.get()) return
                val seed = result.data
                followingOffset.set(seed.followingFeedItems.size)
                uiState.update { state ->
                    state.copy(
                        suggestedUsers = seed.suggestedUsers,
                        followingFeedItems = seed.followingFeedItems,
                        isFollowingRefreshing = false,
                        followingErrorMessage = null,
                        followingHasMore = seed.followingFeedItems.isNotEmpty()
                    )
                }
            }

            is Resource.Error -> {
                if (requestVersion != followingRequestVersion.get()) return
                val message = result.throwable.toUserMessage("Failed to load following feed")
                val cachedItems = uiState.value.followingFeedItems.filterNot(HomeCardItem::isSkeleton)
                uiState.update { state ->
                    state.copy(
                        followingFeedItems = cachedItems,
                        isFollowingRefreshing = false,
                        followingHasMore = cachedItems.isNotEmpty(),
                        followingErrorMessage = message
                    )
                }
                events.tryEmit(HomeUiEvent.ShowMessage(message))
            }

            is Resource.Loading -> Unit
        }
    }

    /**
     * 加载“关注”流下一页。
     *
     * @param pageSize 单次拉取数量。
     */
    suspend fun loadMore(pageSize: Int) {
        val state = uiState.value
        if (state.isFollowingRefreshing || state.isFollowingLoadingMore || !state.followingHasMore) {
            return
        }
        if (state.followingFeedItems.any(HomeCardItem::isSkeleton)) return

        val requestVersion = followingRequestVersion.get()
        val offset = followingOffset.get()
        uiState.update { it.copy(isFollowingLoadingMore = true, followingErrorMessage = null) }

        when (
            val result = loadHomeFollowingSeed(
                LoadHomeFollowingSeedParams(offset = offset, limit = pageSize, forceRefresh = false)
            )
        ) {
            is Resource.Success -> {
                if (requestVersion != followingRequestVersion.get()) return
                val newItems = result.data.followingFeedItems
                if (!followingOffset.compareAndSet(offset, offset + newItems.size)) return
                uiState.update { current ->
                    current.copy(
                        suggestedUsers = if (current.suggestedUsers.isEmpty()) {
                            result.data.suggestedUsers
                        } else {
                            current.suggestedUsers
                        },
                        followingFeedItems = current.followingFeedItems + newItems,
                        isFollowingLoadingMore = false,
                        followingHasMore = newItems.isNotEmpty(),
                        followingErrorMessage = null
                    )
                }
            }

            is Resource.Error -> {
                if (requestVersion != followingRequestVersion.get()) return
                val message = result.throwable.toUserMessage("Failed to load following feed")
                uiState.update { it.copy(isFollowingLoadingMore = false, followingErrorMessage = message) }
                events.tryEmit(HomeUiEvent.ShowMessage(message))
            }

            is Resource.Loading -> Unit
        }
    }

    /**
     * 在发起关注请求前先做一次本地状态预更新。
     *
     * @param userId 目标用户 id。
     * @return 被关注的用户；如果未找到则返回 `null`。
     */
    fun beginFollowUser(userId: String): FollowingUserItem? {
        val targetUser = uiState.value.suggestedUsers.firstOrNull { it.id == userId } ?: return null
        uiState.update { state ->
            val user = state.suggestedUsers.firstOrNull { it.id == userId } ?: return@update state
            if (state.followingUsers.any { it.id == userId }) return@update state
            state.copy(
                suggestedUsers = state.suggestedUsers.filterNot { it.id == userId },
                followingUsers = state.followingUsers + user
            )
        }
        return targetUser
    }

    /**
     * 确认关注用户。
     *
     * 如果请求失败，会把本地预更新回滚回去。
     *
     * @param userId 目标用户 id。
     * @param targetUser 预更新阶段保存的用户快照。
     */
    suspend fun confirmFollowUser(userId: String, targetUser: FollowingUserItem) {
        when (val result = followHomeUser(userId)) {
            is Resource.Success -> Unit

            is Resource.Error -> {
                uiState.update { state ->
                    if (state.suggestedUsers.any { it.id == userId } || state.followingUsers.none { it.id == userId }) {
                        state
                    } else {
                        state.copy(
                            suggestedUsers = state.suggestedUsers + targetUser,
                            followingUsers = state.followingUsers.filterNot { it.id == userId }
                        )
                    }
                }
                events.tryEmit(
                    HomeUiEvent.ShowMessage(
                        result.throwable.toUserMessage("Failed to follow user")
                    )
                )
            }

            is Resource.Loading -> Unit
        }
    }

    /**
     * 移除一个推荐用户建议项。
     *
     * @param userId 目标用户 id。
     */
    fun dismissSuggestion(userId: String) {
        uiState.update { state ->
            state.copy(suggestedUsers = state.suggestedUsers.filterNot { it.id == userId })
        }
    }
}
