/**
 * 文件说明：LoadHomeDiscoverItemsUseCase.kt
 * 作用：封装 Load Home Discover Items Use Case 相关业务动作与场景编排逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.mapper.HomeMapper
import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.ui.home.DiscoverCategoryItem
import com.zhengyang.redbook.ui.home.HomeCardItem
import javax.inject.Inject

/**
 * 首页发现流加载用例
 *
 * 根据当前分类和分页参数读取内容，
 * 并在映射为 UI 模型前应用可展示性过滤策略。
 */
class LoadHomeDiscoverItemsUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
    private val homeMapper: HomeMapper,
    private val displayPolicy: HomeDiscoverItemDisplayPolicy
) {

    /**
     * 加载某个分类下的发现流内容。
     *
     * @param category 当前选中的分类
     * @param offset 偏移量
     * @param limit 单次请求数量
     * @return 成功时返回首页卡片 UI 模型列表，失败时返回异常
     */
    suspend operator fun invoke(
        category: DiscoverCategoryItem,
        offset: Int = 0,
        limit: Int = DEFAULT_PAGE_SIZE
    ): Result<List<HomeCardItem>> {
        return runCatching {
            homeRepository.getDiscoverItemsPage(
                categoryId = category.id,
                offset = offset,
                limit = limit
            )
                .filter(displayPolicy::isDisplayable)
                .map(homeMapper::toHomeCardUiModel)
        }
    }

    companion object {
        /** 发现流默认分页大小。 */
        const val DEFAULT_PAGE_SIZE = 10
    }
}
