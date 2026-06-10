/**
 * 文件说明：MyRepositoryImpl.kt
 * 作用："我的"模块数据仓库的实现类，负责协调网络请求和本地缓存。
 * 备注：实现缓存优先、多级降级的策略，并维护个人资料的内存缓存。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.local.ListDao
import com.zhengyang.redbook.data.model.InterestPersonEntity
import com.zhengyang.redbook.data.model.MyProfileEntity
import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.RemoteResourceMapper
import com.zhengyang.redbook.data.remote.model.RemoteMyInterestPersonDto
import com.zhengyang.redbook.data.remote.model.RemoteMyProfileDto
import com.zhengyang.redbook.data.remote.requireData
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
 * "我的"模块数据仓库实现类。
 *
 * 负责整合网络数据源（RedBookApiService）和本地缓存（ListDao），
 * 并维护个人资料的内存缓存以提升访问速度。
 *
 * 数据加载策略：
 * 1. 若非强制刷新，优先查询内存缓存。
 * 2. 内存缓存未命中时查询本地数据库。
 * 3. 本地也无缓存时请求网络数据。
 * 4. 网络请求失败时，降级到本地缓存。
 * 5. 成功获取网络数据后，更新内存缓存和本地数据库。
 *
 * @property listDao 本地数据库访问对象。
 * @property apiService 远端 API 服务对象。
 * @property resourceMapper 远端资源映射器，用于解析服务端下发的资源名称。
 */
