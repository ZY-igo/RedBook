package com.zhengyang.redbook.ui.home

/**
 * 首页一次性 UI 事件。
 *
 * 这类事件只消费一次，不适合放在持久状态里。
 */
sealed interface HomeUiEvent {

    /**
     * 请求界面展示一条提示消息。
     *
     * @property message 需要展示给用户的文本。
     */
    data class ShowMessage(val message: String) : HomeUiEvent
}
