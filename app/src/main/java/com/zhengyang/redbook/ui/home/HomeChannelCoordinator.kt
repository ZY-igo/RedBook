package com.zhengyang.redbook.ui.home

/**
 * 首页频道协调器。
 *
 * 对外暴露更贴近交互层的频道管理接口，
 * 底层实际状态维护由 [HomeChannelState] 完成。
 *
 * @param persistedSelectionProvider 读取已持久化的“我的频道” id 列表。
 * @param selectionChangeListener “我的频道” id 列表变化时的回调，用于持久化。
 */
class HomeChannelCoordinator(
    persistedSelectionProvider: () -> List<String>? = { null },
    selectionChangeListener: (List<String>) -> Unit = {},
    private val state: HomeChannelState = HomeChannelState(
        persistedSelectionProvider = persistedSelectionProvider,
        selectionChangeListener = selectionChangeListener
    )
) {
    /**
     * 当前全部频道列表。
     */
    val allChannels: List<DiscoverCategoryItem>
        get() = state.allChannels

    /**
     * 当前“我的频道”列表。
     */
    val myChannels: List<DiscoverCategoryItem>
        get() = state.myChannels

    /**
     * 当前选中的频道。
     */
    val currentCategory: DiscoverCategoryItem?
        get() = state.currentCategory

    /**
     * 当前是否处于频道编辑模式。
     */
    var isEditMode: Boolean = false
        private set

    /**
     * 同步最新频道列表。
     *
     * @param categories 最新频道数据。
     */
    fun sync(categories: List<DiscoverCategoryItem>) {
        state.sync(categories)
    }

    /**
     * 设置当前频道。
     *
     * @param category 需要切换到的频道。
     */
    fun setCurrentCategory(category: DiscoverCategoryItem) {
        state.setCurrentCategory(category)
    }

    /**
     * 添加一个频道到“我的频道”。
     *
     * @param category 需要添加的频道。
     * @return 是否成功添加。
     */
    fun addChannel(category: DiscoverCategoryItem): Boolean {
        return state.addChannel(category)
    }

    /**
     * 从“我的频道”中移除频道。
     *
     * @param category 需要移除的频道。
     * @return 描述本次移除结果的状态对象。
     */
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

    /**
     * 判断当前频道是否允许移除。
     *
     * @param category 待判断频道。
     * @return 是否允许移除。
     */
    fun canRemoveChannel(category: DiscoverCategoryItem): Boolean {
        return state.canRemoveChannel(category)
    }

    /**
     * 查找“附近”页签使用的兜底频道。
     *
     * @return 适合展示在“附近”的频道；没有时返回 `null`。
     */
    fun findNearbyFallbackCategory(): DiscoverCategoryItem? {
        return state.findNearbyFallbackCategory()
    }

    /**
     * 切换编辑模式。
     *
     * @return 切换后的编辑模式状态。
     */
    fun toggleEditMode(): Boolean {
        isEditMode = !isEditMode
        return isEditMode
    }

    /**
     * 退出频道编辑模式。
     */
    fun exitEditMode() {
        isEditMode = false
    }

    /**
     * 频道移除结果。
     */
    sealed interface ChannelRemovalResult {
        /** 本次调用没有产生任何变化。 */
        data object Unchanged : ChannelRemovalResult

        /** 频道已移除，当前选中项未变化。 */
        data object Removed : ChannelRemovalResult

        /**
         * 频道已移除，且当前选中频道发生变化。
         *
         * @property category 移除后新的当前频道。
         */
        data class CurrentCategoryChanged(
            val category: DiscoverCategoryItem?
        ) : ChannelRemovalResult
    }
}
