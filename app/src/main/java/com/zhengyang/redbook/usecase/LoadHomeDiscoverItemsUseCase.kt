package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.mapper.HomeMapper
import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.ui.home.DiscoverCategoryItem
import com.zhengyang.redbook.ui.home.HomeCardItem
import javax.inject.Inject

/**
 * 加载首页发现流时使用的参数。
 *
 * @property category 当前需要加载的分类。
 * @property offset 分页偏移量。
 * @property limit 单次加载条数。
 * @property forceRefresh 是否强制刷新底层数据源。
 */
data class LoadHomeDiscoverItemsParams(
    val category: DiscoverCategoryItem,
    val offset: Int = 0,
    val limit: Int = LoadHomeDiscoverItemsUseCase.DEFAULT_PAGE_SIZE,
    val forceRefresh: Boolean = false
)

/**
 * 加载首页发现流的用例。
 *
 * 负责拉取仓库数据、过滤不可展示内容，并映射成首页卡片 UI 模型。
 *
 * @property homeRepository 首页仓库。
 * @property homeMapper 首页映射器。
 * @property displayPolicy 发现流展示策略。
 */
class LoadHomeDiscoverItemsUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
    private val homeMapper: HomeMapper,
    private val displayPolicy: HomeDiscoverItemDisplayPolicy
) : ResourceUseCase<LoadHomeDiscoverItemsParams, List<HomeCardItem>>() {

    /**
     * 加载并转换发现流数据。
     *
     * @param input 加载参数。
     * @return 可用于界面展示的首页卡片列表。
     */
    override suspend fun execute(input: LoadHomeDiscoverItemsParams): List<HomeCardItem> {
        return homeRepository.getDiscoverItemsPage(
            categoryId = input.category.id,
            offset = input.offset,
            limit = input.limit,
            forceRefresh = input.forceRefresh
        )
            .filter(displayPolicy::isDisplayable)
            .map(homeMapper::toHomeCardUiModel)
    }

    companion object {
        /**
         * 发现流默认分页大小。
         */
        const val DEFAULT_PAGE_SIZE = 10
    }
}
