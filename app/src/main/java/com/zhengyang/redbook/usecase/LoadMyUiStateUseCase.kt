package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.repository.MyRepository
import com.zhengyang.redbook.ui.my.MyUiState
import javax.inject.Inject

class LoadMyUiStateUseCase @Inject constructor(
    private val myRepository: MyRepository
) {
    suspend operator fun invoke(): MyUiState {
        return MyUiState(
            stats = myRepository.getProfileStats(),
            interestPeople = myRepository.getInterestPeople(),
            isInterestSectionVisible = true
        )
    }
}
