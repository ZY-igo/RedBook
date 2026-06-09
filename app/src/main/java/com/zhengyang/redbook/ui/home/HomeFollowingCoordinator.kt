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

class HomeFollowingCoordinator(
    private val uiState: MutableStateFlow<HomeUiState>,
    private val events: MutableSharedFlow<HomeUiEvent>,
    private val loadHomeFollowingSeed: LoadHomeFollowingSeedUseCase,
    private val followHomeUser: FollowHomeUserUseCase
) {
    private val followingOffset = AtomicInteger(0)
    private val followingRequestVersion = AtomicInteger(0)

    suspend fun refresh(force: Boolean = false) {
        followingOffset.set(0)
        val requestVersion = followingRequestVersion.incrementAndGet()
        uiState.update {
            it.copy(
                isFollowingRefreshing = true,
                isFollowingLoadingMore = false,
                followingHasMore = true,
                followingErrorMessage = null,
                followingFeedItems = if (it.followingFeedItems.isEmpty()) HomeSkeletonFactory.following() else it.followingFeedItems
            )
        }

        val shouldLoad = force || uiState.value.suggestedUsers.isEmpty() || uiState.value.followingFeedItems.isEmpty()
        if (!shouldLoad && uiState.value.followingFeedItems.none(HomeCardItem::isSkeleton)) {
            uiState.update { it.copy(isFollowingRefreshing = false) }
            return
        }

        when (val result = loadHomeFollowingSeed(LoadHomeFollowingSeedParams())) {
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
                LoadHomeFollowingSeedParams(offset = offset, limit = pageSize)
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

    fun dismissSuggestion(userId: String) {
        uiState.update { state ->
            state.copy(suggestedUsers = state.suggestedUsers.filterNot { it.id == userId })
        }
    }
}
