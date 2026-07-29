/**
 * 文件说明：RedBookApiService.kt
 * 作用：集中声明 remote 层所有 Retrofit 接口契约。
 * 备注：这里只描述“如何请求后端”以及“返回什么 DTO”，不承担业务组装、缓存或 UI 逻辑。
 */
package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.remote.model.ApiResponseDto
import com.zhengyang.redbook.data.remote.model.PageResponseDto
import com.zhengyang.redbook.data.remote.model.RemoteCommentDto
import com.zhengyang.redbook.data.remote.model.RemoteCreateCommentRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteCreateNoteRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteDraftDto
import com.zhengyang.redbook.data.remote.model.RemoteFeedItemDto
import com.zhengyang.redbook.data.remote.model.RemoteFollowToggleRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteFollowingSeedDto
import com.zhengyang.redbook.data.remote.model.RemoteHomeCategoryDto
import com.zhengyang.redbook.data.remote.model.RemoteMessageOverviewDto
import com.zhengyang.redbook.data.remote.model.RemoteMyInterestPersonDto
import com.zhengyang.redbook.data.remote.model.RemoteMyProfileDto
import com.zhengyang.redbook.data.remote.model.RemoteNoteDetailDto
import com.zhengyang.redbook.data.remote.model.RemotePushTestRequestDto
import com.zhengyang.redbook.data.remote.model.RemotePushTokenRegistrationRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteSaveDraftRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteSearchBootstrapDto
import com.zhengyang.redbook.data.remote.model.RemoteSearchGuessDto
import com.zhengyang.redbook.data.remote.model.RemoteSearchResultDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 后端接口服务定义。
 *
 * 该接口是 remote 层对 HTTP API 的统一声明入口，职责主要有两点：
 * 1. 使用 Retrofit 注解描述请求方法、请求路径、查询参数和请求体。
 * 2. 将后端返回的数据映射成结构化 DTO，交给 repository 继续处理。
 *
 * 这里不做业务判断，也不做数据拼装，尽量保持“接口契约层”的单一职责。
 */
interface RedBookApiService {

    /** 获取首页分类列表，通常用于首页顶部频道栏初始化。 */
    @GET("api/v1/home/categories")
    suspend fun getHomeCategories(): ApiResponseDto<List<RemoteHomeCategoryDto>>

    /**
     * 获取首页内容流。
     *
     * 该接口返回某个分类下的 feed 数据，采用偏移量分页。
     *
     * @param categoryId 当前频道或分类 ID。
     * @param offset 分页偏移量。
     * @param limit 单次请求数量。
     * @return 包裹分页结构的内容流结果。
     */
    @GET("api/v1/home/feed")
    suspend fun getHomeFeed(
        @Query("categoryId") categoryId: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): ApiResponseDto<PageResponseDto<RemoteFeedItemDto>>

    /**
     * 获取关注页首屏种子数据。
     *
     * 这里的“种子数据”通常同时包含推荐关注用户与关注 feed，
     * 方便页面一次请求完成首屏搭建。
     *
     * @param offset feed 分页偏移量。
     * @param limit 单次请求数量。
     * @return 推荐用户与关注流的组合结果。
     */
    @GET("api/v1/home/following/seed")
    suspend fun getFollowingSeed(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): ApiResponseDto<RemoteFollowingSeedDto>

    /**
     * 关注或取消关注指定用户。
     *
     * `value=true` 表示关注，`value=false` 表示取消关注。
     *
     * @param userId 目标用户 ID。
     * @param request 关注开关请求体。
     * @return 通用响应体。
     */
    @POST("api/v1/users/{userId}/follow")
    suspend fun followUser(
        @Path("userId") userId: String,
        @Body request: RemoteFollowToggleRequestDto = RemoteFollowToggleRequestDto(true)
    ): ApiResponseDto<Map<String, Any?>>

    /** 获取消息页概览数据，用于消息列表与推荐人物模块初始化。 */
    @GET("api/v1/messages/overview")
    suspend fun getMessageOverview(): ApiResponseDto<RemoteMessageOverviewDto>

    /** 获取当前用户个人资料，常用于“我的”页头部数据回填。 */
    @GET("api/v1/me/profile")
    suspend fun getMyProfile(): ApiResponseDto<RemoteMyProfileDto>

    /**
     * 上传当前用户头像。
     *
     * 使用 multipart 上传二进制文件，服务端返回最新个人资料，
     * 便于调用方直接用确认后的远端数据刷新页面。
     */
    @Multipart
    @POST("api/v1/me/avatar")
    suspend fun uploadMyAvatar(
        @Part file: MultipartBody.Part
    ): ApiResponseDto<RemoteMyProfileDto>

    /** 获取当前用户感兴趣的人列表，常用于推荐关注区域。 */
    @GET("api/v1/me/interests")
    suspend fun getMyInterests(): ApiResponseDto<List<RemoteMyInterestPersonDto>>

    /**
     * 获取笔记详情。
     *
     * 返回详情页展示所需的核心字段，包括作者、媒体内容、互动状态和统计信息。
     *
     * @param noteId 笔记 ID。
     * @return 笔记详情 DTO。
     */
    @GET("api/v1/notes/{noteId}")
    suspend fun getNoteDetail(
        @Path("noteId") noteId: String
    ): ApiResponseDto<RemoteNoteDetailDto>

