package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.repository.HomeRepository
import javax.inject.Inject

class FollowHomeUserUseCase @Inject constructor(
    private val homeRepository: HomeRepository
) : ResourceUseCase<String, Unit>() {

    override suspend fun execute(input: String) {
        homeRepository.followUser(input)
    }
}
