package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.mapper.HomeMapper
import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.ui.home.FollowingUserItem
import com.zhengyang.redbook.ui.home.HomeCardItem
import javax.inject.Inject

/**
 * 加载首页“关注”种子数据时使用的参数。
 *
 * @property offset 分页偏移量。
 * @property limit 单次加载数量。
 * @property forceRefresh 是否强制刷新推荐用户数据。
 */
data class LoadHomeFollowingSeedParams(
    val offset: Int = 0,
    val limit: Int = LoadHomeFollowingSeedUseCase.DEFAULT_PAGE_SIZE,
    val forceRefresh: Boolean = false
)

/**
 * 首页“关注”模块的种子结果。
 *
 * @property suggestedUsers 推荐关注用户列表。
 * @property followingFeedItems 关注 feed 卡片列表。
 */
data class HomeFollowingSeed(
    val suggestedUsers: List<FollowingUserItem>,
    val followingFeedItems: List<HomeCardItem>
)

/**
 * 加载首页“关注”模块种子数据的用例。
 *
 * 会同时返回推荐用户和关注流第一页或指定页的数据。
 *
 * @property homeRepository 首页仓库。
 * @property homeMapper 首页映射器。
 * @property displayPolicy 发现/关注流卡片展示策略。
 */
class LoadHomeFollowingSeedUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
    private val homeMapper: HomeMapper,
    private val displayPolicy: HomeDiscoverItemDisplayPolicy
) : ResourceUseCase<LoadHomeFollowingSeedParams, HomeFollowingSeed>() {

    /**
     * 加载并组装“关注”种子数据。
     *
     * @param input 加载参数。
     * @return 推荐用户和关注 feed 的组合结果。
     */
    override suspend fun execute(input: LoadHomeFollowingSeedParams): HomeFollowingSeed {
        val suggestedUsers = homeRepository.getSuggestedFollowingUsers(forceRefresh = input.forceRefresh)
            .map(homeMapper::toFollowingUserUiModel)
        return HomeFollowingSeed(
            suggestedUsers = suggestedUsers,
            followingFeedItems = homeRepository.getFollowingFeedItemsPage(
                offset = input.offset,
                limit = input.limit,
                forceRefresh = false
            )
                .filter(displayPolicy::isDisplayable)
                .map(homeMapper::toHomeCardUiModel)
        )
    }

    companion object {
        /**
         * “关注”流默认分页大小。
         */
        const val DEFAULT_PAGE_SIZE = 8
    }
}
