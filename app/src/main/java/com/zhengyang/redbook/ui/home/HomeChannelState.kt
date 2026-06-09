/**
 * 文件说明：HomeChannelState.kt
 * 作用：维护首页频道选择与排序相关的纯状态逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 首页频道状态容器
 *
 * 负责维护全量频道、我的频道和当前选中频道之间的一致性，
 * 并提供纯状态层面的新增、删除和同步逻辑。
 */
class HomeChannelState {
    /** 首页频道内部可变快照流。 */
    private val _state = MutableStateFlow(HomeChannelSnapshot())

    /** 对外暴露的只读频道快照流。 */
    val state: StateFlow<HomeChannelSnapshot> = _state.asStateFlow()

    val allChannels: List<DiscoverCategoryItem>
        get() = _state.value.allChannels

    val myChannels: List<DiscoverCategoryItem>
        get() = _state.value.myChannels

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
                currentCategory = nextMyChannels.firstOrNull { it.id == currentId } ?: nextMyChannels.firstOrNull()
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
     * 新增一个“我的频道”。
     *
     * @param category 需要新增的频道。
     * @return `true` 表示频道已成功加入当前列表。
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
     * 从“我的频道”中移除一个频道。
     *
     * @param category 需要移除的频道。
     * @return `true` 表示频道已成功移除。
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
     * 判断频道是否允许被移除。
     *
     * @param category 待判断频道。
     * @return `true` 表示当前状态下允许移除。
     */
    fun canRemoveChannel(category: DiscoverCategoryItem): Boolean {
        return canRemoveChannel(category, _state.value)
    }

    /**
     * 查找“附近”页签使用的兜底频道。
     *
     * @return 旅行频道；若不存在则返回 `null`。
     */
    fun findNearbyFallbackCategory(): DiscoverCategoryItem? {
        val channels = _state.value.allChannels
        return channels.firstOrNull { it.bucket == DiscoverCategoryBucket.RED && it.usesWaterfall }
            ?: channels.firstOrNull { it.id != RECOMMEND_CHANNEL_ID && it.usesWaterfall }
            ?: channels.firstOrNull { it.id == TRAVEL_CHANNEL_ID }
            ?: channels.firstOrNull { it.id != RECOMMEND_CHANNEL_ID }
    }

    /**
     * 基于指定快照判断频道是否可移除。
     *
     * @param category 待判断频道。
     * @param snapshot 当前用于判断的频道快照。
     * @return `true` 表示频道可移除。
     */
    private fun canRemoveChannel(
        category: DiscoverCategoryItem,
        snapshot: HomeChannelSnapshot
    ): Boolean {
        return category.id != RECOMMEND_CHANNEL_ID && snapshot.myChannels.size > MIN_CHANNEL_COUNT
    }

    private companion object {
        /** “我的频道”允许保留的最小频道数量。 */
        private const val MIN_CHANNEL_COUNT = 1
        /** 推荐频道固定标识，不允许被移除。 */
        private const val RECOMMEND_CHANNEL_ID = "recommend"
        /** “附近”页签优先使用的旅行频道标识。 */
        private const val TRAVEL_CHANNEL_ID = "travel"
    }
}

/**
 * 首页频道状态快照
 *
 * 用于同时描述全量频道、我的频道和当前选中频道，
 * 便于频道相关逻辑以不可变对象方式更新。
 */
data class HomeChannelSnapshot(
    /** 全量频道列表。 */
    val allChannels: List<DiscoverCategoryItem> = emptyList(),
    /** 当前“我的频道”列表。 */
    val myChannels: List<DiscoverCategoryItem> = emptyList(),
    /** 当前选中的频道。 */
    val currentCategory: DiscoverCategoryItem? = null
)
