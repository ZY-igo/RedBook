/**
 * 文件说明：FollowHomeUserUseCase.kt
 * 作用：封装 Follow Home User Use Case 相关业务动作与场景编排逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
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
