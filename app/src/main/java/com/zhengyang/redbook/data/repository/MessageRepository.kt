package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.ui.message.MessageRowItem
import com.zhengyang.redbook.ui.message.PersonSuggestionItem

interface MessageRepository {
    suspend fun getMessageRows(): List<MessageRowItem>
    suspend fun getPeopleSuggestions(): List<PersonSuggestionItem>
}
