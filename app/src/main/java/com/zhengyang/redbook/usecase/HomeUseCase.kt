package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.model.NoteItem
import com.zhengyang.redbook.data.repository.HomeRepository

class HomeUseCase(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke(): List<NoteItem> = homeRepository.getListContent()
}
