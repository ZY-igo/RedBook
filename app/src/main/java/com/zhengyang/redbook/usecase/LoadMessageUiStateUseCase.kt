/**
 * 文件说明： LoadMessageUiStateUseCase.kt
 * 作用： 封装单一业务动作，为界面层提供清晰的业务入口。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
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
