package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.mapper.HomeMapper
import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.ui.home.DiscoverCategoryItem
import com.zhengyang.redbook.ui.home.HomeCardItem
import javax.inject.Inject

data class LoadHomeDiscoverItemsParams(
    val category: DiscoverCategoryItem,
    val offset: Int = 0,
    val limit: Int = LoadHomeDiscoverItemsUseCase.DEFAULT_PAGE_SIZE
)

class LoadHomeDiscoverItemsUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
    private val homeMapper: HomeMapper,
    private val displayPolicy: HomeDiscoverItemDisplayPolicy
) : ResourceUseCase<LoadHomeDiscoverItemsParams, List<HomeCardItem>>() {

    override suspend fun execute(input: LoadHomeDiscoverItemsParams): List<HomeCardItem> {
        return homeRepository.getDiscoverItemsPage(
            categoryId = input.category.id,
            offset = input.offset,
            limit = input.limit
        )
            .filter(displayPolicy::isDisplayable)
            .map(homeMapper::toHomeCardUiModel)
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 10
    }
}
