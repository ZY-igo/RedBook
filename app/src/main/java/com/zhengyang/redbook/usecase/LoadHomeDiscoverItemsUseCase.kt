/**
 * 文件说明： LoadHomeDiscoverItemsUseCase.kt
 * 作用： 封装单一业务动作，为界面层提供清晰的业务入口。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
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
