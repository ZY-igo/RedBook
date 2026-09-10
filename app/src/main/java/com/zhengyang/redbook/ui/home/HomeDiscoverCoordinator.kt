package com.zhengyang.redbook.ui.home

import com.zhengyang.redbook.core.common.Resource
import com.zhengyang.redbook.core.common.toUserMessage
import com.zhengyang.redbook.usecase.LoadHomeDiscoverItemsParams
import com.zhengyang.redbook.usecase.LoadHomeDiscoverItemsUseCase
import com.zhengyang.redbook.utils.AppLogger
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * 首页“发现”区协调器。
 *
 * 负责封装“发现”区的分类切换、刷新、分页和分类级缓存逻辑，
 * 让 [HomeViewModel] 不必直接处理所有细节。
 */
class HomeDiscoverCoordinator(
    private val uiState: MutableStateFlow<HomeUiState>,
    private val events: MutableSharedFlow<HomeUiEvent>,
    private val loadHomeDiscoverItems: LoadHomeDiscoverItemsUseCase
) {
    /**
     * 当前分类已经加载到的偏移量。
     */
    private val discoverOffset = AtomicInteger(0)

    /**
     * 当前“发现”请求版本号。
     *
     * 用于丢弃过时响应，避免旧请求覆盖新状态。
     */
    private val discoverRequestVersion = AtomicInteger(0)

    /**
     * 正在预取中的分类 id 集合，防止同一分类重复发起预取请求。
     */
    private val prefetchingIds = mutableSetOf<String>()

    /**
     * 按分类缓存已经拿到的卡片列表。
     */
    private val discoverCache = linkedMapOf<String, List<HomeCardItem>>()

    /**
     * 当前激活分类 id。
     */
    var currentCategoryId: String? = null
        private set

    /**
     * 强制刷新指定分类的数据。
     *
     * @param category 需要刷新的分类。
     */
    suspend fun refresh(category: DiscoverCategoryItem) {
        refreshInternal(category, forceRefresh = true)
    }

    /**
     * 切换到新的分类。
     *
     * 如果本地已有缓存，会先立刻展示缓存内容，再决定是否继续刷新。
     *
     * @param category 当前选中的分类。
     */
    suspend fun selectCategory(category: DiscoverCategoryItem) {
        AppLogger.d("HomeDiscover", "selectCategory id=${category.id}, title=${category.title}")
        currentCategoryId = category.id
        val cachedItems = discoverCache[category.id]
        discoverOffset.set(cachedItems?.size ?: 0)
        uiState.update { state ->
            state.copy(
                discoverItems = cachedItems ?: HomeSkeletonFactory.discover(),
                isDiscoverRefreshing = cachedItems == null,
                isDiscoverLoadingMore = false,
                discoverHasMore = true,
                discoverErrorMessage = null
            )
        }
        refreshInternal(category, forceRefresh = false)
    }

    /**
     * 读取某个分类的缓存数据。
     *
     * @param categoryId 分类 id。
     * @return 已缓存列表；没有缓存时返回 `null`。
     */
    fun peekItems(categoryId: String): List<HomeCardItem>? = discoverCache[categoryId]

    /**
     * 静默预取某个分类的第一页数据，只更新缓存，不影响当前 UI 状态。
     *
     * 用于横向切分类时，在用户手指滑向目标分类的途中提前准备数据，
     * 让用户滑过去时直接看到真实内容，而不是骨架屏。
     *
     * @param category 需要预取的目标分类。
     */
    suspend fun prefetchFirstPage(category: DiscoverCategoryItem) {
        // 已有缓存则无需再拉。
        if (discoverCache.containsKey(category.id)) return
        // 同一分类正在预取中则跳过，避免并发请求。
        if (!prefetchingIds.add(category.id)) return
        try {
            AppLogger.d("HomeDiscover", "prefetchFirstPage id=${category.id}")
            val result = loadHomeDiscoverItems(
                LoadHomeDiscoverItemsParams(category = category, forceRefresh = false)
            )
            // 只有当缓存仍为空时才写入，避免覆盖 refreshInternal 已写入的新鲜数据。
            if (result is Resource.Success && !discoverCache.containsKey(category.id)) {
                discoverCache[category.id] = result.data
            }
        } finally {
            prefetchingIds.remove(category.id)
        }
    }

    /**
     * 加载指定分类的下一页内容。
     *
     * @param category 当前分页所属分类。
     */
    suspend fun loadMore(category: DiscoverCategoryItem) {
        currentCategoryId = category.id
        val state = uiState.value
        if (state.isDiscoverRefreshing || state.isDiscoverLoadingMore || !state.discoverHasMore) {
            return
        }
        if (state.discoverItems.any(HomeCardItem::isSkeleton)) return

        val requestVersion = discoverRequestVersion.get()
        val offset = discoverOffset.get()
        uiState.update { it.copy(isDiscoverLoadingMore = true, discoverErrorMessage = null) }

        when (
            val result = loadHomeDiscoverItems(
                LoadHomeDiscoverItemsParams(
                    category = category,
                    offset = offset,
                    forceRefresh = false
                )
            )
        ) {
            is Resource.Success -> {
                if (requestVersion != discoverRequestVersion.get()) return
                val items = result.data
                if (!discoverOffset.compareAndSet(offset, offset + items.size)) return
                uiState.update { current ->
                    val mergedItems = current.discoverItems + items
                    discoverCache[category.id] = mergedItems
                    current.copy(
                        discoverItems = mergedItems,
                        isDiscoverLoadingMore = false,
                        discoverHasMore = items.isNotEmpty(),
                        discoverErrorMessage = null
                    )
                }
            }

            is Resource.Error -> {
                if (requestVersion != discoverRequestVersion.get()) return
                val message = result.throwable.toUserMessage("Failed to load discover feed")
                uiState.update { it.copy(isDiscoverLoadingMore = false, discoverErrorMessage = message) }
                events.tryEmit(HomeUiEvent.ShowMessage(message))
            }

            is Resource.Loading -> Unit
        }
    }

    /**
     * 执行一次分类级刷新。
     *
     * @param category 需要刷新的分类。
     * @param forceRefresh 是否强制刷新底层数据源。
     */
    private suspend fun refreshInternal(category: DiscoverCategoryItem, forceRefresh: Boolean) {
        AppLogger.d(
            "HomeDiscover",
            "refreshInternal id=${category.id}, forceRefresh=$forceRefresh, cached=${discoverCache[category.id]?.size ?: 0}"
        )
        currentCategoryId = category.id
        discoverOffset.set(0)
        val requestVersion = discoverRequestVersion.incrementAndGet()
        uiState.update {
            it.copy(
                isDiscoverRefreshing = true,
                isDiscoverLoadingMore = false,
                discoverHasMore = true,
                discoverErrorMessage = null,
                discoverItems = if (it.discoverItems.isEmpty()) {
                    HomeSkeletonFactory.discover()
                } else {
                    it.discoverItems
                }
            )
        }

        when (
            val result = loadHomeDiscoverItems(
                LoadHomeDiscoverItemsParams(category = category, forceRefresh = forceRefresh)
            )
        ) {
            is Resource.Success -> {
                if (requestVersion != discoverRequestVersion.get()) return
                val items = result.data
                AppLogger.d("HomeDiscover", "refresh success id=${category.id}, itemCount=${items.size}")
                discoverCache[category.id] = items
                discoverOffset.set(items.size)
                uiState.update { state ->
                    state.copy(
                        discoverItems = items,
                        isDiscoverRefreshing = false,
                        discoverHasMore = items.isNotEmpty(),
                        discoverErrorMessage = null
                    )
                }
            }

            is Resource.Error -> {
                if (requestVersion != discoverRequestVersion.get()) return
                val message = result.throwable.toUserMessage("Failed to load discover feed")
                AppLogger.e("HomeDiscover", "refresh error id=${category.id}: $message", result.throwable)
                val cachedItems = discoverCache[category.id]
                    ?: uiState.value.discoverItems.filterNot(HomeCardItem::isSkeleton)
                uiState.update { state ->
                    state.copy(
                        discoverItems = cachedItems,
                        isDiscoverRefreshing = false,
                        discoverHasMore = cachedItems.isNotEmpty(),
                        discoverErrorMessage = message
                    )
                }
                events.tryEmit(HomeUiEvent.ShowMessage(message))
            }

            is Resource.Loading -> Unit
        }
    }
}
