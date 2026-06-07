package com.zhengyang.redbook.ui.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeChannelState {
    private val _state = MutableStateFlow(HomeChannelSnapshot())
    val state: StateFlow<HomeChannelSnapshot> = _state.asStateFlow()

    val allChannels: List<DiscoverCategoryItem>
        get() = _state.value.allChannels

    val myChannels: List<DiscoverCategoryItem>
        get() = _state.value.myChannels

    val currentCategory: DiscoverCategoryItem?
        get() = _state.value.currentCategory

    fun sync(categories: List<DiscoverCategoryItem>) {
        if (categories.isEmpty()) return

        _state.update { snapshot ->
            val nextMyChannels = if (snapshot.myChannels.isEmpty()) {
                categories.filter { it.isDefaultSelected }
            } else {
                val selectedIds = snapshot.myChannels.mapTo(hashSetOf(), DiscoverCategoryItem::id)
                categories.filter { it.id in selectedIds }
                    .ifEmpty { categories.filter { it.isDefaultSelected } }
            }

            val currentId = snapshot.currentCategory?.id
            HomeChannelSnapshot(
                allChannels = categories,
                myChannels = nextMyChannels,
                currentCategory = nextMyChannels.firstOrNull { it.id == currentId } ?: nextMyChannels.firstOrNull()
            )
        }
    }

    fun setCurrentCategory(category: DiscoverCategoryItem) {
        _state.update { snapshot ->
            snapshot.copy(
                currentCategory = snapshot.myChannels.firstOrNull { it.id == category.id } ?: category
            )
        }
    }

    fun addChannel(category: DiscoverCategoryItem): Boolean {
        var added = false
        _state.update { snapshot ->
            if (snapshot.myChannels.any { it.id == category.id }) {
                snapshot
            } else {
                added = true
                snapshot.copy(myChannels = snapshot.myChannels + category)
            }
        }
        return added
    }

    fun removeChannel(category: DiscoverCategoryItem): Boolean {
        var removed = false
        _state.update { snapshot ->
            if (!canRemoveChannel(category, snapshot) || snapshot.myChannels.none { it.id == category.id }) {
                snapshot
            } else {
                removed = true
                val nextMyChannels = snapshot.myChannels.filterNot { it.id == category.id }
                snapshot.copy(
                    myChannels = nextMyChannels,
                    currentCategory = if (snapshot.currentCategory?.id == category.id) {
                        nextMyChannels.firstOrNull()
                    } else {
                        snapshot.currentCategory
                    }
                )
            }
        }
        return removed
    }

    fun canRemoveChannel(category: DiscoverCategoryItem): Boolean {
        return canRemoveChannel(category, _state.value)
    }

    fun findNearbyFallbackCategory(): DiscoverCategoryItem? {
        return _state.value.allChannels.firstOrNull { it.id == TRAVEL_CHANNEL_ID }
    }

    private fun canRemoveChannel(
        category: DiscoverCategoryItem,
        snapshot: HomeChannelSnapshot
    ): Boolean {
        return category.id != RECOMMEND_CHANNEL_ID && snapshot.myChannels.size > MIN_CHANNEL_COUNT
    }

    private companion object {
        private const val MIN_CHANNEL_COUNT = 1
        private const val RECOMMEND_CHANNEL_ID = "recommend"
        private const val TRAVEL_CHANNEL_ID = "travel"
    }
}

data class HomeChannelSnapshot(
    val allChannels: List<DiscoverCategoryItem> = emptyList(),
    val myChannels: List<DiscoverCategoryItem> = emptyList(),
    val currentCategory: DiscoverCategoryItem? = null
)
