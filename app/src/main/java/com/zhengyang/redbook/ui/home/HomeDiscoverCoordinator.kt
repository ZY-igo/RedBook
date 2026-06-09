package com.zhengyang.redbook.ui.home

import com.zhengyang.redbook.core.common.Resource
import com.zhengyang.redbook.core.common.toUserMessage
import com.zhengyang.redbook.usecase.LoadHomeDiscoverItemsParams
import com.zhengyang.redbook.usecase.LoadHomeDiscoverItemsUseCase
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class HomeDiscoverCoordinator(
    private val uiState: MutableStateFlow<HomeUiState>,
    private val events: MutableSharedFlow<HomeUiEvent>,
    private val loadHomeDiscoverItems: LoadHomeDiscoverItemsUseCase
) {
    private val discoverOffset = AtomicInteger(0)
    private val discoverRequestVersion = AtomicInteger(0)
    private val discoverCache = linkedMapOf<String, List<HomeCardItem>>()

    var currentCategoryId: String? = null
        private set

    suspend fun refresh(category: DiscoverCategoryItem) {
        currentCategoryId = category.id
        discoverOffset.set(0)
        val requestVersion = discoverRequestVersion.incrementAndGet()
        uiState.update {
            it.copy(
                isDiscoverRefreshing = true,
                isDiscoverLoadingMore = false,
                discoverHasMore = true,
                discoverErrorMessage = null,
                discoverItems = if (it.discoverItems.isEmpty()) HomeSkeletonFactory.discover() else it.discoverItems
            )
        }

        when (val result = loadHomeDiscoverItems(LoadHomeDiscoverItemsParams(category = category))) {
            is Resource.Success -> {
                if (requestVersion != discoverRequestVersion.get()) return
                val items = result.data
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

    suspend fun selectCategory(category: DiscoverCategoryItem) {
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
        refresh(category)
    }

    fun peekItems(categoryId: String): List<HomeCardItem>? = discoverCache[categoryId]

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
                LoadHomeDiscoverItemsParams(category = category, offset = offset)
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
}
