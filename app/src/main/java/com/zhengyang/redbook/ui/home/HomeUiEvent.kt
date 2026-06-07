/**
 * 文件说明：HomeUiEvent.kt
 * 作用：定义首页模块中的一次性界面事件。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

/**
 * 首页 UI 事件
 *
 * 用于表达首页页面的单次消费事件，
 * 避免将短暂提示信息直接放入持久化状态中。
 */
sealed interface HomeUiEvent {

    /**
     * 请求界面展示一条提示消息。
     *
     * @param message 需要展示给用户的文案。
     */
    data class ShowMessage(val message: String) : HomeUiEvent
}
