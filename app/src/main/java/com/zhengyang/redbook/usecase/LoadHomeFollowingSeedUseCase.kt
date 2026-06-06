/**
 * 文件说明： LoadHomeFollowingSeedUseCase.kt
 * 作用： 封装单一业务动作，为界面层提供清晰的业务入口。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.ui.home.FollowingUserItem
import com.zhengyang.redbook.ui.home.HomeCardItem
import javax.inject.Inject

class LoadHomeFollowingSeedUseCase @Inject constructor(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke(): HomeFollowingSeed {
        return HomeFollowingSeed(
            suggestedUsers = homeRepository.getSuggestedFollowingUsers(),
            followingFeedItems = homeRepository.getFollowingFeedItems()
        )
    }
}

data class HomeFollowingSeed(
    val suggestedUsers: List<FollowingUserItem>,
    val followingFeedItems: List<HomeCardItem>
)
