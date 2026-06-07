package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.repository.HomeRepository
import javax.inject.Inject

class FollowHomeUserUseCase @Inject constructor(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke(userId: String): Result<Unit> {
        return runCatching { homeRepository.followUser(userId) }
    }
}
