package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.repository.MessageRepository
import com.zhengyang.redbook.ui.message.MessageUiState
import javax.inject.Inject

class LoadMessageUiStateUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(): MessageUiState {
        return MessageUiState(
            messageRows = messageRepository.getMessageRows(),
            peopleSuggestions = messageRepository.getPeopleSuggestions()
        )
    }
}
