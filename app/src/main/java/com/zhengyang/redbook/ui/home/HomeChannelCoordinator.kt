package com.zhengyang.redbook.ui.home

class HomeChannelCoordinator(
    private val state: HomeChannelState = HomeChannelState()
) {
    val allChannels: List<DiscoverCategoryItem>
        get() = state.allChannels

    val myChannels: List<DiscoverCategoryItem>
        get() = state.myChannels

    val currentCategory: DiscoverCategoryItem?
        get() = state.currentCategory

    var isEditMode: Boolean = false
        private set

    fun sync(categories: List<DiscoverCategoryItem>) {
        state.sync(categories)
    }

    fun setCurrentCategory(category: DiscoverCategoryItem) {
        state.setCurrentCategory(category)
    }

    fun addChannel(category: DiscoverCategoryItem): Boolean {
        return state.addChannel(category)
    }

    fun removeChannel(category: DiscoverCategoryItem): ChannelRemovalResult {
        val previousCurrentId = currentCategory?.id
        if (!state.removeChannel(category)) {
            return ChannelRemovalResult.Unchanged
        }
        return if (previousCurrentId == category.id) {
            ChannelRemovalResult.CurrentCategoryChanged(state.currentCategory)
        } else {
            ChannelRemovalResult.Removed
        }
    }

    fun canRemoveChannel(category: DiscoverCategoryItem): Boolean {
        return state.canRemoveChannel(category)
    }

    fun findNearbyFallbackCategory(): DiscoverCategoryItem? {
        return state.findNearbyFallbackCategory()
    }

    fun toggleEditMode(): Boolean {
        isEditMode = !isEditMode
        return isEditMode
    }

    fun exitEditMode() {
        isEditMode = false
    }

    sealed interface ChannelRemovalResult {
        data object Unchanged : ChannelRemovalResult
        data object Removed : ChannelRemovalResult
        data class CurrentCategoryChanged(
            val category: DiscoverCategoryItem?
        ) : ChannelRemovalResult
    }
}
