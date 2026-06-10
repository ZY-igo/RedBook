package com.zhengyang.redbook.ui.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 首页频道纯状态容器。
 *
 * 负责维护全部频道、我的频道和当前选中频道之间的一致性。
 */
class HomeChannelState {
    /**
     * 内部可变频道快照。
     */
    private val _state = MutableStateFlow(HomeChannelSnapshot())

    /**
     * 对外暴露的只读频道状态流。
     */
    val state: StateFlow<HomeChannelSnapshot> = _state.asStateFlow()

    /**
     * 当前全部频道。
     */
    val allChannels: List<DiscoverCategoryItem>
        get() = _state.value.allChannels

    /**
     * 当前“我的频道”列表。
     */
    val myChannels: List<DiscoverCategoryItem>
        get() = _state.value.myChannels

    /**
     * 当前选中频道。
     */
    val currentCategory: DiscoverCategoryItem?
        get() = _state.value.currentCategory

    /**
     * 同步外部传入的频道列表。
     *
     * @param categories 最新频道列表。
     */
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
                currentCategory = nextMyChannels.firstOrNull { it.id == currentId }
                    ?: nextMyChannels.firstOrNull()
            )
        }
    }

    /**
     * 设置当前选中频道。
     *
     * @param category 需要切换到的频道。
     */
    fun setCurrentCategory(category: DiscoverCategoryItem) {
        _state.update { snapshot ->
            snapshot.copy(
                currentCategory = snapshot.myChannels.firstOrNull { it.id == category.id } ?: category
            )
        }
    }

    /**
     * 向“我的频道”追加频道。
     *
     * @param category 待添加频道。
     * @return 是否成功添加。
     */
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

    /**
     * 从“我的频道”移除频道。
     *
     * @param category 待移除频道。
     * @return 是否成功移除。
     */
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

    /**
     * 判断频道当前是否允许移除。
     *
     * @param category 待判断频道。
     * @return 是否允许移除。
     */
    fun canRemoveChannel(category: DiscoverCategoryItem): Boolean {
        return canRemoveChannel(category, _state.value)
    }

    /**
     * 查找“附近”页签的兜底频道。
     *
     * @return 合适的兜底频道；没有时返回 `null`。
     */
    fun findNearbyFallbackCategory(): DiscoverCategoryItem? {
        val channels = _state.value.allChannels
        return channels.firstOrNull { it.bucket == DiscoverCategoryBucket.RED && it.usesWaterfall }
            ?: channels.firstOrNull { it.id != RECOMMEND_CHANNEL_ID && it.usesWaterfall }
            ?: channels.firstOrNull { it.id == TRAVEL_CHANNEL_ID }
            ?: channels.firstOrNull { it.id != RECOMMEND_CHANNEL_ID }
    }

    /**
     * 基于指定快照判断频道是否允许被移除。
     */
    private fun canRemoveChannel(
        category: DiscoverCategoryItem,
        snapshot: HomeChannelSnapshot
    ): Boolean {
        return category.id != RECOMMEND_CHANNEL_ID && snapshot.myChannels.size > MIN_CHANNEL_COUNT
    }

    private companion object {
        /**
         * “我的频道”至少保留的数量。
         */
        private const val MIN_CHANNEL_COUNT = 1

        /**
         * 推荐频道固定 id，不允许删除。
         */
        private const val RECOMMEND_CHANNEL_ID = "recommend"

        /**
         * “附近”页签偏好的旅行频道 id。
         */
        private const val TRAVEL_CHANNEL_ID = "travel"
    }
}

/**
 * 频道状态快照。
 *
 * @property allChannels 全部频道。
 * @property myChannels 当前“我的频道”列表。
 * @property currentCategory 当前选中频道。
 */
data class HomeChannelSnapshot(
    val allChannels: List<DiscoverCategoryItem> = emptyList(),
    val myChannels: List<DiscoverCategoryItem> = emptyList(),
    val currentCategory: DiscoverCategoryItem? = null
)
