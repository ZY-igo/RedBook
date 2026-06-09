/**
 * 文件说明：MyRepositoryImpl.kt
 * 作用：实现个人页场景仓储接口，负责个人资料远端数据获取与短时缓存。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 个人页仓储实现类
 *
 * 负责加载个人资料和感兴趣的人相关接口，并将远端 DTO 映射为页面展示模型。
 * 为避免短时间重复拉取个人资料，仓储内部维护了一个简单的内存缓存。
 */
class MyRepositoryImpl @Inject constructor(
    /** 个人页相关远端接口服务。 */
    private val apiService: RedBookApiService,

    /** 远端资源键到本地资源 ID 的映射器。 */
    private val resourceMapper: RemoteResourceMapper
) : MyRepository {

    /** 保护个人资料缓存并发读写的互斥锁。 */
    private val profileMutex = Mutex()

    /** 最近一次加载成功的个人资料缓存。 */
    private var cachedProfile: RemoteMyProfileDto? = null

    /** 最近一次缓存写入时间戳。 */
    private var cachedProfileAt: Long = 0L

    /**
     * 获取个人页头部资料。
     *
     * @return 个人页头部展示模型。
     */
    override suspend fun getProfile(): MyProfileHeader = withContext(Dispatchers.IO) {
        loadProfile().toHeader()
    }

    /**
     * 获取个人页统计信息。
     *
     * @return 个人页统计展示模型。
     */
    override suspend fun getProfileStats(): MyProfileStats = withContext(Dispatchers.IO) {
        loadProfile().toStats()
    }

    /**
     * 获取感兴趣的人列表。
     *
     * @return 感兴趣的人展示模型列表。
     */
    override suspend fun getInterestPeople(): List<InterestPersonItem> = withContext(Dispatchers.IO) {
        apiService.getMyInterests().requireData().map { it.toUiModel() }
    }

    override suspend fun uploadAvatar(
        fileName: String,
        contentType: String,
        bytes: ByteArray
    ): MyProfileHeader = withContext(Dispatchers.IO) {
        val requestBody = bytes.toRequestBody(contentType.toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", fileName, requestBody)
        apiService.uploadMyAvatar(filePart).requireData().also {
            cachedProfile = it
            cachedProfileAt = System.currentTimeMillis()
        }.toHeader()
    }

    /**
     * 加载个人资料。
     *
     * 优先命中短时缓存，缓存失效后再发起远端请求，并通过互斥锁避免重复加载。
     *
     * @return 个人资料远端 DTO。
     */
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

    /**
     * 将远端个人资料 DTO 转换为头部展示模型。
     *
     * @return 个人页头部展示模型。
     */
    private fun RemoteMyProfileDto.toHeader(): MyProfileHeader {
        return MyProfileHeader(
            id = id,
            name = name,
            avatarUrl = avatarUrl,
            avatarText = avatarText,
            avatarColorHex = avatarColorHex,
            bio = bio
        )
    }

    /**
     * 将远端个人资料 DTO 转换为统计展示模型。
     *
     * @return 个人页统计展示模型。
     */
    private fun RemoteMyProfileDto.toStats(): MyProfileStats {
        return MyProfileStats(
            followingCount = followingCount.toString(),
            fansCount = fansCount.toString(),
            likesCount = likesCount.toString()
        )
    }

    /**
     * 将远端感兴趣的人 DTO 转换为页面模型。
     *
     * @return 感兴趣的人展示模型。
     */
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
        /** 个人资料缓存有效期，单位为毫秒。 */
        const val CACHE_TTL_MS = 1_000L
    }
}
