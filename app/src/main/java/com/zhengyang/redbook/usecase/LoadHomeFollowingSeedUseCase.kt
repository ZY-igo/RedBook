/**
 * 文件说明： LoadHomeFollowingSeedUseCase.kt
 * 作用： 封装单一业务动作，为界面层提供清晰的业务入口。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.mapper.HomeMapper
import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.ui.home.FollowingUserItem
import com.zhengyang.redbook.ui.home.HomeCardItem
import javax.inject.Inject

class LoadHomeFollowingSeedUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
    private val homeMapper: HomeMapper,
    private val displayPolicy: HomeDiscoverItemDisplayPolicy
) {
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
        const val DEFAULT_PAGE_SIZE = 8
    }
}

data class HomeFollowingSeed(
    val suggestedUsers: List<FollowingUserItem>,
    val followingFeedItems: List<HomeCardItem>
)
