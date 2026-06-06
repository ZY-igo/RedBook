package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.ui.home.DiscoverCategoryItem
import javax.inject.Inject

class LoadHomeCategoriesUseCase @Inject constructor(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke(): List<DiscoverCategoryItem> {
        return homeRepository.getCategories()
    }
}
