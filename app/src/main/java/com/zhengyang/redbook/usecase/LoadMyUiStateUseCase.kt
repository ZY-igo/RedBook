/**
 * 文件说明：LoadMyUiStateUseCase.kt
 * 作用：封装 Load My Ui State Use Case 相关业务动作与场景编排逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.repository.MyRepository
import com.zhengyang.redbook.ui.my.MyUiState
import javax.inject.Inject

class LoadMyUiStateUseCase @Inject constructor(
    private val myRepository: MyRepository
) {
    suspend operator fun invoke(): MyUiState {
        return MyUiState(
            profile = myRepository.getProfile(),
            stats = myRepository.getProfileStats(),
            interestPeople = myRepository.getInterestPeople(),
            isInterestSectionVisible = true
        )
    }
}
