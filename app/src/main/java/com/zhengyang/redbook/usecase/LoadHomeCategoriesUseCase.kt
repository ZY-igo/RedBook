/**
 * 文件说明：LoadHomeCategoriesUseCase.kt
 * 作用：封装 Load Home Categories Use Case 相关业务动作与场景编排逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.mapper.HomeMapper
import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.ui.home.DiscoverCategoryItem
import javax.inject.Inject

/**
 * 首页分类加载用例
 *
 * 从仓储读取业务层分类数据后映射为界面模型，
 * 并通过 [Result] 对上层暴露成功或失败结果。
 */
class LoadHomeCategoriesUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
    private val homeMapper: HomeMapper
) {

    /**
     * 加载首页分类列表。
     *
     * @return 成功时返回分类 UI 模型列表，失败时返回异常
     */
    suspend operator fun invoke(): Result<List<DiscoverCategoryItem>> {
        return runCatching {
            homeRepository.getCategories()
                .map(homeMapper::toCategoryUiModel)
        }
    }
}
