/**
 * 文件说明：HomeChannelCoordinator.kt
 * 作用：负责首页频道管理的状态协调与交互调度。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

/**
 * 首页频道协调器
 *
 * 负责对外暴露频道列表、当前选中项和编辑模式等组合状态，
 * 将 [HomeChannelState] 的纯状态能力封装为更贴近界面交互的调用入口。
 */
class HomeChannelCoordinator(
    /** 首页频道底层状态容器。 */
    private val state: HomeChannelState = HomeChannelState()
) {
    /** 全量频道列表。 */
    val allChannels: List<DiscoverCategoryItem>
        get() = state.allChannels

    val myChannels: List<DiscoverCategoryItem>
        get() = state.myChannels

    val currentCategory: DiscoverCategoryItem?
        get() = state.currentCategory

    var isEditMode: Boolean = false
        private set

    /**
     * 同步频道数据源。
     *
     * @param categories 最新频道列表。
     */
    fun sync(categories: List<DiscoverCategoryItem>) {
        state.sync(categories)
    }

    /**
     * 设置当前选中频道。
     *
     * @param category 需要切换到的频道。
     */
    fun setCurrentCategory(category: DiscoverCategoryItem) {
        state.setCurrentCategory(category)
    }

    /**
     * 将频道加入“我的频道”。
     *
     * @param category 需要加入的频道。
     * @return `true` 表示本次调用成功新增了频道。
     */
    fun addChannel(category: DiscoverCategoryItem): Boolean {
        return state.addChannel(category)
    }

    /**
     * 从“我的频道”中移除频道。
     *
     * @param category 需要移除的频道。
     * @return 当前移除动作的结果，用于决定界面后续刷新策略。
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
     * 判断频道当前是否允许被移除。
     *
     * @param category 待判断的频道。
     * @return `true` 表示当前可以移除。
     */
    fun canRemoveChannel(category: DiscoverCategoryItem): Boolean {
        return state.canRemoveChannel(category)
    }

    /**
     * 查找“附近”页签的兜底频道。
     *
     * @return 用于附近页签展示的频道；若不存在则返回 `null`。
     */
    fun findNearbyFallbackCategory(): DiscoverCategoryItem? {
        return state.findNearbyFallbackCategory()
    }

    /**
     * 切换频道编辑模式。
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
     * 频道移除结果枚举。
     *
     * 用于描述移除动作是否生效，以及是否影响到了当前选中频道。
     */
    sealed interface ChannelRemovalResult {
        /** 当前调用没有产生任何状态变化。 */
        data object Unchanged : ChannelRemovalResult
        /** 频道已被移除，当前选中频道未变化。 */
        data object Removed : ChannelRemovalResult
        data class CurrentCategoryChanged(
            /** 移除后新的当前频道，若已空则为 `null`。 */
            val category: DiscoverCategoryItem?
        ) : ChannelRemovalResult
    }
}
