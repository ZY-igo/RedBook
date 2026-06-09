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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadHomeCategories: LoadHomeCategoriesUseCase,
    loadHomeDiscoverItems: LoadHomeDiscoverItemsUseCase,
    loadHomeFollowingSeed: LoadHomeFollowingSeedUseCase,
    followHomeUser: FollowHomeUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()

    private val discoverCoordinator = HomeDiscoverCoordinator(
        uiState = _uiState,
        events = _events,
        loadHomeDiscoverItems = loadHomeDiscoverItems
    )
    private val followingCoordinator = HomeFollowingCoordinator(
        uiState = _uiState,
        events = _events,
        loadHomeFollowingSeed = loadHomeFollowingSeed,
        followHomeUser = followHomeUser
    )

    init {
        loadInitialData()
    }

    fun refreshDiscover(category: DiscoverCategoryItem) {
        viewModelScope.launch {
            discoverCoordinator.refresh(category)
        }
    }

    fun selectDiscoverCategory(category: DiscoverCategoryItem) {
        viewModelScope.launch {
            discoverCoordinator.selectCategory(category)
        }
    }

    fun peekDiscoverItems(categoryId: String): List<HomeCardItem>? {
        return discoverCoordinator.peekItems(categoryId)
    }

    fun loadMoreDiscover(category: DiscoverCategoryItem) {
        viewModelScope.launch {
            discoverCoordinator.loadMore(category)
        }
    }

    fun refreshFollowing(force: Boolean = false) {
        viewModelScope.launch {
            followingCoordinator.refresh(force)
        }
    }

    fun loadMoreFollowing() {
        viewModelScope.launch {
            followingCoordinator.loadMore(FOLLOWING_PAGE_SIZE)
        }
    }

    fun followUser(userId: String) {
        val targetUser = followingCoordinator.beginFollowUser(userId) ?: return
        viewModelScope.launch {
            followingCoordinator.confirmFollowUser(userId, targetUser)
        }
    }

    fun dismissSuggestion(userId: String) {
        followingCoordinator.dismissSuggestion(userId)
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitialLoading = true, discoverItems = HomeSkeletonFactory.discover()) }
            when (val result = loadHomeCategories(NoParams)) {
                is Resource.Success -> {
                    val categories = result.data
                    val initialCategory = categories.firstOrNull(DiscoverCategoryItem::isDefaultSelected)
                        ?: categories.firstOrNull()
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
        const val FOLLOWING_PAGE_SIZE = 8
    }
}
