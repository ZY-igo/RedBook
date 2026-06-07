package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.RemoteResourceMapper
import com.zhengyang.redbook.data.remote.requireData
import com.zhengyang.redbook.data.remote.model.RemoteMyInterestPersonDto
import com.zhengyang.redbook.data.remote.model.RemoteMyProfileDto
import com.zhengyang.redbook.ui.my.InterestPersonItem
import com.zhengyang.redbook.ui.my.MyProfileHeader
import com.zhengyang.redbook.ui.my.MyProfileStats
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MyRepositoryImpl @Inject constructor(
    private val apiService: RedBookApiService,
    private val resourceMapper: RemoteResourceMapper
) : MyRepository {

    private val profileMutex = Mutex()
    private var cachedProfile: RemoteMyProfileDto? = null
    private var cachedProfileAt: Long = 0L

    override suspend fun getProfile(): MyProfileHeader = withContext(Dispatchers.IO) {
        loadProfile().toHeader()
    }

    override suspend fun getProfileStats(): MyProfileStats = withContext(Dispatchers.IO) {
        loadProfile().toStats()
    }

    override suspend fun getInterestPeople(): List<InterestPersonItem> = withContext(Dispatchers.IO) {
        apiService.getMyInterests().requireData().map { it.toUiModel() }
    }

    private suspend fun loadProfile(): RemoteMyProfileDto {
        val now = System.currentTimeMillis()
        cachedProfile?.takeIf { now - cachedProfileAt < CACHE_TTL_MS }?.let { return it }
        return profileMutex.withLock {
            val current = System.currentTimeMillis()
            cachedProfile?.takeIf { current - cachedProfileAt < CACHE_TTL_MS }?.let { return@withLock it }
            apiService.getMyProfile().requireData().also {
                cachedProfile = it
                cachedProfileAt = current
            }
        }
    }

    private fun RemoteMyProfileDto.toHeader(): MyProfileHeader {
        return MyProfileHeader(
            id = id,
            name = name,
            avatarText = avatarText,
            avatarColorHex = avatarColorHex,
            bio = bio
        )
    }

    private fun RemoteMyProfileDto.toStats(): MyProfileStats {
        return MyProfileStats(
            followingCount = followingCount.toString(),
            fansCount = fansCount.toString(),
            likesCount = likesCount.toString()
        )
    }

    private fun RemoteMyInterestPersonDto.toUiModel(): InterestPersonItem {
        return InterestPersonItem(
            id = id,
            avatarText = avatarText,
            name = name,
            fansText = fansText,
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