class MyRepositoryImpl @Inject constructor(
    private val listDao: ListDao,
    private val apiService: RedBookApiService,
    private val resourceMapper: RemoteResourceMapper
) : MyRepository {

    /** 个人资料缓存的互斥锁，用于并发控制。 */
    private val profileMutex = Mutex()

    /** 个人资料的内存缓存。 */
    private var cachedProfile: RemoteMyProfileDto? = null

    /**
     * 获取个人资料头部信息。
     *
     * 内部委托给 loadProfile 方法，获取完整资料后转换为头部模型。
     *
     * @param forceRefresh 是否强制从网络刷新。
     * @return 个人资料头部数据。
     */
    override suspend fun getProfile(forceRefresh: Boolean): MyProfileHeader = withContext(Dispatchers.IO) {
        // 加载完整资料后转换为头部模型
        loadProfile(forceRefresh).toHeader()
    }

    /**
     * 获取个人数据统计。
     *
     * 内部委托给 loadProfile 方法，获取完整资料后提取统计数据。
     *
     * @param forceRefresh 是否强制从网络刷新。
     * @return 个人统计数据。
     */
    override suspend fun getProfileStats(forceRefresh: Boolean): MyProfileStats = withContext(Dispatchers.IO) {
        // 加载完整资料后提取统计数据
        loadProfile(forceRefresh).toStats()
    }

    /**
     * 获取推荐关注的人列表。
     *
     * 优先返回本地缓存，网络请求成功后将数据写入本地缓存。
     *
     * @param forceRefresh 是否强制从网络刷新。
     * @return 推荐关注的人列表。
     */
    override suspend fun getInterestPeople(forceRefresh: Boolean): List<InterestPersonItem> = withContext(Dispatchers.IO) {
        // 优先检查本地缓存
        if (!forceRefresh) {
            val cached = listDao.getInterestPeople()
            if (cached.isNotEmpty()) return@withContext cached.map { it.toUiModel() }
        }

        // 尝试从网络获取
        val remote = runCatching { apiService.getMyInterests().requireData() }.getOrElse { error ->
            // 网络失败，降级到本地缓存
            val cached = listDao.getInterestPeople()
            if (cached.isNotEmpty()) return@withContext cached.map { it.toUiModel() }
            throw error
        }

        // 成功获取网络数据，更新本地缓存并返回
        listDao.replaceInterestPeople(remote.mapIndexed { index, item -> item.toEntity(index) })
        remote.map { it.toUiModel() }
    }

    /**
     * 上传新头像。
     *
     * 使用 multipart 格式上传图片文件，成功后更新缓存和本地数据。
     *
     * @param fileName 上传文件的文件名。
     * @param contentType 上传文件的 MIME 类型。
     * @param bytes 上传文件的二进制内容。
     * @return 更新后的个人资料头部信息。
     */
    override suspend fun uploadAvatar(
        fileName: String,
        contentType: String,
        bytes: ByteArray
    ): MyProfileHeader = withContext(Dispatchers.IO) {
        // 将字节数组转换为 OkHttp 请求体
        val requestBody = bytes.toRequestBody(contentType.toMediaTypeOrNull())
        // 创建 multipart 文件 part
        val filePart = MultipartBody.Part.createFormData("file", fileName, requestBody)
        // 调用 API 上传头像
        val profile = apiService.uploadMyAvatar(filePart).requireData()
        // 更新内存缓存
        cachedProfile = profile
        // 更新本地数据库
        listDao.insertMyProfile(profile.toEntity())
        // 转换为头部模型返回
        profile.toHeader()
    }

    /**
     * 加载个人资料的完整数据。
     *
     * 实现了三级缓存策略：内存缓存 -> 本地数据库 -> 网络请求。
     * 使用互斥锁确保并发场景下的数据一致性。
     *
     * @param forceRefresh 是否强制从网络刷新。
     * @return 完整的个人资料 DTO。
     */
    private suspend fun loadProfile(forceRefresh: Boolean): RemoteMyProfileDto {
        // 非强制刷新时，先检查内存缓存
        if (!forceRefresh) {
            cachedProfile?.let { return it }
            // 再检查本地数据库
            listDao.getMyProfile()?.let { return it.toRemoteDto() }
        }

        // 使用互斥锁进行并发控制
        return profileMutex.withLock {
            // 获取锁后再次检查缓存（双重检查锁定模式）
            if (!forceRefresh) {
                cachedProfile?.let { return@withLock it }
                listDao.getMyProfile()?.let { return@withLock it.toRemoteDto() }
            }

            // 尝试从网络获取
            val remote = runCatching { apiService.getMyProfile().requireData() }.getOrElse { error ->
                // 网络失败，降级到本地数据库
                listDao.getMyProfile()?.let { return@withLock it.toRemoteDto() }
                throw error
            }

            // 成功获取网络数据，更新缓存
            cachedProfile = remote
            listDao.insertMyProfile(remote.toEntity())
            remote
        }
    }

    // ==================== DTO -> Entity 转换方法 ====================

    /**
     * 将远程个人资料 DTO 转换为数据库实体。
     *
     * @return 个人资料实体对象。
     */
    private fun RemoteMyProfileDto.toEntity(): MyProfileEntity {
        return MyProfileEntity(
            id = id,
            name = name,
            avatarUrl = avatarUrl,
            avatarText = avatarText,
            avatarColorHex = avatarColorHex,
            bio = bio,
            followingCount = followingCount,
            fansCount = fansCount,
            likesCount = likesCount,
            noteCount = noteCount
        )
    }

    /**
     * 将数据库实体转换为远程个人资料 DTO。
     *
     * 主要用于将本地缓存数据转换为 DTO 格式，以统一处理逻辑。
     *
     * @return 个人资料 DTO 对象。
     */
    private fun MyProfileEntity.toRemoteDto(): RemoteMyProfileDto {
        return RemoteMyProfileDto(
            id = id,
            name = name,
            avatarUrl = avatarUrl,
            avatarText = avatarText,
            avatarColorHex = avatarColorHex,
            bio = bio,
            followingCount = followingCount,
            fansCount = fansCount,
            likesCount = likesCount,
            noteCount = noteCount
        )
    }

    /**
     * 将远程个人资料 DTO 转换为 UI 头部模型。
     *
     * @return 个人资料头部模型。
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
     * 将远程个人资料 DTO 转换为 UI 统计模型。
     *
     * 将数字类型的统计字段转换为字符串格式。
     *
     * @return 个人统计模型。
     */
    private fun RemoteMyProfileDto.toStats(): MyProfileStats {
        return MyProfileStats(
            followingCount = followingCount.toString(),
            fansCount = fansCount.toString(),
            likesCount = likesCount.toString()
        )
    }

    /**
     * 将远程感兴趣的人 DTO 转换为数据库实体。
     *
     * @param sortOrder 排序顺序。
     * @return 感兴趣的人实体对象。
     */
    private fun RemoteMyInterestPersonDto.toEntity(sortOrder: Int): InterestPersonEntity {
        return InterestPersonEntity(
            id = id,
            avatarText = avatarText,
            name = name,
            fansText = fansText,
            // 使用资源映射器解析头像背景资源 ID
            avatarBackgroundRes = resourceMapper.drawableByName(
                name = avatarBackgroundKey,
                fallback = resourceMapper.defaultSuggestionAvatarBackground()
            ),
            sortOrder = sortOrder
        )
    }

    /**
     * 将远程感兴趣的人 DTO 直接转换为 UI 模型。
     *
     * @return 感兴趣的人 UI 模型。
     */
    private fun RemoteMyInterestPersonDto.toUiModel(): InterestPersonItem {
        return InterestPersonItem(
            id = id,
            avatarText = avatarText,
            name = name,
            fansText = fansText,
            // 使用资源映射器解析头像背景资源 ID
            avatarBackgroundRes = resourceMapper.drawableByName(
                name = avatarBackgroundKey,
                fallback = resourceMapper.defaultSuggestionAvatarBackground()
            )
        )
    }

    /**
     * 将数据库实体转换为 UI 模型。
     *
     * @return 感兴趣的人 UI 模型。
     */
    private fun InterestPersonEntity.toUiModel(): InterestPersonItem {
        return InterestPersonItem(
            id = id,
            avatarText = avatarText,
            name = name,
            fansText = fansText,
            avatarBackgroundRes = avatarBackgroundRes
        )
    }
}