package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.mapper.HomeMapper
import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.ui.home.DiscoverCategoryItem
import javax.inject.Inject

/**
 * 加载首页分类列表的用例。
 *
 * @property homeRepository 首页仓库。
 * @property homeMapper 首页数据映射器。
 */
class LoadHomeCategoriesUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
    private val homeMapper: HomeMapper
) : ResourceUseCase<NoParams, List<DiscoverCategoryItem>>() {

    /**
     * 拉取分类列表并映射成 UI 模型。
     *
     * @param input 无参数占位对象。
     * @return 首页分类 UI 列表。
     */
    override suspend fun execute(input: NoParams): List<DiscoverCategoryItem> {
        return homeRepository.getCategories()
            .map(homeMapper::toCategoryUiModel)
    }
}
