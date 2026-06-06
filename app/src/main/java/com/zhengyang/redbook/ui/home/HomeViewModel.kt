package com.zhengyang.redbook.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhengyang.redbook.usecase.LoadHomeCategoriesUseCase
import com.zhengyang.redbook.usecase.HomeFollowingSeed
import com.zhengyang.redbook.usecase.LoadHomeDiscoverItemsUseCase
import com.zhengyang.redbook.usecase.LoadHomeFollowingSeedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadHomeCategories: LoadHomeCategoriesUseCase,
    private val loadHomeDiscoverItems: LoadHomeDiscoverItemsUseCase,
    private val loadHomeFollowingSeed: LoadHomeFollowingSeedUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun refreshDiscover(category: DiscoverCategoryItem) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(discoverItems = loadHomeDiscoverItems(category))
            }
        }
    }

    fun refreshFollowing() {
        viewModelScope.launch {
            if (_uiState.value.followingFeedItems.isEmpty() && _uiState.value.suggestedUsers.isEmpty()) {
                applyFollowingSeed(loadHomeFollowingSeed())
            }
        }
    }

    fun followUser(userId: String) {
        _uiState.update { state ->
            val user = state.suggestedUsers.firstOrNull { it.id == userId } ?: return@update state
            if (state.followingUsers.any { it.id == userId }) return@update state
            state.copy(
                suggestedUsers = state.suggestedUsers.filterNot { it.id == userId },
                followingUsers = state.followingUsers + user
            )
        }
    }

    fun dismissSuggestion(userId: String) {
        _uiState.update { state ->
            state.copy(suggestedUsers = state.suggestedUsers.filterNot { it.id == userId })
        }
    }

    private fun refreshFollowingSeed() {
        viewModelScope.launch {
            applyFollowingSeed(loadHomeFollowingSeed())
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val categories = loadHomeCategories()
            _uiState.update { it.copy(categories = categories) }
            categories.firstOrNull()?.let { refreshDiscover(it) }
            refreshFollowingSeed()
        }
    }

    private fun applyFollowingSeed(seed: HomeFollowingSeed) {
        _uiState.update { state ->
            state.copy(
                suggestedUsers = seed.suggestedUsers,
                followingFeedItems = seed.followingFeedItems
            )
        }
    }
}