    /**
     * 获取笔记评论列表。
     *
     * `sortMode` 由前端评论排序控件驱动，并直接透传给后端，
     * 用于保持前后端排序语义一致。
     *
     * @param noteId 笔记 ID。
     * @param sortMode 排序模式。
     * @param offset 分页偏移量。
     * @param limit 单次请求数量。
     * @return 评论分页结果。
     */
    @GET("api/v1/notes/{noteId}/comments")
    suspend fun getNoteComments(
        @Path("noteId") noteId: String,
        @Query("sortMode") sortMode: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): ApiResponseDto<PageResponseDto<RemoteCommentDto>>

    /**
     * 切换笔记点赞状态。
     *
     * @param noteId 笔记 ID。
     * @param request 点赞开关请求体。
     * @return 通用响应体。
     */
    @POST("api/v1/notes/{noteId}/like")
    suspend fun toggleNoteLike(
        @Path("noteId") noteId: String,
        @Body request: RemoteFollowToggleRequestDto
    ): ApiResponseDto<Map<String, Any?>>

    /**
     * 切换笔记收藏状态。
     *
     * @param noteId 笔记 ID。
     * @param request 收藏开关请求体。
     * @return 通用响应体。
     */
    @POST("api/v1/notes/{noteId}/collect")
    suspend fun toggleNoteCollect(
        @Path("noteId") noteId: String,
        @Body request: RemoteFollowToggleRequestDto
    ): ApiResponseDto<Map<String, Any?>>

    /**
     * 创建评论或回复。
     *
     * 当 `parentCommentId` 为空时表示创建一级评论；
     * 否则表示在指定父评论下创建回复。
     *
     * @param request 评论创建请求体。
     * @return 服务端创建后的评论 DTO。
     */
    @POST("api/v1/comments")
    suspend fun createComment(
        @Body request: RemoteCreateCommentRequestDto
    ): ApiResponseDto<RemoteCommentDto>

    /**
     * 切换评论点赞状态。
     *
     * 该接口同时适用于一级评论和回复点赞，只要两者共用同一套 commentId 语义即可。
     *
     * @param commentId 评论或回复 ID。
     * @param request 点赞开关请求体。
     * @return 通用响应体。
     */
    @POST("api/v1/comments/{commentId}/like")
    suspend fun toggleCommentLike(
        @Path("commentId") commentId: String,
        @Body request: RemoteFollowToggleRequestDto
    ): ApiResponseDto<Map<String, Any?>>

    /** 获取搜索页启动数据，用于初始化历史记录和猜词区域。 */
    @GET("api/v1/search/bootstrap")
    suspend fun getSearchBootstrap(): ApiResponseDto<RemoteSearchBootstrapDto>

    /**
     * 获取搜索联想词。
     *
     * @param query 当前输入内容。
     * @return 联想词列表。
     */
    @GET("api/v1/search/suggestions")
    suspend fun getSearchSuggestions(
        @Query("query") query: String
    ): ApiResponseDto<List<RemoteSearchGuessDto>>

    /**
     * 获取搜索结果列表。
     *
     * @param query 搜索词。
     * @return 搜索结果项列表。
     */
    @GET("api/v1/search/results")
    suspend fun getSearchResults(
        @Query("query") query: String
    ): ApiResponseDto<List<RemoteSearchResultDto>>

    /**
     * 创建新笔记。
     *
     * 发布成功后直接返回完整详情 DTO，方便前端立即跳转到详情页或刷新发布结果页。
     *
     * @param request 创建笔记请求体。
     * @return 新创建的笔记详情 DTO。
     */
    @POST("api/v1/notes")
    suspend fun createNote(
        @Body request: RemoteCreateNoteRequestDto
    ): ApiResponseDto<RemoteNoteDetailDto>

    /** 获取当前用户草稿列表，用于草稿箱和发布页恢复。 */
    @GET("api/v1/drafts")
    suspend fun getDrafts(): ApiResponseDto<List<RemoteDraftDto>>

    /**
     * 保存草稿。
     *
     * 当 `id` 为空时通常表示新建草稿；有值时表示更新已有草稿。
     *
     * @param request 草稿保存请求体。
     * @return 保存后的草稿 DTO。
     */
    @POST("api/v1/drafts")
    suspend fun saveDraft(
        @Body request: RemoteSaveDraftRequestDto
    ): ApiResponseDto<RemoteDraftDto>

    @POST("api/v1/push/device-token")
    suspend fun registerPushToken(
        @Body request: RemotePushTokenRegistrationRequestDto
    ): ApiResponseDto<Map<String, Any?>>

    @POST("api/v1/push/device-token/unregister")
    suspend fun unregisterPushToken(
        @Body request: RemotePushTokenRegistrationRequestDto
    ): ApiResponseDto<Map<String, Any?>>

    @POST("api/v1/push/test")
    suspend fun sendPushTest(
        @Body request: RemotePushTestRequestDto
    ): ApiResponseDto<Map<String, Any?>>
}
