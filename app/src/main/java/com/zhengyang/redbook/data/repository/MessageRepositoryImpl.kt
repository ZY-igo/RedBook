package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.R
import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.RemoteResourceMapper
import com.zhengyang.redbook.data.remote.requireData
import com.zhengyang.redbook.data.remote.model.RemoteMessageOverviewDto
import com.zhengyang.redbook.data.remote.model.RemoteMessageRowDto
import com.zhengyang.redbook.data.remote.model.RemotePersonSuggestionDto
import com.zhengyang.redbook.ui.message.MessageRowItem
import com.zhengyang.redbook.ui.message.PersonSuggestionItem
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MessageRepositoryImpl @Inject constructor(
    private val apiService: RedBookApiService,
    private val resourceMapper: RemoteResourceMapper
) : MessageRepository {

    private val overviewMutex = Mutex()
    private var cachedOverview: RemoteMessageOverviewDto? = null
    private var cachedAt: Long = 0L

    override suspend fun getMessageRows(): List<MessageRowItem> = withContext(Dispatchers.IO) {
        loadOverview().messageRows.map { it.toUiModel() }
    }

    override suspend fun getPeopleSuggestions(): List<PersonSuggestionItem> = withContext(Dispatchers.IO) {
        loadOverview().peopleSuggestions.map { it.toUiModel() }
    }

    private suspend fun loadOverview(): RemoteMessageOverviewDto {
        val now = System.currentTimeMillis()
        cachedOverview?.takeIf { now - cachedAt < CACHE_TTL_MS }?.let { return it }
        return overviewMutex.withLock {
            val current = System.currentTimeMillis()
            cachedOverview?.takeIf { current - cachedAt < CACHE_TTL_MS }?.let { return@withLock it }
            apiService.getMessageOverview().requireData().also {
                cachedOverview = it
                cachedAt = current
            }
        }
    }

    private fun RemoteMessageRowDto.toUiModel(): MessageRowItem {
        return MessageRowItem(
            id = id,
            title = title,
            subtitle = subtitle,
            timeText = timeText,
            backgroundRes = resourceMapper.drawableByName(
                name = backgroundKey,
                fallback = resourceMapper.defaultMessageAvatarBackground()
            ),
            iconRes = iconKey?.let {
                resourceMapper.drawableByName(it, resourceMapper.defaultMessageIcon())
            },
            avatarText = avatarText?.takeIf { it.isNotBlank() },
            iconSizeDp = iconSizeDp,
            showsVerifiedBadge = showsVerifiedBadge,
            showsRedDot = showsRedDot
        )
    }

    private fun RemotePersonSuggestionDto.toUiModel(): PersonSuggestionItem {
        return PersonSuggestionItem(
            id = id,
            avatarText = avatarText,
            name = name,
            subtitle = subtitle,
            avatarBackgroundRes = resourceMapper.drawableByName(
                name = avatarBackgroundKey,
                fallback = resourceMapper.defaultSuggestionAvatarBackground()
            )
        )
    }

    private companion object {
        const val CACHE_TTL_MS = 1_000L
    }
}
