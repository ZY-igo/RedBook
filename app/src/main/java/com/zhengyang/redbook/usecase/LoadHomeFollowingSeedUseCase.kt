/**
 * 文件说明：LoadHomeFollowingSeedUseCase.kt
 * 作用：封装 Load Home Following Seed Use Case 相关业务动作与场景编排逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.mapper.HomeMapper
import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.ui.home.FollowingUserItem
import com.zhengyang.redbook.ui.home.HomeCardItem
import javax.inject.Inject

/**
 * 首页关注种子数据加载用例
 *
 * 用于一次性准备关注页首屏所需的推荐用户和内容流，
 * 便于界面在初始化时同步建立完整状态。
 */
class LoadHomeFollowingSeedUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
    private val homeMapper: HomeMapper,
    private val displayPolicy: HomeDiscoverItemDisplayPolicy
) {

    /**
     * 加载关注页首屏种子数据。
     *
     * @param offset 偏移量
     * @param limit 单次请求数量
     * @return 成功时返回推荐用户和关注流内容组合，失败时返回异常
     */
    suspend operator fun invoke(
        offset: Int = 0,
        limit: Int = DEFAULT_PAGE_SIZE
    ): Result<HomeFollowingSeed> {
        return runCatching {
            HomeFollowingSeed(
                suggestedUsers = homeRepository.getSuggestedFollowingUsers()
                    .map(homeMapper::toFollowingUserUiModel),
                followingFeedItems = homeRepository.getFollowingFeedItemsPage(offset = offset, limit = limit)
                    .filter(displayPolicy::isDisplayable)
                    .map(homeMapper::toHomeCardUiModel)
            )
        }
    }

    companion object {
        /** 关注页默认分页大小。 */
        const val DEFAULT_PAGE_SIZE = 8
    }
}

/**
 * 关注页首屏种子数据聚合结果。
 *
 * 将推荐用户列表和关注流内容放在同一个结构中返回，
 * 方便 ViewModel 一次性落到 UI 状态。
 */
data class HomeFollowingSeed(
    val suggestedUsers: List<FollowingUserItem>,
    val followingFeedItems: List<HomeCardItem>
)
