/**
 * 文件说明：LoadMessageUiStateUseCase.kt
 * 作用：封装 Load Message Ui State Use Case 相关业务动作与场景编排逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
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
