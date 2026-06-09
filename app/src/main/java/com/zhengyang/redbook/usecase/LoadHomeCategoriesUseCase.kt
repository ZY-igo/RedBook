package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.mapper.HomeMapper
import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.ui.home.DiscoverCategoryItem
import javax.inject.Inject

class LoadHomeCategoriesUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
    private val homeMapper: HomeMapper
) : ResourceUseCase<NoParams, List<DiscoverCategoryItem>>() {

    override suspend fun execute(input: NoParams): List<DiscoverCategoryItem> {
        return homeRepository.getCategories()
            .map(homeMapper::toCategoryUiModel)
    }
}
