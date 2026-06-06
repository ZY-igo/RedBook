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
