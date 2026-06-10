package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.repository.MessageRepository
import com.zhengyang.redbook.ui.message.MessageUiState
import javax.inject.Inject

/**
 * 加载消息页状态时使用的参数。
 *
 * @property forceRefresh 是否强制刷新消息数据。
 */
data class LoadMessageUiStateParams(
    val forceRefresh: Boolean = false
)

/**
 * 加载消息页 UI 状态的用例。
 *
 * @property messageRepository 消息模块仓库。
 */
class LoadMessageUiStateUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) : SuspendUseCase<LoadMessageUiStateParams, MessageUiState>() {

    /**
     * 组装消息页 UI 状态。
     *
     * @param input 加载参数。
     * @return 完整消息页状态。
     */
    override suspend fun execute(input: LoadMessageUiStateParams): MessageUiState {
        val messageRows = messageRepository.getMessageRows(forceRefresh = input.forceRefresh)
        return MessageUiState(
            messageRows = messageRows,
            peopleSuggestions = messageRepository.getPeopleSuggestions(forceRefresh = false)
        )
    }
}
