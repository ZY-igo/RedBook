package com.zhengyang.redbook.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhengyang.redbook.usecase.FollowHomeUserUseCase
import com.zhengyang.redbook.usecase.HomeFollowingSeed
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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadHomeCategories: LoadHomeCategoriesUseCase,
    private val loadHomeDiscoverItems: LoadHomeDiscoverItemsUseCase,
    private val loadHomeFollowingSeed: LoadHomeFollowingSeedUseCase,
    private val followHomeUser: FollowHomeUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()

    private val discoverOffset = AtomicInteger(0)
    private val followingOffset = AtomicInteger(0)
    private val discoverRequestVersion = AtomicInteger(0)
    private val followingRequestVersion = AtomicInteger(0)

    init {
        loadInitialData()
    }

    fun refreshDiscover(category: DiscoverCategoryItem) {
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
                    _uiState.update { state ->
                        state.copy(
                            discoverItems = emptyList(),
                            isDiscoverRefreshing = false,
                            discoverHasMore = false,
                            discoverErrorMessage = message
                        )
                    }
                    _events.tryEmit(HomeUiEvent.ShowMessage(message))
                }
            )
        }
    }

    fun loadMoreDiscover(category: DiscoverCategoryItem) {
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
                        state.copy(
                            discoverItems = state.discoverItems + items,
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

            val shouldLoad = force || _uiState.value.suggestedUsers.isEmpty() || _uiState.value.followingUsers.isEmpty()
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
                    _uiState.update { state ->
                        state.copy(
                            followingFeedItems = emptyList(),
                            isFollowingRefreshing = false,
                            followingHasMore = false,
                            followingErrorMessage = message
                        )
                    }
                    _events.tryEmit(HomeUiEvent.ShowMessage(message))
                }
            )
        }
    }

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

    fun dismissSuggestion(userId: String) {
        _uiState.update { state ->
            state.copy(suggestedUsers = state.suggestedUsers.filterNot { it.id == userId })
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitialLoading = true, discoverItems = HomeSkeletonFactory.discover()) }
            loadHomeCategories().fold(
                onSuccess = { categories ->
                    _uiState.update {
                        it.copy(
                            categories = categories,
                            isInitialLoading = false
                        )
                    }
                    categories.firstOrNull()?.let(::refreshDiscover)
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

    private fun Throwable.toUserMessage(defaultMessage: String): String {
        return message?.takeIf(String::isNotBlank) ?: defaultMessage
    }

    private companion object {
        const val FOLLOWING_PAGE_SIZE = 8
    }
}
