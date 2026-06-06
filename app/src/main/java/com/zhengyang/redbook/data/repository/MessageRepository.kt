/**
 * 文件说明： MessageRepository.kt
 * 作用： 协调不同数据源，并向上层提供统一的仓储实现。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.ui.message.MessageRowItem
import com.zhengyang.redbook.ui.message.PersonSuggestionItem

interface MessageRepository {
    suspend fun getMessageRows(): List<MessageRowItem>
    suspend fun getPeopleSuggestions(): List<PersonSuggestionItem>
}
