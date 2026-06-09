package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.mapper.HomeMapper
import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.ui.home.FollowingUserItem
import com.zhengyang.redbook.ui.home.HomeCardItem
import javax.inject.Inject

data class LoadHomeFollowingSeedParams(
    val offset: Int = 0,
    val limit: Int = LoadHomeFollowingSeedUseCase.DEFAULT_PAGE_SIZE
)

class LoadHomeFollowingSeedUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
    private val homeMapper: HomeMapper,
    private val displayPolicy: HomeDiscoverItemDisplayPolicy
) : ResourceUseCase<LoadHomeFollowingSeedParams, HomeFollowingSeed>() {

    override suspend fun execute(input: LoadHomeFollowingSeedParams): HomeFollowingSeed {
        return HomeFollowingSeed(
            suggestedUsers = homeRepository.getSuggestedFollowingUsers()
                .map(homeMapper::toFollowingUserUiModel),
            followingFeedItems = homeRepository.getFollowingFeedItemsPage(
                offset = input.offset,
                limit = input.limit
            )
                .filter(displayPolicy::isDisplayable)
                .map(homeMapper::toHomeCardUiModel)
        )
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 8
    }
}

data class HomeFollowingSeed(
    val suggestedUsers: List<FollowingUserItem>,
    val followingFeedItems: List<HomeCardItem>
)
