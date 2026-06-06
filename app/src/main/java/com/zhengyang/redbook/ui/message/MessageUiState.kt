/**
 * 文件说明： MessageUiState.kt
 * 作用： 承载消息页相关的界面状态与交互逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.message

data class MessageUiState(
    val messageRows: List<MessageRowItem> = emptyList(),
    val peopleSuggestions: List<PersonSuggestionItem> = emptyList()
)

data class MessageRowItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val timeText: String,
    val backgroundRes: Int,
    val iconRes: Int? = null,
    val avatarText: String? = null,
    val iconSizeDp: Int = 20,
    val showsVerifiedBadge: Boolean = false,
    val showsRedDot: Boolean = false
)

data class PersonSuggestionItem(
    val id: String,
    val avatarText: String,
    val name: String,
    val subtitle: String,
    val avatarBackgroundRes: Int
)
