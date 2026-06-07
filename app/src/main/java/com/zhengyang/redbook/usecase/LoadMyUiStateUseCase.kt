/**
 * 文件说明： LoadMyUiStateUseCase.kt
 * 作用： 封装单一业务动作，为界面层提供清晰的业务入口。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
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
