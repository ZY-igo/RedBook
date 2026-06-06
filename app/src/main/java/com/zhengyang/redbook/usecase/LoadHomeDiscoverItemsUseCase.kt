package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.ui.home.DiscoverCategoryItem
import com.zhengyang.redbook.ui.home.HomeCardItem
import javax.inject.Inject

class LoadHomeDiscoverItemsUseCase @Inject constructor(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke(category: DiscoverCategoryItem): List<HomeCardItem> {
        return homeRepository.getDiscoverItems(category)
    }
}
