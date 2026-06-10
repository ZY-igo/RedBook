package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.repository.HomeRepository
import javax.inject.Inject

/**
 * 首页关注用户的用例。
 *
 * 输入参数直接使用目标用户 id。
 *
 * @property homeRepository 首页数据仓库。
 */
class FollowHomeUserUseCase @Inject constructor(
    private val homeRepository: HomeRepository
) : ResourceUseCase<String, Unit>() {

    /**
     * 关注指定用户。
     *
     * @param input 目标用户 id。
     */
    override suspend fun execute(input: String) {
        homeRepository.followUser(input)
    }
}
