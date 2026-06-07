/**
 * 文件说明：MessageUiState.kt
 * 作用：定义 Message Ui State 场景使用的界面状态模型。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
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
