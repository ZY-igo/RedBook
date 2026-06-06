/**
 * 文件说明： MessageRepositoryImpl.kt
 * 作用： 协调不同数据源，并向上层提供统一的仓储实现。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.local.ListDao
import com.zhengyang.redbook.data.local.LocalSeedInitializer
import com.zhengyang.redbook.data.local.AssetSeedDataSource
import com.zhengyang.redbook.data.model.MessageRowEntity
import com.zhengyang.redbook.data.model.PersonSuggestionEntity
import com.zhengyang.redbook.ui.message.MessageRowItem
import com.zhengyang.redbook.ui.message.PersonSuggestionItem
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val listDao: ListDao,
    private val localSeedInitializer: LocalSeedInitializer,
    private val assetSeedDataSource: AssetSeedDataSource
) : MessageRepository {
    override suspend fun getMessageRows(): List<MessageRowItem> {
        localSeedInitializer.ensureSeeded()
        return listDao.getMessageRows().map { it.toMessageRowItem() }
    }

    override suspend fun getPeopleSuggestions(): List<PersonSuggestionItem> {
        return assetSeedDataSource.load().personSuggestions.map { it.toPersonSuggestionItem() }
    }

    private fun MessageRowEntity.toMessageRowItem(): MessageRowItem {
        return MessageRowItem(
            id = id,
            title = title,
            subtitle = subtitle,
            timeText = timeText,
            backgroundRes = backgroundRes,
            iconRes = iconRes,
            avatarText = avatarText,
            iconSizeDp = iconSizeDp,
            showsVerifiedBadge = showsVerifiedBadge,
            showsRedDot = showsRedDot
        )
    }

    private fun PersonSuggestionEntity.toPersonSuggestionItem(): PersonSuggestionItem {
        return PersonSuggestionItem(
            id = id,
            avatarText = avatarText,
            name = name,
            subtitle = subtitle,
            avatarBackgroundRes = avatarBackgroundRes
        )
    }
}
